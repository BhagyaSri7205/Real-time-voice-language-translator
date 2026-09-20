package com.voxtranslate.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

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
            trySend(
                SpeechEvent.Error(
                    "Speech recognition isn't available on this device. Make sure the " +
                        "Google app (or your device's voice input service) is installed and enabled."
                )
            )
            trySend(SpeechEvent.Done)
            close()
            return@callbackFlow
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            // Give people more room to pause mid-sentence before the recognizer decides
            // they're done — 1.2s was cutting people off, especially outside English.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000)
        }

        // Captured explicitly because the RecognitionListener below is a plain object (not a
        // CoroutineScope), so its methods can't resolve a bare `launch` on their own — they
        // need this reference to start/restart the watchdog from inside a callback.
        val producerScope = this

        fun startWatchdog(message: String): Job = producerScope.launch {
            delay(12_000)
            trySend(SpeechEvent.Error(message))
            trySend(SpeechEvent.Done)
            close()
        }

        // Watchdog: on some devices, asking for a language the recognizer service doesn't
        // actually support makes it silently do nothing at all — no onError, no onResults,
        // nothing — so the mic would otherwise sit "listening" forever with zero feedback.
        // If neither a result nor an error arrives within this window, surface a clear,
        // actionable message ourselves instead of hanging indefinitely.
        var watchdog: Job? = startWatchdog(
            "No response from the speech recognizer for this language after 12 seconds. " +
                "Your phone likely doesn't have voice input installed for this language — " +
                "check Settings > System > Languages & input > Voice input, or try English " +
                "to confirm the mic itself works."
        )

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(SpeechEvent.Listening)
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                watchdog?.cancel()
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH ->
                        "Didn't catch that — try speaking a bit slower and closer to the mic."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected — tap the mic and try again."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                        "Network error — check your connection."
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone error."
                    // Added in API 31; referencing the constant is safe on older devices since
                    // it's just an int — this branch simply won't be hit pre-31.
                    12 -> "This language isn't available for voice input on this device yet."
                    13 -> "This language isn't supported by your phone's speech recognizer. " +
                        "Try English, or check Settings > System > Languages & input > Voice input " +
                        "to see which languages are installed."
                    else -> "Speech recognition error ($error)."
                }
                trySend(SpeechEvent.Error(msg))
                trySend(SpeechEvent.Done)
                close()
            }

            override fun onResults(results: Bundle?) {
                watchdog?.cancel()
                // Take the first non-blank candidate rather than always index 0 — with
                // EXTRA_MAX_RESULTS raised to 3, a later candidate is sometimes the only
                // usable one for less common languages.
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull { it.isNotBlank() }
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
                    // Partial results prove the recognizer is actively working, so push the
                    // watchdog out rather than let a long sentence trip a false timeout.
                    watchdog?.cancel()
                    watchdog = startWatchdog(
                        "The speech recognizer stopped responding. Try again, or check " +
                            "Settings > System > Languages & input > Voice input for this language."
                    )
                    trySend(SpeechEvent.PartialResult(text))
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        recognizer.setRecognitionListener(listener)
        recognizer.startListening(intent)

        awaitClose {
            watchdog?.cancel()
            recognizer.stopListening()
            recognizer.destroy()
        }
    }
}
