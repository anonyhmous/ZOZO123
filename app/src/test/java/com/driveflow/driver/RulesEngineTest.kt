package com.driveflow.driver

import android.graphics.Rect
import com.driveflow.driver.core.model.DistanceUnit
import com.driveflow.driver.core.model.LongTripMode
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.ReadSource
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.RoadState
import com.driveflow.driver.core.model.StopsMode
import com.driveflow.driver.core.model.Tone
import com.driveflow.driver.core.model.VehicleType
import com.driveflow.driver.core.model.Verdict
import com.driveflow.driver.data.prefs.HomeWay
import com.driveflow.driver.engine.ArabicTextNormalizer
import com.driveflow.driver.engine.DecisionStore
import com.driveflow.driver.engine.HomeWayScore
import com.driveflow.driver.engine.HomeWayScorer
import com.driveflow.driver.engine.OpportunityDetector
import com.driveflow.driver.engine.RulesEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RulesEngineTest {

    private fun createBaseRequest(
        app: RideApp = RideApp.INDRIVE,
        key: String = "test_req_1",
        fare: Double? = 150.0,
        currency: String? = "EGP",
        pickupKm: Double? = 2.0,
        tripKm: Double? = 10.0,
        totalKm: Double? = 12.0,
        pickupMinutes: Int? = 5,
        riderRating: Double? = 4.9,
        riderTrips: Int? = 50,
        requestAgeSeconds: Int? = 60,
        pickupAddress: String? = "المعادي",
        destinationAddress: String? = "التجمع الخامس",
        extraStops: Int = 0,
        paymentMethod: String? = "CASH",
        riderNote: String? = null,
        roadState: RoadState = RoadState.UNKNOWN,
        productName: String? = null,
        isDetailOpen: Boolean = false
    ) = ParsedRequest(
        app = app,
        key = key,
        source = ReadSource.ACCESSIBILITY,
        fare = fare,
        currency = currency,
        pickupKm = pickupKm,
        tripKm = tripKm,
        totalKm = totalKm,
        pickupMinutes = pickupMinutes,
        riderRating = riderRating,
        riderTrips = riderTrips,
        requestAgeSeconds = requestAgeSeconds,
        pickupAddress = pickupAddress,
        destinationAddress = destinationAddress,
        extraStops = extraStops,
        paymentMethod = paymentMethod,
        riderNote = riderNote,
        offerCount = null,
        roadState = roadState,
        productName = productName,
        bounds = Rect(0, 0, 500, 300),
        isDetailOpen = isDetailOpen,
        confidence = 0.95f
    )

    private val defaultPolicy = Policy(
        minPricePerKm = 10.0,
        currency = "EGP",
        negotiationStep = 5
    )

    // 1. App disabled check
    @Test
    fun test01_appDisabled_returnsNull() {
        val policy = defaultPolicy.copy(appEnabled = mapOf(RideApp.INDRIVE to false))
        val req = createBaseRequest(app = RideApp.INDRIVE)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNull(verdict)
    }

    // 2. Policy missing
    @Test
    fun test02_policyMissing_returnsGray() {
        val policy = defaultPolicy.copy(minPricePerKm = 0.0)
        val req = createBaseRequest()
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.GRAY, verdict!!.tone)
        assertEquals("policy_missing", verdict.reasonCode)
    }

    // 3. Currency mismatch
    @Test
    fun test03_currencyMismatch_returnsGray() {
        val policy = defaultPolicy.copy(currency = "EGP")
        val req = createBaseRequest(currency = "USD")
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.GRAY, verdict!!.tone)
        assertEquals("currency_mismatch", verdict.reasonCode)
    }

    // 4. Currency matching
    @Test
    fun test04_currencyMatch_passesCheck() {
        val policy = defaultPolicy.copy(currency = "EGP")
        val req = createBaseRequest(currency = "egp")
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertTrue(verdict!!.reasonCode != "currency_mismatch")
    }

    // 5. List fields missing - fare null
    @Test
    fun test05_listFieldsMissing_nullFare() {
        val req = createBaseRequest(fare = null)
        val verdict = RulesEngine.evaluate(req, defaultPolicy)
        assertNotNull(verdict)
        assertEquals(Tone.GRAY, verdict!!.tone)
        assertEquals("list_fields_missing", verdict.reasonCode)
    }

    // 6. List fields missing - pickup null
    @Test
    fun test06_listFieldsMissing_nullPickup() {
        val req = createBaseRequest(pickupKm = null)
        val verdict = RulesEngine.evaluate(req, defaultPolicy)
        assertNotNull(verdict)
        assertEquals(Tone.GRAY, verdict!!.tone)
        assertEquals("list_fields_missing", verdict.reasonCode)
    }

    // 7. Avoided area in pickup address
    @Test
    fun test07_avoidedArea_pickupAddress_returnsRed() {
        val policy = defaultPolicy.copy(avoidedAreas = listOf("حلوان", "المرج"))
        val req = createBaseRequest(pickupAddress = "محطة مترو حلوان")
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("avoided_area", verdict.reasonCode)
    }

    // 8. Avoided area in destination address
    @Test
    fun test08_avoidedArea_destinationAddress_returnsRed() {
        val policy = defaultPolicy.copy(avoidedAreas = listOf("المرج"))
        val req = createBaseRequest(destinationAddress = "الدائري نزلة المرج")
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("avoided_area", verdict.reasonCode)
    }

    // 9. Avoided area in rider note
    @Test
    fun test09_avoidedArea_inRiderNote_returnsRed() {
        val policy = defaultPolicy.copy(avoidedAreas = listOf("شبرا"))
        val req = createBaseRequest(riderNote = "رايح شبرا الخيمة")
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("avoided_area", verdict.reasonCode)
    }

    // 10. Arabic normalization with diacritics and Alef forms
    @Test
    fun test10_arabicNormalization_matchesAcrossVariations() {
        val policy = defaultPolicy.copy(avoidedAreas = listOf("أكتوبر")) // Hamza on Alef
        val req = createBaseRequest(destinationAddress = "مدينه 6 اكتوبر") // Bare Alef
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("avoided_area", verdict.reasonCode)

        // Test normalizer directly
        val raw = "الرِّحَابُ"
        val normalized = ArabicTextNormalizer.normalize(raw)
        assertEquals("الرحاب", normalized)
    }

    // 11. Payment refused
    @Test
    fun test11_paymentRefused_whenNotAllowed() {
        val policy = defaultPolicy.copy(allowedPayments = setOf("CASH"))
        val req = createBaseRequest(paymentMethod = "CARD")
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("payment_refused", verdict.reasonCode)
    }

    // 12. Payment allowed
    @Test
    fun test12_paymentAllowed_whenInSet() {
        val policy = defaultPolicy.copy(allowedPayments = setOf("CASH", "WALLET"))
        val req = createBaseRequest(paymentMethod = "CASH")
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertTrue(verdict!!.reasonCode != "payment_refused")
    }

    // 13. Note word banned
    @Test
    fun test13_noteWord_bannedWord_returnsRed() {
        val policy = defaultPolicy.copy(bannedNoteWords = listOf("تدخين", "سجاير", "كلب"))
        val req = createBaseRequest(riderNote = "معايا سجاير وبدخن")
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("note_word", verdict.reasonCode)
    }

    // 14. Extra stops with AVOID mode
    @Test
    fun test14_extraStops_avoidMode_returnsRed() {
        val policy = defaultPolicy.copy(stopsMode = StopsMode.AVOID)
        val req = createBaseRequest(extraStops = 1)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("extra_stops", verdict.reasonCode)
    }

    // 15. Pickup distance exceeds policy
    @Test
    fun test15_pickupOverPolicy_km_returnsRed() {
        val policy = defaultPolicy.copy(maxPickupKm = 3.0)
        val req = createBaseRequest(pickupKm = 4.5)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("pickup_over_policy", verdict.reasonCode)
    }

    // 16. Pickup minutes exceeds policy
    @Test
    fun test16_pickupOverPolicy_minutes_returnsRed() {
        val policy = defaultPolicy.copy(maxPickupMinutes = 8)
        val req = createBaseRequest(pickupKm = 2.0, pickupMinutes = 12)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("pickup_over_policy", verdict.reasonCode)
    }

    // 17. Rider trips below policy
    @Test
    fun test17_riderTripsBelowPolicy_returnsOrange() {
        val policy = defaultPolicy.copy(minRiderTrips = 20)
        val req = createBaseRequest(riderTrips = 5)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.ORANGE, verdict!!.tone)
        assertEquals("rider_trips_below_policy", verdict.reasonCode)
    }

    // 18. Rider rating below policy
    @Test
    fun test18_riderRatingBelowPolicy_returnsOrange() {
        val policy = defaultPolicy.copy(minRiderRating = 4.75)
        val req = createBaseRequest(riderRating = 4.50)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.ORANGE, verdict!!.tone)
        assertEquals("rider_rating_below_policy", verdict.reasonCode)
    }

    // 19. Rider fields unreadable - rating null when required
    @Test
    fun test19_riderFieldsUnreadable_nullRating() {
        val policy = defaultPolicy.copy(minRiderRating = 4.6)
        val req = createBaseRequest(riderRating = null)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.ORANGE, verdict!!.tone)
        assertEquals("rider_fields_unreadable", verdict.reasonCode)
    }

    // 20. Rider fields unreadable - trips null when required
    @Test
    fun test20_riderFieldsUnreadable_nullTrips() {
        val policy = defaultPolicy.copy(minRiderTrips = 10)
        val req = createBaseRequest(riderTrips = null)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.ORANGE, verdict!!.tone)
        assertEquals("rider_fields_unreadable", verdict.reasonCode)
    }

    // 21. Rider fields unreadable - age null when required
    @Test
    fun test21_riderFieldsUnreadable_nullAge() {
        val policy = defaultPolicy.copy(maxRequestAgeMinutes = 5)
        val req = createBaseRequest(requestAgeSeconds = null)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.ORANGE, verdict!!.tone)
        assertEquals("rider_fields_unreadable", verdict.reasonCode)
    }

    // 22. Offer older than policy
    @Test
    fun test22_offerOlderThanPolicy_returnsOrange() {
        val policy = defaultPolicy.copy(maxRequestAgeMinutes = 3)
        val req = createBaseRequest(requestAgeSeconds = 300) // 5 minutes
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.ORANGE, verdict!!.tone)
        assertEquals("offer_older_than_policy", verdict.reasonCode)
    }

    // 23. Extra stops with WARN mode
    @Test
    fun test23_extraStops_warnMode_returnsOrange() {
        val policy = defaultPolicy.copy(stopsMode = StopsMode.WARN)
        val req = createBaseRequest(extraStops = 1)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.ORANGE, verdict!!.tone)
        assertEquals("extra_stops", verdict.reasonCode)
    }

    // 24. Uber Saver warning
    @Test
    fun test24_uberSaver_warning_returnsOrange() {
        val policy = defaultPolicy.copy(warnOnUberSaver = true)
        val req = createBaseRequest(app = RideApp.UBER, productName = "UberX Saver")
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.ORANGE, verdict!!.tone)
        assertEquals("uber_saver", verdict.reasonCode)
    }

    // 25. Uber Saver warning ignored when false
    @Test
    fun test25_uberSaver_ignoredWhenDisabled() {
        val policy = defaultPolicy.copy(warnOnUberSaver = false)
        val req = createBaseRequest(app = RideApp.UBER, productName = "UberX Saver")
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals("list_ok", verdict!!.reasonCode)
    }

    // 26. Preferred area match in List mode
    @Test
    fun test26_preferredAreaMatch_inList_returnsGray() {
        val policy = defaultPolicy.copy(preferredAreas = listOf("المهندسين", "المعادي"))
        val req = createBaseRequest(destinationAddress = "المعادي دجلة")
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.GRAY, verdict!!.tone)
        assertEquals("preferred_area_match", verdict.reasonCode)
    }

    // 27. List OK returns GREEN
    @Test
    fun test27_listOk_returnsGreen() {
        val req = createBaseRequest(isDetailOpen = false)
        val verdict = RulesEngine.evaluate(req, defaultPolicy)
        assertNotNull(verdict)
        assertEquals(Tone.GREEN, verdict!!.tone)
        assertEquals("list_ok", verdict.reasonCode)
    }

    // 28. Detail incomplete when totalKm is null
    @Test
    fun test28_detailIncomplete_whenTotalKmNull() {
        val req = createBaseRequest(isDetailOpen = true, totalKm = null, tripKm = null)
        val verdict = RulesEngine.evaluate(req, defaultPolicy)
        assertNotNull(verdict)
        assertEquals(Tone.GRAY, verdict!!.tone)
        assertEquals("detail_incomplete", verdict.reasonCode)
    }

    // 29. Detail inDrive with trip_unknown flag
    @Test
    fun test29_detailInDrive_tripUnknownFlag() {
        val req = createBaseRequest(app = RideApp.INDRIVE, isDetailOpen = true, totalKm = null, tripKm = null)
        val verdict = RulesEngine.evaluate(req, defaultPolicy)
        assertNotNull(verdict)
        assertEquals("detail_incomplete", verdict!!.reasonCode)
        assertTrue(verdict.flags.contains("trip_unknown"))
    }

    // 30. Detail: Skip total distance floor
    @Test
    fun test30_detail_skipTotalDistanceFloor() {
        val policy = defaultPolicy.copy(minTotalKm = 5.0)
        val req = createBaseRequest(isDetailOpen = true, totalKm = 3.0)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("skip_total_distance_floor", verdict.reasonCode)
    }

    // 31. Detail: Skip total distance cap
    @Test
    fun test31_detail_skipTotalDistanceCap() {
        val policy = defaultPolicy.copy(maxTotalKm = 40.0)
        val req = createBaseRequest(isDetailOpen = true, totalKm = 45.0)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("skip_total_distance_cap", verdict.reasonCode)
    }

    // 32. Detail: Skip trip distance cap
    @Test
    fun test32_detail_skipTripDistanceCap() {
        val policy = defaultPolicy.copy(maxTripKm = 25.0)
        val req = createBaseRequest(isDetailOpen = true, pickupKm = 2.0, tripKm = 30.0, totalKm = 32.0)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("skip_trip_distance_cap", verdict.reasonCode)
    }

    // 33. Detail: Long trip mode AVOID
    @Test
    fun test33_detail_longTrip_avoid_returnsRed() {
        val policy = defaultPolicy.copy(longTripKm = 35.0, longTripMode = LongTripMode.AVOID)
        val req = createBaseRequest(isDetailOpen = true, totalKm = 40.0)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("long_trip", verdict.reasonCode)
    }

    // 34. Detail: Long trip mode WARN
    @Test
    fun test34_detail_longTrip_warn_returnsOrange() {
        val policy = defaultPolicy.copy(longTripKm = 35.0, longTripMode = LongTripMode.WARN)
        val req = createBaseRequest(isDetailOpen = true, totalKm = 40.0)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.ORANGE, verdict!!.tone)
        assertEquals("long_trip", verdict.reasonCode)
    }

    // 35. Detail: Long trip mode IGNORE
    @Test
    fun test35_detail_longTrip_ignore_doesNotWarn() {
        val policy = defaultPolicy.copy(minPricePerKm = 10.0, longTripKm = 35.0, longTripMode = LongTripMode.IGNORE)
        val req = createBaseRequest(isDetailOpen = true, fare = 500.0, totalKm = 40.0) // 12.5 EGP/km > 10.0
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.GREEN, verdict!!.tone)
        assertEquals("accept_if_road_ok", verdict.reasonCode)
    }

    // 36. Detail: Accept if road ok (price fine)
    @Test
    fun test36_detail_acceptIfRoadOk() {
        val policy = defaultPolicy.copy(minPricePerKm = 10.0)
        val req = createBaseRequest(isDetailOpen = true, fare = 150.0, totalKm = 10.0, roadState = RoadState.EASY)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.GREEN, verdict!!.tone)
        assertEquals("accept_if_road_ok", verdict.reasonCode)
        assertEquals(15.0, verdict.grossPerKm!!, 0.01)
    }

    // 37. Detail: Review slow route when RoadState SLOW
    @Test
    fun test37_detail_reviewSlowRoute_slow() {
        val policy = defaultPolicy.copy(minPricePerKm = 10.0)
        val req = createBaseRequest(isDetailOpen = true, fare = 150.0, totalKm = 10.0, roadState = RoadState.SLOW)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.ORANGE, verdict!!.tone)
        assertEquals("review_slow_route", verdict.reasonCode)
    }

    // 38. Detail: Review slow route when RoadState HARD
    @Test
    fun test38_detail_reviewSlowRoute_hard() {
        val policy = defaultPolicy.copy(minPricePerKm = 10.0)
        val req = createBaseRequest(isDetailOpen = true, fare = 150.0, totalKm = 10.0, roadState = RoadState.HARD)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.ORANGE, verdict!!.tone)
        assertEquals("review_slow_route", verdict.reasonCode)
    }

    // 39. Detail: Extra stops preferred flag
    @Test
    fun test39_detail_extraStops_preferredFlag() {
        val policy = defaultPolicy.copy(minPricePerKm = 10.0, stopsMode = StopsMode.PREFER)
        val req = createBaseRequest(isDetailOpen = true, fare = 200.0, totalKm = 10.0, extraStops = 2)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.GREEN, verdict!!.tone)
        assertTrue(verdict.flags.contains("extra_stops_preferred"))
    }

    // 40. inDrive counter or skip with suggested price rounded by step 5
    @Test
    fun test40_inDrive_counterOrSkip_step5() {
        val policy = defaultPolicy.copy(minPricePerKm = 12.0, negotiationStep = 5)
        // requiredFare = 12 * 10 = 120 EGP. Current fare = 90 EGP (< 120).
        val req = createBaseRequest(app = RideApp.INDRIVE, isDetailOpen = true, fare = 90.0, totalKm = 10.0)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.ORANGE, verdict!!.tone)
        assertEquals("counter_or_skip", verdict.reasonCode)
        assertEquals(120.0, verdict.suggestedPrice!!, 0.01)
    }

    // 41. inDrive counter or skip with suggested price rounded by step 10
    @Test
    fun test41_inDrive_counterOrSkip_step10() {
        val policy = defaultPolicy.copy(minPricePerKm = 12.0, negotiationStep = 10)
        // requiredFare = 12 * 11 = 132. ceil(132 / 10) * 10 = 140.
        val req = createBaseRequest(app = RideApp.INDRIVE, isDetailOpen = true, fare = 80.0, totalKm = 11.0)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.ORANGE, verdict!!.tone)
        assertEquals("counter_or_skip", verdict.reasonCode)
        assertEquals(140.0, verdict.suggestedPrice!!, 0.01)
    }

    // 42. inDrive over counter cap
    @Test
    fun test42_inDrive_skipAppCounterCap() {
        val policy = defaultPolicy.copy(minPricePerKm = 15.0, negotiationStep = 5, maxOffer = 200.0)
        // requiredFare = 15 * 20 = 300 EGP > maxOffer 200 EGP.
        val req = createBaseRequest(app = RideApp.INDRIVE, isDetailOpen = true, fare = 100.0, totalKm = 20.0)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("skip_app_counter_cap", verdict.reasonCode)
    }

    // 43. Uber skip price below rate (fixed fare)
    @Test
    fun test43_uber_skipPriceBelowRate() {
        val policy = defaultPolicy.copy(minPricePerKm = 15.0)
        // requiredFare = 15 * 10 = 150. Current fare = 100. Shortfall = 50.
        val req = createBaseRequest(app = RideApp.UBER, isDetailOpen = true, fare = 100.0, totalKm = 10.0)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("skip_price_below_rate", verdict.reasonCode)
    }

    // 44. DiDi skip price below rate (fixed fare)
    @Test
    fun test44_didi_skipPriceBelowRate() {
        val policy = defaultPolicy.copy(minPricePerKm = 15.0)
        val req = createBaseRequest(app = RideApp.DIDI, isDetailOpen = true, fare = 80.0, totalKm = 10.0)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("skip_price_below_rate", verdict.reasonCode)
    }

    // 45. Commission enabled vs disabled calculation
    @Test
    fun test45_commissionCalculation() {
        val policyWithComm = defaultPolicy.copy(
            commissionPct = mapOf(RideApp.UBER to 20.0),
            commissionEnabled = mapOf(RideApp.UBER to true),
            fuelCostPerKm = 2.0,
            upkeepCostPerKm = 1.0 // carCostPerKm = 3.0
        )
        // fare = 100 EGP, 10 km.
        // With 20% commission: netFare = 80. carCost = 3 * 10 = 30. netProfit = 50. netPerKm = 5.0.
        val req = createBaseRequest(app = RideApp.UBER, isDetailOpen = true, fare = 100.0, totalKm = 10.0)
        val verdictWithComm = RulesEngine.evaluate(req, policyWithComm)
        assertEquals(50.0, verdictWithComm!!.netProfit!!, 0.01)
        assertEquals(5.0, verdictWithComm.netPerKm!!, 0.01)

        val policyNoComm = policyWithComm.copy(commissionEnabled = mapOf(RideApp.UBER to false))
        val verdictNoComm = RulesEngine.evaluate(req, policyNoComm)
        // netFare = 100. carCost = 30. netProfit = 70. netPerKm = 7.0.
        assertEquals(70.0, verdictNoComm!!.netProfit!!, 0.01)
        assertEquals(7.0, verdictNoComm.netPerKm!!, 0.01)
    }

    // 46. BasisNet true compares net rate
    @Test
    fun test46_basisNetTrue_evaluatesAgainstNetPerKm() {
        val policy = defaultPolicy.copy(
            minPricePerKm = 10.0,
            basisNet = true,
            commissionPct = mapOf(RideApp.UBER to 20.0),
            commissionEnabled = mapOf(RideApp.UBER to true),
            fuelCostPerKm = 2.0 // carCost = 2.0
        )
        // fare = 120, totalKm = 10. grossPerKm = 12.0 (>= 10.0, gross is fine).
        // netFare = 96. carCost = 20. netProfit = 76. netPerKm = 7.6 (< 10.0 minPricePerKm).
        val req = createBaseRequest(app = RideApp.UBER, isDetailOpen = true, fare = 120.0, totalKm = 10.0)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        // Because basisNet = true, netPerKm 7.6 < 10.0 -> skip_price_below_rate
        assertEquals(Tone.RED, verdict!!.tone)
        assertEquals("skip_price_below_rate", verdict.reasonCode)
    }

    // 47. BasisNet false compares gross rate
    @Test
    fun test47_basisNetFalse_evaluatesAgainstGrossPerKm() {
        val policy = defaultPolicy.copy(
            minPricePerKm = 10.0,
            basisNet = false,
            commissionPct = mapOf(RideApp.UBER to 20.0),
            commissionEnabled = mapOf(RideApp.UBER to true),
            fuelCostPerKm = 2.0
        )
        // fare = 120, totalKm = 10. grossPerKm = 12.0 (>= 10.0).
        val req = createBaseRequest(app = RideApp.UBER, isDetailOpen = true, fare = 120.0, totalKm = 10.0)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        // Because basisNet = false, grossPerKm 12.0 >= 10.0 -> accept_if_road_ok
        assertEquals(Tone.GREEN, verdict!!.tone)
        assertEquals("accept_if_road_ok", verdict.reasonCode)
    }

    // 48. Free pickup km waives pickup distance calculation
    @Test
    fun test48_freePickupKm_waivesPickupCalculation() {
        val policy = defaultPolicy.copy(minPricePerKm = 10.0, freePickupKm = 2.0)
        // pickupKm = 1.5 (< 2.0 free threshold) -> effectivePickup = 0.
        // tripKm = 10. totalKm = 10.0 (instead of 11.5).
        val req = createBaseRequest(isDetailOpen = true, pickupKm = 1.5, tripKm = 10.0, totalKm = null)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(10.0, verdict!!.totalKm!!, 0.01)
    }

    // 49. Stop fee increases required fare
    @Test
    fun test49_stopFee_increasesRequiredFare() {
        val policy = defaultPolicy.copy(minPricePerKm = 10.0, stopFee = 30.0, negotiationStep = 5)
        // totalKm = 10, extraStops = 2.
        // requiredFare = 10 * 10 + 30 * 2 = 160 EGP.
        val req = createBaseRequest(app = RideApp.INDRIVE, isDetailOpen = true, fare = 110.0, totalKm = 10.0, extraStops = 2)
        val verdict = RulesEngine.evaluate(req, policy)
        assertNotNull(verdict)
        assertEquals(160.0, verdict!!.suggestedPrice!!, 0.01)
    }

    // 50. Distance unit conversions: Miles and Both
    @Test
    fun test50_distanceUnit_conversions() {
        val policyMiles = defaultPolicy.copy(distanceUnit = DistanceUnit.MILES)
        val req = createBaseRequest(pickupKm = 10.0, totalKm = 16.09344)
        val verdictMiles = RulesEngine.evaluate(req, policyMiles)
        assertNotNull(verdictMiles)
        // 16.09344 km is exactly 10.0 miles
        assertTrue(verdictMiles!!.flags.any { it.contains("mi") })

        val policyBoth = defaultPolicy.copy(distanceUnit = DistanceUnit.BOTH)
        val verdictBoth = RulesEngine.evaluate(req, policyBoth)
        assertNotNull(verdictBoth)
        assertTrue(verdictBoth!!.flags.any { it.contains("كم") && it.contains("mi") })
    }

    // 51. DecisionStore debounce and blinks counter
    @Test
    fun test51_decisionStore_debounceAndBlinks() = runTest {
        var recordedBlinks = 0
        var recordedReasons = mutableListOf<String>()

        val store = DecisionStore(
            debounceDelayMs = 0L, // Immediate for test execution
            diagnosticCounterIncrementer = { counter ->
                if (counter == "blinks") recordedBlinks++
                if (counter.startsWith("verdict:")) recordedReasons.add(counter)
            }
        )

        val verdict1 = Verdict(
            tone = Tone.GREEN,
            reasonCode = "list_ok",
            titleAr = "مناسب",
            titleEn = "OK",
            detailAr = "",
            detailEn = "",
            flags = emptyList(),
            totalKm = 10.0,
            grossPerKm = 12.0,
            netPerKm = 10.0,
            netProfit = 80.0,
            suggestedPrice = null
        )

        store.postDecision("order_123", verdict1)
        val decisions1 = store.decisions.first()
        assertEquals(1, decisions1.size)
        assertEquals(Tone.GREEN, decisions1["order_123"]?.tone)

        // Change tone to RED for same key -> triggers a blink!
        val verdict2 = verdict1.copy(tone = Tone.RED, reasonCode = "avoided_area")
        store.postDecision("order_123", verdict2)
        val decisions2 = store.decisions.first()
        assertEquals(Tone.RED, decisions2["order_123"]?.tone)
        assertEquals(1, store.blinkCount)
        assertEquals(1, recordedBlinks)

        // Remove key
        store.remove("order_123")
        assertTrue(store.decisions.first().isEmpty())
    }

    // 52. OpportunityDetector emits green and throttles repeat for 60s
    @Test
    fun test52_opportunityDetector_throttling() = runTest {
        var currentTime = 1000L
        val detector = OpportunityDetector(
            alertCooldownMs = 60_000L,
            timeProvider = { currentTime }
        )

        val req = createBaseRequest(key = "card_999", isDetailOpen = true)
        val greenVerdict = Verdict(
            tone = Tone.GREEN,
            reasonCode = "accept_if_road_ok",
            titleAr = "",
            titleEn = "",
            detailAr = "",
            detailEn = "",
            flags = emptyList(),
            totalKm = 10.0,
            grossPerKm = 15.0,
            netPerKm = 12.0,
            netProfit = 100.0,
            suggestedPrice = null
        )

        // First event accepted
        val firstResult = detector.inspect(req, greenVerdict)
        assertTrue(firstResult)

        // Same key after 10 seconds -> rejected (in cooldown)
        currentTime += 10_000L
        val secondResult = detector.inspect(req, greenVerdict)
        assertFalse(secondResult)

        // Same key after 65 seconds -> accepted!
        currentTime += 55_000L // Total 65s later
        val thirdResult = detector.inspect(req, greenVerdict)
        assertTrue(thirdResult)
    }

    // 53. HomeWayScorer closer calculation
    @Test
    fun test53_homeWayScorer_calculation() = runTest {
        val scorer = HomeWayScorer()
        // Cairo Home coords
        val home = HomeWay(enabled = true, lat = 30.0444, lon = 31.2357, label = "Home")
        // Pickup far from home (30.15, 31.35)
        // Dest closer to home (30.05, 31.24)
        val score = scorer.scoreAsync(
            homeWay = home,
            pickupAddress = "30.15, 31.35",
            destinationAddress = "30.05, 31.24"
        )
        assertTrue(score is HomeWayScore.Closer)
        val percent = (score as HomeWayScore.Closer).percent
        assertTrue("Expected closer percent > 50, got $percent", percent > 50)

        // Trip moving away
        val scoreAway = scorer.scoreAsync(
            homeWay = home,
            pickupAddress = "30.05, 31.24",
            destinationAddress = "30.25, 31.50"
        )
        assertEquals(HomeWayScore.Away, scoreAway)
    }

    // 54. Null safety for every nullable field
    @Test
    fun test54_nullSafety_forAllNullableFields() {
        val barePolicy = Policy(minPricePerKm = 8.0)
        val bareRequest = ParsedRequest(
            app = RideApp.INDRIVE,
            key = "bare_key",
            source = ReadSource.ACCESSIBILITY,
            fare = 80.0,
            currency = null,
            pickupKm = 2.0,
            tripKm = null,
            totalKm = null,
            pickupMinutes = null,
            riderRating = null,
            riderTrips = null,
            requestAgeSeconds = null,
            pickupAddress = null,
            destinationAddress = null,
            extraStops = 0,
            paymentMethod = null,
            riderNote = null,
            offerCount = null,
            roadState = RoadState.UNKNOWN,
            productName = null,
            bounds = Rect(0, 0, 100, 100),
            isDetailOpen = false,
            confidence = 0.5f
        )
        val verdict = RulesEngine.evaluate(bareRequest, barePolicy)
        assertNotNull(verdict)
        assertEquals(Tone.GREEN, verdict!!.tone)
    }

    // 55. Motorcycle 150cc Net Profit Verification
    @Test
    fun test55_motorcycle150cc_netProfit_accurate() {
        // 150cc benchmark: fuel = 0.40, upkeep = 0.25 -> total vehicle cost = 0.65 EGP/km
        val motoPolicy = Policy(
            vehicleType = VehicleType.MOTORCYCLE_150CC,
            minPricePerKm = 5.0,
            basisNet = true
        )
        // Fare = 60 EGP, 10 km trip
        // With 0% commission (default map empty -> commissionFactor = 1.0):
        // netFare = 60. Vehicle expense = 0.65 * 10 = 6.5 EGP.
        // netProfit = 60 - 6.5 = 53.5 EGP.
        // netPerKm = 5.35 EGP/km >= 5.0 minPricePerKm -> GREEN accept_if_road_ok
        val req = createBaseRequest(isDetailOpen = true, fare = 60.0, totalKm = 10.0)
        val verdict = RulesEngine.evaluate(req, motoPolicy)
        assertNotNull(verdict)
        assertEquals(53.5, verdict!!.netProfit!!, 0.01)
        assertEquals(5.35, verdict.netPerKm!!, 0.01)
        assertEquals(Tone.GREEN, verdict.tone)
        assertEquals("accept_if_road_ok", verdict.reasonCode)
    }

    // 56. Motorcycle 250cc vs Car Comparison on identical fare
    @Test
    fun test56_motorcycle250cc_vs_car_expenseDifference() {
        // Motorcycle 250cc benchmark: fuel 0.70 + upkeep 0.40 = 1.10 EGP/km
        val moto250Policy = Policy(
            vehicleType = VehicleType.MOTORCYCLE_250CC,
            minPricePerKm = 4.0,
            basisNet = true
        )
        // Car benchmark: fuel 2.20 + upkeep 1.10 = 3.30 EGP/km
        val carPolicy = Policy(
            vehicleType = VehicleType.CAR,
            fuelCostPerKm = 2.20,
            upkeepCostPerKm = 1.10,
            minPricePerKm = 4.0,
            basisNet = true
        )

        // 10 km trip for 45 EGP
        val req = createBaseRequest(isDetailOpen = true, fare = 45.0, totalKm = 10.0)

        // For 250cc moto: cost = 1.10 * 10 = 11 EGP. netProfit = 34.0. netPerKm = 3.40.
        val motoVerdict = RulesEngine.evaluate(req, moto250Policy)
        assertEquals(34.0, motoVerdict!!.netProfit!!, 0.01)

        // For car: cost = 3.30 * 10 = 33 EGP. netProfit = 12.0. netPerKm = 1.20.
        val carVerdict = RulesEngine.evaluate(req, carPolicy)
        assertEquals(12.0, carVerdict!!.netProfit!!, 0.01)

        assertTrue(motoVerdict.netProfit!! > carVerdict.netProfit!!)
    }
}
