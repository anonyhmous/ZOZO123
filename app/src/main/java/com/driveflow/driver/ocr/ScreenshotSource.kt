package com.driveflow.driver.ocr

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.view.Display
import androidx.annotation.RequiresApi
import com.driveflow.driver.core.model.RideApp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

sealed class CaptureResult {
    data class Success(val bitmap: Bitmap) : CaptureResult()
    data class Failure(val reasonCode: String) : CaptureResult()
}

/**
 * ScreenshotSource wraps AccessibilityService.takeScreenshot (API 30+).
 * Converts HardwareBuffer into an in-memory ARGB_8888 Bitmap without disk writes.
 * Enforces rate limiting, backoff on throttle errors, and active ride app validation.
 */
class ScreenshotSource(
    private val serviceProvider: () -> AccessibilityService?
) {
    private val captureMutex = Mutex()
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "DriveFlow-Screenshot-Thread").apply { isDaemon = true }
    }
    private var lastCaptureTimeMs = 0L
    private val isCapturing = AtomicBoolean(false)

    /**
     * Checks if a supported ride app window is currently in the foreground.
     */
    fun isSupportedRideAppVisible(activePackageName: String?): Boolean {
        if (activePackageName.isNullOrBlank()) return false
        return activePackageName == RideApp.INDRIVE.pkg ||
                activePackageName == RideApp.UBER.pkg ||
                activePackageName == RideApp.DIDI.pkg
    }

    /**
     * Captures a screenshot from the active accessibility service.
     * Guaranteed: Max one capture in flight, backoff on interval errors, never saved to disk.
     */
    suspend fun capture(activePackageName: String? = null): CaptureResult {
        if (activePackageName != null && !isSupportedRideAppVisible(activePackageName)) {
            return CaptureResult.Failure("capture_refused")
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return CaptureResult.Failure("capture_refused")
        }
        val service = serviceProvider() ?: return CaptureResult.Failure("capture_refused")

        captureMutex.withLock {
            if (!isCapturing.compareAndSet(false, true)) {
                return CaptureResult.Failure("capture_refused")
            }
            try {
                // Rate limiting: at least 350ms since last capture
                val now = System.currentTimeMillis()
                val elapsed = now - lastCaptureTimeMs
                if (elapsed < 350) {
                    delay(350 - elapsed)
                }

                var attemptResult = takeScreenshotInternal(service)
                // If throttled by system (ERROR_TAKE_SCREENSHOT_INTERVAL_REQUEST = 3), back off and retry after 400ms
                if (attemptResult is CaptureResult.Failure && attemptResult.reasonCode == "interval_throttled") {
                    delay(400)
                    attemptResult = takeScreenshotInternal(service)
                }
                if (attemptResult is CaptureResult.Success) {
                    lastCaptureTimeMs = System.currentTimeMillis()
                }
                return attemptResult
            } finally {
                isCapturing.set(false)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun takeScreenshotInternal(service: AccessibilityService): CaptureResult {
        val deferred = CompletableDeferred<CaptureResult>()
        try {
            service.takeScreenshot(
                Display.DEFAULT_DISPLAY,
                executor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                        try {
                            val hardwareBuffer = result.hardwareBuffer
                            val colorSpace = result.colorSpace
                            val hwBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                            val argbBitmap = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                            hardwareBuffer.close()
                            if (argbBitmap != null) {
                                deferred.complete(CaptureResult.Success(argbBitmap))
                            } else {
                                deferred.complete(CaptureResult.Failure("capture_refused"))
                            }
                        } catch (e: Exception) {
                            deferred.complete(CaptureResult.Failure("capture_refused"))
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        // ErrorCode 3 = ERROR_TAKE_SCREENSHOT_INTERVAL_REQUEST
                        if (errorCode == 3) {
                            deferred.complete(CaptureResult.Failure("interval_throttled"))
                        } else {
                            deferred.complete(CaptureResult.Failure("capture_refused"))
                        }
                    }
                }
            )
        } catch (e: Exception) {
            deferred.complete(CaptureResult.Failure("capture_refused"))
        }
        return deferred.await()
    }
}
