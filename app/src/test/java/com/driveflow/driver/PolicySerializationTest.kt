package com.driveflow.driver

import com.driveflow.driver.core.model.DistanceUnit
import com.driveflow.driver.core.model.LongTripMode
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.StopsMode
import com.driveflow.driver.core.model.VehicleType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicySerializationTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun defaultPolicy_serializesAndDeserializesCorrectly() {
        val original = Policy()
        val jsonString = json.encodeToString(original)
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("\"currency\": \"EGP\""))
        assertTrue(jsonString.contains("\"negotiationStep\": 5"))

        val deserialized = json.decodeFromString<Policy>(jsonString)
        assertEquals(original, deserialized)
        assertEquals(0.0, deserialized.carCostPerKm, 0.001)
        assertEquals(VehicleType.CAR, deserialized.vehicleType)
        assertEquals(DistanceUnit.KM, deserialized.distanceUnit)
        assertEquals(StopsMode.IGNORE, deserialized.stopsMode)
        assertEquals(LongTripMode.IGNORE, deserialized.longTripMode)
    }

    @Test
    fun motorcyclePolicy_150cc_calculatesAccurateCostPerKm() {
        val policy150 = Policy(vehicleType = VehicleType.MOTORCYCLE_150CC)
        val jsonString = json.encodeToString(policy150)
        val deserialized = json.decodeFromString<Policy>(jsonString)

        assertEquals(VehicleType.MOTORCYCLE_150CC, deserialized.vehicleType)
        // Default benchmark for 150cc: 0.40 fuel + 0.25 upkeep = 0.65 EGP/km
        assertEquals(0.65, deserialized.carCostPerKm, 0.001)
    }

    @Test
    fun motorcyclePolicy_200cc_and_250cc_benchmarks() {
        val policy200 = Policy(vehicleType = VehicleType.MOTORCYCLE_200CC)
        // 0.55 fuel + 0.30 upkeep = 0.85 EGP/km
        assertEquals(0.85, policy200.carCostPerKm, 0.001)

        val policy250 = Policy(vehicleType = VehicleType.MOTORCYCLE_250CC)
        // 0.70 fuel + 0.40 upkeep = 1.10 EGP/km
        assertEquals(1.10, policy250.carCostPerKm, 0.001)
    }

    @Test
    fun customizedPolicy_serializesAndDeserializesAllFields() {
        val original = Policy(
            vehicleType = VehicleType.MOTORCYCLE_200CC,
            minPricePerKm = 12.5,
            minTotalKm = 2.0,
            maxTotalKm = 45.0,
            maxTripKm = 35.0,
            maxPickupKm = 4.5,
            maxPickupMinutes = 8,
            minRiderRating = 4.7,
            minRiderTrips = 15,
            maxRequestAgeMinutes = 3,
            negotiationStep = 10,
            maxOffer = 500.0,
            currency = "EGP",
            governorate = "Cairo",
            avoidedAreas = listOf("Ring Road Stop", "Narrow Alley A"),
            preferredAreas = listOf("Tagamoa", "Maadi", "Zamalek"),
            allowedPayments = setOf("CASH", "CARD"),
            bannedNoteWords = listOf("smoke", "pet", "wait"),
            stopsMode = StopsMode.WARN,
            stopFee = 25.0,
            longTripMode = LongTripMode.WARN,
            longTripKm = 40.0,
            warnOnUberSaver = true,
            freePickupKm = 1.0,
            fuelCostPerKm = 0.60,
            upkeepCostPerKm = 0.35,
            commissionPct = mapOf(
                RideApp.INDRIVE to 10.0,
                RideApp.UBER to 22.5,
                RideApp.DIDI to 18.0
            ),
            commissionEnabled = mapOf(
                RideApp.INDRIVE to true,
                RideApp.UBER to true,
                RideApp.DIDI to false
            ),
            appEnabled = mapOf(
                RideApp.INDRIVE to true,
                RideApp.UBER to true,
                RideApp.DIDI to false
            ),
            distanceUnit = DistanceUnit.KM,
            basisNet = true
        )

        val jsonString = json.encodeToString(original)
        val deserialized = json.decodeFromString<Policy>(jsonString)

        assertEquals(original, deserialized)
        assertEquals(VehicleType.MOTORCYCLE_200CC, deserialized.vehicleType)
        assertEquals(12.5, deserialized.minPricePerKm, 0.001)
        assertEquals(0.95, deserialized.carCostPerKm, 0.001) // 0.60 + 0.35
        assertEquals(StopsMode.WARN, deserialized.stopsMode)
        assertEquals(LongTripMode.WARN, deserialized.longTripMode)
        assertEquals(true, deserialized.basisNet)
        assertEquals(listOf("Tagamoa", "Maadi", "Zamalek"), deserialized.preferredAreas)
        assertEquals(10.0, deserialized.commissionPct[RideApp.INDRIVE])
        assertEquals(false, deserialized.appEnabled[RideApp.DIDI])
    }

    @Test
    fun policy_handlesLenientJsonWithMissingOrExtraFields() {
        val rawJson = """
            {
                "minPricePerKm": 8.0,
                "unknownFieldShouldBeIgnored": "abc",
                "currency": "EGP"
            }
        """.trimIndent()

        val deserialized = json.decodeFromString<Policy>(rawJson)
        assertEquals(8.0, deserialized.minPricePerKm, 0.001)
        assertEquals("EGP", deserialized.currency)
        assertEquals(5, deserialized.negotiationStep)
        assertEquals(StopsMode.IGNORE, deserialized.stopsMode)
    }
}
