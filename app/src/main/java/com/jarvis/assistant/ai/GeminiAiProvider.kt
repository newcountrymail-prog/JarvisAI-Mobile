package com.jarvis.assistant.ai

import com.jarvis.assistant.ai.gemini.GeminiAutoConfigEngine
import com.jarvis.assistant.ai.gemini.GeminiCompatibilityTester
import com.jarvis.assistant.ai.gemini.GeminiDiscoveredModel
import com.jarvis.assistant.ai.gemini.GeminiErrorDiagnoser
import com.jarvis.assistant.ai.gemini.GeminiKeyValidator
import com.jarvis.assistant.ai.gemini.GeminiModelCompatibility
import com.jarvis.assistant.ai.gemini.GeminiModelDiscovery
import com.jarvis.assistant.data.model.ChatMessage
import com.jarvis.assistant.data.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class GeminiAiProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    val autoConfigEngine: GeminiAutoConfigEngine? = null,
    val modelDiscovery: GeminiModelDiscovery = GeminiModelDiscovery(client),
    val compatibilityTester: GeminiCompatibilityTester = GeminiCompatibilityTester(client)
) : AiProvider {

    override val providerId: String = "google_gemini"
    override val displayName: String = "Google Gemini Live & Flash"
    override val defaultModel: String = "gemini-1.5-flash"

    override suspend fun listModels(apiKey: String): Result<List<ModelInfo>> = withContext(Dispatchers.IO) {
        val cleanedKey = GeminiKeyValidator.cleanKey(apiKey)
        if (cleanedKey.isBlank()) {
            return@withContext Result.success(getCuratedDefaultModels())
        }

        val discoveryResult = modelDiscovery.discoverLiveModels(cleanedKey)
        if (discoveryResult.isSuccess) {
            val discoveredList = discoveryResult.getOrThrow()
            if (discoveredList.isEmpty()) {
                return@withContext Result.success(getCuratedDefaultModels())
            }
            val modelInfoList = discoveredList.map { dm ->
                val caps = mutableSetOf(AiCapability.GENERAL, AiCapability.STREAMING)
                if (dm.inputTokenLimit >= 128000) caps.add(AiCapability.LONG_CONTEXT)
                if (dm.id.contains("pro") || dm.id.contains("thinking")) caps.add(AiCapability.REASONING)
                if (dm.id.contains("flash")) caps.add(AiCapability.FAST)
                if (dm.id.contains("code") || dm.id.contains("pro")) caps.add(AiCapability.CODING)

                ModelInfo(
                    id = dm.id,
                    name = dm.displayName,
                    description = dm.description,
                    contextLength = dm.inputTokenLimit,
                    isFree = true,
                    capabilities = caps
                )
            }
            Result.success(modelInfoList)
        } else {
            // Forward real discovery error if caller needs it
            val ex = discoveryResult.exceptionOrNull()
            Result.failure(ex ?: Exception("Failed to discover Gemini models from Google API."))
        }
    }

    private fun getCuratedDefaultModels(): List<ModelInfo> {
        return listOf(
            ModelInfo(
                id = "gemini-1.5-flash",
                name = "Gemini 1.5 Flash",
                description = "Fast, versatile multimodal model for general intelligence & speed.",
                contextLength = 1048576,
                isFree = true,
                capabilities = setOf(AiCapability.GENERAL, AiCapability.FAST, AiCapability.STREAMING, AiCapability.LONG_CONTEXT)
            ),
            ModelInfo(
                id = "gemini-2.0-flash",
                name = "Gemini 2.0 Flash",
                description = "Next-generation high-efficiency model with real-time speed.",
                contextLength = 1048576,
                isFree = true,
                capabilities = setOf(AiCapability.GENERAL, AiCapability.FAST, AiCapability.STREAMING)
            ),
            ModelInfo(
                id = "gemini-1.5-pro",
                name = "Gemini 1.5 Pro",
                description = "Highly capable reasoning model for complex architecture and coding.",
                contextLength = 2097152,
                isFree = false,
                capabilities = setOf(AiCapability.GENERAL, AiCapability.REASONING, AiCapability.CODING, AiCapability.LONG_CONTEXT)
            )
        )
    }

    override suspend fun generateResponse(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        systemInstruction: String,
        model: String,
        apiKey: String
    ): Result<AiResponse> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val cleanedKey = GeminiKeyValidator.cleanKey(apiKey)
        if (cleanedKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is required. Please configure your key in Settings ➔ Gemini Live API.")
            )
        }

        var activeModel = resolveModelIdentifier(model)
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$activeModel:generateContent"

        val jsonBody = buildRequestBody(prompt, conversationHistory, systemInstruction)
        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$endpoint?key=$cleanedKey")
            .addHeader("x-goog-api-key", cleanedKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // If model is not found (404), attempt intelligent self-repair
                if (response.code == 404 && autoConfigEngine != null) {
                    val repairResult = autoConfigEngine.autoRepairModel(cleanedKey, activeModel)
                    if (repairResult.isSuccess) {
                        val newModel = repairResult.getOrThrow()
                        activeModel = newModel
                        // Retry once with newly repaired model
                        val retryEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/$activeModel:generateContent"
                        val retryRequest = Request.Builder()
                            .url("$retryEndpoint?key=$cleanedKey")
                            .addHeader("x-goog-api-key", cleanedKey)
                            .addHeader("Content-Type", "application/json")
                            .post(requestBody)
                            .build()
                        val retryResponse = client.newCall(retryRequest).execute()
                        val retryBody = retryResponse.body?.string() ?: ""
                        if (retryResponse.isSuccessful) {
                            val root = JSONObject(retryBody)
                            val text = extractTextFromResponse(root)
                            val usage = extractUsageMetadata(root)
                            return@withContext Result.success(
                                AiResponse(
                                    text = text,
                                    model = activeModel,
                                    providerId = providerId,
                                    usage = usage,
                                    isSuccess = true,
                                    latencyMs = System.currentTimeMillis() - startTime
                                )
                            )
                        }
                    }
                }

                val diagnosis = GeminiErrorDiagnoser.diagnoseHttpError(
                    code = response.code,
                    responseBody = responseString,
                    modelId = activeModel,
                    endpoint = endpoint
                )
                return@withContext Result.failure(Exception("${diagnosis.errorCategory} (${response.code}): ${diagnosis.googleErrorMessage} [Fix: ${diagnosis.suggestedCorrection}]"))
            }

            val root = JSONObject(responseString)
            val text = extractTextFromResponse(root)
            val usage = extractUsageMetadata(root)

            Result.success(
                AiResponse(
                    text = text,
                    model = activeModel,
                    providerId = providerId,
                    usage = usage,
                    isSuccess = true,
                    latencyMs = System.currentTimeMillis() - startTime
                )
            )
        } catch (e: Exception) {
            val diagnosis = GeminiErrorDiagnoser.diagnoseException(e, activeModel, endpoint)
            Result.failure(Exception("${diagnosis.errorCategory}: ${diagnosis.googleErrorMessage} [Fix: ${diagnosis.suggestedCorrection}]"))
        }
    }

    override fun streamResponse(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        systemInstruction: String,
        model: String,
        apiKey: String
    ): Flow<AiStreamChunk> = flow {
        val cleanedKey = GeminiKeyValidator.cleanKey(apiKey)
        if (cleanedKey.isBlank()) {
            emit(AiStreamChunk(textDelta = "", isComplete = true, error = "Gemini API key is required."))
            return@flow
        }

        var activeModel = resolveModelIdentifier(model)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$activeModel:streamGenerateContent?alt=sse&key=$cleanedKey"
        val jsonBody = buildRequestBody(prompt, conversationHistory, systemInstruction)
        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", cleanedKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            var response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                // If 404 and auto-repair available, attempt once
                if (response.code == 404 && autoConfigEngine != null) {
                    val repair = autoConfigEngine.autoRepairModel(cleanedKey, activeModel)
                    if (repair.isSuccess) {
                        activeModel = repair.getOrThrow()
                        val retryUrl = "https://generativelanguage.googleapis.com/v1beta/models/$activeModel:streamGenerateContent?alt=sse&key=$cleanedKey"
                        val retryReq = Request.Builder()
                            .url(retryUrl)
                            .addHeader("x-goog-api-key", cleanedKey)
                            .addHeader("Content-Type", "application/json")
                            .post(requestBody)
                            .build()
                        response = client.newCall(retryReq).execute()
                    }
                }
            }

            if (!response.isSuccessful) {
                val diagnosis = GeminiErrorDiagnoser.diagnoseHttpError(
                    code = response.code,
                    responseBody = response.body?.string() ?: "",
                    modelId = activeModel,
                    endpoint = url
                )
                emit(AiStreamChunk(textDelta = "", isComplete = true, error = "${diagnosis.errorCategory} (${response.code}): ${diagnosis.googleErrorMessage}"))
                return@flow
            }

            val inputStream = response.body?.byteStream()
            if (inputStream == null) {
                emit(AiStreamChunk(textDelta = "", isComplete = true, error = "Empty stream response from Gemini API."))
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.startsWith("data: ")) {
                    val jsonChunk = currentLine.removePrefix("data: ").trim()
                    if (jsonChunk.isNotBlank()) {
                        try {
                            val chunkObj = JSONObject(jsonChunk)
                            val delta = extractTextFromResponse(chunkObj)
                            if (delta.isNotBlank()) {
                                emit(AiStreamChunk(textDelta = delta, isComplete = false))
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
            emit(AiStreamChunk(textDelta = "", isComplete = true))
        } catch (e: Exception) {
            val diagnosis = GeminiErrorDiagnoser.diagnoseException(e, activeModel, url)
            emit(AiStreamChunk(textDelta = "", isComplete = true, error = "${diagnosis.errorCategory}: ${diagnosis.googleErrorMessage}"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun testConnection(model: String, apiKey: String): Result<ConnectionTestResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val cleanedKey = GeminiKeyValidator.cleanKey(apiKey)
        if (cleanedKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("API key is empty. Enter a valid Google AI Studio key."))
        }

        try {
            val resolvedModel = resolveModelIdentifier(model)
            val dummyModel = GeminiDiscoveredModel(
                id = resolvedModel,
                rawResourceName = "models/$resolvedModel",
                displayName = resolvedModel,
                description = "Connection probe",
                supportedGenerationMethods = listOf("generateContent"),
                inputTokenLimit = 1048576,
                outputTokenLimit = 8192
            )

            val tested = compatibilityTester.testSingleModel(dummyModel, cleanedKey)
            val duration = System.currentTimeMillis() - startTime

            if (tested.compatibility == GeminiModelCompatibility.VERIFIED_WORKING) {
                Result.success(
                    ConnectionTestResult(
                        isSuccessful = true,
                        latencyMs = tested.latencyMs.takeIf { it > 0 } ?: duration,
                        message = "HTTP 200 OK — Handshake Verified with Google Generative Language ($resolvedModel).",
                        activeModel = resolvedModel,
                        discoveredModelsCount = 1
                    )
                )
            } else {
                val errMsg = tested.lastErrorMessage ?: "Handshake failed without specific error message."
                Result.failure(Exception("Connection verification failed: $errMsg"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection handshake exception: ${e.localizedMessage ?: "Network unreachable"}"))
        }
    }

    fun resolveModelIdentifier(model: String): String {
        val cleaned = model.trim().removePrefix("models/").ifBlank { defaultModel }
        // Replace known non-existent or deprecated placeholders with stable flash model
        return if (cleaned.contains("gemini-3.1-flash", ignoreCase = true) || cleaned.isBlank()) {
            defaultModel
        } else {
            cleaned
        }
    }

    private fun buildRequestBody(
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String
    ): JSONObject {
        val root = JSONObject()
        if (systemInstruction.isNotBlank()) {
            root.put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                })
            })
        }

        val contentsArray = JSONArray()
        for (msg in history.takeLast(10)) {
            val role = if (msg.sender == MessageSender.USER) "user" else "model"
            contentsArray.put(JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", msg.text) })
                })
            })
        }

        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", prompt) })
            })
        })

        root.put("contents", contentsArray)
        root.put("generationConfig", JSONObject().apply {
            put("temperature", 0.7)
            put("maxOutputTokens", 2048)
        })

        return root
    }

    private fun extractTextFromResponse(root: JSONObject): String {
        val candidates = root.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content") ?: return ""
        val parts = content.optJSONArray("parts") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            sb.append(parts.getJSONObject(i).optString("text", ""))
        }
        return sb.toString()
    }

    private fun extractUsageMetadata(root: JSONObject): TokenUsage? {
        val usage = root.optJSONObject("usageMetadata") ?: return null
        return TokenUsage(
            promptTokens = usage.optInt("promptTokenCount", 0),
            candidateTokens = usage.optInt("candidatesTokenCount", 0),
            totalTokens = usage.optInt("totalTokenCount", 0)
        )
    }
}
