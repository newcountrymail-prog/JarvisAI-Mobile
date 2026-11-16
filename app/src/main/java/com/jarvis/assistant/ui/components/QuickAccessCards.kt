package com.jarvis.assistant.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.ui.theme.SunsetCardBackground
import com.jarvis.assistant.ui.theme.SunsetCoral
import com.jarvis.assistant.ui.theme.SunsetPeach
import com.jarvis.assistant.ui.theme.SunsetPurple
import com.jarvis.assistant.ui.theme.SunsetTextPrimary
import com.jarvis.assistant.ui.theme.SunsetTextSecondary

@Composable
fun QuickAccessCards(
    currentLanguage: String,
    onScreenTapped: () -> Unit,
    onMemoriesTapped: () -> Unit,
    onLanguageTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // CARD 1: SCREEN
        QuickCardItem(
            icon = Icons.Default.ScreenShare,
            title = "Screen",
            subtitle = "Share screen",
            onClick = onScreenTapped,
            modifier = Modifier.weight(1f)
        )

        // CARD 2: MEMORIES
        QuickCardItem(
            icon = Icons.Default.Layers,
            title = "Memories",
            subtitle = "Recall Core",
            onClick = onMemoriesTapped,
            modifier = Modifier.weight(1f)
        )

        // CARD 3: LANGUAGE
        QuickCardItem(
            icon = Icons.Default.Language,
            title = "Language",
            subtitle = currentLanguage,
            onClick = onLanguageTapped,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickCardItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = SunsetPurple.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(SunsetCardBackground)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.8f),
                shape = RoundedCornerShape(22.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(SunsetPeach.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = SunsetCoral,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SunsetTextPrimary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = SunsetTextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}
