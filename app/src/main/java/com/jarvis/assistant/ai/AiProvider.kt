package com.jarvis.assistant.ai

import com.jarvis.assistant.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

data class TokenUsage(
    val promptTokens: Int = 0,
    val candidateTokens: Int = 0,
    val totalTokens: Int = 0
)

data class AiResponse(
    val text: String,
    val model: String,
    val usage: TokenUsage? = null,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

data class AiStreamChunk(
    val textDelta: String,
    val isComplete: Boolean,
    val usage: TokenUsage? = null,
    val error: String? = null
)

data class ConnectionTestResult(
    val isSuccessful: Boolean,
    val latencyMs: Long,
    val message: String
)

interface AiProvider {
    val providerId: String
    val displayName: String

    suspend fun generateResponse(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        systemInstruction: String,
        model: String,
        apiKey: String
    ): Result<AiResponse>

    fun streamResponse(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        systemInstruction: String,
        model: String,
        apiKey: String
    ): Flow<AiStreamChunk>

    suspend fun testConnection(model: String, apiKey: String): Result<ConnectionTestResult>
}
