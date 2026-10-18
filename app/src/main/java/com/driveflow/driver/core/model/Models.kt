package com.driveflow.driver.core.model

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.serialization.Serializable

@Serializable
enum class RideApp(val pkg: String, val code: String) {
    INDRIVE("sinet.startup.inDriver", "indrive"),
    UBER("com.ubercab.driver", "uber"),
    DIDI("com.didiglobal.driver", "didi")
}

@Serializable
enum class ReadSource { ACCESSIBILITY, OCR }

@Serializable
enum class RoadState { UNKNOWN, EASY, SLOW, HARD }

@Serializable
enum class Tone { GREEN, ORANGE, RED, GRAY }

@Serializable
enum class StopsMode { IGNORE, PREFER, WARN, AVOID }

@Serializable
enum class LongTripMode { IGNORE, WARN, AVOID }

@Serializable
enum class DistanceUnit { KM, MILES, BOTH }

@Serializable
enum class LicenseStatus { ACTIVE, TRIAL, FREE }

/**
 * Supported vehicle types including motorcycles (150cc, 200cc, 250cc) and standard cars.
 * Preconfigured with typical Egypt fuel consumption (92 octane) and maintenance/wear benchmarks.
 */
@Serializable
enum class VehicleType(
    val id: String,
    val displayNameAr: String,
    val displayNameEn: String,
    val defaultFuelCostPerKm: Double,
    val defaultUpkeepCostPerKm: Double
) {
    CAR("car", "سيارة ملاكي", "Standard Car", 2.20, 1.10),
    MOTORCYCLE_150CC("moto_150cc", "دراجة نارية (150cc)", "Motorcycle (150cc)", 0.40, 0.25),
    MOTORCYCLE_200CC("moto_200cc", "دراجة نارية (200cc)", "Motorcycle (200cc)", 0.55, 0.30),
    MOTORCYCLE_250CC("moto_250cc", "دراجة نارية (250cc)", "Motorcycle (250cc)", 0.70, 0.40);

    val defaultTotalCostPerKm: Double
        get() = defaultFuelCostPerKm + defaultUpkeepCostPerKm
}

data class ParsedRequest(
    val app: RideApp,
    val key: String,
    val source: ReadSource,
    val fare: Double?,
    val currency: String?,
    val pickupKm: Double?,
    val tripKm: Double?,
    val totalKm: Double?,
    val pickupMinutes: Int?,
    val riderRating: Double?,
    val riderTrips: Int?,
    val requestAgeSeconds: Int?,
    val pickupAddress: String?,
    val destinationAddress: String?,
    val extraStops: Int,
    val paymentMethod: String?,
    val riderNote: String?,
    val offerCount: Int?,
    val roadState: RoadState,
    val productName: String?,
    val bounds: Rect,
    val isDetailOpen: Boolean,
    val confidence: Float
)

@Serializable
data class Policy(
    val vehicleType: VehicleType = VehicleType.CAR,
    val minPricePerKm: Double = 0.0,
    val minTotalKm: Double? = null,
    val maxTotalKm: Double? = null,
    val maxTripKm: Double? = null,
    val maxPickupKm: Double? = null,
    val maxPickupMinutes: Int? = null,
    val minRiderRating: Double? = null,
    val minRiderTrips: Int? = null,
    val maxRequestAgeMinutes: Int? = null,
    val negotiationStep: Int = 5,
    val maxOffer: Double? = null,
    val currency: String = "EGP",
    val governorate: String? = null,
    val avoidedAreas: List<String> = emptyList(),
    val preferredAreas: List<String> = emptyList(),
    val allowedPayments: Set<String> = emptySet(),
    val bannedNoteWords: List<String> = emptyList(),
    val stopsMode: StopsMode = StopsMode.IGNORE,
    val stopFee: Double? = null,
    val longTripMode: LongTripMode = LongTripMode.IGNORE,
    val longTripKm: Double? = null,
    val warnOnUberSaver: Boolean = false,
    val freePickupKm: Double? = null,
    val fuelCostPerKm: Double? = null,
    val upkeepCostPerKm: Double? = null,
    val commissionPct: Map<RideApp, Double> = emptyMap(),
    val commissionEnabled: Map<RideApp, Boolean> = emptyMap(),
    val appEnabled: Map<RideApp, Boolean> = mapOf(
        RideApp.INDRIVE to true,
        RideApp.UBER to true,
        RideApp.DIDI to true
    ),
    val distanceUnit: DistanceUnit = DistanceUnit.KM,
    val basisNet: Boolean = false
) {
    /**
     * Calculates the vehicle operating expense per kilometer.
     * Uses explicitly defined fuelCostPerKm and upkeepCostPerKm if set.
     * If not explicitly set and a motorcycle class is selected, uses the motorcycle class defaults.
     */
    val carCostPerKm: Double
        get() {
            val fuel = fuelCostPerKm ?: if (vehicleType != VehicleType.CAR) vehicleType.defaultFuelCostPerKm else 0.0
            val upkeep = upkeepCostPerKm ?: if (vehicleType != VehicleType.CAR) vehicleType.defaultUpkeepCostPerKm else 0.0
            return fuel + upkeep
        }
}

data class Verdict(
    val tone: Tone,
    val reasonCode: String,
    val titleAr: String,
    val titleEn: String,
    val detailAr: String,
    val detailEn: String,
    val flags: List<String>,
    val totalKm: Double?,
    val grossPerKm: Double?,
    val netPerKm: Double?,
    val netProfit: Double?,
    val suggestedPrice: Double?
)

interface AppReader {
    val app: RideApp
    fun isRideAppWindow(root: AccessibilityNodeInfo): Boolean
    fun readList(root: AccessibilityNodeInfo): List<ParsedRequest>
    fun readDetail(root: AccessibilityNodeInfo): ParsedRequest?
    fun mapBounds(root: AccessibilityNodeInfo): Rect?
}

data class TripKmResult(val value: Double?, val failure: String?)

interface TripDistanceOcr {
    suspend fun readTripKm(mapBounds: Rect): TripKmResult
}
