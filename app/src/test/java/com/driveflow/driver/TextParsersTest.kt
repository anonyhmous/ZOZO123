package com.driveflow.driver

import com.driveflow.driver.core.model.DistanceUnit
import com.driveflow.driver.core.util.TextParsers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TextParsersTest {

    @Test
    fun testNormalizeDigits() {
        val input = "١٢٫٥ كم"
        val normalized = TextParsers.normalizeDigits(input)
        assertEquals("12.5 كم", normalized)
    }

    @Test
    fun testParseNumber() {
        assertEquals(1250.0, TextParsers.parseNumber("1,250")!!, 0.001)
        assertEquals(1250.0, TextParsers.parseNumber("1.250")!!, 0.001)
        assertEquals(12.5, TextParsers.parseNumber("12,5")!!, 0.001)
        assertEquals(12.5, TextParsers.parseNumber("12.5")!!, 0.001)
        assertEquals(1250.0, TextParsers.parseNumber("1 250")!!, 0.001)
        assertEquals(12.5, TextParsers.parseNumber("١٢٫٥")!!, 0.001)
    }

    @Test
    fun testParseDistance_arabicKm() {
        val res = TextParsers.parseDistance("١٢٫٥ كم")
        assertNotNull(res)
        assertEquals(12.5, res!!.km!!, 0.001)
        assertEquals(DistanceUnit.KM, res.originalUnit)
    }

    @Test
    fun testParseDistance_meters() {
        val res = TextParsers.parseDistance("850 متر")
        assertNotNull(res)
        assertEquals(0.85, res!!.km!!, 0.001)
    }

    @Test
    fun testParseDistance_miles() {
        val res = TextParsers.parseDistance("2.4 mi")
        assertNotNull(res)
        assertEquals(3.862, res!!.km!!, 0.01)
        assertEquals(DistanceUnit.MILES, res.originalUnit)
    }

    @Test
    fun testParseAgeSeconds_minutes() {
        val age = TextParsers.parseAgeSeconds("3 دقائق")
        assertEquals(180, age)
    }

    @Test
    fun testParseAgeSeconds_now() {
        val age = TextParsers.parseAgeSeconds("الآن")
        assertEquals(0, age)
        val ageEnglish = TextParsers.parseAgeSeconds("just now")
        assertEquals(0, ageEnglish)
    }

    @Test
    fun testParseRating() {
        val rating = TextParsers.parseRating("4.9")
        assertEquals(4.9, rating!!, 0.001)
        val ratingArabic = TextParsers.parseRating("٤٫٨")
        assertEquals(4.8, ratingArabic!!, 0.001)
    }

    @Test
    fun testParseTrips() {
        val trips = TextParsers.parseTrips("(120)")
        assertEquals(120, trips)
        val tripsText = TextParsers.parseTrips("350 رحلة")
        assertEquals(350, tripsText)
    }

    @Test
    fun testParseFare() {
        val (fare, currency) = TextParsers.parseFare("1,250 ج.م")
        assertEquals(1250.0, fare!!, 0.001)
        assertEquals("EGP", currency)

        val (fare2, currency2) = TextParsers.parseFare("85.50 EGP")
        assertEquals(85.5, fare2!!, 0.001)
        assertEquals("EGP", currency2)
    }
}
