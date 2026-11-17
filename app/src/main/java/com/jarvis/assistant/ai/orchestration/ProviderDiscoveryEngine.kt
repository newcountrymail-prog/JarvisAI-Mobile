package com.jarvis.assistant.ai.orchestration

import kotlinx.coroutines.Dispatchers
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
 * Universal Provider Discovery & Validation Engine.
 * Identifies provider presets, sanitizes and masks keys, validates endpoints,
 * executes connection handshakes, and dynamically queries model catalogs.
 */
class ProviderDiscoveryEngine(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Automatically infers provider preset based on API key structure and base URL.
     */
    fun detectProvider(apiKey: String, baseUrl: String): ProviderPreset {
        val cleanKey = sanitizeKey(apiKey)
        val cleanUrl = baseUrl.trim().lowercase()

        // 1. Detect from Base URL if provided
        if (cleanUrl.contains("openrouter.ai")) return ProviderPreset.OPENROUTER
        if (cleanUrl.contains("generativelanguage.googleapis.com")) return ProviderPreset.GEMINI
        if (cleanUrl.contains("api.openai.com")) return ProviderPreset.OPENAI
        if (cleanUrl.contains("api.deepseek.com")) return ProviderPreset.DEEPSEEK
        if (cleanUrl.contains("api.groq.com")) return ProviderPreset.GROQ
        if (cleanUrl.contains("api.mistral.ai")) return ProviderPreset.MISTRAL
        if (cleanUrl.contains("11434") || cleanUrl.contains("ollama")) return ProviderPreset.OLLAMA
        if (cleanUrl.contains("api.anthropic.com")) return ProviderPreset.ANTHROPIC
        if (cleanUrl.contains("aliyuncs.com") || cleanUrl.contains("qwen")) return ProviderPreset.QWEN

        // 2. Detect from API Key pattern
        if (cleanKey.startsWith("sk-or-v1-")) return ProviderPreset.OPENROUTER
        if (cleanKey.startsWith("AIzaSy") || cleanKey.startsWith("AQ.")) return ProviderPreset.GEMINI
        if (cleanKey.startsWith("sk-proj-") || cleanKey.startsWith("sk-org-")) return ProviderPreset.OPENAI
        if (cleanKey.startsWith("gsk_")) return ProviderPreset.GROQ
        if (cleanKey.startsWith("sk-ant-")) return ProviderPreset.ANTHROPIC
        if (cleanKey.startsWith("ms-") || cleanKey.startsWith("mistral-")) return ProviderPreset.MISTRAL

        // 3. Fallback to Custom
        return if (cleanKey.isNotBlank()) ProviderPreset.CUSTOM else ProviderPreset.OLLAMA
    }

    /**
     * Strips accidental whitespace, newlines, and quotes from pasted keys.
     */
    fun sanitizeKey(key: String): String {
        return key.trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
            .trim()
    }

    /**
     * Generates a safe masked representation of an API key for UI display.
     */
    fun maskKey(key: String): String {
        val clean = sanitizeKey(key)
        if (clean.length <= 8) return "••••••••"
        return "${clean.take(8)}••••••••${clean.takeLast(4)}"
    }

    /**
     * Redacts keys and sensitive tokens from strings, error messages, and logs.
     */
    fun redactSensitive(text: String): String {
        return text.replace(Regex("sk-[A-Za-z0-9_-]{20,}"), "[REDACTED_API_KEY]")
            .replace(Regex("AIza[0-9A-Za-z-_]{35}"), "[REDACTED_GEMINI_KEY]")
            .replace(Regex("Bearer\\s+[A-Za-z0-9_.-]+", RegexOption.IGNORE_CASE), "Bearer [REDACTED]")
    }

    /**
     * Tests connectivity to a provider and measures latency.
     */
    suspend fun testConnectivity(
        credential: ProviderCredential,
        modelIdentifier: String? = null
    ): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val preset = try {
                ProviderPreset.valueOf(credential.providerPreset)
            } catch (e: Exception) {
                ProviderPreset.CUSTOM
            }

            val isHealthy = when (preset) {
                ProviderPreset.GEMINI -> testGemini(credential)
                ProviderPreset.OPENROUTER -> testOpenRouter(credential)
                ProviderPreset.OLLAMA -> testOllama(credential)
                else -> testOpenAiCompatible(credential, modelIdentifier)
            }
            val latency = System.currentTimeMillis() - startTime
            Pair(isHealthy, latency)
        } catch (e: Exception) {
            Pair(false, System.currentTimeMillis() - startTime)
        }
    }

    /**
     * Discovers available models for a given provider credential.
     */
    suspend fun discoverModels(
        credential: ProviderCredential
    ): Result<List<DiscoveredModel>> = withContext(Dispatchers.IO) {
        val preset = try {
            ProviderPreset.valueOf(credential.providerPreset)
        } catch (e: Exception) {
            ProviderPreset.CUSTOM
        }

        try {
            when (preset) {
                ProviderPreset.GEMINI -> discoverGeminiModels(credential)
                ProviderPreset.OPENROUTER -> discoverOpenRouterModels(credential)
                ProviderPreset.OLLAMA -> discoverOllamaModels(credential)
                else -> discoverOpenAiCompatibleModels(credential)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Model discovery failed: ${redactSensitive(e.localizedMessage ?: "Unknown error")}"))
        }
    }

    // --- Internal Handshake Implementations ---

    private fun testGemini(credential: ProviderCredential): Boolean {
        val key = sanitizeKey(credential.apiKey)
        val url = "https://generativelanguage.googleapis.com/v1beta/models?pageSize=1"
        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", key)
            .get()
            .build()
        val response = client.newCall(request).execute()
        return response.isSuccessful
    }

    private fun testOpenRouter(credential: ProviderCredential): Boolean {
        val key = sanitizeKey(credential.apiKey)
        val url = "https://openrouter.ai/api/v1/auth/key"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $key")
            .header("HTTP-Referer", "https://jarvis.assistant.ai")
            .header("X-Title", "JARVIS Universal AI")
            .get()
            .build()
        val response = client.newCall(request).execute()
        // If auth/key is 200, key is verified!
        if (response.isSuccessful) return true
        // Fallback to checking /models
        val fallbackReq = Request.Builder()
            .url("https://openrouter.ai/api/v1/models")
            .header("Authorization", "Bearer $key")
            .get()
            .build()
        return client.newCall(fallbackReq).execute().isSuccessful
    }

    private fun testOllama(credential: ProviderCredential): Boolean {
        val base = credential.baseUrl.trimEnd('/')
        val url = "$base/api/tags"
        val request = Request.Builder().url(url).get().build()
        return client.newCall(request).execute().isSuccessful
    }

    private fun testOpenAiCompatible(credential: ProviderCredential, model: String?): Boolean {
        val base = credential.baseUrl.trimEnd('/')
        val modelsUrl = if (base.endsWith("/v1")) "$base/models" else "$base/v1/models"
        val key = sanitizeKey(credential.apiKey)

        val reqBuilder = Request.Builder().url(modelsUrl).get()
        if (key.isNotBlank()) {
            reqBuilder.header("Authorization", "Bearer $key")
        }
        val response = client.newCall(reqBuilder.build()).execute()
        return response.isSuccessful
    }

    // --- Dynamic Model Fetchers ---

    private fun discoverGeminiModels(credential: ProviderCredential): Result<List<DiscoveredModel>> {
        val key = sanitizeKey(credential.apiKey)
        val url = "https://generativelanguage.googleapis.com/v1beta/models?pageSize=50"
        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", key)
            .get()
            .build()

        val response = client.newCall(request).execute()
        val bodyStr = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            return Result.failure(Exception("Gemini discovery returned HTTP ${response.code}"))
        }

        val json = JSONObject(bodyStr)
        val modelsArray = json.optJSONArray("models") ?: JSONArray()
        val result = mutableListOf<DiscoveredModel>()

        for (i in 0 until modelsArray.length()) {
            val obj = modelsArray.getJSONObject(i)
            val methods = obj.optJSONArray("supportedGenerationMethods")
            var supportsGenerate = false
            if (methods != null) {
                for (m in 0 until methods.length()) {
                    if (methods.getString(m) == "generateContent") {
                        supportsGenerate = true
                        break
                    }
                }
            }
            if (!supportsGenerate) continue

            val rawName = obj.optString("name", "")
            val id = rawName.removePrefix("models/")
            val displayName = obj.optString("displayName", id)
            val inputLimit = obj.optInt("inputTokenLimit", 32768)
            val desc = obj.optString("description", "")

            val caps = mutableListOf(ModelCapability.GENERAL.name, ModelCapability.STREAMING.name)
            if (inputLimit >= 64000) caps.add(ModelCapability.LONG_CONTEXT.name)
            if (id.contains("flash") || id.contains("pro")) caps.add(ModelCapability.VISION.name)
            if (id.contains("flash")) caps.add(ModelCapability.FREE.name)

            val codingScore = if (id.contains("pro")) 90 else 85
            val reasoningScore = if (id.contains("pro")) 92 else 82

            result.add(
                DiscoveredModel(
                    id = "gemini-$id",
                    providerId = credential.id,
                    modelIdentifier = id,
                    displayName = displayName,
                    description = desc.take(120),
                    contextCapacity = inputLimit,
                    freeCategory = FreeTierCategory.CATEGORY_B_FREE_TIER_QUOTA.name,
                    capabilities = caps,
                    codingSuitabilityScore = codingScore,
                    reasoningSuitabilityScore = reasoningScore,
                    pricingPrompt = "Free tier: 15 RPM / Paid after",
                    pricingCompletion = "Free tier: 1M tokens/min",
                    isAvailable = true
                )
            )
        }

        return Result.success(result)
    }

    private fun discoverOpenRouterModels(credential: ProviderCredential): Result<List<DiscoveredModel>> {
        val key = sanitizeKey(credential.apiKey)
        val url = "https://openrouter.ai/api/v1/models"
        val requestBuilder = Request.Builder().url(url).get()
        if (key.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $key")
        }
        requestBuilder.header("HTTP-Referer", "https://jarvis.assistant.ai")
        requestBuilder.header("X-Title", "JARVIS Universal AI")

        val response = client.newCall(requestBuilder.build()).execute()
        val bodyStr = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            return Result.failure(Exception("OpenRouter discovery returned HTTP ${response.code}"))
        }

        val root = JSONObject(bodyStr)
        val dataArray = root.optJSONArray("data") ?: JSONArray()
        val result = mutableListOf<DiscoveredModel>()

        for (i in 0 until dataArray.length()) {
            val item = dataArray.getJSONObject(i)
            val id = item.optString("id", "")
            val name = item.optString("name", id)
            val desc = item.optString("description", "")
            val contextLen = item.optInt("context_length", 32768)
            val pricing = item.optJSONObject("pricing")
            val promptPrice = pricing?.optString("prompt", "0") ?: "0"
            val completionPrice = pricing?.optString("completion", "0") ?: "0"

            val isFree = id.contains(":free") || (promptPrice == "0" && completionPrice == "0")

            val freeCategory = if (isFree) {
                FreeTierCategory.CATEGORY_A_VERIFIED_FREE.name
            } else {
                FreeTierCategory.CATEGORY_D_PAID.name
            }

            val caps = mutableListOf(ModelCapability.GENERAL.name, ModelCapability.STREAMING.name)
            if (isFree) caps.add(ModelCapability.FREE.name)
            if (contextLen >= 64000) caps.add(ModelCapability.LONG_CONTEXT.name)
            if (id.contains("vision") || id.contains("flash") || id.contains("4o")) caps.add(ModelCapability.VISION.name)
            if (id.contains("coder") || id.contains("code") || id.contains("qwen") || id.contains("dev")) caps.add(ModelCapability.CODING.name)
            if (id.contains("r1") || id.contains("reason") || id.contains("o1") || id.contains("70b")) caps.add(ModelCapability.REASONING.name)

            var codingScore = 60
            if (id.contains("coder") || id.contains("qwen-2.5-coder")) codingScore = 95
            else if (id.contains("claude") || id.contains("gpt-4")) codingScore = 92
            else if (id.contains("70b")) codingScore = 85

            var reasoningScore = 65
            if (id.contains("r1") || id.contains("o1") || id.contains("deepseek-r1")) reasoningScore = 98
            else if (id.contains("70b")) reasoningScore = 88

            result.add(
                DiscoveredModel(
                    id = "openrouter-${id.replace('/', '_')}",
                    providerId = credential.id,
                    modelIdentifier = id,
                    displayName = name,
                    description = desc.take(120),
                    contextCapacity = contextLen,
                    freeCategory = freeCategory,
                    capabilities = caps,
                    codingSuitabilityScore = codingScore,
                    reasoningSuitabilityScore = reasoningScore,
                    pricingPrompt = promptPrice,
                    pricingCompletion = completionPrice,
                    isAvailable = true
                )
            )
        }

        return Result.success(result)
    }

    private fun discoverOllamaModels(credential: ProviderCredential): Result<List<DiscoveredModel>> {
        val base = credential.baseUrl.trimEnd('/')
        val url = "$base/api/tags"
        val request = Request.Builder().url(url).get().build()
        val response = client.newCall(request).execute()
        val bodyStr = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            return Result.failure(Exception("Ollama tags returned HTTP ${response.code}"))
        }

        val json = JSONObject(bodyStr)
        val modelsArray = json.optJSONArray("models") ?: JSONArray()
        val result = mutableListOf<DiscoveredModel>()

        for (i in 0 until modelsArray.length()) {
            val item = modelsArray.getJSONObject(i)
            val name = item.optString("name", "")
            if (name.isBlank()) continue

            result.add(
                DiscoveredModel(
                    id = "ollama-$name",
                    providerId = credential.id,
                    modelIdentifier = name,
                    displayName = "Ollama $name",
                    description = "Local on-device / LAN LLM running on Ollama",
                    contextCapacity = 32768,
                    freeCategory = FreeTierCategory.CATEGORY_A_VERIFIED_FREE.name,
                    capabilities = listOf(
                        ModelCapability.GENERAL.name,
                        ModelCapability.CODING.name,
                        ModelCapability.FREE.name,
                        ModelCapability.STREAMING.name
                    ),
                    codingSuitabilityScore = 80,
                    reasoningSuitabilityScore = 80,
                    pricingPrompt = "0.00 (Local Hardware)",
                    pricingCompletion = "0.00 (Local Hardware)",
                    isAvailable = true
                )
            )
        }

        return Result.success(result)
    }

    private fun discoverOpenAiCompatibleModels(credential: ProviderCredential): Result<List<DiscoveredModel>> {
        val base = credential.baseUrl.trimEnd('/')
        val url = if (base.endsWith("/v1")) "$base/models" else "$base/v1/models"
        val key = sanitizeKey(credential.apiKey)

        val reqBuilder = Request.Builder().url(url).get()
        if (key.isNotBlank()) {
            reqBuilder.header("Authorization", "Bearer $key")
        }

        val response = client.newCall(reqBuilder.build()).execute()
        val bodyStr = response.body?.string() ?: ""
        if (!response.isSuccessful) {
            return Result.failure(Exception("OpenAI models returned HTTP ${response.code}"))
        }

        val json = JSONObject(bodyStr)
        val dataArray = json.optJSONArray("data") ?: JSONArray()
        val result = mutableListOf<DiscoveredModel>()

        for (i in 0 until dataArray.length()) {
            val item = dataArray.getJSONObject(i)
            val id = item.optString("id", "")
            if (id.isBlank()) continue

            val isCoding = id.contains("code") || id.contains("qwen") || id.contains("coder")
            val isReasoning = id.contains("r1") || id.contains("o1") || id.contains("reasoner")
            val isFree = id.contains(":free")

            val caps = mutableListOf(ModelCapability.GENERAL.name, ModelCapability.STREAMING.name)
            if (isFree) caps.add(ModelCapability.FREE.name)
            if (isCoding) caps.add(ModelCapability.CODING.name)
            if (isReasoning) caps.add(ModelCapability.REASONING.name)

            val freeCategory = if (isFree) {
                FreeTierCategory.CATEGORY_A_VERIFIED_FREE.name
            } else {
                FreeTierCategory.CATEGORY_D_PAID.name
            }

            result.add(
                DiscoveredModel(
                    id = "${credential.id}-$id",
                    providerId = credential.id,
                    modelIdentifier = id,
                    displayName = id,
                    description = "Model discovered on ${credential.displayName}",
                    contextCapacity = 32768,
                    freeCategory = freeCategory,
                    capabilities = caps,
                    codingSuitabilityScore = if (isCoding) 90 else 70,
                    reasoningSuitabilityScore = if (isReasoning) 95 else 75,
                    isAvailable = true
                )
            )
        }

        return Result.success(result)
    }
}
