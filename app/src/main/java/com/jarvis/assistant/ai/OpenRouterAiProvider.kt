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

class OpenRouterAiProvider(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()
) : AiProvider {

    override val providerId: String = "openrouter"
    override val displayName: String = "OpenRouter (Unified LLM)"
    override val defaultModel: String = "meta-llama/llama-3.3-70b-instruct:free"

    override suspend fun listModels(apiKey: String): Result<List<ModelInfo>> = withContext(Dispatchers.IO) {
        try {
            val url = "https://openrouter.ai/api/v1/models"
            val requestBuilder = Request.Builder().url(url).get()
            if (apiKey.isNotBlank()) {
                requestBuilder.header("Authorization", "Bearer $apiKey")
            }
            requestBuilder.header("HTTP-Referer", "https://jarvis.assistant.ai")
            requestBuilder.header("X-Title", "JARVIS X Personal AI")

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.success(getCuratedDefaultModels())
            }

            val root = JSONObject(bodyString)
            val dataArray = root.optJSONArray("data") ?: JSONArray()
            val list = mutableListOf<ModelInfo>()

            for (i in 0 until dataArray.length()) {
                val item = dataArray.getJSONObject(i)
                val id = item.optString("id", "")
                val name = item.optString("name", id)
                val desc = item.optString("description", "")
                val contextLen = item.optInt("context_length", 32768)
                val pricing = item.optJSONObject("pricing")
                val promptPrice = pricing?.optString("prompt", "0")
                val completionPrice = pricing?.optString("completion", "0")
                val isFree = id.contains(":free") || (promptPrice == "0" && completionPrice == "0")

                val caps = mutableSetOf(AiCapability.GENERAL, AiCapability.STREAMING)
                if (isFree) caps.add(AiCapability.FREE)
                if (contextLen >= 128000) caps.add(AiCapability.LONG_CONTEXT)
                if (id.contains("coder") || id.contains("code") || id.contains("qwen")) caps.add(AiCapability.CODING)
                if (id.contains("r1") || id.contains("reason") || id.contains("o1") || id.contains("70b")) caps.add(AiCapability.REASONING)
                if (id.contains("8b") || id.contains("mini") || id.contains("flash")) caps.add(AiCapability.FAST)

                list.add(
                    ModelInfo(
                        id = id,
                        name = name,
                        description = desc.take(150),
                        contextLength = contextLen,
                        isFree = isFree,
                        capabilities = caps,
                        pricingPrompt = promptPrice,
                        pricingCompletion = completionPrice
                    )
                )
            }

            if (list.isEmpty()) Result.success(getCuratedDefaultModels()) else Result.success(list.take(100))
        } catch (e: Exception) {
            Result.success(getCuratedDefaultModels())
        }
    }

    private fun getCuratedDefaultModels(): List<ModelInfo> {
        return listOf(
            ModelInfo(
                id = "meta-llama/llama-3.3-70b-instruct:free",
                name = "Llama 3.3 70B Instruct (Free)",
                description = "High-performance open weights model, exceptional reasoning & speed.",
                contextLength = 131072,
                isFree = true,
                capabilities = setOf(AiCapability.GENERAL, AiCapability.REASONING, AiCapability.FREE, AiCapability.LONG_CONTEXT)
            ),
            ModelInfo(
                id = "deepseek/deepseek-r1:free",
                name = "DeepSeek R1 Reasoning (Free)",
                description = "State-of-the-art open reasoning & chain-of-thought architecture.",
                contextLength = 65536,
                isFree = true,
                capabilities = setOf(AiCapability.GENERAL, AiCapability.REASONING, AiCapability.CODING, AiCapability.FREE)
            ),
            ModelInfo(
                id = "qwen/qwen-2.5-coder-32b-instruct",
                name = "Qwen 2.5 Coder 32B",
                description = "Benchmark-leading coding intelligence for Android & Kotlin.",
                contextLength = 32768,
                isFree = false,
                capabilities = setOf(AiCapability.CODING, AiCapability.GENERAL)
            ),
            ModelInfo(
                id = "mistralai/mistral-7b-instruct:free",
                name = "Mistral 7B Instruct (Free)",
                description = "Fast, lightweight assistant model with low latency.",
                contextLength = 32768,
                isFree = true,
                capabilities = setOf(AiCapability.GENERAL, AiCapability.FAST, AiCapability.FREE)
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
            return@withContext Result.failure(IllegalStateException("OpenRouter API key is required."))
        }

        try {
            val url = "https://openrouter.ai/api/v1/chat/completions"
            val targetModel = model.ifBlank { defaultModel }
            val jsonBody = buildChatRequestBody(prompt, conversationHistory, systemInstruction, targetModel, stream = false)
            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $apiKey")
                .header("HTTP-Referer", "https://jarvis.assistant.ai")
                .header("X-Title", "JARVIS X Personal AI")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val err = parseOpenRouterError(response.code, responseString)
                return@withContext Result.failure(Exception(err))
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
            Result.failure(Exception("OpenRouter request failed: ${e.localizedMessage ?: "Unknown error"}"))
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
            emit(AiStreamChunk(textDelta = "", isComplete = true, error = "OpenRouter API key is required."))
            return@flow
        }

        val url = "https://openrouter.ai/api/v1/chat/completions"
        val targetModel = model.ifBlank { defaultModel }
        val jsonBody = buildChatRequestBody(prompt, conversationHistory, systemInstruction, targetModel, stream = true)
        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://jarvis.assistant.ai")
            .header("X-Title", "JARVIS X Personal AI")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = parseOpenRouterError(response.code, response.body?.string() ?: "")
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
                    val dataContent = currentLine.removePrefix("data: ").trim()
                    if (dataContent == "[DONE]") {
                        break
                    }
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
            val modelsResult = listModels(apiKey)
            val discoveredCount = modelsResult.getOrNull()?.size ?: 0
            val targetModel = model.ifBlank { defaultModel }

            val testPayload = JSONObject().apply {
                put("model", targetModel)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "JARVIS connection ping. Reply 'READY'.")
                    })
                })
                put("max_tokens", 10)
            }

            val requestBody = testPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
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
                        message = "HTTP 200 OK — OpenRouter API Connected ($targetModel).",
                        activeModel = targetModel,
                        discoveredModelsCount = discoveredCount
                    )
                )
            } else {
                val err = parseOpenRouterError(response.code, response.body?.string() ?: "")
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(Exception("OpenRouter connection failed: ${e.localizedMessage ?: "Network error"}"))
        }
    }

    private fun buildChatRequestBody(
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
        root.put("temperature", 0.7)
        return root
    }

    private fun parseOpenRouterError(code: Int, body: String): String {
        val serverMsg = try {
            JSONObject(body).optJSONObject("error")?.optString("message", "") ?: ""
        } catch (_: Exception) { "" }

        val detail = if (serverMsg.isNotBlank()) " Detail: $serverMsg" else ""
        return when (code) {
            401 -> "HTTP 401 Unauthorized: Invalid OpenRouter API key.$detail"
            402 -> "HTTP 402 Insufficient Balance: OpenRouter credits exhausted or paid model selected without credits.$detail"
            403 -> "HTTP 403 Forbidden: Access restricted.$detail"
            429 -> "HTTP 429 Rate Limit: OpenRouter request quota reached.$detail"
            500, 502, 503 -> "HTTP $code Upstream Model Provider Unavailable.$detail"
            else -> "HTTP $code Error.$detail"
        }
    }
}
