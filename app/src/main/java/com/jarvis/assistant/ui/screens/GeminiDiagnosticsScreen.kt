package com.jarvis.assistant.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.ai.GeminiAiProvider
import com.jarvis.assistant.ai.gemini.*
import com.jarvis.assistant.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiDiagnosticsScreen(
    engine: GeminiAutoConfigEngine,
    provider: GeminiAiProvider,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by engine.engineState.collectAsState()

    var showKeyDialog by remember { mutableStateOf(false) }
    var inputKey by remember { mutableStateOf("") }
    var isConfiguring by remember { mutableStateOf(false) }
    var progressMessage by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<ModelCapabilityCategory?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportContent by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Gemini AI Diagnostics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SunsetTextPrimary
                        )
                        Text(
                            text = "Universal Detection & Self-Repair Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = SunsetTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = SunsetTextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            progressMessage = "Refreshing state from persistent storage..."
                            engine.restoreSavedState()
                            progressMessage = ""
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SunsetPurple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SunsetBackground)
            )
        },
        containerColor = SunsetBackground,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            // ==========================================
            // 1. CONNECTION STATUS BANNER
            // ==========================================
            item {
                StatusCard(
                    state = state,
                    onEditKeyTapped = {
                        inputKey = state.apiKey
                        showKeyDialog = true
                    }
                )
            }

            // ==========================================
            // 2. LIVE ACTION PANEL
            // ==========================================
            item {
                ActionPanel(
                    isBusy = isConfiguring,
                    hasKey = state.apiKey.isNotBlank(),
                    onAutoConfigure = {
                        isConfiguring = true
                        progressMessage = "Starting Universal Auto Configuration..."
                        scope.launch {
                            val res = engine.autoConfigure(state.apiKey) { msg ->
                                progressMessage = msg
                            }
                            isConfiguring = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "Gemini Connected & Configured!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Auto-Config: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onDiscoverModels = {
                        isConfiguring = true
                        progressMessage = "Querying live model catalogue via Google API..."
                        scope.launch {
                            val res = provider.modelDiscovery.discoverLiveModels(state.apiKey)
                            isConfiguring = false
                            progressMessage = ""
                            if (res.isSuccess) {
                                val list = res.getOrThrow()
                                Toast.makeText(context, "Discovered ${list.size} text models.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Discovery Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onTestConnection = {
                        isConfiguring = true
                        progressMessage = "Sending real handshake probe to ${state.selectedModel}..."
                        scope.launch {
                            val res = provider.testConnection(state.selectedModel, state.apiKey)
                            isConfiguring = false
                            progressMessage = ""
                            res.fold(
                                onSuccess = {
                                    Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                                },
                                onFailure = {
                                    Toast.makeText(context, "Test Failed: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    onExportReport = {
                        reportContent = generateDiagnosticReport(state)
                        showReportDialog = true
                    },
                    onClearConfig = {
                        scope.launch {
                            provider.autoConfigEngine?.let {
                                context.getSystemService(Context.CLIPBOARD_SERVICE)
                            }
                            engine.autoConfigure("") // clear
                            Toast.makeText(context, "Gemini configuration cleared.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // ==========================================
            // 3. PROGRESS INDICATOR (If active)
            // ==========================================
            if (isConfiguring || progressMessage.isNotBlank()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SunsetPeach.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = SunsetCoral,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = progressMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = SunsetTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 4. ERROR DIAGNOSIS CARD (If error recorded)
            // ==========================================
            state.lastErrorDiagnosis?.let { diag ->
                item {
                    ErrorDiagnosisCard(
                        diagnosis = diag,
                        onAutoRepair = {
                            isConfiguring = true
                            progressMessage = "Triggering Autonomous Self-Repair Engine..."
                            scope.launch {
                                val repairRes = engine.autoRepairModel(state.apiKey, diag.modelId)
                                isConfiguring = false
                                progressMessage = ""
                                if (repairRes.isSuccess) {
                                    Toast.makeText(context, "Repaired! Switched to ${repairRes.getOrThrow()}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Self-Repair: ${repairRes.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }

            // ==========================================
            // 5. MODEL COMPATIBILITY TABLE & SELECTOR
            // ==========================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Discovered Models (${state.discoveredModels.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SunsetTextPrimary
                    )

                    val verifiedCount = state.discoveredModels.count { it.compatibility == GeminiModelCompatibility.VERIFIED_WORKING }
                    Text(
                        text = "$verifiedCount Verified",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (verifiedCount > 0) SunsetGreen else SunsetTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (state.discoveredModels.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SunsetCardBackground),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CloudQueue, null, tint = SunsetTextSecondary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No live models discovered yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SunsetTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'Auto Configure' or enter your API key to query Google's live model registry.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SunsetTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(state.discoveredModels) { model ->
                    ModelRowItem(
                        model = model,
                        isSelected = model.id == state.selectedModel,
                        onSelect = {
                            scope.launch {
                                engine.autoConfigure(state.apiKey)
                            }
                        },
                        onProbe = {
                            scope.launch {
                                progressMessage = "Probing ${model.displayName} live..."
                                val tested = provider.compatibilityTester.testSingleModel(model, state.apiKey)
                                progressMessage = ""
                                Toast.makeText(context, "${tested.displayName}: ${tested.compatibility.label}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }

    // ==========================================
    // API KEY INPUT DIALOG WITH PASTE & CLEAN
    // ==========================================
    if (showKeyDialog) {
        var keyInputText by remember { mutableStateOf(inputKey) }
        var showPassword by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = {
                Text(
                    text = "Configure Gemini API Key",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SunsetTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter your Google AI Studio API key. Whitespace, surrounding quotes, and delimiters will be cleaned automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SunsetTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = keyInputText,
                        onValueChange = { keyInputText = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle visibility",
                                    tint = SunsetTextSecondary
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val item = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                if (item.isNotBlank()) {
                                    keyInputText = item
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Paste from Clipboard")
                        }

                        val cleaned = GeminiKeyValidator.cleanKey(keyInputText)
                        val isValidFormat = GeminiKeyValidator.validateKeyFormat(cleaned)
                        Text(
                            text = if (isValidFormat) "✓ Format OK" else if (keyInputText.isBlank()) "" else "Format warning",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isValidFormat) SunsetGreen else SunsetCoral,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = GeminiKeyValidator.cleanKey(keyInputText)
                        showKeyDialog = false
                        isConfiguring = true
                        progressMessage = "Executing Universal Auto Configuration..."
                        scope.launch {
                            val res = engine.autoConfigure(cleaned) { msg ->
                                progressMessage = msg
                            }
                            isConfiguring = false
                            if (res.isSuccess) {
                                Toast.makeText(context, "Gemini Connected & Verified!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Configuration error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save & Auto-Configure", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) {
                    Text("Cancel", color = SunsetTextSecondary)
                }
            },
            containerColor = SunsetCardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // ==========================================
    // EXPORT REPORT DIALOG (Redacted)
    // ==========================================
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Text(
                    text = "Gemini Diagnostic Report",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SunsetTextPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = "All API keys and tokens have been automatically redacted.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SunsetTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .background(SunsetBackground, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = reportContent,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = SunsetTextPrimary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Gemini Diagnostic Report", reportContent)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_SHORT).show()
                        showReportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Copy Report", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Close", color = SunsetTextSecondary)
                }
            },
            containerColor = SunsetCardBackground,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun StatusCard(
    state: GeminiConfigState,
    onEditKeyTapped: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = when (state.status) {
            GeminiConnectionStatus.CONNECTED -> SunsetGreen
            GeminiConnectionStatus.UNVERIFIED -> Color(0xFFD97706) // Amber
            GeminiConnectionStatus.DISCONNECTED -> Color(0xFFDC2626) // Red
            GeminiConnectionStatus.TESTING -> SunsetBlue
        },
        label = "statusColor"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = SunsetCardBackground),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp), spotColor = SunsetPurple.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Status Header Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.status.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        letterSpacing = 0.5.sp
                    )
                }

                if (state.lastMeasuredLatencyMs > 0L) {
                    Text(
                        text = "${state.lastMeasuredLatencyMs} ms latency",
                        style = MaterialTheme.typography.labelSmall,
                        color = SunsetTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = SunsetBorder.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // Details Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "API Key", style = MaterialTheme.typography.labelSmall, color = SunsetTextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.maskedKey.ifBlank { "Not configured" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SunsetTextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = onEditKeyTapped,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Key", tint = SunsetCoral, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Active Model", style = MaterialTheme.typography.labelSmall, color = SunsetTextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.selectedModel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SunsetCoral
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "API Protocol", style = MaterialTheme.typography.labelSmall, color = SunsetTextSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "REST v1beta • SSE",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SunsetTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionPanel(
    isBusy: Boolean,
    hasKey: Boolean,
    onAutoConfigure: () -> Unit,
    onDiscoverModels: () -> Unit,
    onTestConnection: () -> Unit,
    onExportReport: () -> Unit,
    onClearConfig: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SunsetCardBackground),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Autonomous Actions",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SunsetTextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Primary ⚡ Auto Configure Button
            Button(
                onClick = onAutoConfigure,
                enabled = hasKey && !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Bolt, null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("⚡ Universal Auto Configure & Select", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDiscoverModels,
                    enabled = hasKey && !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Discover", fontSize = 12.sp, color = SunsetTextPrimary)
                }

                OutlinedButton(
                    onClick = onTestConnection,
                    enabled = hasKey && !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test Handshake", fontSize = 12.sp, color = SunsetTextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onExportReport) {
                    Icon(Icons.Default.Summarize, null, modifier = Modifier.size(16.dp), tint = SunsetPurple)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Diagnostic Report", fontSize = 12.sp, color = SunsetPurple)
                }

                TextButton(onClick = onClearConfig) {
                    Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(16.dp), tint = Color.Red.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Config", fontSize = 12.sp, color = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun ErrorDiagnosisCard(
    diagnosis: GeminiErrorDiagnosis,
    onAutoRepair: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, Color(0xFFF87171).copy(alpha = 0.6f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = diagnosis.errorCategory,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }

                diagnosis.httpStatusCode?.let { code ->
                    Surface(
                        color = Color(0xFFDC2626).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "HTTP $code",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = diagnosis.googleErrorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7F1D1D)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Correction suggestion
            Surface(
                color = Color.White.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.Lightbulb, null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = diagnosis.suggestedCorrection,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E)
                    )
                }
            }

            if (diagnosis.isRecoverable) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAutoRepair,
                    colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.AutoFixHigh, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-Repair Now")
                }
            }
        }
    }
}

@Composable
private fun ModelRowItem(
    model: GeminiDiscoveredModel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onProbe: () -> Unit
) {
    val compatColor = when (model.compatibility) {
        GeminiModelCompatibility.VERIFIED_WORKING -> SunsetGreen
        GeminiModelCompatibility.TESTING -> SunsetBlue
        GeminiModelCompatibility.QUOTA_EXCEEDED -> Color(0xFFD97706)
        GeminiModelCompatibility.RESTRICTED -> Color(0xFFDC2626)
        GeminiModelCompatibility.CONNECTION_FAILED -> Color(0xFFDC2626)
        else -> SunsetTextSecondary
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SunsetPeach.copy(alpha = 0.4f) else SunsetCardBackground
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 0.8.dp,
                color = if (isSelected) SunsetCoral else SunsetBorder.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                        color = SunsetTextPrimary
                    )
                    if (isSelected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.CheckCircle, null, tint = SunsetCoral, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "ID: ${model.id} • ${model.category.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SunsetTextSecondary,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(compatColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (model.latencyMs > 0L) "${model.compatibility.label} (${model.latencyMs}ms)" else model.compatibility.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = compatColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(onClick = onProbe) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Probe Model", tint = SunsetPurple)
            }
        }
    }
}

private fun generateDiagnosticReport(state: GeminiConfigState): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
    val dateStr = if (state.lastVerifiedTimestamp > 0) sdf.format(Date(state.lastVerifiedTimestamp)) else "Never"

    val sb = StringBuilder()
    sb.appendLine("=========================================")
    sb.appendLine("JARVIS X — GEMINI ENGINE DIAGNOSTIC REPORT")
    sb.appendLine("=========================================")
    sb.appendLine("Timestamp: ${sdf.format(Date())}")
    sb.appendLine("Status: ${state.status.label}")
    sb.appendLine("API Version: ${state.apiVersion}")
    sb.appendLine("Masked Key: ${state.maskedKey}")
    sb.appendLine("Selected Model: ${state.selectedModel}")
    sb.appendLine("Last Handshake: $dateStr")
    sb.appendLine("Measured Latency: ${state.lastMeasuredLatencyMs} ms")
    sb.appendLine("Discovered Models: ${state.discoveredModels.size}")
    sb.appendLine("Verified Working: ${state.discoveredModels.count { it.compatibility == GeminiModelCompatibility.VERIFIED_WORKING }}")
    sb.appendLine("-----------------------------------------")
    sb.appendLine("MODEL STATUS AUDIT TABLE:")
    for (m in state.discoveredModels) {
        val lat = if (m.latencyMs > 0) "${m.latencyMs}ms" else "N/A"
        sb.appendLine("• ${m.id} | ${m.category.label} | ${m.compatibility.name} | Latency: $lat")
    }
    state.lastErrorDiagnosis?.let { diag ->
        sb.appendLine("-----------------------------------------")
        sb.appendLine("LAST ERROR DIAGNOSIS:")
        sb.appendLine("Category: ${diag.errorCategory}")
        sb.appendLine("HTTP Status: ${diag.httpStatusCode ?: "N/A"}")
        sb.appendLine("Message: ${diag.googleErrorMessage}")
        sb.appendLine("Suggested Fix: ${diag.suggestedCorrection}")
    }
    sb.appendLine("=========================================")
    return sb.toString()
}
