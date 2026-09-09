package com.voxtranslate.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.voxtranslate.app.MainViewModel
import com.voxtranslate.app.speech.SpeechEvent
import com.voxtranslate.app.speech.SpeechToTextManager
import com.voxtranslate.app.translate.Languages
import com.voxtranslate.app.translate.TranslateResult
import com.voxtranslate.app.ui.components.LanguagePicker
import com.voxtranslate.app.ui.components.MicButton

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    var sourceLang by remember(settings.defaultSourceLanguage) {
        mutableStateOf(Languages.byName(settings.defaultSourceLanguage))
    }
    var targetLang by remember(settings.defaultTargetLanguage) {
        mutableStateOf(Languages.byName(settings.defaultTargetLanguage))
    }

    var listening by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Tap the mic and start speaking") }
    var recognizedText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }

    val speechManager = remember { SpeechToTextManager(context) }

    var pendingListen by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingListen) {
            pendingListen = false
            listening = true
        } else {
            pendingListen = false
            statusText = "Microphone permission is required to speak."
        }
    }

    fun hasMicPermission() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    fun startListening() {
        recognizedText = ""
        translatedText = ""
        if (hasMicPermission()) {
            listening = true
        } else {
            pendingListen = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(listening) {
        if (!listening) return@LaunchedEffect
        speechManager.listen(sourceLang.speechLocale).collect { event ->
            when (event) {
                is SpeechEvent.Listening -> statusText = "Listening…"
                is SpeechEvent.PartialResult -> recognizedText = event.text
                is SpeechEvent.FinalResult -> {
                    recognizedText = event.text
                    if (event.text.isNotBlank()) {
                        statusText = "Translating…"
                        when (val result = viewModel.translate(event.text, sourceLang.mlkitCode, targetLang.mlkitCode)) {
                            is TranslateResult.Success -> {
                                translatedText = result.text
                                statusText = "Done"
                                viewModel.saveHistory(
                                    sourceLang.displayName, targetLang.displayName,
                                    event.text, result.text, "speech"
                                )
                                if (settings.autoSpeakTranslations) {
                                    viewModel.speak(result.text, targetLang.speechLocale)
                                }
                            }
                            is TranslateResult.Error -> statusText = result.message
                            is TranslateResult.NeedsDownload -> statusText = result.message
                        }
                    } else {
                        statusText = "Didn't catch that — tap the mic to try again."
                    }
                }
                is SpeechEvent.Error -> statusText = event.message
                is SpeechEvent.Done -> listening = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("VoxTranslate", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Speak, and hear it in another language instantly",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LanguagePicker(
                label = "I speak",
                selected = sourceLang,
                onSelect = { sourceLang = it },
                modifier = Modifier.weight(1f)
            )
            LanguagePicker(
                label = "Translate to",
                selected = targetLang,
                onSelect = { targetLang = it },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        MicButton(listening = listening, onClick = {
            if (listening) listening = false else startListening()
        })

        Text(statusText, style = MaterialTheme.typography.bodyMedium)

        if (recognizedText.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(14.dp)) {
                    Text("${sourceLang.flag} You said", style = MaterialTheme.typography.labelLarge)
                    Text(recognizedText, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        if (translatedText.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("${targetLang.flag} Translation", style = MaterialTheme.typography.labelLarge)
                    Text(translatedText, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
