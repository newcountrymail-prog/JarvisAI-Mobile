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

    @Test
    fun testToolDiscoveryRegistryConstructors() {
        val reg1 = com.jarvis.assistant.terminal.ToolDiscoveryRegistry()
        assertTrue(reg1.getAllTools().isNotEmpty())
        val reg2 = com.jarvis.assistant.terminal.ToolDiscoveryRegistry(null, null)
        assertTrue(reg2.getAllTools().isNotEmpty())
    }

    @Test
    fun testGeminiKeyValidator() {
        val raw1 = "  AIzaSyD-1234567890abcdefABCDEF1234  \n"
        assertEquals("AIzaSyD-1234567890abcdefABCDEF1234", com.jarvis.assistant.ai.gemini.GeminiKeyValidator.cleanKey(raw1))

        val rawWithDoubleQuotes = "\"AIzaSyD-1234567890abcdefABCDEF1234\""
        assertEquals("AIzaSyD-1234567890abcdefABCDEF1234", com.jarvis.assistant.ai.gemini.GeminiKeyValidator.cleanKey(rawWithDoubleQuotes))

        val rawWithSingleQuotes = "'AIzaSyD-1234567890abcdefABCDEF1234'"
        assertEquals("AIzaSyD-1234567890abcdefABCDEF1234", com.jarvis.assistant.ai.gemini.GeminiKeyValidator.cleanKey(rawWithSingleQuotes))

        val rawWithSmartQuotes = "“AIzaSyD-1234567890abcdefABCDEF1234”"
        assertEquals("AIzaSyD-1234567890abcdefABCDEF1234", com.jarvis.assistant.ai.gemini.GeminiKeyValidator.cleanKey(rawWithSmartQuotes))

        val masked = com.jarvis.assistant.ai.gemini.GeminiKeyValidator.maskKey("AIzaSyD-1234567890abcdefABCDEF1234")
        assertTrue(masked.startsWith("AIzaSy..."))
        assertTrue(masked.endsWith("1234"))
        assertFalse(masked.contains("abcdef"))

        val redacted = com.jarvis.assistant.ai.gemini.GeminiKeyValidator.redactString(
            "Connecting with key AIzaSyD-1234567890abcdefABCDEF1234 to endpoint",
            "AIzaSyD-1234567890abcdefABCDEF1234"
        )
        assertFalse(redacted.contains("AIzaSyD-1234567890abcdefABCDEF1234"))
        assertTrue(redacted.contains("AIzaSy...1234"))

        assertTrue(com.jarvis.assistant.ai.gemini.GeminiKeyValidator.validateKeyFormat("AIzaSyD-1234567890abcdefABCDEF1234"))
        assertFalse(com.jarvis.assistant.ai.gemini.GeminiKeyValidator.validateKeyFormat("short_key"))
    }

    @Test
    fun testGeminiErrorDiagnoser() {
        val err404 = com.jarvis.assistant.ai.gemini.GeminiErrorDiagnoser.diagnoseHttpError(
            code = 404,
            responseBody = "{\"error\":{\"message\":\"models/gemini-3.1-flash-live-preview is not found\"}}",
            modelId = "gemini-3.1-flash-live-preview",
            endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-live-preview:generateContent"
        )
        assertEquals("MODEL_NOT_FOUND", err404.errorCategory)
        assertTrue(err404.isRecoverable)
        assertNotNull(err404.suggestedAlternativeModel)

        val err403 = com.jarvis.assistant.ai.gemini.GeminiErrorDiagnoser.diagnoseHttpError(
            code = 403,
            responseBody = "{\"error\":{\"message\":\"Generative Language API has not been used in project 123 before or it is disabled.\"}}",
            modelId = "gemini-1.5-flash",
            endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"
        )
        assertEquals("API_NOT_ENABLED", err403.errorCategory)
        assertFalse(err403.isRecoverable)
        assertTrue(err403.suggestedCorrection.contains("Google Cloud Console"))

        val err429 = com.jarvis.assistant.ai.gemini.GeminiErrorDiagnoser.diagnoseHttpError(
            code = 429,
            responseBody = "{\"error\":{\"message\":\"Resource has been exhausted (e.g. check quota).\"}}",
            modelId = "gemini-1.5-flash",
            endpoint = "generateContent"
        )
        assertEquals("RATE_LIMIT_EXCEEDED", err429.errorCategory)

        val dnsEx = com.jarvis.assistant.ai.gemini.GeminiErrorDiagnoser.diagnoseException(
            java.net.UnknownHostException("generativelanguage.googleapis.com"),
            "gemini-1.5-flash",
            "https://generativelanguage.googleapis.com"
        )
        assertEquals("DNS_OR_OFFLINE", dnsEx.errorCategory)
        assertTrue(dnsEx.isRecoverable)
    }

    @Test
    fun testGeminiModelSelectionAndResolution() {
        val provider = com.jarvis.assistant.ai.GeminiAiProvider()
        assertEquals("gemini-1.5-flash", provider.resolveModelIdentifier("gemini-3.1-flash-live-preview"))
        assertEquals("gemini-1.5-flash", provider.resolveModelIdentifier(""))
        assertEquals("gemini-2.0-flash", provider.resolveModelIdentifier("models/gemini-2.0-flash"))

        val model1 = com.jarvis.assistant.ai.gemini.GeminiDiscoveredModel(
            id = "gemini-1.5-pro",
            rawResourceName = "models/gemini-1.5-pro",
            displayName = "Gemini 1.5 Pro",
            description = "Pro",
            supportedGenerationMethods = listOf("generateContent"),
            inputTokenLimit = 2000000,
            outputTokenLimit = 8192,
            category = com.jarvis.assistant.ai.gemini.ModelCapabilityCategory.REASONING,
            compatibility = com.jarvis.assistant.ai.gemini.GeminiModelCompatibility.VERIFIED_WORKING,
            latencyMs = 1200L
        )
        val model2 = com.jarvis.assistant.ai.gemini.GeminiDiscoveredModel(
            id = "gemini-1.5-flash",
            rawResourceName = "models/gemini-1.5-flash",
            displayName = "Gemini 1.5 Flash",
            description = "Flash",
            supportedGenerationMethods = listOf("generateContent"),
            inputTokenLimit = 1000000,
            outputTokenLimit = 8192,
            category = com.jarvis.assistant.ai.gemini.ModelCapabilityCategory.FAST_RESPONSE,
            compatibility = com.jarvis.assistant.ai.gemini.GeminiModelCompatibility.VERIFIED_WORKING,
            latencyMs = 450L
        )

        val verifiedList = listOf(model1, model2)
        val best = com.jarvis.assistant.ai.gemini.GeminiAutoConfigEngine.selectBestModel(verifiedList, listOf(model1, model2))
        assertEquals("gemini-1.5-flash", best.id)
        assertEquals(com.jarvis.assistant.ai.gemini.ModelCapabilityCategory.FAST_RESPONSE, best.category)
    }

    // =========================================================================
    // UNIVERSAL AI ORCHESTRATION UNIT TESTS
    // =========================================================================

    @Test
    fun testOrchestrationProviderPresetsAndDetection() {
        val engine = com.jarvis.assistant.ai.orchestration.ProviderDiscoveryEngine()

        // OpenRouter detection
        assertEquals(
            com.jarvis.assistant.ai.orchestration.ProviderPreset.OPENROUTER,
            engine.detectProvider("sk-or-v1-00000000000000000000000000000000", "https://openrouter.ai/api/v1")
        )

        // Gemini detection
        assertEquals(
            com.jarvis.assistant.ai.orchestration.ProviderPreset.GEMINI,
            engine.detectProvider("AIzaSyDummyTestKey0000000000000000000", "https://generativelanguage.googleapis.com")
        )

        // OpenAI detection
        assertEquals(
            com.jarvis.assistant.ai.orchestration.ProviderPreset.OPENAI,
            engine.detectProvider("sk-proj-dummyOpenAiKey00000000000000000", "https://api.openai.com/v1")
        )

        // Groq detection
        assertEquals(
            com.jarvis.assistant.ai.orchestration.ProviderPreset.GROQ,
            engine.detectProvider("gsk_dummyGroqKey000000000000000000000", "https://api.groq.com/openai/v1")
        )

        // Ollama detection
        assertEquals(
            com.jarvis.assistant.ai.orchestration.ProviderPreset.OLLAMA,
            engine.detectProvider("", "http://127.0.0.1:11434")
        )
    }

    @Test
    fun testProviderKeySanitizationAndMasking() {
        val engine = com.jarvis.assistant.ai.orchestration.ProviderDiscoveryEngine()

        val messyKey = "  \"sk-or-v1-mocktestdummy1234567890abcdef1234567890abcdef1234567890mock\"\n\t "
        val cleaned = engine.sanitizeKey(messyKey)
        assertEquals("sk-or-v1-mocktestdummy1234567890abcdef1234567890abcdef1234567890mock", cleaned)

        val masked = engine.maskKey(cleaned)
        assertTrue(masked.startsWith("sk-or-v1"))
        assertTrue(masked.endsWith("mock"))
        assertFalse(masked.contains("1234567890abcdef"))

        val log = "Request failed for key sk-proj-dummytestkey99999999999999999999 with code 401"
        val redacted = engine.redactSensitive(log)
        assertFalse(redacted.contains("sk-proj-dummytestkey"))
        assertTrue(redacted.contains("[REDACTED_API_KEY]"))
    }

    @Test
    fun testFreeModelCategorization() {
        val detector = com.jarvis.assistant.ai.orchestration.FreeModelDetectionEngine()

        // 1. Ollama is Category A
        assertEquals(
            com.jarvis.assistant.ai.orchestration.FreeTierCategory.CATEGORY_A_VERIFIED_FREE,
            detector.evaluatePricingCategory(com.jarvis.assistant.ai.orchestration.ProviderPreset.OLLAMA, "llama3.2", null, null)
        )

        // 2. OpenRouter :free tag is Category A
        assertEquals(
            com.jarvis.assistant.ai.orchestration.FreeTierCategory.CATEGORY_A_VERIFIED_FREE,
            detector.evaluatePricingCategory(com.jarvis.assistant.ai.orchestration.ProviderPreset.OPENROUTER, "meta-llama/llama-3.3-70b-instruct:free", "0", "0")
        )

        // 3. OpenRouter non-free is Category D (Paid)
        assertEquals(
            com.jarvis.assistant.ai.orchestration.FreeTierCategory.CATEGORY_D_PAID,
            detector.evaluatePricingCategory(com.jarvis.assistant.ai.orchestration.ProviderPreset.OPENROUTER, "anthropic/claude-3.5-sonnet", "0.003", "0.015")
        )

        // 4. Gemini official API has Category B (Free tier quota)
        assertEquals(
            com.jarvis.assistant.ai.orchestration.FreeTierCategory.CATEGORY_B_FREE_TIER_QUOTA,
            detector.evaluatePricingCategory(com.jarvis.assistant.ai.orchestration.ProviderPreset.GEMINI, "gemini-1.5-flash", null, null)
        )
    }

    @Test
    fun testFreeModeOnlyStrictEnforcement() {
        val detector = com.jarvis.assistant.ai.orchestration.FreeModelDetectionEngine()

        val modelFree = com.jarvis.assistant.ai.orchestration.DiscoveredModel(
            id = "m1",
            providerId = "or",
            modelIdentifier = "qwen/qwen-2.5-coder-32b-instruct:free",
            displayName = "Qwen Free",
            freeCategory = com.jarvis.assistant.ai.orchestration.FreeTierCategory.CATEGORY_A_VERIFIED_FREE.name
        )

        val modelPaid = com.jarvis.assistant.ai.orchestration.DiscoveredModel(
            id = "m2",
            providerId = "or",
            modelIdentifier = "openai/gpt-4o",
            displayName = "GPT-4o Paid",
            freeCategory = com.jarvis.assistant.ai.orchestration.FreeTierCategory.CATEGORY_D_PAID.name
        )

        val modelUnknown = com.jarvis.assistant.ai.orchestration.DiscoveredModel(
            id = "m3",
            providerId = "custom",
            modelIdentifier = "custom-llm",
            displayName = "Custom Unknown",
            freeCategory = com.jarvis.assistant.ai.orchestration.FreeTierCategory.CATEGORY_E_UNKNOWN.name
        )

        val allModels = listOf(modelFree, modelPaid, modelUnknown)

        // In Free Mode Only = true: PAID and UNKNOWN are excluded!
        val freeOnlyList = detector.filterEligibleModels(allModels, freeModeOnly = true)
        assertEquals(1, freeOnlyList.size)
        assertEquals("m1", freeOnlyList[0].id)

        // In Free Mode Only = false: All available models permitted
        val allAllowed = detector.filterEligibleModels(allModels, freeModeOnly = false)
        assertEquals(3, allAllowed.size)
    }

    @Test
    fun testTaskIntelligenceClassification14Categories() {
        val classifier = com.jarvis.assistant.ai.orchestration.TaskIntelligenceClassifier()

        // Android Development
        assertEquals(
            com.jarvis.assistant.ai.orchestration.TaskCategory.ANDROID_DEVELOPMENT,
            classifier.classifyTask("How do I fix recomposition lag in Jetpack Compose LazyColumn in Kotlin?")
        )

        // Code Debugging
        assertEquals(
            com.jarvis.assistant.ai.orchestration.TaskCategory.CODE_DEBUGGING,
            classifier.classifyTask("Here is a fatal error NullPointerException crash stacktrace in my app.")
        )

        // Programming
        assertEquals(
            com.jarvis.assistant.ai.orchestration.TaskCategory.PROGRAMMING,
            classifier.classifyTask("```python\ndef solve(): pass\n``` Write an algorithm to find shortest path.")
        )

        // Terminal Assistance
        assertEquals(
            com.jarvis.assistant.ai.orchestration.TaskCategory.TERMINAL_ASSISTANCE,
            classifier.classifyTask("Jarvis, pkg install python aur bash script run karo.")
        )

        // Complex Reasoning
        assertEquals(
            com.jarvis.assistant.ai.orchestration.TaskCategory.COMPLEX_REASONING,
            classifier.classifyTask("Reason step by step to prove that the square root of 2 is irrational.")
        )

        // Image Understanding
        assertEquals(
            com.jarvis.assistant.ai.orchestration.TaskCategory.IMAGE_UNDERSTANDING,
            classifier.classifyTask("What does this diagram show?", hasImage = true)
        )

        // General Conversation
        assertEquals(
            com.jarvis.assistant.ai.orchestration.TaskCategory.GENERAL_CONVERSATION,
            classifier.classifyTask("Good morning, what's on the schedule today?")
        )
    }

    @Test
    fun testUsageQuotaManagerRateLimitHeaderParsing() {
        val quotaManager = com.jarvis.assistant.ai.orchestration.UsageQuotaManager()

        // Headers with remaining requests
        val headersWithQuota = okhttp3.Headers.Builder()
            .add("x-ratelimit-remaining-requests", "148")
            .add("x-ratelimit-remaining-tokens", "99400")
            .add("x-ratelimit-reset", "2")
            .build()

        val parsed = quotaManager.extractRateLimitInfo(headersWithQuota)
        assertTrue(parsed.contains("148"))
        assertTrue(parsed.contains("99400"))

        // Headers without rate limit info -> must return "QUOTA UNKNOWN"
        val emptyHeaders = okhttp3.Headers.Builder().build()
        val emptyParsed = quotaManager.extractRateLimitInfo(emptyHeaders)
        assertEquals("QUOTA UNKNOWN", emptyParsed)
    }

    @Test
    fun testUsageQuotaManagerErrorClassification() {
        val quotaManager = com.jarvis.assistant.ai.orchestration.UsageQuotaManager()

        assertTrue(quotaManager.classifyError(401, "Invalid API Key").startsWith("AUTH_FAILURE"))
        assertTrue(quotaManager.classifyError(404, "Model not found").startsWith("MODEL_UNAVAILABLE"))
        assertTrue(quotaManager.classifyError(429, "Rate limit exceeded").startsWith("RATE_LIMIT_REACHED"))
        assertTrue(quotaManager.classifyError(429, "Daily quota depleted").startsWith("DAILY_QUOTA_EXHAUSTED"))
        assertTrue(quotaManager.classifyError(503, "Service unavailable").startsWith("TEMPORARY_SERVICE_FAILURE"))
    }

    @Test
    fun testMultiAgentSystemSystemPrompts() {
        val engine = com.jarvis.assistant.ai.orchestration.AutonomousRotationEngine()
        val multiAgent = com.jarvis.assistant.ai.orchestration.AutonomousMultiAgentSystem(engine)

        val corePrompt = multiAgent.getAgentSystemPrompt(com.jarvis.assistant.ai.orchestration.AgentType.CORE)
        assertTrue(corePrompt.contains("JARVIS CORE"))

        val coderPrompt = multiAgent.getAgentSystemPrompt(com.jarvis.assistant.ai.orchestration.AgentType.CODER)
        assertTrue(coderPrompt.contains("JARVIS CODER"))
        assertTrue(coderPrompt.contains("Jetpack Compose"))

        val reviewerPrompt = multiAgent.getAgentSystemPrompt(com.jarvis.assistant.ai.orchestration.AgentType.REVIEWER)
        assertTrue(reviewerPrompt.contains("JARVIS REVIEWER"))
        assertTrue(reviewerPrompt.contains("Independent Quality Assurance"))
    }
}

