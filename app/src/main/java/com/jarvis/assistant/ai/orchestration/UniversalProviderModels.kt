package com.jarvis.assistant.ai.orchestration

import kotlinx.serialization.Serializable

/**
 * Supported provider presets in JARVIS Universal AI Orchestration.
 */
enum class ProviderPreset(
    val id: String,
    val displayName: String,
    val defaultEndpoint: String,
    val defaultModel: String,
    val authHeaderType: AuthHeaderType,
    val isOfficialPreset: Boolean = true
) {
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        defaultEndpoint = "https://generativelanguage.googleapis.com",
        defaultModel = "gemini-1.5-flash",
        authHeaderType = AuthHeaderType.QUERY_OR_GOOG_HEADER
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter (Unified LLM)",
        defaultEndpoint = "https://openrouter.ai/api/v1",
        defaultModel = "meta-llama/llama-3.3-70b-instruct:free",
        authHeaderType = AuthHeaderType.BEARER
    ),
    QWEN(
        id = "qwen",
        displayName = "Qwen Coder AI (Alibaba)",
        defaultEndpoint = "https://dashscope-intl.aliyuncs.com/compatible-mode/v1",
        defaultModel = "qwen2.5-coder-32b-instruct",
        authHeaderType = AuthHeaderType.BEARER
    ),
    DEEPSEEK(
        id = "deepseek",
        displayName = "DeepSeek AI",
        defaultEndpoint = "https://api.deepseek.com/v1",
        defaultModel = "deepseek-chat",
        authHeaderType = AuthHeaderType.BEARER
    ),
    GROQ(
        id = "groq",
        displayName = "Groq LPU (Ultra Fast)",
        defaultEndpoint = "https://api.groq.com/openai/v1",
        defaultModel = "llama-3.3-70b-versatile",
        authHeaderType = AuthHeaderType.BEARER
    ),
    MISTRAL(
        id = "mistral",
        displayName = "Mistral AI",
        defaultEndpoint = "https://api.mistral.ai/v1",
        defaultModel = "mistral-small-latest",
        authHeaderType = AuthHeaderType.BEARER
    ),
    OLLAMA(
        id = "ollama",
        displayName = "Ollama Local / Network",
        defaultEndpoint = "http://127.0.0.1:11434",
        defaultModel = "llama3.2:latest",
        authHeaderType = AuthHeaderType.NONE
    ),
    OPENAI(
        id = "openai",
        displayName = "OpenAI Direct",
        defaultEndpoint = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        authHeaderType = AuthHeaderType.BEARER
    ),
    ANTHROPIC(
        id = "anthropic",
        displayName = "Anthropic Claude",
        defaultEndpoint = "https://api.anthropic.com/v1",
        defaultModel = "claude-3-5-sonnet-20241022",
        authHeaderType = AuthHeaderType.X_API_KEY
    ),
    CUSTOM(
        id = "custom",
        displayName = "Custom OpenAI Compatible",
        defaultEndpoint = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        authHeaderType = AuthHeaderType.BEARER,
        isOfficialPreset = false
    )
}

enum class AuthHeaderType {
    BEARER,
    X_API_KEY,
    QUERY_OR_GOOG_HEADER,
    NONE
}

/**
 * Pricing tier classification for dynamic model discovery.
 */
enum class FreeTierCategory(val label: String, val badgeColorHex: String) {
    CATEGORY_A_VERIFIED_FREE("Verified Free", "#28AE86"),
    CATEGORY_B_FREE_TIER_QUOTA("Free-Tier Limited Quota", "#8295AD"),
    CATEGORY_C_TRIAL("Trial / Promotional", "#F16D43"),
    CATEGORY_D_PAID("Paid Access", "#A66C91"),
    CATEGORY_E_UNKNOWN("Pricing Unknown", "#718096")
}

/**
 * Functional capabilities of discovered AI models.
 */
enum class ModelCapability(val label: String) {
    GENERAL("General Chat"),
    CODING("Code & Syntax"),
    REASONING("Deep Reasoning"),
    VISION("Vision & Image Input"),
    STREAMING("Streaming SSE"),
    TOOL_CALLING("Tool Calling"),
    LONG_CONTEXT("Long Context (>64k)"),
    FREE("Zero Cost")
}

/**
 * Task classification categories for intelligent request routing.
 */
enum class TaskCategory(val displayName: String, val description: String) {
    GENERAL_CONVERSATION("General Conversation", "Casual dialogue, assistance, general Q&A"),
    PROGRAMMING("Programming", "Software engineering, algorithms, script writing"),
    CODE_DEBUGGING("Code Debugging", "Error stack trace triage, bug patching"),
    ANDROID_DEVELOPMENT("Android Development", "Kotlin, Jetpack Compose, Gradle, Android SDK"),
    COMPLEX_REASONING("Complex Reasoning", "Multi-step logic, mathematics, architectural trade-offs"),
    RESEARCH("Research", "Literature synthesis, fact verification, topic investigation"),
    DOCUMENT_ANALYSIS("Document Analysis", "Long document comprehension, summarization"),
    IMAGE_UNDERSTANDING("Image Understanding", "Visual inspection, screenshot analysis, OCR"),
    CREATIVE_WRITING("Creative Writing", "Storytelling, copy generation, tone adaptation"),
    TRANSLATION("Translation", "Cross-language translation, idiom localization"),
    DATA_ANALYSIS("Data Analysis", "Numerical analysis, statistics, tabular data formatting"),
    LONG_CONTEXT("Long Context Analysis", "Multi-file repositories, book-length inputs"),
    TOOL_EXECUTION("Tool Execution", "Calling device automation, MCP APIs, shell functions"),
    TERMINAL_ASSISTANCE("Terminal Assistance", "Termux commands, Linux administration, shell scripting")
}

/**
 * Specialized autonomous agents coordinated by JARVIS.
 */
enum class AgentType(val displayName: String, val roleDescription: String) {
    CORE("JARVIS CORE", "General coordination, conversational interface, orchestrator"),
    CODER("JARVIS CODER", "Software engineering, Kotlin/Compose development, bug fixing"),
    RESEARCHER("JARVIS RESEARCHER", "Information synthesis, documentation search, deep research"),
    REASONER("JARVIS REASONER", "Logical problem solving, mathematical analysis, planning"),
    TOOL_OPERATOR("JARVIS TOOL OPERATOR", "Termux terminal, MCP tool execution, device automation"),
    REVIEWER("JARVIS REVIEWER", "Independent code review, validation, error identification")
}

/**
 * A user-configured AI Provider credential entry.
 */
@Serializable
data class ProviderCredential(
    val id: String,
    val providerPreset: String,
    val displayName: String,
    val apiKey: String,
    val baseUrl: String,
    val authType: String = AuthHeaderType.BEARER.name,
    val isEnabled: Boolean = true,
    val maskedKey: String = "",
    val lastTestedTimestamp: Long = 0L,
    val isHealthy: Boolean = false,
    val lastLatencyMs: Long = 0L,
    val lastErrorMessage: String? = null,
    val discoveredModelsCount: Int = 0
)

/**
 * A discovered AI model with its capabilities and pricing classification.
 */
@Serializable
data class DiscoveredModel(
    val id: String,
    val providerId: String,
    val modelIdentifier: String,
    val displayName: String,
    val description: String = "",
    val contextCapacity: Int = 32768,
    val freeCategory: String = FreeTierCategory.CATEGORY_E_UNKNOWN.name,
    val capabilities: List<String> = listOf(ModelCapability.GENERAL.name, ModelCapability.STREAMING.name),
    val codingSuitabilityScore: Int = 50,
    val reasoningSuitabilityScore: Int = 50,
    val pricingPrompt: String? = null,
    val pricingCompletion: String? = null,
    val isAvailable: Boolean = true,
    val lastTestedSuccess: Boolean = false,
    val lastLatencyMs: Long = 0L
)

/**
 * Rotation and failover event recorded when the engine switches providers.
 */
@Serializable
data class RotationEvent(
    val id: String,
    val timestamp: Long,
    val fromProviderId: String,
    val fromModelId: String,
    val toProviderId: String,
    val toModelId: String,
    val reason: String,
    val success: Boolean,
    val latencyMs: Long
)

/**
 * Ledger entry tracking actual tokens, requests, and cost estimates.
 */
@Serializable
data class UsageLedgerEntry(
    val id: String,
    val timestamp: Long,
    val providerId: String,
    val modelId: String,
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val latencyMs: Long,
    val rateLimitRemaining: String = "QUOTA UNKNOWN",
    val isFree: Boolean = true,
    val estimatedCostUsd: Double = 0.0
)

/**
 * Overall state of the Universal AI Orchestration system.
 */
data class OrchestrationState(
    val isAutoModelSelection: Boolean = true,
    val isAutoProviderSwitching: Boolean = true,
    val isFreeModeOnly: Boolean = false,
    val isAutoFreeModelDiscovery: Boolean = true,
    val isCodingModelOptimization: Boolean = true,
    val isMultiAgentReview: Boolean = false,
    val isPaidModelAccess: Boolean = true,
    val maxSpendingLimitUsd: Double = 10.0,
    val maxRetryCount: Int = 3,
    val primaryProviderId: String = "gemini",
    val backupProviderId: String = "openrouter",
    val activeModelId: String = "gemini-1.5-flash",
    val activeRoutingStatus: String = "READY • AUTO ROUTING ACTIVE",
    val connectedProvidersCount: Int = 0,
    val availableModelsCount: Int = 0,
    val verifiedFreeModelsCount: Int = 0,
    val providersNearLimitCount: Int = 0,
    val providersExhaustedCount: Int = 0,
    val totalRequestsCount: Long = 0L,
    val totalTokensConsumed: Long = 0L
)
