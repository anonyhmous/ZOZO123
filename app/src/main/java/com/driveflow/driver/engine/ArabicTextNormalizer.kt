package com.driveflow.driver.engine

object ArabicTextNormalizer {
    /**
     * Normalizes Arabic text for consistent searching and matching:
     * - Strips tashkeel (diacritics)
     * - Unifies all forms of Alef (أ, إ, آ -> ا)
     * - Unifies Alef Maqsura to Ya (ى -> ي)
     * - Unifies Ta Marbuta to Ha (ة -> ه)
     * - Removes Tatweel / Kashida (ـ)
     * - Converts Latin text to lowercase
     * - Normalizes multiple spaces and punctuation
     */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val sb = StringBuilder(input.length)
        for (char in input) {
            when (char) {
                // Diacritics (Tashkeel) to skip
                '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650', '\u0651', '\u0652',
                '\u0653', '\u0654', '\u0655', '\u0670' -> continue
                // Tatweel / Kashida
                '\u0640' -> continue
                // Alef variations -> Bare Alef
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                // Alef Maqsura -> Ya
                'ى' -> sb.append('ي')
                // Ta Marbuta -> Ha
                'ة' -> sb.append('ه')
                // Normalize special Arabic punctuation to space
                '،', '؛', '؟' -> sb.append(' ')
                else -> sb.append(char.lowercaseChar())
            }
        }
        // Replace consecutive whitespace with single space and trim
        return sb.toString().replace(Regex("""\s+"""), " ").trim()
    }

    /**
     * Checks if any area in targetAreas matches as a substring within the given address.
     */
    fun matchesAnyArea(address: String?, targetAreas: List<String>): String? {
        if (address.isNullOrBlank() || targetAreas.isEmpty()) return null
        val normAddress = normalize(address)
        if (normAddress.isEmpty()) return null
        for (area in targetAreas) {
            val normArea = normalize(area)
            if (normArea.isNotEmpty() && normAddress.contains(normArea)) {
                return area
            }
        }
        return null
    }
}
