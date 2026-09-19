package com.voxtranslate.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.voxtranslate.app.MainViewModel
import com.voxtranslate.app.data.auth.AuthResult
import kotlinx.coroutines.launch

/**
 * Local sign-in screen, shown before the rest of the app. Mirrors the
 * original desktop app's login/register window — accounts are stored only
 * on this device (see AuthRepository), so there's no email verification or
 * password reset flow, just the same local username + password the
 * desktop app used.
 */
@Composable
fun LoginScreen(viewModel: MainViewModel) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    fun submit() {
        errorText = null
        isLoading = true
        scope.launch {
            val result = if (isRegisterMode) {
                viewModel.signUp(username, password)
            } else {
                viewModel.logIn(username, password)
            }
            isLoading = false
            when (result) {
                is AuthResult.Success -> { /* MainActivity observes currentUsername and swaps screens automatically */ }
                is AuthResult.Failure -> errorText = result.message
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("🎙️", style = MaterialTheme.typography.headlineLarge)
                Text("VoxTranslate", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    if (isRegisterMode) "Create a new account" else "Login to your account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(onClick = { submit() }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (isRegisterMode) "REGISTER" else "LOGIN")
                    }
                }

                TextButton(onClick = {
                    isRegisterMode = !isRegisterMode
                    errorText = null
                }) {
                    Text(
                        if (isRegisterMode) "Already have an account? Login"
                        else "Don't have an account? Register"
                    )
                }
            }
        }
    }
}
