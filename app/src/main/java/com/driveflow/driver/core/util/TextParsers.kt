package com.driveflow.driver.core.util

import com.driveflow.driver.core.model.DistanceUnit

data class DistanceResult(
    val km: Double?,
    val originalUnit: DistanceUnit
)

object TextParsers {
    /**
     * Converts Eastern Arabic-Indic digits (٠-٩) and Persian digits (۰-۹) to ASCII (0-9).
     * Normalizes Arabic decimal separator (٫) to dot (.).
     * Strips non-breaking spaces, thin spaces, and zero-width spaces.
     */
    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (char in input) {
            when (char) {
                '٠', '۰' -> sb.append('0')
                '١', '۱' -> sb.append('1')
                '٢', '۲' -> sb.append('2')
                '٣', '۳' -> sb.append('3')
                '٤', '۴' -> sb.append('4')
                '٥', '۵' -> sb.append('5')
                '٦', '۶' -> sb.append('6')
                '٧', '۷' -> sb.append('7')
                '٨', '۸' -> sb.append('8')
                '٩', '۹' -> sb.append('9')
                '٫' -> sb.append('.')
                '،' -> sb.append(',')
                // Remove invisible/special whitespace: NBSP, NNBSP, thin spaces, zero-width spaces
                '\u00A0', '\u202F', '\u2009', '\u200A', '\u200B', '\uFEFF' -> sb.append(' ')
                else -> sb.append(char)
            }
        }
        return sb.toString().trim()
    }

    /**
     * Parses numeric values with intelligence regarding thousands vs decimal separators.
     * Examples:
     * - "1,250" -> 1250.0 (3 digits after separator)
     * - "1.250" -> 1250.0 (3 digits after separator, commonly used in thousands grouping in Egypt/Europe)
     * - "12,5" -> 12.5 (1 digit after separator)
     * - "12.5" -> 12.5
     * - "1 250" -> 1250.0
     * - "١٢٫٥" -> 12.5
     */
    fun parseNumber(input: String?): Double? {
        if (input.isNullOrBlank()) return null
        val normalized = normalizeDigits(input)
        // Extract first sequence containing digits, dots, commas, spaces
        val regex = Regex("""[0-9]+([.,\s][0-9]+)*""")
        val match = regex.find(normalized)?.value?.trim() ?: return null
        val cleanedSpaces = match.replace(" ", "")

        // Check if there are both dot and comma
        val hasDot = cleanedSpaces.contains('.')
        val hasComma = cleanedSpaces.contains(',')
        if (hasDot && hasComma) {
            val lastDot = cleanedSpaces.lastIndexOf('.')
            val lastComma = cleanedSpaces.lastIndexOf(',')
            return if (lastDot > lastComma) {
                // Comma is thousand separator, dot is decimal: "1,250.50"
                cleanedSpaces.replace(",", "").toDoubleOrNull()
            } else {
                // Dot is thousand separator, comma is decimal: "1.250,50"
                cleanedSpaces.replace(".", "").replace(",", ".").toDoubleOrNull()
            }
        }

        // Only one separator type
        val separator = if (hasDot) '.' else if (hasComma) ',' else null
        if (separator != null) {
            val occurrences = cleanedSpaces.count { it == separator }
            if (occurrences > 1) {
                // Multiple separators = thousands separators: "1,000,000" or "1.000.000"
                return cleanedSpaces.replace(separator.toString(), "").toDoubleOrNull()
            }

            // Single separator: check digits after separator
            val parts = cleanedSpaces.split(separator)
            val afterSeparator = parts.getOrNull(1) ?: ""
            return if (afterSeparator.length == 3 && parts[0].isNotEmpty() && parts[0].length in 1..3) {
                // Exactly 3 digits and preceding 1-3 digits: Treat as thousands separator "1,250" or "1.250" -> 1250.0
                cleanedSpaces.replace(separator.toString(), "").toDoubleOrNull()
            } else {
                // Decimal separator: "12,5" -> 12.5, "2.4" -> 2.4
                cleanedSpaces.replace(',', '.').toDoubleOrNull()
            }
        }

        return cleanedSpaces.toDoubleOrNull()
    }

    /**
     * Parses distance from text. Supports:
     * - km: كم, km, kms
     * - miles: ميل, mi, mile, miles (1 mile = 1.609344 km)
     * - meters: متر, m (meters / 1000.0)
     */
    fun parseDistance(input: String?): DistanceResult? {
        if (input.isNullOrBlank()) return null
        val normalized = normalizeDigits(input)
        val lower = normalized.lowercase()
        val num = parseNumber(normalized) ?: return null

        return when {
            // Miles
            lower.contains("ميل") || lower.contains("ميلا") || lower.contains("أميال") ||
            Regex("""\b(mi|mile|miles)\b""").containsMatchIn(lower) -> {
                DistanceResult(km = num * 1.609344, originalUnit = DistanceUnit.MILES)
            }
            // Meters: must be careful not to match 'm' when part of 'km' or word
            lower.contains("متر") ||
            ((Regex("""\d+\s*م(?![ا-يa-zA-Z])""").containsMatchIn(lower) || Regex("""\b\d+\s*m\b""").containsMatchIn(lower))
                && !lower.contains("كم") && !lower.contains("km") && !lower.contains("ميل")) -> {
                DistanceResult(km = num / 1000.0, originalUnit = DistanceUnit.KM)
            }
            // Kilometers (default)
            else -> {
                DistanceResult(km = num, originalUnit = DistanceUnit.KM)
            }
        }
    }

    /**
     * Parses request age in seconds from text.
     * - الآن / now / just now -> 0
     * - 3 دقائق / 3 min -> 180
     * - 1 ساعة / 1 hr -> 3600
     */
    fun parseAgeSeconds(input: String?): Int? {
        if (input.isNullOrBlank()) return null
        val normalized = normalizeDigits(input)
        val lower = normalized.lowercase()

        if (lower.contains("الآن") || lower.contains("الان") || lower.contains("منذ قليل") ||
            lower.contains("توا") || lower.contains("now") || lower.contains("just now")) {
            return 0
        }

        val num = parseNumber(normalized)?.toInt() ?: return 0

        return when {
            lower.contains("يوم") || lower.contains("أيام") || lower.contains("ايام") ||
            Regex("""\b(day|days|d)\b""").containsMatchIn(lower) -> num * 86400

            lower.contains("ساعة") || lower.contains("ساعات") || lower.contains("س") ||
            Regex("""\b(hour|hours|hr|hrs|h)\b""").containsMatchIn(lower) -> num * 3600

            lower.contains("دقيقة") || lower.contains("دقائق") || lower.contains("د") ||
            Regex("""\b(min|mins|m)\b""").containsMatchIn(lower) -> num * 60

            lower.contains("ثانية") || lower.contains("ثواني") || lower.contains("ث") ||
            Regex("""\b(sec|secs|s)\b""").containsMatchIn(lower) -> num

            else -> num * 60 // Default to minutes if unit ambiguous in driver apps
        }
    }

    /**
     * Parses rider rating (0.0 to 5.0).
     */
    fun parseRating(input: String?): Double? {
        if (input.isNullOrBlank()) return null
        val num = parseNumber(input) ?: return null
        return if (num in 0.0..5.0) num else null
    }

    /**
     * Parses rider completed trips count (handles "(120)", "120 trips", "120 رحلة").
     */
    fun parseTrips(input: String?): Int? {
        if (input.isNullOrBlank()) return null
        val normalized = normalizeDigits(input)
        val digits = Regex("""\d+""").find(normalized)?.value
        return digits?.toIntOrNull()
    }

    /**
     * Parses fare price and detects currency code.
     * Handles: "1,250 ج.م", "65.5 EGP", "45 LE", "80 جنيه".
     */
    fun parseFare(input: String?): Pair<Double?, String?> {
        if (input.isNullOrBlank()) return null to null
        val normalized = normalizeDigits(input)
        val currency = when {
            normalized.contains("USD") || normalized.contains("$") -> "USD"
            normalized.contains("SAR") || normalized.contains("ريال") -> "SAR"
            normalized.contains("AED") || normalized.contains("درهم") -> "AED"
            else -> "EGP"
        }

        // Clean currency keywords to isolate number
        val cleaned = normalized
            .replace("ج.م", "")
            .replace("جنيه", "")
            .replace("جنية", "")
            .replace("EGP", "", ignoreCase = true)
            .replace("LE", "", ignoreCase = true)
            .replace("L.E", "", ignoreCase = true)
            .trim()

        val fare = parseNumber(cleaned)
        return fare to currency
    }
}
