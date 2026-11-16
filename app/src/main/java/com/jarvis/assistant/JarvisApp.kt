package com.jarvis.assistant

import android.app.Application
import com.jarvis.assistant.data.repository.JarvisPreferences
import com.jarvis.assistant.data.repository.NeuralMemoryRepository
import com.jarvis.assistant.data.repository.WeatherRepository
import com.jarvis.assistant.device.DeviceAutomationController
import com.jarvis.assistant.service.JarvisVoiceEngine

class JarvisApp : Application() {

    lateinit var preferences: JarvisPreferences
        private set

    lateinit var memoryRepository: NeuralMemoryRepository
        private set

    lateinit var weatherRepository: WeatherRepository
        private set

    lateinit var automationController: DeviceAutomationController
        private set

    lateinit var voiceEngine: JarvisVoiceEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferences = JarvisPreferences(this)
        memoryRepository = NeuralMemoryRepository(this)
        weatherRepository = WeatherRepository()
        automationController = DeviceAutomationController(this)
        voiceEngine = JarvisVoiceEngine(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        voiceEngine.destroy()
    }

    companion object {
        lateinit var instance: JarvisApp
            private set
    }
}
