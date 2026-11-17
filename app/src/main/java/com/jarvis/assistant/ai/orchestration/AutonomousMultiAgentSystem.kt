package com.jarvis.assistant.ai.orchestration

import com.jarvis.assistant.data.model.ChatMessage

/**
 * Autonomous Multi-Agent System for JARVIS X.
 * Coordinates 6 specialized AI subagents with distinct personas and prompts,
 * and orchestrates the CODER → REVIEWER → TESTER pipeline for high-assurance tasks.
 */
class AutonomousMultiAgentSystem(
    private val rotationEngine: AutonomousRotationEngine
) {

    /**
     * Agent instruction templates defining specialized cognitive profiles.
     */
    fun getAgentSystemPrompt(agent: AgentType): String {
        return when (agent) {
            AgentType.CORE ->
                "You are JARVIS CORE, the executive personal AI operating assistant for AntiGem and Android. Coordinate tasks, provide direct concise answers, and orchestrate subagents."

            AgentType.CODER ->
                "You are JARVIS CODER, an elite Principal Software Architect specializing in Android Kotlin, Jetpack Compose, Coroutines, Room, and clean architecture. Deliver production-ready code with clean structure and robust edge-case handling."

            AgentType.RESEARCHER ->
                "You are JARVIS RESEARCHER, a Senior Intelligence Analyst. Synthesize technical documentation, extract facts with high precision, compare alternatives, and cite references."

            AgentType.REASONER ->
                "You are JARVIS REASONER, an Advanced Problem Solving and Logic Engine. Break down complex challenges step-by-step, evaluate mathematical and algorithmic constraints, and formulate structured action plans."

            AgentType.TOOL_OPERATOR ->
                "You are JARVIS TOOL OPERATOR, the Terminal & Device Automation Specialist. Formulate precise Termux shell commands, call MCP tools, inspect Android device state, and verify execution logs."

            AgentType.REVIEWER ->
                "You are JARVIS REVIEWER, an Independent Quality Assurance & Security Auditor. Critically review proposed code and plans. Verify memory safety, recomposition overhead, permission security, edge-case bugs, and compliance."
        }
    }

    /**
     * Executes a multi-agent coding workflow: CODER produces solution -> REVIEWER audits and refines it.
     */
    suspend fun executeCoderReviewerPipeline(
        prompt: String,
        conversationHistory: List<ChatMessage>,
        providers: List<ProviderCredential>,
        discoveredModels: List<DiscoveredModel>,
        freeModeOnly: Boolean,
        autoSwitchingEnabled: Boolean
    ): OrchestrationExecutionResult {
        // Step 1: Execute with JARVIS CODER
        val coderPrompt = "Task: $prompt\n\nPlease provide complete, robust implementation code with explanations."
        val coderResult = rotationEngine.executeWithAutoRotation(
            prompt = coderPrompt,
            conversationHistory = conversationHistory,
            systemInstruction = getAgentSystemPrompt(AgentType.CODER),
            providers = providers,
            discoveredModels = discoveredModels,
            primaryModelId = "qwen/qwen-2.5-coder-32b-instruct",
            freeModeOnly = freeModeOnly,
            autoSwitchingEnabled = autoSwitchingEnabled
        )

        if (!coderResult.isSuccess) return coderResult

        // Step 2: Execute with JARVIS REVIEWER to audit the proposed code
        val reviewerPrompt = """
            Original Task: $prompt
            
            Proposed Code by JARVIS CODER:
            ${coderResult.responseText}
            
            Perform an independent review:
            1. Verify edge-case resilience, null-safety, and potential crashes.
            2. Identify any performance or memory leaks.
            3. Provide the verified, perfected final code with audit notes.
        """.trimIndent()

        val reviewerResult = rotationEngine.executeWithAutoRotation(
            prompt = reviewerPrompt,
            conversationHistory = emptyList(),
            systemInstruction = getAgentSystemPrompt(AgentType.REVIEWER),
            providers = providers,
            discoveredModels = discoveredModels,
            primaryModelId = "deepseek/deepseek-r1:free",
            freeModeOnly = freeModeOnly,
            autoSwitchingEnabled = autoSwitchingEnabled
        )

        if (!reviewerResult.isSuccess) {
            // If reviewer fails, return coder output with note
            return coderResult.copy(
                responseText = "[JARVIS CODER Output (Reviewer unavailable)]\n\n" + coderResult.responseText
            )
        }

        val combinedText = "[CODER → REVIEWER VERIFIED PIPELINE]\n\n${reviewerResult.responseText}"
        return reviewerResult.copy(
            responseText = combinedText,
            promptTokens = coderResult.promptTokens + reviewerResult.promptTokens,
            completionTokens = coderResult.completionTokens + reviewerResult.completionTokens,
            latencyMs = coderResult.latencyMs + reviewerResult.latencyMs
        )
    }
}
