package com.driveflow.driver.overlay

import android.graphics.Point
import android.graphics.Rect
import com.driveflow.driver.data.prefs.MarkerSize
import kotlin.math.max

/**
 * Pure helper for overlay positioning, bounds calculation, scaling, and screen safety.
 */
object OverlayPositionHelper {
    const val SMALL_MARKER_DP = 24f
    const val NORMAL_MARKER_DP = 32f
    const val LARGE_MARKER_DP = 44f
    const val DEFAULT_STALE_MS = 400L
    const val DEFAULT_THROTTLE_MS = 250L

    /**
     * Resolves the unscaled dp dimension for a MarkerSize preference.
     */
    fun markerSizeDp(size: MarkerSize): Float = when (size) {
        MarkerSize.SMALL -> SMALL_MARKER_DP
        MarkerSize.NORMAL -> NORMAL_MARKER_DP
        MarkerSize.LARGE -> LARGE_MARKER_DP
    }

    /**
     * Calculates marker size in pixels with fontScale and display density applied.
     */
    fun calculateScaledMarkerSizePx(size: MarkerSize, density: Float, fontScale: Float = 1.0f): Int {
        val baseDp = markerSizeDp(size)
        val scaled = baseDp * density * fontScale.coerceIn(0.7f, 2.0f)
        return scaled.toInt().coerceAtLeast(16)
    }

    /**
     * Anchors a badge marker to a ride card bounds:
     * - Right edge for LTR screens / apps.
     * - Left edge for RTL screens.
     * - Returns null if the card is not fully inside the screen bounds.
     */
    fun calculateMarkerBounds(
        cardBounds: Rect,
        markerSizePx: Int,
        isRtl: Boolean,
        screenBounds: Rect,
        marginPx: Int = 8
    ): Rect? {
        // Never draw if card is outside or partially clipped by screen
        if (cardBounds.left < screenBounds.left ||
            cardBounds.top < screenBounds.top ||
            cardBounds.right > screenBounds.right ||
            cardBounds.bottom > screenBounds.bottom ||
            cardBounds.width() < markerSizePx ||
            cardBounds.height() < markerSizePx
        ) {
            return null
        }

        val top = (cardBounds.centerY() - markerSizePx / 2).coerceIn(
            cardBounds.top + marginPx,
            max(cardBounds.top + marginPx, cardBounds.bottom - markerSizePx - marginPx)
        )
        val bottom = top + markerSizePx

        val left = if (isRtl) {
            cardBounds.left + marginPx
        } else {
            cardBounds.right - markerSizePx - marginPx
        }
        val right = left + markerSizePx

        return Rect(left, top, right, bottom)
    }

    /**
     * Computes AUTO position for the DecisionBar:
     * - Beside the map without covering the map route or the bottom action buttons.
     * - Avoids the bottom 18% of the screen (Accept/Decline buttons).
     * - Avoids covering mapBounds when present.
     */
    fun calculateAutoPosition(
        screenBounds: Rect,
        mapBounds: Rect?,
        barWidthPx: Int,
        barHeightPx: Int,
        isLandscape: Boolean = false,
        density: Float = 1.0f
    ): Point {
        val screenWidth = screenBounds.width()
        val screenHeight = screenBounds.height()
        val bottomButtonsReserved = (screenHeight * 0.18f).toInt()
        val topStatusReserved = (screenHeight * 0.05f).toInt()
        val safeBottom = screenHeight - bottomButtonsReserved

        // Default horizontal centering
        val defaultX = max(0, (screenWidth - barWidthPx) / 2)

        if (isLandscape) {
            // In landscape, place beside map
            val x = if (mapBounds != null && mapBounds.centerX() > screenWidth / 2) {
                // Map center is on right half, put bar on left
                (mapBounds.left - barWidthPx - 16).coerceAtLeast(16)
            } else if (mapBounds != null && mapBounds.centerX() <= screenWidth / 2) {
                // Map center is on left half, put bar on right
                (mapBounds.right + 16).coerceAtMost(screenWidth - barWidthPx - 16)
            } else {
                defaultX
            }
            val y = (screenHeight - barHeightPx) / 2
            return Point(
                x.coerceIn(0, max(0, screenWidth - barWidthPx)),
                y.coerceIn(topStatusReserved, max(topStatusReserved, screenHeight - barHeightPx))
            )
        }

        // Portrait layout
        if (mapBounds != null && mapBounds.height() > 50 && mapBounds.bottom < safeBottom) {
            // Map is in upper section: check if space below map can fit the bar
            val spaceBelowMap = safeBottom - (mapBounds.bottom + 12)
            if (spaceBelowMap >= barHeightPx) {
                // Place just below map and above bottom buttons
                val targetY = mapBounds.bottom + 12
                return Point(
                    defaultX.coerceIn(0, max(0, screenWidth - barWidthPx)),
                    targetY.coerceIn(topStatusReserved, max(topStatusReserved, safeBottom - barHeightPx))
                )
            } else {
                // Space below map is tight: check if space above map exists
                val spaceAboveMap = mapBounds.top - topStatusReserved
                if (spaceAboveMap >= barHeightPx) {
                    val targetY = max(topStatusReserved, mapBounds.top - barHeightPx - 12)
                    return Point(
                        defaultX.coerceIn(0, max(0, screenWidth - barWidthPx)),
                        targetY
                    )
                }
            }
        }

        // Default: placed directly above the bottom action buttons
        val targetY = max(topStatusReserved, safeBottom - barHeightPx - 16)
        return Point(
            defaultX.coerceIn(0, max(0, screenWidth - barWidthPx)),
            targetY.coerceIn(topStatusReserved, max(topStatusReserved, safeBottom - barHeightPx))
        )
    }

    /**
     * Checks if a card mark has not been observed for longer than staleThresholdMs.
     */
    fun isStale(lastSeenTimeMs: Long, currentTimeMs: Long, staleThresholdMs: Long = DEFAULT_STALE_MS): Boolean {
        return (currentTimeMs - lastSeenTimeMs) > staleThresholdMs
    }
}
