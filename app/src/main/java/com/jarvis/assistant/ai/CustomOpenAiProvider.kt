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

/**
 * Universal OpenAI-compatible AI provider.
 * Supports OpenAI, Together AI, Groq, Ollama, LocalAI, vLLM, DeepSeek, and custom proxy endpoints.
 */
class CustomOpenAiProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build(),
    var baseUrl: String = "https://api.openai.com/v1"
) : AiProvider {

    override val providerId: String = "custom"
    override val displayName: String = "Custom OpenAI Compatible"
    override val defaultModel: String = "gpt-4o-mini"

    private fun normalizeEndpoint(path: String): String {
        val base = baseUrl.trim().trimEnd('/')
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return if (base.endsWith("/v1")) {
            "$base$cleanPath"
        } else {
            "$base/v1$cleanPath"
        }
    }

    override suspend fun listModels(apiKey: String): Result<List<ModelInfo>> = withContext(Dispatchers.IO) {
        try {
            val url = normalizeEndpoint("/models")
            val requestBuilder = Request.Builder().url(url).get()
            if (apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.success(getFallbackModels())
            }

            val root = JSONObject(bodyString)
            val dataArray = root.optJSONArray("data") ?: JSONArray()
            val list = mutableListOf<ModelInfo>()

            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val id = item.optString("id", "")
                if (id.isNotBlank()) {
                    list.add(
                        ModelInfo(
                            id = id,
                            name = id,
                            description = "Model on ${baseUrl.take(30)}",
                            capabilities = setOf(AiCapability.GENERAL, AiCapability.STREAMING)
                        )
                    )
                }
            }

            if (list.isEmpty()) {
                Result.success(getFallbackModels())
            } else {
                Result.success(list.sortedBy { it.id })
            }
        } catch (e: Exception) {
            Result.success(getFallbackModels())
        }
    }

    private fun getFallbackModels(): List<ModelInfo> {
        return listOf(
            ModelInfo("gpt-4o-mini", "GPT-4o Mini", "Fast and intelligent flagship model", contextLength = 128000),
            ModelInfo("gpt-4o", "GPT-4o", "High-intelligence multimodal flagship", contextLength = 128000),
            ModelInfo("deepseek-chat", "DeepSeek Chat (V3)", "High capability general chat & reasoning", contextLength = 64000),
            ModelInfo("deepseek-reasoner", "DeepSeek R1", "Advanced reasoning and chain of thought", contextLength = 64000),
            ModelInfo("llama-3.3-70b-instruct", "Llama 3.3 70B", "Meta open-weights flagship", contextLength = 128000),
            ModelInfo("qwen2.5-coder-32b", "Qwen 2.5 Coder 32B", "Leading coding and syntax LLM", contextLength = 32768)
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
        try {
            val effectiveModel = if (model.isNotBlank()) model else defaultModel
            val url = normalizeEndpoint("/chat/completions")

            val jsonBody = JSONObject().apply {
                put("model", effectiveModel)
                put("messages", buildMessagesArray(prompt, conversationHistory, systemInstruction))
                put("temperature", 0.7)
                put("stream", false)
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))

            if (apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val latency = System.currentTimeMillis() - startTime
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = parseErrorMessage(response.code, bodyString)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(bodyString)
            val choices = json.optJSONArray("choices")
            val content = choices?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content", "")
                ?: ""

            val usageObj = json.optJSONObject("usage")
            val usage = usageObj?.let {
                TokenUsage(
                    promptTokens = it.optInt("prompt_tokens", 0),
                    candidateTokens = it.optInt("completion_tokens", 0),
                    totalTokens = it.optInt("total_tokens", 0)
                )
            }

            Result.success(
                AiResponse(
                    text = content.trim(),
                    model = effectiveModel,
                    providerId = providerId,
                    usage = usage,
                    isSuccess = true,
                    latencyMs = latency
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun streamResponse(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        systemInstruction: String,
        model: String,
        apiKey: String
    ): Flow<AiStreamChunk> = flow {
        val effectiveModel = if (model.isNotBlank()) model else defaultModel
        val url = normalizeEndpoint("/chat/completions")

        val jsonBody = JSONObject().apply {
            put("model", effectiveModel)
            put("messages", buildMessagesArray(prompt, conversationHistory, systemInstruction))
            put("temperature", 0.7)
            put("stream", true)
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))

        if (apiKey.isNotBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        if (!response.isSuccessful) {
            val body = response.body?.string() ?: ""
            emit(AiStreamChunk("", isComplete = true, error = parseErrorMessage(response.code, body)))
            return@flow
        }

        val source = response.body?.byteStream()
        if (source == null) {
            emit(AiStreamChunk("", isComplete = true, error = "Empty response stream from server."))
            return@flow
        }

        val reader = BufferedReader(InputStreamReader(source))
        var line: String?

        try {
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line?.trim() ?: continue
                if (trimmed.isEmpty() || trimmed.startsWith(":")) continue

                if (trimmed.startsWith("data:")) {
                    val data = trimmed.removePrefix("data:").trim()
                    if (data == "[DONE]") {
                        emit(AiStreamChunk("", isComplete = true))
                        break
                    }

                    try {
                        val chunkObj = JSONObject(data)
                        val deltaObj = chunkObj.optJSONArray("choices")
                            ?.optJSONObject(0)
                            ?.optJSONObject("delta")
                        val deltaText = deltaObj?.optString("content", "") ?: ""

                        if (deltaText.isNotEmpty()) {
                            emit(AiStreamChunk(deltaText, isComplete = false))
                        }
                    } catch (_: Exception) {
                        // Keep streaming on partial JSON chunks
                    }
                }
            }
            emit(AiStreamChunk("", isComplete = true))
        } catch (e: Exception) {
            emit(AiStreamChunk("", isComplete = true, error = e.localizedMessage ?: "Stream interrupted."))
        } finally {
            reader.close()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun testConnection(model: String, apiKey: String): Result<ConnectionTestResult> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val effectiveModel = if (model.isNotBlank()) model else defaultModel
            val testPrompt = "Ping. Reply with exactly: PONG"
            val res = generateResponse(testPrompt, emptyList(), "Respond concisely.", effectiveModel, apiKey)

            val latency = System.currentTimeMillis() - start
            if (res.isSuccess) {
                Result.success(
                    ConnectionTestResult(
                        isSuccessful = true,
                        latencyMs = latency,
                        message = "Connected to $baseUrl ($effectiveModel) in ${latency}ms",
                        activeModel = effectiveModel
                    )
                )
            } else {
                val err = res.exceptionOrNull()?.message ?: "Unknown error"
                Result.success(
                    ConnectionTestResult(
                        isSuccessful = false,
                        latencyMs = latency,
                        message = "Connection failed: $err",
                        activeModel = effectiveModel
                    )
                )
            }
        } catch (e: Exception) {
            Result.success(
                ConnectionTestResult(
                    isSuccessful = false,
                    latencyMs = System.currentTimeMillis() - start,
                    message = "Network error: ${e.localizedMessage ?: "Unreachable host"}",
                    activeModel = model
                )
            )
        }
    }

    private fun buildMessagesArray(
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String
    ): JSONArray {
        val messages = JSONArray()

        if (systemInstruction.isNotBlank()) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemInstruction)
            })
        }

        val window = history.takeLast(10)
        for (msg in window) {
            val role = when (msg.sender) {
                MessageSender.USER -> "user"
                MessageSender.JARVIS -> "assistant"
                MessageSender.SYSTEM -> "system"
            }
            messages.put(JSONObject().apply {
                put("role", role)
                put("content", msg.text)
            })
        }

        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", prompt)
        })

        return messages
    }

    private fun parseErrorMessage(code: Int, body: String): String {
        return try {
            val json = JSONObject(body)
            val errObj = json.optJSONObject("error")
            val msg = errObj?.optString("message", "")
            if (!msg.isNullOrBlank()) {
                "API Error ($code): $msg"
            } else {
                "HTTP $code: $body"
            }
        } catch (_: Exception) {
            when (code) {
                401 -> "HTTP 401 Unauthorized: Invalid API key or missing authentication."
                403 -> "HTTP 403 Forbidden: Endpoint access denied or quota exceeded."
                404 -> "HTTP 404 Not Found: Check Base URL ($baseUrl) or model name."
                429 -> "HTTP 429 Rate Limit Exceeded: Cooldown required."
                500, 502, 503, 504 -> "HTTP $code: Server error at $baseUrl."
                else -> "HTTP $code: $body"
            }
        }
    }
}
