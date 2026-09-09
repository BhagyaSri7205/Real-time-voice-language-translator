package com.voxtranslate.app

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voxtranslate.app.data.db.HistoryEntity
import com.voxtranslate.app.data.settings.VoxSettings
import com.voxtranslate.app.speech.TtsEvent
import com.voxtranslate.app.translate.TranslateResult
import com.voxtranslate.app.translate.TranslationManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val app: VoxTranslateApp) : AndroidViewModel(app) {

    val settings: StateFlow<VoxSettings> = app.settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, VoxSettings())

    val history: StateFlow<List<HistoryEntity>> = app.database.historyDao().observeAll()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    suspend fun translate(text: String, sourceCode: String, targetCode: String): TranslateResult {
        return TranslationManager.translate(text, sourceCode, targetCode)
    }

    fun saveHistory(
        sourceLanguage: String,
        targetLanguage: String,
        original: String,
        translated: String,
        mode: String
    ) {
        if (!settings.value.saveHistory) return
        if (original.isBlank() || translated.isBlank()) return
        viewModelScope.launch {
            app.database.historyDao().insert(
                HistoryEntity(
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    originalText = original,
                    translatedText = translated,
                    mode = mode,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteHistoryItem(entity: HistoryEntity) {
        viewModelScope.launch { app.database.historyDao().delete(entity) }
    }

    fun clearHistory() {
        viewModelScope.launch { app.database.historyDao().clearAll() }
    }

    fun speak(text: String, localeTag: String, onEvent: (TtsEvent) -> Unit = {}) {
        if (text.isBlank()) return
        viewModelScope.launch {
            app.ttsManager.speak(text, localeTag, settings.value.speechRate).collect { onEvent(it) }
        }
    }

    fun stopSpeaking() {
        app.ttsManager.stop()
    }

    fun setDefaultSourceLanguage(name: String) = viewModelScope.launch {
        app.settingsRepository.setDefaultSourceLanguage(name)
    }

    fun setDefaultTargetLanguage(name: String) = viewModelScope.launch {
        app.settingsRepository.setDefaultTargetLanguage(name)
    }

    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch {
        app.settingsRepository.setDarkTheme(enabled)
    }

    fun setSpeechRate(rate: Float) = viewModelScope.launch {
        app.settingsRepository.setSpeechRate(rate)
    }

    fun setAutoSpeak(enabled: Boolean) = viewModelScope.launch {
        app.settingsRepository.setAutoSpeak(enabled)
    }

    fun setSaveHistory(enabled: Boolean) = viewModelScope.launch {
        app.settingsRepository.setSaveHistory(enabled)
    }

    class Factory(private val app: VoxTranslateApp) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(app) as T
        }
    }
}
