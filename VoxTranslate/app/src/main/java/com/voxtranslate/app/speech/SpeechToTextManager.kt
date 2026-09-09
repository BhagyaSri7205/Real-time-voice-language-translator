package com.voxtranslate.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed class SpeechEvent {
    object Listening : SpeechEvent()
    data class PartialResult(val text: String) : SpeechEvent()
    data class FinalResult(val text: String) : SpeechEvent()
    data class Error(val message: String) : SpeechEvent()
    object Done : SpeechEvent()
}

/**
 * Wraps Android's built-in SpeechRecognizer (free, on-device where the OS
 * supports it, otherwise uses Google's speech service transparently).
 * Equivalent to the desktop app's `backend/speech.py` `listen()` function.
 */
class SpeechToTextManager(private val context: Context) {

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun listen(localeTag: String): Flow<SpeechEvent> = callbackFlow {
        if (!isAvailable()) {
            trySend(SpeechEvent.Error("Speech recognition isn't available on this device."))
            trySend(SpeechEvent.Done)
            close()
            return@callbackFlow
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200)
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechEvent.Listening)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — try again."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Network error — check your connection."
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone error."
                    else -> "Speech recognition error ($error)."
                }
                trySend(SpeechEvent.Error(msg))
                trySend(SpeechEvent.Done)
                close()
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                trySend(SpeechEvent.FinalResult(text))
                trySend(SpeechEvent.Done)
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    trySend(SpeechEvent.PartialResult(text))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        recognizer.setRecognitionListener(listener)
        recognizer.startListening(intent)

        awaitClose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }
}
