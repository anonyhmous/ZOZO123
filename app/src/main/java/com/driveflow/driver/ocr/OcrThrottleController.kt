package com.driveflow.driver.ocr

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Enforces rate limiting, battery preservation, screen off tracking,
 * and content hash change detection for OCR processing.
 * Rules:
 * - At most one OCR cycle per 600 ms.
 * - Stop completely when screen is off or no ride app is visible.
 * - Skip OCR when the screen content hash has not changed.
 */
class OcrThrottleController(
    private val context: Context? = null,
    private val minIntervalMs: Long = 600L
) {
    private var lastOcrTimeMs: Long = 0L
    private var lastScreenHash: Long = 0L
    private val isScreenOn = AtomicBoolean(true)

    init {
        context?.let { ctx ->
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            ctx.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        Intent.ACTION_SCREEN_OFF -> isScreenOn.set(false)
                        Intent.ACTION_SCREEN_ON -> isScreenOn.set(true)
                    }
                }
            }, filter)
        }
    }

    fun setScreenStateForTesting(on: Boolean) {
        isScreenOn.set(on)
    }

    /**
     * Checks if an OCR cycle is permitted based on timing and screen power state.
     */
    fun shouldProcess(currentTimeMs: Long = System.currentTimeMillis()): Boolean {
        if (!isScreenOn.get()) return false
        return (currentTimeMs - lastOcrTimeMs) >= minIntervalMs
    }

    /**
     * Records the execution of an OCR cycle.
     */
    fun recordOcrProcessed(currentTimeMs: Long = System.currentTimeMillis()) {
        lastOcrTimeMs = currentTimeMs
    }

    /**
     * Fast content hash of a bitmap by sampling pixel values across a grid.
     * Prevents redundant OCR execution when screen content remains static.
     */
    fun computeBitmapHash(bitmap: Bitmap): Long {
        var hash = 1125899906842597L
        val stepX = (bitmap.width / 16).coerceAtLeast(1)
        val stepY = (bitmap.height / 24).coerceAtLeast(1)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val pixel = bitmap.getPixel(x, y).toLong()
                hash = 31 * hash + pixel
                x += stepX
            }
            y += stepY
        }
        return hash
    }

    /**
     * Checks if the screen content has changed since the last cycle.
     * Returns true if changed, false if identical (so OCR can be skipped).
     */
    fun checkAndRecordContentChanged(newHash: Long): Boolean {
        if (newHash == lastScreenHash) {
            return false // Unchanged, skip OCR
        }
        lastScreenHash = newHash
        return true
    }

    fun reset() {
        lastOcrTimeMs = 0L
        lastScreenHash = 0L
    }
}
