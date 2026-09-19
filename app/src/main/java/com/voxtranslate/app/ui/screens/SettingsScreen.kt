package com.voxtranslate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.voxtranslate.app.MainViewModel
import com.voxtranslate.app.translate.Languages
import com.voxtranslate.app.ui.components.LanguagePicker

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    val currentUsername by viewModel.currentUsername.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Signed in as", style = MaterialTheme.typography.labelLarge)
                    Text(currentUsername ?: "", style = MaterialTheme.typography.titleMedium)
                }
                OutlinedButton(onClick = { viewModel.logOut() }) {
                    Text("Log out")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Default languages", style = MaterialTheme.typography.titleMedium)
                LanguagePicker(
                    label = "Default source language",
                    selected = Languages.byName(settings.defaultSourceLanguage),
                    onSelect = { viewModel.setDefaultSourceLanguage(it.displayName) },
                    modifier = Modifier.fillMaxWidth()
                )
                LanguagePicker(
                    label = "Default target language",
                    selected = Languages.byName(settings.defaultTargetLanguage),
                    onSelect = { viewModel.setDefaultTargetLanguage(it.displayName) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Voice & speech", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-speak translations")
                    Switch(
                        checked = settings.autoSpeakTranslations,
                        onCheckedChange = { viewModel.setAutoSpeak(it) }
                    )
                }

                Text("Speech rate: ${"%.1f".format(settings.speechRate)}x")
                Slider(
                    value = settings.speechRate,
                    onValueChange = { viewModel.setSpeechRate(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 5
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Appearance & data", style = MaterialTheme.typography.titleMedium)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dark theme")
                    Switch(
                        checked = settings.darkTheme,
                        onCheckedChange = { viewModel.setDarkTheme(it) }
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Save translation history")
                    Switch(
                        checked = settings.saveHistory,
                        onCheckedChange = { viewModel.setSaveHistory(it) }
                    )
                }
            }
        }

        Text(
            "VoxTranslate 1.0 — translation and OCR run fully on-device after the first " +
                "download for each language, so most features work offline.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
