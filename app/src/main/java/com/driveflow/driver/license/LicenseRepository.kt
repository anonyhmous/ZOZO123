package com.driveflow.driver.license

import android.content.Context
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.di.LicenseVerificationResult
import com.driveflow.driver.core.model.LicenseStatus

interface LicenseRepository {
    suspend fun activate(token: String, context: Context? = null): LicenseVerificationResult
    suspend fun getRemainingDays(): Int
}

class DefaultLicenseRepository(private val context: Context? = null) : LicenseRepository {
    override suspend fun activate(token: String, context: Context?): LicenseVerificationResult {
        val trimmed = token.trim()
        val verifier = AppGraph.licenseVerifier
        val currentDay = AppGraph.clock.currentUtcDay()
        val clockHighWater = AppGraph.settingsStore.getClockHighWaterDay()
        val targetContext = context ?: this.context
        val deviceId = if (targetContext != null) {
            DeviceHelper.getDeviceIdHex(targetContext)
        } else {
            DeviceHelper.normalizeDeviceId(AppGraph.settingsStore.getContactPhone())
        }

        val result = verifier.verify(
            token = trimmed,
            publicKeySpkiB64 = AppGraph.licensePublicKey,
            deviceIdHex = deviceId,
            currentDay = currentDay,
            clockHighWaterDay = clockHighWater
        )
        if (result is LicenseVerificationResult.Valid) {
            AppGraph.settingsStore.setLicenseToken(trimmed)
            AppGraph.settingsStore.updateClockHighWaterDayIfGreater(currentDay)
        }
        return result
    }

    override suspend fun getRemainingDays(): Int {
        val token = AppGraph.settingsStore.getLicenseToken()
        if (token.isNullOrBlank()) return 0
        return 30
    }
}
