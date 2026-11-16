package com.jarvis.assistant

import com.jarvis.assistant.data.model.AssistantState
import com.jarvis.assistant.data.model.ChatMessage
import com.jarvis.assistant.data.model.ChatMode
import com.jarvis.assistant.data.model.MemoryCategory
import com.jarvis.assistant.data.model.MessageSender
import com.jarvis.assistant.data.model.NeuralMemory
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
}
