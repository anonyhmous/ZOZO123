package com.driveflow.driver.engine

import android.content.Context
import android.location.Geocoder
import com.driveflow.driver.data.prefs.HomeWay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

sealed class HomeWayScore {
    data class Closer(val percent: Int) : HomeWayScore()
    object Away : HomeWayScore()
    object Unknown : HomeWayScore()
}

/**
 * Calculates whether a trip takes the driver closer to home.
 * Pure non-blocking helper that caches addresses and never interrupts or blocks verdicts.
 */
class HomeWayScorer(
    private val context: Context? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val addressGeoCache = ConcurrentHashMap<String, Pair<Double, Double>?>()

    suspend fun scoreAsync(
        homeWay: HomeWay,
        pickupAddress: String?,
        destinationAddress: String?
    ): HomeWayScore {
        if (!homeWay.enabled || (homeWay.lat == 0.0 && homeWay.lon == 0.0)) {
            return HomeWayScore.Unknown
        }
        if (pickupAddress.isNullOrBlank() || destinationAddress.isNullOrBlank()) {
            return HomeWayScore.Unknown
        }

        return withContext(Dispatchers.IO) {
            try {
                val pickupCoords = geocodeBestEffort(pickupAddress)
                val destCoords = geocodeBestEffort(destinationAddress)
                if (pickupCoords == null || destCoords == null) {
                    return@withContext HomeWayScore.Unknown
                }

                val distPickupToHome = calculateDistanceMeters(pickupCoords.first, pickupCoords.second, homeWay.lat, homeWay.lon)
                val distDestToHome = calculateDistanceMeters(destCoords.first, destCoords.second, homeWay.lat, homeWay.lon)

                if (distPickupToHome <= 100.0) {
                    return@withContext if (distDestToHome <= 200.0) HomeWayScore.Closer(100) else HomeWayScore.Away
                }

                val difference = distPickupToHome - distDestToHome
                if (difference > 0) {
                    val percentCloser = ((difference / distPickupToHome) * 100).toInt().coerceIn(1, 100)
                    HomeWayScore.Closer(percentCloser)
                } else {
                    HomeWayScore.Away
                }
            } catch (_: Exception) {
                HomeWayScore.Unknown
            }
        }
    }

    fun geocodeBestEffort(addressStr: String): Pair<Double, Double>? {
        if (addressGeoCache.containsKey(addressStr)) {
            return addressGeoCache[addressStr]
        }
        val coords = parseCoordinatesOrNull(addressStr) ?: run {
            if (context != null && Geocoder.isPresent()) {
                try {
                    val geocoder = Geocoder(context, Locale("ar", "EG"))
                    @Suppress("DEPRECATION")
                    val list = geocoder.getFromLocationName(addressStr, 1)
                    val first = list?.firstOrNull()
                    if (first != null) first.latitude to first.longitude else null
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        }
        addressGeoCache[addressStr] = coords
        return coords
    }

    fun injectCoordinates(address: String, lat: Double, lon: Double) {
        addressGeoCache[address] = lat to lon
    }

    private fun parseCoordinatesOrNull(str: String): Pair<Double, Double>? {
        if (!str.contains(",")) return null
        val parts = str.split(",")
        if (parts.size == 2) {
            val lat = parts[0].trim().toDoubleOrNull()
            val lon = parts[1].trim().toDoubleOrNull()
            if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                return lat to lon
            }
        }
        return null
    }

    // Haversine formula
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    fun clearCache() {
        addressGeoCache.clear()
    }
}
