package com.driveflow.driver.ocr

import android.graphics.Bitmap
import android.graphics.RectF
import kotlinx.coroutines.asCoroutineDispatcher
import java.io.Closeable
import java.util.concurrent.Executors

/**
 * Common interface for OCR recognition engines.
 */
interface OcrEngine : Closeable {
    /**
     * Reads text from a specified relative fraction region of the bitmap.
     * @param bitmap The full screen or container bitmap (optional for testing/fake engines).
     * @param region Fractional bounds (0.0f..1.0f) to crop before running recognition.
     */
    suspend fun read(bitmap: Bitmap?, region: RectF): OcrResult

    override fun close() {}

    companion object {
        /**
         * Dedicated single-thread dispatcher for OCR operations to prevent blocking
         * the main thread or competing with other background jobs.
         */
        val ocrDispatcher = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "DriveFlow-Ocr-Worker").apply {
                isDaemon = true
                priority = Thread.NORM_PRIORITY - 1
            }
        }.asCoroutineDispatcher()

        /**
         * Crops and optionally upscales a region of a bitmap before passing to OCR.
         * Upscales crops smaller than 300x150 by 2x for improved recognition accuracy on digits.
         */
        fun prepareCrop(bitmap: Bitmap, region: RectF): CropInfo {
            val bmpW = bitmap.width
            val bmpH = bitmap.height
            val leftPx = (region.left.coerceIn(0f, 1f) * bmpW).toInt().coerceIn(0, bmpW - 1)
            val topPx = (region.top.coerceIn(0f, 1f) * bmpH).toInt().coerceIn(0, bmpH - 1)
            val rightPx = (region.right.coerceIn(0f, 1f) * bmpW).toInt().coerceIn(leftPx + 1, bmpW)
            val bottomPx = (region.bottom.coerceIn(0f, 1f) * bmpH).toInt().coerceIn(topPx + 1, bmpH)

            val cropW = (rightPx - leftPx).coerceAtLeast(1)
            val cropH = (bottomPx - topPx).coerceAtLeast(1)

            val rawCropped = Bitmap.createBitmap(bitmap, leftPx, topPx, cropW, cropH)
            val shouldUpscale = cropW < 300 || cropH < 150
            val scaleFactor = if (shouldUpscale) 2.0f else 1.0f

            val finalBitmap = if (shouldUpscale) {
                val scaled = Bitmap.createScaledBitmap(rawCropped, cropW * 2, cropH * 2, true)
                if (scaled != rawCropped) {
                    rawCropped.recycle()
                }
                scaled
            } else {
                rawCropped
            }

            return CropInfo(
                bitmap = finalBitmap,
                cropLeftPx = leftPx,
                cropTopPx = topPx,
                scaleFactor = scaleFactor
            )
        }
    }
}

data class CropInfo(
    val bitmap: Bitmap,
    val cropLeftPx: Int,
    val cropTopPx: Int,
    val scaleFactor: Float
)
