package com.example.util

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

object LocaleHelper {

    fun getLocalizedContext(baseContext: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "ar" -> Locale.forLanguageTag("ar")
            "fr" -> Locale.forLanguageTag("fr")
            else -> Locale.forLanguageTag("en")
        }
        Locale.setDefault(locale)
        val config = Configuration(baseContext.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return baseContext.createConfigurationContext(config)
    }

    fun getLayoutDirection(languageCode: String): LayoutDirection {
        return if (languageCode == "ar") {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }
    }
}
