package com.driveflow.driver.core.di

import android.content.Context
import com.driveflow.driver.BuildConfig
import com.driveflow.driver.core.model.LicenseStatus
import com.driveflow.driver.data.prefs.SettingsStore
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Clock abstraction to allow deterministic testing of dates and "todayDay".
 */
interface Clock {
    fun currentTimeMillis(): Long
    fun currentUtcDay(): Int // Whole days since 2024-01-01 00:00 UTC
}

/**
 * Real system clock calculating UTC days since 2024-01-01.
 */
class SystemClock : Clock {
    private val baseEpochDay = LocalDate.of(2024, 1, 1).toEpochDay()

    override fun currentTimeMillis(): Long = System.currentTimeMillis()

    override fun currentUtcDay(): Int {
        val todayEpochDay = LocalDate.now(ZoneOffset.UTC).toEpochDay()
        return (todayEpochDay - baseEpochDay).toInt()
    }
}

/**
 * Fixed / mock clock for unit tests.
 */
class FixedClock(
    private var fixedTimeMillis: Long = 0L,
    private var fixedUtcDay: Int = 1020 // Default pretend "today" from test vectors
) : Clock {
    override fun currentTimeMillis(): Long = fixedTimeMillis
    override fun currentUtcDay(): Int = fixedUtcDay

    fun setDay(day: Int) {
        fixedUtcDay = day
    }

    fun setTime(millis: Long) {
        fixedTimeMillis = millis
    }
}

/**
 * Results of verifying an offline license token.
 */
sealed class LicenseVerificationResult {
    data class Valid(
        val plan: Int, // 1 = paid, 2 = trial
        val expiresDay: Int,
        val issuedDay: Int,
        val status: LicenseStatus
    ) : LicenseVerificationResult()

    data class Error(
        val code: ErrorCode,
        val message: String
    ) : LicenseVerificationResult()

    enum class ErrorCode {
        BAD_FORMAT,
        BAD_SIGNATURE,
        WRONG_DEVICE,
        NOT_YET_VALID,
        EXPIRED,
        CLOCK_TAMPERED
    }
}

/**
 * License verifier interface. Must always receive the public key as a parameter.
 */
interface LicenseVerifier {
    fun verify(
        token: String,
        publicKeySpkiB64: String,
        deviceIdHex: String,
        currentDay: Int,
        clockHighWaterDay: Int
    ): LicenseVerificationResult
}

/**
 * Default fallback verifier for unit tests.
 */
class DefaultLicenseVerifier : LicenseVerifier {
    override fun verify(
        token: String,
        publicKeySpkiB64: String,
        deviceIdHex: String,
        currentDay: Int,
        clockHighWaterDay: Int
    ): LicenseVerificationResult {
        val cleaned = token.replace("\\s+".toRegex(), "")
        if (!cleaned.startsWith("DF1.")) {
            return LicenseVerificationResult.Error(
                LicenseVerificationResult.ErrorCode.BAD_FORMAT,
                "Token must start with DF1."
            )
        }
        return LicenseVerificationResult.Valid(
            plan = 1,
            expiresDay = currentDay + 30,
            issuedDay = currentDay,
            status = LicenseStatus.ACTIVE
        )
    }
}

/**
 * Manual Dependency Injection container holding singleton dependencies.
 */
object AppGraph {
    const val DEFAULT_ADMIN_PUBLIC_KEY =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEgVJ6n5L6G5sml84eOMpYjQop6TxafsQpX1E9k6pZOrFRanQ5RgsRKH1Kv5WavZGrrnhMIEm5KQ3NUNLIeBsz7w=="

    @Volatile
    private var initialized = false

    lateinit var settingsStore: SettingsStore
        private set

    lateinit var ocrCalibrationManager: com.driveflow.driver.ocr.OcrCalibrationManager
        private set

    var clock: Clock = SystemClock()
    var licenseVerifier: LicenseVerifier = com.driveflow.driver.license.OfflineLicenseVerifier()
    var ocrEngine: com.driveflow.driver.ocr.OcrEngine? = null

    val isInitialized: Boolean
        get() = initialized

    val licensePublicKey: String
        get() = try {
            if (BuildConfig.LICENSE_PUBLIC_KEY.isNotBlank()) BuildConfig.LICENSE_PUBLIC_KEY else DEFAULT_ADMIN_PUBLIC_KEY
        } catch (_: Exception) {
            DEFAULT_ADMIN_PUBLIC_KEY
        }

    val ownerWhatsapp: String
        get() = try { BuildConfig.OWNER_WHATSAPP } catch (_: Exception) { "201128440045" }

    val ownerInstapay: String
        get() = try { BuildConfig.OWNER_INSTAPAY } catch (_: Exception) { "01122104999" }

    val priceText: String
        get() = try { BuildConfig.PRICE_TEXT } catch (_: Exception) { "150 EGP / month" }

    val freeDailyList: Int
        get() = try { BuildConfig.FREE_DAILY_LIST } catch (_: Exception) { 20 }

    val freeDailyDetails: Int
        get() = try { BuildConfig.FREE_DAILY_DETAILS } catch (_: Exception) { 5 }

    lateinit var decisionStore: com.driveflow.driver.engine.DecisionStore
        private set

    lateinit var opportunityDetector: com.driveflow.driver.engine.OpportunityDetector
        private set

    lateinit var homeWayScorer: com.driveflow.driver.engine.HomeWayScorer
        private set

    fun init(context: Context) {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    val appContext = context.applicationContext
                    settingsStore = SettingsStore(appContext)
                    ocrCalibrationManager = com.driveflow.driver.ocr.OcrCalibrationManager(appContext)
                    decisionStore = com.driveflow.driver.engine.DecisionStore(
                        diagnosticCounterIncrementer = { counterName ->
                            settingsStore.incrementDiagnosticCounter(counterName)
                        }
                    )
                    opportunityDetector = com.driveflow.driver.engine.OpportunityDetector()
                    homeWayScorer = com.driveflow.driver.engine.HomeWayScorer(appContext)
                    initialized = true
                }
            }
        }
    }

    /**
     * Test injector for isolation in unit and Robolectric tests.
     */
    fun initForTesting(
        settingsStore: SettingsStore,
        clock: Clock = FixedClock(),
        licenseVerifier: LicenseVerifier = DefaultLicenseVerifier(),
        ocrCalibrationManager: com.driveflow.driver.ocr.OcrCalibrationManager? = null,
        ocrEngine: com.driveflow.driver.ocr.OcrEngine? = null,
        decisionStore: com.driveflow.driver.engine.DecisionStore? = null,
        opportunityDetector: com.driveflow.driver.engine.OpportunityDetector? = null,
        homeWayScorer: com.driveflow.driver.engine.HomeWayScorer? = null
    ) {
        this.settingsStore = settingsStore
        this.clock = clock
        this.licenseVerifier = licenseVerifier
        if (ocrCalibrationManager != null) {
            this.ocrCalibrationManager = ocrCalibrationManager
        }
        this.ocrEngine = ocrEngine
        this.decisionStore = decisionStore ?: com.driveflow.driver.engine.DecisionStore(debounceDelayMs = 0L)
        this.opportunityDetector = opportunityDetector ?: com.driveflow.driver.engine.OpportunityDetector()
        this.homeWayScorer = homeWayScorer ?: com.driveflow.driver.engine.HomeWayScorer()
        this.initialized = true
    }
}
