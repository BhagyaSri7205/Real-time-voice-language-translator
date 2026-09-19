package com.voxtranslate.app

import android.app.Application
import com.voxtranslate.app.data.auth.AuthRepository
import com.voxtranslate.app.data.db.AppDatabase
import com.voxtranslate.app.data.settings.SettingsRepository
import com.voxtranslate.app.speech.TextToSpeechManager

class VoxTranslateApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val authRepository: AuthRepository by lazy { AuthRepository(this, database) }
    val ttsManager: TextToSpeechManager by lazy { TextToSpeechManager(this) }

    override fun onCreate() {
        super.onCreate()
        ttsManager.init()
    }

    override fun onTerminate() {
        ttsManager.shutdown()
        super.onTerminate()
    }
}
