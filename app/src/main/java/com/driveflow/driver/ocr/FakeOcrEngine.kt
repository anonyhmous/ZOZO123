package com.driveflow.driver.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF

/**
 * Testable, debug-only Fake OCR engine that provides canned lines without needing hardware or models.
 */
class FakeOcrEngine(
    private var defaultLines: List<OcrLine> = emptyList()
) : OcrEngine {
    private val regionHandlers = mutableMapOf<String, List<OcrLine>>()

    fun setCannedText(lines: List<String>, confidence: Float = 0.95f) {
        defaultLines = lines.mapIndexed { idx, text ->
            OcrLine(
                text = text,
                boundsPx = Rect(10, 100 + (idx * 40), 500, 140 + (idx * 40)),
                confidence = confidence
            )
        }
    }

    fun setCannedLines(lines: List<OcrLine>) {
        defaultLines = lines
    }

    fun setRegionCanned(regionKey: String, lines: List<OcrLine>) {
        regionHandlers[regionKey] = lines
    }

    override suspend fun read(bitmap: Bitmap?, region: RectF): OcrResult {
        // Return region specific match if configured, otherwise default lines
        for ((key, canned) in regionHandlers) {
            // Check if region closely matches or key matches
            if (region.left.toString().startsWith(key) || region.top.toString().startsWith(key)) {
                return OcrResult(canned)
            }
        }
        return OcrResult(defaultLines)
    }

    override fun close() {
        // No-op for fake
    }
}
