package com.voxtranslate.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VoxDarkScheme = darkColorScheme(
    primary = VoxPink,
    onPrimary = VoxPurpleDark,
    secondary = VoxViolet,
    onSecondary = Color.White,
    tertiary = VoxTeal,
    background = VoxPurpleDark,
    onBackground = VoxPinkSoft,
    surface = VoxPurple,
    onSurface = VoxPinkSoft,
    surfaceVariant = VoxPurpleSurface,
    onSurfaceVariant = VoxPinkSoft,
    error = VoxRed,
)

private val VoxLightScheme = lightColorScheme(
    primary = VoxViolet,
    onPrimary = Color.White,
    secondary = VoxPink,
    onSecondary = Color.White,
    tertiary = VoxTeal,
    background = Color(0xFFFAF7FF),
    onBackground = Color(0xFF241238),
    surface = Color.White,
    onSurface = Color(0xFF241238),
    surfaceVariant = Color(0xFFF0E9FB),
    onSurfaceVariant = Color(0xFF4A3768),
    error = VoxRed,
)

@Composable
fun VoxTranslateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) VoxDarkScheme else VoxLightScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = VoxTypography,
        content = content
    )
}
