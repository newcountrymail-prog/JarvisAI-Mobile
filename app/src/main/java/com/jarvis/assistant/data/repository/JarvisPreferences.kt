package com.jarvis.assistant.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jarvis.assistant.data.model.AiPersonaConfig
import com.jarvis.assistant.data.model.AiProviderType
import com.jarvis.assistant.data.model.AvatarType
import com.jarvis.assistant.data.model.DockEdge
import com.jarvis.assistant.data.model.OrbCustomizationConfig
import com.jarvis.assistant.data.model.OverlayAvatarConfig
import com.jarvis.assistant.data.model.PersonalityType
import com.jarvis.assistant.data.model.ThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

        // Active Provider Selection
        val KEY_ACTIVE_PROVIDER = stringPreferencesKey("active_ai_provider")
        val KEY_CODING_PROVIDER = stringPreferencesKey("coding_ai_provider")
        val KEY_VOICE_PROVIDER = stringPreferencesKey("voice_ai_provider")
        val KEY_BACKUP_PROVIDER = stringPreferencesKey("backup_ai_provider")
        val KEY_AUTO_FALLBACK = booleanPreferencesKey("auto_fallback_enabled")

        // 1. Google Gemini
        val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_GEMINI_STATUS = stringPreferencesKey("gemini_connection_status")
        val KEY_GEMINI_MASKED_KEY = stringPreferencesKey("gemini_masked_key")
        val KEY_GEMINI_LAST_LATENCY = longPreferencesKey("gemini_last_latency_ms")
        val KEY_GEMINI_LAST_VERIFIED = longPreferencesKey("gemini_last_verified_timestamp")
        val KEY_GEMINI_COMPATIBLE_MODELS = stringPreferencesKey("gemini_compatible_models_json")

        // 2. OpenRouter
        val KEY_OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val KEY_OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")

        // 3. Qwen Coder AI
        val KEY_QWEN_API_KEY = stringPreferencesKey("qwen_api_key")
        val KEY_QWEN_MODEL = stringPreferencesKey("qwen_model")
        val KEY_QWEN_BASE_URL = stringPreferencesKey("qwen_base_url")

        // 4. Custom OpenAI
        val KEY_CUSTOM_API_KEY = stringPreferencesKey("custom_openai_key")
        val KEY_CUSTOM_BASE_URL = stringPreferencesKey("custom_openai_base_url")
        val KEY_CUSTOM_MODEL = stringPreferencesKey("custom_openai_model")

        // Voice & Male Voice Configuration
        val KEY_PERSONA_NAME = stringPreferencesKey("persona_name")
        val KEY_PERSONA_TYPE = stringPreferencesKey("persona_type")
        val KEY_PERSONA_INSTRUCTIONS = stringPreferencesKey("persona_instructions")
        val KEY_PERSONA_VOICE = stringPreferencesKey("persona_voice")
        val KEY_PERSONA_SPEED = floatPreferencesKey("persona_speed")
        val KEY_PERSONA_PITCH = floatPreferencesKey("persona_pitch")
        val KEY_MALE_VOICE_PRESET = stringPreferencesKey("male_voice_preset")

        // Orb Customization
        val KEY_ORB_BATTERY_SAVER = booleanPreferencesKey("orb_battery_saver")
        val KEY_ORB_PARTICLE_DENSITY = floatPreferencesKey("orb_particle_density")
        val KEY_ORB_SIZE_SCALE = floatPreferencesKey("orb_size_scale")
        val KEY_ORB_SPEED_SCALE = floatPreferencesKey("orb_speed_scale")
        val KEY_ORB_AUDIO_REACTIVE = booleanPreferencesKey("orb_audio_reactive")

        // Theme & Overlay
        val KEY_THEME = stringPreferencesKey("theme_preset")
        val KEY_OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val KEY_OVERLAY_AVATAR = stringPreferencesKey("overlay_avatar")
        val KEY_OVERLAY_SIZE = intPreferencesKey("overlay_size")
        val KEY_OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        val KEY_OVERLAY_DOCK = stringPreferencesKey("overlay_dock")

        // Universal AI Orchestrator
        val KEY_ORCHESTRATION_PROVIDERS_JSON = stringPreferencesKey("orchestration_providers_json")
        val KEY_ORCHESTRATION_MODELS_JSON = stringPreferencesKey("orchestration_models_json")
        val KEY_FREE_MODE_ONLY = booleanPreferencesKey("orchestration_free_mode_only")
        val KEY_AUTO_SWITCHING = booleanPreferencesKey("orchestration_auto_switching")
        val KEY_AUTO_MODEL_SELECTION = booleanPreferencesKey("orchestration_auto_model_selection")
        val KEY_AUTO_FREE_DISCOVERY = booleanPreferencesKey("orchestration_auto_free_discovery")
        val KEY_CODING_OPTIMIZATION = booleanPreferencesKey("orchestration_coding_optimization")
        val KEY_MULTI_AGENT_REVIEW = booleanPreferencesKey("orchestration_multi_agent_review")
        val KEY_PAID_MODEL_ACCESS = booleanPreferencesKey("orchestration_paid_model_access")
        val KEY_MAX_SPENDING_LIMIT = floatPreferencesKey("orchestration_max_spending_limit")
        val KEY_MAX_RETRY_COUNT = intPreferencesKey("orchestration_max_retry_count")
        val KEY_ORCHESTRATION_PRIMARY_PROVIDER = stringPreferencesKey("orchestration_primary_provider")
        val KEY_ORCHESTRATION_BACKUP_PROVIDER = stringPreferencesKey("orchestration_backup_provider")
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { it[KEY_USER_NAME] ?: "Master" }
    val userRoleFlow: Flow<String> = context.dataStore.data.map { it[KEY_USER_ROLE] ?: "JARVIS OPERATOR • PRIMARY USER" }
    val voiceEnrolledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_VOICE_ENROLLED] ?: true }
    val jarvisProFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_JARVIS_PRO] ?: true }

    val silentExecutionFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_SILENT_EXECUTION] ?: false }
    val languageFlow: Flow<String> = context.dataStore.data.map { it[KEY_LANGUAGE] ?: "Hinglish" }

    // Active AI Providers
    val activeProviderFlow: Flow<String> = context.dataStore.data.map { it[KEY_ACTIVE_PROVIDER] ?: AiProviderType.GEMINI.id }
    val codingProviderFlow: Flow<String> = context.dataStore.data.map { it[KEY_CODING_PROVIDER] ?: AiProviderType.QWEN.id }
    val voiceProviderFlow: Flow<String> = context.dataStore.data.map { it[KEY_VOICE_PROVIDER] ?: AiProviderType.GEMINI.id }
    val backupProviderFlow: Flow<String> = context.dataStore.data.map { it[KEY_BACKUP_PROVIDER] ?: AiProviderType.OPENROUTER.id }
    val autoFallbackFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_FALLBACK] ?: true }

    // Gemini
    val geminiModelFlow: Flow<String> = context.dataStore.data.map { it[KEY_GEMINI_MODEL] ?: "gemini-1.5-flash" }
    val geminiApiKeyFlow: Flow<String> = context.dataStore.data.map { it[KEY_GEMINI_API_KEY] ?: "" }
    val geminiStatusFlow: Flow<String> = context.dataStore.data.map { it[KEY_GEMINI_STATUS] ?: "DISCONNECTED" }
    val geminiMaskedKeyFlow: Flow<String> = context.dataStore.data.map { it[KEY_GEMINI_MASKED_KEY] ?: "" }
    val geminiLastLatencyFlow: Flow<Long> = context.dataStore.data.map { it[KEY_GEMINI_LAST_LATENCY] ?: 0L }
    val geminiLastVerifiedFlow: Flow<Long> = context.dataStore.data.map { it[KEY_GEMINI_LAST_VERIFIED] ?: 0L }
    val geminiCompatibleModelsJsonFlow: Flow<String> = context.dataStore.data.map { it[KEY_GEMINI_COMPATIBLE_MODELS] ?: "" }

    // OpenRouter
    val openRouterApiKeyFlow: Flow<String> = context.dataStore.data.map { it[KEY_OPENROUTER_API_KEY] ?: "" }
    val openRouterModelFlow: Flow<String> = context.dataStore.data.map { it[KEY_OPENROUTER_MODEL] ?: "meta-llama/llama-3.3-70b-instruct:free" }

    // Qwen Coder AI
    val qwenApiKeyFlow: Flow<String> = context.dataStore.data.map { it[KEY_QWEN_API_KEY] ?: "" }
    val qwenModelFlow: Flow<String> = context.dataStore.data.map { it[KEY_QWEN_MODEL] ?: "qwen/qwen-2.5-coder-32b-instruct" }
    val qwenBaseUrlFlow: Flow<String> = context.dataStore.data.map { it[KEY_QWEN_BASE_URL] ?: "https://openrouter.ai/api/v1" }

    // Custom OpenAI
    val customApiKeyFlow: Flow<String> = context.dataStore.data.map { it[KEY_CUSTOM_API_KEY] ?: "" }
    val customBaseUrlFlow: Flow<String> = context.dataStore.data.map { it[KEY_CUSTOM_BASE_URL] ?: "http://10.0.2.2:11434/v1" }
    val customModelFlow: Flow<String> = context.dataStore.data.map { it[KEY_CUSTOM_MODEL] ?: "llama3.2" }

    // Theme Preset
    val themeFlow: Flow<ThemePreset> = context.dataStore.data.map {
        val name = it[KEY_THEME] ?: ThemePreset.MIDNIGHT_COMMAND.name
        try { ThemePreset.valueOf(name) } catch (e: Exception) { ThemePreset.MIDNIGHT_COMMAND }
    }

    // Persona & Voice
    val personaFlow: Flow<AiPersonaConfig> = context.dataStore.data.map {
        val typeName = it[KEY_PERSONA_TYPE] ?: PersonalityType.LOYAL_BUTLER.name
        val pType = try { PersonalityType.valueOf(typeName) } catch (e: Exception) { PersonalityType.LOYAL_BUTLER }
        AiPersonaConfig(
            assistantName = it[KEY_PERSONA_NAME] ?: "JARVIS X",
            personalityType = pType,
            customInstructions = it[KEY_PERSONA_INSTRUCTIONS] ?: "Executive AI Operator. Clear, composed, intelligent masculine delivery.",
            voiceName = it[KEY_PERSONA_VOICE] ?: "en-us-x-sfg#male",
            speakingSpeed = it[KEY_PERSONA_SPEED] ?: 1.0f
        )
    }

    // Orb Customization
    val orbConfigFlow: Flow<OrbCustomizationConfig> = context.dataStore.data.map {
        OrbCustomizationConfig(
            particleDensity = it[KEY_ORB_PARTICLE_DENSITY] ?: 1.0f,
            sizeScale = it[KEY_ORB_SIZE_SCALE] ?: 1.0f,
            rotationSpeedScale = it[KEY_ORB_SPEED_SCALE] ?: 1.0f,
            isBatterySaver = it[KEY_ORB_BATTERY_SAVER] ?: false,
            audioReactivity = it[KEY_ORB_AUDIO_REACTIVE] ?: true
        )
    }

    // Overlay Avatar
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

    // Provider Setters
    suspend fun setActiveProvider(providerId: String) = context.dataStore.edit { it[KEY_ACTIVE_PROVIDER] = providerId }
    suspend fun setCodingProvider(providerId: String) = context.dataStore.edit { it[KEY_CODING_PROVIDER] = providerId }
    suspend fun setVoiceProvider(providerId: String) = context.dataStore.edit { it[KEY_VOICE_PROVIDER] = providerId }
    suspend fun setBackupProvider(providerId: String) = context.dataStore.edit { it[KEY_BACKUP_PROVIDER] = providerId }
    suspend fun setAutoFallback(enabled: Boolean) = context.dataStore.edit { it[KEY_AUTO_FALLBACK] = enabled }

    // Gemini
    suspend fun setGeminiModel(model: String) = context.dataStore.edit { it[KEY_GEMINI_MODEL] = model }
    suspend fun setGeminiApiKey(key: String) = context.dataStore.edit { it[KEY_GEMINI_API_KEY] = key }
    suspend fun setGeminiStatus(status: String) = context.dataStore.edit { it[KEY_GEMINI_STATUS] = status }
    suspend fun setGeminiMaskedKey(maskedKey: String) = context.dataStore.edit { it[KEY_GEMINI_MASKED_KEY] = maskedKey }
    suspend fun setGeminiLastLatency(latencyMs: Long) = context.dataStore.edit { it[KEY_GEMINI_LAST_LATENCY] = latencyMs }
    suspend fun setGeminiLastVerified(timestamp: Long) = context.dataStore.edit { it[KEY_GEMINI_LAST_VERIFIED] = timestamp }
    suspend fun setGeminiCompatibleModelsJson(json: String) = context.dataStore.edit { it[KEY_GEMINI_COMPATIBLE_MODELS] = json }

    suspend fun getGeminiApiKeySync(): String = geminiApiKeyFlow.first()
    suspend fun getGeminiModelSync(): String = geminiModelFlow.first()
    suspend fun getGeminiStatusSync(): String = geminiStatusFlow.first()
    suspend fun getGeminiMaskedKeySync(): String = geminiMaskedKeyFlow.first()
    suspend fun getGeminiLastLatencySync(): Long = geminiLastLatencyFlow.first()
    suspend fun getGeminiLastVerifiedSync(): Long = geminiLastVerifiedFlow.first()
    suspend fun getGeminiCompatibleModelsJsonSync(): String = geminiCompatibleModelsJsonFlow.first()

    suspend fun clearGeminiConfig() = context.dataStore.edit {
        it.remove(KEY_GEMINI_API_KEY)
        it.remove(KEY_GEMINI_STATUS)
        it.remove(KEY_GEMINI_MASKED_KEY)
        it.remove(KEY_GEMINI_LAST_LATENCY)
        it.remove(KEY_GEMINI_LAST_VERIFIED)
        it.remove(KEY_GEMINI_COMPATIBLE_MODELS)
        it[KEY_GEMINI_MODEL] = "gemini-1.5-flash"
    }

    // OpenRouter
    suspend fun setOpenRouterApiKey(key: String) = context.dataStore.edit { it[KEY_OPENROUTER_API_KEY] = key }
    suspend fun setOpenRouterModel(model: String) = context.dataStore.edit { it[KEY_OPENROUTER_MODEL] = model }

    // Qwen
    suspend fun setQwenApiKey(key: String) = context.dataStore.edit { it[KEY_QWEN_API_KEY] = key }
    suspend fun setQwenModel(model: String) = context.dataStore.edit { it[KEY_QWEN_MODEL] = model }
    suspend fun setQwenBaseUrl(url: String) = context.dataStore.edit { it[KEY_QWEN_BASE_URL] = url }

    // Custom
    suspend fun setCustomApiKey(key: String) = context.dataStore.edit { it[KEY_CUSTOM_API_KEY] = key }
    suspend fun setCustomBaseUrl(url: String) = context.dataStore.edit { it[KEY_CUSTOM_BASE_URL] = url }
    suspend fun setCustomModel(model: String) = context.dataStore.edit { it[KEY_CUSTOM_MODEL] = model }

    // Theme & Persona
    suspend fun setThemePreset(theme: ThemePreset) = context.dataStore.edit { it[KEY_THEME] = theme.name }

    suspend fun updatePersona(config: AiPersonaConfig) = context.dataStore.edit {
        it[KEY_PERSONA_NAME] = config.assistantName
        it[KEY_PERSONA_TYPE] = config.personalityType.name
        it[KEY_PERSONA_INSTRUCTIONS] = config.customInstructions
        it[KEY_PERSONA_VOICE] = config.voiceName
        it[KEY_PERSONA_SPEED] = config.speakingSpeed
    }

    suspend fun updateOrbConfig(config: OrbCustomizationConfig) = context.dataStore.edit {
        it[KEY_ORB_BATTERY_SAVER] = config.isBatterySaver
        it[KEY_ORB_PARTICLE_DENSITY] = config.particleDensity
        it[KEY_ORB_SIZE_SCALE] = config.sizeScale
        it[KEY_ORB_SPEED_SCALE] = config.rotationSpeedScale
        it[KEY_ORB_AUDIO_REACTIVE] = config.audioReactivity
    }

    suspend fun updateOverlayConfig(config: OverlayAvatarConfig) = context.dataStore.edit {
        it[KEY_OVERLAY_ENABLED] = config.isEnabled
        it[KEY_OVERLAY_AVATAR] = config.avatarType.name
        it[KEY_OVERLAY_SIZE] = config.sizeDp
        it[KEY_OVERLAY_OPACITY] = config.opacity
        it[KEY_OVERLAY_DOCK] = config.dockEdge.name
    }

    // Orchestration Flows
    val orchestrationProvidersJsonFlow: Flow<String> = context.dataStore.data.map { it[KEY_ORCHESTRATION_PROVIDERS_JSON] ?: "" }
    val orchestrationModelsJsonFlow: Flow<String> = context.dataStore.data.map { it[KEY_ORCHESTRATION_MODELS_JSON] ?: "" }
    val freeModeOnlyFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_FREE_MODE_ONLY] ?: false }
    val autoSwitchingFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_SWITCHING] ?: true }
    val autoModelSelectionFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_MODEL_SELECTION] ?: true }
    val autoFreeDiscoveryFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_FREE_DISCOVERY] ?: true }
    val codingOptimizationFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_CODING_OPTIMIZATION] ?: true }
    val multiAgentReviewFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_MULTI_AGENT_REVIEW] ?: false }
    val paidModelAccessFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_PAID_MODEL_ACCESS] ?: true }
    val maxSpendingLimitFlow: Flow<Double> = context.dataStore.data.map { (it[KEY_MAX_SPENDING_LIMIT] ?: 10.0f).toDouble() }
    val maxRetryCountFlow: Flow<Int> = context.dataStore.data.map { it[KEY_MAX_RETRY_COUNT] ?: 3 }
    val orchestrationPrimaryProviderFlow: Flow<String> = context.dataStore.data.map { it[KEY_ORCHESTRATION_PRIMARY_PROVIDER] ?: "gemini" }
    val orchestrationBackupProviderFlow: Flow<String> = context.dataStore.data.map { it[KEY_ORCHESTRATION_BACKUP_PROVIDER] ?: "openrouter" }

    // Orchestration Setters
    suspend fun setOrchestrationProvidersJson(json: String) = context.dataStore.edit { it[KEY_ORCHESTRATION_PROVIDERS_JSON] = json }
    suspend fun setOrchestrationModelsJson(json: String) = context.dataStore.edit { it[KEY_ORCHESTRATION_MODELS_JSON] = json }
    suspend fun setFreeModeOnly(enabled: Boolean) = context.dataStore.edit { it[KEY_FREE_MODE_ONLY] = enabled }
    suspend fun setAutoSwitching(enabled: Boolean) = context.dataStore.edit { it[KEY_AUTO_SWITCHING] = enabled }
    suspend fun setAutoModelSelection(enabled: Boolean) = context.dataStore.edit { it[KEY_AUTO_MODEL_SELECTION] = enabled }
    suspend fun setAutoFreeDiscovery(enabled: Boolean) = context.dataStore.edit { it[KEY_AUTO_FREE_DISCOVERY] = enabled }
    suspend fun setCodingOptimization(enabled: Boolean) = context.dataStore.edit { it[KEY_CODING_OPTIMIZATION] = enabled }
    suspend fun setMultiAgentReview(enabled: Boolean) = context.dataStore.edit { it[KEY_MULTI_AGENT_REVIEW] = enabled }
    suspend fun setPaidModelAccess(enabled: Boolean) = context.dataStore.edit { it[KEY_PAID_MODEL_ACCESS] = enabled }
    suspend fun setMaxSpendingLimit(limit: Double) = context.dataStore.edit { it[KEY_MAX_SPENDING_LIMIT] = limit.toFloat() }
    suspend fun setMaxRetryCount(count: Int) = context.dataStore.edit { it[KEY_MAX_RETRY_COUNT] = count }
    suspend fun setOrchestrationPrimaryProvider(providerId: String) = context.dataStore.edit { it[KEY_ORCHESTRATION_PRIMARY_PROVIDER] = providerId }
    suspend fun setOrchestrationBackupProvider(providerId: String) = context.dataStore.edit { it[KEY_ORCHESTRATION_BACKUP_PROVIDER] = providerId }
}
