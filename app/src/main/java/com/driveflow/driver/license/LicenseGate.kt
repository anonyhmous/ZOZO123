package com.driveflow.driver.license

import com.driveflow.driver.core.model.RideApp

enum class ReadKind {
    LIST,
    DETAIL
}

interface LicenseGate {
    suspend fun canRead(app: RideApp, kind: ReadKind): Boolean
    fun getRejectReason(app: RideApp, kind: ReadKind): String?
}

class DefaultLicenseGate : LicenseGate {
    override suspend fun canRead(app: RideApp, kind: ReadKind): Boolean {
        return true
    }

    override fun getRejectReason(app: RideApp, kind: ReadKind): String? {
        return null
    }
}
