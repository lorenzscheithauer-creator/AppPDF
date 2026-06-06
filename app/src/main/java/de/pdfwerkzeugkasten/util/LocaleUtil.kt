package de.pdfwerkzeugkasten.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleUtil {
    const val SYSTEM = "system"
    const val GERMAN = "de"
    const val ENGLISH = "en"

    fun apply(activity: Activity, languageTag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = if (languageTag == SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(languageTag)
            activity.getSystemService(LocaleManager::class.java).applicationLocales = locales
        } else {
            val locale = when (languageTag) {
                GERMAN -> Locale.GERMAN
                ENGLISH -> Locale.ENGLISH
                else -> Locale.getDefault()
            }
            Locale.setDefault(locale)
            @Suppress("DEPRECATION")
            activity.resources.updateConfiguration(
                Configuration(activity.resources.configuration).apply { setLocale(locale) },
                activity.resources.displayMetrics
            )
        }
    }

    fun wrap(context: Context, languageTag: String): Context {
        if (languageTag == SYSTEM) return context
        val locale = Locale.forLanguageTag(languageTag)
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        return context.createConfigurationContext(config)
    }
}
