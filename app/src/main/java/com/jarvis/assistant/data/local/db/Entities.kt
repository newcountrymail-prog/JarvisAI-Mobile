package com.jarvis.assistant.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jarvis.assistant.data.model.ChatMode
import com.jarvis.assistant.data.model.MemoryCategory
import com.jarvis.assistant.data.model.MessageSender
import com.jarvis.assistant.data.model.TaskStatus

@Entity(tableName = "neural_memories")
data class NeuralMemoryEntity(
    @PrimaryKey
    val id: String,
    val category: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String = "default_thread",
    val sender: String, // "USER" or "JARVIS"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String = "JARVIS", // "JARVIS" or "CODE"
    val isCode: Boolean = false,
    val isStreaming: Boolean = false
)

@Entity(tableName = "task_history")
data class TaskHistoryEntity(
    @PrimaryKey
    val taskId: String,
    val userGoal: String,
    val currentOperation: String,
    val completedStepsJson: String, // serialized List<String>
    val status: String,
    val verificationEvidence: String,
    val startTime: Long,
    val endTime: Long = 0L,
    val isSuccess: Boolean = false
)

@Entity(tableName = "announcements")
data class AppAnnouncementEntity(
    @PrimaryKey
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val date: String,
    val statusBadge: String = "VERIFIED",
    val isRead: Boolean = false
)
