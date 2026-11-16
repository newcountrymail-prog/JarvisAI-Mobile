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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.terminal.RiskLevel
import com.jarvis.assistant.terminal.TerminalDashboardState
import com.jarvis.assistant.terminal.TerminalExecutionEngine
import com.jarvis.assistant.terminal.TerminalTaskState
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
fun TerminalDashboardScreen(
    engine: TerminalExecutionEngine,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by engine.dashboardState.collectAsState()
    val scope = rememberCoroutineScope()
    var inputQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val quickCommands = listOf(
        "Python environment check karo",
        "Storage aur memory check karo",
        "Installed packages ki list dikhao",
        "Git repository status check karo",
        "System diagnostics run karo"
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
                        text = "AUTONOMOUS TERMINAL",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        color = MidnightElectricBlue
                    )
                    Text(
                        text = "Termux Execution Engine",
                        style = MaterialTheme.typography.titleMedium,
                        color = MidnightTextSecondary
                    )
                }

                IconButton(onClick = { scope.launch { engine.refreshToolRegistry() } }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Tools",
                        tint = MidnightElectricBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STATUS BANNER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MidnightCardBackground)
                    .border(1.dp, MidnightBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (state.isTermuxInstalled) MidnightEmerald else Color(0xFFFF9100))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (state.isTermuxInstalled) "Termux System Bridge: Online" else "Termux Local Process Bridge: Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MidnightTextPrimary
                        )
                        Text(
                            text = if (state.hasRunCommandPermission) "Native RUN_COMMAND Permission Granted" else "Executing via Secure Sandboxed Local Shell",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MidnightTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // NATURAL LANGUAGE INPUT BAR
            OutlinedTextField(
                value = inputQuery,
                onValueChange = { inputQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Command in Hindi / English (e.g. Python install karo)",
                        color = MidnightTextSecondary.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = MidnightElectricBlue
                    )
                },
                trailingIcon = {
                    if (inputQuery.isNotBlank()) {
                        IconButton(onClick = {
                            val q = inputQuery
                            inputQuery = ""
                            scope.launch { engine.processNaturalLanguageInstruction(q) }
                        }) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Execute",
                                tint = MidnightElectricBlue
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    if (inputQuery.isNotBlank()) {
                        val q = inputQuery
                        inputQuery = ""
                        scope.launch { engine.processNaturalLanguageInstruction(q) }
                    }
                }),
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MidnightElectricBlue,
                    unfocusedBorderColor = MidnightBorder,
                    focusedContainerColor = MidnightCardBackground,
                    unfocusedContainerColor = MidnightCardBackground,
                    focusedTextColor = MidnightTextPrimary,
                    unfocusedTextColor = MidnightTextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // QUICK CHIPS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickCommands.forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(MidnightCardBackground)
                            .border(1.dp, MidnightBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .clickable {
                                scope.launch { engine.processNaturalLanguageInstruction(prompt) }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelSmall,
                            color = MidnightTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // TAB SELECTOR: Console vs Discovered Tools
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MidnightCardBackground,
                contentColor = MidnightElectricBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MidnightElectricBlue
                    )
                },
                modifier = Modifier.clip(RoundedCornerShape(16.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Execution Console", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Discovered Tools (${state.discoveredTools.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // TAB 0: CONSOLE
            if (selectedTab == 0) {
                // APPROVAL CARD IF NEEDED
                AnimatedVisibility(visible = state.taskState == TerminalTaskState.AWAITING_APPROVAL) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF2E1A1A))
                            .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF5252))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Command Approval Required (Risk Level 2)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF5252)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Action: ${state.activePlan?.userIntent ?: "Package management operation"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MidnightTextPrimary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(
                                    onClick = { scope.launch { engine.approvePendingPlan() } },
                                    colors = ButtonDefaults.buttonColors(containerColor = MidnightEmerald),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Approve & Execute", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { engine.rejectPendingPlan() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Reject")
                                }
                            }
                        }
                    }
                }

                // STATE INDICATOR
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.taskState == TerminalTaskState.EXECUTING || state.taskState == TerminalTaskState.PLANNING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = MidnightElectricBlue,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "State: ${state.taskState.name}",
                            style = MaterialTheme.typography.labelLarge,
                            color = when (state.taskState) {
                                TerminalTaskState.COMPLETED -> MidnightEmerald
                                TerminalTaskState.FAILED -> Color(0xFFFF5252)
                                TerminalTaskState.EXECUTING -> MidnightElectricBlue
                                else -> MidnightTextSecondary
                            }
                        )
                    }

                    IconButton(onClick = {
                        val logText = state.consoleLogs.joinToString("\n")
                        clipboardManager.setText(AnnotatedString(logText))
                    }) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Console Logs",
                            tint = MidnightTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // CONSOLE WINDOW
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF06090F))
                        .border(1.dp, MidnightBorder, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    val consoleScroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(consoleScroll)
                    ) {
                        if (state.consoleLogs.isEmpty()) {
                            Text(
                                text = "JARVIS X Terminal Engine ready.\nEnter natural-language command above to begin.",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MidnightTextSecondary.copy(alpha = 0.5f)
                            )
                        } else {
                            state.consoleLogs.forEach { logLine ->
                                val color = when {
                                    logLine.contains("ERROR", true) || logLine.contains("FAILED", true) -> Color(0xFFFF5252)
                                    logLine.contains("SUCCESS", true) || logLine.contains("verified", true) -> MidnightEmerald
                                    logLine.startsWith(">") || logLine.startsWith("[PLAN]") -> MidnightElectricBlue
                                    logLine.startsWith("[STDERR]") -> Color(0xFFFFB74D)
                                    else -> Color(0xFFCFD8DC)
                                }
                                Text(
                                    text = logLine,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = color,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // TAB 1: DISCOVERED TOOLS
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.discoveredTools.forEach { tool ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MidnightCardBackground)
                                .border(1.dp, MidnightBorder, RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = tool.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MidnightTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (tool.isAvailable) MidnightEmerald.copy(alpha = 0.2f) else Color(0x33FF5252))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (tool.isAvailable) "INSTALLED" else "MISSING",
                                                color = if (tool.isAvailable) MidnightEmerald else Color(0xFFFF5252),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (tool.isAvailable) "Version: ${tool.version ?: "Audited"} • ${tool.binaryPath ?: ""}" else "Required package: ${tool.pkgName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MidnightTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                if (!tool.isAvailable) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                engine.processNaturalLanguageInstruction("Install ${tool.name}")
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MidnightElectricBlue),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Install", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
