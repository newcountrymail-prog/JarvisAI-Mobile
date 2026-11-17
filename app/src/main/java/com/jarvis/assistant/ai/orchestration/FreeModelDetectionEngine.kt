package com.jarvis.assistant.ai.orchestration

/**
 * Free Model Detection & Filtering Engine.
 * Implements strict zero-cost enforcement, classifies model pricing tiers into Categories A through E,
 * and ensures that when FREE MODE ONLY is active, paid and unknown models are never used.
 */
class FreeModelDetectionEngine {

    /**
     * Determines the pricing category for a model based on its identifier, provider, and pricing metadata.
     */
    fun evaluatePricingCategory(
        providerPreset: ProviderPreset,
        modelId: String,
        promptPrice: String?,
        completionPrice: String?
    ): FreeTierCategory {
        // 1. Ollama is always verified free (runs on local device or user's local network)
        if (providerPreset == ProviderPreset.OLLAMA) {
            return FreeTierCategory.CATEGORY_A_VERIFIED_FREE
        }

        // 2. OpenRouter explicit :free tag or zero price
        if (providerPreset == ProviderPreset.OPENROUTER) {
            if (modelId.contains(":free")) {
                return FreeTierCategory.CATEGORY_A_VERIFIED_FREE
            }
            if (promptPrice != null && completionPrice != null) {
                val p = promptPrice.toDoubleOrNull() ?: -1.0
                val c = completionPrice.toDoubleOrNull() ?: -1.0
                if (p == 0.0 && c == 0.0) {
                    return FreeTierCategory.CATEGORY_A_VERIFIED_FREE
                }
                if (p > 0.0 || c > 0.0) {
                    return FreeTierCategory.CATEGORY_D_PAID
                }
            }
        }

        // 3. Google Gemini official API offers an authentic free tier (15 requests/min, 1M TPM)
        if (providerPreset == ProviderPreset.GEMINI) {
            return if (modelId.contains("flash") || modelId.contains("1.5-flash")) {
                FreeTierCategory.CATEGORY_B_FREE_TIER_QUOTA
            } else {
                FreeTierCategory.CATEGORY_B_FREE_TIER_QUOTA
            }
        }

        // 4. Check for known paid signatures
        if (modelId.contains("gpt-4") || modelId.contains("claude-3-5") || modelId.contains("o1-preview")) {
            return FreeTierCategory.CATEGORY_D_PAID
        }

        // 5. Default to Category E (Pricing Unknown) rather than assuming it is free
        return FreeTierCategory.CATEGORY_E_UNKNOWN
    }

    /**
     * Checks if a model is eligible under the current Free Mode Only policy.
     */
    fun isModelEligible(model: DiscoveredModel, freeModeOnly: Boolean): Boolean {
        if (!freeModeOnly) {
            return model.isAvailable
        }
        // In Free Mode Only: Only Category A (verified free) and Category B (free-tier quota) are allowed
        val cat = try {
            FreeTierCategory.valueOf(model.freeCategory)
        } catch (e: Exception) {
            FreeTierCategory.CATEGORY_E_UNKNOWN
        }

        val isFreeEligible = cat == FreeTierCategory.CATEGORY_A_VERIFIED_FREE ||
                cat == FreeTierCategory.CATEGORY_B_FREE_TIER_QUOTA

        return isFreeEligible && model.isAvailable
    }

    /**
     * Filters a list of models, strictly enforcing the freeModeOnly constraint.
     */
    fun filterEligibleModels(
        models: List<DiscoveredModel>,
        freeModeOnly: Boolean
    ): List<DiscoveredModel> {
        return models.filter { isModelEligible(it, freeModeOnly) }
    }

    /**
     * Finds the best verified free model from a list of discovered models.
     * Prioritizes Category A with high coding/reasoning capabilities.
     */
    fun selectBestFreeModel(models: List<DiscoveredModel>, preferCoding: Boolean = true): DiscoveredModel? {
        val freeModels = filterEligibleModels(models, freeModeOnly = true)
        if (freeModels.isEmpty()) return null

        return freeModels.maxByOrNull { model ->
            var score = 0
            if (model.freeCategory == FreeTierCategory.CATEGORY_A_VERIFIED_FREE.name) score += 50
            if (preferCoding) {
                score += model.codingSuitabilityScore
            } else {
                score += model.reasoningSuitabilityScore
            }
            if (model.capabilities.contains(ModelCapability.LONG_CONTEXT.name)) score += 10
            score
        }
    }
}
