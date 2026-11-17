package com.jarvis.assistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.device.DeviceAutomationController
import com.jarvis.assistant.ui.theme.MidnightBackground
import com.jarvis.assistant.ui.theme.MidnightBorder
import com.jarvis.assistant.ui.theme.MidnightCardBackground
import com.jarvis.assistant.ui.theme.MidnightElectricBlue
import com.jarvis.assistant.ui.theme.MidnightEmerald
import com.jarvis.assistant.ui.theme.MidnightNeonViolet
import com.jarvis.assistant.ui.theme.MidnightTextPrimary
import com.jarvis.assistant.ui.theme.MidnightTextSecondary

@Composable
fun ToolboxScreen(
    deviceController: DeviceAutomationController,
    onNavigateTerminal: () -> Unit,
    onNavigateSecurityLab: () -> Unit,
    onNavigateQwenCoder: () -> Unit,
    onNavigateMemories: () -> Unit,
    onNavigateGeminiDiagnostics: () -> Unit = {},
    onNavigateAiControlCenter: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val telemetry = remember { deviceController.getDeviceTelemetry() }
    var currentVolume by remember { mutableFloatStateOf(telemetry.mediaVolumePercent.toFloat()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 110.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // HEADER
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "JARVIS X TOOLBOX",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidnightElectricBlue
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Autonomous Intelligence & Hardware Suite",
                    style = MaterialTheme.typography.titleMedium,
                    color = MidnightTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 1: CORE AUTOMATION MODULES
            Text(
                text = "ENGINEERING & DEFENSIVE LABS",
                style = MaterialTheme.typography.labelLarge,
                color = MidnightTextSecondary,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            ToolLaunchCard(
                title = "AI Control Center (Multi-Model)",
                subtitle = "Universal API manager, free model library & autonomous provider rotation",
                icon = Icons.Default.Bolt,
                accentColor = Color(0xFF00E5FF),
                onClick = onNavigateAiControlCenter
            )

            Spacer(modifier = Modifier.height(10.dp))

            ToolLaunchCard(
                title = "Gemini Engine Diagnostics",
                subtitle = "Universal detection, model compatibility testing & self-repair",
                icon = Icons.Default.Bolt,
                accentColor = Color(0xFFF16D43),
                onClick = onNavigateGeminiDiagnostics
            )

            Spacer(modifier = Modifier.height(10.dp))

            ToolLaunchCard(
                title = "Autonomous Termux Engine",
                subtitle = "Natural language shell execution, package manager & error recovery",
                icon = Icons.Default.Terminal,
                accentColor = MidnightElectricBlue,
                onClick = onNavigateTerminal
            )

            Spacer(modifier = Modifier.height(10.dp))

            ToolLaunchCard(
                title = "JARVIS Security Lab",
                subtitle = "10 defensive cybersecurity tools, password entropy & TLS inspector",
                icon = Icons.Default.Security,
                accentColor = MidnightEmerald,
                onClick = onNavigateSecurityLab
            )

            Spacer(modifier = Modifier.height(10.dp))

            ToolLaunchCard(
                title = "Qwen Coder Workspace",
                subtitle = "Specialized Kotlin, Jetpack Compose, Room & Coroutines engine",
                icon = Icons.Default.Code,
                accentColor = MidnightNeonViolet,
                onClick = onNavigateQwenCoder
            )

            Spacer(modifier = Modifier.height(10.dp))

            ToolLaunchCard(
                title = "Neural Memory Vault",
                subtitle = "Persistent user rules, preferences, contextual entities & facts",
                icon = Icons.Default.Psychology,
                accentColor = Color(0xFFFFB74D),
                onClick = onNavigateMemories
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: HARDWARE TELEMETRY
            Text(
                text = "SYSTEM TELEMETRY & HARDWARE",
                style = MaterialTheme.typography.labelLarge,
                color = MidnightTextSecondary,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MidnightCardBackground)
                    .border(1.dp, MidnightBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // RAM
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = MidnightElectricBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("RAM Memory", color = MidnightTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = telemetry.memory?.let { "${it.availableFormatted} free / ${it.totalFormatted}" } ?: "Audited",
                            color = MidnightTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Storage
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = MidnightEmerald, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Internal Storage", color = MidnightTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = telemetry.storage?.let { "${it.freeFormatted} available (${it.usagePercent}% used)" } ?: "Audited",
                            color = MidnightTextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Battery
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (telemetry.isCharging) MidnightEmerald else MidnightElectricBlue)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Battery Power", color = MidnightTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text(
                            text = "${telemetry.batteryLevel}% ${if (telemetry.isCharging) "(Charging)" else ""}",
                            color = MidnightEmerald,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Volume Slider
                    Column {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MidnightElectricBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Media Speaker Volume", color = MidnightTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text("${currentVolume.toInt()}%", color = MidnightElectricBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = currentVolume,
                            onValueChange = {
                                currentVolume = it
                                deviceController.setMediaVolumePercent(it.toInt())
                            },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = MidnightElectricBlue,
                                activeTrackColor = MidnightElectricBlue,
                                inactiveTrackColor = MidnightBorder
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 3: SYSTEM SHORTCUTS
            Text(
                text = "ANDROID SYSTEM SHORTCUTS",
                style = MaterialTheme.typography.labelLarge,
                color = MidnightTextSecondary,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShortcutCard("Display", Icons.Default.DisplaySettings, Modifier.weight(1f)) {
                    deviceController.openDisplaySettings()
                }
                ShortcutCard("Sound", Icons.Default.VolumeUp, Modifier.weight(1f)) {
                    deviceController.openSoundSettings()
                }
                ShortcutCard("Wi-Fi", Icons.Default.Wifi, Modifier.weight(1f)) {
                    deviceController.openWifiSettings()
                }
                ShortcutCard("System", Icons.Default.Settings, Modifier.weight(1f)) {
                    deviceController.openSystemSettings()
                }
            }
        }
    }
}

@Composable
private fun ToolLaunchCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MidnightCardBackground)
            .border(1.dp, MidnightBorder, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MidnightTextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MidnightTextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ShortcutCard(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MidnightCardBackground)
            .border(1.dp, MidnightBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = MidnightElectricBlue, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = label, color = MidnightTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}
