package com.jarvis.assistant.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.data.model.AppUpdateItem
import com.jarvis.assistant.ui.theme.SunsetBackground
import com.jarvis.assistant.ui.theme.SunsetCardBackground
import com.jarvis.assistant.ui.theme.SunsetCoral
import com.jarvis.assistant.ui.theme.SunsetGreen
import com.jarvis.assistant.ui.theme.SunsetPeach
import com.jarvis.assistant.ui.theme.SunsetPurple
import com.jarvis.assistant.ui.theme.SunsetTextPrimary
import com.jarvis.assistant.ui.theme.SunsetTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdatesScreen(
    modifier: Modifier = Modifier
) {
    // All 12 specified update cards from Section 7
    val allUpdates = remember {
        listOf(
            AppUpdateItem(
                id = "up_1",
                category = "AGENT TASKS",
                title = "Verified actions and recovery",
                description = "JARVIS observes the current screen, checks action results, tracks progress, and stops safely when the UI is unclear.",
                date = "v1.0.0 • Nov 16",
                statusBadge = "VERIFIED"
            ),
            AppUpdateItem(
                id = "up_2",
                category = "FLOATING AVATAR",
                title = "Always-on or working-only display",
                description = "Choose an avatar, double-tap for compact task progress, drag it anywhere on screen, and choose a dock edge in Settings.",
                date = "v1.0.0 • Nov 16",
                statusBadge = "CORE"
            ),
            AppUpdateItem(
                id = "up_3",
                category = "TASK PROGRESS",
                title = "Four-step compact status",
                description = "The task HUD shows the goal, recent steps, a running indicator, completed checks, and a subdued dark progress line.",
                date = "v1.0.0 • Nov 15",
                statusBadge = "HUD"
            ),
            AppUpdateItem(
                id = "up_4",
                category = "SILENT EXECUTION",
                title = "Quiet task automation",
                description = "When enabled, task narration is muted while ordinary conversation stays audible.",
                date = "v1.0.0 • Nov 15",
                statusBadge = "AUDIO"
            ),
            AppUpdateItem(
                id = "up_5",
                category = "WHATSAPP",
                title = "Saved contact lookup",
                description = "Contact aliases are supported. Grant Contacts permission first; JARVIS will report a missing permission separately from a name mismatch.",
                date = "v1.0.0 • Nov 14",
                statusBadge = "COMMS"
            ),
            AppUpdateItem(
                id = "up_6",
                category = "PHONE CALLS",
                title = "Permission-aware calling",
                description = "Normal calls require Contacts and Phone permissions. WhatsApp calls use WhatsApp and do not place a cellular call.",
                date = "v1.0.0 • Nov 14",
                statusBadge = "TELEPHONY"
            ),
            AppUpdateItem(
                id = "up_7",
                category = "GOOGLE FLOW / AI STUDIO",
                title = "Browser input diagnostics",
                description = "The captured Flow run could not verify the prompt input in Chrome. Some editor fields are not exposed to Accessibility; screen sharing must be active and have a fresh frame for visual targeting. The captured logs did not show a Google AI Studio page, so that failure still needs a fresh log capture.",
                date = "v1.0.0 • Nov 13",
                statusBadge = "DIAGNOSTIC"
            ),
            AppUpdateItem(
                id = "up_8",
                category = "INSTAGRAM",
                title = "Bulk action safety",
                description = "Bulk tasks track list progress and require end-of-list evidence. The captured run repeatedly reached the action limit and did not establish completion; avoid treating an unverified result as done.",
                date = "v1.0.0 • Nov 13",
                statusBadge = "SAFETY"
            ),
            AppUpdateItem(
                id = "up_9",
                category = "SPOTIFY",
                title = "Song search and playback checks",
                description = "Searches can inspect results and verify playback before reporting success; physical-device behavior still depends on the Spotify screen and account.",
                date = "v1.0.0 • Nov 12",
                statusBadge = "MEDIA"
            ),
            AppUpdateItem(
                id = "up_10",
                category = "PERMISSIONS",
                title = "Per-feature access status",
                description = "The System Authorizations page lists microphone, location, notifications, overlay, Accessibility, contacts, phone, camera, Bluetooth, battery, and brightness access.",
                date = "v1.0.0 • Nov 12",
                statusBadge = "SECURITY"
            ),
            AppUpdateItem(
                id = "up_11",
                category = "AI PERSONA",
                title = "Operator profile preview",
                description = "Preview your selected assistant voice and the name it will use before activating the persona.",
                date = "v1.0.0 • Nov 11",
                statusBadge = "PERSONA"
            ),
            AppUpdateItem(
                id = "up_12",
                category = "APPEARANCE",
                title = "Sunset theme by default",
                description = "Fresh installs start with the sunset theme; you can still select other themes in Settings.",
                date = "v1.0.0 • Nov 11",
                statusBadge = "THEME"
            )
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedUpdateForDetail by remember { mutableStateOf<AppUpdateItem?>(null) }

    val categories = remember {
        listOf("ALL", "AGENT TASKS", "FLOATING AVATAR", "TASK PROGRESS", "WHATSAPP", "PHONE CALLS", "SECURITY", "THEME")
    }

    val filteredUpdates = remember(searchQuery, selectedCategory) {
        allUpdates.filter { update ->
            val matchCat = selectedCategory == "ALL" || update.category.contains(selectedCategory, ignoreCase = true)
            val matchQuery = searchQuery.isBlank() ||
                    update.title.contains(searchQuery, ignoreCase = true) ||
                    update.description.contains(searchQuery, ignoreCase = true) ||
                    update.category.contains(searchQuery, ignoreCase = true)
            matchCat && matchQuery
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SunsetBackground)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp)
        ) {
            // Page Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Updates",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = SunsetTextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Jarvis updates & announcements",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SunsetTextSecondary
                        )
                    }

                    // Notification Icon Indicator
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(SunsetPeach.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Newspaper,
                            contentDescription = null,
                            tint = SunsetCoral,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Filter
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter updates or features...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = SunsetPurple) },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Chips Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) SunsetCoral else SunsetPeach.copy(alpha = 0.4f))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else SunsetTextPrimary
                            )
                        }
                    }
                }
            }

            // Updates Scrollable Feed
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredUpdates, key = { it.id }) { update ->
                    UpdateGlassCard(
                        item = update,
                        onClick = { selectedUpdateForDetail = update }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Update Detail View Sheet
    if (selectedUpdateForDetail != null) {
        val detail = selectedUpdateForDetail!!
        ModalBottomSheet(
            onDismissRequest = { selectedUpdateForDetail = null },
            containerColor = SunsetCardBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = detail.category,
                        style = MaterialTheme.typography.labelLarge,
                        color = SunsetCoral,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SunsetGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = detail.statusBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = SunsetGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SunsetTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = detail.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = SunsetPurple
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = detail.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SunsetTextPrimary,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
private fun UpdateGlassCard(
    item: AppUpdateItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = SunsetPurple.copy(alpha = 0.16f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(SunsetCardBackground)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.85f),
                shape = RoundedCornerShape(26.dp)
            )
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category label in uppercase coral
                Text(
                    text = item.category,
                    style = MaterialTheme.typography.labelLarge,
                    color = SunsetCoral,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SunsetGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.statusBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = SunsetGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = SunsetTextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bold feature title
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SunsetTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Detailed description
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = SunsetTextSecondary,
                lineHeight = 19.sp
            )
        }
    }
}
