package de.pdfwerkzeugkasten.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import de.pdfwerkzeugkasten.domain.model.CompressionLevel
import de.pdfwerkzeugkasten.domain.model.UserPlan
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore("settings")
class SettingsRepository(private val context: Context) {
    private object Keys { val ONBOARDED = booleanPreferencesKey("onboarded"); val PLAN = stringPreferencesKey("plan"); val THEME = stringPreferencesKey("theme"); val COMPRESSION = stringPreferencesKey("compression"); val LANGUAGE = stringPreferencesKey("language") }
    val onboarded = context.settingsStore.data.map { it[Keys.ONBOARDED] ?: false }
    val userPlan = context.settingsStore.data.map { runCatching { UserPlan.valueOf(it[Keys.PLAN] ?: UserPlan.FREE.name) }.getOrDefault(UserPlan.FREE) }
    val theme = context.settingsStore.data.map { it[Keys.THEME] ?: "System" }
    val compression = context.settingsStore.data.map { runCatching { CompressionLevel.valueOf(it[Keys.COMPRESSION] ?: CompressionLevel.MEDIUM.name) }.getOrDefault(CompressionLevel.MEDIUM) }
    val language = context.settingsStore.data.map { it[Keys.LANGUAGE] ?: "system" }
    suspend fun setOnboarded(value: Boolean) = context.settingsStore.edit { it[Keys.ONBOARDED] = value }
    suspend fun setPlan(plan: UserPlan) = context.settingsStore.edit { it[Keys.PLAN] = plan.name }
    suspend fun setTheme(theme: String) = context.settingsStore.edit { it[Keys.THEME] = theme }
    suspend fun setCompression(level: CompressionLevel) = context.settingsStore.edit { it[Keys.COMPRESSION] = level.name }
    suspend fun setLanguage(language: String) = context.settingsStore.edit { it[Keys.LANGUAGE] = language }
}
