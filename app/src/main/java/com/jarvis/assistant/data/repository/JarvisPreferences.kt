package com.jarvis.assistant.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jarvis.assistant.data.model.AiPersonaConfig
import com.jarvis.assistant.data.model.AvatarType
import com.jarvis.assistant.data.model.DockEdge
import com.jarvis.assistant.data.model.OverlayAvatarConfig
import com.jarvis.assistant.data.model.PersonalityType
import com.jarvis.assistant.data.model.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "jarvis_preferences")

class JarvisPreferences(private val context: Context) {

    companion object {
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_ROLE = stringPreferencesKey("user_role")
        val KEY_VOICE_ENROLLED = booleanPreferencesKey("voice_enrolled")
        val KEY_JARVIS_PRO = booleanPreferencesKey("jarvis_pro")

        val KEY_SILENT_EXECUTION = booleanPreferencesKey("silent_execution")
        val KEY_LANGUAGE = stringPreferencesKey("conversation_language")

        val KEY_PERSONA_NAME = stringPreferencesKey("persona_name")
        val KEY_PERSONA_TYPE = stringPreferencesKey("persona_type")
        val KEY_PERSONA_INSTRUCTIONS = stringPreferencesKey("persona_instructions")
        val KEY_PERSONA_VOICE = stringPreferencesKey("persona_voice")
        val KEY_PERSONA_SPEED = floatPreferencesKey("persona_speed")

        val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")

        val KEY_THEME = stringPreferencesKey("theme_preset")

        val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val KEY_OVERLAY_AVATAR = stringPreferencesKey("overlay_avatar")
        val KEY_OVERLAY_SIZE = intPreferencesKey("overlay_size")
        val KEY_OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        val KEY_OVERLAY_DOCK = stringPreferencesKey("overlay_dock")
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { it[KEY_USER_NAME] ?: "Master" }
    val userRoleFlow: Flow<String> = context.dataStore.data.map { it[KEY_USER_ROLE] ?: "JARVIS OPERATOR • PRIMARY USER" }
    val voiceEnrolledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_VOICE_ENROLLED] ?: true }
    val jarvisProFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_JARVIS_PRO] ?: true }

    val silentExecutionFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_SILENT_EXECUTION] ?: false }
    val languageFlow: Flow<String> = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "Hinglish" }

    val geminiModelFlow: Flow<String> = context.dataStore.data.map { it[KEY_GEMINI_MODEL] ?: "gemini-3.1-flash-live-preview" }
    val geminiApiKeyFlow: Flow<String> = context.dataStore.data.map { it[KEY_GEMINI_API_KEY] ?: "" }

    val themeFlow: Flow<ThemePreset> = context.dataStore.data.map {
        val name = it[KEY_THEME] ?: ThemePreset.SUNSET.name
        try { ThemePreset.valueOf(name) } catch (e: Exception) { ThemePreset.SUNSET }
    }

    val personaFlow: Flow<AiPersonaConfig> = context.dataStore.data.map {
        val typeName = it[KEY_PERSONA_TYPE] ?: PersonalityType.LOYAL_BUTLER.name
        val pType = try { PersonalityType.valueOf(typeName) } catch (e: Exception) { PersonalityType.LOYAL_BUTLER }
        AiPersonaConfig(
            assistantName = it[KEY_PERSONA_NAME] ?: "JARVIS",
            personalityType = pType,
            customInstructions = it[KEY_PERSONA_INSTRUCTIONS] ?: "Respond respectfully to Master. Deliver concise, actionable insights.",
            voiceName = it[KEY_PERSONA_VOICE] ?: "Charon",
            speakingSpeed = it[KEY_PERSONA_SPEED] ?: 1.0f
        )
    }

    val overlayConfigFlow: Flow<OverlayAvatarConfig> = context.dataStore.data.map {
        val aType = try { AvatarType.valueOf(it[KEY_OVERLAY_AVATAR] ?: AvatarType.MINIMAL_ORB.name) } catch (e: Exception) { AvatarType.MINIMAL_ORB }
        val dEdge = try { DockEdge.valueOf(it[KEY_OVERLAY_DOCK] ?: DockEdge.RIGHT.name) } catch (e: Exception) { DockEdge.RIGHT }
        OverlayAvatarConfig(
            isEnabled = it[KEY_OVERLAY_ENABLED] ?: false,
            avatarType = aType,
            sizeDp = it[KEY_OVERLAY_SIZE] ?: 56,
            opacity = it[KEY_OVERLAY_OPACITY] ?: 0.9f,
            dockEdge = dEdge
        )
    }

    suspend fun setUserName(name: String) = context.dataStore.edit { it[KEY_USER_NAME] = name }
    suspend fun setUserRole(role: String) = context.dataStore.edit { it[KEY_USER_ROLE] = role }
    suspend fun setVoiceEnrolled(enrolled: Boolean) = context.dataStore.edit { it[KEY_VOICE_ENROLLED] = enrolled }
    suspend fun setSilentExecution(silent: Boolean) = context.dataStore.edit { it[KEY_SILENT_EXECUTION] = silent }
    suspend fun setLanguage(lang: String) = context.dataStore.edit { it[KEY_LANGUAGE] = lang }
    suspend fun setGeminiModel(model: String) = context.dataStore.edit { it[KEY_GEMINI_MODEL] = model }
    suspend fun setGeminiApiKey(key: String) = context.dataStore.edit { it[KEY_GEMINI_API_KEY] = key }
    suspend fun setThemePreset(theme: ThemePreset) = context.dataStore.edit { it[KEY_THEME] = theme.name }

    suspend fun updatePersona(config: AiPersonaConfig) = context.dataStore.edit {
        it[KEY_PERSONA_NAME] = config.assistantName
        it[KEY_PERSONA_TYPE] = config.personalityType.name
        it[KEY_PERSONA_INSTRUCTIONS] = config.customInstructions
        it[KEY_PERSONA_VOICE] = config.voiceName
        it[KEY_PERSONA_SPEED] = config.speakingSpeed
    }

    suspend fun updateOverlayConfig(config: OverlayAvatarConfig) = context.dataStore.edit {
        it[KEY_OVERLAY_ENABLED] = config.isEnabled
        it[KEY_OVERLAY_AVATAR] = config.avatarType.name
        it[KEY_OVERLAY_SIZE] = config.sizeDp
        it[KEY_OVERLAY_OPACITY] = config.opacity
        it[KEY_OVERLAY_DOCK] = config.dockEdge.name
    }
}
