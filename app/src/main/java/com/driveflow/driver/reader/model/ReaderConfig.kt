package com.driveflow.driver.reader.model

import kotlinx.serialization.Serializable

@Serializable
data class FieldFallback(
    val alternativeIds: List<String> = emptyList(),
    val textPattern: String? = null
)

@Serializable
data class ReaderConfig(
    val minRevision: Int = 1,
    val appCode: String = "",
    val packageName: String = "",
    val appVersion: String = "",
    val idPrefix: String = "",
    val ids: Map<String, String> = emptyMap(),
    val fallbacks: Map<String, FieldFallback> = emptyMap()
) {
    fun getFullId(key: String): String {
        val rawId = ids[key] ?: return ""
        return if (rawId.contains(":id/")) rawId else "$idPrefix$rawId"
    }

    fun getAlternativeFullIds(key: String): List<String> {
        val fallback = fallbacks[key] ?: return emptyList()
        return fallback.alternativeIds.map { altId ->
            if (altId.contains(":id/")) altId else "$idPrefix$altId"
        }
    }
}
