package com.jarvis.assistant

import com.jarvis.assistant.agent.AgentToolRegistry
import com.jarvis.assistant.ai.AiCapability
import com.jarvis.assistant.ai.ModelInfo
import com.jarvis.assistant.ai.PersonaPromptBuilder
import com.jarvis.assistant.automation.JarvisIntent
import com.jarvis.assistant.automation.JarvisIntentParser
import com.jarvis.assistant.data.model.AiPersonaConfig
import com.jarvis.assistant.data.model.AiProviderType
import com.jarvis.assistant.data.model.AssistantState
import com.jarvis.assistant.data.model.ChatMessage
import com.jarvis.assistant.data.model.ChatMode
import com.jarvis.assistant.data.model.MemoryCategory
import com.jarvis.assistant.data.model.MessageSender
import com.jarvis.assistant.data.model.NeuralMemory
import com.jarvis.assistant.data.model.PersonalityType
import com.jarvis.assistant.data.model.ProviderRole
import com.jarvis.assistant.data.model.TaskHudState
import com.jarvis.assistant.data.model.TaskStatus
import com.jarvis.assistant.data.model.ThemePreset
import com.jarvis.assistant.security.AuditStatus
import com.jarvis.assistant.security.PasswordStrengthReport
import com.jarvis.assistant.security.SecurityRiskGrade
import com.jarvis.assistant.terminal.AutonomousCommandPlanner
import com.jarvis.assistant.terminal.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.log2

class JarvisUnitTest {

    @Test
    fun testAssistantStates() {
        assertEquals("Offline", AssistantState.OFFLINE.label)
        assertEquals("Idle", AssistantState.IDLE.label)
        assertEquals("Listening", AssistantState.LISTENING.label)
        assertEquals("Thinking", AssistantState.THINKING.label)
        assertEquals("Processing", AssistantState.PROCESSING.label)
        assertEquals("Speaking", AssistantState.SPEAKING.label)
        assertEquals("Executing Task", AssistantState.EXECUTING_TASK.label)
        assertEquals("Completed", AssistantState.COMPLETED.label)
        assertEquals("Error", AssistantState.ERROR.label)
    }

    @Test
    fun testChatMessageCreation() {
        val msg = ChatMessage(
            id = "test-123",
            sender = MessageSender.USER,
            text = "Good morning Jarvis",
            mode = ChatMode.JARVIS
        )
        assertEquals("test-123", msg.id)
        assertEquals(MessageSender.USER, msg.sender)
        assertEquals("Good morning Jarvis", msg.text)
        assertEquals(ChatMode.JARVIS, msg.mode)
        assertFalse(msg.isCode)
    }

    @Test
    fun testTaskHudStateMachine() {
        val hud = TaskHudState(
            taskName = "WhatsApp Comms: Sarah",
            currentOperation = "Resolving contact alias...",
            completedSteps = listOf("Alias verified"),
            currentStepIndex = 2,
            totalSteps = 4,
            percentage = 50,
            status = TaskStatus.OBSERVING,
            needsVerification = true,
            isActive = true
        )
        assertEquals(2, hud.currentStepIndex)
        assertEquals(50, hud.percentage)
        assertTrue(hud.needsVerification)
        assertEquals(TaskStatus.OBSERVING, hud.status)
    }

    @Test
    fun testNeuralMemoryFiltering() {
        val memories = listOf(
            NeuralMemory("1", MemoryCategory.USER_PREFERENCES, "Hinglish Tone", "Master prefers Hinglish."),
            NeuralMemory("2", MemoryCategory.SAVED_FACTS, "Spotify Audio", "Master uses Spotify for music.")
        )
        val filtered = memories.filter { it.category == MemoryCategory.USER_PREFERENCES }
        assertEquals(1, filtered.size)
        assertEquals("Hinglish Tone", filtered[0].title)
    }

    @Test
    fun testThemePresets() {
        assertEquals("Sunset Glass", ThemePreset.SUNSET.displayName)
        assertEquals("Midnight Command", ThemePreset.MIDNIGHT_COMMAND.displayName)
        assertEquals("Dark Theme", ThemePreset.DARK.displayName)
        assertEquals("Light Theme", ThemePreset.LIGHT.displayName)
        assertEquals("AMOLED Black", ThemePreset.AMOLED.displayName)
    }

    @Test
    fun testAiProviderRolesAndTypes() {
        assertEquals("gemini", AiProviderType.GEMINI.id)
        assertEquals("openrouter", AiProviderType.OPENROUTER.id)
        assertEquals("qwen", AiProviderType.QWEN.id)
        assertEquals("custom", AiProviderType.CUSTOM.id)

        assertEquals("General AI", ProviderRole.GENERAL.label)
        assertEquals("Coding & Architecture", ProviderRole.CODING.label)
        assertEquals("Voice Conversational", ProviderRole.VOICE.label)
        assertEquals("Failover Backup", ProviderRole.BACKUP.label)
    }

    @Test
    fun testModelInfoDataStructure() {
        val model = ModelInfo(
            id = "meta-llama/llama-3.3-70b-instruct:free",
            name = "Llama 3.3 70B Free",
            contextLength = 128000,
            isFree = true,
            capabilities = setOf(AiCapability.GENERAL, AiCapability.FAST, AiCapability.FREE)
        )
        assertEquals("meta-llama/llama-3.3-70b-instruct:free", model.id)
        assertTrue(model.isFree)
        assertEquals(128000, model.contextLength)
        assertTrue(model.capabilities.contains(AiCapability.FREE))
    }

    @Test
    fun testTerminalRiskClassification() {
        val planner = AutonomousCommandPlanner(null)

        // Level 1: Safe read-only commands
        assertEquals(RiskLevel.LEVEL_1_SAFE, planner.classifyRisk("ls -la"))
        assertEquals(RiskLevel.LEVEL_1_SAFE, planner.classifyRisk("pwd"))
        assertEquals(RiskLevel.LEVEL_1_SAFE, planner.classifyRisk("python --version"))
        assertEquals(RiskLevel.LEVEL_1_SAFE, planner.classifyRisk("git status"))
        assertEquals(RiskLevel.LEVEL_1_SAFE, planner.classifyRisk("df -h"))

        // Level 2: Modification / Package installation
        assertEquals(RiskLevel.LEVEL_2_APPROVAL_REQUIRED, planner.classifyRisk("pkg install python -y"))
        assertEquals(RiskLevel.LEVEL_2_APPROVAL_REQUIRED, planner.classifyRisk("pip install requests"))
        assertEquals(RiskLevel.LEVEL_2_APPROVAL_REQUIRED, planner.classifyRisk("rm file.txt"))
        assertEquals(RiskLevel.LEVEL_2_APPROVAL_REQUIRED, planner.classifyRisk("git commit -m 'test'"))

        // Level 3: Dangerous system destruction
        assertEquals(RiskLevel.LEVEL_3_RESTRICTED, planner.classifyRisk("rm -rf /"))
        assertEquals(RiskLevel.LEVEL_3_RESTRICTED, planner.classifyRisk("mkfs.ext4 /dev/sda"))
        assertEquals(RiskLevel.LEVEL_3_RESTRICTED, planner.classifyRisk(":(){ :|:& };:"))
    }

    @Test
    fun testTerminalCommandSanitizer() {
        val planner = AutonomousCommandPlanner(null)

        // Dangerous pattern should fail sanitization
        val dangerous = planner.sanitizeCommand("rm -rf /")
        assertFalse(dangerous.isSuccess)

        // Safe command should succeed sanitization
        val safe = planner.sanitizeCommand("python -V")
        assertTrue(safe.isSuccess)
    }

    @Test
    fun testAutonomousNaturalLanguagePlanning() {
        val planner = AutonomousCommandPlanner(null)

        // Hindi/Hinglish Python setup
        val planPython = planner.planFromNaturalLanguage("Jarvis, Python environment setup karo")
        assertTrue(planPython.commands.isNotEmpty())
        assertTrue(planPython.commands.any { it.rawCommand.contains("python") })

        // Git status
        val planGit = planner.planFromNaturalLanguage("Mere project ka git status check karo")
        assertTrue(planGit.commands.any { it.rawCommand.contains("git status") })

        // Storage and RAM
        val planStorage = planner.planFromNaturalLanguage("Storage aur memory check karo")
        assertTrue(planStorage.commands.any { it.rawCommand.contains("df -h") })
    }

    @Test
    fun testAgentToolRegistry() {
        val tools = AgentToolRegistry.tools
        assertTrue(tools.isNotEmpty())
        assertTrue(tools.any { it.name == "terminal_execute" })
        assertTrue(tools.any { it.name == "device_open_app" })
        assertTrue(tools.any { it.name == "security_audit_device" })

        val openAiJson = AgentToolRegistry.toOpenAiToolsJson()
        assertNotNull(openAiJson)
        assertTrue(openAiJson.length() >= 5)
    }

    @Test
    fun testPasswordEntropyCalculation() {
        val shortWeak = "123456"
        val lower = shortWeak.lowercase()
        assertTrue(lower.contains("123456"))

        val strong = "Tr0ub4dor&349!Zx"
        var poolSize = 0
        if (strong.any { it.isLowerCase() }) poolSize += 26
        if (strong.any { it.isUpperCase() }) poolSize += 26
        if (strong.any { it.isDigit() }) poolSize += 10
        if (strong.any { !it.isLetterOrDigit() }) poolSize += 33

        val entropy = strong.length * log2(poolSize.toDouble())
        assertTrue(entropy > 80.0) // Highly robust entropy
    }

    @Test
    fun testSecurityModelsAndRiskGrades() {
        assertEquals("Secure / Hardened", SecurityRiskGrade.EXCELLENT.label)
        assertEquals("Critical Vulnerability", SecurityRiskGrade.CRITICAL.label)

        assertEquals(AuditStatus.PASSED, AuditStatus.valueOf("PASSED"))
        assertEquals(AuditStatus.WARNING, AuditStatus.valueOf("WARNING"))
        assertEquals(AuditStatus.FAILED, AuditStatus.valueOf("FAILED"))
    }

    @Test
    fun testIntentParserWhatsApp() {
        val intent1 = JarvisIntentParser.parse("Send a whatsapp message to Sarah saying meeting at 5")
        assertTrue(intent1 is JarvisIntent.CommunicationWhatsApp)
        val wa1 = intent1 as JarvisIntent.CommunicationWhatsApp
        assertEquals("Sarah", wa1.targetContact)

        val intentHinglish = JarvisIntentParser.parse("Sarah ko whatsapp pe message bhejo ki I will be late")
        assertTrue(intentHinglish is JarvisIntent.CommunicationWhatsApp)
        val wa2 = intentHinglish as JarvisIntent.CommunicationWhatsApp
        assertEquals("Sarah", wa2.targetContact)
    }

    @Test
    fun testIntentParserPhoneCall() {
        val intent1 = JarvisIntentParser.parse("Call Sarah right now")
        assertTrue(intent1 is JarvisIntent.CommunicationCall)
        val call1 = intent1 as JarvisIntent.CommunicationCall
        assertEquals("Sarah", call1.targetContact)

        val intentHindi = JarvisIntentParser.parse("Sarah ko phone milao")
        assertTrue(intentHindi is JarvisIntent.CommunicationCall)
        val call2 = intentHindi as JarvisIntent.CommunicationCall
        assertEquals("Sarah", call2.targetContact)
    }

    @Test
    fun testIntentParserYouTubeAndSpotify() {
        val ytIntent = JarvisIntentParser.parse("Search youtube for Jetpack Compose animations")
        assertTrue(ytIntent is JarvisIntent.YouTubeAction)
        val yt = ytIntent as JarvisIntent.YouTubeAction
        assertTrue(yt.query.contains("compose"))

        val spotIntent = JarvisIntentParser.parse("Play synthwave on spotify")
        assertTrue(spotIntent is JarvisIntent.SpotifyAction)
        val spot = spotIntent as JarvisIntent.SpotifyAction
        assertTrue(spot.trackOrArtist.contains("synthwave"))
    }

    @Test
    fun testIntentParserTelemetryAndWeather() {
        val batIntent = JarvisIntentParser.parse("Check battery status")
        assertTrue(batIntent is JarvisIntent.DeviceTelemetryAction)
        assertEquals("battery", (batIntent as JarvisIntent.DeviceTelemetryAction).target)

        val weatherIntent = JarvisIntentParser.parse("Aaj ka mausam kaisa hai")
        assertTrue(weatherIntent is JarvisIntent.WeatherQuery)
    }

    @Test
    fun testPersonaPromptBuilder() {
        val persona = AiPersonaConfig(
            assistantName = "JARVIS X",
            personalityType = PersonalityType.LOYAL_BUTLER,
            formalityLevel = 0.85f,
            responseStyle = "Concise & Loyal",
            customInstructions = "Always greet Master respectfully."
        )
        val memories = listOf(
            NeuralMemory("m1", MemoryCategory.USER_PREFERENCES, "Preferred Name", "Address user as Master."),
            NeuralMemory("m2", MemoryCategory.SAVED_FACTS, "Location", "Silicon Valley.")
        )
        val prompt = PersonaPromptBuilder.buildSystemInstruction("Master", persona, memories)
        assertTrue(prompt.contains("JARVIS X"))
        assertTrue(prompt.contains("British-style AI Butler"))
        assertTrue(prompt.contains("Formality Level: 85%"))
        assertTrue(prompt.contains("NEURAL CORE MEMORIES"))
        assertTrue(prompt.contains("Address user as Master."))
    }
}
