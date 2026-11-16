package com.jarvis.assistant.ai

import com.jarvis.assistant.data.model.AiPersonaConfig
import com.jarvis.assistant.data.model.NeuralMemory
import com.jarvis.assistant.data.model.PersonalityType

object PersonaPromptBuilder {

    fun buildSystemInstruction(
        userName: String,
        persona: AiPersonaConfig,
        memories: List<NeuralMemory>
    ): String {
        val personalityGuidance = when (persona.personalityType) {
            PersonalityType.LOYAL_BUTLER -> "You are a refined, highly efficient British-style AI Butler. Address the user as '$userName' with unwavering loyalty and razor-sharp intellect."
            PersonalityType.FRIENDLY_PEER -> "You are a warm, supportive peer and collaborator. You speak naturally in Hinglish/English with friendly camaraderie."
            PersonalityType.STRICT_CODER -> "You are a senior software architect. Zero fluff, high precision, deliver modular and clean architectural code patterns."
            PersonalityType.WITTY_COMPANION -> "You are a witty, charming, and sharp conversational companion with playful banter while staying extremely competent."
        }

        val memoryContext = if (memories.isNotEmpty()) {
            val facts = memories.take(6).joinToString("\n- ") { "${it.title}: ${it.content}" }
            "\n### NEURAL CORE MEMORIES (GROUND TRUTH):\n- $facts"
        } else ""

        return """
You are ${persona.assistantName}, the primary AI operator for $userName.
$personalityGuidance

${persona.customInstructions}

Operating Rules:
1. Always align tone with Formality Level: ${(persona.formalityLevel * 100).toInt()}%.
2. Desired Response Style: ${persona.responseStyle}.
3. Support natural conversation in English, Hindi, and Hinglish.
$memoryContext
        """.trimIndent()
    }
}
