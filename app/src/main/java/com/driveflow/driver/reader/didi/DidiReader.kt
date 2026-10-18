package com.driveflow.driver.reader.didi

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.driveflow.driver.core.model.AppReader
import com.driveflow.driver.core.model.ParsedRequest
import com.driveflow.driver.core.model.ReadSource
import com.driveflow.driver.core.model.RideApp
import com.driveflow.driver.core.model.RoadState
import com.driveflow.driver.core.util.TextParsers
import com.driveflow.driver.ocr.OcrEngine
import com.driveflow.driver.ocr.OcrResult
import java.security.MessageDigest
import kotlin.math.abs

/**
 * DiDi Reader parsing ride requests via Screenshot + OCR.
 * Enforces per-km cross check: reduces confidence if printed rate and fare/km differ by > 20%.
 */
class DidiReader(
    private val ocrEngine: OcrEngine? = null
) : AppReader {
    override val app: RideApp = RideApp.DIDI

    companion object {
        val RATING_REGEX = Regex("""(?<![\d.,])[1-5][.,]\d{1,2}(?![\d.,])""")
        // Matches printed rate per km, e.g. "12.5 ج.م/كم" or "10.00 EGP/km" or "14.2 جنيه / كم"
        val RATE_PER_KM_REGEX = Regex(
            """(\d+(?:[.,]\d+)?)\s*(?:ج.م|EGP|LE|جنيه)?\s*(?:/|لكل)\s*(?:كم|km)""",
            RegexOption.IGNORE_CASE
        )
        val DISTANCE_DURATION_REGEX = Regex(
            """(\d+)\s*(?:min|mins|د|m)\s*\(([^)]+)\)""",
            RegexOption.IGNORE_CASE
        )
    }

    override fun isRideAppWindow(root: AccessibilityNodeInfo): Boolean {
        val pkg = root.packageName?.toString() ?: ""
        return pkg == RideApp.DIDI.pkg
    }

    override fun readList(root: AccessibilityNodeInfo): List<ParsedRequest> = emptyList()

    override fun readDetail(root: AccessibilityNodeInfo): ParsedRequest? = null

    override fun mapBounds(root: AccessibilityNodeInfo): Rect? {
        val screenBounds = Rect()
        root.getBoundsInScreen(screenBounds)
        return Rect(screenBounds.left, screenBounds.top, screenBounds.right, (screenBounds.height() * 0.40f).toInt())
    }

    /**
     * Parses OCR lines extracted from a DiDi Driver screen capture.
     */
    fun parseOcrResult(ocrResult: OcrResult, screenBounds: Rect = Rect(0, 0, 1080, 2400)): ParsedRequest? {
        val lines = ocrResult.lines
        if (lines.isEmpty()) return null
        val fullText = ocrResult.fullText

        // 1. Parse Fare & Currency
        var fare: Double? = null
        var currency: String? = "EGP"
        for (line in lines) {
            val (parsedFare, parsedCur) = TextParsers.parseFare(line.text)
            if (parsedFare != null && parsedFare > 5.0) {
                fare = parsedFare
                if (parsedCur != null) currency = parsedCur
                break
            }
        }

        // 2. Parse printed per-km rate if displayed
        var printedRatePerKm: Double? = null
        val rateMatch = RATE_PER_KM_REGEX.find(fullText)
        if (rateMatch != null) {
            printedRatePerKm = TextParsers.parseNumber(rateMatch.groupValues[1])
        }

        // 3. Pickup and trip distances
        val distanceMatches = DISTANCE_DURATION_REGEX.findAll(fullText).toList()
        var pickupMinutes: Int? = null
        var pickupKm: Double? = null
        var tripKm: Double? = null

        if (distanceMatches.isNotEmpty()) {
            val first = distanceMatches[0]
            pickupMinutes = TextParsers.normalizeDigits(first.groupValues[1]).toIntOrNull()
            pickupKm = TextParsers.parseDistance(first.groupValues[2])?.km
            if (distanceMatches.size > 1) {
                val second = distanceMatches[1]
                tripKm = TextParsers.parseDistance(second.groupValues[2])?.km
            }
        } else {
            val foundDistances = lines.mapNotNull { line -> TextParsers.parseDistance(line.text)?.km }
            if (foundDistances.isNotEmpty()) {
                pickupKm = foundDistances[0]
                if (foundDistances.size > 1) {
                    tripKm = foundDistances[1]
                }
            }
        }

        val totalKm = if (pickupKm != null && tripKm != null) {
            pickupKm + tripKm
        } else {
            tripKm ?: pickupKm
        }

        if (fare == null && totalKm == null) {
            return null
        }

        // 4. Rider Rating
        var riderRating: Double? = null
        val ratingLine = lines.firstOrNull { line ->
            (line.text.contains("★") || line.text.contains("⭐") || line.text.contains("*")) &&
                RATING_REGEX.containsMatchIn(line.text)
        }
        if (ratingLine != null) {
            val match = RATING_REGEX.find(ratingLine.text)
            if (match != null) {
                riderRating = TextParsers.parseNumber(match.value)
            }
        }
        if (riderRating == null) {
            for (line in lines) {
                val t = line.text.lowercase()
                if (t.contains("km") || t.contains("كم") || t.contains("min") || t.contains("د") ||
                    t.contains("egp") || t.contains("ج.م") || t.contains("/") || t.contains("لكل")) {
                    continue
                }
                val match = RATING_REGEX.find(line.text)
                if (match != null) {
                    val num = TextParsers.parseNumber(match.value)
                    if (num != null && num in 1.0..5.99) {
                        riderRating = num
                        break
                    }
                }
            }
        }

        // 5. Addresses
        val addressLines = lines.filter { line ->
            val t = line.text.trim()
            t.length > 5 &&
                !RATING_REGEX.containsMatchIn(t) &&
                !RATE_PER_KM_REGEX.containsMatchIn(t) &&
                !DISTANCE_DURATION_REGEX.containsMatchIn(t) &&
                TextParsers.parseFare(t).first == null
        }
        val pickupAddress = addressLines.getOrNull(0)?.text?.trim()
        val destinationAddress = addressLines.getOrNull(1)?.text?.trim()

        // 6. Confidence & Cross-Check Calculation
        var confidence = ocrResult.meanConfidence.coerceIn(0.4f, 1.0f)
        // Cross check fare / totalKm against printed per-km rate
        if (fare != null && totalKm != null && totalKm > 0.1 && printedRatePerKm != null && printedRatePerKm > 0.0) {
            val computedRate = fare / totalKm
            val differencePct = abs(computedRate - printedRatePerKm) / printedRatePerKm
            if (differencePct > 0.20) {
                confidence *= 0.65f
            }
        }

        val cardBounds = if (lines.isNotEmpty()) {
            val left = lines.minOf { it.boundsPx.left }
            val top = lines.minOf { it.boundsPx.top }
            val right = lines.maxOf { it.boundsPx.right }
            val bottom = lines.maxOf { it.boundsPx.bottom }
            Rect(left, top, right, bottom)
        } else {
            screenBounds
        }

        val key = computeHash(fare, pickupKm, tripKm, pickupAddress)
        return ParsedRequest(
            app = RideApp.DIDI,
            key = key,
            source = ReadSource.OCR,
            fare = fare,
            currency = currency,
            pickupKm = pickupKm,
            tripKm = tripKm,
            totalKm = totalKm,
            pickupMinutes = pickupMinutes,
            riderRating = riderRating,
            riderTrips = null,
            requestAgeSeconds = 0,
            pickupAddress = pickupAddress,
            destinationAddress = destinationAddress,
            extraStops = 0,
            paymentMethod = null,
            riderNote = null,
            offerCount = null,
            roadState = RoadState.UNKNOWN,
            productName = "DiDi",
            bounds = cardBounds,
            isDetailOpen = false,
            confidence = confidence
        )
    }

    private fun computeHash(fare: Double?, pickupKm: Double?, tripKm: Double?, address: String?): String {
        val raw = "didi_${fare ?: 0.0}_${pickupKm ?: 0.0}_${tripKm ?: 0.0}_${address ?: ""}"
        val md = MessageDigest.getInstance("MD5")
        return md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
