package com.jarvis.assistant.data.model

import kotlinx.serialization.Serializable

/**
 * 8 distinct states of the JARVIS AI Orb
 */
enum class AssistantState(val label: String) {
    OFFLINE("Offline"),
    IDLE("Idle"),
    LISTENING("Listening"),
    THINKING("Thinking"),
    PROCESSING("Processing"),
    SPEAKING("Speaking"),
    EXECUTING_TASK("Executing Task"),
    ERROR("Error")
}

enum class MessageSender {
    USER, JARVIS
}

enum class ChatMode(val displayName: String, val subtitle: String) {
    JARVIS("Jarvis", "General conversation & daily tasks"),
    CODE("Code", "Technical, syntax & algorithm mode")
}

@Serializable
data class ChatMessage(
    val id: String,
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: ChatMode = ChatMode.JARVIS,
    val isStreaming: Boolean = false,
    val isCode: Boolean = false
)

enum class MemoryCategory(val displayName: String) {
    ALL("All"),
    USER_PREFERENCES("User Preferences"),
    SAVED_FACTS("Saved Facts"),
    CONVERSATION_MEMORIES("Conversation"),
    SYSTEM_RULES("System Rules")
}

@Serializable
data class NeuralMemory(
    val id: String,
    val category: MemoryCategory,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class WeatherData(
    val temperature: String = "28°C",
    val condition: String = "Clear Sky",
    val location: String = "Atmosphere Core",
    val humidity: String = "45%",
    val windSpeed: String = "12 km/h",
    val isDay: Boolean = true,
    val lastUpdated: String = "Just now",
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SystemPermissionItem(
    val id: String,
    val name: String,
    val permission: String,
    val description: String,
    val isRequired: Boolean,
    val isGranted: Boolean,
    val iconType: String = "default"
)

enum class TaskStatus(val displayName: String) {
    PREPARING("Preparing"),
    OBSERVING("Observing"),
    EXECUTING("Executing"),
    VERIFYING("Verifying"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    WAITING_FOR_PERMISSION("Waiting for Permission"),
    PAUSED("Paused")
}

data class TaskHudState(
    val taskName: String = "",
    val currentOperation: String = "",
    val completedSteps: List<String> = emptyList(),
    val currentStepIndex: Int = 1,
    val totalSteps: Int = 4,
    val percentage: Int = 0,
    val status: TaskStatus = TaskStatus.PREPARING,
    val needsVerification: Boolean = false,
    val isActive: Boolean = false,
    val errorMessage: String? = null
)

@Serializable
data class AppUpdateItem(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val date: String,
    val statusBadge: String = "VERIFIED",
    val isRead: Boolean = false
)

enum class PersonalityType(val title: String, val tagline: String) {
    LOYAL_BUTLER("Loyal Butler", "Refined, formal, highly efficient Master protocol"),
    FRIENDLY_PEER("Friendly Peer", "Casual, warm, Hinglish conversationalist"),
    STRICT_CODER("Strict Coder", "Zero fluff, high-precision architectural solutions"),
    WITTY_COMPANION("Witty Companion", "Playful, energetic with intelligent banter")
}

@Serializable
data class AiPersonaConfig(
    val assistantName: String = "JARVIS",
    val personalityType: PersonalityType = PersonalityType.LOYAL_BUTLER,
    val customInstructions: String = "Respond respectfully to Master. Deliver concise, actionable insights.",
    val formalityLevel: Float = 0.85f,
    val responseStyle: String = "Concise & Actionable",
    val speakingSpeed: Float = 1.0f,
    val voiceName: String = "Charon",
    val greetingTemplate: String = "At your command, Master.",
    val activationPhrase: String = "Hey Jarvis"
)

enum class ThemePreset(val displayName: String) {
    SUNSET("Sunset Theme"),
    DARK("Dark Theme"),
    LIGHT("Light Theme"),
    AMOLED("AMOLED Black"),
    CUSTOM("Custom Neural")
}

enum class AvatarType(val displayName: String) {
    AVATAR_2D("2D Avatar"),
    ANIMATED_AVATAR("Animated Avatar"),
    MINIMAL_ORB("Minimal Orb"),
    COMPACT_ICON("Compact Icon")
}

enum class DockEdge(val displayName: String) {
    LEFT("Left Edge"),
    RIGHT("Right Edge"),
    TOP("Top"),
    BOTTOM("Bottom")
}

data class OverlayAvatarConfig(
    val isEnabled: Boolean = false,
    val avatarType: AvatarType = AvatarType.MINIMAL_ORB,
    val sizeDp: Int = 56,
    val opacity: Float = 0.9f,
    val dockEdge: DockEdge = DockEdge.RIGHT,
    val isWorkingOnly: Boolean = false
)
