package com.driveflow.driver.ocr

import android.graphics.Rect
import kotlinx.serialization.Serializable

@Serializable
data class OcrLine(
    val text: String,
    val boundsPx: @Serializable(with = RectSerializer::class) Rect = Rect(),
    val confidence: Float = 1.0f
)

@Serializable
data class OcrResult(
    val lines: List<OcrLine> = emptyList()
) {
    val fullText: String get() = lines.joinToString("\n") { it.text }
    val meanConfidence: Float get() = if (lines.isEmpty()) 0f else lines.map { it.confidence }.average().toFloat()
}

/**
 * Custom serializer for Android Rect to support kotlinx.serialization without platform issues.
 */
object RectSerializer : kotlinx.serialization.KSerializer<Rect> {
    @Serializable
    private data class RectSurrogate(val left: Int, val top: Int, val right: Int, val bottom: Int)

    override val descriptor = kotlinx.serialization.descriptors.SerialDescriptor("android.graphics.Rect", RectSurrogate.serializer().descriptor)

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Rect) {
        encoder.encodeSerializableValue(RectSurrogate.serializer(), RectSurrogate(value.left, value.top, value.right, value.bottom))
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Rect {
        val surrogate = decoder.decodeSerializableValue(RectSurrogate.serializer())
        return Rect(surrogate.left, surrogate.top, surrogate.right, surrogate.bottom)
    }
}
