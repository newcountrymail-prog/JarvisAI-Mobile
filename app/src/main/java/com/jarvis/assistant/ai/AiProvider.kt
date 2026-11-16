package com.jarvis.assistant.ai

import com.jarvis.assistant.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

enum class AiCapability(val label: String) {
    GENERAL("General AI"),
    CODING("Coding & Syntax"),
    REASONING("Deep Reasoning"),
    FAST("Fast & Lightweight"),
    LONG_CONTEXT("Long Context (128k+)"),
    MULTIMODAL("Vision & Multimodal"),
    STREAMING("Real-time Streaming"),
    FREE("Free Tier")
}

data class ModelInfo(
    val id: String,
    val name: String,
    val description: String = "",
    val contextLength: Int? = null,
    val isFree: Boolean = false,
    val capabilities: Set<AiCapability> = setOf(AiCapability.GENERAL, AiCapability.STREAMING),
    val pricingPrompt: String? = null,
    val pricingCompletion: String? = null
)

data class TokenUsage(
    val promptTokens: Int = 0,
    val candidateTokens: Int = 0,
    val totalTokens: Int = 0
)

data class AiResponse(
    val text: String,
    val model: String,
    val providerId: String = "unknown",
    val usage: TokenUsage? = null,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null,
    val latencyMs: Long = 0L
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
    val message: String,
    val activeModel: String = "",
    val discoveredModelsCount: Int = 0
)

interface AiProvider {
    val providerId: String
    val displayName: String
    val defaultModel: String
    val supportsModelListing: Boolean get() = true

    suspend fun listModels(apiKey: String): Result<List<ModelInfo>>

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
