package com.driveflow.driver.core.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

object LocaleHelper {
    fun isRtl(languageCode: String): Boolean {
        return languageCode.lowercase() == "ar"
    }

    fun getLayoutDirection(languageCode: String): LayoutDirection {
        return if (isRtl(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    /**
     * Updates the context locale dynamically at runtime.
     */
    fun updateLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                localeManager?.applicationLocales = LocaleList(locale)
            } catch (_: Exception) {
            }
        }
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
