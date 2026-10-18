package com.driveflow.driver.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.driveflow.driver.core.model.Policy
import com.driveflow.driver.core.model.RideApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "driveflow_settings")

@Serializable
enum class MarkerSize {
    SMALL,
    NORMAL,
    LARGE
}

@Serializable
enum class AlertMode {
    TONE,
    TONE_VIBRATE
}

@Serializable
data class BarPosition(
    val isAuto: Boolean = true,
    val x: Int = 0,
    val y: Int = 0
)

@Serializable
data class HomeWay(
    val enabled: Boolean = false,
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val label: String = ""
)

@Serializable
data class FractionRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
)

@Serializable
data class OcrCalibration(
    val regions: Map<String, FractionRect> = emptyMap(),
    val offsetX: Int = 0,
    val offsetY: Int = 0,
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val extraParams: Map<String, String> = emptyMap()
)

class SettingsStore(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val secureSharedPrefs: SharedPreferences =
        context.getSharedPreferences("driveflow_license_secure", Context.MODE_PRIVATE)

    companion object {
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_POLICY_JSON = stringPreferencesKey("policy_json")
        private val KEY_MARKER_SIZE = stringPreferencesKey("marker_size")
        private val KEY_BAR_OPACITY_PERCENT = intPreferencesKey("bar_opacity_percent")
        private val KEY_BAR_POSITION = stringPreferencesKey("bar_position")
        private val KEY_AUTO_OFF_HOURS = intPreferencesKey("auto_off_hours")
        private val KEY_ALERT_ENABLED = booleanPreferencesKey("alert_enabled")
        private val KEY_ALERT_MODE = stringPreferencesKey("alert_mode")
        private val KEY_HOME_WAY = stringPreferencesKey("home_way")
        private val KEY_CONSENT_VERSION = intPreferencesKey("consent_version")
        private val KEY_WELCOME_SEEN = booleanPreferencesKey("welcome_seen")
        private val KEY_SETUP_DONE = booleanPreferencesKey("setup_done")
        private val KEY_CONTACT_NAME = stringPreferencesKey("contact_name")
        private val KEY_CONTACT_PHONE = stringPreferencesKey("contact_phone")
        private val KEY_LICENSE_TOKEN = stringPreferencesKey("license_token")
        private val KEY_CLOCK_HIGH_WATER_DAY = intPreferencesKey("clock_high_water_day")
        private val KEY_OCR_CALIBRATION_JSON = stringPreferencesKey("ocr_calibration_map")
        private val KEY_READER_CONFIG_OVERRIDE_JSON = stringPreferencesKey("reader_config_override_map")
        private val KEY_DIAGNOSTICS_COUNTERS_JSON = stringPreferencesKey("diagnostics_counters_map")
        private val KEY_CHAT_HISTORY_JSON = stringPreferencesKey("chat_history_list")

        const val SP_KEY_PRO_ACTIVE = "sp_is_pro_active"
        const val SP_KEY_LICENSE_TOKEN = "sp_license_token"
        const val SP_KEY_EXPIRES_DAY = "sp_expires_day"
        const val SP_KEY_PLAN = "sp_plan"

        const val DEFAULT_LANGUAGE = "ar"
        const val DEFAULT_BAR_OPACITY = 67
        const val DEFAULT_AUTO_OFF_HOURS = 3
        const val MAX_CHAT_HISTORY = 100
    }

    private val dataStore = context.dataStore

    private val preferencesFlow: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val languageFlow: Flow<String> = preferencesFlow.map { prefs ->
        prefs[KEY_LANGUAGE] ?: DEFAULT_LANGUAGE
    }

    suspend fun setLanguage(language: String) {
        dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language
        }
    }

    val policyFlow: Flow<Policy> = preferencesFlow.map { prefs ->
        val rawJson = prefs[KEY_POLICY_JSON]
        if (rawJson.isNullOrBlank()) {
            Policy()
        } else {
            try {
                json.decodeFromString<Policy>(rawJson)
            } catch (e: Exception) {
                Policy()
            }
        }
    }

    val policyJsonFlow: Flow<String> = preferencesFlow.map { prefs ->
        prefs[KEY_POLICY_JSON] ?: json.encodeToString(Policy())
    }

    suspend fun setPolicy(policy: Policy) {
        val serialized = json.encodeToString(policy)
        dataStore.edit { prefs ->
            prefs[KEY_POLICY_JSON] = serialized
        }
    }

    suspend fun setPolicyJson(policyJson: String) {
        dataStore.edit { prefs ->
            prefs[KEY_POLICY_JSON] = policyJson
        }
    }

    val markerSizeFlow: Flow<MarkerSize> = preferencesFlow.map { prefs ->
        val value = prefs[KEY_MARKER_SIZE]
        if (value != null) {
            try {
                MarkerSize.valueOf(value)
            } catch (e: Exception) {
                MarkerSize.NORMAL
            }
        } else {
            MarkerSize.NORMAL
        }
    }

    suspend fun setMarkerSize(size: MarkerSize) {
        dataStore.edit { prefs ->
            prefs[KEY_MARKER_SIZE] = size.name
        }
    }

    val barOpacityPercentFlow: Flow<Int> = preferencesFlow.map { prefs ->
        prefs[KEY_BAR_OPACITY_PERCENT] ?: DEFAULT_BAR_OPACITY
    }

    suspend fun setBarOpacityPercent(opacity: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_BAR_OPACITY_PERCENT] = opacity.coerceIn(10, 100)
        }
    }

    val barPositionFlow: Flow<BarPosition> = preferencesFlow.map { prefs ->
        val raw = prefs[KEY_BAR_POSITION]
        if (raw.isNullOrBlank()) {
            BarPosition(isAuto = true)
        } else {
            try {
                json.decodeFromString<BarPosition>(raw)
            } catch (e: Exception) {
                BarPosition(isAuto = true)
            }
        }
    }

    suspend fun setBarPosition(barPosition: BarPosition) {
        dataStore.edit { prefs ->
            prefs[KEY_BAR_POSITION] = json.encodeToString(barPosition)
        }
    }

    val autoOffHoursFlow: Flow<Int> = preferencesFlow.map { prefs ->
        prefs[KEY_AUTO_OFF_HOURS] ?: DEFAULT_AUTO_OFF_HOURS
    }

    suspend fun setAutoOffHours(hours: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_OFF_HOURS] = hours
        }
    }

    val alertEnabledFlow: Flow<Boolean> = preferencesFlow.map { prefs ->
        prefs[KEY_ALERT_ENABLED] ?: true
    }

    suspend fun setAlertEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_ALERT_ENABLED] = enabled
        }
    }

    val alertModeFlow: Flow<AlertMode> = preferencesFlow.map { prefs ->
        val raw = prefs[KEY_ALERT_MODE]
        if (raw != null) {
            try {
                AlertMode.valueOf(raw)
            } catch (e: Exception) {
                AlertMode.TONE_VIBRATE
            }
        } else {
            AlertMode.TONE_VIBRATE
        }
    }

    suspend fun setAlertMode(mode: AlertMode) {
        dataStore.edit { prefs ->
            prefs[KEY_ALERT_MODE] = mode.name
        }
    }

    val homeWayFlow: Flow<HomeWay> = preferencesFlow.map { prefs ->
        val raw = prefs[KEY_HOME_WAY]
        if (raw.isNullOrBlank()) {
            HomeWay()
        } else {
            try {
                json.decodeFromString<HomeWay>(raw)
            } catch (e: Exception) {
                HomeWay()
            }
        }
    }

    suspend fun setHomeWay(homeWay: HomeWay) {
        dataStore.edit { prefs ->
            prefs[KEY_HOME_WAY] = json.encodeToString(homeWay)
        }
    }

    val consentVersionFlow: Flow<Int> = preferencesFlow.map { prefs ->
        prefs[KEY_CONSENT_VERSION] ?: 0
    }

    suspend fun setConsentVersion(version: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_CONSENT_VERSION] = version
        }
    }

    val welcomeSeenFlow: Flow<Boolean> = preferencesFlow.map { prefs ->
        prefs[KEY_WELCOME_SEEN] ?: false
    }

    suspend fun setWelcomeSeen(seen: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_WELCOME_SEEN] = seen
        }
    }

    val setupDoneFlow: Flow<Boolean> = preferencesFlow.map { prefs ->
        prefs[KEY_SETUP_DONE] ?: false
    }

    suspend fun setSetupDone(done: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SETUP_DONE] = done
        }
    }

    val contactNameFlow: Flow<String> = preferencesFlow.map { prefs ->
        prefs[KEY_CONTACT_NAME] ?: ""
    }

    suspend fun setContactName(name: String) {
        dataStore.edit { prefs ->
            prefs[KEY_CONTACT_NAME] = name
        }
    }

    val contactPhoneFlow: Flow<String> = preferencesFlow.map { prefs ->
        prefs[KEY_CONTACT_PHONE] ?: ""
    }

    suspend fun setContactPhone(phone: String) {
        dataStore.edit { prefs ->
            prefs[KEY_CONTACT_PHONE] = phone
        }
    }

    val licenseTokenFlow: Flow<String?> = preferencesFlow.map { prefs ->
        val fromDataStore = prefs[KEY_LICENSE_TOKEN]
        if (!fromDataStore.isNullOrBlank()) {
            fromDataStore
        } else {
            val fromSp = secureSharedPrefs.getString(SP_KEY_LICENSE_TOKEN, null)
            fromSp
        }
    }

    fun isProActiveSync(): Boolean {
        return secureSharedPrefs.getBoolean(SP_KEY_PRO_ACTIVE, false)
    }

    fun getLicenseTokenSync(): String? {
        return secureSharedPrefs.getString(SP_KEY_LICENSE_TOKEN, null)
    }

    suspend fun setLicenseToken(token: String?) {
        // Securely write to SharedPreferences to immediately persist activation across launches
        secureSharedPrefs.edit().apply {
            if (token == null) {
                putBoolean(SP_KEY_PRO_ACTIVE, false)
                remove(SP_KEY_LICENSE_TOKEN)
            } else {
                putBoolean(SP_KEY_PRO_ACTIVE, true)
                putString(SP_KEY_LICENSE_TOKEN, token)
            }
            apply()
        }

        dataStore.edit { prefs ->
            if (token == null) {
                prefs.remove(KEY_LICENSE_TOKEN)
            } else {
                prefs[KEY_LICENSE_TOKEN] = token
            }
        }
    }

    val clockHighWaterDayFlow: Flow<Int> = preferencesFlow.map { prefs ->
        prefs[KEY_CLOCK_HIGH_WATER_DAY] ?: 0
    }

    suspend fun setClockHighWaterDay(day: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_CLOCK_HIGH_WATER_DAY] = day
        }
    }

    suspend fun updateClockHighWaterDayIfGreater(todayDay: Int) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_CLOCK_HIGH_WATER_DAY] ?: 0
            if (todayDay > current) {
                prefs[KEY_CLOCK_HIGH_WATER_DAY] = todayDay
            }
        }
    }

    val ocrCalibrationFlow: Flow<Map<RideApp, OcrCalibration>> = preferencesFlow.map { prefs ->
        val raw = prefs[KEY_OCR_CALIBRATION_JSON]
        if (raw.isNullOrBlank()) {
            emptyMap()
        } else {
            try {
                json.decodeFromString<Map<RideApp, OcrCalibration>>(raw)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    suspend fun setOcrCalibration(app: RideApp, calibration: OcrCalibration) {
        dataStore.edit { prefs ->
            val raw = prefs[KEY_OCR_CALIBRATION_JSON]
            val currentMap: MutableMap<RideApp, OcrCalibration> = if (raw.isNullOrBlank()) {
                mutableMapOf()
            } else {
                try {
                    json.decodeFromString<Map<RideApp, OcrCalibration>>(raw).toMutableMap()
                } catch (e: Exception) {
                    mutableMapOf()
                }
            }
            currentMap[app] = calibration
            prefs[KEY_OCR_CALIBRATION_JSON] = json.encodeToString(currentMap)
        }
    }

    val readerConfigOverrideFlow: Flow<Map<RideApp, String>> = preferencesFlow.map { prefs ->
        val raw = prefs[KEY_READER_CONFIG_OVERRIDE_JSON]
        if (raw.isNullOrBlank()) {
            emptyMap()
        } else {
            try {
                json.decodeFromString<Map<RideApp, String>>(raw)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    suspend fun setReaderConfigOverride(app: RideApp, overrideJson: String) {
        dataStore.edit { prefs ->
            val raw = prefs[KEY_READER_CONFIG_OVERRIDE_JSON]
            val currentMap: MutableMap<RideApp, String> = if (raw.isNullOrBlank()) {
                mutableMapOf()
            } else {
                try {
                    json.decodeFromString<Map<RideApp, String>>(raw).toMutableMap()
                } catch (e: Exception) {
                    mutableMapOf()
                }
            }
            currentMap[app] = overrideJson
            prefs[KEY_READER_CONFIG_OVERRIDE_JSON] = json.encodeToString(currentMap)
        }
    }

    val diagnosticsCountersFlow: Flow<Map<String, Int>> = preferencesFlow.map { prefs ->
        val raw = prefs[KEY_DIAGNOSTICS_COUNTERS_JSON]
        if (raw.isNullOrBlank()) {
            emptyMap()
        } else {
            try {
                json.decodeFromString<Map<String, Int>>(raw)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    suspend fun incrementDiagnosticCounter(name: String, delta: Int = 1) {
        dataStore.edit { prefs ->
            val raw = prefs[KEY_DIAGNOSTICS_COUNTERS_JSON]
            val currentMap: MutableMap<String, Int> = if (raw.isNullOrBlank()) {
                mutableMapOf()
            } else {
                try {
                    json.decodeFromString<Map<String, Int>>(raw).toMutableMap()
                } catch (e: Exception) {
                    mutableMapOf()
                }
            }
            val currentVal = currentMap[name] ?: 0
            currentMap[name] = currentVal + delta
            prefs[KEY_DIAGNOSTICS_COUNTERS_JSON] = json.encodeToString(currentMap)
        }
    }

    suspend fun setDiagnosticsCounters(counters: Map<String, Int>) {
        dataStore.edit { prefs ->
            prefs[KEY_DIAGNOSTICS_COUNTERS_JSON] = json.encodeToString(counters)
        }
    }

    val chatHistoryFlow: Flow<List<String>> = preferencesFlow.map { prefs ->
        val raw = prefs[KEY_CHAT_HISTORY_JSON]
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                json.decodeFromString<List<String>>(raw)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun addChatMessage(message: String) {
        dataStore.edit { prefs ->
            val raw = prefs[KEY_CHAT_HISTORY_JSON]
            val currentList: MutableList<String> = if (raw.isNullOrBlank()) {
                mutableListOf()
            } else {
                try {
                    json.decodeFromString<List<String>>(raw).toMutableList()
                } catch (e: Exception) {
                    mutableListOf()
                }
            }
            currentList.add(message)
            val trimmed = if (currentList.size > MAX_CHAT_HISTORY) {
                currentList.subList(currentList.size - MAX_CHAT_HISTORY, currentList.size)
            } else {
                currentList
            }
            prefs[KEY_CHAT_HISTORY_JSON] = json.encodeToString(trimmed)
        }
    }

    suspend fun clearChatHistory() {
        dataStore.edit { prefs ->
            prefs[KEY_CHAT_HISTORY_JSON] = json.encodeToString(emptyList<String>())
        }
    }

    suspend fun getLanguage(): String = languageFlow.first()
    suspend fun getPolicy(): Policy = policyFlow.first()
    suspend fun getMarkerSize(): MarkerSize = markerSizeFlow.first()
    suspend fun getBarOpacityPercent(): Int = barOpacityPercentFlow.first()
    suspend fun getBarPosition(): BarPosition = barPositionFlow.first()
    suspend fun getAutoOffHours(): Int = autoOffHoursFlow.first()
    suspend fun getAlertEnabled(): Boolean = alertEnabledFlow.first()
    suspend fun getAlertMode(): AlertMode = alertModeFlow.first()
    suspend fun getHomeWay(): HomeWay = homeWayFlow.first()
    suspend fun getConsentVersion(): Int = consentVersionFlow.first()
    suspend fun getWelcomeSeen(): Boolean = welcomeSeenFlow.first()
    suspend fun getSetupDone(): Boolean = setupDoneFlow.first()
    suspend fun getContactName(): String = contactNameFlow.first()
    suspend fun getContactPhone(): String = contactPhoneFlow.first()
    suspend fun getLicenseToken(): String? = licenseTokenFlow.first()
    suspend fun getClockHighWaterDay(): Int = clockHighWaterDayFlow.first()
}
