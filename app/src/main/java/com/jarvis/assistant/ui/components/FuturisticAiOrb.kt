package com.jarvis.assistant.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.data.model.AssistantState
import com.jarvis.assistant.data.model.OrbCustomizationConfig
import com.jarvis.assistant.ui.theme.SunsetBlue
import com.jarvis.assistant.ui.theme.SunsetCoral
import com.jarvis.assistant.ui.theme.SunsetPink
import com.jarvis.assistant.ui.theme.SunsetPurple
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// 3D Point on spherical shell
private data class Point3D(val x: Float, val y: Float, val z: Float, val colorVariant: Int)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FuturisticAiOrb(
    state: AssistantState,
    modifier: Modifier = Modifier,
    size: Dp = 260.dp,
    audioAmplitude: Float = 0f,
    config: OrbCustomizationConfig = OrbCustomizationConfig(),
    onOrbTapped: () -> Unit = {},
    onOrbDoubleTapped: () -> Unit = {},
    onOrbLongPressed: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbMotion")

    // Dynamic rotation speed based on state
    val baseRotationSpeedMs = when (state) {
        AssistantState.OFFLINE -> 24000
        AssistantState.IDLE -> 8000
        AssistantState.LISTENING -> 3500
        AssistantState.THINKING -> 1800
        AssistantState.PROCESSING -> 1400
        AssistantState.SPEAKING -> 3000
        AssistantState.EXECUTING_TASK -> 2200
        AssistantState.COMPLETED -> 6000
        AssistantState.ERROR -> 5000
    }

    val effectiveSpeed = (baseRotationSpeedMs / config.rotationSpeedScale.coerceIn(0.5f, 2.5f)).toInt()

    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = effectiveSpeed, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Breathing pulse
    val breathing by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (state == AssistantState.SPEAKING || state == AssistantState.LISTENING) 800 else 2200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbBreathing"
    )

    // Secondary ripple for listening / speaking
    val ripplePulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRipple"
    )

    // Particle count scaled by density and battery saver
    val particleCount = if (config.isBatterySaver) {
        (90 * config.particleDensity).toInt().coerceIn(40, 120)
    } else {
        (260 * config.particleDensity).toInt().coerceIn(80, 360)
    }

    val particles = remember(particleCount) {
        val rand = Random(42)
        val list = mutableListOf<Point3D>()
        for (i in 0 until particleCount) {
            val u = rand.nextFloat() * 2f - 1f
            val phi = rand.nextFloat() * 2f * PI.toFloat()
            val radius = 0.75f + rand.nextFloat() * 0.25f
            val sinTheta = kotlin.math.sqrt(1f - u * u)
            val x = radius * sinTheta * cos(phi)
            val y = radius * sinTheta * sin(phi)
            val z = radius * u
            val variant = rand.nextInt(3)
            list.add(Point3D(x, y, z, variant))
        }
        list
    }

    val effectiveSize = size * config.sizeScale.coerceIn(0.7f, 1.4f)

    Box(
        modifier = modifier
            .size(effectiveSize)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOrbTapped,
                onDoubleClick = onOrbDoubleTapped,
                onLongClick = onOrbLongPressed
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(effectiveSize)) {
            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)

            // Audio amplitude reactivity boost
            val audioBoost = if (config.audioReactivity) (audioAmplitude.coerceIn(0f, 1f) * 0.25f) else 0f
            val baseRadius = (this.size.minDimension / 2f) * 0.72f * (breathing + audioBoost)

            // Colors tailored to AI state
            val (coreColor, auraColor, ringColor) = when (state) {
                AssistantState.OFFLINE -> Triple(
                    SunsetPurple.copy(alpha = 0.25f),
                    SunsetPurple.copy(alpha = 0.08f),
                    SunsetPurple.copy(alpha = 0.2f)
                )
                AssistantState.IDLE -> Triple(
                    SunsetCoral.copy(alpha = 0.65f),
                    SunsetPink.copy(alpha = 0.35f),
                    SunsetCoral.copy(alpha = 0.45f)
                )
                AssistantState.LISTENING -> Triple(
                    SunsetBlue.copy(alpha = 0.85f),
                    SunsetPurple.copy(alpha = 0.5f),
                    SunsetBlue.copy(alpha = 0.75f)
                )
                AssistantState.THINKING -> Triple(
                    SunsetPurple.copy(alpha = 0.9f),
                    SunsetCoral.copy(alpha = 0.55f),
                    SunsetPurple.copy(alpha = 0.8f)
                )
                AssistantState.PROCESSING -> Triple(
                    SunsetCoral.copy(alpha = 0.95f),
                    SunsetBlue.copy(alpha = 0.6f),
                    SunsetCoral.copy(alpha = 0.85f)
                )
                AssistantState.SPEAKING -> Triple(
                    SunsetCoral.copy(alpha = 0.95f),
                    SunsetPink.copy(alpha = 0.7f),
                    SunsetCoral.copy(alpha = 0.9f)
                )
                AssistantState.EXECUTING_TASK -> Triple(
                    SunsetBlue.copy(alpha = 0.9f),
                    SunsetCoral.copy(alpha = 0.65f),
                    SunsetBlue.copy(alpha = 0.8f)
                )
                AssistantState.COMPLETED -> Triple(
                    Color(0xFF00E676).copy(alpha = 0.9f),
                    Color(0xFF00B0FF).copy(alpha = 0.5f),
                    Color(0xFF00E676).copy(alpha = 0.8f)
                )
                AssistantState.ERROR -> Triple(
                    Color(0xFFE53935).copy(alpha = 0.85f),
                    Color(0xFFD32F2F).copy(alpha = 0.4f),
                    Color(0xFFE53935).copy(alpha = 0.7f)
                )
            }

            // 1. Outer Glowing Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(coreColor, auraColor, Color.Transparent),
                    center = centerOffset,
                    radius = baseRadius * 1.45f
                ),
                radius = baseRadius * 1.45f,
                center = centerOffset
            )

            // 2. Ripple rings if active (Listening / Speaking)
            if (state == AssistantState.LISTENING || state == AssistantState.SPEAKING) {
                val rippleRadius = baseRadius * (ripplePulse + audioBoost)
                drawCircle(
                    color = ringColor.copy(alpha = (1f - (ripplePulse - 0.6f) / 0.85f).coerceIn(0f, 0.75f)),
                    radius = rippleRadius,
                    center = centerOffset,
                    style = Stroke(width = 2.5f)
                )
            }

            // 3. Orbital Rings
            val radAngle = (angle * PI.toFloat()) / 180f
            val radAngleReverse = (-angle * 0.75f * PI.toFloat()) / 180f

            drawCircle(
                color = ringColor.copy(alpha = 0.45f),
                radius = baseRadius * 1.05f,
                center = centerOffset,
                style = Stroke(
                    width = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )

            drawCircle(
                color = SunsetPurple.copy(alpha = 0.35f),
                radius = baseRadius * 1.18f,
                center = centerOffset,
                style = Stroke(
                    width = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 14f), angle * 2f)
                )
            )

            // 4. Render 3D Projected Particle Cloud
            val cosRot = cos(radAngle)
            val sinRot = sin(radAngle)
            val cosRotRev = cos(radAngleReverse)
            val sinRotRev = sin(radAngleReverse)

            for (p in particles) {
                val x1 = p.x * cosRot - p.z * sinRot
                val z1 = p.x * sinRot + p.z * cosRot

                val y2 = p.y * cosRotRev - z1 * sinRotRev
                val z2 = p.y * sinRotRev + z1 * cosRotRev

                val depthFactor = (z2 + 1.2f) / 2.4f
                val screenX = centerOffset.x + x1 * baseRadius
                val screenY = centerOffset.y + y2 * baseRadius

                val particleRadius = (1.5f + depthFactor * 3.5f) * if (state == AssistantState.OFFLINE) 0.7f else 1f
                val particleAlpha = (0.2f + depthFactor * 0.8f) * if (state == AssistantState.OFFLINE) 0.3f else 1f

                val particleColor = when (p.colorVariant) {
                    0 -> SunsetCoral
                    1 -> SunsetPurple
                    else -> SunsetPink
                }

                drawCircle(
                    color = particleColor.copy(alpha = particleAlpha.coerceIn(0f, 1f)),
                    radius = particleRadius,
                    center = Offset(screenX, screenY)
                )
            }

            // 5. Center Core Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (state == AssistantState.OFFLINE) 0.2f else 0.88f),
                        coreColor,
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = baseRadius * (0.45f + audioBoost * 0.5f)
                ),
                radius = baseRadius * (0.45f + audioBoost * 0.5f),
                center = centerOffset
            )
        }
    }
}
