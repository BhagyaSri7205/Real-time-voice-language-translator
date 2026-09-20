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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.voxtranslate.app.MainViewModel
import com.voxtranslate.app.speech.SpeechEvent
import com.voxtranslate.app.speech.SpeechToTextManager
import com.voxtranslate.app.speech.TtsEvent
import com.voxtranslate.app.translate.Languages
import com.voxtranslate.app.translate.TranslateResult
import com.voxtranslate.app.translate.VoxLanguage
import com.voxtranslate.app.ui.components.LanguagePicker
import com.voxtranslate.app.ui.components.MicButton
import com.voxtranslate.app.ui.components.TalkingAvatar

private data class Turn(
    val speakerLabel: String,
    val original: String,
    val translated: String,
    val translatedLocale: String
)

/**
 * Two people, two languages, one conversation. Each side has its own mic —
 * tap Person A's mic to speak in language A, it's translated and spoken in
 * language B, and vice versa. Mirrors what a real face-to-face bilingual
 * conversation needs, unlike a single-direction translate box.
 */
@Composable
fun ConversationScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()

    var langA by remember(settings.defaultSourceLanguage) {
        mutableStateOf(Languages.byName(settings.defaultSourceLanguage))
    }
    var langB by remember(settings.defaultTargetLanguage) {
        mutableStateOf(Languages.byName(settings.defaultTargetLanguage))
    }

    var activeSide by remember { mutableStateOf<Char?>(null) } // 'A', 'B', or null
    var statusText by remember { mutableStateOf("Tap either mic to start the conversation") }
    var autoContinue by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val turns = remember { mutableStateListOf<Turn>() }

    val speechManager = remember { SpeechToTextManager(context) }

    var pendingSide by remember { mutableStateOf<Char?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingSide != null) {
            activeSide = pendingSide
        } else {
            statusText = "Microphone permission is required."
        }
        pendingSide = null
    }

    fun hasMicPermission() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    fun startSide(side: Char) {
        if (activeSide != null) return
        if (hasMicPermission()) {
            activeSide = side
        } else {
            pendingSide = side
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(activeSide) {
        val side = activeSide ?: return@LaunchedEffect
        errorText = null
        val speakLang: VoxLanguage = if (side == 'A') langA else langB
        val hearLang: VoxLanguage = if (side == 'A') langB else langA
        val speakerLabel = if (side == 'A') "Person A (${speakLang.displayName})" else "Person B (${speakLang.displayName})"

        speechManager.listen(speakLang.speechLocale).collect { event ->
            when (event) {
                is SpeechEvent.Listening -> statusText = "Listening to $speakerLabel…"
                is SpeechEvent.PartialResult -> {}
                is SpeechEvent.FinalResult -> {
                    if (event.text.isNotBlank()) {
                        statusText = "Translating…"
                        val result = try {
                            viewModel.translate(event.text, speakLang.mlkitCode, hearLang.mlkitCode)
                        } catch (e: Exception) {
                            TranslateResult.Error("Unexpected error: ${e.message ?: e.javaClass.simpleName}")
                        }
                        when (result) {
                            is TranslateResult.Success -> {
                                turns.add(0, Turn(speakerLabel, event.text, result.text, hearLang.speechLocale))
                                viewModel.saveHistory(
                                    speakLang.displayName, hearLang.displayName,
                                    event.text, result.text, "conversation"
                                )
                                statusText = "Speaking translation to the other person…"
                                val nextSide = if (side == 'A') 'B' else 'A'
                                viewModel.speak(result.text, hearLang.speechLocale) { ttsEvent ->
                                    if (ttsEvent is TtsEvent.Done || ttsEvent is TtsEvent.Error) {
                                        if (autoContinue) {
                                            // Automatically starts listening for the other
                                            // person's reply once the translation finishes
                                            // playing, so the conversation keeps flowing
                                            // without needing to tap a mic every turn.
                                            activeSide = nextSide
                                        } else {
                                            statusText = "Tap either mic to continue"
                                        }
                                    }
                                }
                            }
                            is TranslateResult.Error -> {
                                errorText = result.message
                                statusText = "Translation failed — see details below"
                            }
                            is TranslateResult.NeedsDownload -> {
                                errorText = result.message
                                statusText = "Translation needs a download — see details below"
                            }
                        }
                    } else {
                        statusText = "Didn't catch that — try again."
                    }
                }
                is SpeechEvent.Error -> {
                    errorText = event.message
                    statusText = "Couldn't hear $speakerLabel"
                }
                is SpeechEvent.Done -> {
                    // Only clear the active mic here; if a translation succeeded above,
                    // activeSide may already have been (re)armed for the next speaker by
                    // the TTS completion callback, so only null it out if nothing else set it.
                    if (activeSide == side) activeSide = null
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Conversation Mode", style = MaterialTheme.typography.headlineMedium)
            TalkingAvatar(isTalking = isSpeaking, size = 72.dp)
        }
        Text(statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        errorText?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Auto-continue after each turn", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = autoContinue, onCheckedChange = { autoContinue = it })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Person A", fontWeight = FontWeight.Bold)
                LanguagePicker(label = "Language", selected = langA, onSelect = { langA = it })
                MicButton(
                    listening = activeSide == 'A',
                    onClick = { if (activeSide == 'A') activeSide = null else startSide('A') },
                    size = 64.dp
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Person B", fontWeight = FontWeight.Bold)
                LanguagePicker(label = "Language", selected = langB, onSelect = { langB = it })
                MicButton(
                    listening = activeSide == 'B',
                    onClick = { if (activeSide == 'B') activeSide = null else startSide('B') },
                    size = 64.dp
                )
            }
        }

        HorizontalDivider()

        Text("Conversation", style = MaterialTheme.typography.titleMedium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            turns.forEach { turn ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(turn.speakerLabel, style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(turn.original, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "→ ${turn.translated}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.speak(turn.translated, turn.translatedLocale) }) {
                                Icon(Icons.Filled.VolumeUp, contentDescription = "Listen again")
                            }
                        }
                    }
                }
            }
        }
    }
}
