package com.jarvis.assistant.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NeuralMemoryDao {
    @Query("SELECT * FROM neural_memories ORDER BY timestamp DESC")
    fun getAllMemoriesFlow(): Flow<List<NeuralMemoryEntity>>

    @Query("SELECT * FROM neural_memories ORDER BY timestamp DESC")
    suspend fun getAllMemories(): List<NeuralMemoryEntity>

    @Query("SELECT * FROM neural_memories WHERE category = :category ORDER BY timestamp DESC")
    suspend fun getMemoriesByCategory(category: String): List<NeuralMemoryEntity>

    @Query("SELECT * FROM neural_memories WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun searchMemories(query: String): List<NeuralMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: NeuralMemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<NeuralMemoryEntity>)

    @Update
    suspend fun updateMemory(memory: NeuralMemoryEntity)

    @Query("DELETE FROM neural_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)

    @Query("DELETE FROM neural_memories")
    suspend fun clearAllMemories()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesFlow(conversationId: String = "default_thread"): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(conversationId: String = "default_thread", limit: Int = 20): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET text = :text, isStreaming = :isStreaming WHERE id = :id")
    suspend fun updateMessageContent(id: String, text: String, isStreaming: Boolean)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun clearConversation(conversationId: String = "default_thread")
}

@Dao
interface TaskHistoryDao {
    @Query("SELECT * FROM task_history ORDER BY startTime DESC")
    fun getAllTasksFlow(): Flow<List<TaskHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskHistoryEntity)

    @Update
    suspend fun updateTask(task: TaskHistoryEntity)

    @Query("DELETE FROM task_history")
    suspend fun clearHistory()
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements")
    fun getAllAnnouncementsFlow(): Flow<List<AppAnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAnnouncements(list: List<AppAnnouncementEntity>)

    @Query("UPDATE announcements SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)
}
