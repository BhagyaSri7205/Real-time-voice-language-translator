package com.voxtranslate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.voxtranslate.app.ui.navigation.VoxTranslateNavHost
import com.voxtranslate.app.ui.screens.LoginScreen
import com.voxtranslate.app.ui.theme.VoxTranslateTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application as VoxTranslateApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsState()
            val currentUsername by viewModel.currentUsername.collectAsState()

            VoxTranslateTheme(darkTheme = settings.darkTheme) {
                if (currentUsername == null) {
                    LoginScreen(viewModel = viewModel)
                } else {
                    VoxTranslateNavHost(viewModel = viewModel)
                }
            }
        }
    }
}
