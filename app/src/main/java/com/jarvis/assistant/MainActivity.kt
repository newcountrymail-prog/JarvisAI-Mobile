package com.jarvis.assistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jarvis.assistant.ai.PersonaPromptBuilder
import com.jarvis.assistant.automation.JarvisIntent
import com.jarvis.assistant.automation.JarvisIntentParser
import com.jarvis.assistant.data.model.AssistantState
import com.jarvis.assistant.data.model.ChatMessage
import com.jarvis.assistant.data.model.ChatMode
import com.jarvis.assistant.data.model.MemoryCategory
import com.jarvis.assistant.data.model.MessageSender
import com.jarvis.assistant.data.model.TaskHudState
import com.jarvis.assistant.data.model.TaskStatus
import com.jarvis.assistant.data.model.ThemePreset
import com.jarvis.assistant.service.JarvisOverlayService
import com.jarvis.assistant.service.JarvisScreenService
import com.jarvis.assistant.ui.components.FloatingBottomNavigation
import com.jarvis.assistant.ui.components.NavDestination
import com.jarvis.assistant.ui.components.TaskHudOverlay
import com.jarvis.assistant.ui.components.VoiceInteractionSheet
import com.jarvis.assistant.data.model.OrbCustomizationConfig
import com.jarvis.assistant.data.model.ProviderRole
import com.jarvis.assistant.ui.screens.AiControlCenterScreen
import com.jarvis.assistant.ui.screens.ChatScreen
import com.jarvis.assistant.ui.screens.CodingWorkspaceScreen
import com.jarvis.assistant.ui.screens.GeminiDiagnosticsScreen
import com.jarvis.assistant.ui.screens.HomeScreen
import com.jarvis.assistant.ui.screens.SecurityLabScreen
import com.jarvis.assistant.ui.screens.SettingsScreen
import com.jarvis.assistant.ui.screens.TerminalDashboardScreen
import com.jarvis.assistant.ui.screens.ToolboxScreen
import com.jarvis.assistant.ui.screens.UpdatesScreen
import com.jarvis.assistant.ui.theme.JarvisTheme
import com.jarvis.assistant.ui.theme.SunsetBackground
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {

    private val app get() = JarvisApp.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val scope = rememberCoroutineScope()
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: NavDestination.HOME.route

            // Preferences state
            val userName by app.preferences.userNameFlow.collectAsState(initial = "Master")
            val userRole by app.preferences.userRoleFlow.collectAsState(initial = "JARVIS OPERATOR • PRIMARY USER")
            val voiceEnrolled by app.preferences.voiceEnrolledFlow.collectAsState(initial = true)
            val jarvisPro by app.preferences.jarvisProFlow.collectAsState(initial = true)
            val silentExecution by app.preferences.silentExecutionFlow.collectAsState(initial = false)
            val currentLanguage by app.preferences.languageFlow.collectAsState(initial = "Hinglish")
            val geminiModel by app.preferences.geminiModelFlow.collectAsState(initial = "gemini-1.5-flash")
            val geminiApiKey by app.preferences.geminiApiKeyFlow.collectAsState(initial = "")
            val geminiConfigState by app.geminiAutoConfigEngine.engineState.collectAsState()
            val themePreset by app.preferences.themeFlow.collectAsState(initial = ThemePreset.SUNSET)
            val personaConfig by app.preferences.personaFlow.collectAsState(initial = com.jarvis.assistant.data.model.AiPersonaConfig())
            val overlayConfig by app.preferences.overlayConfigFlow.collectAsState(initial = com.jarvis.assistant.data.model.OverlayAvatarConfig())
            val orbConfig by app.preferences.orbConfigFlow.collectAsState(initial = OrbCustomizationConfig())
            val audioAmplitude by app.voiceEngine.audioAmplitudeFlow.collectAsState()

            // Live Weather
            val weatherState by app.weatherRepository.weatherState.collectAsState()

            // AI Orb & Assistant State
            var assistantState by remember { mutableStateOf(AssistantState.IDLE) }
            var showVoiceSheet by remember { mutableStateOf(false) }
            var voiceTranscript by remember { mutableStateOf("") }
            var voiceAiResponse by remember { mutableStateOf("") }

            // Task HUD State connected to real execution engine
            val taskHudState by app.taskEngine.hudState.collectAsState()

            // Real Room-backed Chat Messages
            val chatMessages by app.chatRepository.getMessagesFlow().collectAsState(initial = emptyList())
            var isThinking by remember { mutableStateOf(false) }

            // Initial auto-seed and weather refresh
            LaunchedEffect(Unit) {
                app.weatherRepository.refreshWeather()
                val existingMessages = app.chatRepository.getMessagesFlow().firstOrNull()
                if (existingMessages.isNullOrEmpty()) {
                    app.chatRepository.addMessage(
                        sender = MessageSender.JARVIS,
                        text = "Good day, Master. JARVIS neural core is calibrated and awaiting your command. How may I assist your workflow today?"
                    )
                }
            }

            // Sync Overlay Service
            LaunchedEffect(overlayConfig.isEnabled) {
                if (overlayConfig.isEnabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this@MainActivity)) {
                        val serviceIntent = Intent(this@MainActivity, JarvisOverlayService::class.java)
                        startService(serviceIntent)
                    }
                } else {
                    val stopIntent = Intent(this@MainActivity, JarvisOverlayService::class.java)
                    stopService(stopIntent)
                }
            }

            // Screen Share launcher
            val screenCaptureLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    Toast.makeText(this@MainActivity, "Screen frame observation active for verified actions.", Toast.LENGTH_SHORT).show()
                    val serviceIntent = Intent(this@MainActivity, JarvisScreenService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                }
            }

            // Voice command executor
            fun processVoiceCommand(prompt: String) {
                val intent = JarvisIntentParser.parse(prompt)
                scope.launch {
                    app.chatRepository.addMessage(
                        sender = MessageSender.USER,
                        text = prompt,
                        mode = ChatMode.JARVIS
                    )
                    when (intent) {
                        is JarvisIntent.WeatherQuery -> {
                            app.weatherRepository.refreshWeather()
                            val w = app.weatherRepository.weatherState.value
                            val reply = "Atmosphere report for ${w.location}: ${w.temperature}°C, ${w.condition}. Humidity at ${w.humidity}%."
                            voiceAiResponse = reply
                            app.chatRepository.addMessage(MessageSender.JARVIS, reply)
                            if (!silentExecution) {
                                app.voiceEngine.speak(reply, personaConfig.speakingSpeed)
                            }
                            delay(1200)
                            assistantState = AssistantState.IDLE
                        }
                        is JarvisIntent.DeviceTelemetryAction -> {
                            val t = app.automationController.getDeviceTelemetry()
                            val reply = when (intent.target) {
                                "battery" -> "Battery is currently at ${t.batteryLevel}% (${if (t.isCharging) "Charging" else "Discharging"})."
                                "network" -> "Network interface connected via ${t.networkName}."
                                "settings" -> {
                                    app.automationController.openSettings()
                                    "Opening system settings panel, Master."
                                }
                                else -> "Battery is at ${t.batteryLevel}%, network connected to ${t.networkName}."
                            }
                            voiceAiResponse = reply
                            app.chatRepository.addMessage(MessageSender.JARVIS, reply)
                            if (!silentExecution) {
                                app.voiceEngine.speak(reply, personaConfig.speakingSpeed)
                            }
                            delay(1200)
                            assistantState = AssistantState.IDLE
                        }
                        is JarvisIntent.CommunicationWhatsApp,
                        is JarvisIntent.CommunicationCall,
                        is JarvisIntent.YouTubeAction,
                        is JarvisIntent.SpotifyAction -> {
                            app.taskEngine.executeIntent(intent) { feedback ->
                                voiceAiResponse = feedback
                                scope.launch {
                                    app.chatRepository.addMessage(MessageSender.JARVIS, feedback)
                                    if (!silentExecution) {
                                        app.voiceEngine.speak(feedback, personaConfig.speakingSpeed)
                                    }
                                }
                            }
                            assistantState = AssistantState.EXECUTING_TASK
                        }
                        is JarvisIntent.MemoryRecall -> {
                            val mems = app.memoryRepository.searchMemories(intent.query)
                            val reply = if (mems.isNotEmpty()) {
                                "Recalled from neural memory: ${mems.first().title} - ${mems.first().content}"
                            } else {
                                "No matching neural memories found for '${intent.query}'."
                            }
                            voiceAiResponse = reply
                            app.chatRepository.addMessage(MessageSender.JARVIS, reply)
                            if (!silentExecution) {
                                app.voiceEngine.speak(reply, personaConfig.speakingSpeed)
                            }
                            delay(1200)
                            assistantState = AssistantState.IDLE
                        }
                        is JarvisIntent.MemorySave -> {
                            app.memoryRepository.addMemory(MemoryCategory.USER_PREFERENCES, "Voice Note", intent.fact)
                            val reply = "Saved to neural core: ${intent.fact}"
                            voiceAiResponse = reply
                            app.chatRepository.addMessage(MessageSender.JARVIS, reply)
                            if (!silentExecution) {
                                app.voiceEngine.speak(reply, personaConfig.speakingSpeed)
                            }
                            delay(1200)
                            assistantState = AssistantState.IDLE
                        }
                        else -> {
                            if (geminiApiKey.isBlank()) {
                                val reply = "Gemini API key is not configured. Please enter your API key in Settings & Intelligence to activate live AI conversation."
                                voiceAiResponse = reply
                                app.chatRepository.addMessage(MessageSender.JARVIS, reply)
                                if (!silentExecution) {
                                    app.voiceEngine.speak(reply, personaConfig.speakingSpeed)
                                }
                                delay(1200)
                                assistantState = AssistantState.IDLE
                            } else {
                                val activeMemories = app.memoryRepository.memories.value
                                val systemPrompt = PersonaPromptBuilder.buildSystemInstruction(userName, personaConfig, activeMemories)
                                val result = app.geminiAiProvider.generateResponse(
                                    prompt = prompt,
                                    conversationHistory = emptyList(),
                                    systemInstruction = systemPrompt,
                                    model = geminiModel,
                                    apiKey = geminiApiKey
                                )
                                val reply = result.getOrNull()?.text ?: "AI communication failed: ${result.exceptionOrNull()?.message}"
                                voiceAiResponse = reply
                                app.chatRepository.addMessage(MessageSender.JARVIS, reply)
                                if (!silentExecution) {
                                    app.voiceEngine.speak(reply.take(150), personaConfig.speakingSpeed)
                                }
                                delay(1200)
                                assistantState = AssistantState.IDLE
                            }
                        }
                    }
                }
            }

            // Audio Permission Launcher
            val recordAudioLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    voiceTranscript = ""
                    voiceAiResponse = ""
                    showVoiceSheet = true
                    assistantState = AssistantState.LISTENING
                    app.voiceEngine.playActivationChime()
                    app.voiceEngine.startListening(currentLanguage)
                } else {
                    Toast.makeText(this@MainActivity, "Microphone permission required for voice interaction.", Toast.LENGTH_SHORT).show()
                }
            }

            // Wire Voice Engine Listener
            DisposableEffect(Unit) {
                app.voiceEngine.onTranscriptionResult = { text ->
                    voiceTranscript = text
                }
                app.voiceEngine.onListeningStarted = {
                    assistantState = AssistantState.LISTENING
                }
                app.voiceEngine.onListeningEnded = {
                    if (voiceTranscript.isNotBlank() && assistantState == AssistantState.LISTENING) {
                        assistantState = AssistantState.PROCESSING
                        processVoiceCommand(voiceTranscript)
                    }
                }
                app.voiceEngine.onSpeechStarted = {
                    assistantState = AssistantState.SPEAKING
                }
                app.voiceEngine.onSpeechCompleted = {
                    assistantState = AssistantState.IDLE
                }
                app.voiceEngine.onError = { error ->
                    voiceAiResponse = error
                    assistantState = AssistantState.IDLE
                }

                onDispose {
                    app.voiceEngine.onTranscriptionResult = null
                    app.voiceEngine.onListeningStarted = null
                    app.voiceEngine.onListeningEnded = null
                    app.voiceEngine.onSpeechStarted = null
                    app.voiceEngine.onSpeechCompleted = null
                    app.voiceEngine.onError = null
                }
            }

            JarvisTheme(preset = themePreset, darkTheme = themePreset == ThemePreset.DARK || themePreset == ThemePreset.AMOLED) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    // Main NavHost
                    NavHost(
                        navController = navController,
                        startDestination = NavDestination.HOME.route,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(NavDestination.HOME.route) {
                            HomeScreen(
                                assistantState = assistantState,
                                weatherState = weatherState,
                                currentLanguage = currentLanguage,
                                audioAmplitude = audioAmplitude,
                                orbConfig = orbConfig,
                                geminiStatus = geminiConfigState.status,
                                onPowerToggle = {
                                    if (assistantState == AssistantState.OFFLINE) {
                                        assistantState = AssistantState.IDLE
                                        app.voiceEngine.playActivationChime()
                                        if (!silentExecution) {
                                            app.voiceEngine.speak("JARVIS online. At your service, Master.", personaConfig.speakingSpeed)
                                        }
                                    } else {
                                        assistantState = AssistantState.OFFLINE
                                        app.voiceEngine.stop()
                                    }
                                },
                                onNavigateChat = { navController.navigate(NavDestination.CHAT.route) },
                                onNavigateSettings = { navController.navigate(NavDestination.SETTINGS.route) },
                                onRefreshWeather = {
                                    scope.launch { app.weatherRepository.refreshWeather() }
                                },
                                onLanguageSelected = { lang ->
                                    scope.launch { app.preferences.setLanguage(lang) }
                                },
                                onMemoriesTapped = {
                                    navController.navigate(NavDestination.SETTINGS.route)
                                },
                                onRequestScreenShare = {
                                    val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                                    screenCaptureLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
                                }
                            )
                        }

                        composable(NavDestination.CHAT.route) {
                            ChatScreen(
                                messages = chatMessages,
                                isThinking = isThinking,
                                onSendMessage = { text, mode ->
                                    scope.launch {
                                        app.chatRepository.addMessage(
                                            sender = MessageSender.USER,
                                            text = text,
                                            mode = mode
                                        )
                                        isThinking = true
                                        assistantState = AssistantState.THINKING

                                        val intent = JarvisIntentParser.parse(text)
                                        when (intent) {
                                            is JarvisIntent.WeatherQuery -> {
                                                app.weatherRepository.refreshWeather()
                                                val w = app.weatherRepository.weatherState.value
                                                val reply = "Atmospheric telemetry for ${w.location}: ${w.temperature}, ${w.condition}. Humidity at ${w.humidity}, wind speed ${w.windSpeed}."
                                                app.chatRepository.addMessage(MessageSender.JARVIS, reply, mode = mode)
                                                if (!silentExecution) {
                                                    app.voiceEngine.speak(reply, personaConfig.speakingSpeed)
                                                }
                                                isThinking = false
                                                assistantState = AssistantState.IDLE
                                            }
                                            is JarvisIntent.DeviceTelemetryAction -> {
                                                val t = app.automationController.getDeviceTelemetry()
                                                val reply = when (intent.target) {
                                                    "battery" -> "Battery is at ${t.batteryLevel}% (${if (t.isCharging) "Charging via AC/USB" else "Discharging"}). Battery Health: ${t.batteryHealth}."
                                                    "network" -> "Network interface: ${t.networkName} (Connected: ${t.isNetworkConnected})."
                                                    "settings" -> {
                                                        app.automationController.openSettings()
                                                        "Opening system settings panel, Master."
                                                    }
                                                    else -> "System telemetry: Battery ${t.batteryLevel}%, Network ${t.networkName}."
                                                }
                                                app.chatRepository.addMessage(MessageSender.JARVIS, reply, mode = mode)
                                                if (!silentExecution) {
                                                    app.voiceEngine.speak(reply, personaConfig.speakingSpeed)
                                                }
                                                isThinking = false
                                                assistantState = AssistantState.IDLE
                                            }
                                            is JarvisIntent.MemoryRecall -> {
                                                val mems = app.memoryRepository.searchMemories(intent.query)
                                                val reply = if (mems.isNotEmpty()) {
                                                    "Recalled ${mems.size} relevant memory records:\n" + mems.take(4).joinToString("\n") { "• ${it.title}: ${it.content}" }
                                                } else {
                                                    "No specific neural memories matching '${intent.query}' found in core storage."
                                                }
                                                app.chatRepository.addMessage(MessageSender.JARVIS, reply, mode = mode)
                                                if (!silentExecution) {
                                                    app.voiceEngine.speak(reply.take(120), personaConfig.speakingSpeed)
                                                }
                                                isThinking = false
                                                assistantState = AssistantState.IDLE
                                            }
                                            is JarvisIntent.MemorySave -> {
                                                app.memoryRepository.addMemory(MemoryCategory.USER_PREFERENCES, "User Fact", intent.fact)
                                                val reply = "Affirmative. Stored in neural memory: \"${intent.fact}\""
                                                app.chatRepository.addMessage(MessageSender.JARVIS, reply, mode = mode)
                                                if (!silentExecution) {
                                                    app.voiceEngine.speak(reply, personaConfig.speakingSpeed)
                                                }
                                                isThinking = false
                                                assistantState = AssistantState.IDLE
                                            }
                                            is JarvisIntent.CommunicationWhatsApp,
                                            is JarvisIntent.CommunicationCall,
                                            is JarvisIntent.YouTubeAction,
                                            is JarvisIntent.SpotifyAction -> {
                                                val initMsgId = app.chatRepository.addMessage(
                                                    MessageSender.JARVIS,
                                                    "Initiating verified execution protocol for command...",
                                                    mode = mode
                                                )
                                                app.taskEngine.executeIntent(intent) { feedback ->
                                                    scope.launch {
                                                        app.chatRepository.updateMessageStream(initMsgId, feedback, isStreaming = false)
                                                        if (!silentExecution) {
                                                            app.voiceEngine.speak(feedback, personaConfig.speakingSpeed)
                                                        }
                                                    }
                                                }
                                                isThinking = false
                                                assistantState = AssistantState.IDLE
                                            }
                                            is JarvisIntent.OpenApp -> {
                                                val opened = app.automationController.openApp(intent.appName)
                                                val reply = if (opened) "Launching ${intent.appName}." else "Could not locate application package for ${intent.appName}."
                                                app.chatRepository.addMessage(MessageSender.JARVIS, reply, mode = mode)
                                                if (!silentExecution) {
                                                    app.voiceEngine.speak(reply, personaConfig.speakingSpeed)
                                                }
                                                isThinking = false
                                                assistantState = AssistantState.IDLE
                                            }
                                            is JarvisIntent.GeneralAiConversation -> {
                                                val hasConfiguredProvider = geminiApiKey.isNotBlank() ||
                                                        app.universalOrchestrator.providers.value.any { it.isEnabled && (it.apiKey.isNotBlank() || it.authType == "NONE") }

                                                if (!hasConfiguredProvider) {
                                                    val notice = "No active AI provider is configured. Please open AI Control Center (via Toolbox or Settings) and enter an API credential (Gemini, OpenRouter, Qwen, Ollama, etc.) to activate multi-model intelligence."
                                                    app.chatRepository.addMessage(MessageSender.JARVIS, notice, mode = mode)
                                                    if (!silentExecution) {
                                                        app.voiceEngine.speak("AI provider required for cloud intelligence. Please configure in AI Control Center.", personaConfig.speakingSpeed)
                                                    }
                                                    isThinking = false
                                                    assistantState = AssistantState.IDLE
                                                } else {
                                                    val aiMsgId = app.chatRepository.addMessage(
                                                        sender = MessageSender.JARVIS,
                                                        text = "Thinking...",
                                                        mode = mode,
                                                        isCode = mode == ChatMode.CODE
                                                    )
                                                    val activeMemories = app.memoryRepository.memories.value
                                                    val systemInstruction = PersonaPromptBuilder.buildSystemInstruction(userName, personaConfig, activeMemories)
                                                    val history = app.chatRepository.getMessagesFlow().firstOrNull() ?: emptyList()

                                                    val orchResult = app.universalOrchestrator.executeChat(
                                                        prompt = text,
                                                        conversationHistory = history.takeLast(10),
                                                        systemInstruction = systemInstruction
                                                    )

                                                    isThinking = false
                                                    if (orchResult.isSuccess) {
                                                        app.chatRepository.updateMessageStream(aiMsgId, orchResult.responseText, false)
                                                        if (!silentExecution) {
                                                            val speakText = if (mode == ChatMode.CODE) "Code implementation prepared, Master." else orchResult.responseText.take(150)
                                                            app.voiceEngine.speak(speakText, personaConfig.speakingSpeed)
                                                        }
                                                    } else {
                                                        val errText = "Orchestration Error: ${orchResult.errorMessage ?: "Unable to complete request across eligible providers."}"
                                                        app.chatRepository.updateMessageStream(aiMsgId, errText, false)
                                                    }
                                                    assistantState = AssistantState.IDLE
                                                }
                                            }
                                        }
                                    }
                                },
                                onNewChat = {
                                    scope.launch {
                                        app.chatRepository.clearHistory()
                                        app.chatRepository.addMessage(
                                            sender = MessageSender.JARVIS,
                                            text = "New neural session initialized. Standing by for instructions.",
                                            mode = ChatMode.JARVIS
                                        )
                                    }
                                },
                                onDeleteMessage = { id ->
                                    scope.launch { app.chatRepository.deleteMessage(id) }
                                },
                                onRegenerateResponse = {
                                    scope.launch {
                                        val history = app.chatRepository.getMessagesFlow().firstOrNull() ?: emptyList()
                                        val lastUserMsg = history.lastOrNull { it.sender == MessageSender.USER }
                                        if (lastUserMsg != null) {
                                            isThinking = true
                                            val aiMsgId = app.chatRepository.addMessage(
                                                sender = MessageSender.JARVIS,
                                                text = "Regenerating...",
                                                mode = lastUserMsg.mode,
                                                isCode = lastUserMsg.mode == ChatMode.CODE
                                            )
                                            val activeMemories = app.memoryRepository.memories.value
                                            val systemInstruction = PersonaPromptBuilder.buildSystemInstruction(userName, personaConfig, activeMemories)
                                            val sb = StringBuilder()
                                            val role = if (lastUserMsg.mode == ChatMode.CODE) ProviderRole.CODING else ProviderRole.GENERAL
                                            app.aiProviderManager.streamResponse(
                                                prompt = lastUserMsg.text,
                                                conversationHistory = history.dropLast(1).takeLast(10),
                                                systemInstruction = systemInstruction,
                                                role = role
                                            ).collect { chunk ->
                                                isThinking = false
                                                if (chunk.error != null) {
                                                    sb.append("\n[Error: ${chunk.error}]")
                                                    app.chatRepository.updateMessageStream(aiMsgId, sb.toString(), false)
                                                } else {
                                                    sb.append(chunk.textDelta)
                                                    app.chatRepository.updateMessageStream(aiMsgId, sb.toString(), !chunk.isComplete)
                                                }
                                            }
                                        }
                                    }
                                },
                                onSpeakMessage = { text ->
                                    app.voiceEngine.speak(text, personaConfig.speakingSpeed)
                                },
                                onVoiceMicTapped = {
                                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                        recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        voiceTranscript = ""
                                        voiceAiResponse = ""
                                        showVoiceSheet = true
                                        assistantState = AssistantState.LISTENING
                                        app.voiceEngine.playActivationChime()
                                        app.voiceEngine.startListening(currentLanguage)
                                    }
                                }
                            )
                        }

                        composable(NavDestination.SETTINGS.route) {
                            SettingsScreen(
                                userName = userName,
                                userRole = userRole,
                                voiceEnrolled = voiceEnrolled,
                                jarvisPro = jarvisPro,
                                silentExecution = silentExecution,
                                currentLanguage = currentLanguage,
                                personaConfig = personaConfig,
                                geminiModel = geminiModel,
                                geminiApiKey = geminiApiKey,
                                themePreset = themePreset,
                                overlayConfig = overlayConfig,
                                memoryRepository = app.memoryRepository,
                                automationController = app.automationController,
                                voiceEngine = app.voiceEngine,
                                geminiAiProvider = app.geminiAiProvider,
                                taskEngine = app.taskEngine,
                                onUpdateUserName = { newName -> scope.launch { app.preferences.setUserName(newName) } },
                                onToggleSilentExecution = { silent -> scope.launch { app.preferences.setSilentExecution(silent) } },
                                onLanguageChanged = { lang -> scope.launch { app.preferences.setLanguage(lang) } },
                                onPersonaUpdated = { newConfig -> scope.launch { app.preferences.updatePersona(newConfig) } },
                                onGeminiConfigUpdated = { model, key ->
                                    scope.launch {
                                        app.preferences.setGeminiModel(model)
                                        app.preferences.setGeminiApiKey(key)
                                    }
                                },
                                onThemeUpdated = { newTheme -> scope.launch { app.preferences.setThemePreset(newTheme) } },
                                onOverlayConfigUpdated = { newOverlay -> scope.launch { app.preferences.updateOverlayConfig(newOverlay) } },
                                onClearChatHistory = {
                                    scope.launch { app.chatRepository.clearHistory() }
                                },
                                onNavigateGeminiDiagnostics = { navController.navigate(NavDestination.GEMINI_DIAGNOSTICS.route) },
                                onNavigateAiControlCenter = { navController.navigate(NavDestination.AI_CONTROL_CENTER.route) }
                            )
                        }

                        composable(NavDestination.UPDATES.route) {
                            UpdatesScreen()
                        }

                        composable(NavDestination.TOOLS.route) {
                            ToolboxScreen(
                                deviceController = app.automationController,
                                onNavigateTerminal = { navController.navigate(NavDestination.TERMINAL.route) },
                                onNavigateSecurityLab = { navController.navigate(NavDestination.SECURITY_LAB.route) },
                                onNavigateQwenCoder = { navController.navigate(NavDestination.QWEN_CODER.route) },
                                onNavigateMemories = { navController.navigate(NavDestination.SETTINGS.route) },
                                onNavigateGeminiDiagnostics = { navController.navigate(NavDestination.GEMINI_DIAGNOSTICS.route) },
                                onNavigateAiControlCenter = { navController.navigate(NavDestination.AI_CONTROL_CENTER.route) }
                            )
                        }

                        composable(NavDestination.TERMINAL.route) {
                            TerminalDashboardScreen(
                                engine = app.terminalEngine,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavDestination.SECURITY_LAB.route) {
                            SecurityLabScreen(
                                securityLab = app.securityLab,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavDestination.QWEN_CODER.route) {
                            CodingWorkspaceScreen(
                                aiProviderManager = app.aiProviderManager,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavDestination.GEMINI_DIAGNOSTICS.route) {
                            GeminiDiagnosticsScreen(
                                engine = app.geminiAutoConfigEngine,
                                provider = app.geminiAiProvider,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(NavDestination.AI_CONTROL_CENTER.route) {
                            AiControlCenterScreen(
                                orchestrator = app.universalOrchestrator,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }

                    // Persistent Floating Bottom Navigation
                    FloatingBottomNavigation(
                        currentDestination = currentRoute,
                        assistantState = assistantState,
                        onNavigate = { route ->
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo(NavDestination.HOME.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        onMicTapped = {
                            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                voiceTranscript = ""
                                voiceAiResponse = ""
                                showVoiceSheet = true
                                assistantState = AssistantState.LISTENING
                                app.voiceEngine.playActivationChime()
                                app.voiceEngine.startListening(currentLanguage)
                            }
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // Task HUD Overlay for autonomous verified executions
                    TaskHudOverlay(
                        state = taskHudState,
                        onCancelTask = {
                            app.taskEngine.dismissHud()
                            assistantState = AssistantState.IDLE
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .navigationBarsPadding()
                    )

                    // Voice Interaction Sheet
                    if (showVoiceSheet) {
                        VoiceInteractionSheet(
                            state = assistantState,
                            transcription = voiceTranscript,
                            aiResponse = voiceAiResponse,
                            onDismiss = {
                                showVoiceSheet = false
                                assistantState = AssistantState.IDLE
                                app.voiceEngine.stopListening()
                                app.voiceEngine.stop()
                            },
                            onQuickPromptSelected = { prompt ->
                                voiceTranscript = prompt
                                processVoiceCommand(prompt)
                            }
                        )
                    }
                }
            }
        }
    }
}
