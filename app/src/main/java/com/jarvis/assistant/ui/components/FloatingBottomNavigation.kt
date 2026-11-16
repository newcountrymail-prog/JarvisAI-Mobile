package com.jarvis.assistant.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jarvis.assistant.data.model.AssistantState
import com.jarvis.assistant.ui.theme.SunsetBlue
import com.jarvis.assistant.ui.theme.SunsetCardBackground
import com.jarvis.assistant.ui.theme.SunsetCoral
import com.jarvis.assistant.ui.theme.SunsetDarkPurple
import com.jarvis.assistant.ui.theme.SunsetPeach
import com.jarvis.assistant.ui.theme.SunsetPurple
import com.jarvis.assistant.ui.theme.SunsetTextPrimary
import com.jarvis.assistant.ui.theme.SunsetTextSecondary

enum class NavDestination(val route: String, val label: String) {
    HOME("home", "Home"),
    CHAT("chat", "Chat"),
    SETTINGS("settings", "Settings"),
    UPDATES("updates", "Updates")
}

@Composable
fun FloatingBottomNavigation(
    currentDestination: String,
    assistantState: AssistantState,
    onNavigate: (String) -> Unit,
    onMicTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating pill container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(elevation = 14.dp, shape = RoundedCornerShape(36.dp), spotColor = SunsetPurple.copy(alpha = 0.25f))
                .clip(RoundedCornerShape(36.dp))
                .background(SunsetCardBackground.copy(alpha = 0.94f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(36.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Home
                NavItem(
                    icon = Icons.Default.Home,
                    isSelected = currentDestination == NavDestination.HOME.route,
                    onClick = { onNavigate(NavDestination.HOME.route) }
                )

                // 2. Chat
                NavItem(
                    icon = Icons.Default.ChatBubbleOutline,
                    isSelected = currentDestination == NavDestination.CHAT.route,
                    onClick = { onNavigate(NavDestination.CHAT.route) }
                )

                // Spacer for central prominent mic
                Box(modifier = Modifier.size(62.dp))

                // 4. Settings
                NavItem(
                    icon = Icons.Default.Settings,
                    isSelected = currentDestination == NavDestination.SETTINGS.route,
                    onClick = { onNavigate(NavDestination.SETTINGS.route) }
                )

                // 5. Updates
                NavItem(
                    icon = Icons.Default.Newspaper,
                    isSelected = currentDestination == NavDestination.UPDATES.route,
                    onClick = { onNavigate(NavDestination.UPDATES.route) }
                )
            }
        }

        // 3. Central Prominent Microphone Button (Layered slightly raised above the pill)
        CentralMicButton(
            state = assistantState,
            onClick = onMicTapped,
            modifier = Modifier.offset(y = (-14).dp)
        )
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) SunsetTextPrimary else SunsetTextSecondary.copy(alpha = 0.8f),
        label = "NavIconColor"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) SunsetPeach.copy(alpha = 0.75f) else Color.Transparent,
        label = "NavBgColor"
    )

    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun CentralMicButton(
    state: AssistantState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CentralMicPulse")

    // Ripple scale when active
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Restart
        ),
        label = "MicRipple"
    )

    val isActive = state == AssistantState.LISTENING || state == AssistantState.SPEAKING || state == AssistantState.PROCESSING

    Box(
        modifier = modifier
            .size(72.dp),
        contentAlignment = Alignment.Center
    ) {
        // Active pulsing aura ring
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(68.dp * rippleScale)
                    .clip(CircleShape)
                    .background(SunsetCoral.copy(alpha = (1.3f - rippleScale).coerceIn(0f, 0.45f)))
            )
        }

        // Button container
        Box(
            modifier = Modifier
                .size(62.dp)
                .shadow(elevation = 10.dp, shape = CircleShape, spotColor = SunsetCoral.copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(SunsetCoral, SunsetPurple)
                    )
                )
                .border(
                    width = 2.5.dp,
                    color = SunsetBlue.copy(alpha = 0.75f),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Voice Interaction",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
