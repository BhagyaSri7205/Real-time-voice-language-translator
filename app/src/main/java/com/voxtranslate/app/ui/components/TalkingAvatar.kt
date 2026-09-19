package com.voxtranslate.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voxtranslate.app.ui.theme.VoxOrange
import com.voxtranslate.app.ui.theme.VoxPink
import com.voxtranslate.app.ui.theme.VoxViolet
import kotlin.math.sin

/**
 * A small standing character that idles with a gentle breathing sway and
 * switches to a waving, talking pose (with a pulsing speech-bubble badge)
 * whenever [isTalking] is true — a lighter Compose take on the original
 * desktop app's hand-drawn Tkinter avatar, which did the same thing while
 * text-to-speech played.
 */
@Composable
fun TalkingAvatar(
    isTalking: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    val infinite = rememberInfiniteTransition(label = "avatar_idle")

    // Continuous idle phase used for breathing/sway.
    val idlePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "idle_phase"
    )

    // Faster phase, used for the wave/soundwave pulse while talking.
    val talkPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing)),
        label = "talk_phase"
    )

    val skin = Color(0xFFFFD9B3)
    val hair = Color(0xFF3B1F52)
    val shirt = VoxViolet
    val outline = VoxPink
    val badgeColor = Color(0xFFE83EBD)

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w * 0.46f

        val bob = sin(idlePhase) * (h * 0.01f)
        val sway = sin(idlePhase * 0.6f) * (w * 0.015f)
        val dx = sway
        val dy = bob

        val headR = w * 0.20f
        val headCy = h * 0.16f + dy
        val neckTop = headCy + headR * 0.85f
        val shoulderY = neckTop + h * 0.02f
        val shoulderW = w * 0.46f
        val hipW = w * 0.34f
        val torsoBottom = h * 0.55f + dy
        val hipBottom = h * 0.58f + dy
        val legBottom = h * 0.9f
        val centerX = cx + dx

        // legs
        drawRoundRect(
            color = Color(0xFF2A1440),
            topLeft = Offset(centerX - hipW * 0.32f, hipBottom),
            size = androidx.compose.ui.geometry.Size(hipW * 0.3f, legBottom - hipBottom)
        )
        drawRoundRect(
            color = Color(0xFF2A1440),
            topLeft = Offset(centerX + w * 0.02f, hipBottom),
            size = androidx.compose.ui.geometry.Size(hipW * 0.3f, legBottom - hipBottom)
        )
        // shoes
        drawCircle(hair, radius = w * 0.06f, center = Offset(centerX - hipW * 0.17f, legBottom))
        drawCircle(hair, radius = w * 0.06f, center = Offset(centerX + hipW * 0.17f, legBottom))

        // far arm (behind torso, static)
        drawLine(
            color = shirt.copy(alpha = 0.75f),
            start = Offset(centerX + shoulderW * 0.42f, shoulderY),
            end = Offset(centerX + shoulderW * 0.55f, torsoBottom * 0.95f),
            strokeWidth = w * 0.09f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // torso
        val torsoPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(centerX - shoulderW / 2, shoulderY)
            lineTo(centerX + shoulderW / 2, shoulderY)
            lineTo(centerX + hipW / 2, torsoBottom)
            lineTo(centerX - hipW / 2, torsoBottom)
            close()
        }
        drawPath(torsoPath, color = shirt)
        drawPath(torsoPath, color = outline, style = Stroke(width = 3f))

        // near arm — waves while talking
        val wave = if (isTalking) sin(talkPhase * 2f) * shoulderW * 0.2f else 0f
        val handX = if (isTalking) {
            centerX - shoulderW * 0.62f + wave
        } else {
            centerX - shoulderW * 0.6f
        }
        val handY = if (isTalking) {
            shoulderY + (torsoBottom - shoulderY) * 0.3f
        } else {
            torsoBottom * 0.9f
        }
        drawLine(
            color = shirt,
            start = Offset(centerX - shoulderW * 0.42f, shoulderY),
            end = Offset(handX, handY),
            strokeWidth = w * 0.09f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawCircle(skin, radius = w * 0.045f, center = Offset(handX, handY))

        // neck
        drawRoundRect(
            color = skin,
            topLeft = Offset(centerX - w * 0.045f, headCy + headR * 0.65f),
            size = androidx.compose.ui.geometry.Size(w * 0.09f, neckTop - (headCy + headR * 0.65f))
        )

        // hair back
        drawCircle(hair, radius = headR * 1.05f, center = Offset(centerX, headCy - headR * 0.15f))
        // head
        drawCircle(skin, radius = headR, center = Offset(centerX, headCy))
        // eyes
        val eyeR = headR * 0.1f
        drawCircle(Color(0xFF2A0F3D), radius = eyeR, center = Offset(centerX - headR * 0.38f, headCy))
        drawCircle(Color(0xFF2A0F3D), radius = eyeR, center = Offset(centerX + headR * 0.38f, headCy))
        // cheeks
        drawCircle(VoxOrange.copy(alpha = 0.5f), radius = 3f, center = Offset(centerX - headR * 0.75f, headCy + headR * 0.15f))
        drawCircle(VoxOrange.copy(alpha = 0.5f), radius = 3f, center = Offset(centerX + headR * 0.75f, headCy + headR * 0.15f))

        // floating speech badge — pulses with soundwave bars while talking
        val badgeCx = centerX + shoulderW * 0.85f
        val badgeFloat = sin(idlePhase * 1.4f) * (headR * 0.15f)
        val badgeCy = shoulderY + h * 0.02f + badgeFloat
        val badgeR = w * 0.15f

        drawCircle(badgeColor, radius = badgeR, center = Offset(badgeCx, badgeCy))

        val barGap = badgeR * 0.5f
        for (i in -1..1) {
            val barX = badgeCx + i * barGap
            val amp = if (isTalking) {
                (sin(talkPhase + i * 1.7f) + 1f) / 2f
            } else 0f
            val halfH = badgeR * (0.12f + 0.55f * amp)
            drawLine(
                color = Color.White,
                start = Offset(barX, badgeCy - halfH),
                end = Offset(barX, badgeCy + halfH),
                strokeWidth = w * 0.018f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }
}
