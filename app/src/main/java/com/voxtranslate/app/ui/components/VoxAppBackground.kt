package com.voxtranslate.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.voxtranslate.app.ui.theme.VoxPurpleDark
import com.voxtranslate.app.ui.theme.VoxPurpleSurface
import kotlin.math.cos
import kotlin.math.sin

/**
 * A slowly-shifting purple/pink/violet gradient, matching the original
 * desktop app's animated gradient background. Wrap any screen's content in
 * this instead of leaving it on a flat theme background.
 */
@Composable
fun VoxAppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "bg_shift")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing)),
        label = "bg_phase"
    )

    val accent1 = Color(0xFF7B2CBF) // violet
    val accent2 = Color(0xFFE83EBD) // pink
    val accent3 = VoxPurpleSurface

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(VoxPurpleDark, accent3, VoxPurpleDark))
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(accent1.copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(
                        0.25f + 0.15f * cos(t),
                        0.2f + 0.1f * sin(t)
                    ).let { Offset(it.x * 1000f, it.y * 1000f) },
                    radius = 900f
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(accent2.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(
                        0.8f + 0.1f * sin(t * 0.8f),
                        0.75f + 0.12f * cos(t * 0.8f)
                    ).let { Offset(it.x * 1000f, it.y * 1000f) },
                    radius = 900f
                )
            )
    ) {
        content()
    }
}
