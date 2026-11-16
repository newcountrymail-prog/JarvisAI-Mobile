package com.jarvis.assistant.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.data.model.ChatMessage
import com.jarvis.assistant.data.model.ChatMode
import com.jarvis.assistant.data.model.MessageSender
import com.jarvis.assistant.ui.theme.DarkCardBackground
import com.jarvis.assistant.ui.theme.SunsetBackground
import com.jarvis.assistant.ui.theme.SunsetCardBackground
import com.jarvis.assistant.ui.theme.SunsetCoral
import com.jarvis.assistant.ui.theme.SunsetPeach
import com.jarvis.assistant.ui.theme.SunsetPink
import com.jarvis.assistant.ui.theme.SunsetPurple
import com.jarvis.assistant.ui.theme.SunsetTextPrimary
import com.jarvis.assistant.ui.theme.SunsetTextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    isThinking: Boolean,
    onSendMessage: (String, ChatMode) -> Unit,
    onNewChat: () -> Unit,
    onDeleteMessage: (String) -> Unit,
    onRegenerateResponse: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onVoiceMicTapped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf(ChatMode.JARVIS) }
    var showAttachmentSheet by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll on new message
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SunsetBackground)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp) // Clearance for floating bottom bar & composer
        ) {
            // ==========================================
            // HEADER BAR
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Menu Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(SunsetCardBackground)
                        .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = SunsetCoral,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center: Title & Subtitle
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Master Neural Channel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SunsetTextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Jarvis • Gemini Live • Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = SunsetCoral,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Right: New Chat (+) Button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(4.dp, shape = CircleShape, spotColor = SunsetCoral.copy(alpha = 0.25f))
                        .clip(CircleShape)
                        .background(SunsetCardBackground)
                        .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                        .clickable(onClick = onNewChat),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Chat",
                        tint = SunsetCoral,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // ==========================================
            // CHAT AREA: Empty State or Message List
            // ==========================================
            if (messages.isEmpty() && !isThinking) {
                EmptyChatStarter(
                    onSuggestionClicked = { prompt, mode ->
                        selectedMode = mode
                        onSendMessage(prompt, mode)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Jarvis Message", message.text))
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onDelete = { onDeleteMessage(message.id) },
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, message.text)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Message"))
                            },
                            onRegenerate = onRegenerateResponse,
                            onSpeak = { onSpeakMessage(message.text) }
                        )
                    }

                    // Thinking state indicator
                    if (isThinking) {
                        item {
                            ThinkingIndicator()
                        }
                    }
                }
            }

            // ==========================================
            // MESSAGE COMPOSER PANEL
            // ==========================================
            ChatComposer(
                inputText = inputText,
                selectedMode = selectedMode,
                onInputChanged = { inputText = it },
                onModeChanged = { selectedMode = it },
                onAttachmentClicked = { showAttachmentSheet = true },
                onVoiceMicTapped = onVoiceMicTapped,
                onSend = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText.trim(), selectedMode)
                        inputText = ""
                    }
                }
            )
        }
    }

    // Attachment Bottom Sheet
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = SunsetCardBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Attach Media or Context",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SunsetTextPrimary
                )
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    AttachmentOption(
                        icon = Icons.Default.Image,
                        label = "Image",
                        onClick = {
                            showAttachmentSheet = false
                            Toast.makeText(context, "Select image from gallery", Toast.LENGTH_SHORT).show()
                        }
                    )
                    AttachmentOption(
                        icon = Icons.Default.AttachFile,
                        label = "Document",
                        onClick = {
                            showAttachmentSheet = false
                            Toast.makeText(context, "Select code/document file", Toast.LENGTH_SHORT).show()
                        }
                    )
                    AttachmentOption(
                        icon = Icons.Default.Code,
                        label = "Code Snippet",
                        onClick = {
                            showAttachmentSheet = false
                            selectedMode = ChatMode.CODE
                            inputText = "```kotlin\n// Paste code snippet here\n```"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit,
    onSpeak: () -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .shadow(
                    elevation = if (isUser) 4.dp else 2.dp,
                    shape = RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (isUser) 22.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 22.dp
                    ),
                    spotColor = SunsetCoral.copy(alpha = 0.2f)
                )
                .clip(
                    RoundedCornerShape(
                        topStart = 22.dp,
                        topEnd = 22.dp,
                        bottomStart = if (isUser) 22.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 22.dp
                    )
                )
                .background(
                    if (isUser) {
                        Brush.linearGradient(listOf(SunsetCoral, SunsetPurple))
                    } else if (message.isCode) {
                        SolidColor(DarkCardBackground)
                    } else {
                        SolidColor(SunsetCardBackground)
                    }
                )
                .border(
                    width = 1.dp,
                    color = if (isUser) Color.Transparent else Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable { showMenu = true }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                if (!isUser && message.isCode) {
                    Text(
                        text = "CODE ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SunsetCoral,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.text,
                    style = if (message.isCode) {
                        TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color(0xFFFFF0E9)
                        )
                    } else {
                        MaterialTheme.typography.bodyLarge.copy(
                            color = if (isUser) Color.White else SunsetTextPrimary
                        )
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isUser) Color.White.copy(alpha = 0.7f) else SunsetTextSecondary,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // Message options dropdown
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Copy") },
                    onClick = { showMenu = false; onCopy() },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, null, tint = SunsetCoral) }
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = { showMenu = false; onShare() },
                    leadingIcon = { Icon(Icons.Default.Share, null, tint = SunsetPurple) }
                )
                if (!isUser) {
                    DropdownMenuItem(
                        text = { Text("Speak aloud") },
                        onClick = { showMenu = false; onSpeak() },
                        leadingIcon = { Icon(Icons.Default.VolumeUp, null, tint = SunsetCoral) }
                    )
                    DropdownMenuItem(
                        text = { Text("Regenerate") },
                        onClick = { showMenu = false; onRegenerate() },
                        leadingIcon = { Icon(Icons.Default.Refresh, null, tint = SunsetPurple) }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { showMenu = false; onDelete() },
                    leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = Color.Red) }
                )
            }
        }
    }
}

@Composable
private fun ThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "ThinkingDots")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dots"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SunsetCardBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = SunsetCoral,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Jarvis is processing neural thoughts...",
            style = MaterialTheme.typography.bodyMedium,
            color = SunsetTextSecondary,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ChatComposer(
    inputText: String,
    selectedMode: ChatMode,
    onInputChanged: (String) -> Unit,
    onModeChanged: (ChatMode) -> Unit,
    onAttachmentClicked: () -> Unit,
    onVoiceMicTapped: () -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Mode Selector Pills: [ Jarvis | Code ]
        Row(
            modifier = Modifier.padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatModePill(
                title = "Jarvis Mode",
                isSelected = selectedMode == ChatMode.JARVIS,
                onClick = { onModeChanged(ChatMode.JARVIS) }
            )
            ChatModePill(
                title = "Code Mode",
                isSelected = selectedMode == ChatMode.CODE,
                onClick = { onModeChanged(ChatMode.CODE) }
            )
        }

        // Floating rounded input panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, shape = RoundedCornerShape(28.dp), spotColor = SunsetPurple.copy(alpha = 0.2f))
                .clip(RoundedCornerShape(28.dp))
                .background(SunsetCardBackground)
                .border(1.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(28.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachment (+) button
                IconButton(
                    onClick = onAttachmentClicked,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Attach",
                        tint = SunsetCoral,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Text Field
                BasicTextField(
                    value = inputText,
                    onValueChange = onInputChanged,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = SunsetTextPrimary),
                    maxLines = 4,
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = if (selectedMode == ChatMode.CODE) "Ask code or architecture questions..." else "Message Jarvis...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = SunsetTextSecondary.copy(alpha = 0.7f)
                            )
                        }
                        innerTextField()
                    }
                )

                // Voice Mic Button
                IconButton(
                    onClick = onVoiceMicTapped,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice input",
                        tint = SunsetPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Send Button (Large circular coral-red button with upward arrow)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(4.dp, shape = CircleShape, spotColor = SunsetCoral.copy(alpha = 0.4f))
                        .clip(CircleShape)
                        .background(SunsetCoral)
                        .clickable(onClick = onSend),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatModePill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) SunsetCoral else SunsetPeach.copy(alpha = 0.4f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else SunsetTextPrimary
        )
    }
}

@Composable
private fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(SunsetPeach.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = SunsetCoral, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = SunsetTextPrimary)
    }
}

@Composable
private fun EmptyChatStarter(
    onSuggestionClicked: (String, ChatMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SunsetPeach.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = SunsetCoral,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Master Neural Channel Ready",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = SunsetTextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Select a prompt below or start speaking to initiate voice/text commands.",
            style = MaterialTheme.typography.bodyMedium,
            color = SunsetTextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        val starterPrompts = listOf(
            Pair("Check battery & device telemetry status", ChatMode.JARVIS),
            Pair("Write a Kotlin coroutine worker for background tasks", ChatMode.CODE),
            Pair("Open WhatsApp and verify Sarah's contact alias", ChatMode.JARVIS),
            Pair("Explain Android Jetpack Compose 3D Canvas rendering", ChatMode.CODE)
        )

        starterPrompts.forEach { (prompt, mode) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SunsetCardBackground)
                    .border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .clickable { onSuggestionClicked(prompt, mode) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SunsetTextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = mode.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = SunsetCoral,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
