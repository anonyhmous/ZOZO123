package com.driveflow.driver.reader.uber

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

/**
 * Uber Reader parsing ride requests via Screenshot + OCR.
 */
class UberReader(
    private val ocrEngine: OcrEngine? = null
) : AppReader {
    override val app: RideApp = RideApp.UBER

    companion object {
        val PRODUCT_REGEX = Regex(
            """(?i)\b(uber\s*[a-z0-9]+|saver|uberx\s*saver|comfort|scooter|moto|select|\bxl\b|uberx|uberblack)\b"""
        )
        val RATING_REGEX = Regex("""(?<![\d.,])[1-5][.,]\d{2}(?![\d.,])""")
        // Pattern: "6 min (2.0 km) away" or "6 د (2.0 كم)"
        val DISTANCE_DURATION_REGEX = Regex(
            """(\d+)\s*(?:min|mins|د|m)\s*\(([^)]+)\)""",
            RegexOption.IGNORE_CASE
        )
        val STOPS_REGEX = Regex("""(?i)\b(stops|stop|وقفات|وقفة)\b""")
    }

    override fun isRideAppWindow(root: AccessibilityNodeInfo): Boolean {
        val pkg = root.packageName?.toString() ?: ""
        return pkg == RideApp.UBER.pkg
    }

    override fun readList(root: AccessibilityNodeInfo): List<ParsedRequest> {
        return emptyList()
    }

    override fun readDetail(root: AccessibilityNodeInfo): ParsedRequest? {
        return null
    }

    override fun mapBounds(root: AccessibilityNodeInfo): Rect? {
        val screenBounds = Rect()
        root.getBoundsInScreen(screenBounds)
        return Rect(screenBounds.left, screenBounds.top, screenBounds.right, (screenBounds.height() * 0.45f).toInt())
    }

    /**
     * Parses OCR output lines extracted from an Uber Driver screen capture.
     */
    fun parseOcrResult(ocrResult: OcrResult, screenBounds: Rect = Rect(0, 0, 1080, 2400)): ParsedRequest? {
        val lines = ocrResult.lines
        if (lines.isEmpty()) return null
        val fullText = ocrResult.fullText

        // 1. Detect product name (e.g., UberX, Saver, Comfort, Moto)
        var matchedProduct: String? = null
        for (line in lines) {
            val match = PRODUCT_REGEX.find(line.text)
            if (match != null) {
                matchedProduct = match.value.trim()
                break
            }
        }

        // 2. Detect Fare & Currency
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

        // Must have at least fare or product to identify as an Uber request card
        if (fare == null && matchedProduct == null) {
            return null
        }

        // 3. Detect pickup and trip distances
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

        // 4. Rider Rating (1.00 to 5.99)
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
                    t.contains("egp") || t.contains("ج.م")) {
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

        // 5. Extra Stops
        val hasStops = fullText.contains("stops", ignoreCase = true) ||
            fullText.contains("stop", ignoreCase = true) ||
            fullText.contains("وقفات") ||
            fullText.contains("وقفة") ||
            fullText.contains("نقطة توقف")
        val extraStops = if (hasStops) 1 else 0

        // 6. Addresses
        val addressLines = lines.filter { line ->
            val t = line.text.trim()
            t.length > 5 &&
                !PRODUCT_REGEX.containsMatchIn(t) &&
                !RATING_REGEX.containsMatchIn(t) &&
                !DISTANCE_DURATION_REGEX.containsMatchIn(t) &&
                TextParsers.parseFare(t).first == null &&
                !t.contains("EGP", ignoreCase = true)
        }
        val pickupAddress = addressLines.getOrNull(0)?.text?.trim()
        val destinationAddress = addressLines.getOrNull(1)?.text?.trim()

        val roadState = RoadState.UNKNOWN
        val usedLinesConfidence = ocrResult.meanConfidence.coerceIn(0.4f, 1.0f)
        val cardBounds = if (lines.isNotEmpty()) {
            val left = lines.minOf { it.boundsPx.left }
            val top = lines.minOf { it.boundsPx.top }
            val right = lines.maxOf { it.boundsPx.right }
            val bottom = lines.maxOf { it.boundsPx.bottom }
            Rect(left, top, right, bottom)
        } else {
            screenBounds
        }

        val key = computeHash(fare, pickupKm, tripKm, matchedProduct, pickupAddress)
        return ParsedRequest(
            app = RideApp.UBER,
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
            extraStops = extraStops,
            paymentMethod = null,
            riderNote = null,
            offerCount = null,
            roadState = roadState,
            productName = matchedProduct ?: "Uber",
            bounds = cardBounds,
            isDetailOpen = false,
            confidence = usedLinesConfidence
        )
    }

    private fun computeHash(fare: Double?, pickupKm: Double?, tripKm: Double?, product: String?, address: String?): String {
        val raw = "uber_${fare ?: 0.0}_${pickupKm ?: 0.0}_${tripKm ?: 0.0}_${product ?: ""}_${address ?: ""}"
        val md = MessageDigest.getInstance("MD5")
        return md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
