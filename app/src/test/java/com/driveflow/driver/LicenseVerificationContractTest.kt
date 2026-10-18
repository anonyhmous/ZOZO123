package com.driveflow.driver

import com.driveflow.driver.core.di.LicenseVerificationResult
import com.driveflow.driver.core.model.LicenseStatus
import com.driveflow.driver.license.OfflineLicenseVerifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LicenseVerificationContractTest {

    private val verifier = OfflineLicenseVerifier()

    companion object {
        const val TEST_PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEoGjsZvNDUPlp8yESZA8nwEq/G4ASQ8+PWaOe1fYBDDgZpYbxfXmgyFCasIxNkXw1RLS2JyHoIdjYuaVyn6YkMA=="
        const val ADMIN_PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEgVJ6n5L6G5sml84eOMpYjQop6TxafsQpX1E9k6pZOrFRanQ5RgsRKH1Kv5WavZGrrnhMIEm5KQ3NUNLIeBsz7w=="
        const val DEVICE_ID_HEX = "0123456789abcdef0123456789abcdef"
        const val TODAY_DAY = 1020 // 2026-10-17
    }

    @Test
    fun testVector1_validPaid30d() {
        val token = "DF1.AQED_AQaASNFZ4mrze8BI0VniavN7zV1F9cXcT8z5VOtBQUpzYNJ6KepHaIGEMhOWNiyNSGHfFPAzqePAM8cI8DnLjNlnbVqJljRMUipJ7i4kSKl4vo"
        val result = verifier.verify(
            token = token,
            publicKeySpkiB64 = TEST_PUBLIC_KEY,
            deviceIdHex = DEVICE_ID_HEX,
            currentDay = TODAY_DAY,
            clockHighWaterDay = TODAY_DAY
        )
        assertTrue("Expected Valid result, got $result", result is LicenseVerificationResult.Valid)
        val valid = result as LicenseVerificationResult.Valid
        assertEquals("Expected plan 1 (paid)", 1, valid.plan)
        assertEquals("Expected status ACTIVE", LicenseStatus.ACTIVE, valid.status)
        assertEquals("Expected expiresDay 1050", 1050, valid.expiresDay)
        assertEquals("Expected issuedDay 1020", 1020, valid.issuedDay)
    }

    @Test
    fun testVector2_validTrial2d() {
        val token = "DF1.AQID_AP-ASNFZ4mrze8BI0VniavN70ZX4JPeC432UdJyi-_l86TS6BQx2MIbGSpffpmcGrw89-UNi2byH8DmcH2wmHrcRmrQJte2dNePEjBiKI7L4P0"
        val result = verifier.verify(
            token = token,
            publicKeySpkiB64 = TEST_PUBLIC_KEY,
            deviceIdHex = DEVICE_ID_HEX,
            currentDay = TODAY_DAY,
            clockHighWaterDay = TODAY_DAY
        )
        assertTrue("Expected Valid result, got $result", result is LicenseVerificationResult.Valid)
        val valid = result as LicenseVerificationResult.Valid
        assertEquals("Expected plan 2 (trial)", 2, valid.plan)
        assertEquals("Expected status TRIAL", LicenseStatus.TRIAL, valid.status)
        assertEquals("Expected expiresDay 1022", 1022, valid.expiresDay)
        assertEquals("Expected issuedDay 1020", 1020, valid.issuedDay)
    }

    @Test
    fun testVector3_expired() {
        val token = "DF1.AQED1AP7ASNFZ4mrze8BI0VniavN7xge78VdY9Oqn3AJZ5krSEt6HwLzWArTo94sMH3VBQsAFiTffDPOWmppHsLn0gGZ0uFT4aZFhF0ykuyrLU2CPPY"
        val result = verifier.verify(
            token = token,
            publicKeySpkiB64 = TEST_PUBLIC_KEY,
            deviceIdHex = DEVICE_ID_HEX,
            currentDay = TODAY_DAY,
            clockHighWaterDay = TODAY_DAY
        )
        assertTrue("Expected Error result, got $result", result is LicenseVerificationResult.Error)
        val error = result as LicenseVerificationResult.Error
        assertEquals("Expected EXPIRED error", LicenseVerificationResult.ErrorCode.EXPIRED, error.code)
    }

    @Test
    fun testVector4_wrongDevice() {
        val token = "DF1.AQED_AQa_ty6mHZUMhD-3LqYdlQyEOb58b_BbQFo_tYrjCsExxvJ_BFT4vwm4yQDRmtIDPvkUSrtAszmWxBA4zr3IvYCTFGpqMwNIj_ShKq725ugnCo"
        val result = verifier.verify(
            token = token,
            publicKeySpkiB64 = TEST_PUBLIC_KEY,
            deviceIdHex = DEVICE_ID_HEX,
            currentDay = TODAY_DAY,
            clockHighWaterDay = TODAY_DAY
        )
        assertTrue("Expected Error result, got $result", result is LicenseVerificationResult.Error)
        val error = result as LicenseVerificationResult.Error
        assertEquals("Expected WRONG_DEVICE error", LicenseVerificationResult.ErrorCode.WRONG_DEVICE, error.code)
    }

    @Test
    fun testVector5_tamperedSignature() {
        val token = "DF1.AQED_AQaASNFZ4mrze8BI0VniavN7zV1F9cXcT8z5VOtBQUpzYNJ6KepHaIGEMhOWNiyNSGHfFPAzqePAM8cI8DnLjNlnbVqJljRMUipJ7i4kSKl4vs"
        val result = verifier.verify(
            token = token,
            publicKeySpkiB64 = TEST_PUBLIC_KEY,
            deviceIdHex = DEVICE_ID_HEX,
            currentDay = TODAY_DAY,
            clockHighWaterDay = TODAY_DAY
        )
        assertTrue("Expected Error result, got $result", result is LicenseVerificationResult.Error)
        val error = result as LicenseVerificationResult.Error
        assertEquals("Expected BAD_SIGNATURE error", LicenseVerificationResult.ErrorCode.BAD_SIGNATURE, error.code)
    }

    @Test
    fun testClockTamperingDetection() {
        val token = "DF1.AQED_AQaASNFZ4mrze8BI0VniavN7zV1F9cXcT8z5VOtBQUpzYNJ6KepHaIGEMhOWNiyNSGHfFPAzqePAM8cI8DnLjNlnbVqJljRMUipJ7i4kSKl4vo"
        // Phone clock turned back by 5 days (1015 < 1020 - 1)
        val result = verifier.verify(
            token = token,
            publicKeySpkiB64 = TEST_PUBLIC_KEY,
            deviceIdHex = DEVICE_ID_HEX,
            currentDay = 1015,
            clockHighWaterDay = 1020
        )
        assertTrue("Expected CLOCK_TAMPERED, got $result", result is LicenseVerificationResult.Error)
        val error = result as LicenseVerificationResult.Error
        assertEquals(LicenseVerificationResult.ErrorCode.CLOCK_TAMPERED, error.code)
    }

    @Test
    fun testNotYetValid() {
        val token = "DF1.AQED_AQaASNFZ4mrze8BI0VniavN7zV1F9cXcT8z5VOtBQUpzYNJ6KepHaIGEMhOWNiyNSGHfFPAzqePAM8cI8DnLjNlnbVqJljRMUipJ7i4kSKl4vo"
        // Token was issued on day 1020, but current phone is on day 1018 (issuedDay > effectiveDay + 1)
        val result = verifier.verify(
            token = token,
            publicKeySpkiB64 = TEST_PUBLIC_KEY,
            deviceIdHex = DEVICE_ID_HEX,
            currentDay = 1018,
            clockHighWaterDay = 1018
        )
        assertTrue("Expected NOT_YET_VALID, got $result", result is LicenseVerificationResult.Error)
        val error = result as LicenseVerificationResult.Error
        assertEquals(LicenseVerificationResult.ErrorCode.NOT_YET_VALID, error.code)
    }
}
