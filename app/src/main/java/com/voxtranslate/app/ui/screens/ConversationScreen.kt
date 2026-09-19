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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.voxtranslate.app.translate.Languages
import com.voxtranslate.app.translate.TranslateResult
import com.voxtranslate.app.translate.VoxLanguage
import com.voxtranslate.app.ui.components.LanguagePicker
import com.voxtranslate.app.ui.components.MicButton
import com.voxtranslate.app.ui.components.TalkingAvatar

private data class Turn(val speakerLabel: String, val original: String, val translated: String)

/** How many misheard/no-match turns in a row we tolerate before pausing continuous
 *  mode with a helpful message, instead of looping silently forever. */
private const val MAX_CONSECUTIVE_FAILURES = 3

/**
 * Two people, two languages, one conversation. Each side has its own mic —
 * tap Person A's mic to speak in language A, it's translated and spoken in
 * language B, and vice versa. Mirrors what a real face-to-face bilingual
 * conversation needs, unlike a single-direction translate box.
 *
 * Tapping "Start Conversation" turns this into a continuous session: after a
 * turn is translated and spoken aloud, the app automatically starts
 * listening again for the *other* person's reply, looping back and forth
 * with no extra taps needed, until "Stop Conversation" is tapped. Tapping a
 * person's mic mid-session immediately switches the turn to that person
 * (useful if the auto-alternation guessed wrong about who's about to talk).
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

    var conversationActive by remember { mutableStateOf(false) }
    var activeSide by remember { mutableStateOf<Char?>(null) } // 'A', 'B', or null
    // Bumped to force the listening effect to restart even when activeSide's value
    // doesn't change (e.g. retrying the same speaker after a "didn't catch that").
    var sessionTick by remember { mutableStateOf(0) }
    var consecutiveFailures by remember { mutableStateOf(0) }
    var statusText by remember { mutableStateOf("Tap \"Start Conversation\", then a mic, to begin") }
    val turns = remember { mutableStateListOf<Turn>() }

    val speechManager = remember { SpeechToTextManager(context) }

    var pendingSide by remember { mutableStateOf<Char?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingSide != null) {
            activeSide = pendingSide
            sessionTick++
        } else {
            statusText = "Microphone permission is required."
        }
        pendingSide = null
    }

    fun hasMicPermission() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    fun startSide(side: Char) {
        // In one-shot (non-continuous) mode, don't interrupt an in-progress turn.
        // In continuous mode, tapping a mic is a deliberate "it's this person's turn
        // now" override, so let it barge in and restart the loop on that side.
        if (activeSide != null && !conversationActive) return
        if (hasMicPermission()) {
            activeSide = side
            sessionTick++
        } else {
            pendingSide = side
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopConversation() {
        conversationActive = false
        activeSide = null
        consecutiveFailures = 0
        statusText = "Tap \"Start Conversation\", then a mic, to begin"
    }

    LaunchedEffect(activeSide, sessionTick) {
        val side = activeSide ?: return@LaunchedEffect
        val speakLang: VoxLanguage = if (side == 'A') langA else langB
        val hearLang: VoxLanguage = if (side == 'A') langB else langA
        val speakerLabel = if (side == 'A') "Person A (${speakLang.displayName})" else "Person B (${speakLang.displayName})"

        var turnSucceeded = false
        var gotError = false

        speechManager.listen(speakLang.speechLocale).collect { event ->
            when (event) {
                is SpeechEvent.Listening -> statusText = "Listening to $speakerLabel…"
                is SpeechEvent.PartialResult -> {}
                is SpeechEvent.FinalResult -> {
                    if (event.text.isNotBlank()) {
                        statusText = "Translating…"
                        when (val result = viewModel.translate(event.text, speakLang.mlkitCode, hearLang.mlkitCode)) {
                            is TranslateResult.Success -> {
                                turns.add(0, Turn(speakerLabel, event.text, result.text))
                                viewModel.saveHistory(
                                    speakLang.displayName, hearLang.displayName,
                                    event.text, result.text, "conversation"
                                )
                                viewModel.speak(result.text, hearLang.speechLocale)
                                statusText = if (conversationActive) {
                                    "Listening for a reply from ${if (side == 'A') "Person B" else "Person A"}…"
                                } else {
                                    "Tap either mic to continue"
                                }
                                turnSucceeded = true
                            }
                            is TranslateResult.Error -> {
                                statusText = result.message
                                gotError = true
                            }
                            is TranslateResult.NeedsDownload -> statusText = result.message
                        }
                    } else {
                        statusText = "Didn't catch that — try again."
                        gotError = true
                    }
                }
                is SpeechEvent.Error -> {
                    statusText = event.message
                    gotError = true
                }
                is SpeechEvent.Done -> {}
            }
        }

        // The recognizer session for this turn has fully finished. Decide what
        // happens next — this is what makes the mode "continuous" rather than
        // one tap per utterance.
        if (!conversationActive) {
            activeSide = null
            return@LaunchedEffect
        }

        if (turnSucceeded) {
            consecutiveFailures = 0
            val otherSide = if (side == 'A') 'B' else 'A'
            activeSide = otherSide
            sessionTick++
        } else if (gotError) {
            consecutiveFailures++
            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                statusText = "Having trouble hearing $speakerLabel — tap a mic when you're ready to try again."
                conversationActive = false
                activeSide = null
            } else {
                // Let the same person try again rather than assuming the turn passed.
                activeSide = side
                sessionTick++
            }
        } else {
            activeSide = null
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

        Button(
            onClick = {
                if (conversationActive) {
                    stopConversation()
                } else {
                    conversationActive = true
                    consecutiveFailures = 0
                    statusText = "Tap either mic to begin — I'll keep listening after that"
                }
            },
            colors = if (conversationActive) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.buttonColors()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (conversationActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(if (conversationActive) "Stop Conversation" else "Start Conversation")
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
                    onClick = {
                        if (activeSide == 'A' && !conversationActive) activeSide = null else startSide('A')
                    },
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
                    onClick = {
                        if (activeSide == 'B' && !conversationActive) activeSide = null else startSide('B')
                    },
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
                        Text(
                            "→ ${turn.translated}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
