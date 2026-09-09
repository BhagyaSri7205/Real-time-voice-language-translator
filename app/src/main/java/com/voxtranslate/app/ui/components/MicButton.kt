package com.voxtranslate.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import com.voxtranslate.app.ui.theme.GradientEnd
import com.voxtranslate.app.ui.theme.GradientStart
import com.voxtranslate.app.ui.theme.VoxRed

@Composable
fun MicButton(
    listening: Boolean,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 88.dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "mic_pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (listening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(if (listening) pulse else 1f)
            .background(
                brush = if (listening) {
                    Brush.linearGradient(listOf(VoxRed, GradientEnd))
                } else {
                    Brush.linearGradient(listOf(GradientStart, GradientEnd))
                },
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (listening) Icons.Filled.MicOff else Icons.Filled.Mic,
            contentDescription = if (listening) "Stop listening" else "Start listening",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(size / 2.4f)
        )
    }
}
