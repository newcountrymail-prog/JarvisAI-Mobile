package com.jarvis.assistant.data.repository

import android.content.Context
import com.jarvis.assistant.data.model.MemoryCategory
import com.jarvis.assistant.data.model.NeuralMemory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class NeuralMemoryRepository(private val context: Context) {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val memoryFile = File(context.filesDir, "jarvis_neural_memories.json")

    private val _memories = MutableStateFlow<List<NeuralMemory>>(emptyList())
    val memories: StateFlow<List<NeuralMemory>> = _memories.asStateFlow()

    init {
        loadMemories()
    }

    private fun loadMemories() {
        if (memoryFile.exists()) {
            try {
                val content = memoryFile.readText()
                val list = json.decodeFromString<List<NeuralMemory>>(content)
                _memories.value = list
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Pre-seed with neural facts as specified
        val defaultList = listOf(
            NeuralMemory(
                id = UUID.randomUUID().toString(),
                category = MemoryCategory.USER_PREFERENCES,
                title = "Language & Tone Preference",
                content = "Master prefers technical responses in Hinglish and refined English with high architectural precision.",
                timestamp = System.currentTimeMillis() - 86400000L * 2
            ),
            NeuralMemory(
                id = UUID.randomUUID().toString(),
                category = MemoryCategory.SAVED_FACTS,
                title = "Preferred Audio Ecosystem",
                content = "Spotify high-fidelity audio output preferred for deep focus synthwave and lo-fi work tracks.",
                timestamp = System.currentTimeMillis() - 86400000L * 4
            ),
            NeuralMemory(
                id = UUID.randomUUID().toString(),
                category = MemoryCategory.CONVERSATION_MEMORIES,
                title = "WhatsApp Contact Alias Map",
                content = "Alias 'Sarah' resolves to Core Lead (+1-555-0199) and 'Dev Team' maps to WhatsApp Engineering Group.",
                timestamp = System.currentTimeMillis() - 86400000L * 5
            ),
            NeuralMemory(
                id = UUID.randomUUID().toString(),
                category = MemoryCategory.SYSTEM_RULES,
                title = "Zero-Unverified Actions Policy",
                content = "Never report an action as finished until accessibility tree or system state verifies task success.",
                timestamp = System.currentTimeMillis() - 86400000L * 7
            ),
            NeuralMemory(
                id = UUID.randomUUID().toString(),
                category = MemoryCategory.USER_PREFERENCES,
                title = "Sunset Glassmorphism Aesthetics",
                content = "Always prioritize warm coral highlights (#F16D43), cream backgrounds (#FFF0E9), and soft blur cards.",
                timestamp = System.currentTimeMillis() - 86400000L * 8
            )
        )
        _memories.value = defaultList
        saveMemoriesToFile()
    }

    private fun saveMemoriesToFile() {
        try {
            val serialized = json.encodeToString(_memories.value)
            memoryFile.writeText(serialized)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addMemory(category: MemoryCategory, title: String, content: String) {
        val newMem = NeuralMemory(
            id = UUID.randomUUID().toString(),
            category = category,
            title = title,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        _memories.value = listOf(newMem) + _memories.value
        saveMemoriesToFile()
    }

    fun updateMemory(id: String, title: String, content: String, category: MemoryCategory) {
        _memories.value = _memories.value.map {
            if (it.id == id) it.copy(title = title, content = content, category = category) else it
        }
        saveMemoriesToFile()
    }

    fun deleteMemory(id: String) {
        _memories.value = _memories.value.filterNot { it.id == id }
        saveMemoriesToFile()
    }

    fun searchMemories(query: String, category: MemoryCategory = MemoryCategory.ALL): List<NeuralMemory> {
        return _memories.value.filter { mem ->
            val matchesCat = (category == MemoryCategory.ALL || mem.category == category)
            val matchesQuery = query.isBlank() ||
                    mem.title.contains(query, ignoreCase = true) ||
                    mem.content.contains(query, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    fun exportToJson(): String {
        return json.encodeToString(_memories.value)
    }

    fun importFromJson(jsonString: String): Boolean {
        return try {
            val list = json.decodeFromString<List<NeuralMemory>>(jsonString)
            _memories.value = list
            saveMemoriesToFile()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun clearAll() {
        _memories.value = emptyList()
        saveMemoriesToFile()
    }
}
