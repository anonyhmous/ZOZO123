package com.driveflow.driver.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Secondary OCR engine using Tesseract4Android for Arabic text recognition.
 * Manages traineddata file copying from assets to filesDir/tessdata on first run.
 */
class TesseractOcrEngine(
    private val context: Context,
    private val language: String = "ara+eng"
) : OcrEngine {
    private var tessBaseAPI: TessBaseAPI? = null
    private var isInitialized = false

    init {
        ensureTessDataReady()
    }

    private fun ensureTessDataReady() {
        try {
            val tessDir = File(context.filesDir, "tessdata")
            if (!tessDir.exists()) {
                tessDir.mkdirs()
            }
            listOf("ara.traineddata", "eng.traineddata").forEach { filename ->
                val destFile = File(tessDir, filename)
                if (!destFile.exists() || destFile.length() == 0L) {
                    try {
                        context.assets.open("tessdata/$filename").use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (_: Exception) {
                        // Assets may be placeholders or missing in testing
                    }
                }
            }

            // Attempt initialization
            val api = TessBaseAPI()
            val initOk = api.init(context.filesDir.absolutePath, language)
            if (initOk) {
                tessBaseAPI = api
                isInitialized = true
            } else {
                api.recycle()
            }
        } catch (_: Exception) {
            isInitialized = false
        }
    }

    override suspend fun read(bitmap: Bitmap?, region: RectF): OcrResult = withContext(OcrEngine.ocrDispatcher) {
        if (bitmap == null || !isInitialized || tessBaseAPI == null) {
            return@withContext OcrResult(emptyList())
        }
        val cropInfo = OcrEngine.prepareCrop(bitmap, region)
        try {
            val api = tessBaseAPI ?: return@withContext OcrResult(emptyList())
            api.setImage(cropInfo.bitmap)
            val utf8Text = api.utF8Text ?: ""
            val confidenceScore = (api.meanConfidence().coerceIn(0, 100)) / 100f
            val lines = mutableListOf<OcrLine>()

            val textLines = utf8Text.split("\n").filter { it.isNotBlank() }
            val totalLines = textLines.size.coerceAtLeast(1)
            textLines.forEachIndexed { idx, lineText ->
                val relativeTop = (idx.toFloat() / totalLines) * (bitmap.height * region.height())
                val relativeBottom = ((idx + 1).toFloat() / totalLines) * (bitmap.height * region.height())
                val origLeft = cropInfo.cropLeftPx
                val origTop = cropInfo.cropTopPx + relativeTop.toInt()
                val origRight = cropInfo.cropLeftPx + (bitmap.width * region.width()).toInt()
                val origBottom = cropInfo.cropTopPx + relativeBottom.toInt()

                lines.add(
                    OcrLine(
                        text = lineText.trim(),
                        boundsPx = Rect(origLeft, origTop, origRight, origBottom),
                        confidence = confidenceScore
                    )
                )
            }
            api.clear()
            OcrResult(lines)
        } catch (_: Exception) {
            OcrResult(emptyList())
        } finally {
            if (cropInfo.bitmap != bitmap && !cropInfo.bitmap.isRecycled) {
                cropInfo.bitmap.recycle()
            }
        }
    }

    override fun close() {
        try {
            tessBaseAPI?.recycle()
            tessBaseAPI = null
            isInitialized = false
        } catch (_: Exception) {}
    }
}
