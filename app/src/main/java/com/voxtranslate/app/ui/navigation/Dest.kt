package com.voxtranslate.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    object Home : Dest("home", "Home", Icons.Filled.Home)
    object Conversation : Dest("conversation", "Conversation", Icons.Filled.SwapHoriz)
    object TranslateText : Dest("translate_text", "Translate", Icons.Filled.Translate)
    object Camera : Dest("camera", "Camera", Icons.Filled.Camera)
    object History : Dest("history", "History", Icons.Filled.History)
    object Settings : Dest("settings", "Settings", Icons.Filled.Settings)

    companion object {
        val bottomBarItems = listOf(Home, TranslateText, Conversation, Camera, History)
    }
}
