package com.jarvis.assistant.ai.orchestration

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.Headers
import java.util.UUID

/**
 * API Quota and Usage Intelligence Manager.
 * Analyzes rate-limit response headers, maintains an immutable usage ledger,
 * monitors quota consumption, and classifies provider health states.
 */
class UsageQuotaManager {

    private val _totalRequests = MutableStateFlow(0L)
    val totalRequests: StateFlow<Long> = _totalRequests.asStateFlow()

    private val _totalPromptTokens = MutableStateFlow(0L)
    val totalPromptTokens: StateFlow<Long> = _totalPromptTokens.asStateFlow()

    private val _totalCompletionTokens = MutableStateFlow(0L)
    val totalCompletionTokens: StateFlow<Long> = _totalCompletionTokens.asStateFlow()

    private val _totalTokens = MutableStateFlow(0L)
    val totalTokens: StateFlow<Long> = _totalTokens.asStateFlow()

    private val _estimatedTotalCostUsd = MutableStateFlow(0.0)
    val estimatedTotalCostUsd: StateFlow<Double> = _estimatedTotalCostUsd.asStateFlow()

    private val _providerQuotaStates = MutableStateFlow<Map<String, String>>(emptyMap())
    val providerQuotaStates: StateFlow<Map<String, String>> = _providerQuotaStates.asStateFlow()

    private val _usageLedger = MutableStateFlow<List<UsageLedgerEntry>>(emptyList())
    val usageLedger: StateFlow<List<UsageLedgerEntry>> = _usageLedger.asStateFlow()

    /**
     * Inspects OkHttp headers for official rate-limit metrics.
     * When headers are absent, returns "QUOTA UNKNOWN" rather than fabricating numbers.
     */
    fun extractRateLimitInfo(headers: Headers): String {
        val remainingReq = headers["x-ratelimit-remaining-requests"]
            ?: headers["x-ratelimit-remaining"]
            ?: headers["ratelimit-remaining"]

        val remainingTokens = headers["x-ratelimit-remaining-tokens"]

        val resetSeconds = headers["x-ratelimit-reset-requests"]
            ?: headers["x-ratelimit-reset"]
            ?: headers["retry-after"]

        return when {
            remainingReq != null && remainingTokens != null ->
                "Req: $remainingReq | Tokens: $remainingTokens${if (resetSeconds != null) " (Reset: ${resetSeconds}s)" else ""}"
            remainingReq != null ->
                "Remaining Req: $remainingReq${if (resetSeconds != null) " (Reset: ${resetSeconds}s)" else ""}"
            else -> "QUOTA UNKNOWN"
        }
    }

    /**
     * Records a completed API transaction in the local ledger.
     */
    fun recordTransaction(
        providerId: String,
        modelId: String,
        promptTokens: Int,
        completionTokens: Int,
        latencyMs: Long,
        rateLimitHeader: String = "QUOTA UNKNOWN",
        isFree: Boolean = true,
        estimatedCost: Double = 0.0
    ) {
        val total = promptTokens + completionTokens
        _totalRequests.value += 1
        _totalPromptTokens.value += promptTokens
        _totalCompletionTokens.value += completionTokens
        _totalTokens.value += total
        _estimatedTotalCostUsd.value += estimatedCost

        val currentMap = _providerQuotaStates.value.toMutableMap()
        currentMap[providerId] = rateLimitHeader
        _providerQuotaStates.value = currentMap

        val entry = UsageLedgerEntry(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            providerId = providerId,
            modelId = modelId,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = total,
            latencyMs = latencyMs,
            rateLimitRemaining = rateLimitHeader,
            isFree = isFree,
            estimatedCostUsd = estimatedCost
        )

        val updatedLedger = (_usageLedger.value + entry).takeLast(200)
        _usageLedger.value = updatedLedger
    }

    /**
     * Classifies HTTP error status codes into distinct architectural error categories.
     */
    fun classifyError(httpCode: Int, responseBody: String): String {
        return when (httpCode) {
            401 -> "AUTH_FAILURE: API key rejected or unauthorized."
            402 -> "BILLING_RESTRICTION: Account balance insufficient."
            403 -> "PERMISSION_DENIED: API disabled or geographic/account restriction."
            404 -> "MODEL_UNAVAILABLE: Model identifier not found or deprecated."
            429 -> {
                val lower = responseBody.lowercase()
                if (lower.contains("quota") || lower.contains("exceeded")) {
                    "DAILY_QUOTA_EXHAUSTED: Daily/monthly API quota depleted."
                } else {
                    "RATE_LIMIT_REACHED: Per-minute request or token velocity limit hit."
                }
            }
            500, 502, 503 -> "TEMPORARY_SERVICE_FAILURE: Upstream provider server outage."
            504 -> "GATEWAY_TIMEOUT: Provider did not respond in time."
            else -> "PROVIDER_ERROR_$httpCode: $responseBody"
        }
    }
}
