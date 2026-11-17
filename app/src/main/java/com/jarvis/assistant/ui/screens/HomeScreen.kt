package com.jarvis.assistant.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.jarvis.assistant.data.model.WeatherData
import com.jarvis.assistant.ui.components.FuturisticAiOrb
import com.jarvis.assistant.ui.components.QuickAccessCards
import com.jarvis.assistant.ui.components.WeatherCard
import com.jarvis.assistant.ui.theme.SunsetBackground
import com.jarvis.assistant.ui.theme.SunsetBackgroundSecondary
import com.jarvis.assistant.ui.theme.SunsetBlue
import com.jarvis.assistant.ui.theme.SunsetCardBackground
import com.jarvis.assistant.ui.theme.SunsetCoral
import com.jarvis.assistant.ui.theme.SunsetGreen
import com.jarvis.assistant.ui.theme.SunsetPeach
import com.jarvis.assistant.ui.theme.SunsetPurple
import com.jarvis.assistant.ui.theme.SunsetTextPrimary
import com.jarvis.assistant.ui.theme.SunsetTextSecondary
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    assistantState: AssistantState,
    weatherState: WeatherData,
    currentLanguage: String,
    onPowerToggle: () -> Unit,
    onNavigateChat: () -> Unit,
    onNavigateSettings: () -> Unit,
    onRefreshWeather: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onMemoriesTapped: () -> Unit,
    onRequestScreenShare: () -> Unit,
    audioAmplitude: Float = 0f,
    orbConfig: com.jarvis.assistant.data.model.OrbCustomizationConfig = com.jarvis.assistant.data.model.OrbCustomizationConfig(),
    geminiStatus: com.jarvis.assistant.ai.gemini.GeminiConnectionStatus = com.jarvis.assistant.ai.gemini.GeminiConnectionStatus.DISCONNECTED,
    modifier: Modifier = Modifier
) {
    var showLanguageModal by remember { mutableStateOf(false) }
    var showScreenShareDialog by remember { mutableStateOf(false) }

    // Dynamic greeting based on current local hour
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning, Master"
            hour < 17 -> "Good afternoon, Master"
            else -> "Good evening, Master"
        }
    }

    val isOnline = assistantState != AssistantState.OFFLINE
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SunsetBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = 110.dp), // Space for floating bottom navigation
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // ==========================================
            // HEADER BAR: Chat (Left), JARVIS + Greeting (Center), Settings (Right)
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Rounded Chat Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(4.dp, shape = CircleShape, spotColor = SunsetPurple.copy(alpha = 0.2f))
                        .clip(CircleShape)
                        .background(SunsetCardBackground)
                        .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .clickable(onClick = onNavigateChat),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Open Chat",
                        tint = SunsetCoral,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Center: JARVIS Branding + Dynamic Greeting
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "JARVIS",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 2.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SunsetCoral
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SunsetTextPrimary
                    )
                }

                // Right: Rounded Settings Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(4.dp, shape = CircleShape, spotColor = SunsetPurple.copy(alpha = 0.2f))
                        .clip(CircleShape)
                        .background(SunsetCardBackground)
                        .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .clickable(onClick = onNavigateSettings),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open Settings",
                        tint = SunsetPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ==========================================
            // MAIN AI ORB (Animated 3D Particle Sphere)
            // ==========================================
            FuturisticAiOrb(
                state = assistantState,
                size = 250.dp,
                audioAmplitude = audioAmplitude,
                config = orbConfig,
                onOrbTapped = onPowerToggle
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // POWER CONTROL STATUS BUTTON
            // ==========================================
            val powerBgColor by animateColorAsState(
                targetValue = if (isOnline) SunsetCardBackground else SunsetCardBackground.copy(alpha = 0.8f),
                label = "powerBg"
            )

            Box(
                modifier = Modifier
                    .shadow(8.dp, shape = RoundedCornerShape(28.dp), spotColor = SunsetCoral.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(28.dp))
                    .background(powerBgColor)
                    .border(
                        width = 1.2.dp,
                        color = if (isOnline) SunsetCoral.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .clickable(onClick = onPowerToggle)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Status indicator dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isOnline) SunsetGreen else SunsetCoral.copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isOnline) "JARVIS ONLINE • READY" else "JARVIS OFF • TAP TO START",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOnline) SunsetTextPrimary else SunsetTextSecondary,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isOnline) "Neural link calibrated • Master instructions active" else "Tap mic or orb to power on.",
                style = MaterialTheme.typography.labelSmall,
                color = SunsetTextSecondary,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ==========================================
            // ACTUAL AI PROVIDER CONNECTION BADGE
            // Mandatory Section 11 Requirement
            // ==========================================
            val geminiBadgeColor = when (geminiStatus) {
                com.jarvis.assistant.ai.gemini.GeminiConnectionStatus.CONNECTED -> SunsetGreen
                com.jarvis.assistant.ai.gemini.GeminiConnectionStatus.UNVERIFIED -> Color(0xFFD97706)
                com.jarvis.assistant.ai.gemini.GeminiConnectionStatus.DISCONNECTED -> Color(0xFFDC2626)
                com.jarvis.assistant.ai.gemini.GeminiConnectionStatus.TESTING -> SunsetBlue
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SunsetCardBackground)
                    .border(1.dp, geminiBadgeColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .clickable(onClick = onNavigateSettings)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(geminiBadgeColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = geminiStatus.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = geminiBadgeColor,
                        fontSize = 10.5.sp,
                        letterSpacing = 0.4.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ==========================================
            // LIVE WEATHER CARD
            // ==========================================
            WeatherCard(
                weather = weatherState,
                onRefresh = onRefreshWeather
            )

            Spacer(modifier = Modifier.height(18.dp))

            // ==========================================
            // THREE HORIZONTALLY ARRANGED QUICK ACCESS CARDS
            // 1. SCREEN, 2. MEMORIES, 3. LANGUAGE
            // ==========================================
            QuickAccessCards(
                currentLanguage = currentLanguage,
                onScreenTapped = { showScreenShareDialog = true },
                onMemoriesTapped = onMemoriesTapped,
                onLanguageTapped = { showLanguageModal = true }
            )
        }
    }

    // Language Selection Modal
    if (showLanguageModal) {
        LanguageSelectionSheet(
            currentLanguage = currentLanguage,
            onDismiss = { showLanguageModal = false },
            onSelect = {
                onLanguageSelected(it)
                showLanguageModal = false
            }
        )
    }

    // Screen Sharing Dialog
    if (showScreenShareDialog) {
        AlertDialog(
            onDismissRequest = { showScreenShareDialog = false },
            containerColor = SunsetCardBackground,
            shape = RoundedCornerShape(26.dp),
            title = {
                Text(
                    text = "Screen Sharing & Context",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SunsetTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "JARVIS will observe visual on-screen hierarchy to verify automation actions, recognize button targets, and ensure accurate execution without blind touches.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SunsetTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showScreenShareDialog = false
                        onRequestScreenShare()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Grant & Activate", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showScreenShareDialog = false }) {
                    Text("Dismiss", color = SunsetTextSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelectionSheet(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val languages = listOf("English", "Hindi", "Hinglish", "Auto Detect")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SunsetCardBackground,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Select Conversation Language",
                style = MaterialTheme.typography.headlineSmall,
                color = SunsetTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Choose your preferred linguistic modality for Jarvis vocal and text replies.",
                style = MaterialTheme.typography.bodyMedium,
                color = SunsetTextSecondary
            )

            Spacer(modifier = Modifier.height(18.dp))

            languages.forEach { lang ->
                val isSelected = currentLanguage.equals(lang, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) SunsetPeach.copy(alpha = 0.5f) else Color.Transparent)
                        .clickable { onSelect(lang) }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = lang,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) SunsetCoral else SunsetTextPrimary
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SunsetCoral,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
