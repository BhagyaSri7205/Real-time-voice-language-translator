package com.voxtranslate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.voxtranslate.app.MainViewModel
import com.voxtranslate.app.translate.Languages
import com.voxtranslate.app.translate.TranslateResult
import com.voxtranslate.app.ui.components.LanguagePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TranslateTextScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    var sourceLang by remember(settings.defaultSourceLanguage) {
        mutableStateOf(Languages.byName(settings.defaultSourceLanguage))
    }
    var targetLang by remember(settings.defaultTargetLanguage) {
        mutableStateOf(Languages.byName(settings.defaultTargetLanguage))
    }
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    fun runTranslate() {
        if (inputText.isBlank()) return
        scope.launch {
            isLoading = true
            errorText = null
            when (val result = viewModel.translate(inputText, sourceLang.mlkitCode, targetLang.mlkitCode)) {
                is TranslateResult.Success -> {
                    outputText = result.text
                    viewModel.saveHistory(
                        sourceLang.displayName, targetLang.displayName, inputText, result.text, "text"
                    )
                    if (settings.autoSpeakTranslations) {
                        viewModel.speak(result.text, targetLang.speechLocale)
                    }
                }
                is TranslateResult.Error -> errorText = result.message
                is TranslateResult.NeedsDownload -> errorText = result.message
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Translate Text", style = MaterialTheme.typography.headlineMedium)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LanguagePicker(
                label = "From",
                selected = sourceLang,
                onSelect = { sourceLang = it },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val tmp = sourceLang
                sourceLang = targetLang
                targetLang = tmp
                val tmpText = inputText
                inputText = outputText
                outputText = tmpText
            }) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = "Swap languages")
            }
            LanguagePicker(
                label = "To",
                selected = targetLang,
                onSelect = { targetLang = it },
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Type text to translate") },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { runTranslate() }, modifier = Modifier.weight(1f)) {
                Text("Translate")
            }
            Button(onClick = { inputText = ""; outputText = ""; errorText = null }) {
                Text("Clear")
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        errorText?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(it, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }

        if (outputText.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "${targetLang.flag} ${targetLang.displayName}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(outputText, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(10.dp))
                    Row {
                        IconButton(onClick = { viewModel.speak(outputText, targetLang.speechLocale) }) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = "Listen")
                        }
                        IconButton(onClick = { clipboard.setText(AnnotatedString(outputText)) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                        }
                    }
                }
            }
        }
    }
}
