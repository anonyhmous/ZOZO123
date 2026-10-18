package com.driveflow.driver.license

import com.driveflow.driver.core.di.LicenseVerificationResult
import com.driveflow.driver.core.di.LicenseVerifier
import com.driveflow.driver.core.model.LicenseStatus
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import kotlin.math.max

/**
 * Production implementation of the LICENSE CONTRACT offline verifier.
 * Verifies ECDSA P-256 signatures with SHA-256 entirely on-device without any server.
 * Operates in strict order according to the License Contract specification.
 */
class OfflineLicenseVerifier : LicenseVerifier {
    companion object {
        const val PREFIX = "DF1."
        const val EXPECTED_PAYLOAD_LEN = 22
        const val EXPECTED_SIG_LEN = 64
        const val EXPECTED_TOTAL_LEN = EXPECTED_PAYLOAD_LEN + EXPECTED_SIG_LEN // 86 bytes
    }

    override fun verify(
        token: String,
        publicKeySpkiB64: String,
        deviceIdHex: String,
        currentDay: Int,
        clockHighWaterDay: Int
    ): LicenseVerificationResult {
        // 1. Remove spaces and line breaks
        val clean = token.replace("\\s+".toRegex(), "")

        // 2. Text must start with "DF1." else BAD_FORMAT
        if (!clean.startsWith(PREFIX)) {
            return LicenseVerificationResult.Error(
                LicenseVerificationResult.ErrorCode.BAD_FORMAT,
                "Token must start with $PREFIX"
            )
        }

        val b64Part = clean.substring(PREFIX.length)

        // 3. Base64url decode and length must be exactly 86 bytes else BAD_FORMAT
        val rawBytes = try {
            Base64.getUrlDecoder().decode(b64Part)
        } catch (e: Exception) {
            return LicenseVerificationResult.Error(
                LicenseVerificationResult.ErrorCode.BAD_FORMAT,
                "Invalid Base64url encoding"
            )
        }

        if (rawBytes.size != EXPECTED_TOTAL_LEN) {
            return LicenseVerificationResult.Error(
                LicenseVerificationResult.ErrorCode.BAD_FORMAT,
                "Expected 86 decoded bytes, found ${rawBytes.size}"
            )
        }

        // 4. Payload byte 0 must be 1 (version) else BAD_FORMAT
        val version = rawBytes[0].toInt() and 0xFF
        if (version != 1) {
            return LicenseVerificationResult.Error(
                LicenseVerificationResult.ErrorCode.BAD_FORMAT,
                "Unsupported payload version: $version"
            )
        }

        // Extract payload parts:
        val planByte = rawBytes[1].toInt() and 0xFF
        val issuedDay = ((rawBytes[2].toInt() and 0xFF) shl 8) or (rawBytes[3].toInt() and 0xFF)
        val expiresDay = ((rawBytes[4].toInt() and 0xFF) shl 8) or (rawBytes[5].toInt() and 0xFF)
        val payloadDeviceIdBytes = rawBytes.copyOfRange(6, 22)
        val payloadDeviceIdHex = payloadDeviceIdBytes.joinToString("") { "%02x".format(it) }.lowercase()

        // Extract raw signature r (32 bytes) and s (32 bytes)
        val payloadBytes = rawBytes.copyOfRange(0, EXPECTED_PAYLOAD_LEN)
        val rBytes = rawBytes.copyOfRange(22, 54)
        val sBytes = rawBytes.copyOfRange(54, 86)

        // 5. Verify ECDSA signature over the 22 payload bytes
        val publicKey = try {
            val pubBytes = Base64.getDecoder().decode(publicKeySpkiB64.trim())
            val keySpec = X509EncodedKeySpec(pubBytes)
            KeyFactory.getInstance("EC").generatePublic(keySpec)
        } catch (e: Exception) {
            return LicenseVerificationResult.Error(
                LicenseVerificationResult.ErrorCode.BAD_SIGNATURE,
                "Invalid public key specification: ${e.message}"
            )
        }

        val derSig = try {
            rawRsToDer(rBytes, sBytes)
        } catch (e: Exception) {
            return LicenseVerificationResult.Error(
                LicenseVerificationResult.ErrorCode.BAD_SIGNATURE,
                "Failed to convert raw signature to DER: ${e.message}"
            )
        }

        val sigValid = try {
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(publicKey)
            sig.update(payloadBytes)
            sig.verify(derSig)
        } catch (e: Exception) {
            false
        }

        if (!sigValid) {
            return LicenseVerificationResult.Error(
                LicenseVerificationResult.ErrorCode.BAD_SIGNATURE,
                "Cryptographic signature check failed"
            )
        }

        // 6. Bytes 6-21 must equal this phone's deviceId else WRONG_DEVICE
        val normalizedTargetDeviceId = DeviceHelper.normalizeDeviceId(deviceIdHex)
        if (payloadDeviceIdHex != normalizedTargetDeviceId) {
            return LicenseVerificationResult.Error(
                LicenseVerificationResult.ErrorCode.WRONG_DEVICE,
                "Token device ($payloadDeviceIdHex) does not match target device ($normalizedTargetDeviceId)"
            )
        }

        // Clock tampering check: if phone clock is earlier than clockHighWaterDay - 1
        if (currentDay < clockHighWaterDay - 1) {
            return LicenseVerificationResult.Error(
                LicenseVerificationResult.ErrorCode.CLOCK_TAMPERED,
                "Device clock has been rolled back ($currentDay < ${clockHighWaterDay - 1})"
            )
        }

        // 7. Effective day calculation
        val effectiveDay = max(currentDay, clockHighWaterDay)

        // 8. IssuedDay must be <= effectiveDay + 1 else NOT_YET_VALID
        if (issuedDay > effectiveDay + 1) {
            return LicenseVerificationResult.Error(
                LicenseVerificationResult.ErrorCode.NOT_YET_VALID,
                "Token issued in the future ($issuedDay > ${effectiveDay + 1})"
            )
        }

        // 9. EffectiveDay must be <= expiresDay else EXPIRED
        if (effectiveDay > expiresDay) {
            return LicenseVerificationResult.Error(
                LicenseVerificationResult.ErrorCode.EXPIRED,
                "Token expired on day $expiresDay (effective day: $effectiveDay)"
            )
        }

        val status = if (planByte == 2) LicenseStatus.TRIAL else LicenseStatus.ACTIVE

        return LicenseVerificationResult.Valid(
            plan = planByte,
            expiresDay = expiresDay,
            issuedDay = issuedDay,
            status = status
        )
    }

    /**
     * Converts raw 32-byte r and 32-byte s to an ASN.1 DER SEQUENCE of two INTEGERs.
     */
    private fun rawRsToDer(r: ByteArray, s: ByteArray): ByteArray {
        val rDer = encodeDerInteger(r)
        val sDer = encodeDerInteger(s)
        val totalLen = 2 + rDer.size + 2 + sDer.size
        val der = ByteArray(2 + totalLen)
        der[0] = 0x30.toByte() // SEQUENCE
        der[1] = totalLen.toByte()
        der[2] = 0x02.toByte() // INTEGER
        der[3] = rDer.size.toByte()
        System.arraycopy(rDer, 0, der, 4, rDer.size)
        val sOffset = 4 + rDer.size
        der[sOffset] = 0x02.toByte() // INTEGER
        der[sOffset + 1] = sDer.size.toByte()
        System.arraycopy(sDer, 0, der, sOffset + 2, sDer.size)
        return der
    }

    private fun encodeDerInteger(bytes: ByteArray): ByteArray {
        var offset = 0
        while (offset < bytes.size - 1 && bytes[offset] == 0.toByte()) {
            offset++
        }
        val stripped = bytes.copyOfRange(offset, bytes.size)
        return if ((stripped[0].toInt() and 0x80) != 0) {
            byteArrayOf(0.toByte()) + stripped
        } else {
            stripped
        }
    }
}
