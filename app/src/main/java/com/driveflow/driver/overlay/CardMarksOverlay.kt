package com.driveflow.driver.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.core.model.Verdict
import com.driveflow.driver.data.prefs.MarkerSize
import kotlinx.coroutines.flow.MutableStateFlow

data class ActiveMarker(
    val key: String,
    val bounds: Rect,
    val tone: Tone,
    val isListCard: Boolean,
    val lastSeenTimeMs: Long
)

/**
 * Overlay manager for drawing non-touchable card badges over ride request cards.
 */
class CardMarksOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
    private val windowType: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    }
) {
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var isAttached = false
    private val markersFlow = MutableStateFlow<List<ActiveMarker>>(emptyList())
    private val markerStore = mutableMapOf<String, ActiveMarker>()
    private var lastRefreshTimeMs = 0L

    var markerSize: MarkerSize = MarkerSize.NORMAL
    var fontScale: Float = 1.0f
    var isRtl: Boolean = true

    fun attach() {
        if (isAttached) return
        val view = ComposeView(context).apply {
            setContent {
                val markers by markersFlow.collectAsState()
                CardMarksCanvas(markers = markers)
            }
        }
        val owner = OverlayLifecycleOwner()
        owner.attachToView(view)
        this.lifecycleOwner = owner
        this.composeView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            windowManager.addView(view, params)
            isAttached = true
        } catch (e: Exception) {
            owner.destroy()
            this.composeView = null
            this.lifecycleOwner = null
            throw e
        }
    }

    fun detach() {
        if (!isAttached) return
        composeView?.let { view ->
            try {
                windowManager.removeViewImmediate(view)
            } catch (_: Exception) {}
        }
        lifecycleOwner?.destroy()
        composeView = null
        lifecycleOwner = null
        isAttached = false
        markerStore.clear()
        markersFlow.value = emptyList()
    }

    /**
     * Updates active markers from newly parsed requests and their verdicts.
     * Enforces the 250 ms maximum refresh rate and 400 ms stale removal.
     */
    fun updateRequests(
        requests: List<ParsedRequest>,
        verdicts: Map<String, Verdict>,
        screenBounds: Rect,
        currentTimeMs: Long = System.currentTimeMillis()
    ) {
        // Enforce 250 ms throttle
        if (currentTimeMs - lastRefreshTimeMs < OverlayPositionHelper.DEFAULT_THROTTLE_MS && lastRefreshTimeMs != 0L) {
            return
        }
        lastRefreshTimeMs = currentTimeMs

        val density = context.resources.displayMetrics.density
        val markerSizePx = OverlayPositionHelper.calculateScaledMarkerSizePx(markerSize, density, fontScale)

        // 1. Process new/updated requests
        for (req in requests) {
            val verdict = verdicts[req.key] ?: continue
            val anchoredBounds = OverlayPositionHelper.calculateMarkerBounds(
                cardBounds = req.bounds,
                markerSizePx = markerSizePx,
                isRtl = isRtl,
                screenBounds = screenBounds
            ) ?: continue

            markerStore[req.key] = ActiveMarker(
                key = req.key,
                bounds = anchoredBounds,
                tone = verdict.tone,
                isListCard = !req.isDetailOpen,
                lastSeenTimeMs = currentTimeMs
            )
        }

        // 2. Evict stale markers (> 400 ms)
        val iterator = markerStore.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (OverlayPositionHelper.isStale(entry.value.lastSeenTimeMs, currentTimeMs)) {
                iterator.remove()
            }
        }

        markersFlow.value = markerStore.values.toList()
    }

    fun clear() {
        markerStore.clear()
        markersFlow.value = emptyList()
    }
}

@Composable
fun CardMarksCanvas(markers: List<ActiveMarker>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (marker in markers) {
            drawMarkerBadge(marker)
        }
    }
}

private fun DrawScope.drawMarkerBadge(marker: ActiveMarker) {
    val b = marker.bounds
    val badgeSize = b.width().toFloat()
    val topLeft = Offset(b.left.toFloat(), b.top.toFloat())
    val cornerRadius = CornerRadius(badgeSize * 0.28f, badgeSize * 0.28f)

    // Dark Obsidian & Racing Theme Colors
    val toneColor = when (marker.tone) {
        Tone.GREEN -> Color(0xFF10B981)  // Emerald Green
        Tone.ORANGE -> Color(0xFFFF6B00) // Racing Orange
        Tone.RED -> Color(0xFFEF4444)    // Red
        Tone.GRAY -> Color(0xFF6B7280)   // Silver Gray
    }

    // Shadow / dark border
    drawRoundRect(
        color = Color(0x990F1115),
        topLeft = Offset(topLeft.x + 2f, topLeft.y + 2f),
        size = Size(badgeSize, badgeSize),
        cornerRadius = cornerRadius
    )

    // Base background (Graphite Gray)
    drawRoundRect(
        color = Color(0xFF181B20),
        topLeft = topLeft,
        size = Size(badgeSize, badgeSize),
        cornerRadius = cornerRadius
    )

    // Outer colored verdict border
    drawRoundRect(
        color = toneColor,
        topLeft = topLeft,
        size = Size(badgeSize, badgeSize),
        cornerRadius = cornerRadius,
        style = Stroke(width = badgeSize * 0.10f)
    )

    // Center icon
    val center = Offset(topLeft.x + badgeSize / 2f, topLeft.y + badgeSize / 2f)
    val iconRadius = badgeSize * 0.24f
    val strokeWidth = (badgeSize * 0.12f).coerceAtLeast(2f)

    if (marker.tone == Tone.GREEN && marker.isListCard) {
        // Bright open-eye icon ("open it and see the route") in Emerald Green
        drawOpenEyeIcon(center, iconRadius, strokeWidth, Color(0xFF10B981))
    } else {
        when (marker.tone) {
            Tone.GREEN -> drawCheckmark(center, iconRadius, strokeWidth, toneColor)
            Tone.ORANGE -> drawClockIcon(center, iconRadius, strokeWidth, toneColor)
            Tone.RED -> drawCrossIcon(center, iconRadius, strokeWidth, toneColor)
            Tone.GRAY -> drawDotsIcon(center, iconRadius, toneColor)
        }
    }
}

private fun DrawScope.drawOpenEyeIcon(center: Offset, radius: Float, strokeWidth: Float, color: Color) {
    val eyeWidth = radius * 1.5f
    val eyeHeight = radius * 0.9f
    val path = Path().apply {
        moveTo(center.x - eyeWidth, center.y)
        quadraticTo(center.x, center.y - eyeHeight, center.x + eyeWidth, center.y)
        quadraticTo(center.x, center.y + eyeHeight, center.x - eyeWidth, center.y)
        close()
    }
    drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
    drawCircle(color = color, radius = radius * 0.45f, center = center)
}

private fun DrawScope.drawCheckmark(center: Offset, radius: Float, strokeWidth: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x - radius * 0.8f, center.y - radius * 0.1f)
        lineTo(center.x - radius * 0.2f, center.y + radius * 0.6f)
        lineTo(center.x + radius * 0.8f, center.y - radius * 0.6f)
    }
    drawPath(path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

private fun DrawScope.drawClockIcon(center: Offset, radius: Float, strokeWidth: Float, color: Color) {
    drawCircle(color = color, radius = radius, center = center, style = Stroke(width = strokeWidth))
    drawLine(
        color = color,
        start = center,
        end = Offset(center.x, center.y - radius * 0.55f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = center,
        end = Offset(center.x + radius * 0.45f, center.y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawCrossIcon(center: Offset, radius: Float, strokeWidth: Float, color: Color) {
    val d = radius * 0.65f
    drawLine(
        color = color,
        start = Offset(center.x - d, center.y - d),
        end = Offset(center.x + d, center.y + d),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(center.x + d, center.y - d),
        end = Offset(center.x - d, center.y + d),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawDotsIcon(center: Offset, radius: Float, color: Color) {
    val dotRadius = radius * 0.25f
    val spacing = radius * 0.7f
    drawCircle(color = color, radius = dotRadius, center = Offset(center.x - spacing, center.y))
    drawCircle(color = color, radius = dotRadius, center = center)
    drawCircle(color = color, radius = dotRadius, center = Offset(center.x + spacing, center.y))
}
