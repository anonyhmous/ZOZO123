package com.driveflow.driver.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.model.LicenseStatus
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.Verdict
import com.driveflow.driver.engine.RulesEngine
import com.driveflow.driver.ocr.CaptureResult
import com.driveflow.driver.ocr.CompositeOcrEngine
import com.driveflow.driver.ocr.OcrThrottleController
import com.driveflow.driver.ocr.ScreenshotSource
import com.driveflow.driver.overlay.OverlayManager
import com.driveflow.driver.reader.didi.DidiReader
import com.driveflow.driver.reader.indrive.InDriveReader
import com.driveflow.driver.reader.indrive.InDriveTripDistanceOcr
import com.driveflow.driver.reader.uber.UberReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

/**
 * Core Accessibility Service for DriveFlow Driver.
 * Responsibilities:
 * - Listens to window state and content changes.
 * - Inspects active ride apps (inDrive, Uber, DiDi) strictly in read-only mode.
 * - Extracts requests (UI hierarchy for inDrive, Screenshot+OCR for Uber & DiDi).
 * - Evaluates requests using the driver's custom RulesEngine and Policy.
 * - Updates floating overlay markers and DecisionBar.
 * - Refuses to read unless user consent is recorded (consentVersion >= 1).
 * Guaranteed: NEVER performs clicks, gestures, or taps on any ride app.
 */
class DriveFlowAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val processingMutex = Mutex()
    private var debounceJob: Job? = null

    private var compositeOcrEngine: CompositeOcrEngine? = null
    private var screenshotSource: ScreenshotSource? = null
    private var ocrThrottleController: OcrThrottleController? = null
    private var overlayManager: OverlayManager? = null

    private var inDriveReader: InDriveReader? = null
    private var uberReader: UberReader? = null
    private var didiReader: DidiReader? = null

    @Volatile
    private var currentForegroundPackage: String? = null

    @Volatile
    private var cachedPolicy: Policy = Policy()

    @Volatile
    private var cachedLicenseStatus: LicenseStatus = LicenseStatus.FREE

    @Volatile
    private var currentConsentVersion: Int = 0

    companion object {
        private val IGNORED_PACKAGES = setOf(
            "com.android.systemui",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
            "com.touchtype.swiftkey",
            "android"
        )
        private val SUPPORTED_PACKAGES = setOf(
            RideApp.INDRIVE.pkg,
            RideApp.UBER.pkg,
            RideApp.DIDI.pkg
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppGraph.init(applicationContext)

        val ocrEngine = CompositeOcrEngine(applicationContext)
        this.compositeOcrEngine = ocrEngine
        val screenshots = ScreenshotSource { this }
        this.screenshotSource = screenshots
        val throttle = OcrThrottleController(applicationContext)
        this.ocrThrottleController = throttle

        val overlays = OverlayManager(
            context = applicationContext,
            isAccessibilityService = true
        )
        this.overlayManager = overlays

        val tripOcr = InDriveTripDistanceOcr(
            screenshotSource = screenshots,
            ocrEngine = ocrEngine,
            overlayController = overlays,
            screenBoundsProvider = { getScreenBounds() }
        )
        this.inDriveReader = InDriveReader(applicationContext, tripOcr)
        this.uberReader = UberReader(ocrEngine)
        this.didiReader = DidiReader(ocrEngine)

        overlays.start()
        observeSettings()
    }

    private fun observeSettings() {
        val store = AppGraph.settingsStore
        serviceScope.launch {
            store.policyFlow.collectLatest { policy ->
                cachedPolicy = policy
            }
        }
        serviceScope.launch {
            store.licenseTokenFlow.collectLatest { token ->
                cachedLicenseStatus = if (token.isNullOrBlank()) LicenseStatus.FREE else LicenseStatus.ACTIVE
            }
        }
        serviceScope.launch {
            store.consentVersionFlow.collectLatest { version ->
                currentConsentVersion = version
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkg = event.packageName?.toString()
                if (!pkg.isNullOrBlank() && !isIgnoredPackage(pkg)) {
                    currentForegroundPackage = pkg
                    if (pkg !in SUPPORTED_PACKAGES) {
                        overlayManager?.hideAll()
                        return
                    }
                }
                triggerProcessingDebounced()
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                val pkg = event.packageName?.toString() ?: currentForegroundPackage
                if (pkg in SUPPORTED_PACKAGES) {
                    triggerProcessingDebounced()
                }
            }
        }
    }

    override fun onInterrupt() {
        overlayManager?.hideAll()
    }

    override fun onDestroy() {
        debounceJob?.cancel()
        serviceScope.cancel()
        overlayManager?.stop()
        compositeOcrEngine?.close()
        compositeOcrEngine = null
        screenshotSource = null
        ocrThrottleController = null
        overlayManager = null
        inDriveReader = null
        uberReader = null
        didiReader = null
        super.onDestroy()
    }

    private fun isIgnoredPackage(pkg: String): Boolean {
        return pkg in IGNORED_PACKAGES || pkg.contains("inputmethod") || pkg.contains("keyboard")
    }

    private fun triggerProcessingDebounced() {
        val activePkg = currentForegroundPackage
        if (activePkg.isNullOrBlank() || activePkg !in SUPPORTED_PACKAGES) return

        debounceJob?.cancel()
        debounceJob = serviceScope.launch {
            delay(300L)
            processCurrentScreen()
        }
    }

    private suspend fun processCurrentScreen() {
        if (!processingMutex.tryLock()) return
        try {
            // Refuse to read unless consentVersion >= 1
            if (currentConsentVersion < 1) {
                overlayManager?.hideAll()
                return
            }

            val activePkg = currentForegroundPackage
            if (activePkg.isNullOrBlank() || activePkg !in SUPPORTED_PACKAGES) {
                overlayManager?.hideAll()
                return
            }

            val policy = cachedPolicy
            val licenseStatus = cachedLicenseStatus
            val screenBounds = getScreenBounds()

            when (activePkg) {
                RideApp.INDRIVE.pkg -> processInDrive(policy, licenseStatus, screenBounds)
                RideApp.UBER.pkg -> processUber(policy, licenseStatus, screenBounds)
                RideApp.DIDI.pkg -> processDidi(policy, licenseStatus, screenBounds)
            }
        } finally {
            processingMutex.unlock()
        }
    }

    private suspend fun processInDrive(
        policy: Policy,
        licenseStatus: LicenseStatus,
        screenBounds: Rect
    ) {
        if (policy.appEnabled[RideApp.INDRIVE] != true) {
            overlayManager?.hideAll()
            return
        }
        val reader = inDriveReader ?: return
        val root = rootInActiveWindow ?: return

        try {
            val requests = mutableListOf<ParsedRequest>()
            val detailRequest = reader.readDetail(root)
            val mapBounds = reader.mapBounds(root)

            if (detailRequest != null) {
                requests.add(detailRequest)
            } else {
                val listRequests = reader.readList(root)
                requests.addAll(listRequests)
            }

            if (requests.isEmpty()) {
                overlayManager?.hideAll()
                return
            }

            val verdicts = mutableMapOf<String, Verdict>()
            val detector = AppGraph.opportunityDetector
            for (req in requests) {
                val verdict = RulesEngine.evaluate(req, policy)
                if (verdict != null) {
                    verdicts[req.key] = verdict
                    detector.inspect(req, verdict)
                }
            }

            overlayManager?.onScreenContentChanged(
                currentForegroundPackage = RideApp.INDRIVE.pkg,
                requests = requests,
                verdicts = verdicts,
                screenBounds = screenBounds,
                mapBounds = mapBounds,
                licenseStatus = licenseStatus,
                readerActive = true
            )
        } finally {
            @Suppress("DEPRECATION")
            root.recycle()
        }
    }

    private suspend fun processUber(
        policy: Policy,
        licenseStatus: LicenseStatus,
        screenBounds: Rect
    ) {
        if (policy.appEnabled[RideApp.UBER] != true) {
            overlayManager?.hideAll()
            return
        }
        val throttle = ocrThrottleController ?: return
        if (!throttle.shouldProcess()) return

        val captureResult = screenshotSource?.capture(RideApp.UBER.pkg)
        if (captureResult !is CaptureResult.Success) {
            return
        }
        val bitmap = captureResult.bitmap

        try {
            val region = AppGraph.ocrCalibrationManager.getRegion(RideApp.UBER, "offer_card")
            val ocrResult = compositeOcrEngine?.read(bitmap, region) ?: return
            val reader = uberReader ?: return
            val request = reader.parseOcrResult(ocrResult, screenBounds)

            if (request != null) {
                throttle.recordOcrProcessed()
                val verdict = RulesEngine.evaluate(request, policy)
                val verdicts = if (verdict != null) {
                    AppGraph.opportunityDetector.inspect(request, verdict)
                    mapOf(request.key to verdict)
                } else {
                    emptyMap()
                }

                overlayManager?.onScreenContentChanged(
                    currentForegroundPackage = RideApp.UBER.pkg,
                    requests = listOf(request),
                    verdicts = verdicts,
                    screenBounds = screenBounds,
                    mapBounds = null,
                    licenseStatus = licenseStatus,
                    readerActive = true
                )
            } else {
                overlayManager?.hideAll()
            }
        } finally {
            try {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun processDidi(
        policy: Policy,
        licenseStatus: LicenseStatus,
        screenBounds: Rect
    ) {
        if (policy.appEnabled[RideApp.DIDI] != true) {
            overlayManager?.hideAll()
            return
        }
        val throttle = ocrThrottleController ?: return
        if (!throttle.shouldProcess()) return

        val captureResult = screenshotSource?.capture(RideApp.DIDI.pkg)
        if (captureResult !is CaptureResult.Success) {
            return
        }
        val bitmap = captureResult.bitmap

        try {
            val region = AppGraph.ocrCalibrationManager.getRegion(RideApp.DIDI, "offer_card")
            val ocrResult = compositeOcrEngine?.read(bitmap, region) ?: return
            val reader = didiReader ?: return
            val request = reader.parseOcrResult(ocrResult, screenBounds)

            if (request != null) {
                throttle.recordOcrProcessed()
                val verdict = RulesEngine.evaluate(request, policy)
                val verdicts = if (verdict != null) {
                    AppGraph.opportunityDetector.inspect(request, verdict)
                    mapOf(request.key to verdict)
                } else {
                    emptyMap()
                }

                overlayManager?.onScreenContentChanged(
                    currentForegroundPackage = RideApp.DIDI.pkg,
                    requests = listOf(request),
                    verdicts = verdicts,
                    screenBounds = screenBounds,
                    mapBounds = null,
                    licenseStatus = licenseStatus,
                    readerActive = true
                )
            } else {
                overlayManager?.hideAll()
            }
        } finally {
            try {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            } catch (_: Exception) {}
        }
    }

    private fun getScreenBounds(): Rect {
        val metrics = resources.displayMetrics
        return Rect(0, 0, metrics.widthPixels, metrics.heightPixels)
    }
}
