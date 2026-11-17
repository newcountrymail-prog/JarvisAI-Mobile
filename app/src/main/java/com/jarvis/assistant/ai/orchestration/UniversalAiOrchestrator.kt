package com.jarvis.assistant.ai.orchestration

import com.jarvis.assistant.data.model.ChatMessage
import com.jarvis.assistant.data.repository.JarvisPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Universal AI Orchestrator for JARVIS X & AntiGem.
 * Central intelligence coordinating multi-provider credentials, automatic discovery,
 * zero-cost free model enforcement, multi-agent pipelines, and usage tracking.
 */
class UniversalAiOrchestrator(
    private val preferences: JarvisPreferences,
    val discoveryEngine: ProviderDiscoveryEngine = ProviderDiscoveryEngine(),
    val freeModelDetector: FreeModelDetectionEngine = FreeModelDetectionEngine(),
    val quotaManager: UsageQuotaManager = UsageQuotaManager(),
    val taskClassifier: TaskIntelligenceClassifier = TaskIntelligenceClassifier(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val rotationEngine: AutonomousRotationEngine = AutonomousRotationEngine(
        freeModelDetector = freeModelDetector,
        taskClassifier = taskClassifier,
        quotaManager = quotaManager
    )

    val multiAgentSystem: AutonomousMultiAgentSystem = AutonomousMultiAgentSystem(rotationEngine)

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    private val _providers = MutableStateFlow<List<ProviderCredential>>(emptyList())
    val providers: StateFlow<List<ProviderCredential>> = _providers.asStateFlow()

    private val _models = MutableStateFlow<List<DiscoveredModel>>(emptyList())
    val models: StateFlow<List<DiscoveredModel>> = _models.asStateFlow()

    private val _orchestrationState = MutableStateFlow(OrchestrationState())
    val orchestrationState: StateFlow<OrchestrationState> = _orchestrationState.asStateFlow()

    init {
        scope.launch {
            loadPersistedState()
        }
    }

    private suspend fun loadPersistedState() = withContext(Dispatchers.IO) {
        val providersJson = preferences.orchestrationProvidersJsonFlow.first()
        val loadedProviders = if (providersJson.isNotBlank()) {
            try {
                json.decodeFromString<List<ProviderCredential>>(providersJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        val modelsJson = preferences.orchestrationModelsJsonFlow.first()
        val loadedModels = if (modelsJson.isNotBlank()) {
            try {
                json.decodeFromString<List<DiscoveredModel>>(modelsJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()

        if (loadedProviders.isNotEmpty()) {
            _providers.value = loadedProviders
        } else {
            // Seed initial presets using any previously saved keys in preferences
            seedInitialPresets()
        }

        if (loadedModels.isNotEmpty()) {
            _models.value = loadedModels
        } else {
            // Seed default curated models
            seedCuratedDefaultModels()
        }

        updateOrchestrationState()
    }

    private suspend fun seedInitialPresets() {
        val geminiKey = preferences.geminiApiKeyFlow.first()
        val openRouterKey = preferences.openRouterApiKeyFlow.first()
        val qwenKey = preferences.qwenApiKeyFlow.first()
        val customKey = preferences.customApiKeyFlow.first()
        val customUrl = preferences.customBaseUrlFlow.first()

        val initial = listOf(
            ProviderCredential(
                id = "gemini",
                providerPreset = ProviderPreset.GEMINI.name,
                displayName = "Google Gemini",
                apiKey = geminiKey,
                baseUrl = ProviderPreset.GEMINI.defaultEndpoint,
                authType = AuthHeaderType.QUERY_OR_GOOG_HEADER.name,
                isEnabled = true,
                maskedKey = discoveryEngine.maskKey(geminiKey)
            ),
            ProviderCredential(
                id = "openrouter",
                providerPreset = ProviderPreset.OPENROUTER.name,
                displayName = "OpenRouter",
                apiKey = openRouterKey,
                baseUrl = ProviderPreset.OPENROUTER.defaultEndpoint,
                authType = AuthHeaderType.BEARER.name,
                isEnabled = true,
                maskedKey = discoveryEngine.maskKey(openRouterKey)
            ),
            ProviderCredential(
                id = "qwen",
                providerPreset = ProviderPreset.QWEN.name,
                displayName = "Qwen Coder AI",
                apiKey = qwenKey,
                baseUrl = ProviderPreset.QWEN.defaultEndpoint,
                authType = AuthHeaderType.BEARER.name,
                isEnabled = true,
                maskedKey = discoveryEngine.maskKey(qwenKey)
            ),
            ProviderCredential(
                id = "ollama",
                providerPreset = ProviderPreset.OLLAMA.name,
                displayName = "Ollama Local",
                apiKey = "",
                baseUrl = ProviderPreset.OLLAMA.defaultEndpoint,
                authType = AuthHeaderType.NONE.name,
                isEnabled = false,
                maskedKey = "No Key Required"
            ),
            ProviderCredential(
                id = "custom",
                providerPreset = ProviderPreset.CUSTOM.name,
                displayName = "Custom OpenAI",
                apiKey = customKey,
                baseUrl = customUrl.ifBlank { "https://api.openai.com/v1" },
                authType = AuthHeaderType.BEARER.name,
                isEnabled = true,
                maskedKey = discoveryEngine.maskKey(customKey)
            )
        )
        _providers.value = initial
        saveProviders(initial)
    }

    private fun seedCuratedDefaultModels() {
        val defaultModels = listOf(
            DiscoveredModel(
                id = "gemini-1.5-flash",
                providerId = "gemini",
                modelIdentifier = "gemini-1.5-flash",
                displayName = "Gemini 1.5 Flash (Official Free Tier)",
                description = "High-speed multimodal, free tier (15 RPM), 1M token context",
                contextCapacity = 1048576,
                freeCategory = FreeTierCategory.CATEGORY_B_FREE_TIER_QUOTA.name,
                capabilities = listOf(
                    ModelCapability.GENERAL.name,
                    ModelCapability.VISION.name,
                    ModelCapability.CODING.name,
                    ModelCapability.LONG_CONTEXT.name,
                    ModelCapability.STREAMING.name,
                    ModelCapability.FREE.name
                ),
                codingSuitabilityScore = 85,
                reasoningSuitabilityScore = 85,
                pricingPrompt = "Free (15 RPM)",
                pricingCompletion = "Free (1M TPM)",
                isAvailable = true
            ),
            DiscoveredModel(
                id = "openrouter-meta-llama_llama-3.3-70b-instruct_free",
                providerId = "openrouter",
                modelIdentifier = "meta-llama/llama-3.3-70b-instruct:free",
                displayName = "Llama 3.3 70B Instruct (Free)",
                description = "Benchmark open weights model with zero cost, 128k context",
                contextCapacity = 131072,
                freeCategory = FreeTierCategory.CATEGORY_A_VERIFIED_FREE.name,
                capabilities = listOf(
                    ModelCapability.GENERAL.name,
                    ModelCapability.REASONING.name,
                    ModelCapability.CODING.name,
                    ModelCapability.LONG_CONTEXT.name,
                    ModelCapability.FREE.name
                ),
                codingSuitabilityScore = 85,
                reasoningSuitabilityScore = 90,
                pricingPrompt = "0.00",
                pricingCompletion = "0.00",
                isAvailable = true
            ),
            DiscoveredModel(
                id = "openrouter-deepseek_deepseek-r1_free",
                providerId = "openrouter",
                modelIdentifier = "deepseek/deepseek-r1:free",
                displayName = "DeepSeek R1 Reasoning (Free)",
                description = "State-of-the-art chain-of-thought open reasoning architecture",
                contextCapacity = 65536,
                freeCategory = FreeTierCategory.CATEGORY_A_VERIFIED_FREE.name,
                capabilities = listOf(
                    ModelCapability.REASONING.name,
                    ModelCapability.CODING.name,
                    ModelCapability.FREE.name
                ),
                codingSuitabilityScore = 92,
                reasoningSuitabilityScore = 98,
                pricingPrompt = "0.00",
                pricingCompletion = "0.00",
                isAvailable = true
            ),
            DiscoveredModel(
                id = "qwen-2.5-coder-32b-instruct",
                providerId = "qwen",
                modelIdentifier = "qwen/qwen-2.5-coder-32b-instruct",
                displayName = "Qwen 2.5 Coder 32B",
                description = "Specialized Kotlin, Android, and software engineering architecture",
                contextCapacity = 32768,
                freeCategory = FreeTierCategory.CATEGORY_D_PAID.name,
                capabilities = listOf(
                    ModelCapability.CODING.name,
                    ModelCapability.STREAMING.name
                ),
                codingSuitabilityScore = 96,
                reasoningSuitabilityScore = 88,
                pricingPrompt = "$0.0002/1k",
                pricingCompletion = "$0.0002/1k",
                isAvailable = true
            )
        )
        _models.value = defaultModels
        saveModels(defaultModels)
    }

    /**
     * Adds or updates a provider credential.
     */
    suspend fun addOrUpdateProvider(
        id: String? = null,
        preset: ProviderPreset,
        name: String,
        apiKey: String,
        baseUrl: String
    ) = withContext(Dispatchers.IO) {
        val cleanKey = discoveryEngine.sanitizeKey(apiKey)
        val cleanUrl = baseUrl.ifBlank { preset.defaultEndpoint }
        val providerId = id ?: UUID.randomUUID().toString().take(8)

        val cred = ProviderCredential(
            id = providerId,
            providerPreset = preset.name,
            displayName = name.ifBlank { preset.displayName },
            apiKey = cleanKey,
            baseUrl = cleanUrl,
            authType = preset.authHeaderType.name,
            isEnabled = true,
            maskedKey = discoveryEngine.maskKey(cleanKey),
            lastTestedTimestamp = 0L,
            isHealthy = false
        )

        val updated = _providers.value.filter { it.id != cred.id } + cred
        _providers.value = updated
        saveProviders(updated)

        // Automatically test connectivity and discover models
        testAndDiscover(cred)
    }

    /**
     * Removes a provider credential.
     */
    suspend fun deleteProvider(id: String) = withContext(Dispatchers.IO) {
        val updated = _providers.value.filter { it.id != id }
        _providers.value = updated
        saveProviders(updated)

        val updatedModels = _models.value.filter { it.providerId != id }
        _models.value = updatedModels
        saveModels(updatedModels)

        updateOrchestrationState()
    }

    /**
     * Toggles an existing provider on or off.
     */
    suspend fun toggleProvider(id: String, isEnabled: Boolean) = withContext(Dispatchers.IO) {
        val updated = _providers.value.map {
            if (it.id == id) it.copy(isEnabled = isEnabled) else it
        }
        _providers.value = updated
        saveProviders(updated)
        updateOrchestrationState()
    }

    /**
     * Runs connectivity test and model discovery for a single provider.
     */
    suspend fun testAndDiscover(credential: ProviderCredential) = withContext(Dispatchers.IO) {
        val (isHealthy, latency) = discoveryEngine.testConnectivity(credential)
        val discoveryResult = if (isHealthy) {
            discoveryEngine.discoverModels(credential)
        } else {
            Result.failure(Exception("Connection test failed"))
        }

        val discoveredCount = discoveryResult.getOrNull()?.size ?: 0
        val updatedCred = credential.copy(
            isHealthy = isHealthy,
            lastTestedTimestamp = System.currentTimeMillis(),
            lastLatencyMs = latency,
            lastErrorMessage = if (!isHealthy) "Handshake failed or endpoint unreachable" else null,
            discoveredModelsCount = discoveredCount
        )

        val updatedProviders = _providers.value.map {
            if (it.id == credential.id) updatedCred else it
        }
        _providers.value = updatedProviders
        saveProviders(updatedProviders)

        if (discoveryResult.isSuccess) {
            val newModels = discoveryResult.getOrThrow()
            val filteredOld = _models.value.filter { it.providerId != credential.id }
            val mergedModels = filteredOld + newModels
            _models.value = mergedModels
            saveModels(mergedModels)
        }

        updateOrchestrationState()
    }

    /**
     * Discovers models across all currently enabled providers.
     */
    suspend fun discoverAllModels() = withContext(Dispatchers.IO) {
        for (provider in _providers.value) {
            if (provider.isEnabled && provider.apiKey.isNotBlank()) {
                testAndDiscover(provider)
            }
        }
    }

    /**
     * Executes a chat prompt through the orchestration engine,
     * applying task classification, multi-agent pipelines, and autonomous rotation.
     */
    suspend fun executeChat(
        prompt: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        systemInstruction: String = ""
    ): OrchestrationExecutionResult = withContext(Dispatchers.IO) {
        val state = _orchestrationState.value
        val taskCat = taskClassifier.classifyTask(prompt)

        // Route to specialized Multi-Agent Coder-Reviewer pipeline if requested
        val isCodingTask = taskCat == TaskCategory.PROGRAMMING ||
                taskCat == TaskCategory.ANDROID_DEVELOPMENT ||
                taskCat == TaskCategory.CODE_DEBUGGING

        if (state.isMultiAgentReview && isCodingTask) {
            return@withContext multiAgentSystem.executeCoderReviewerPipeline(
                prompt = prompt,
                conversationHistory = conversationHistory,
                providers = _providers.value,
                discoveredModels = _models.value,
                freeModeOnly = state.isFreeModeOnly,
                autoSwitchingEnabled = state.isAutoProviderSwitching
            )
        }

        // Standard Autonomous Rotation
        rotationEngine.executeWithAutoRotation(
            prompt = prompt,
            conversationHistory = conversationHistory,
            systemInstruction = systemInstruction,
            providers = _providers.value,
            discoveredModels = _models.value,
            primaryModelId = state.activeModelId,
            freeModeOnly = state.isFreeModeOnly,
            autoSwitchingEnabled = state.isAutoProviderSwitching,
            maxRetries = state.maxRetryCount
        )
    }

    private fun saveProviders(list: List<ProviderCredential>) {
        scope.launch {
            val jsonStr = json.encodeToString(list)
            preferences.setOrchestrationProvidersJson(jsonStr)
        }
    }

    private fun saveModels(list: List<DiscoveredModel>) {
        scope.launch {
            val jsonStr = json.encodeToString(list)
            preferences.setOrchestrationModelsJson(jsonStr)
        }
    }

    private fun updateOrchestrationState() {
        val providers = _providers.value
        val models = _models.value
        val freeModels = freeModelDetector.filterEligibleModels(models, freeModeOnly = true)

        val connected = providers.count { it.isHealthy && it.isEnabled }
        val available = models.count { it.isAvailable }
        val verifiedFree = freeModels.size

        val primary = providers.find { it.isHealthy && it.isEnabled }?.id ?: "gemini"
        val activeModel = models.firstOrNull { it.isAvailable }?.displayName ?: "gemini-1.5-flash"

        _orchestrationState.value = _orchestrationState.value.copy(
            connectedProvidersCount = connected,
            availableModelsCount = available,
            verifiedFreeModelsCount = verifiedFree,
            primaryProviderId = primary,
            activeModelId = activeModel,
            activeRoutingStatus = if (connected > 0) "READY • $connected PROVIDERS CONNECTED" else "NO PROVIDERS CONNECTED",
            totalRequestsCount = quotaManager.totalRequests.value,
            totalTokensConsumed = quotaManager.totalTokens.value
        )
    }

    // --- User Setting Toggles ---

    suspend fun setFreeModeOnly(enabled: Boolean) {
        preferences.setFreeModeOnly(enabled)
        _orchestrationState.value = _orchestrationState.value.copy(isFreeModeOnly = enabled)
    }

    suspend fun setAutoSwitching(enabled: Boolean) {
        preferences.setAutoSwitching(enabled)
        _orchestrationState.value = _orchestrationState.value.copy(isAutoProviderSwitching = enabled)
    }

    suspend fun setAutoModelSelection(enabled: Boolean) {
        preferences.setAutoModelSelection(enabled)
        _orchestrationState.value = _orchestrationState.value.copy(isAutoModelSelection = enabled)
    }

    suspend fun setMultiAgentReview(enabled: Boolean) {
        preferences.setMultiAgentReview(enabled)
        _orchestrationState.value = _orchestrationState.value.copy(isMultiAgentReview = enabled)
    }

    suspend fun setMaxRetryCount(count: Int) {
        preferences.setMaxRetryCount(count)
        _orchestrationState.value = _orchestrationState.value.copy(maxRetryCount = count)
    }

    suspend fun setMaxSpendingLimit(limit: Double) {
        preferences.setMaxSpendingLimit(limit)
        _orchestrationState.value = _orchestrationState.value.copy(maxSpendingLimitUsd = limit)
    }
}
