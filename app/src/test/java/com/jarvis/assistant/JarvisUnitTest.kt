package com.jarvis.assistant

import com.jarvis.assistant.ai.PersonaPromptBuilder
import com.jarvis.assistant.automation.JarvisIntent
import com.jarvis.assistant.automation.JarvisIntentParser
import com.jarvis.assistant.data.model.AiPersonaConfig
import com.jarvis.assistant.data.model.AssistantState
import com.jarvis.assistant.data.model.ChatMessage
import com.jarvis.assistant.data.model.ChatMode
import com.jarvis.assistant.data.model.MemoryCategory
import com.jarvis.assistant.data.model.MessageSender
import com.jarvis.assistant.data.model.NeuralMemory
import com.jarvis.assistant.data.model.PersonalityType
import com.jarvis.assistant.data.model.TaskHudState
import com.jarvis.assistant.data.model.TaskStatus
import com.jarvis.assistant.data.model.ThemePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
        assertEquals("Sunset Theme", ThemePreset.SUNSET.displayName)
        assertEquals("Dark Theme", ThemePreset.DARK.displayName)
        assertEquals("Light Theme", ThemePreset.LIGHT.displayName)
        assertEquals("AMOLED Black", ThemePreset.AMOLED.displayName)
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
            assistantName = "JARVIS",
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
        assertTrue(prompt.contains("JARVIS"))
        assertTrue(prompt.contains("British-style AI Butler"))
        assertTrue(prompt.contains("Formality Level: 85%"))
        assertTrue(prompt.contains("NEURAL CORE MEMORIES"))
        assertTrue(prompt.contains("Address user as Master."))
    }
}
