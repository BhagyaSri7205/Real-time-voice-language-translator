package com.voxtranslate.app.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class TtsEvent {
    object Started : TtsEvent()
    object Done : TtsEvent()
    data class Error(val message: String) : TtsEvent()
}

/**
 * Wraps Android's built-in TextToSpeech engine. Equivalent to the desktop
 * app's `backend/voice.py` `speak()` (which used gTTS + playsound and
 * required internet); this uses the on-device TTS engine so it also works
 * offline once the voice pack for a language is installed.
 */
class TextToSpeechManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private val appContext = context.applicationContext

    fun init(onReady: (Boolean) -> Unit = {}) {
        if (tts != null) {
            onReady(ready)
            return
        }
        tts = TextToSpeech(appContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            onReady(ready)
        }
    }

    fun isLanguageAvailable(localeTag: String): Boolean {
        val engine = tts ?: return false
        val locale = Locale.forLanguageTag(localeTag)
        val result = engine.isLanguageAvailable(locale)
        return result == TextToSpeech.LANG_AVAILABLE ||
            result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
            result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
    }

    fun speak(text: String, localeTag: String, speechRate: Float = 1.0f): Flow<TtsEvent> = callbackFlow {
        val engine = tts
        if (engine == null || !ready) {
            trySend(TtsEvent.Error("Text-to-speech engine isn't ready yet."))
            close()
            return@callbackFlow
        }
        if (text.isBlank()) {
            trySend(TtsEvent.Done)
            close()
            return@callbackFlow
        }

        val locale = Locale.forLanguageTag(localeTag)
        val langResult = engine.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            trySend(
                TtsEvent.Error(
                    "No voice installed for this language yet. " +
                        "Go to your phone's Settings > Text-to-speech to download it."
                )
            )
            close()
            return@callbackFlow
        }
        engine.setSpeechRate(speechRate)

        val utteranceId = UUID.randomUUID().toString()
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                trySend(TtsEvent.Started)
            }

            override fun onDone(id: String?) {
                trySend(TtsEvent.Done)
                close()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                trySend(TtsEvent.Error("Couldn't play speech."))
                close()
            }

            override fun onError(id: String?, errorCode: Int) {
                trySend(TtsEvent.Error("Couldn't play speech (code $errorCode)."))
                close()
            }
        })

        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        awaitClose { }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
