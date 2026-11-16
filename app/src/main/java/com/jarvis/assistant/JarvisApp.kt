package com.jarvis.assistant

import android.app.Application
import com.jarvis.assistant.ai.GeminiAiProvider
import com.jarvis.assistant.ai.GeminiLiveSession
import com.jarvis.assistant.automation.TaskExecutionEngine
import com.jarvis.assistant.data.local.db.JarvisDatabase
import com.jarvis.assistant.data.repository.ChatRepository
import com.jarvis.assistant.data.repository.JarvisPreferences
import com.jarvis.assistant.data.repository.NeuralMemoryRepository
import com.jarvis.assistant.data.repository.WeatherRepository
import com.jarvis.assistant.device.DeviceAutomationController
import com.jarvis.assistant.service.JarvisVoiceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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

    lateinit var geminiAiProvider: GeminiAiProvider
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
        geminiAiProvider = GeminiAiProvider()
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
