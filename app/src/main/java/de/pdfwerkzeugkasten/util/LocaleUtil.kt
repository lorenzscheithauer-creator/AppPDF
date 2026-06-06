package de.pdfwerkzeugkasten.util

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import de.pdfwerkzeugkasten.R
import java.util.Locale

object LocaleUtil {
    const val SYSTEM = "system"
    const val GERMAN = "de"
    const val ENGLISH = "en"
    const val SPANISH = "es"
    const val FRENCH = "fr"
    const val ITALIAN = "it"
    const val PORTUGUESE = "pt"
    const val TURKISH = "tr"

    val SUPPORTED_LANGUAGES = listOf(
        SupportedLanguage(GERMAN, R.string.settings_language_de),
        SupportedLanguage(ENGLISH, R.string.settings_language_en),
        SupportedLanguage(SPANISH, R.string.settings_language_es),
        SupportedLanguage(FRENCH, R.string.settings_language_fr),
        SupportedLanguage(ITALIAN, R.string.settings_language_it),
        SupportedLanguage(PORTUGUESE, R.string.settings_language_pt),
        SupportedLanguage(TURKISH, R.string.settings_language_tr)
    )

    fun apply(activity: Activity, languageTag: String) {
        if (Build.VERSION.SDK_INT >= 33) {
            val locales = if (languageTag == SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(languageTag)
            activity.getSystemService(LocaleManager::class.java).applicationLocales = locales
        } else {
            val locale = when (languageTag) {
                GERMAN -> Locale.GERMAN
                ENGLISH -> Locale.ENGLISH
                SPANISH -> Locale("es")
                FRENCH -> Locale.FRENCH
                ITALIAN -> Locale.ITALIAN
                PORTUGUESE -> Locale("pt")
                TURKISH -> Locale("tr")
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

data class SupportedLanguage(val tag: String, val labelRes: Int)
