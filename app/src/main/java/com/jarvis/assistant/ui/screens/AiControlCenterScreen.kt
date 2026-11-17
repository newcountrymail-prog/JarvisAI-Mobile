package com.jarvis.assistant.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.ai.orchestration.AgentType
import com.jarvis.assistant.ai.orchestration.DiscoveredModel
import com.jarvis.assistant.ai.orchestration.FreeTierCategory
import com.jarvis.assistant.ai.orchestration.ModelCapability
import com.jarvis.assistant.ai.orchestration.ProviderCredential
import com.jarvis.assistant.ai.orchestration.ProviderPreset
import com.jarvis.assistant.ai.orchestration.UniversalAiOrchestrator
import com.jarvis.assistant.ui.theme.CardBackground
import com.jarvis.assistant.ui.theme.CardBorder
import com.jarvis.assistant.ui.theme.PrimaryBackground
import com.jarvis.assistant.ui.theme.PrimaryText
import com.jarvis.assistant.ui.theme.SecondaryBackground
import com.jarvis.assistant.ui.theme.SecondaryText
import com.jarvis.assistant.ui.theme.SunsetCoral
import com.jarvis.assistant.ui.theme.SunsetGreen
import com.jarvis.assistant.ui.theme.SunsetPurple
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiControlCenterScreen(
    orchestrator: UniversalAiOrchestrator,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val state by orchestrator.orchestrationState.collectAsState()
    val providers by orchestrator.providers.collectAsState()
    val models by orchestrator.models.collectAsState()
    val usageLedger by orchestrator.quotaManager.usageLedger.collectAsState()
    val rotationEvents by orchestrator.rotationEngine.rotationEvents.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddProviderDialog by remember { mutableStateOf(false) }
    var isRefreshingAll by remember { mutableStateOf(false) }

    val tabs = listOf(
        "Providers",
        "Model Library",
        "Free AI Library",
        "Usage Center",
        "Auto Routing",
        "Specialized Agents",
        "Diagnostics",
        "Security & Settings"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI CONTROL CENTER",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = PrimaryText
                        )
                        Text(
                            text = "Universal AI Intelligence & Autonomous Routing",
                            fontSize = 11.sp,
                            color = SecondaryText
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = PrimaryText)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isRefreshingAll = true
                                orchestrator.discoverAllModels()
                                isRefreshingAll = false
                            }
                        }
                    ) {
                        if (isRefreshingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = SunsetCoral
                            )
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Refresh All", tint = SunsetCoral)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBackground)
            )
        },
        containerColor = PrimaryBackground
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // KPI Summary Dashboard
            ControlCenterDashboardHeader(
                state = state,
                onAddProviderClick = { showAddProviderDialog = true }
            )

            // Section Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = SecondaryBackground,
                contentColor = SunsetCoral,
                edgePadding = 12.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) SunsetCoral else SecondaryText
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                when (selectedTabIndex) {
                    0 -> ProviderManagerTab(
                        providers = providers,
                        onAddProvider = { showAddProviderDialog = true },
                        onToggle = { id, enabled -> scope.launch { orchestrator.toggleProvider(id, enabled) } },
                        onDelete = { id -> scope.launch { orchestrator.deleteProvider(id) } },
                        onTest = { cred -> scope.launch { orchestrator.testAndDiscover(cred) } }
                    )
                    1 -> ModelLibraryTab(models = models)
                    2 -> FreeAiLibraryTab(
                        models = models,
                        freeModeOnly = state.isFreeModeOnly,
                        onToggleFreeModeOnly = { enabled -> scope.launch { orchestrator.setFreeModeOnly(enabled) } }
                    )
                    3 -> UsageCenterTab(
                        usageLedger = usageLedger,
                        totalRequests = state.totalRequestsCount,
                        totalTokens = state.totalTokensConsumed,
                        quotaManager = orchestrator.quotaManager
                    )
                    4 -> AutoRoutingTab(
                        state = state,
                        rotationEvents = rotationEvents,
                        onToggleAutoSwitch = { enabled -> scope.launch { orchestrator.setAutoSwitching(enabled) } }
                    )
                    5 -> SpecializedAgentsTab(
                        multiAgentSystem = orchestrator.multiAgentSystem,
                        multiAgentReviewEnabled = state.isMultiAgentReview,
                        onToggleReview = { enabled -> scope.launch { orchestrator.setMultiAgentReview(enabled) } }
                    )
                    6 -> DiagnosticsTab(
                        state = state,
                        providers = providers,
                        onDiscoverAll = { scope.launch { orchestrator.discoverAllModels() } }
                    )
                    7 -> SecuritySettingsTab(
                        state = state,
                        onToggleFreeModeOnly = { scope.launch { orchestrator.setFreeModeOnly(it) } },
                        onToggleAutoSwitch = { scope.launch { orchestrator.setAutoSwitching(it) } },
                        onToggleAutoModel = { scope.launch { orchestrator.setAutoModelSelection(it) } },
                        onToggleMultiAgentReview = { scope.launch { orchestrator.setMultiAgentReview(it) } },
                        onUpdateMaxRetries = { scope.launch { orchestrator.setMaxRetryCount(it) } }
                    )
                }
            }
        }
    }

    if (showAddProviderDialog) {
        AddProviderDialog(
            discoveryEngine = orchestrator.discoveryEngine,
            onDismiss = { showAddProviderDialog = false },
            onConfirm = { preset, name, key, url ->
                scope.launch {
                    orchestrator.addOrUpdateProvider(
                        preset = preset,
                        name = name,
                        apiKey = key,
                        baseUrl = url
                    )
                    showAddProviderDialog = false
                }
            }
        )
    }
}

// =========================================================================
// SECTION: KPI HEADER
// =========================================================================

@Composable
fun ControlCenterDashboardHeader(
    state: com.jarvis.assistant.ai.orchestration.OrchestrationState,
    onAddProviderClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .background(if (state.connectedProvidersCount > 0) SunsetGreen else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.activeRoutingStatus,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.connectedProvidersCount > 0) SunsetGreen else SecondaryText
                    )
                }

                if (state.isFreeModeOnly) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SunsetGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "FREE MODE ONLY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SunsetGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KpiMetricItem(label = "Providers", value = "${state.connectedProvidersCount}", color = SunsetCoral)
                KpiMetricItem(label = "Models", value = "${state.availableModelsCount}", color = SunsetPurple)
                KpiMetricItem(label = "Free AI", value = "${state.verifiedFreeModelsCount}", color = SunsetGreen)
                KpiMetricItem(label = "Requests", value = "${state.totalRequestsCount}", color = PrimaryText)
            }
        }
    }
}

@Composable
fun KpiMetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 11.sp, color = SecondaryText)
    }
}

// =========================================================================
// SECTION A: PROVIDER MANAGER
// =========================================================================

@Composable
fun ProviderManagerTab(
    providers: List<ProviderCredential>,
    onAddProvider: () -> Unit,
    onToggle: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
    onTest: (ProviderCredential) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Connected Providers (${providers.size})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText
                )
                Button(
                    onClick = onAddProvider,
                    colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Add Provider", fontSize = 12.sp)
                }
            }
        }

        items(providers, key = { it.id }) { provider ->
            ProviderCredentialCard(
                credential = provider,
                onToggle = { onToggle(provider.id, it) },
                onDelete = { onDelete(provider.id) },
                onTest = { onTest(provider) }
            )
        }
    }
}

@Composable
fun ProviderCredentialCard(
    credential: ProviderCredential,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .background(if (credential.isHealthy) SunsetGreen else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = credential.displayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText
                    )
                }

                Switch(
                    checked = credential.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = SunsetCoral, checkedTrackColor = SunsetCoral.copy(alpha = 0.3f))
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Preset: ${credential.providerPreset} • Auth: ${credential.authType}",
                fontSize = 11.sp,
                color = SecondaryText
            )

            Text(
                text = "Key: ${credential.maskedKey}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = PrimaryText
            )

            Text(
                text = "Endpoint: ${credential.baseUrl}",
                fontSize = 11.sp,
                color = SecondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (credential.isHealthy) "Latency: ${credential.lastLatencyMs}ms • ${credential.discoveredModelsCount} models"
                    else credential.lastErrorMessage ?: "Untested / Offline",
                    fontSize = 11.sp,
                    color = if (credential.isHealthy) SunsetGreen else Color(0xFFE53935)
                )

                Row {
                    OutlinedButton(
                        onClick = onTest,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(text = "Test & Discover", fontSize = 11.sp, color = SunsetCoral)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// =========================================================================
// SECTION B: MODEL LIBRARY
// =========================================================================

@Composable
fun ModelLibraryTab(models: List<DiscoveredModel>) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredModels = models.filter {
        it.displayName.contains(searchQuery, ignoreCase = true) ||
                it.modelIdentifier.contains(searchQuery, ignoreCase = true)
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Discovered Models (${models.size})") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            )
        }

        items(filteredModels, key = { it.id }) { model ->
            DiscoveredModelCard(model = model)
        }
    }
}

@Composable
fun DiscoveredModelCard(model: DiscoveredModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.displayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryText,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (model.freeCategory.contains("FREE")) SunsetGreen.copy(alpha = 0.15f) else SunsetPurple.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (model.freeCategory.contains("FREE")) "FREE" else "PAID",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (model.freeCategory.contains("FREE")) SunsetGreen else SunsetPurple
                    )
                }
            }

            Text(
                text = model.modelIdentifier,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = SecondaryText
            )

            if (model.description.isNotBlank()) {
                Text(
                    text = model.description,
                    fontSize = 11.sp,
                    color = PrimaryText.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Context: ${model.contextCapacity / 1024}k tokens",
                    fontSize = 10.sp,
                    color = SecondaryText
                )
                Text(
                    text = "Coding: ${model.codingSuitabilityScore}/100 • Logic: ${model.reasoningSuitabilityScore}/100",
                    fontSize = 10.sp,
                    color = SecondaryText
                )
            }
        }
    }
}

// =========================================================================
// SECTION C: FREE AI LIBRARY
// =========================================================================

@Composable
fun FreeAiLibraryTab(
    models: List<DiscoveredModel>,
    freeModeOnly: Boolean,
    onToggleFreeModeOnly: (Boolean) -> Unit
) {
    val freeModels = models.filter {
        it.freeCategory == FreeTierCategory.CATEGORY_A_VERIFIED_FREE.name ||
                it.freeCategory == FreeTierCategory.CATEGORY_B_FREE_TIER_QUOTA.name
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SunsetGreen.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SunsetGreen.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "FREE MODE ONLY",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SunsetGreen
                        )
                        Text(
                            text = "Guarantees zero charges. Paid and unknown pricing models are strictly excluded.",
                            fontSize = 11.sp,
                            color = PrimaryText.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = freeModeOnly,
                        onCheckedChange = onToggleFreeModeOnly,
                        colors = SwitchDefaults.colors(checkedThumbColor = SunsetGreen)
                    )
                }
            }
        }

        item {
            Text(
                text = "Verified Free & Free-Tier Models (${freeModels.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryText
            )
        }

        items(freeModels, key = { it.id }) { model ->
            DiscoveredModelCard(model = model)
        }
    }
}

// =========================================================================
// SECTION D: USAGE CENTER
// =========================================================================

@Composable
fun UsageCenterTab(
    usageLedger: List<com.jarvis.assistant.ai.orchestration.UsageLedgerEntry>,
    totalRequests: Long,
    totalTokens: Long,
    quotaManager: com.jarvis.assistant.ai.orchestration.UsageQuotaManager
) {
    val quotaStates by quotaManager.providerQuotaStates.collectAsState()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Total Usage Telemetry", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Total Requests", fontSize = 11.sp, color = SecondaryText)
                            Text(text = "$totalRequests", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SunsetCoral)
                        }
                        Column {
                            Text(text = "Total Tokens", fontSize = 11.sp, color = SecondaryText)
                            Text(text = "$totalTokens", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SunsetPurple)
                        }
                        Column {
                            Text(text = "Estimated Cost", fontSize = 11.sp, color = SecondaryText)
                            Text(text = "$0.00 USD", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SunsetGreen)
                        }
                    }
                }
            }
        }

        if (quotaStates.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "Provider Rate-Limit Status", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryText)
                        Spacer(modifier = Modifier.height(6.dp))
                        quotaStates.forEach { (provider, quota) ->
                            Text(text = "$provider: $quota", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = SecondaryText)
                        }
                    }
                }
            }
        }

        item {
            Text(text = "Local Usage Ledger (${usageLedger.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryText)
        }

        items(usageLedger.reversed(), key = { it.id }) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = entry.modelId, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
                        Text(text = "Tokens: ${entry.totalTokens} (${entry.promptTokens} in / ${entry.completionTokens} out)", fontSize = 10.sp, color = SecondaryText)
                    }
                    Text(text = "${entry.latencyMs}ms", fontSize = 11.sp, color = SunsetCoral, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =========================================================================
// SECTION E: AUTO ROUTING
// =========================================================================

@Composable
fun AutoRoutingTab(
    state: com.jarvis.assistant.ai.orchestration.OrchestrationState,
    rotationEvents: List<com.jarvis.assistant.ai.orchestration.RotationEvent>,
    onToggleAutoSwitch: (Boolean) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Autonomous AI Rotation", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
                        Text(
                            text = "Automatically switches to backup AI when primary hits rate limits or service failure.",
                            fontSize = 11.sp,
                            color = SecondaryText
                        )
                    }
                    Switch(
                        checked = state.isAutoProviderSwitching,
                        onCheckedChange = onToggleAutoSwitch,
                        colors = SwitchDefaults.colors(checkedThumbColor = SunsetCoral)
                    )
                }
            }
        }

        item {
            Text(text = "Failover & Rotation History (${rotationEvents.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryText)
        }

        if (rotationEvents.isEmpty()) {
            item {
                Text(
                    text = "No rotation events yet. System is operating normally on primary provider.",
                    fontSize = 12.sp,
                    color = SecondaryText,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(rotationEvents.reversed(), key = { it.id }) { event ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardBackground),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Rotated: ${event.fromModelId} ➔ ${event.toModelId}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SunsetCoral
                        )
                        Text(text = "Reason: ${event.reason}", fontSize = 11.sp, color = SecondaryText)
                    }
                }
            }
        }
    }
}

// =========================================================================
// SECTION F: SPECIALIZED AGENTS
// =========================================================================

@Composable
fun SpecializedAgentsTab(
    multiAgentSystem: com.jarvis.assistant.ai.orchestration.AutonomousMultiAgentSystem,
    multiAgentReviewEnabled: Boolean,
    onToggleReview: (Boolean) -> Unit
) {
    val agents = AgentType.values().toList()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SunsetPurple.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SunsetPurple.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Multi-Agent Review Pipeline",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SunsetPurple
                        )
                        Text(
                            text = "CODER produces implementation ➔ REVIEWER independently audits edge-cases & syntax.",
                            fontSize = 11.sp,
                            color = PrimaryText.copy(alpha = 0.8f)
                        )
                    }
                    Switch(
                        checked = multiAgentReviewEnabled,
                        onCheckedChange = onToggleReview,
                        colors = SwitchDefaults.colors(checkedThumbColor = SunsetPurple)
                    )
                }
            }
        }

        item {
            Text(text = "Active Specialized Agents", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryText)
        }

        items(agents) { agent ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = agent.displayName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SunsetCoral)
                    Text(text = agent.roleDescription, fontSize = 11.sp, color = SecondaryText)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Prompt: ${multiAgentSystem.getAgentSystemPrompt(agent)}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = PrimaryText.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// =========================================================================
// SECTION G: DIAGNOSTICS
// =========================================================================

@Composable
fun DiagnosticsTab(
    state: com.jarvis.assistant.ai.orchestration.OrchestrationState,
    providers: List<ProviderCredential>,
    onDiscoverAll: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Provider Connectivity Health", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryText)
                    Spacer(modifier = Modifier.height(8.dp))
                    providers.forEach { provider ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = provider.displayName, fontSize = 12.sp, color = PrimaryText)
                            Text(
                                text = if (provider.isHealthy) "ONLINE (${provider.lastLatencyMs}ms)" else "OFFLINE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (provider.isHealthy) SunsetGreen else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onDiscoverAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Run Full System Diagnostics & Model Discovery")
            }
        }
    }
}

// =========================================================================
// SECTION H: SECURITY & SETTINGS
// =========================================================================

@Composable
fun SecuritySettingsTab(
    state: com.jarvis.assistant.ai.orchestration.OrchestrationState,
    onToggleFreeModeOnly: (Boolean) -> Unit,
    onToggleAutoSwitch: (Boolean) -> Unit,
    onToggleAutoModel: (Boolean) -> Unit,
    onToggleMultiAgentReview: (Boolean) -> Unit,
    onUpdateMaxRetries: (Int) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = SunsetGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Credential Protection & Isolation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryText)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "• API keys are stored in encrypted local DataStore.", fontSize = 11.sp, color = SecondaryText)
                    Text(text = "• Keys are masked in all UI presentations and never shown raw.", fontSize = 11.sp, color = SecondaryText)
                    Text(text = "• Sensitive credentials are automatically redacted from error logs.", fontSize = 11.sp, color = SecondaryText)
                    Text(text = "• Provider isolation ensures credentials are never shared cross-endpoint.", fontSize = 11.sp, color = SecondaryText)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Orchestration Control Policies", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryText)
                    Spacer(modifier = Modifier.height(8.dp))

                    SettingSwitchRow("FREE MODE ONLY", "Strict zero-cost enforcement", state.isFreeModeOnly, onToggleFreeModeOnly)
                    Divider(color = CardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                    SettingSwitchRow("AUTO PROVIDER SWITCHING", "Rotate to backup on limits or outages", state.isAutoProviderSwitching, onToggleAutoSwitch)
                    Divider(color = CardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                    SettingSwitchRow("AUTO MODEL SELECTION", "Pick optimal model based on task", state.isAutoModelSelection, onToggleAutoModel)
                    Divider(color = CardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))

                    SettingSwitchRow("MULTI-AGENT REVIEW", "Coder ➔ Reviewer verification pipeline", state.isMultiAgentReview, onToggleMultiAgentReview)
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PrimaryText)
            Text(text = subtitle, fontSize = 10.sp, color = SecondaryText)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = SunsetCoral)
        )
    }
}

// =========================================================================
// SECTION: ADD PROVIDER DIALOG
// =========================================================================

@Composable
fun AddProviderDialog(
    discoveryEngine: com.jarvis.assistant.ai.orchestration.ProviderDiscoveryEngine,
    onDismiss: () -> Unit,
    onConfirm: (ProviderPreset, String, String, String) -> Unit
) {
    var selectedPreset by remember { mutableStateOf(ProviderPreset.OPENROUTER) }
    var providerName by remember { mutableStateOf(selectedPreset.displayName) }
    var apiKey by remember { mutableStateOf("") }
    var endpointUrl by remember { mutableStateOf(selectedPreset.defaultEndpoint) }
    var presetMenuExpanded by remember { mutableStateOf(false) }

    val presets = ProviderPreset.values().toList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Add AI Provider Credential", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Preset Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { presetMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Preset: ${selectedPreset.displayName}")
                    }
                    DropdownMenu(
                        expanded = presetMenuExpanded,
                        onDismissRequest = { presetMenuExpanded = false }
                    ) {
                        presets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(preset.displayName) },
                                onClick = {
                                    selectedPreset = preset
                                    providerName = preset.displayName
                                    endpointUrl = preset.defaultEndpoint
                                    presetMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = providerName,
                    onValueChange = { providerName = it },
                    label = { Text("Provider Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it
                        // Auto-detect preset if key pattern is recognized
                        val detected = discoveryEngine.detectProvider(it, endpointUrl)
                        if (detected != ProviderPreset.CUSTOM) {
                            selectedPreset = detected
                            providerName = detected.displayName
                            endpointUrl = detected.defaultEndpoint
                        }
                    },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = { endpointUrl = it },
                    label = { Text("Base URL Endpoint") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedPreset, providerName, apiKey, endpointUrl) },
                colors = ButtonDefaults.buttonColors(containerColor = SunsetCoral)
            ) {
                Text("Save & Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SecondaryText)
            }
        }
    )
}
