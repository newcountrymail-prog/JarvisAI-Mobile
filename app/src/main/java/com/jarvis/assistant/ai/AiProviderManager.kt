package com.jarvis.assistant.ai

import com.jarvis.assistant.data.model.AiProviderType
import com.jarvis.assistant.data.model.ChatMessage
import com.jarvis.assistant.data.model.ProviderRole
import com.jarvis.assistant.data.repository.JarvisPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Central AI Provider Orchestrator for JARVIS X.
 * Manages Gemini, OpenRouter, Qwen Coder, and Custom OpenAI providers,
 * handles role-based routing (General, Coding, Voice, Backup), transparent fallback,
 * model discovery, and usage metrics.
 */
class AiProviderManager(
    val geminiProvider: GeminiAiProvider,
    val openRouterProvider: OpenRouterAiProvider,
    val qwenProvider: QwenAiProvider,
    val customProvider: CustomOpenAiProvider,
    private val preferences: JarvisPreferences,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val providers = mapOf<String, AiProvider>(
        AiProviderType.GEMINI.id to geminiProvider,
        AiProviderType.OPENROUTER.id to openRouterProvider,
        AiProviderType.QWEN.id to qwenProvider,
        AiProviderType.CUSTOM.id to customProvider
    )

    private val _totalPromptTokens = MutableStateFlow(0)
    val totalPromptTokens: StateFlow<Int> = _totalPromptTokens.asStateFlow()

    private val _totalCompletionTokens = MutableStateFlow(0)
    val totalCompletionTokens: StateFlow<Int> = _totalCompletionTokens.asStateFlow()

    private val _lastLatencyMs = MutableStateFlow(0L)
    val lastLatencyMs: StateFlow<Long> = _lastLatencyMs.asStateFlow()

    private val _activeProviderState = MutableStateFlow<AiProvider>(geminiProvider)
    val activeProviderState: StateFlow<AiProvider> = _activeProviderState.asStateFlow()

    private val _modelCatalogues = MutableStateFlow<Map<String, List<ModelInfo>>>(emptyMap())
    val modelCatalogues: StateFlow<Map<String, List<ModelInfo>>> = _modelCatalogues.asStateFlow()

    init {
        scope.launch {
            preferences.activeProviderFlow.collect { id ->
                val p = getProvider(id) ?: geminiProvider
                _activeProviderState.value = p
            }
        }
        scope.launch {
            preferences.customBaseUrlFlow.collect { url ->
                customProvider.baseUrl = url
            }
        }
    }

    fun getProvider(id: String): AiProvider? = providers[id]

    fun getAllProviders(): List<AiProvider> = providers.values.toList()

    suspend fun getEffectiveProvider(role: ProviderRole): Pair<AiProvider, Pair<String, String>> {
        val (providerId, model, key) = when (role) {
            ProviderRole.CODING -> {
                val pId = preferences.codingProviderFlow.first()
                val m = preferences.qwenModelFlow.first()
                val k = preferences.qwenApiKeyFlow.first()
                Triple(pId, m, k)
            }
            ProviderRole.VOICE -> {
                val pId = preferences.voiceProviderFlow.first()
                val (m, k) = getModelAndKeyForProvider(pId)
                Triple(pId, m, k)
            }
            ProviderRole.BACKUP -> {
                val pId = preferences.backupProviderFlow.first()
                val (m, k) = getModelAndKeyForProvider(pId)
                Triple(pId, m, k)
            }
            ProviderRole.GENERAL -> {
                val pId = preferences.activeProviderFlow.first()
                val (m, k) = getModelAndKeyForProvider(pId)
                Triple(pId, m, k)
            }
        }

        val provider = getProvider(providerId) ?: geminiProvider
        val effectiveModel = if (model.isNotBlank()) model else provider.defaultModel
        return Pair(provider, Pair(effectiveModel, key))
    }

    suspend fun getModelAndKeyForProvider(providerId: String): Pair<String, String> {
        return when (providerId) {
            AiProviderType.GEMINI.id -> {
                Pair(preferences.geminiModelFlow.first(), preferences.geminiApiKeyFlow.first())
            }
            AiProviderType.OPENROUTER.id -> {
                Pair(preferences.openRouterModelFlow.first(), preferences.openRouterApiKeyFlow.first())
            }
            AiProviderType.QWEN.id -> {
                Pair(preferences.qwenModelFlow.first(), preferences.qwenApiKeyFlow.first())
            }
            AiProviderType.CUSTOM.id -> {
                Pair(preferences.customModelFlow.first(), preferences.customApiKeyFlow.first())
            }
            else -> Pair(preferences.geminiModelFlow.first(), preferences.geminiApiKeyFlow.first())
        }
    }

    suspend fun generateResponse(
        prompt: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        systemInstruction: String = "",
        role: ProviderRole = ProviderRole.GENERAL
    ): Result<AiResponse> = withContext(Dispatchers.IO) {
        val (primaryProvider, modelAndKey) = getEffectiveProvider(role)
        val (model, key) = modelAndKey

        val primaryResult = primaryProvider.generateResponse(
            prompt = prompt,
            conversationHistory = conversationHistory,
            systemInstruction = systemInstruction,
            model = model,
            apiKey = key
        )

        if (primaryResult.isSuccess) {
            val response = primaryResult.getOrThrow()
            recordUsage(response)
            return@withContext primaryResult
        }

        val autoFallback = preferences.autoFallbackFlow.first()
        if (!autoFallback) {
            return@withContext primaryResult
        }

        // Attempt fallback to configured Backup provider
        val (backupProvider, backupConfig) = getEffectiveProvider(ProviderRole.BACKUP)
        if (backupProvider.providerId != primaryProvider.providerId) {
            val fallbackResult = backupProvider.generateResponse(
                prompt = prompt,
                conversationHistory = conversationHistory,
                systemInstruction = systemInstruction,
                model = backupConfig.first,
                apiKey = backupConfig.second
            )

            if (fallbackResult.isSuccess) {
                val resp = fallbackResult.getOrThrow()
                recordUsage(resp)
                return@withContext Result.success(
                    resp.copy(text = "[Failover via ${backupProvider.displayName}]\n\n" + resp.text)
                )
            }
        }

        primaryResult
    }

    suspend fun streamResponse(
        prompt: String,
        conversationHistory: List<ChatMessage> = emptyList(),
        systemInstruction: String = "",
        role: ProviderRole = ProviderRole.GENERAL
    ): Flow<AiStreamChunk> {
        val (provider, modelAndKey) = getEffectiveProvider(role)
        val (model, key) = modelAndKey
        return provider.streamResponse(prompt, conversationHistory, systemInstruction, model, key)
    }

    suspend fun discoverModels(providerId: String): Result<List<ModelInfo>> = withContext(Dispatchers.IO) {
        val provider = getProvider(providerId) ?: return@withContext Result.failure(Exception("Unknown provider $providerId"))
        val (_, key) = getModelAndKeyForProvider(providerId)
        val result = provider.listModels(key)
        if (result.isSuccess) {
            val current = _modelCatalogues.value.toMutableMap()
            current[providerId] = result.getOrThrow()
            _modelCatalogues.value = current
        }
        result
    }

    suspend fun testConnection(providerId: String): Result<ConnectionTestResult> = withContext(Dispatchers.IO) {
        val provider = getProvider(providerId) ?: return@withContext Result.failure(Exception("Unknown provider $providerId"))
        val (model, key) = getModelAndKeyForProvider(providerId)
        provider.testConnection(model, key)
    }

    private fun recordUsage(response: AiResponse) {
        _lastLatencyMs.value = response.latencyMs
        response.usage?.let { u ->
            _totalPromptTokens.value += u.promptTokens
            _totalCompletionTokens.value += u.candidateTokens
        }
    }
}
