package com.jarvis.assistant.ai.gemini

enum class GeminiConnectionStatus(val label: String, val badge: String) {
    CONNECTED("GEMINI ONLINE", "ONLINE"),
    UNVERIFIED("CONNECTION NOT VERIFIED", "UNVERIFIED"),
    DISCONNECTED("GEMINI DISCONNECTED", "OFFLINE"),
    TESTING("PROBING CONNECTION...", "PROBING")
}

enum class ModelCapabilityCategory(val label: String) {
    GENERAL_CHAT("General Chat"),
    FAST_RESPONSE("Fast Response"),
    REASONING("Reasoning & Deep Thinking"),
    CODING("Coding & Architecture"),
    MULTIMODAL("Multimodal Vision/Audio"),
    VOICE_COMPATIBLE("Voice Low Latency")
}

enum class GeminiModelCompatibility(val label: String, val isUsable: Boolean) {
    VERIFIED_WORKING("Connection Successful", true),
    TESTING("Testing Compatibility...", false),
    CONNECTION_FAILED("Connection Failed", false),
    RESTRICTED("Restricted / Project Auth Required", false),
    UNSUPPORTED("Unsupported Generation Method", false),
    QUOTA_EXCEEDED("Quota Exceeded (HTTP 429)", false),
    AVAILABLE("Discovered / Untested", false)
}

data class GeminiDiscoveredModel(
    val id: String,
    val rawResourceName: String,
    val displayName: String,
    val description: String,
    val supportedGenerationMethods: List<String>,
    val inputTokenLimit: Int,
    val outputTokenLimit: Int,
    val category: ModelCapabilityCategory = ModelCapabilityCategory.GENERAL_CHAT,
    val compatibility: GeminiModelCompatibility = GeminiModelCompatibility.AVAILABLE,
    val latencyMs: Long = 0L,
    val lastTestedTime: Long = 0L,
    val lastErrorMessage: String? = null
)

data class GeminiErrorDiagnosis(
    val errorCategory: String,
    val httpStatusCode: Int?,
    val googleErrorMessage: String,
    val modelId: String,
    val endpoint: String,
    val suggestedCorrection: String,
    val isRecoverable: Boolean = false,
    val suggestedAlternativeModel: String? = null
)

data class GeminiConfigState(
    val apiKey: String = "",
    val maskedKey: String = "",
    val selectedModel: String = "gemini-1.5-flash",
    val apiVersion: String = "v1beta",
    val status: GeminiConnectionStatus = GeminiConnectionStatus.DISCONNECTED,
    val lastVerifiedTimestamp: Long = 0L,
    val lastMeasuredLatencyMs: Long = 0L,
    val discoveredModels: List<GeminiDiscoveredModel> = emptyList(),
    val lastErrorDiagnosis: GeminiErrorDiagnosis? = null
)
