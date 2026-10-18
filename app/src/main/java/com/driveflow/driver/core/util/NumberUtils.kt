package com.driveflow.driver.core.util

object NumberUtils {
    /**
     * Replaces Eastern Arabic-Indic numerals (٠-٩) and Persian numerals (۰-۹)
     * with Western digits (0-9). Also normalizes Arabic decimal separator (٫) and comma (،) to dot (.).
     */
    fun normalizeArabicDigits(input: String): String {
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
                '٫', '،', ',' -> sb.append('.')
                else -> sb.append(char)
            }
        }
        return sb.toString().trim()
    }

    /**
     * Parses a string into Double, accepting Arabic-Indic digits and decimal separators.
     */
    fun parseDoubleOrNull(input: String?): Double? {
        if (input.isNullOrBlank()) return null
        val normalized = normalizeArabicDigits(input)
        return normalized.toDoubleOrNull()
    }

    /**
     * Parses a string into Int, accepting Arabic-Indic digits.
     */
    fun parseIntOrNull(input: String?): Int? {
        if (input.isNullOrBlank()) return null
        val normalized = normalizeArabicDigits(input)
        return normalized.toIntOrNull()
    }

    /**
     * Formats Double to a clean string with 1 or 2 decimal places if needed.
     */
    fun formatNumber(value: Double?): String {
        if (value == null) return ""
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            "%.1f".format(java.util.Locale.US, value)
        }
    }
}
