package com.jarvis.assistant

import android.app.Application
import com.jarvis.assistant.ai.AiProviderManager
import com.jarvis.assistant.ai.CustomOpenAiProvider
import com.jarvis.assistant.ai.GeminiAiProvider
import com.jarvis.assistant.ai.GeminiLiveSession
import com.jarvis.assistant.ai.OpenRouterAiProvider
import com.jarvis.assistant.ai.QwenAiProvider
import com.jarvis.assistant.automation.TaskExecutionEngine
import com.jarvis.assistant.data.local.db.JarvisDatabase
import com.jarvis.assistant.data.repository.ChatRepository
import com.jarvis.assistant.data.repository.JarvisPreferences
import com.jarvis.assistant.data.repository.NeuralMemoryRepository
import com.jarvis.assistant.data.repository.WeatherRepository
import com.jarvis.assistant.device.DeviceAutomationController
import com.jarvis.assistant.security.SecurityLab
import com.jarvis.assistant.service.JarvisVoiceEngine
import com.jarvis.assistant.terminal.AutonomousCommandPlanner
import com.jarvis.assistant.terminal.TerminalExecutionEngine
import com.jarvis.assistant.terminal.TermuxBridge
import com.jarvis.assistant.terminal.ToolDiscoveryRegistry
import com.jarvis.assistant.ai.gemini.GeminiAutoConfigEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JarvisApp : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var database: JarvisDatabase
        private set

    lateinit var preferences: JarvisPreferences
        private set

    lateinit var memoryRepository: NeuralMemoryRepository
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var weatherRepository: WeatherRepository
        private set

    lateinit var automationController: DeviceAutomationController
        private set

    lateinit var voiceEngine: JarvisVoiceEngine
        private set

    lateinit var geminiAutoConfigEngine: GeminiAutoConfigEngine
        private set

    lateinit var geminiAiProvider: GeminiAiProvider
        private set

    lateinit var openRouterProvider: OpenRouterAiProvider
        private set

    lateinit var qwenAiProvider: QwenAiProvider
        private set

    lateinit var customAiProvider: CustomOpenAiProvider
        private set

    lateinit var aiProviderManager: AiProviderManager
        private set

    lateinit var securityLab: SecurityLab
        private set

    lateinit var termuxBridge: TermuxBridge
        private set

    lateinit var toolDiscoveryRegistry: ToolDiscoveryRegistry
        private set

    lateinit var commandPlanner: AutonomousCommandPlanner
        private set

    lateinit var terminalEngine: TerminalExecutionEngine
        private set

    lateinit var liveSession: GeminiLiveSession
        private set

    lateinit var taskEngine: TaskExecutionEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = JarvisDatabase.getInstance(this)
        preferences = JarvisPreferences(this)
        memoryRepository = NeuralMemoryRepository(database.memoryDao(), applicationScope)
        chatRepository = ChatRepository(database.chatDao())
        weatherRepository = WeatherRepository()
        automationController = DeviceAutomationController(this)
        voiceEngine = JarvisVoiceEngine(this)

        // Multi-AI Providers & Gemini Auto-Config Engine
        geminiAutoConfigEngine = GeminiAutoConfigEngine(preferences)
        geminiAiProvider = GeminiAiProvider(autoConfigEngine = geminiAutoConfigEngine)
        applicationScope.launch {
            geminiAutoConfigEngine.restoreSavedState()
        }
        openRouterProvider = OpenRouterAiProvider()
        qwenAiProvider = QwenAiProvider()
        customAiProvider = CustomOpenAiProvider()
        aiProviderManager = AiProviderManager(
            geminiProvider = geminiAiProvider,
            openRouterProvider = openRouterProvider,
            qwenProvider = qwenAiProvider,
            customProvider = customAiProvider,
            preferences = preferences,
            scope = applicationScope
        )

        // Defensive Cybersecurity Lab
        securityLab = SecurityLab(this)

        // Autonomous Termux Execution Engine
        termuxBridge = TermuxBridge(this)
        toolDiscoveryRegistry = ToolDiscoveryRegistry(this, termuxBridge)
        commandPlanner = AutonomousCommandPlanner(toolDiscoveryRegistry)
        terminalEngine = TerminalExecutionEngine(
            context = this,
            bridge = termuxBridge,
            toolRegistry = toolDiscoveryRegistry,
            planner = commandPlanner,
            aiProviderManager = aiProviderManager,
            scope = applicationScope
        )

        liveSession = GeminiLiveSession(this, applicationScope)
        taskEngine = TaskExecutionEngine(this, automationController, database.taskDao(), applicationScope)
    }

    override fun onTerminate() {
        super.onTerminate()
        voiceEngine.destroy()
        liveSession.disconnect()
    }

    companion object {
        lateinit var instance: JarvisApp
            private set
    }
}
