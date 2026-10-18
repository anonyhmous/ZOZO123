package com.driveflow.driver.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Primary OCR engine using bundled ML Kit Text Recognition (Latin model).
 * Optimally suited for digits, numbers, currencies (EGP), and Latin labels.
 */
class MlKitOcrEngine : OcrEngine {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    override suspend fun read(bitmap: Bitmap?, region: RectF): OcrResult = withContext(OcrEngine.ocrDispatcher) {
        if (bitmap == null) return@withContext OcrResult(emptyList())

        val cropInfo = OcrEngine.prepareCrop(bitmap, region)
        val image = InputImage.fromBitmap(cropInfo.bitmap, 0)

        try {
            val result = suspendCancellableCoroutine { continuation ->
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val lines = mutableListOf<OcrLine>()
                        for (block in visionText.textBlocks) {
                            for (line in block.lines) {
                                val box = line.boundingBox ?: Rect()
                                // Transform coordinates back to the original full bitmap coordinate space
                                val origLeft = (box.left / cropInfo.scaleFactor).toInt() + cropInfo.cropLeftPx
                                val origTop = (box.top / cropInfo.scaleFactor).toInt() + cropInfo.cropTopPx
                                val origRight = (box.right / cropInfo.scaleFactor).toInt() + cropInfo.cropLeftPx
                                val origBottom = (box.bottom / cropInfo.scaleFactor).toInt() + cropInfo.cropTopPx
                                val lineConfidence = line.confidence ?: 0.90f
                                lines.add(
                                    OcrLine(
                                        text = line.text,
                                        boundsPx = Rect(origLeft, origTop, origRight, origBottom),
                                        confidence = lineConfidence
                                    )
                                )
                            }
                        }
                        if (continuation.isActive) {
                            continuation.resume(OcrResult(lines))
                        }
                    }
                    .addOnFailureListener { _ ->
                        if (continuation.isActive) {
                            continuation.resume(OcrResult(emptyList()))
                        }
                    }
            }
            result
        } finally {
            if (cropInfo.bitmap != bitmap && !cropInfo.bitmap.isRecycled) {
                cropInfo.bitmap.recycle()
            }
        }
    }

    override fun close() {
        try {
            recognizer.close()
        } catch (_: Exception) {}
    }
}
