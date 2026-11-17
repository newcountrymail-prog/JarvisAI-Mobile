package com.jarvis.assistant.ai.gemini

import com.jarvis.assistant.data.repository.JarvisPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GeminiAutoConfigEngine(
    private val preferences: JarvisPreferences,
    private val discovery: GeminiModelDiscovery = GeminiModelDiscovery(),
    private val tester: GeminiCompatibilityTester = GeminiCompatibilityTester()
) {

    private val _engineState = MutableStateFlow(GeminiConfigState())
    val engineState: StateFlow<GeminiConfigState> = _engineState.asStateFlow()

    suspend fun autoConfigure(
        rawApiKey: String,
        onProgress: ((String) -> Unit)? = null
    ): Result<GeminiConfigState> = withContext(Dispatchers.IO) {
        val cleanedKey = GeminiKeyValidator.cleanKey(rawApiKey)
        if (cleanedKey.isBlank()) {
            val diagnosis = GeminiErrorDiagnosis(
                errorCategory = "EMPTY_API_KEY",
                httpStatusCode = null,
                googleErrorMessage = "No API key was provided.",
                modelId = "",
                endpoint = "",
                suggestedCorrection = "Obtain a free Gemini API key from Google AI Studio (https://aistudio.google.com/app/apikey) and enter it.",
                isRecoverable = false
            )
            _engineState.value = _engineState.value.copy(
                status = GeminiConnectionStatus.DISCONNECTED,
                lastErrorDiagnosis = diagnosis
            )
            return@withContext Result.failure(IllegalArgumentException(diagnosis.suggestedCorrection))
        }

        val masked = GeminiKeyValidator.maskKey(cleanedKey)
        onProgress?.invoke("Validating key format and querying Google Generative Language Models API...")

        _engineState.value = _engineState.value.copy(
            apiKey = cleanedKey,
            maskedKey = masked,
            status = GeminiConnectionStatus.TESTING
        )

        // 1. Discover Live Models
        val discoveryResult = discovery.discoverLiveModels(cleanedKey)
        if (discoveryResult.isFailure) {
            val ex = discoveryResult.exceptionOrNull()
            val diagnosis = GeminiErrorDiagnosis(
                errorCategory = "DISCOVERY_FAILED",
                httpStatusCode = null,
                googleErrorMessage = ex?.localizedMessage ?: "Failed to query Google Models endpoint.",
                modelId = "v1beta/models",
                endpoint = "https://generativelanguage.googleapis.com/v1beta/models",
                suggestedCorrection = "Check if your Google Cloud project has enabled Generative Language API or if the API key has restrictions.",
                isRecoverable = false
            )
            _engineState.value = _engineState.value.copy(
                status = GeminiConnectionStatus.DISCONNECTED,
                lastErrorDiagnosis = diagnosis
            )
            return@withContext Result.failure(Exception(diagnosis.googleErrorMessage))
        }

        val discovered = discoveryResult.getOrThrow()
        if (discovered.isEmpty()) {
            val diagnosis = GeminiErrorDiagnosis(
                errorCategory = "NO_MODELS_FOUND",
                httpStatusCode = 200,
                googleErrorMessage = "Zero text-generation models discovered for this credential.",
                modelId = "",
                endpoint = "https://generativelanguage.googleapis.com/v1beta/models",
                suggestedCorrection = "Ensure your API key is linked to an active AI Studio project.",
                isRecoverable = false
            )
            _engineState.value = _engineState.value.copy(
                status = GeminiConnectionStatus.DISCONNECTED,
                lastErrorDiagnosis = diagnosis
            )
            return@withContext Result.failure(Exception(diagnosis.googleErrorMessage))
        }

        onProgress?.invoke("Discovered ${discovered.size} compatible models. Probing live response compatibility...")

        // 2. Compatibility Test on Top Candidate Models
        val probedModels = tester.probeCandidateModels(
            models = discovered,
            apiKey = cleanedKey,
            maxCandidatesToTest = 5
        ) { testedModel ->
            onProgress?.invoke("Probing ${testedModel.displayName}: ${testedModel.compatibility.label} (${testedModel.latencyMs}ms)")
        }

        // 3. Automatically Select the Best Model
        val verifiedModels = probedModels.filter { it.compatibility == GeminiModelCompatibility.VERIFIED_WORKING }
        val bestModel = selectBestModel(verifiedModels, probedModels)

        val connectionStatus = if (verifiedModels.isNotEmpty()) {
            GeminiConnectionStatus.CONNECTED
        } else {
            GeminiConnectionStatus.UNVERIFIED
        }

        val updatedState = GeminiConfigState(
            apiKey = cleanedKey,
            maskedKey = masked,
            selectedModel = bestModel.id,
            status = connectionStatus,
            lastVerifiedTimestamp = System.currentTimeMillis(),
            lastMeasuredLatencyMs = bestModel.latencyMs,
            discoveredModels = probedModels,
            lastErrorDiagnosis = if (verifiedModels.isEmpty()) {
                GeminiErrorDiagnosis(
                    errorCategory = "UNVERIFIED_MODELS",
                    httpStatusCode = null,
                    googleErrorMessage = "Models discovered, but live probe handshake did not return expected confirmation.",
                    modelId = bestModel.id,
                    endpoint = "generateContent",
                    suggestedCorrection = "Review individual model error details in the diagnostics table.",
                    isRecoverable = true
                )
            } else null
        )

        _engineState.value = updatedState

        // 4. Save to Persistent Preferences
        saveConfigToPreferences(updatedState)

        onProgress?.invoke("Auto Configuration Complete: Selected ${bestModel.displayName} (${updatedState.status.label})")
        Result.success(updatedState)
    }

    companion object {
        /**
         * Automatic model selection priority:
         * 1. Verified working models.
         * 2. Low-latency fast models (e.g. gemini-2.0-flash, gemini-1.5-flash).
         * 3. Long context capabilities.
         * 4. Fallback to first available model if none succeeded.
         */
        fun selectBestModel(
            verifiedList: List<GeminiDiscoveredModel>,
            allDiscovered: List<GeminiDiscoveredModel>
        ): GeminiDiscoveredModel {
            if (verifiedList.isNotEmpty()) {
                // Prioritize fast flash models for mobile assistant responsiveness
                val preferredFlash = verifiedList.firstOrNull {
                    it.id.contains("2.0-flash", ignoreCase = true) ||
                            it.id.contains("1.5-flash", ignoreCase = true) ||
                            it.category == ModelCapabilityCategory.FAST_RESPONSE
                }
                if (preferredFlash != null) return preferredFlash

                // Otherwise pick the lowest latency verified model
                return verifiedList.minByOrNull { it.latencyMs.takeIf { lat -> lat > 0 } ?: Long.MAX_VALUE }
                    ?: verifiedList.first()
            }

            // If no model verified, pick standard flash if present in discovered list
            val defaultFlash = allDiscovered.firstOrNull { it.id.contains("flash", ignoreCase = true) }
            return defaultFlash ?: allDiscovered.firstOrNull() ?: GeminiDiscoveredModel(
                id = "gemini-1.5-flash",
                rawResourceName = "models/gemini-1.5-flash",
                displayName = "Gemini 1.5 Flash",
                description = "Default standard model",
                supportedGenerationMethods = listOf("generateContent"),
                inputTokenLimit = 1048576,
                outputTokenLimit = 8192
            )
        }
    }

    suspend fun autoRepairModel(
        apiKey: String,
        failedModelId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val discoveryRes = discovery.discoverLiveModels(apiKey)
        if (discoveryRes.isFailure) {
            return@withContext Result.failure(discoveryRes.exceptionOrNull() ?: Exception("Discovery failed during auto-repair."))
        }

        val discovered = discoveryRes.getOrThrow()
        val alternativeCandidates = discovered.filter { it.id != failedModelId }
        val probed = tester.probeCandidateModels(alternativeCandidates, apiKey, maxCandidatesToTest = 3)

        val working = probed.firstOrNull { it.compatibility == GeminiModelCompatibility.VERIFIED_WORKING }
            ?: alternativeCandidates.firstOrNull()

        if (working != null) {
            preferences.setGeminiModel(working.id)
            val currentState = _engineState.value
            _engineState.value = currentState.copy(
                selectedModel = working.id,
                discoveredModels = probed,
                status = GeminiConnectionStatus.CONNECTED
            )
            Result.success(working.id)
        } else {
            Result.failure(Exception("Self-repair could not locate another compatible working model."))
        }
    }

    private suspend fun saveConfigToPreferences(config: GeminiConfigState) {
        preferences.setGeminiModel(config.selectedModel)
        preferences.setGeminiApiKey(config.apiKey)
        preferences.setGeminiStatus(config.status.name)
        preferences.setGeminiMaskedKey(config.maskedKey)
        preferences.setGeminiLastLatency(config.lastMeasuredLatencyMs)
        preferences.setGeminiLastVerified(config.lastVerifiedTimestamp)

        // Save serialized discovered models JSON for instant offline display
        val jsonArray = JSONArray()
        for (m in config.discoveredModels) {
            jsonArray.put(JSONObject().apply {
                put("id", m.id)
                put("name", m.displayName)
                put("desc", m.description)
                put("category", m.category.name)
                put("compatibility", m.compatibility.name)
                put("latency", m.latencyMs)
            })
        }
        preferences.setGeminiCompatibleModelsJson(jsonArray.toString())
    }

    suspend fun restoreSavedState(): GeminiConfigState = withContext(Dispatchers.IO) {
        val key = preferences.getGeminiApiKeySync()
        val model = preferences.getGeminiModelSync()
        val statusStr = preferences.getGeminiStatusSync()
        val masked = preferences.getGeminiMaskedKeySync()
        val latency = preferences.getGeminiLastLatencySync()
        val lastVerified = preferences.getGeminiLastVerifiedSync()
        val modelsJson = preferences.getGeminiCompatibleModelsJsonSync()

        val status = try {
            GeminiConnectionStatus.valueOf(statusStr)
        } catch (_: Exception) {
            if (key.isNotBlank()) GeminiConnectionStatus.UNVERIFIED else GeminiConnectionStatus.DISCONNECTED
        }

        val parsedModels = mutableListOf<GeminiDiscoveredModel>()
        if (modelsJson.isNotBlank()) {
            try {
                val array = JSONArray(modelsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val cat = try { ModelCapabilityCategory.valueOf(obj.optString("category")) } catch (_: Exception) { ModelCapabilityCategory.GENERAL_CHAT }
                    val comp = try { GeminiModelCompatibility.valueOf(obj.optString("compatibility")) } catch (_: Exception) { GeminiModelCompatibility.AVAILABLE }
                    parsedModels.add(
                        GeminiDiscoveredModel(
                            id = obj.optString("id"),
                            rawResourceName = "models/${obj.optString("id")}",
                            displayName = obj.optString("name"),
                            description = obj.optString("desc"),
                            supportedGenerationMethods = listOf("generateContent"),
                            inputTokenLimit = 1048576,
                            outputTokenLimit = 8192,
                            category = cat,
                            compatibility = comp,
                            latencyMs = obj.optLong("latency", 0L)
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        val state = GeminiConfigState(
            apiKey = key,
            maskedKey = masked.ifBlank { GeminiKeyValidator.maskKey(key) },
            selectedModel = model.ifBlank { "gemini-1.5-flash" },
            status = status,
            lastVerifiedTimestamp = lastVerified,
            lastMeasuredLatencyMs = latency,
            discoveredModels = parsedModels
        )
        _engineState.value = state
        state
    }
}
