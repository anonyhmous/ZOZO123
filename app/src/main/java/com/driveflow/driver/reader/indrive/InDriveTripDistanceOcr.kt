package com.driveflow.driver.reader.indrive

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.model.TripDistanceOcr
import com.driveflow.driver.core.model.TripKmResult
import com.driveflow.driver.core.util.TextParsers
import com.driveflow.driver.ocr.CaptureResult
import com.driveflow.driver.ocr.OcrEngine
import com.driveflow.driver.ocr.OcrResult
import com.driveflow.driver.ocr.ScreenshotSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Controller callback to ask the overlay layer to hide the floating bar briefly during map OCR capture.
 */
interface OverlayVisibilityController {
    fun hideTemporarily(durationMs: Long)
}

/**
 * inDrive Trip Distance OCR extractor.
 * Reads the distance badge drawn on the inDrive route map.
 */
class InDriveTripDistanceOcr(
    private val screenshotSource: ScreenshotSource? = null,
    private val ocrEngine: OcrEngine? = null,
    private val overlayController: OverlayVisibilityController? = null,
    private val screenBoundsProvider: () -> Rect = { Rect(0, 0, 1080, 2400) }
) : TripDistanceOcr {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        val DISTANCE_BOX_REGEX = Regex(
            """(\d+(?:[.,]\d+)?)\s*(?:كم|km|متر|m|mi|ميل)""",
            RegexOption.IGNORE_CASE
        )
    }

    override suspend fun readTripKm(mapBounds: Rect): TripKmResult {
        // If mapBounds is empty or invalid, report missing box
        if (mapBounds.width() <= 10 || mapBounds.height() <= 10) {
            recordFailure("trip_box_missing")
            return TripKmResult(null, "trip_box_missing")
        }

        // 1. Temporarily hide our overlay bar to prevent covering the route box
        overlayController?.hideTemporarily(150)
        delay(30) // Wait brief instant for overlay view hierarchy to hide

        // 2. Capture screenshot
        val captureResult = screenshotSource?.capture()
        if (captureResult == null || captureResult !is CaptureResult.Success) {
            recordFailure("capture_refused")
            return TripKmResult(null, "capture_refused")
        }
        val bitmap = captureResult.bitmap

        try {
            val screen = screenBoundsProvider()
            val regionFraction = RectF(
                mapBounds.left.toFloat() / screen.width().coerceAtLeast(1),
                mapBounds.top.toFloat() / screen.height().coerceAtLeast(1),
                mapBounds.right.toFloat() / screen.width().coerceAtLeast(1),
                mapBounds.bottom.toFloat() / screen.height().coerceAtLeast(1)
            )

            // 3. OCR on map region
            val engine = ocrEngine ?: return parseFromBitmap(bitmap, regionFraction, mapBounds)
            val ocrResult = engine.read(bitmap, regionFraction)
            return parseOcrResult(ocrResult, mapBounds, screen)
        } finally {
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    /**
     * Parses the distance box from OCR results over the map bounds.
     */
    fun parseOcrResult(ocrResult: OcrResult, mapBounds: Rect, screenBounds: Rect): TripKmResult {
        val lines = ocrResult.lines
        if (lines.isEmpty()) {
            recordFailure("trip_box_missing")
            return TripKmResult(null, "trip_box_missing")
        }

        // Look for the distance badge line
        val matchingLine = lines.firstOrNull { line ->
            val normalized = TextParsers.normalizeDigits(line.text)
            DISTANCE_BOX_REGEX.containsMatchIn(normalized)
        }
        if (matchingLine == null) {
            recordFailure("trip_box_missing")
            return TripKmResult(null, "trip_box_missing")
        }

        // Check if digits are unclear (confidence < 0.60)
        if (matchingLine.confidence < 0.60f) {
            recordFailure("digits_unclear")
            return TripKmResult(null, "digits_unclear")
        }

        val lineBounds = matchingLine.boundsPx
        val screenW = if (screenBounds.right > 0) screenBounds.right else 1080
        val screenH = if (screenBounds.bottom > 0) screenBounds.bottom else 2400
        val mapBottom = if (mapBounds.bottom > 0) mapBounds.bottom else 1200

        // Check if box is clipped by screen edges
        val edgeTolerance = 8
        if (lineBounds.left <= edgeTolerance ||
            lineBounds.top <= edgeTolerance ||
            lineBounds.right >= (screenW - edgeTolerance) ||
            lineBounds.bottom >= (screenH - edgeTolerance)
        ) {
            recordFailure("trip_km_clipped")
            return TripKmResult(null, "trip_km_clipped")
        }

        // Check if box touches or exceeds the bottom map boundary (covered by sheet/overlay)
        if (lineBounds.bottom >= (mapBottom - 4)) {
            recordFailure("trip_km_covered")
            return TripKmResult(null, "trip_km_covered")
        }

        // Parse distance value
        val parsed = TextParsers.parseDistance(matchingLine.text)
        val km = parsed?.km
        if (km == null || km <= 0.0) {
            recordFailure("digits_unclear")
            return TripKmResult(null, "digits_unclear")
        }

        return TripKmResult(value = km, failure = null)
    }

    private fun parseFromBitmap(bitmap: Bitmap, regionFraction: RectF, mapBounds: Rect): TripKmResult {
        recordFailure("trip_box_missing")
        return TripKmResult(null, "trip_box_missing")
    }

    private fun recordFailure(reason: String) {
        scope.launch {
            try {
                if (AppGraph.isInitialized) {
                    AppGraph.settingsStore.incrementDiagnosticCounter("ocr_fail:$reason")
                    AppGraph.settingsStore.incrementDiagnosticCounter("ocr_fail:indrive:$reason")
                }
            } catch (_: Exception) {}
        }
    }
}
