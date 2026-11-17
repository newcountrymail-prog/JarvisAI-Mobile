package com.jarvis.assistant.ai.orchestration

/**
 * Task Intelligence Classifier.
 * Analyzes natural language prompts, code context, and operational intents
 * to categorize the user's workload into 14 distinct task categories,
 * and matches the task to the most capable discovered model.
 */
class TaskIntelligenceClassifier {

    /**
     * Classifies a prompt into one of 14 task categories based on linguistic and contextual indicators.
     */
    fun classifyTask(prompt: String, hasImage: Boolean = false): TaskCategory {
        if (hasImage) return TaskCategory.IMAGE_UNDERSTANDING

        val text = prompt.trim()
        val lower = text.lowercase()

        // 1. Android Development
        if (lower.contains("jetpack compose") || lower.contains("kotlin") || lower.contains("androidmanifest") ||
            lower.contains("gradle") || lower.contains("viewmodel") || lower.contains("coroutine") ||
            lower.contains("room database") || lower.contains("apk build") || lower.contains("termux")
        ) {
            return TaskCategory.ANDROID_DEVELOPMENT
        }

        // 2. Code Debugging
        if (lower.contains("stacktrace") || lower.contains("exception") || lower.contains("crash") ||
            lower.contains("fatal error") || lower.contains("syntaxerror") || lower.contains("nullpointer") ||
            lower.contains("bug fix") || lower.contains("fix this error") || lower.contains("traceback")
        ) {
            return TaskCategory.CODE_DEBUGGING
        }

        // 3. Programming / General Coding
        if (text.contains("```") || lower.contains("function") || lower.contains("class ") ||
            lower.contains("algorithm") || lower.contains("write code") || lower.contains("refactor") ||
            lower.contains("python") || lower.contains("javascript") || lower.contains("sql") ||
            lower.contains("implement") || lower.contains("api client")
        ) {
            return TaskCategory.PROGRAMMING
        }

        // 4. Terminal Assistance
        if (lower.contains("bash") || lower.contains("terminal") || lower.contains("shell") ||
            lower.contains("pkg install") || lower.contains("chmod") || lower.contains("curl") ||
            lower.contains("run command") || lower.contains("grep") || lower.contains("ls -la")
        ) {
            return TaskCategory.TERMINAL_ASSISTANCE
        }

        // 5. Tool Execution
        if (lower.contains("open app") || lower.contains("turn on") || lower.contains("set brightness") ||
            lower.contains("take screenshot") || lower.contains("call mcp") || lower.contains("device control")
        ) {
            return TaskCategory.TOOL_EXECUTION
        }

        // 6. Complex Reasoning & Mathematics
        if (lower.contains("reason step by step") || lower.contains("prove that") || lower.contains("calculate") ||
            lower.contains("logic puzzle") || lower.contains("mathematical") || lower.contains("theorem") ||
            lower.contains("chain of thought") || lower.contains("deduce")
        ) {
            return TaskCategory.COMPLEX_REASONING
        }

        // 7. Research & Synthesis
        if (lower.contains("research") || lower.contains("compare and contrast") || lower.contains("literature review") ||
            lower.contains("cite sources") || lower.contains("history of") || lower.contains("explain the background")
        ) {
            return TaskCategory.RESEARCH
        }

        // 8. Translation
        if (lower.contains("translate") || lower.contains("in hindi") || lower.contains("in spanish") ||
            lower.contains("french") || lower.contains("german") || lower.contains("english translation")
        ) {
            return TaskCategory.TRANSLATION
        }

        // 9. Data Analysis
        if (lower.contains("csv") || lower.contains("dataframe") || lower.contains("statistics") ||
            lower.contains("median") || lower.contains("standard deviation") || lower.contains("chart data") ||
            lower.contains("aggregate")
        ) {
            return TaskCategory.DATA_ANALYSIS
        }

        // 10. Long Context
        if (text.length > 8000 || lower.contains("entire file") || lower.contains("full repository") ||
            lower.contains("analyze this document")
        ) {
            return TaskCategory.LONG_CONTEXT
        }

        // 11. Document Analysis
        if (lower.contains("summarize") || lower.contains("executive summary") || lower.contains("key takeaways") ||
            lower.contains("read this text")
        ) {
            return TaskCategory.DOCUMENT_ANALYSIS
        }

        // 12. Creative Writing
        if (lower.contains("write a story") || lower.contains("poem") || lower.contains("script") ||
            lower.contains("creative") || lower.contains("dialogue") || lower.contains("metaphor")
        ) {
            return TaskCategory.CREATIVE_WRITING
        }

        // 13. Default
        return TaskCategory.GENERAL_CONVERSATION
    }

    /**
     * Determines required model capabilities for a task category.
     */
    fun requiredCapabilitiesFor(category: TaskCategory): Set<ModelCapability> {
        return when (category) {
            TaskCategory.PROGRAMMING,
            TaskCategory.CODE_DEBUGGING,
            TaskCategory.ANDROID_DEVELOPMENT -> setOf(ModelCapability.CODING, ModelCapability.STREAMING)

            TaskCategory.COMPLEX_REASONING -> setOf(ModelCapability.REASONING, ModelCapability.STREAMING)
            TaskCategory.IMAGE_UNDERSTANDING -> setOf(ModelCapability.VISION)
            TaskCategory.LONG_CONTEXT -> setOf(ModelCapability.LONG_CONTEXT)
            TaskCategory.TOOL_EXECUTION,
            TaskCategory.TERMINAL_ASSISTANCE -> setOf(ModelCapability.TOOL_CALLING, ModelCapability.CODING)

            else -> setOf(ModelCapability.GENERAL)
        }
    }

    /**
     * Scores a candidate model for a specific task category.
     * Higher score indicates a better fit.
     */
    fun scoreModelForTask(model: DiscoveredModel, category: TaskCategory): Int {
        var score = 50

        when (category) {
            TaskCategory.PROGRAMMING,
            TaskCategory.ANDROID_DEVELOPMENT,
            TaskCategory.CODE_DEBUGGING -> {
                score = model.codingSuitabilityScore
                if (model.capabilities.contains(ModelCapability.CODING.name)) score += 20
                if (model.modelIdentifier.contains("coder") || model.modelIdentifier.contains("qwen-2.5-coder")) score += 15
            }
            TaskCategory.COMPLEX_REASONING -> {
                score = model.reasoningSuitabilityScore
                if (model.capabilities.contains(ModelCapability.REASONING.name)) score += 25
                if (model.modelIdentifier.contains("r1") || model.modelIdentifier.contains("o1")) score += 20
            }
            TaskCategory.IMAGE_UNDERSTANDING -> {
                if (model.capabilities.contains(ModelCapability.VISION.name)) score += 50 else score -= 50
            }
            TaskCategory.LONG_CONTEXT -> {
                if (model.contextCapacity >= 128000) score += 40
                else if (model.contextCapacity >= 64000) score += 20
            }
            TaskCategory.TERMINAL_ASSISTANCE,
            TaskCategory.TOOL_EXECUTION -> {
                score = (model.codingSuitabilityScore + model.reasoningSuitabilityScore) / 2
                if (model.capabilities.contains(ModelCapability.TOOL_CALLING.name)) score += 20
            }
            else -> {
                // General conversation prefers fast, reliable models
                if (model.modelIdentifier.contains("flash") || model.modelIdentifier.contains("mini")) score += 20
            }
        }

        return score
    }

    /**
     * Selects the optimal model from a list of eligible candidates for a given task.
     */
    fun selectOptimalModel(
        eligibleModels: List<DiscoveredModel>,
        category: TaskCategory
    ): DiscoveredModel? {
        if (eligibleModels.isEmpty()) return null
        return eligibleModels.maxByOrNull { scoreModelForTask(it, category) }
    }
}
