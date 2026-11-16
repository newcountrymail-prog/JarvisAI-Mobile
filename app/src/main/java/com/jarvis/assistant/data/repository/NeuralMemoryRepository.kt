package com.jarvis.assistant.data.repository

import com.jarvis.assistant.data.local.db.NeuralMemoryDao
import com.jarvis.assistant.data.local.db.NeuralMemoryEntity
import com.jarvis.assistant.data.model.MemoryCategory
import com.jarvis.assistant.data.model.NeuralMemory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class NeuralMemoryRepository(
    private val memoryDao: NeuralMemoryDao,
    private val scope: CoroutineScope
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    private val _memories = MutableStateFlow<List<NeuralMemory>>(emptyList())
    val memories: StateFlow<List<NeuralMemory>> = _memories.asStateFlow()

    init {
        scope.launch {
            // Pre-seed default memories if database is empty
            val existing = memoryDao.getAllMemories()
            if (existing.isEmpty()) {
                val defaultEntities = listOf(
                    NeuralMemoryEntity(
                        id = UUID.randomUUID().toString(),
                        category = MemoryCategory.USER_PREFERENCES.name,
                        title = "Language & Tone Preference",
                        content = "Master prefers technical responses in Hinglish and refined English with high architectural precision.",
                        timestamp = System.currentTimeMillis() - 86400000L * 2
                    ),
                    NeuralMemoryEntity(
                        id = UUID.randomUUID().toString(),
                        category = MemoryCategory.SAVED_FACTS.name,
                        title = "Preferred Audio Ecosystem",
                        content = "Spotify high-fidelity audio output preferred for deep focus synthwave and lo-fi work tracks.",
                        timestamp = System.currentTimeMillis() - 86400000L * 4
                    ),
                    NeuralMemoryEntity(
                        id = UUID.randomUUID().toString(),
                        category = MemoryCategory.CONVERSATION_MEMORIES.name,
                        title = "WhatsApp Contact Alias Map",
                        content = "Alias 'Sarah' resolves to Core Lead (+1-555-0199) and 'Dev Team' maps to WhatsApp Engineering Group.",
                        timestamp = System.currentTimeMillis() - 86400000L * 5
                    ),
                    NeuralMemoryEntity(
                        id = UUID.randomUUID().toString(),
                        category = MemoryCategory.SYSTEM_RULES.name,
                        title = "Zero-Unverified Actions Policy",
                        content = "Never report an action as finished until accessibility tree or system state verifies task success.",
                        timestamp = System.currentTimeMillis() - 86400000L * 7
                    ),
                    NeuralMemoryEntity(
                        id = UUID.randomUUID().toString(),
                        category = MemoryCategory.USER_PREFERENCES.name,
                        title = "Sunset Glassmorphism Aesthetics",
                        content = "Always prioritize warm coral highlights (#F16D43), cream backgrounds (#FFF0E9), and soft blur cards.",
                        timestamp = System.currentTimeMillis() - 86400000L * 8
                    )
                )
                memoryDao.insertMemories(defaultEntities)
            }

            // Observe Room flow
            memoryDao.getAllMemoriesFlow().collect { entities ->
                _memories.value = entities.map { it.toModel() }
            }
        }
    }

    suspend fun addMemory(category: MemoryCategory, title: String, content: String) = withContext(Dispatchers.IO) {
        val entity = NeuralMemoryEntity(
            id = UUID.randomUUID().toString(),
            category = category.name,
            title = title,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        memoryDao.insertMemory(entity)
    }

    suspend fun updateMemory(id: String, title: String, content: String, category: MemoryCategory) = withContext(Dispatchers.IO) {
        val entity = NeuralMemoryEntity(
            id = id,
            category = category.name,
            title = title,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        memoryDao.updateMemory(entity)
    }

    suspend fun deleteMemory(id: String) = withContext(Dispatchers.IO) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun searchMemories(query: String, category: MemoryCategory = MemoryCategory.ALL): List<NeuralMemory> = withContext(Dispatchers.IO) {
        val raw = if (query.isBlank()) {
            if (category == MemoryCategory.ALL) memoryDao.getAllMemories() else memoryDao.getMemoriesByCategory(category.name)
        } else {
            memoryDao.searchMemories(query)
        }
        raw.map { it.toModel() }.filter {
            category == MemoryCategory.ALL || it.category == category
        }
    }

    fun exportToJson(): String {
        return json.encodeToString(_memories.value)
    }

    suspend fun importFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val list = json.decodeFromString<List<NeuralMemory>>(jsonString)
            val entities = list.map {
                NeuralMemoryEntity(
                    id = it.id,
                    category = it.category.name,
                    title = it.title,
                    content = it.content,
                    timestamp = it.timestamp
                )
            }
            memoryDao.insertMemories(entities)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        memoryDao.clearAllMemories()
    }

    private fun NeuralMemoryEntity.toModel(): NeuralMemory {
        val cat = try { MemoryCategory.valueOf(category) } catch (e: Exception) { MemoryCategory.USER_PREFERENCES }
        return NeuralMemory(
            id = id,
            category = cat,
            title = title,
            content = content,
            timestamp = timestamp
        )
    }
}
