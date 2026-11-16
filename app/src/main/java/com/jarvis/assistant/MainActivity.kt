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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.jarvis.assistant.data.model.AssistantState
import com.jarvis.assistant.data.model.ChatMessage
import com.jarvis.assistant.data.model.ChatMode
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
import com.jarvis.assistant.ui.screens.ChatScreen
import com.jarvis.assistant.ui.screens.HomeScreen
import com.jarvis.assistant.ui.screens.SettingsScreen
import com.jarvis.assistant.ui.screens.UpdatesScreen
import com.jarvis.assistant.ui.theme.JarvisTheme
import com.jarvis.assistant.ui.theme.SunsetBackground
import kotlinx.coroutines.delay
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
            val geminiModel by app.preferences.geminiModelFlow.collectAsState(initial = "gemini-3.1-flash-live-preview")
            val geminiApiKey by app.preferences.geminiApiKeyFlow.collectAsState(initial = "")
            val themePreset by app.preferences.themeFlow.collectAsState(initial = ThemePreset.SUNSET)
            val personaConfig by app.preferences.personaFlow.collectAsState(initial = com.jarvis.assistant.data.model.AiPersonaConfig())
            val overlayConfig by app.preferences.overlayConfigFlow.collectAsState(initial = com.jarvis.assistant.data.model.OverlayAvatarConfig())

            // Live Weather
            val weatherState by app.weatherRepository.weatherState.collectAsState()

            // AI Orb & Assistant State
            var assistantState by remember { mutableStateOf(AssistantState.IDLE) }
            var showVoiceSheet by remember { mutableStateOf(false) }
            var voiceTranscript by remember { mutableStateOf("") }
            var voiceAiResponse by remember { mutableStateOf("") }

            // Task HUD State
            var taskHudState by remember { mutableStateOf(TaskHudState()) }

            // Chat Messages
            val messages = remember {
                mutableStateListOf(
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        sender = MessageSender.JARVIS,
                        text = "Good day, Master. JARVIS neural core is calibrated and awaiting your command. How may I assist your workflow today?",
                        timestamp = System.currentTimeMillis() - 60000L
                    )
                )
            }
            var isThinking by remember { mutableStateOf(false) }

            // Initial refresh of atmospheric weather telemetry
            LaunchedEffect(Unit) {
                app.weatherRepository.refreshWeather()
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

            // Audio Permission Launcher
            val recordAudioLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    app.voiceEngine.playActivationChime()
                }
            }

            JarvisTheme(darkTheme = themePreset == ThemePreset.DARK || themePreset == ThemePreset.AMOLED) {
                Box(modifier = Modifier.fillMaxSize().background(SunsetBackground)) {
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
                                messages = messages,
                                isThinking = isThinking,
                                onSendMessage = { text, mode ->
                                    // 1. Add user message
                                    val userMsg = ChatMessage(
                                        id = UUID.randomUUID().toString(),
                                        sender = MessageSender.USER,
                                        text = text,
                                        mode = mode
                                    )
                                    messages.add(userMsg)
                                    isThinking = true
                                    assistantState = AssistantState.THINKING

                                    // 2. Generate simulated intelligent AI response
                                    scope.launch {
                                        delay(900)
                                        isThinking = false
                                        assistantState = AssistantState.SPEAKING

                                        val replyText = generateAiResponse(text, mode, userName)
                                        val aiMsg = ChatMessage(
                                            id = UUID.randomUUID().toString(),
                                            sender = MessageSender.JARVIS,
                                            text = replyText,
                                            mode = mode,
                                            isCode = mode == ChatMode.CODE
                                        )
                                        messages.add(aiMsg)

                                        if (!silentExecution) {
                                            val vocalSnippet = if (mode == ChatMode.CODE) "Code implementation prepared, Master." else replyText.take(120)
                                            app.voiceEngine.speak(vocalSnippet, personaConfig.speakingSpeed)
                                        }

                                        delay(1500)
                                        assistantState = AssistantState.IDLE
                                    }
                                },
                                onNewChat = {
                                    messages.clear()
                                    messages.add(
                                        ChatMessage(
                                            id = UUID.randomUUID().toString(),
                                            sender = MessageSender.JARVIS,
                                            text = "New neural session initialized. Standing by for instructions.",
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                },
                                onDeleteMessage = { id -> messages.removeAll { it.id == id } },
                                onRegenerateResponse = {
                                    if (messages.isNotEmpty()) {
                                        val lastUserMsg = messages.lastOrNull { it.sender == MessageSender.USER }
                                        if (lastUserMsg != null) {
                                            isThinking = true
                                            scope.launch {
                                                delay(800)
                                                isThinking = false
                                                val regenerated = "Regenerated insight: ${generateAiResponse(lastUserMsg.text, lastUserMsg.mode, userName)}"
                                                messages.add(
                                                    ChatMessage(
                                                        id = UUID.randomUUID().toString(),
                                                        sender = MessageSender.JARVIS,
                                                        text = regenerated,
                                                        mode = lastUserMsg.mode
                                                    )
                                                )
                                            }
                                        }
                                    }
                                },
                                onSpeakMessage = { text ->
                                    app.voiceEngine.speak(text, personaConfig.speakingSpeed)
                                },
                                onVoiceMicTapped = {
                                    showVoiceSheet = true
                                    assistantState = AssistantState.LISTENING
                                    app.voiceEngine.playActivationChime()
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
                                onClearChatHistory = { messages.clear() }
                            )
                        }

                        composable(NavDestination.UPDATES.route) {
                            UpdatesScreen()
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
                            }
                            showVoiceSheet = true
                            assistantState = AssistantState.LISTENING
                            app.voiceEngine.playActivationChime()
                        },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )

                    // Task HUD Overlay for autonomous verified executions
                    TaskHudOverlay(
                        state = taskHudState,
                        onCancelTask = {
                            taskHudState = taskHudState.copy(isActive = false, status = TaskStatus.PAUSED)
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
                                app.voiceEngine.stop()
                            },
                            onQuickPromptSelected = { prompt ->
                                voiceTranscript = prompt
                                assistantState = AssistantState.PROCESSING
                                scope.launch {
                                    delay(700)
                                    assistantState = AssistantState.SPEAKING
                                    voiceAiResponse = executeVoicePromptAction(prompt) { hud ->
                                        taskHudState = hud
                                    }
                                    if (!silentExecution) {
                                        app.voiceEngine.speak(voiceAiResponse, personaConfig.speakingSpeed)
                                    }
                                    delay(1800)
                                    assistantState = AssistantState.IDLE
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun generateAiResponse(prompt: String, mode: ChatMode, userName: String): String {
        val lower = prompt.lowercase()
        return when {
            mode == ChatMode.CODE -> {
                "```kotlin\n// Architecture execution for: ${prompt.take(30)}\nclass JarvisNeuralPipeline {\n    fun execute() {\n        println(\"Executing verified task with Jetpack Compose 3D Canvas...\")\n    }\n}\n```"
            }
            lower.contains("battery") || lower.contains("health") || lower.contains("telemetry") -> {
                val t = app.automationController.getDeviceTelemetry()
                "Telemetry report for $userName: Battery is at ${t.batteryLevel}% (${if (t.isCharging) "Charging" else "Discharging"}). Network interface connected via ${t.networkName}."
            }
            lower.contains("memory") || lower.contains("recall") -> {
                val mems = app.memoryRepository.memories.value
                "Recalled ${mems.size} core memory entries: 1) ${mems.firstOrNull()?.content ?: "Master protocol active."}"
            }
            lower.contains("whatsapp") -> {
                "WhatsApp alias resolver verified: 'Sarah' maps to lead contact (+15550199). Would you like me to initiate the confirmed transmission?"
            }
            else -> {
                "Understood, $userName. I have logged your request. Atmospheric and neural systems are calibrated at 100% efficiency."
            }
        }
    }

    private fun executeVoicePromptAction(prompt: String, onHudUpdate: (TaskHudState) -> Unit): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("battery") -> {
                val telemetry = app.automationController.getDeviceTelemetry()
                "Battery is currently at ${telemetry.batteryLevel}%, connected to ${telemetry.networkName}."
            }
            lower.contains("whatsapp") -> {
                onHudUpdate(
                    TaskHudState(
                        taskName = "WhatsApp Comms: Sarah",
                        currentOperation = "Step 2: Resolving alias and inspecting accessibility tree...",
                        completedSteps = listOf("Contact alias resolved (+15550199)", "Permissions verified"),
                        currentStepIndex = 3,
                        percentage = 75,
                        status = TaskStatus.EXECUTING,
                        needsVerification = true,
                        isActive = true
                    )
                )
                app.automationController.sendWhatsAppMessage("+15550199", "Voice dispatch from Master.")
                "Opening WhatsApp for verified transmission to Sarah."
            }
            lower.contains("spotify") -> {
                app.automationController.searchSpotify("Synthwave Focus")
                "Streaming synthwave focus track on Spotify."
            }
            else -> {
                "Processing vocal command: $prompt. Neural core updated."
            }
        }
    }
}
