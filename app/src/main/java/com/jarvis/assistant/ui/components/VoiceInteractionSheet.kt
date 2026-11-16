package com.jarvis.assistant.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.data.model.AssistantState
import com.jarvis.assistant.ui.theme.SunsetBackgroundSecondary
import com.jarvis.assistant.ui.theme.SunsetCardBackground
import com.jarvis.assistant.ui.theme.SunsetCoral
import com.jarvis.assistant.ui.theme.SunsetPeach
import com.jarvis.assistant.ui.theme.SunsetPurple
import com.jarvis.assistant.ui.theme.SunsetTextPrimary
import com.jarvis.assistant.ui.theme.SunsetTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInteractionSheet(
    state: AssistantState,
    transcription: String,
    aiResponse: String,
    onDismiss: () -> Unit,
    onQuickPromptSelected: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SunsetCardBackground,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SunsetPeach.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "VOICE ENGINE • ${state.label.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SunsetCoral,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Voice",
                        tint = SunsetTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Audio Waveform Bars
            AudioWaveformVisualizer(isActive = state == AssistantState.LISTENING || state == AssistantState.SPEAKING)

            Spacer(modifier = Modifier.height(24.dp))

            // User Transcript / Prompt Display
            Text(
                text = if (transcription.isNotBlank()) "\"$transcription\"" else "Listening for Master's vocal instruction...",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SunsetTextPrimary,
                textAlign = TextAlign.Center
            )

            // AI vocal response
            if (aiResponse.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SunsetBackgroundSecondary)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = SunsetCoral,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = aiResponse,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SunsetTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Voice Command Chips
            Text(
                text = "SUGGESTED COMMANDS",
                style = MaterialTheme.typography.labelSmall,
                color = SunsetTextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            val suggestions = listOf(
                "Check battery & system telemetry",
                "Open WhatsApp and search Sarah",
                "Play synthwave on Spotify",
                "Recall core neural memories"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SunsetPeach.copy(alpha = 0.35f))
                            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .clickable { onQuickPromptSelected(prompt) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SunsetTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioWaveformVisualizer(isActive: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "WaveBars")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = 44f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 28f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 52f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 32f,
        targetValue = 68f,
        animationSpec = infiniteRepeatable(tween(320, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "w4"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(72.dp)
    ) {
        val heights = if (isActive) listOf(h1, h2, h4, h3, h2, h1, h3) else listOf(16f, 20f, 24f, 20f, 16f, 12f, 10f)
        heights.forEach { heightVal ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(heightVal.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(SunsetCoral, SunsetPurple)
                        )
                    )
            )
        }
    }
}
