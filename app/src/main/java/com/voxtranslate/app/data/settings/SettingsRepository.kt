package com.voxtranslate.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.voxtranslate.app.translate.Languages
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "vox_settings")

data class VoxSettings(
    val defaultSourceLanguage: String = Languages.DEFAULT_SOURCE.displayName,
    val defaultTargetLanguage: String = Languages.DEFAULT_TARGET.displayName,
    val darkTheme: Boolean = true,
    val speechRate: Float = 1.0f,
    val autoSpeakTranslations: Boolean = true,
    val saveHistory: Boolean = true
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SOURCE_LANG = stringPreferencesKey("default_source_language")
        val TARGET_LANG = stringPreferencesKey("default_target_language")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak_translations")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
    }

    val settingsFlow: Flow<VoxSettings> = context.dataStore.data.map { prefs ->
        VoxSettings(
            defaultSourceLanguage = prefs[Keys.SOURCE_LANG] ?: Languages.DEFAULT_SOURCE.displayName,
            defaultTargetLanguage = prefs[Keys.TARGET_LANG] ?: Languages.DEFAULT_TARGET.displayName,
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
            autoSpeakTranslations = prefs[Keys.AUTO_SPEAK] ?: true,
            saveHistory = prefs[Keys.SAVE_HISTORY] ?: true
        )
    }

    suspend fun setDefaultSourceLanguage(name: String) {
        context.dataStore.edit { it[Keys.SOURCE_LANG] = name }
    }

    suspend fun setDefaultTargetLanguage(name: String) {
        context.dataStore.edit { it[Keys.TARGET_LANG] = name }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[Keys.SPEECH_RATE] = rate }
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_SPEAK] = enabled }
    }

    suspend fun setSaveHistory(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SAVE_HISTORY] = enabled }
    }
}
