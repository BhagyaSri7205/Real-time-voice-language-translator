package com.voxtranslate.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import com.voxtranslate.app.MainViewModel
import com.voxtranslate.app.R
import com.voxtranslate.app.ui.screens.CameraTranslateScreen
import com.voxtranslate.app.ui.screens.ConversationScreen
import com.voxtranslate.app.ui.screens.HistoryScreen
import com.voxtranslate.app.ui.screens.HomeScreen
import com.voxtranslate.app.ui.screens.SettingsScreen
import com.voxtranslate.app.ui.screens.TranslateTextScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoxTranslateNavHost(viewModel: MainViewModel) {
    val navController = rememberNavController()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { navController.navigate(Dest.Settings.route) }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            NavigationBar {
                Dest.bottomBarItems.forEach { dest ->
                    NavigationBarItem(
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Home.route) { HomeScreen(viewModel) }
            composable(Dest.Conversation.route) { ConversationScreen(viewModel) }
            composable(Dest.TranslateText.route) { TranslateTextScreen(viewModel) }
            composable(Dest.Camera.route) { CameraTranslateScreen(viewModel) }
            composable(Dest.History.route) { HistoryScreen(viewModel) }
            composable(Dest.Settings.route) { SettingsScreen(viewModel) }
        }
    }
}
