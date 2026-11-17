package com.jarvis.assistant.ai.gemini

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiModelDiscovery(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) {

    suspend fun discoverLiveModels(apiKey: String): Result<List<GeminiDiscoveredModel>> = withContext(Dispatchers.IO) {
        val cleanedKey = GeminiKeyValidator.cleanKey(apiKey)
        if (cleanedKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is missing or blank."))
        }

        val discoveredList = mutableListOf<GeminiDiscoveredModel>()
        var nextPageToken: String? = null
        val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models"

        do {
            val urlBuilder = StringBuilder(baseUrl)
            urlBuilder.append("?key=").append(cleanedKey)
            if (!nextPageToken.isNullOrBlank()) {
                urlBuilder.append("&pageToken=").append(nextPageToken)
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .addHeader("x-goog-api-key", cleanedKey)
                .addHeader("Accept", "application/json")
                .get()
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    val diagnosis = GeminiErrorDiagnoser.diagnoseHttpError(
                        code = response.code,
                        responseBody = responseString,
                        modelId = "models-list-endpoint",
                        endpoint = baseUrl
                    )
                    return@withContext Result.failure(Exception("${diagnosis.errorCategory}: ${diagnosis.googleErrorMessage} (${diagnosis.suggestedCorrection})"))
                }

                val root = JSONObject(responseString)
                val modelsArray = root.optJSONArray("models") ?: JSONArray()

                for (i in 0 until modelsArray.length()) {
                    val item = modelsArray.getJSONObject(i)
                    val rawName = item.optString("name", "") // e.g. "models/gemini-1.5-flash"
                    val id = rawName.removePrefix("models/")
                    val displayName = item.optString("displayName", id)
                    val description = item.optString("description", "")
                    val inputLimit = item.optInt("inputTokenLimit", 1048576)
                    val outputLimit = item.optInt("outputTokenLimit", 8192)

                    val methodsJson = item.optJSONArray("supportedGenerationMethods") ?: JSONArray()
                    val methods = mutableListOf<String>()
                    for (j in 0 until methodsJson.length()) {
                        methods.add(methodsJson.getString(j))
                    }

                    // STRICT REQUIREMENT: Only models that support text generation (generateContent)
                    val supportsGenerateContent = methods.contains("generateContent")

                    if (supportsGenerateContent && !id.contains("embedding") && !id.contains("aqa")) {
                        val category = when {
                            id.contains("live", ignoreCase = true) -> ModelCapabilityCategory.VOICE_COMPATIBLE
                            id.contains("flash", ignoreCase = true) -> ModelCapabilityCategory.FAST_RESPONSE
                            id.contains("pro", ignoreCase = true) || id.contains("thinking", ignoreCase = true) -> ModelCapabilityCategory.REASONING
                            id.contains("code", ignoreCase = true) -> ModelCapabilityCategory.CODING
                            else -> ModelCapabilityCategory.GENERAL_CHAT
                        }

                        discoveredList.add(
                            GeminiDiscoveredModel(
                                id = id,
                                rawResourceName = rawName,
                                displayName = displayName,
                                description = description,
                                supportedGenerationMethods = methods,
                                inputTokenLimit = inputLimit,
                                outputTokenLimit = outputLimit,
                                category = category,
                                compatibility = GeminiModelCompatibility.AVAILABLE
                            )
                        )
                    }
                }

                nextPageToken = root.optString("nextPageToken", null).takeIf { !it.isNullOrBlank() }
            } catch (e: Exception) {
                val diagnosis = GeminiErrorDiagnoser.diagnoseException(e, "models-list-endpoint", baseUrl)
                return@withContext Result.failure(Exception("${diagnosis.errorCategory}: ${diagnosis.googleErrorMessage} (${diagnosis.suggestedCorrection})"))
            }
        } while (!nextPageToken.isNullOrBlank())

        // Sort models intelligently: flash models first, then pro, then others
        discoveredList.sortByDescending { model ->
            when {
                model.id.contains("2.0-flash") -> 100
                model.id.contains("1.5-flash") -> 90
                model.id.contains("1.5-pro") -> 80
                model.id.contains("flash") -> 70
                model.id.contains("pro") -> 60
                else -> 10
            }
        }

        Result.success(discoveredList)
    }
}
