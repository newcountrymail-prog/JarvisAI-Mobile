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

class QwenAiProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build(),
    var customBaseUrl: String = "https://openrouter.ai/api/v1"
) : AiProvider {

    override val providerId: String = "qwen_coder"
    override val displayName: String = "Qwen Coder AI (Dedicated Engine)"
    override val defaultModel: String = "qwen/qwen-2.5-coder-32b-instruct"

    override suspend fun listModels(apiKey: String): Result<List<ModelInfo>> = withContext(Dispatchers.IO) {
        val models = listOf(
            ModelInfo(
                id = "qwen/qwen-2.5-coder-32b-instruct",
                name = "Qwen 2.5 Coder 32B Instruct",
                description = "Primary coding model for Kotlin, Jetpack Compose, Gradle, and Android architecture.",
                contextLength = 32768,
                isFree = false,
                capabilities = setOf(AiCapability.CODING, AiCapability.GENERAL, AiCapability.STREAMING, AiCapability.REASONING)
            ),
            ModelInfo(
                id = "qwen/qwen-2.5-coder-7b-instruct",
                name = "Qwen 2.5 Coder 7B",
                description = "Ultra-fast low-latency coding model for unit tests and syntax checks.",
                contextLength = 32768,
                isFree = false,
                capabilities = setOf(AiCapability.CODING, AiCapability.FAST, AiCapability.STREAMING)
            ),
            ModelInfo(
                id = "qwen/qwen-2.5-72b-instruct",
                name = "Qwen 2.5 72B General & Code",
                description = "High-parameter reasoning powerhouse for deep algorithmic and system engineering.",
                contextLength = 131072,
                isFree = false,
                capabilities = setOf(AiCapability.CODING, AiCapability.REASONING, AiCapability.LONG_CONTEXT)
            )
        )
        Result.success(models)
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
            return@withContext Result.failure(IllegalStateException("Qwen API key is required. Configure your key in Settings ➔ Qwen Coder."))
        }

        try {
            val targetUrl = "${customBaseUrl.trimEnd('/')}/chat/completions"
            val targetModel = model.ifBlank { defaultModel }
            val specializedInstruction = buildCodingSystemPrompt(systemInstruction)
            val jsonBody = buildRequestBody(prompt, conversationHistory, specializedInstruction, targetModel, stream = false)
            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(targetUrl)
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://jarvis.assistant.ai")
                .header("X-Title", "JARVIS X Qwen Coder")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Qwen Error ${response.code}: $responseString"))
            }

            val root = JSONObject(responseString)
            val choices = root.optJSONArray("choices") ?: JSONArray()
            val firstChoice = choices.optJSONObject(0)
            val content = firstChoice?.optJSONObject("message")?.optString("content", "") ?: ""
            val usageObj = root.optJSONObject("usage")
            val tokenUsage = usageObj?.let {
                TokenUsage(
                    promptTokens = it.optInt("prompt_tokens", 0),
                    candidateTokens = it.optInt("completion_tokens", 0),
                    totalTokens = it.optInt("total_tokens", 0)
                )
            }

            Result.success(
                AiResponse(
                    text = content,
                    model = targetModel,
                    providerId = providerId,
                    usage = tokenUsage,
                    isSuccess = true,
                    latencyMs = System.currentTimeMillis() - startTime
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Qwen request failed: ${e.localizedMessage ?: "Network error"}"))
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
            emit(AiStreamChunk(textDelta = "", isComplete = true, error = "Qwen API key is required."))
            return@flow
        }

        val targetUrl = "${customBaseUrl.trimEnd('/')}/chat/completions"
        val targetModel = model.ifBlank { defaultModel }
        val specializedInstruction = buildCodingSystemPrompt(systemInstruction)
        val jsonBody = buildRequestBody(prompt, conversationHistory, specializedInstruction, targetModel, stream = true)
        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(targetUrl)
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://jarvis.assistant.ai")
            .header("X-Title", "JARVIS X Qwen Coder")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(AiStreamChunk(textDelta = "", isComplete = true, error = "HTTP ${response.code}: ${response.body?.string() ?: ""}"))
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
                    val dataContent = currentLine.removePrefix("data: ").trim()
                    if (dataContent == "[DONE]") break
                    if (dataContent.isNotBlank()) {
                        try {
                            val chunkObj = JSONObject(dataContent)
                            val choices = chunkObj.optJSONArray("choices")
                            val deltaObj = choices?.optJSONObject(0)?.optJSONObject("delta")
                            val textDelta = deltaObj?.optString("content", "") ?: ""
                            if (textDelta.isNotBlank()) {
                                emit(AiStreamChunk(textDelta = textDelta, isComplete = false))
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
            val targetUrl = "${customBaseUrl.trimEnd('/')}/chat/completions"
            val targetModel = model.ifBlank { defaultModel }

            val testPayload = JSONObject().apply {
                put("model", targetModel)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "JARVIS Qwen handshake test. Return 'QWEN_ONLINE'.")
                    })
                })
                put("max_tokens", 10)
            }

            val requestBody = testPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(targetUrl)
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://jarvis.assistant.ai")
                .header("X-Title", "JARVIS X")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val duration = System.currentTimeMillis() - startTime

            if (response.isSuccessful) {
                Result.success(
                    ConnectionTestResult(
                        isSuccessful = true,
                        latencyMs = duration,
                        message = "HTTP 200 OK — Qwen Coder Engine operational ($targetModel).",
                        activeModel = targetModel,
                        discoveredModelsCount = 3
                    )
                )
            } else {
                Result.failure(Exception("HTTP ${response.code}: ${response.body?.string()?.take(150) ?: "Failed"}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Qwen connection failed: ${e.localizedMessage ?: "Network error"}"))
        }
    }

    private fun buildCodingSystemPrompt(baseInstruction: String): String {
        return """
        $baseInstruction
        ### SPECIALIZED CODING DIRECTIVE:
        You are QWEN CODER, the dedicated senior software architect for JARVIS X.
        You specialize in:
        1. Kotlin, Android Architecture, Jetpack Compose, MVVM, Room, and Coroutines.
        2. Gradle and APK compilation diagnostics.
        3. Python, Node.js, Shell, C/C++, and SQL.
        4. Delivering precise, modular, robust code implementations with zero placeholders.
        5. Explaining architecture, generating unit tests, and providing exact git-style diffs when refactoring.
        """.trimIndent()
    }

    private fun buildRequestBody(
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String,
        model: String,
        stream: Boolean
    ): JSONObject {
        val root = JSONObject()
        root.put("model", model)
        root.put("stream", stream)

        val messages = JSONArray()
        if (systemInstruction.isNotBlank()) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })
        }

        for (msg in history.takeLast(10)) {
            val role = if (msg.sender == MessageSender.USER) "user" else "assistant"
            messages.put(JSONObject().apply {
                put("role", role)
                put("content", msg.text)
            })
        }

        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })

        root.put("messages", messages)
        root.put("temperature", 0.3) // Lower temperature for high coding precision
        return root
    }
}
