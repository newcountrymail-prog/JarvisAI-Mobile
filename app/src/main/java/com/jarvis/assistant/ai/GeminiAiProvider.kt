package com.jarvis.assistant.ai

import com.jarvis.assistant.data.model.ChatMessage
import com.jarvis.assistant.data.model.MessageSender
import kotlinx.coroutines.Dispatchers
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
        .build()
) : AiProvider {

    override val providerId: String = "google_gemini"
    override val displayName: String = "Google Gemini Live & Flash"
    override val defaultModel: String = "gemini-1.5-flash"

    override suspend fun listModels(apiKey: String): Result<List<ModelInfo>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.success(getCuratedDefaultModels())
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.success(getCuratedDefaultModels())
            }

            val root = JSONObject(responseString)
            val modelsArray = root.optJSONArray("models") ?: JSONArray()
            val result = mutableListOf<ModelInfo>()

            for (i in 0 until modelsArray.length()) {
                val item = modelsArray.getJSONObject(i)
                val rawName = item.optString("name", "") // e.g. "models/gemini-1.5-flash"
                val id = rawName.removePrefix("models/")
                val displayName = item.optString("displayName", id)
                val desc = item.optString("description", "")
                val inputLimit = item.optInt("inputTokenLimit", 1048576)
                val supportedMethods = item.optJSONArray("supportedGenerationMethods") ?: JSONArray()

                var supportsGenerate = false
                for (j in 0 until supportedMethods.length()) {
                    if (supportedMethods.getString(j) == "generateContent") {
                        supportsGenerate = true
                        break
                    }
                }

                if (supportsGenerate && id.contains("gemini")) {
                    val caps = mutableSetOf(AiCapability.GENERAL, AiCapability.STREAMING)
                    if (inputLimit >= 128000) caps.add(AiCapability.LONG_CONTEXT)
                    if (id.contains("pro") || id.contains("thinking")) caps.add(AiCapability.REASONING)
                    if (id.contains("flash")) caps.add(AiCapability.FAST)
                    if (id.contains("coder") || id.contains("pro")) caps.add(AiCapability.CODING)

                    result.add(
                        ModelInfo(
                            id = id,
                            name = displayName,
                            description = desc,
                            contextLength = inputLimit,
                            isFree = true,
                            capabilities = caps
                        )
                    )
                }
            }

            if (result.isEmpty()) Result.success(getCuratedDefaultModels()) else Result.success(result)
        } catch (e: Exception) {
            Result.success(getCuratedDefaultModels())
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
                id = "gemini-1.5-pro",
                name = "Gemini 1.5 Pro",
                description = "Highly capable reasoning model for complex architecture and coding.",
                contextLength = 2097152,
                isFree = false,
                capabilities = setOf(AiCapability.GENERAL, AiCapability.REASONING, AiCapability.CODING, AiCapability.LONG_CONTEXT)
            ),
            ModelInfo(
                id = "gemini-2.0-flash",
                name = "Gemini 2.0 Flash",
                description = "Next-gen high efficiency model with advanced real-time performance.",
                contextLength = 1048576,
                isFree = true,
                capabilities = setOf(AiCapability.GENERAL, AiCapability.FAST, AiCapability.STREAMING)
            ),
            ModelInfo(
                id = "gemini-3.1-flash-live-preview",
                name = "Gemini Live Realtime",
                description = "Low-latency voice & conversational streaming model.",
                contextLength = 128000,
                isFree = true,
                capabilities = setOf(AiCapability.GENERAL, AiCapability.FAST, AiCapability.STREAMING)
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
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is required. Configure your API key in Settings ➔ AI Providers.")
            )
        }

        try {
            val resolvedModel = resolveModelIdentifier(model)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$resolvedModel:generateContent?key=$apiKey"

            val jsonBody = buildRequestBody(prompt, conversationHistory, systemInstruction)
            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder().url(url).post(requestBody).build()
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.code, responseString)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val root = JSONObject(responseString)
            val text = extractTextFromResponse(root)
            val usage = extractUsageMetadata(root)

            Result.success(
                AiResponse(
                    text = text,
                    model = resolvedModel,
                    providerId = providerId,
                    usage = usage,
                    isSuccess = true,
                    latencyMs = System.currentTimeMillis() - startTime
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Gemini Communication Error: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    override fun streamResponse(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        systemInstruction: String,
        model: String,
        apiKey: String
    ): Flow<AiStreamChunk> = flow {
        if (apiKey.isBlank()) {
            emit(AiStreamChunk(textDelta = "", isComplete = true, error = "Gemini API key is required."))
            return@flow
        }

        val resolvedModel = resolveModelIdentifier(model)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$resolvedModel:streamGenerateContent?alt=sse&key=$apiKey"
        val jsonBody = buildRequestBody(prompt, conversationHistory, systemInstruction)
        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder().url(url).post(requestBody).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = parseErrorMessage(response.code, response.body?.string() ?: "")
                emit(AiStreamChunk(textDelta = "", isComplete = true, error = err))
                return@flow
            }

            val inputStream = response.body?.byteStream()
            if (inputStream == null) {
                emit(AiStreamChunk(textDelta = "", isComplete = true, error = "Empty stream response."))
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
            emit(AiStreamChunk(textDelta = "", isComplete = true, error = e.localizedMessage ?: "Stream error"))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun testConnection(model: String, apiKey: String): Result<ConnectionTestResult> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("API key is empty."))
        }

        try {
            val modelsResult = listModels(apiKey)
            val discoveredCount = modelsResult.getOrNull()?.size ?: 0

            val resolvedModel = resolveModelIdentifier(model)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$resolvedModel:generateContent?key=$apiKey"

            val testPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", "JARVIS connection test. Return 'ONLINE'.") })
                        })
                    })
                })
            }

            val requestBody = testPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder().url(url).post(requestBody).build()
            val response = client.newCall(request).execute()
            val duration = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                Result.success(
                    ConnectionTestResult(
                        isSuccessful = true,
                        latencyMs = duration,
                        message = "HTTP 200 OK — Connected to Google Generative Language ($resolvedModel).",
                        activeModel = resolvedModel,
                        discoveredModelsCount = discoveredCount
                    )
                )
            } else {
                val err = parseErrorMessage(response.code, response.body?.string() ?: "")
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Connection handshake failed: ${e.localizedMessage ?: "Network unreachable"}"))
        }
    }

    private fun resolveModelIdentifier(model: String): String {
        return model.trim().removePrefix("models/").ifBlank { defaultModel }
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

    private fun parseErrorMessage(code: Int, body: String): String {
        val serverMsg = try {
            JSONObject(body).optJSONObject("error")?.optString("message", "") ?: ""
        } catch (_: Exception) { "" }

        val detail = if (serverMsg.isNotBlank()) " Detail: $serverMsg" else ""

        return when (code) {
            400 -> "HTTP 400 Bad Request: Invalid model parameters or unsupported prompt structure.$detail"
            401, 403 -> "HTTP $code Unauthorized/Forbidden: API key is invalid or restricted for this Google AI Studio project.$detail"
            404 -> "HTTP 404 Model Not Found: The requested model identifier is unavailable.$detail"
            429 -> "HTTP 429 Quota Exceeded: Model rate limits or quota exhausted for this API key.$detail"
            500, 503 -> "HTTP $code Service Unavailable: Google Generative AI backend temporarily offline.$detail"
            else -> "HTTP $code Error: Request failed.$detail"
        }
    }
}
