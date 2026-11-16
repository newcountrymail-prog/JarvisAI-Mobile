package com.jarvis.assistant.data.repository

import com.jarvis.assistant.data.local.db.ChatMessageDao
import com.jarvis.assistant.data.local.db.ChatMessageEntity
import com.jarvis.assistant.data.model.ChatMessage
import com.jarvis.assistant.data.model.ChatMode
import com.jarvis.assistant.data.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val chatDao: ChatMessageDao
) {
    fun getMessagesFlow(conversationId: String = "default_thread"): Flow<List<ChatMessage>> {
        return chatDao.getMessagesFlow(conversationId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun addMessage(
        sender: MessageSender,
        text: String,
        mode: ChatMode = ChatMode.JARVIS,
        isCode: Boolean = false,
        conversationId: String = "default_thread",
        messageId: String = UUID.randomUUID().toString()
    ): String = withContext(Dispatchers.IO) {
        val entity = ChatMessageEntity(
            id = messageId,
            conversationId = conversationId,
            sender = sender.name,
            text = text,
            timestamp = System.currentTimeMillis(),
            mode = mode.name,
            isCode = isCode,
            isStreaming = false
        )
        chatDao.insertMessage(entity)
        messageId
    }

    suspend fun updateMessageStream(id: String, text: String, isStreaming: Boolean) = withContext(Dispatchers.IO) {
        chatDao.updateMessageContent(id, text, isStreaming)
    }

    suspend fun deleteMessage(id: String) = withContext(Dispatchers.IO) {
        chatDao.deleteMessageById(id)
    }

    suspend fun clearHistory(conversationId: String = "default_thread") = withContext(Dispatchers.IO) {
        chatDao.clearConversation(conversationId)
    }

    private fun ChatMessageEntity.toModel(): ChatMessage {
        val msgSender = try { MessageSender.valueOf(sender) } catch (e: Exception) { MessageSender.JARVIS }
        val msgMode = try { ChatMode.valueOf(mode) } catch (e: Exception) { ChatMode.JARVIS }
        return ChatMessage(
            id = id,
            sender = msgSender,
            text = text,
            timestamp = timestamp,
            mode = msgMode,
            isCode = isCode,
            isStreaming = isStreaming
        )
    }
}
