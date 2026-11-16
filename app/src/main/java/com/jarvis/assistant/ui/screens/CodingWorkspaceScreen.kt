package com.jarvis.assistant.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.ai.AiProviderManager
import com.jarvis.assistant.data.model.ProviderRole
import com.jarvis.assistant.ui.theme.MidnightBackground
import com.jarvis.assistant.ui.theme.MidnightBorder
import com.jarvis.assistant.ui.theme.MidnightCardBackground
import com.jarvis.assistant.ui.theme.MidnightElectricBlue
import com.jarvis.assistant.ui.theme.MidnightEmerald
import com.jarvis.assistant.ui.theme.MidnightNeonViolet
import com.jarvis.assistant.ui.theme.MidnightTextPrimary
import com.jarvis.assistant.ui.theme.MidnightTextSecondary
import kotlinx.coroutines.launch

@Composable
fun CodingWorkspaceScreen(
    aiProviderManager: AiProviderManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var promptInput by remember { mutableStateOf("") }
    var codeOutput by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var latencyMs by remember { mutableStateOf(0L) }
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val quickTemplates = listOf(
        "Room Entity aur DAO create karo",
        "Compose custom canvas modifier",
        "Coroutine Flow state repository",
        "Gradle dependency migration AGP 8",
        "Termux process builder helper"
    )

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
                .padding(bottom = 90.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MidnightTextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "QWEN CODER WORKSPACE",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightElectricBlue
                    )
                    Text(
                        text = "Kotlin & Android Architecture Engine",
                        style = MaterialTheme.typography.titleMedium,
                        color = MidnightTextSecondary
                    )
                }

                Box(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QUICK CHIPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickTemplates.forEach { template ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MidnightCardBackground)
                            .border(1.dp, MidnightBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .clickable { promptInput = template }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = template,
                            style = MaterialTheme.typography.labelSmall,
                            color = MidnightTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PROMPT INPUT
            OutlinedTextField(
                value = promptInput,
                onValueChange = { promptInput = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Describe code to generate or refactor (e.g. Jetpack Compose swipeable card with animated dismiss)",
                        color = MidnightTextSecondary.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MidnightElectricBlue,
                    unfocusedBorderColor = MidnightBorder,
                    focusedContainerColor = MidnightCardBackground,
                    unfocusedContainerColor = MidnightCardBackground,
                    focusedTextColor = MidnightTextPrimary,
                    unfocusedTextColor = MidnightTextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // GENERATE BUTTON
            Button(
                onClick = {
                    if (promptInput.isNotBlank()) {
                        scope.launch {
                            isGenerating = true
                            val start = System.currentTimeMillis()
                            val res = aiProviderManager.generateResponse(
                                prompt = promptInput,
                                systemInstruction = "You are Qwen Coder AI, an elite Kotlin & Android architect. Output production-ready, clean, idiomatic code with clear explanations.",
                                role = ProviderRole.CODING
                            )
                            latencyMs = System.currentTimeMillis() - start
                            if (res.isSuccess) {
                                codeOutput = res.getOrThrow().text
                            } else {
                                codeOutput = "// Error generating code: ${res.exceptionOrNull()?.message}"
                            }
                            isGenerating = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MidnightElectricBlue),
                shape = RoundedCornerShape(16.dp),
                enabled = !isGenerating && promptInput.isNotBlank()
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compiling AI Architecture...", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    Icon(imageVector = Icons.Default.Code, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Android Code", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // CODE DISPLAY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (latencyMs > 0) "Generated in ${latencyMs}ms" else "Code Output",
                    style = MaterialTheme.typography.labelLarge,
                    color = MidnightEmerald,
                    fontSize = 12.sp
                )

                if (codeOutput.isNotBlank()) {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(codeOutput))
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Code",
                            tint = MidnightElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF070B14))
                    .border(1.dp, MidnightBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                val codeScroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(codeScroll)
                ) {
                    if (codeOutput.isEmpty()) {
                        Text(
                            text = "// Qwen Coder AI ready.\n// Select a template or describe your Kotlin/Compose architecture above.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MidnightTextSecondary.copy(alpha = 0.5f)
                        )
                    } else {
                        Text(
                            text = codeOutput,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
