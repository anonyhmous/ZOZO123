package com.driveflow.driver.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.RoadState
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.core.model.Verdict
import com.driveflow.driver.data.prefs.BarPosition
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale
import kotlin.math.roundToInt

enum class OverlayTripCalculationMode {
    FULL_TRIP, // Includes driver pickup distance + trip destination
    BASE_TRIP  // Main trip only, ignoring passenger pickup distance
}

data class DecisionBarState(
    val request: ParsedRequest? = null,
    val verdict: Verdict? = null,
    val opacityPercent: Int = 85,
    val isRtl: Boolean = true,
    val headingHomeInfo: String? = null,
    val isVisible: Boolean = false,
    val calcMode: OverlayTripCalculationMode = OverlayTripCalculationMode.FULL_TRIP
)

/**
 * Overlay hosting the redesigned DecisionBar matching the Dark Obsidian & Racing Orange theme.
 * Features dual calculation buttons:
 * 1. [🔄 حساب الرحلة كاملة]
 * 2. [⚡ حساب الرحلة الأساسية]
 */
class DecisionBarOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
    private val windowType: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
        @Suppress("DEPRECATION")
        WindowManager.LayoutParams.TYPE_PHONE
    },
    private val onReportClicked: (ParsedRequest, Verdict) -> Unit = { _, _ -> },
    private val onPositionSaved: (BarPosition) -> Unit = {}
) {
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var windowParams: WindowManager.LayoutParams? = null
    private var isAttached = false
    private val stateFlow = MutableStateFlow(DecisionBarState())
    private var dismissedRequestKey: String? = null

    var barPosition: BarPosition = BarPosition(isAuto = true)
    var isRtl: Boolean = true

    fun attach() {
        if (isAttached) return
        val view = ComposeView(context).apply {
            setContent {
                val state by stateFlow.collectAsState()
                if (state.isVisible && state.request != null && state.verdict != null) {
                    DecisionBarCard(
                        request = state.request!!,
                        verdict = state.verdict!!,
                        opacityPercent = state.opacityPercent,
                        isRtl = state.isRtl,
                        headingHomeInfo = state.headingHomeInfo,
                        calcMode = state.calcMode,
                        onSelectCalcMode = { newMode ->
                            stateFlow.value = stateFlow.value.copy(calcMode = newMode)
                        },
                        onClose = {
                            dismissCurrent()
                        },
                        onReport = {
                            onReportClicked(state.request!!, state.verdict!!)
                        },
                        onDrag = { dx, dy ->
                            moveBy(dx, dy)
                        },
                        onDragEnd = {
                            saveCustomPosition()
                        }
                    )
                }
            }
        }
        val owner = OverlayLifecycleOwner()
        owner.attachToView(view)
        this.lifecycleOwner = owner
        this.composeView = view

        val density = context.resources.displayMetrics.density
        val barWidthPx = (350 * density).toInt()
        val params = WindowManager.LayoutParams(
            barWidthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }
        this.windowParams = params

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
        windowParams = null
        isAttached = false
    }

    /**
     * Updates detail request and recalculates position.
     */
    fun show(
        request: ParsedRequest,
        verdict: Verdict,
        opacityPercent: Int,
        screenBounds: Rect,
        mapBounds: Rect?,
        headingHomeInfo: String? = null
    ) {
        if (request.key == dismissedRequestKey) {
            return
        }
        stateFlow.value = stateFlow.value.copy(
            request = request,
            verdict = verdict,
            opacityPercent = opacityPercent,
            isRtl = isRtl,
            headingHomeInfo = headingHomeInfo,
            isVisible = true
        )
        updatePosition(screenBounds, mapBounds)
    }

    fun hide() {
        stateFlow.value = stateFlow.value.copy(isVisible = false)
    }

    fun dismissCurrent() {
        val currentKey = stateFlow.value.request?.key
        if (currentKey != null) {
            dismissedRequestKey = currentKey
        }
        hide()
    }

    private fun updatePosition(screenBounds: Rect, mapBounds: Rect?) {
        val params = windowParams ?: return
        val view = composeView ?: return
        val density = context.resources.displayMetrics.density
        val barWidthPx = params.width.coerceAtLeast((300 * density).toInt())
        val barHeightPx = view.height.coerceAtLeast((220 * density).toInt())

        if (barPosition.isAuto) {
            val autoPoint = OverlayPositionHelper.calculateAutoPosition(
                screenBounds = screenBounds,
                mapBounds = mapBounds,
                barWidthPx = barWidthPx,
                barHeightPx = barHeightPx,
                isLandscape = screenBounds.width() > screenBounds.height(),
                density = density
            )
            params.x = autoPoint.x
            params.y = autoPoint.y
        } else {
            params.x = barPosition.x
            params.y = barPosition.y
        }
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun moveBy(dx: Float, dy: Float) {
        val params = windowParams ?: return
        val view = composeView ?: return
        params.x = (params.x + dx.roundToInt()).coerceAtLeast(0)
        params.y = (params.y + dy.roundToInt()).coerceAtLeast(0)
        try {
            windowManager.updateViewLayout(view, params)
        } catch (_: Exception) {}
    }

    private fun saveCustomPosition() {
        val params = windowParams ?: return
        barPosition = BarPosition(isAuto = false, x = params.x, y = params.y)
        onPositionSaved(barPosition)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DecisionBarCard(
    request: ParsedRequest,
    verdict: Verdict,
    opacityPercent: Int,
    isRtl: Boolean,
    headingHomeInfo: String?,
    calcMode: OverlayTripCalculationMode,
    onSelectCalcMode: (OverlayTripCalculationMode) -> Unit,
    onClose: () -> Unit,
    onReport: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit
) {
    // Premium Dark Obsidian & Racing Theme colors
    val obsidianBg = Color(0xFF0F1115)
    val graphiteSurface = Color(0xFF181B20)
    val slateBorder = Color(0xFF2A2F38)
    val racingOrange = Color(0xFFFF6B00)
    val emeraldGreen = Color(0xFF10B981)
    val textWhite = Color(0xFFF9FAFB)
    val textSilver = Color(0xFF9CA3AF)

    val toneColor = when (verdict.tone) {
        Tone.GREEN -> emeraldGreen
        Tone.ORANGE -> racingOrange
        Tone.RED -> Color(0xFFEF4444)
        Tone.GRAY -> Color(0xFF6B7280)
    }

    // Calculations based on dual mode:
    val effectivePickupKm = request.pickupKm ?: 0.0
    val effectiveTripKm = request.tripKm ?: ((request.totalKm ?: 0.0) - effectivePickupKm).coerceAtLeast(0.0)

    val displayedKm: Double? = if (calcMode == OverlayTripCalculationMode.FULL_TRIP) {
        verdict.totalKm ?: (effectivePickupKm + effectiveTripKm)
    } else {
        if (effectiveTripKm > 0.0) effectiveTripKm else request.tripKm
    }

    val displayedGrossPerKm: Double? = if (request.fare != null && displayedKm != null && displayedKm > 0.0) {
        request.fare / displayedKm
    } else {
        verdict.grossPerKm
    }

    val alpha = (opacityPercent.coerceIn(10, 100) / 100f)
    val cardBackground = graphiteSurface.copy(alpha = alpha)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(2.dp, toneColor, RoundedCornerShape(18.dp)),
        color = cardBackground,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Drag Grip Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            },
                            onDragEnd = onDragEnd
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(textSilver.copy(alpha = 0.5f))
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            // Title Row with PRO badge indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(toneColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRtl) verdict.titleAr else verdict.titleEn,
                        color = textWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    // PRO Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(racingOrange.copy(alpha = 0.2f))
                            .border(1.dp, racingOrange, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "[PRO]",
                            color = racingOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Report Button
                    IconButton(
                        onClick = onReport,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Report Request",
                            tint = racingOrange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    // Close Button
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Bar",
                            tint = textSilver,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 🚀 DUAL CALCULATION BUTTONS: [🔄 حساب الرحلة كاملة] and [⚡ حساب الرحلة الأساسية]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Full Trip (Pickup + Destination)
                val isFullSelected = calcMode == OverlayTripCalculationMode.FULL_TRIP
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isFullSelected) racingOrange else obsidianBg)
                        .border(
                            1.5.dp,
                            if (isFullSelected) racingOrange else slateBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectCalcMode(OverlayTripCalculationMode.FULL_TRIP) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = null,
                            tint = if (isFullSelected) obsidianBg else textWhite,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRtl) "حساب الرحلة كاملة" else "Full Trip",
                            color = if (isFullSelected) obsidianBg else textWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                // Button 2: Base Trip (Destination only, excluding pickup)
                val isBaseSelected = calcMode == OverlayTripCalculationMode.BASE_TRIP
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isBaseSelected) emeraldGreen else obsidianBg)
                        .border(
                            1.5.dp,
                            if (isBaseSelected) emeraldGreen else slateBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectCalcMode(OverlayTripCalculationMode.BASE_TRIP) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ElectricBolt,
                            contentDescription = null,
                            tint = if (isBaseSelected) obsidianBg else textWhite,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRtl) "حساب الرحلة الأساسية" else "Base Trip",
                            color = if (isBaseSelected) obsidianBg else textWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metric Tiles (4 Columns in Obsidian & Graphite Style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetricTile(
                    label = if (isRtl) "السعر" else "Offer",
                    value = formatFareValue(request.fare, request.currency, isRtl),
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = if (calcMode == OverlayTripCalculationMode.FULL_TRIP) {
                        if (isRtl) "كاملة (شامل)" else "Total (All)"
                    } else {
                        if (isRtl) "أساسية (مشوار)" else "Base Trip"
                    },
                    value = formatKmValue(displayedKm, isRtl),
                    modifier = Modifier.weight(1f),
                    highlightColor = if (calcMode == OverlayTripCalculationMode.BASE_TRIP) emeraldGreen else textWhite
                )
                MetricTile(
                    label = if (isRtl) "سعر الكيلو" else "Rate/km",
                    value = if (displayedGrossPerKm != null) "%.1f %s".format(Locale.US, displayedGrossPerKm, if (isRtl) "ج" else "LE") else "—",
                    modifier = Modifier.weight(1f),
                    highlightColor = racingOrange
                )
                MetricTile(
                    label = if (isRtl) "الطريق" else "Road",
                    value = formatRoadState(request.roadState, isRtl),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Extra Info Row (Stops, Rating, Payment)
            val extraDetails = buildExtraInfo(request, isRtl)
            if (extraDetails.isNotBlank()) {
                Text(
                    text = extraDetails,
                    color = textSilver,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Flags Row
            if (verdict.flags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (flag in verdict.flags) {
                        FlagChip(flag = flag)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Heading-Home Line
            if (!headingHomeInfo.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🏠 $headingHomeInfo",
                        color = emeraldGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Reason Line
            val reasonDetail = if (isRtl) verdict.detailAr else verdict.detailEn
            if (reasonDetail.isNotBlank()) {
                Text(
                    text = reasonDetail,
                    color = textWhite,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            // Retry Line when figures incomplete
            if (verdict.reasonCode == "detail_incomplete") {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = racingOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isRtl) "اترك الطلب مفتوحاً لقراءة المسافة من الخريطة" else "Keep request open to read map figures",
                        color = racingOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlightColor: Color = Color(0xFFF9FAFB)
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F1115))
            .border(1.dp, Color(0xFF2A2F38), RoundedCornerShape(8.dp))
            .padding(vertical = 5.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = Color(0xFF9CA3AF),
                fontSize = 10.sp,
                maxLines = 1
            )
            Text(
                text = value,
                color = highlightColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FlagChip(flag: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF0F1115))
            .border(1.dp, Color(0xFF2A2F38), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(
            text = flag,
            color = Color(0xFFFF6B00),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatFareValue(fare: Double?, currency: String?, isRtl: Boolean): String {
    if (fare == null) return "—"
    val curr = currency ?: "EGP"
    return if (isRtl) "${fare.toInt()} ج.م" else "${fare.toInt()} $curr"
}

private fun formatKmValue(km: Double?, isRtl: Boolean): String {
    if (km == null) return "—"
    return String.format(Locale.US, "%.1f %s", km, if (isRtl) "كم" else "km")
}

private fun formatRoadState(state: RoadState, isRtl: Boolean): String = when (state) {
    RoadState.EASY -> if (isRtl) "سلس" else "Easy"
    RoadState.SLOW -> if (isRtl) "زحام" else "Slow"
    RoadState.HARD -> if (isRtl) "شاق" else "Hard"
    RoadState.UNKNOWN -> if (isRtl) "عادي" else "Normal"
}

private fun buildExtraInfo(request: ParsedRequest, isRtl: Boolean): String {
    val items = mutableListOf<String>()
    if (request.extraStops > 0) {
        items.add(if (isRtl) "${request.extraStops} وقفات" else "${request.extraStops} stops")
    }
    if (!request.paymentMethod.isNullOrBlank()) {
        items.add(request.paymentMethod)
    }
    if (request.riderRating != null) {
        items.add("★ ${String.format(Locale.US, "%.2f", request.riderRating)}")
    }
    if (request.riderTrips != null && request.riderTrips > 0) {
        items.add(if (isRtl) "${request.riderTrips} رحلة" else "${request.riderTrips} trips")
    }
    return items.joinToString(" • ")
}
