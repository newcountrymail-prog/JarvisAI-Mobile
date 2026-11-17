package com.jarvis.assistant.ai.gemini

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiCompatibilityTester(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    suspend fun testSingleModel(
        model: GeminiDiscoveredModel,
        apiKey: String
    ): GeminiDiscoveredModel = withContext(Dispatchers.IO) {
        val cleanedKey = GeminiKeyValidator.cleanKey(apiKey)
        val startTime = System.currentTimeMillis()
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/${model.id}:generateContent"

        val testPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Reply with exactly: JARVIS_CONNECTION_OK")
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.0)
                put("maxOutputTokens", 16)
            })
        }

        val requestBody = testPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url("$endpoint?key=$cleanedKey")
            .addHeader("x-goog-api-key", cleanedKey)
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val root = JSONObject(bodyString)
                val candidates = root.optJSONArray("candidates")
                val text = if (candidates != null && candidates.length() > 0) {
                    val parts = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        parts.getJSONObject(0).optString("text", "")
                    } else ""
                } else ""

                val isVerified = text.isNotBlank() &&
                        (text.contains("JARVIS_CONNECTION_OK", ignoreCase = true) || text.isNotBlank())

                if (isVerified) {
                    model.copy(
                        compatibility = GeminiModelCompatibility.VERIFIED_WORKING,
                        latencyMs = latency,
                        lastTestedTime = System.currentTimeMillis(),
                        lastErrorMessage = null
                    )
                } else {
                    model.copy(
                        compatibility = GeminiModelCompatibility.CONNECTION_FAILED,
                        latencyMs = latency,
                        lastTestedTime = System.currentTimeMillis(),
                        lastErrorMessage = "Invalid or empty response structure from model."
                    )
                }
            } else {
                val diagnosis = GeminiErrorDiagnoser.diagnoseHttpError(
                    code = response.code,
                    responseBody = bodyString,
                    modelId = model.id,
                    endpoint = endpoint
                )

                val compat = when (response.code) {
                    429 -> GeminiModelCompatibility.QUOTA_EXCEEDED
                    401, 403 -> GeminiModelCompatibility.RESTRICTED
                    else -> GeminiModelCompatibility.CONNECTION_FAILED
                }

                model.copy(
                    compatibility = compat,
                    latencyMs = latency,
                    lastTestedTime = System.currentTimeMillis(),
                    lastErrorMessage = "${diagnosis.errorCategory}: ${diagnosis.googleErrorMessage}"
                )
            }
        } catch (e: Exception) {
            val diagnosis = GeminiErrorDiagnoser.diagnoseException(e, model.id, endpoint)
            model.copy(
                compatibility = GeminiModelCompatibility.CONNECTION_FAILED,
                latencyMs = System.currentTimeMillis() - startTime,
                lastTestedTime = System.currentTimeMillis(),
                lastErrorMessage = "${diagnosis.errorCategory}: ${diagnosis.googleErrorMessage}"
            )
        }
    }

    suspend fun probeCandidateModels(
        models: List<GeminiDiscoveredModel>,
        apiKey: String,
        maxCandidatesToTest: Int = 5,
        onProgress: ((GeminiDiscoveredModel) -> Unit)? = null
    ): List<GeminiDiscoveredModel> = withContext(Dispatchers.IO) {
        val candidates = models.take(maxCandidatesToTest)
        val testedResults = mutableListOf<GeminiDiscoveredModel>()

        for (candidate in candidates) {
            val updated = testSingleModel(candidate, apiKey)
            testedResults.add(updated)
            onProgress?.invoke(updated)
            // If we found a verified fast model, we have an immediate winner
            if (updated.compatibility == GeminiModelCompatibility.VERIFIED_WORKING &&
                (updated.id.contains("flash") || updated.latencyMs < 1000)
            ) {
                // Keep testing others up to maxCandidatesToTest or break if we have enough
            }
        }

        // Return combined list with tested ones updated
        models.map { original ->
            testedResults.find { it.id == original.id } ?: original
        }
    }
}
