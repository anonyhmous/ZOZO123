package com.driveflow.driver.license

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object DeviceHelper {
    /**
     * Computes the 32 lowercase hex characters of SHA-256( ANDROID_ID + ":" + "com.driveflow.driver" ).
     */
    @SuppressLint("HardwareIds")
    fun getDeviceIdHex(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "0000000000000000"
        return computeDeviceIdHex(androidId)
    }

    /**
     * Pure function to compute deviceId hex from raw androidId.
     */
    fun computeDeviceIdHex(androidId: String): String {
        val input = "$androidId:com.driveflow.driver"
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        val fullHex = digest.joinToString("") { "%02x".format(it) }
        return fullHex.take(32).lowercase()
    }

    /**
     * Formats 32 hex characters into 8 groups of 4 uppercase characters separated by dashes.
     * Example: ABCD-EF01-2345-6789-ABCD-EF01-2345-6789
     */
    fun formatDisplayCode(deviceIdHex: String): String {
        val clean = deviceIdHex.replace("-", "").replace(" ", "").uppercase()
        if (clean.length < 32) return clean
        return clean.take(32).chunked(4).joinToString("-")
    }

    /**
     * Normalizes any input display code or hex back into 32 lowercase hex characters.
     */
    fun normalizeDeviceId(input: String): String {
        return input.replace("-", "").replace(" ", "").lowercase().take(32)
    }
}
