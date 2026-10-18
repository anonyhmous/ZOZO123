package com.driveflow.driver.ocr

import android.content.Context
import android.graphics.RectF
import com.driveflow.driver.core.di.AppGraph
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.data.prefs.FractionRect
import com.driveflow.driver.data.prefs.OcrCalibration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class OcrDefaultConfigFile(
    val app: String = "",
    val version: Int = 1,
    val regions: Map<String, FractionRect> = emptyMap()
)

/**
 * Manages crop region calibrations for OCR readers across Uber and DiDi.
 */
class OcrCalibrationManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val cachedCalibrations = mutableMapOf<RideApp, OcrCalibration>()

    init {
        // Preload defaults
        loadDefault(RideApp.UBER, "ocr_default_uber.json")
        loadDefault(RideApp.DIDI, "ocr_default_didi.json")

        // Observe custom calibrations from SettingsStore
        coroutineScope.launch {
            AppGraph.settingsStore.ocrCalibrationFlow.collect { userCalibrations ->
                userCalibrations.forEach { (app, calibration) ->
                    if (calibration.regions.isNotEmpty()) {
                        cachedCalibrations[app] = calibration
                    }
                }
            }
        }
    }

    private fun loadDefault(app: RideApp, assetName: String) {
        try {
            val content = context.assets.open(assetName).bufferedReader().use { it.readText() }
            val parsed = json.decodeFromString<OcrDefaultConfigFile>(content)
            cachedCalibrations[app] = OcrCalibration(regions = parsed.regions)
        } catch (_: Exception) {
            // Sensible fallback in code if asset is unavailable
            val fallbackRegions = when (app) {
                RideApp.UBER -> mapOf(
                    "offer_card" to FractionRect(0.04f, 0.42f, 0.96f, 0.94f),
                    "fare" to FractionRect(0.05f, 0.44f, 0.55f, 0.56f),
                    "product" to FractionRect(0.05f, 0.55f, 0.65f, 0.63f),
                    "rating" to FractionRect(0.65f, 0.44f, 0.95f, 0.56f),
                    "distances" to FractionRect(0.05f, 0.63f, 0.95f, 0.77f),
                    "addresses" to FractionRect(0.05f, 0.77f, 0.95f, 0.93f)
                )
                RideApp.DIDI -> mapOf(
                    "offer_card" to FractionRect(0.04f, 0.38f, 0.96f, 0.92f),
                    "fare" to FractionRect(0.05f, 0.40f, 0.60f, 0.52f),
                    "rate_per_km" to FractionRect(0.05f, 0.52f, 0.60f, 0.60f),
                    "rating" to FractionRect(0.65f, 0.40f, 0.95f, 0.52f),
                    "distances" to FractionRect(0.05f, 0.60f, 0.95f, 0.74f),
                    "addresses" to FractionRect(0.05f, 0.74f, 0.95f, 0.90f)
                )
                else -> emptyMap()
            }
            cachedCalibrations[app] = OcrCalibration(regions = fallbackRegions)
        }
    }

    fun getCalibration(app: RideApp): OcrCalibration {
        return cachedCalibrations[app] ?: OcrCalibration()
    }

    fun getRegion(app: RideApp, regionKey: String): RectF {
        val calibration = getCalibration(app)
        val fraction = calibration.regions[regionKey]
            ?: FractionRect(0.05f, 0.35f, 0.95f, 0.90f)
        return RectF(fraction.left, fraction.top, fraction.right, fraction.bottom)
    }

    suspend fun saveCalibration(app: RideApp, regions: Map<String, FractionRect>) {
        val current = getCalibration(app)
        val updated = current.copy(regions = regions)
        cachedCalibrations[app] = updated
        AppGraph.settingsStore.setOcrCalibration(app, updated)
    }
}
