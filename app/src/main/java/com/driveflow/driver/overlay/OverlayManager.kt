package com.driveflow.driver.overlay

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.os.Build
import android.provider.Settings
import android.view.Display
import android.view.WindowManager
import com.driveflow.driver.MainActivity
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.model.LicenseStatus
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.Verdict
import com.driveflow.driver.data.prefs.AlertMode
import com.driveflow.driver.data.prefs.BarPosition
import com.driveflow.driver.data.prefs.MarkerSize
import com.driveflow.driver.data.prefs.SettingsStore
import com.driveflow.driver.engine.OpportunityEvent
import com.driveflow.driver.reader.indrive.OverlayVisibilityController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Main coordinator for all floating overlay windows:
 * - Card markers overlay (non-touchable badges anchored to request cards)
 * - Decision bar overlay (touchable metrics bar with dual calculation mode buttons, grip, Close, and Report)
 * - Opportunity alert manager (sound + vibration)
 * - OverlayVisibilityController implementation for inDrive screenshot capture
 * - Global safety kill wrapper on exceptions
 */
class OverlayManager(
    private val context: Context,
    private val isAccessibilityService: Boolean = false,
    private val settingsStore: SettingsStore = AppGraph.settingsStore
) : OverlayVisibilityController {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var windowContext: Context = context
    private var cardMarksOverlay: CardMarksOverlay? = null
    private var decisionBarOverlay: DecisionBarOverlay? = null
    val opportunityAlertManager: OpportunityAlertManager = OpportunityAlertManager(context)

    private var activeJob: Job? = null
    private var isStarted = false
    private var isTemporarilyHidden = false

    private var currentMarkerSize = MarkerSize.NORMAL
    private var currentOpacityPercent = 85
    private var currentBarPosition = BarPosition(isAuto = true)
    private var currentAlertEnabled = true
    private var currentAlertMode = AlertMode.TONE_VIBRATE
    private var currentLanguage = "ar"
    private var currentConsentVersion = 0

    companion object {
        const val SELF_PACKAGE = "com.driveflow.driver"
    }

    /**
     * Initializes window context and WindowManager.
     */
    fun start() {
        if (isStarted) return
        try {
            val windowType = resolveWindowType()
            val wm = resolveWindowManager(windowType)
            this.windowManager = wm

            val cardMarks = CardMarksOverlay(windowContext, wm, windowType)
            val decisionBar = DecisionBarOverlay(
                context = windowContext,
                windowManager = wm,
                windowType = windowType,
                onReportClicked = { request, verdict ->
                    handleReport(request, verdict)
                },
                onPositionSaved = { pos ->
                    scope.launch {
                        settingsStore.setBarPosition(pos)
                    }
                }
            )

            this.cardMarksOverlay = cardMarks
            this.decisionBarOverlay = decisionBar

            cardMarks.attach()
            decisionBar.attach()
            isStarted = true

            startPreferencesCollectors()
            startOpportunityCollector()
        } catch (e: Exception) {
            handleOverlayCrash("start_failed", e)
        }
    }

    fun stop() {
        if (!isStarted) return
        activeJob?.cancel()
        activeJob = null

        safeExecution {
            cardMarksOverlay?.detach()
            decisionBarOverlay?.detach()
        }
        cardMarksOverlay = null
        decisionBarOverlay = null
        windowManager = null
        isStarted = false
    }

    /**
     * Updates active screen content from the reader service.
     */
    fun onScreenContentChanged(
        currentForegroundPackage: String?,
        requests: List<ParsedRequest>,
        verdicts: Map<String, Verdict>,
        screenBounds: Rect,
        mapBounds: Rect?,
        licenseStatus: LicenseStatus,
        readerActive: Boolean,
        headingHomeInfo: String? = null
    ) {
        if (!isStarted || isTemporarilyHidden) return

        // 1. Safety Rule: Never draw over our own app
        if (currentForegroundPackage == SELF_PACKAGE) {
            hideAll()
            return
        }

        // 2. Safety Rule: Must be a supported ride app
        val isRideApp = currentForegroundPackage in setOf(
            RideApp.INDRIVE.pkg,
            RideApp.UBER.pkg,
            RideApp.DIDI.pkg
        )
        if (!isRideApp || !readerActive) {
            hideAll()
            return
        }

        // 3. Safety Rule: Consent gate (reader must refuse to run unless consentVersion >= 1)
        if (currentConsentVersion < 1) {
            hideAll()
            return
        }

        safeExecution {
            val isRtl = currentLanguage == "ar"
            val fontScale = context.resources.configuration.fontScale

            // 3. Update Card Marks
            cardMarksOverlay?.let { marks ->
                marks.markerSize = currentMarkerSize
                marks.fontScale = fontScale
                marks.isRtl = isRtl
                marks.updateRequests(requests, verdicts, screenBounds)
            }

            // 4. Update Decision Bar (for detail mode)
            val detailRequest = requests.firstOrNull { it.isDetailOpen }
            if (detailRequest != null) {
                val detailVerdict = verdicts[detailRequest.key]
                if (detailVerdict != null) {
                    decisionBarOverlay?.let { bar ->
                        bar.barPosition = currentBarPosition
                        bar.isRtl = isRtl
                        bar.show(
                            request = detailRequest,
                            verdict = detailVerdict,
                            opacityPercent = currentOpacityPercent,
                            screenBounds = screenBounds,
                            mapBounds = mapBounds,
                            headingHomeInfo = headingHomeInfo
                        )
                    }
                } else {
                    decisionBarOverlay?.hide()
                }
            } else {
                decisionBarOverlay?.hide()
            }
        }
    }

    /**
     * Temporarily hides overlay windows during inDrive map screenshot capture.
     */
    override fun hideTemporarily(durationMs: Long) {
        if (!isStarted) return
        isTemporarilyHidden = true
        safeExecution {
            cardMarksOverlay?.clear()
            decisionBarOverlay?.hide()
        }
        scope.launch {
            delay(durationMs)
            isTemporarilyHidden = false
        }
    }

    fun hideAll() {
        safeExecution {
            cardMarksOverlay?.clear()
            decisionBarOverlay?.hide()
        }
    }

    private fun resolveWindowType(): Int {
        val canDraw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

        return if (canDraw) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
        } else if (isAccessibilityService) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
        }
    }

    private fun resolveWindowManager(windowType: Int): WindowManager {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
                val defaultDisplay = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
                if (defaultDisplay != null) {
                    val displayContext = context.createDisplayContext(defaultDisplay)
                    val winContext = displayContext.createWindowContext(windowType, null)
                    this.windowContext = winContext
                    return winContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                }
            } catch (_: Exception) {}
        }
        this.windowContext = context
        return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private fun startPreferencesCollectors() {
        scope.launch {
            settingsStore.markerSizeFlow.collectLatest { size ->
                currentMarkerSize = size
            }
        }
        scope.launch {
            settingsStore.barOpacityPercentFlow.collectLatest { opacity ->
                currentOpacityPercent = opacity
            }
        }
        scope.launch {
            settingsStore.barPositionFlow.collectLatest { pos ->
                currentBarPosition = pos
            }
        }
        scope.launch {
            settingsStore.alertEnabledFlow.collectLatest { enabled ->
                currentAlertEnabled = enabled
            }
        }
        scope.launch {
            settingsStore.alertModeFlow.collectLatest { mode ->
                currentAlertMode = mode
            }
        }
        scope.launch {
            settingsStore.languageFlow.collectLatest { lang ->
                currentLanguage = lang
            }
        }
        scope.launch {
            settingsStore.consentVersionFlow.collectLatest { version ->
                currentConsentVersion = version
            }
        }
    }

    private fun startOpportunityCollector() {
        val detector = AppGraph.opportunityDetector
        activeJob = scope.launch {
            detector.opportunityEvents.collectLatest { event ->
                if (currentAlertEnabled) {
                    opportunityAlertManager.fireAlert(event, currentAlertMode)
                }
            }
        }
    }

    private fun handleReport(request: ParsedRequest, verdict: Verdict) {
        val fareText = request.fare?.let { "${it.toInt()} ${request.currency ?: "EGP"}" } ?: "—"
        val distText = verdict.totalKm?.let { "${String.format(java.util.Locale.US, "%.1f", it)} km" } ?: "—"
        val message = "طلب فحص تسعير: السعر = $fareText | المسافة = $distText | التقييم = ${verdict.reasonCode} | اللون = ${verdict.tone}"
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "chat")
            putExtra("prefill_message", message)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private inline fun safeExecution(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            handleOverlayCrash("runtime_error", e)
        }
    }

    private fun handleOverlayCrash(tag: String, e: Exception) {
        try {
            cardMarksOverlay?.detach()
            decisionBarOverlay?.detach()
        } catch (_: Exception) {}
        isStarted = false
        scope.launch(Dispatchers.IO) {
            settingsStore.incrementDiagnosticCounter("overlay_error")
        }
    }
}
