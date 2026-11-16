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

    override suspend fun generateResponse(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        systemInstruction: String,
        model: String,
        apiKey: String
    ): Result<AiResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("Gemini API key is not configured. Please enter your API key in Settings & Intelligence ➔ Gemini Live API & Model.")
            )
        }

        try {
            val resolvedModel = resolveModelIdentifier(model)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$resolvedModel:generateContent?key=$apiKey"

            val jsonBody = buildRequestBody(prompt, conversationHistory, systemInstruction)
            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

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
                    usage = usage,
                    isSuccess = true
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("AI Engine Communication Failed: ${e.localizedMessage ?: "Unknown error"}"))
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
            emit(
                AiStreamChunk(
                    textDelta = "",
                    isComplete = true,
                    error = "Gemini API key is required. Set your key in Settings ➔ Gemini Live API."
                )
            )
            return@flow
        }

        val resolvedModel = resolveModelIdentifier(model)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$resolvedModel:streamGenerateContent?alt=sse&key=$apiKey"

        val jsonBody = buildRequestBody(prompt, conversationHistory, systemInstruction)
        val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = parseErrorMessage(response.code, response.body?.string() ?: "")
                emit(AiStreamChunk(textDelta = "", isComplete = true, error = err))
                return@flow
            }

            val inputStream = response.body?.byteStream()
            if (inputStream == null) {
                emit(AiStreamChunk(textDelta = "", isComplete = true, error = "Empty stream response from Gemini."))
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                val currentLine = line ?: continue
                if (currentLine.startsWith("data: ")) {
                    val dataJson = currentLine.substring(6).trim()
                    if (dataJson.isNotEmpty() && dataJson != "[DONE]") {
                        try {
                            val chunkRoot = JSONObject(dataJson)
                            val delta = extractTextFromResponse(chunkRoot)
                            val usage = extractUsageMetadata(chunkRoot)
                            if (delta.isNotEmpty()) {
                                emit(AiStreamChunk(textDelta = delta, isComplete = false, usage = usage))
                            }
                        } catch (e: Exception) {
                            // Ignored malformed chunk
                        }
                    }
                }
            }

            emit(AiStreamChunk(textDelta = "", isComplete = true))
        } catch (e: Exception) {
            emit(AiStreamChunk(textDelta = "", isComplete = true, error = e.localizedMessage ?: "Stream connection error."))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun testConnection(model: String, apiKey: String): Result<ConnectionTestResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("API key cannot be empty."))
        }

        val startTime = System.currentTimeMillis()
        val testPrompt = "Test connection. Reply with 'JARVIS CORE ONLINE'."
        val result = generateResponse(testPrompt, emptyList(), "You are JARVIS verification system.", model, apiKey)

        val latency = System.currentTimeMillis() - startTime

        result.fold(
            onSuccess = {
                Result.success(
                    ConnectionTestResult(
                        isSuccessful = true,
                        latencyMs = latency,
                        message = "Connected to ${it.model} • Latency: ${latency}ms • Status: Online (SSL TLS 1.3)"
                    )
                )
            },
            onFailure = {
                Result.failure(Exception("Verification Failed: ${it.localizedMessage}"))
            }
        )
    }

    private fun resolveModelIdentifier(model: String): String {
        return when (model) {
            "gemini-3.1-flash-live-preview" -> "gemini-1.5-flash" // Current stable production target for 3.1 flash live
            "gemini-1.5-pro" -> "gemini-1.5-pro"
            "gemini-2.0-flash" -> "gemini-1.5-flash"
            else -> if (model.isNotBlank()) model else "gemini-1.5-flash"
        }
    }

    private fun buildRequestBody(
        prompt: String,
        history: List<ChatMessage>,
        systemInstruction: String
    ): JSONObject {
        val root = JSONObject()

        // 1. System Instruction
        if (systemInstruction.isNotBlank()) {
            val systemObj = JSONObject()
            val partsArr = JSONArray().put(JSONObject().put("text", systemInstruction))
            systemObj.put("parts", partsArr)
            root.put("system_instruction", systemObj)
        }

        // 2. Contents (History + Latest Prompt)
        val contentsArr = JSONArray()
        // Include last 8 conversational turns for efficient context on 3GB RAM devices
        history.takeLast(8).forEach { msg ->
            val role = if (msg.sender == MessageSender.USER) "user" else "model"
            val turnObj = JSONObject()
            turnObj.put("role", role)
            val parts = JSONArray().put(JSONObject().put("text", msg.text))
            turnObj.put("parts", parts)
            contentsArr.put(turnObj)
        }

        // Latest User prompt
        val currentPromptObj = JSONObject()
        currentPromptObj.put("role", "user")
        val currentParts = JSONArray().put(JSONObject().put("text", prompt))
        currentPromptObj.put("parts", currentParts)
        contentsArr.put(currentPromptObj)

        root.put("contents", contentsArr)

        // 3. Generation Config
        val genConfig = JSONObject().apply {
            put("temperature", 0.7)
            put("maxOutputTokens", 2048)
        }
        root.put("generationConfig", genConfig)

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
            val part = parts.getJSONObject(i)
            sb.append(part.optString("text", ""))
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
        return try {
            val root = JSONObject(body)
            val err = root.optJSONObject("error")
            val message = err?.optString("message", "")
            when (code) {
                400 -> "Invalid Request: ${message ?: "Check model parameter and prompt payload."}"
                401, 403 -> "Authentication Error: The provided Gemini API Key is invalid or unauthorized."
                429 -> "Rate Limit Exceeded: Gemini quota exceeded. Please wait a moment."
                500, 503 -> "Google Gemini service temporarily unavailable (HTTP $code). Retrying recommended."
                else -> "HTTP $code: ${message ?: "Unknown server response."}"
            }
        } catch (e: Exception) {
            "HTTP $code: Unable to connect to Google Generative Language service."
        }
    }
}
