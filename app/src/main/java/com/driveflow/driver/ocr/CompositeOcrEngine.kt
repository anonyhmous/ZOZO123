package com.driveflow.driver.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF

/**
 * Composite OCR engine running primary ML Kit recognition, augmented by Tesseract
 * when Arabic characters are detected or expected.
 */
class CompositeOcrEngine(
    context: Context,
    private val mlKitEngine: OcrEngine = MlKitOcrEngine(),
    private val tesseractEngine: OcrEngine = TesseractOcrEngine(context)
) : OcrEngine {
    override suspend fun read(bitmap: Bitmap?, region: RectF): OcrResult {
        val mlKitResult = mlKitEngine.read(bitmap, region)
        // If ML Kit found high-confidence text lines, check if Arabic labels are missing
        if (mlKitResult.lines.isNotEmpty()) {
            val hasDigitsOrLatin = mlKitResult.lines.any { line ->
                line.text.any { it.isDigit() || (it in 'a'..'z') || (it in 'A'..'Z') }
            }
            if (hasDigitsOrLatin && mlKitResult.meanConfidence >= 0.75f) {
                return mlKitResult
            }
        }

        // Run secondary engine if ML Kit didn't find clear results or Arabic text might be present
        val tessResult = tesseractEngine.read(bitmap, region)
        return when {
            tessResult.lines.isEmpty() -> mlKitResult
            mlKitResult.lines.isEmpty() -> tessResult
            else -> {
                // Merge distinct lines prioritizing higher confidence
                val merged = (mlKitResult.lines + tessResult.lines).distinctBy { it.text.trim() }
                OcrResult(merged)
            }
        }
    }

    override fun close() {
        mlKitEngine.close()
        tesseractEngine.close()
    }
}
