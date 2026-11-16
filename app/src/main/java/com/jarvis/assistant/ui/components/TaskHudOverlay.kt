package com.jarvis.assistant.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.data.model.TaskHudState
import com.jarvis.assistant.data.model.TaskStatus
import com.jarvis.assistant.ui.theme.SunsetCardBackground
import com.jarvis.assistant.ui.theme.SunsetCoral
import com.jarvis.assistant.ui.theme.SunsetGreen
import com.jarvis.assistant.ui.theme.SunsetPeach
import com.jarvis.assistant.ui.theme.SunsetPurple
import com.jarvis.assistant.ui.theme.SunsetTextPrimary
import com.jarvis.assistant.ui.theme.SunsetTextSecondary

@Composable
fun TaskHudOverlay(
    state: TaskHudState,
    onCancelTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isActive,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(16.dp, shape = RoundedCornerShape(26.dp), spotColor = SunsetCoral.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(26.dp))
                .background(SunsetCardBackground)
                .border(1.dp, SunsetCoral.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header: Task title + Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = SunsetCoral,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = state.taskName.ifBlank { "Autonomous Assistant Action" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SunsetTextPrimary
                            )
                            Text(
                                text = "STATE: ${state.status.displayName.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SunsetCoral,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onCancelTask,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Task",
                            tint = SunsetTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Current Operation description
                Text(
                    text = state.currentOperation.ifBlank { "Inspecting screen hierarchy and preparing verified execution..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = SunsetTextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Four-step compact status bar
                FourStepProgressBar(currentStep = state.currentStepIndex)

                Spacer(modifier = Modifier.height(10.dp))

                // Linear progress indicator
                LinearProgressIndicator(
                    progress = (state.percentage / 100f).coerceIn(0f, 1f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = SunsetCoral,
                    trackColor = SunsetPeach.copy(alpha = 0.5f)
                )

                // Recent completed checks
                if (state.completedSteps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SunsetPeach.copy(alpha = 0.3f))
                            .padding(8.dp)
                    ) {
                        state.completedSteps.takeLast(2).forEach { step ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SunsetGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = step,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 11.sp,
                                    color = SunsetTextPrimary
                                )
                            }
                        }
                    }
                }

                // Verification Warning Alert
                if (state.needsVerification) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFFF3CD))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFF856404),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Verification required: Awaiting accessibility confirmation before final completion.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF856404),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FourStepProgressBar(currentStep: Int) {
    val steps = listOf("Preparing", "Observing", "Executing", "Verifying")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, stepName ->
            val stepNumber = index + 1
            val isDone = stepNumber < currentStep
            val isCurrent = stepNumber == currentStep

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone -> SunsetGreen
                                isCurrent -> SunsetCoral
                                else -> SunsetPeach.copy(alpha = 0.6f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDone || isCurrent) Color.White else SunsetTextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = stepName,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = if (isCurrent) SunsetCoral else SunsetTextSecondary,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
