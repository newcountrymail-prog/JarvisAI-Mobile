package com.jarvis.assistant.ui.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.data.model.WeatherData
import com.jarvis.assistant.ui.theme.SunsetCardBackground
import com.jarvis.assistant.ui.theme.SunsetCoral
import com.jarvis.assistant.ui.theme.SunsetPeach
import com.jarvis.assistant.ui.theme.SunsetPurple
import com.jarvis.assistant.ui.theme.SunsetTextPrimary
import com.jarvis.assistant.ui.theme.SunsetTextSecondary

@Composable
fun WeatherCard(
    weather: WeatherData,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "WeatherRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RefreshSpin"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = SunsetPurple.copy(alpha = 0.18f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(SunsetCardBackground)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.85f),
                shape = RoundedCornerShape(28.dp)
            )
            .clickable { onRefresh() }
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Temperature, Condition, Location
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = weather.temperature,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = SunsetTextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = weather.condition,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SunsetCoral,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = weather.location,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = SunsetTextSecondary
                    )
                }

                // Right Column: Icon & Refresh indicator
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(SunsetPeach.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (weather.isLoading) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refreshing",
                            tint = SunsetCoral,
                            modifier = Modifier
                                .size(28.dp)
                                .rotate(rotation)
                        )
                    } else {
                        Icon(
                            imageVector = if (weather.isDay) Icons.Default.WbSunny else Icons.Default.Cloud,
                            contentDescription = "Weather Icon",
                            tint = SunsetCoral,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle & Status row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tap to refresh live atmosphere.",
                    style = MaterialTheme.typography.labelSmall,
                    color = SunsetTextSecondary,
                    fontSize = 11.sp
                )

                Text(
                    text = "Updated: ${weather.lastUpdated}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SunsetPurple,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Error notice & Retry if any
            if (weather.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3CD))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFF856404),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = weather.error,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF856404),
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = "RETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = SunsetCoral,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onRefresh() }
                    )
                }
            }
        }
    }
}
