package com.jarvis.assistant.ai.gemini

import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object GeminiErrorDiagnoser {

    fun diagnoseHttpError(
        code: Int,
        responseBody: String,
        modelId: String,
        endpoint: String
    ): GeminiErrorDiagnosis {
        val googleMessage = extractGoogleErrorMessage(responseBody)

        val (category, suggestion, isRecoverable, altModel) = when (code) {
            400 -> {
                val isSafety = googleMessage.contains("SAFETY", ignoreCase = true) ||
                        googleMessage.contains("blocked", ignoreCase = true)
                if (isSafety) {
                    Quad(
                        "CONTENT_SAFETY_RESTRICTION",
                        "The prompt was flagged or blocked by Google's safety thresholds. Rephrase your request to remove sensitive terms.",
                        false,
                        null
                    )
                } else {
                    Quad(
                        "MALFORMED_REQUEST",
                        "Invalid model parameters or request payload structure. Ensure valid system instruction format and generation configuration.",
                        false,
                        null
                    )
                }
            }
            401 -> {
                Quad(
                    "AUTHENTICATION_FAILURE",
                    "Invalid Gemini API credential. Verify that your API key is correctly copied from Google AI Studio (https://aistudio.google.com/app/apikey).",
                    false,
                    null
                )
            }
            403 -> {
                val isApiNotEnabled = googleMessage.contains("API has not been used", ignoreCase = true) ||
                        googleMessage.contains("disabled", ignoreCase = true) ||
                        googleMessage.contains("not enabled", ignoreCase = true)
                val isRestricted = googleMessage.contains("restricted", ignoreCase = true) ||
                        googleMessage.contains("unauthorized", ignoreCase = true)

                if (isApiNotEnabled) {
                    Quad(
                        "API_NOT_ENABLED",
                        "Generative Language API is not enabled for your Google Cloud project. Enable it at Google Cloud Console ➔ APIs & Services ➔ Enable 'Generative Language API'.",
                        false,
                        null
                    )
                } else if (isRestricted) {
                    Quad(
                        "KEY_RESTRICTIONS_ACTIVE",
                        "Your API key has IP, HTTP referrer, or package restrictions in Google Cloud Console preventing Android app requests.",
                        false,
                        null
                    )
                } else {
                    Quad(
                        "PERMISSION_DENIED",
                        "Access forbidden. Your Google Cloud account or API key does not have permission to query this resource.",
                        false,
                        null
                    )
                }
            }
            404 -> {
                Quad(
                    "MODEL_NOT_FOUND",
                    "Model identifier '$modelId' does not exist or has been deprecated. Run 'Discover Models' or 'Auto Configure' to switch to a supported model.",
                    true,
                    "gemini-1.5-flash"
                )
            }
            429 -> {
                val isDaily = googleMessage.contains("quota", ignoreCase = true) && googleMessage.contains("day", ignoreCase = true)
                if (isDaily) {
                    Quad(
                        "DAILY_QUOTA_EXHAUSTED",
                        "Daily request limit exceeded on free tier. Consider switching to pay-as-you-go or testing another compatible model.",
                        false,
                        null
                    )
                } else {
                    Quad(
                        "RATE_LIMIT_EXCEEDED",
                        "Rate limit reached (e.g. 15 Requests Per Minute on free tier). Backing off briefly before retrying.",
                        true,
                        null
                    )
                }
            }
            500, 503 -> {
                Quad(
                    "GOOGLE_BACKEND_OUTAGE",
                    "Google Generative Language backend is experiencing temporary disruptions. JARVIS will retry with exponential backoff.",
                    true,
                    null
                )
            }
            504 -> {
                Quad(
                    "GATEWAY_TIMEOUT",
                    "Request timed out waiting for Google servers. Check your internet latency or retry with a shorter prompt.",
                    true,
                    null
                )
            }
            else -> {
                Quad(
                    "HTTP_ERROR_$code",
                    "Unexpected HTTP response code $code from Google servers. Review diagnostic logs.",
                    false,
                    null
                )
            }
        }

        return GeminiErrorDiagnosis(
            errorCategory = category,
            httpStatusCode = code,
            googleErrorMessage = googleMessage.ifBlank { "No detailed error message returned by Google API." },
            modelId = modelId,
            endpoint = endpoint,
            suggestedCorrection = suggestion,
            isRecoverable = isRecoverable,
            suggestedAlternativeModel = altModel
        )
    }

    fun diagnoseException(
        e: Throwable,
        modelId: String,
        endpoint: String
    ): GeminiErrorDiagnosis {
        val (category, message, suggestion, recoverable) = when (e) {
            is UnknownHostException -> Quad(
                "DNS_OR_OFFLINE",
                "Cannot resolve generativelanguage.googleapis.com: ${e.message}",
                "Device has no active internet connection or DNS resolver failed. Connect to Wi-Fi or cellular network.",
                true
            )
            is ConnectException -> Quad(
                "CONNECTION_FAILED",
                "TCP connection could not be established: ${e.message}",
                "Network connection refused or firewall blocking outgoing HTTPS requests.",
                true
            )
            is SocketTimeoutException -> Quad(
                "NETWORK_TIMEOUT",
                "Network socket timed out: ${e.message}",
                "High network latency or slow connection. Verify connectivity and retry.",
                true
            )
            is SSLException -> Quad(
                "TLS_HANDSHAKE_ERROR",
                "SSL/TLS secure handshake failed: ${e.message}",
                "Check device system date/time or corporate proxy/VPN interfering with SSL certificates.",
                false
            )
            else -> Quad(
                "LOCAL_EXCEPTION",
                e.localizedMessage ?: "Unknown exception occurred during API dispatch.",
                "Inspect local device state and ensure valid permissions.",
                false
            )
        }

        return GeminiErrorDiagnosis(
            errorCategory = category,
            httpStatusCode = null,
            googleErrorMessage = message,
            modelId = modelId,
            endpoint = endpoint,
            suggestedCorrection = suggestion,
            isRecoverable = recoverable
        )
    }

    private fun extractGoogleErrorMessage(body: String): String {
        return try {
            val root = JSONObject(body)
            val errObj = root.optJSONObject("error")
            errObj?.optString("message", "") ?: ""
        } catch (_: Exception) {
            body.take(200)
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
