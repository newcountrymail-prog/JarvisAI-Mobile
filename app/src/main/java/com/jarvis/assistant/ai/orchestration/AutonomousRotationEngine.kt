package com.jarvis.assistant.ai.orchestration

import com.jarvis.assistant.data.model.ChatMessage
import com.jarvis.assistant.data.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Result of an orchestrated multi-model execution.
 */
data class OrchestrationExecutionResult(
    val responseText: String,
    val completedByModelId: String,
    val completedByProviderId: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val latencyMs: Long,
    val rotationCount: Int,
    val rotationHistory: List<String> = emptyList(),
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Autonomous AI Rotation Engine.
 * Dynamically routes requests, detects quota exhaustion and outages,
 * and transparently rotates to the next eligible AI provider while strictly
 * respecting FREE MODE ONLY and user spending boundaries.
 */
class AutonomousRotationEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
    private val freeModelDetector: FreeModelDetectionEngine = FreeModelDetectionEngine(),
    private val taskClassifier: TaskIntelligenceClassifier = TaskIntelligenceClassifier(),
    private val quotaManager: UsageQuotaManager = UsageQuotaManager()
) {

    private val _rotationEvents = MutableStateFlow<List<RotationEvent>>(emptyList())
    val rotationEvents: StateFlow<List<RotationEvent>> = _rotationEvents.asStateFlow()

    /**
     * Executes a prompt with automatic multi-model rotation on rate limits or service failures.
     */
    suspend fun executeWithAutoRotation(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        systemInstruction: String,
        providers: List<ProviderCredential>,
        discoveredModels: List<DiscoveredModel>,
        primaryModelId: String,
        freeModeOnly: Boolean,
        autoSwitchingEnabled: Boolean,
        maxRetries: Int = 3
    ): OrchestrationExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val rotationLog = mutableListOf<String>()

        // 1. Identify Task Category
        val taskCat = taskClassifier.classifyTask(prompt)

        // 2. Filter Eligible Models according to Free Mode Only
        val eligibleModels = freeModelDetector.filterEligibleModels(discoveredModels, freeModeOnly)
        if (eligibleModels.isEmpty()) {
            val msg = if (freeModeOnly) {
                "FREE MODE ONLY is enabled, but no verified free models (Category A or B) are currently registered. Add an OpenRouter free key, Gemini key, or Ollama endpoint."
            } else {
                "No eligible AI models available. Please configure at least one active provider credential."
            }
            return@withContext OrchestrationExecutionResult(
                responseText = "",
                completedByModelId = "",
                completedByProviderId = "",
                promptTokens = 0,
                completionTokens = 0,
                latencyMs = System.currentTimeMillis() - startTime,
                rotationCount = 0,
                isSuccess = false,
                errorMessage = msg
            )
        }

        // 3. Build candidate chain
        // First candidate is primary model if eligible, else optimal model for task
        val primaryCandidate = eligibleModels.find { it.modelIdentifier == primaryModelId || it.id == primaryModelId }
            ?: taskClassifier.selectOptimalModel(eligibleModels, taskCat)
            ?: eligibleModels.first()

        val candidateQueue = mutableListOf<DiscoveredModel>()
        candidateQueue.add(primaryCandidate)

        // Add backup candidates in order of suitability, skipping duplicates
        val remainingCandidates = eligibleModels
            .filter { it.id != primaryCandidate.id }
            .sortedByDescending { taskClassifier.scoreModelForTask(it, taskCat) }
        candidateQueue.addAll(remainingCandidates)

        var lastError: String? = null
        var currentAttempt = 0
        val triedModelIds = mutableSetOf<String>()

        while (candidateQueue.isNotEmpty() && currentAttempt <= maxRetries) {
            val candidate = candidateQueue.removeAt(0)
            if (candidate.id in triedModelIds) continue
            triedModelIds.add(candidate.id)
            currentAttempt++

            val provider = providers.find { it.id == candidate.providerId && it.isEnabled }
            if (provider == null) {
                continue
            }

            // Compact context if model has a smaller context capacity
            val effectiveHistory = compactContext(conversationHistory, candidate.contextCapacity)

            // Attempt request execution
            val result = executeSingleModel(
                prompt = prompt,
                conversationHistory = effectiveHistory,
                systemInstruction = systemInstruction,
                credential = provider,
                model = candidate
            )

            if (result.isSuccess) {
                quotaManager.recordTransaction(
                    providerId = provider.id,
                    modelId = candidate.modelIdentifier,
                    promptTokens = result.promptTokens,
                    completionTokens = result.completionTokens,
                    latencyMs = result.latencyMs,
                    isFree = candidate.freeCategory == FreeTierCategory.CATEGORY_A_VERIFIED_FREE.name
                )

                val prefix = if (rotationLog.isNotEmpty()) {
                    "[JARVIS Auto-Routing: Completed via ${candidate.displayName} after failover]\n\n"
                } else ""

                return@withContext result.copy(
                    responseText = prefix + result.responseText,
                    rotationCount = rotationLog.size,
                    rotationHistory = rotationLog
                )
            } else {
                val err = result.errorMessage ?: "Unknown error"
                lastError = err

                // Check if error is rotatable (Rate limit, Quota, Service failure, Timeout, Model not found)
                val isRotatable = isRotatableError(err)
                if (!autoSwitchingEnabled || !isRotatable) {
                    // Do not rotate blindly on bad request (400) or auth error (401)
                    return@withContext result.copy(
                        rotationCount = rotationLog.size,
                        rotationHistory = rotationLog
                    )
                }

                // Record rotation event
                val nextCandidate = candidateQueue.firstOrNull()
                val nextName = nextCandidate?.displayName ?: "None remaining"
                val reasonMsg = "Rotated from ${candidate.displayName} to $nextName ($err)"
                rotationLog.add(reasonMsg)

                recordRotationEvent(
                    fromProviderId = provider.id,
                    fromModelId = candidate.modelIdentifier,
                    toProviderId = nextCandidate?.providerId ?: "none",
                    toModelId = nextCandidate?.modelIdentifier ?: "none",
                    reason = err,
                    success = false,
                    latencyMs = result.latencyMs
                )
            }
        }

        OrchestrationExecutionResult(
            responseText = "",
            completedByModelId = "",
            completedByProviderId = "",
            promptTokens = 0,
            completionTokens = 0,
            latencyMs = System.currentTimeMillis() - startTime,
            rotationCount = rotationLog.size,
            rotationHistory = rotationLog,
            isSuccess = false,
            errorMessage = "All eligible models exhausted ($lastError). Maximum retries reached ($maxRetries)."
        )
    }

    /**
     * Determines whether an error qualifies for automatic failover.
     */
    private fun isRotatableError(errorMessage: String): Boolean {
        val lower = errorMessage.lowercase()
        return lower.contains("429") ||
                lower.contains("rate limit") ||
                lower.contains("quota") ||
                lower.contains("500") ||
                lower.contains("502") ||
                lower.contains("503") ||
                lower.contains("504") ||
                lower.contains("timeout") ||
                lower.contains("404") ||
                lower.contains("not found") ||
                lower.contains("overloaded")
    }

    /**
     * Executes a single API request against a specific model and credential.
     */
    private fun executeSingleModel(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        systemInstruction: String,
        credential: ProviderCredential,
        model: DiscoveredModel
    ): OrchestrationExecutionResult {
        val startTime = System.currentTimeMillis()
        try {
            val preset = try {
                ProviderPreset.valueOf(credential.providerPreset)
            } catch (e: Exception) {
                ProviderPreset.CUSTOM
            }

            return when (preset) {
                ProviderPreset.GEMINI -> executeGemini(prompt, conversationHistory, systemInstruction, credential, model, startTime)
                else -> executeOpenAiCompatible(prompt, conversationHistory, systemInstruction, credential, model, startTime)
            }
        } catch (e: Exception) {
            return OrchestrationExecutionResult(
                responseText = "",
                completedByModelId = model.modelIdentifier,
                completedByProviderId = credential.id,
                promptTokens = 0,
                completionTokens = 0,
                latencyMs = System.currentTimeMillis() - startTime,
                rotationCount = 0,
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Unknown execution error"
            )
        }
    }

    private fun executeGemini(
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String,
        credential: ProviderCredential,
        model: DiscoveredModel,
        startTime: Long
    ): OrchestrationExecutionResult {
        val cleanModel = model.modelIdentifier.removePrefix("models/")
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent"
        val key = credential.apiKey.trim()

        val json = JSONObject()
        val contents = JSONArray()

        if (systemInstruction.isNotBlank()) {
            val sysPart = JSONObject().put("text", systemInstruction)
            val sysObj = JSONObject().put("role", "system").put("parts", JSONArray().put(sysPart))
            contents.put(sysObj)
        }

        for (msg in history.takeLast(10)) {
            val role = if (msg.sender == MessageSender.USER) "user" else "model"
            contents.put(JSONObject().put("role", role).put("parts", JSONArray().put(JSONObject().put("text", msg.text))))
        }
        contents.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt))))

        json.put("contents", contents)

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", key)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val respBody = response.body?.string() ?: ""
        val latency = System.currentTimeMillis() - startTime

        if (!response.isSuccessful) {
            val classified = quotaManager.classifyError(response.code, respBody)
            return OrchestrationExecutionResult(
                responseText = "",
                completedByModelId = model.modelIdentifier,
                completedByProviderId = credential.id,
                promptTokens = 0,
                completionTokens = 0,
                latencyMs = latency,
                rotationCount = 0,
                isSuccess = false,
                errorMessage = classified
            )
        }

        val root = JSONObject(respBody)
        val candidates = root.optJSONArray("candidates") ?: JSONArray()
        val firstCand = candidates.optJSONObject(0)
        val content = firstCand?.optJSONObject("content")
        val parts = content?.optJSONArray("parts") ?: JSONArray()
        val text = parts.optJSONObject(0)?.optString("text", "") ?: ""

        val usageMeta = root.optJSONObject("usageMetadata")
        val promptTokens = usageMeta?.optInt("promptTokenCount", 0) ?: 0
        val completionTokens = usageMeta?.optInt("candidatesTokenCount", 0) ?: 0

        return OrchestrationExecutionResult(
            responseText = text,
            completedByModelId = model.modelIdentifier,
            completedByProviderId = credential.id,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            latencyMs = latency,
            rotationCount = 0,
            isSuccess = true
        )
    }

    private fun executeOpenAiCompatible(
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String,
        credential: ProviderCredential,
        model: DiscoveredModel,
        startTime: Long
    ): OrchestrationExecutionResult {
        val base = credential.baseUrl.trimEnd('/')
        val url = if (base.endsWith("/v1")) "$base/chat/completions" else "$base/v1/chat/completions"
        val key = credential.apiKey.trim()

        val json = JSONObject()
        json.put("model", model.modelIdentifier)

        val messages = JSONArray()
        if (systemInstruction.isNotBlank()) {
            messages.put(JSONObject().put("role", "system").put("content", systemInstruction))
        }

        for (msg in history.takeLast(10)) {
            val role = if (msg.sender == MessageSender.USER) "user" else "assistant"
            messages.put(JSONObject().put("role", role).put("content", msg.text))
        }
        messages.put(JSONObject().put("role", "user").put("content", prompt))

        json.put("messages", messages)

        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val reqBuilder = Request.Builder().url(url).post(body)
        if (key.isNotBlank()) {
            reqBuilder.header("Authorization", "Bearer $key")
        }
        reqBuilder.header("HTTP-Referer", "https://jarvis.assistant.ai")
        reqBuilder.header("X-Title", "JARVIS Universal AI")

        val response = client.newCall(reqBuilder.build()).execute()
        val respBody = response.body?.string() ?: ""
        val latency = System.currentTimeMillis() - startTime

        if (!response.isSuccessful) {
            val classified = quotaManager.classifyError(response.code, respBody)
            return OrchestrationExecutionResult(
                responseText = "",
                completedByModelId = model.modelIdentifier,
                completedByProviderId = credential.id,
                promptTokens = 0,
                completionTokens = 0,
                latencyMs = latency,
                rotationCount = 0,
                isSuccess = false,
                errorMessage = classified
            )
        }

        val root = JSONObject(respBody)
        val choices = root.optJSONArray("choices") ?: JSONArray()
        val firstChoice = choices.optJSONObject(0)
        val text = firstChoice?.optJSONObject("message")?.optString("content", "") ?: ""

        val usage = root.optJSONObject("usage")
        val promptTokens = usage?.optInt("prompt_tokens", 0) ?: 0
        val completionTokens = usage?.optInt("completion_tokens", 0) ?: 0

        return OrchestrationExecutionResult(
            responseText = text,
            completedByModelId = model.modelIdentifier,
            completedByProviderId = credential.id,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            latencyMs = latency,
            rotationCount = 0,
            isSuccess = true
        )
    }

    /**
     * Compacts context history if the destination model has a smaller context capacity.
     */
    private fun compactContext(history: List<ChatMessage>, capacity: Int): List<ChatMessage> {
        if (capacity >= 64000 || history.size <= 6) return history
        // Keep the oldest turn for task objective anchor, and the latest 4 turns
        val first = history.take(1)
        val last = history.takeLast(4)
        return (first + last).distinctBy { it.id }
    }

    private fun recordRotationEvent(
        fromProviderId: String,
        fromModelId: String,
        toProviderId: String,
        toModelId: String,
        reason: String,
        success: Boolean,
        latencyMs: Long
    ) {
        val event = RotationEvent(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            fromProviderId = fromProviderId,
            fromModelId = fromModelId,
            toProviderId = toProviderId,
            toModelId = toModelId,
            reason = reason,
            success = success,
            latencyMs = latencyMs
        )
        _rotationEvents.value = (_rotationEvents.value + event).takeLast(100)
    }
}
