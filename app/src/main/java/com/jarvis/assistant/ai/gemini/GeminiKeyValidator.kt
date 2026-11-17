package com.jarvis.assistant.ai.gemini

object GeminiKeyValidator {

    /**
     * Cleans an entered API key by:
     * 1. Trimming leading/trailing whitespace and control chars.
     * 2. Removing surrounding quotes (single, double, smart quotes) copied accidentally.
     * 3. Removing internal linebreaks or zero-width spaces.
     */
    fun cleanKey(raw: String): String {
        var cleaned = raw.trim()
            .replace("\r", "")
            .replace("\n", "")
            .replace("\u200B", "") // zero-width space
            .replace("\uFEFF", "") // byte order mark

        // Strip matching surrounding quotes if user copied e.g. "AIzaSy..." or 'AIzaSy...'
        if ((cleaned.startsWith("\"") && cleaned.endsWith("\"")) ||
            (cleaned.startsWith("'") && cleaned.endsWith("'")) ||
            (cleaned.startsWith("“") && cleaned.endsWith("”")) ||
            (cleaned.startsWith("‘") && cleaned.endsWith("’"))
        ) {
            cleaned = cleaned.substring(1, cleaned.length - 1).trim()
        }

        return cleaned
    }

    /**
     * Returns a safely masked version of the API key for display.
     * Never exposes the full credential in UI or logs.
     * Example: AIzaSy...K1a9
     */
    fun maskKey(key: String): String {
        val cleaned = cleanKey(key)
        if (cleaned.isBlank()) return "Not Configured"
        return if (cleaned.length >= 10) {
            "${cleaned.take(6)}...${cleaned.takeLast(4)}"
        } else {
            "****"
        }
    }

    /**
     * Checks whether an API key has a plausible format for Google AI Studio / Cloud keys.
     * Note: A valid format does not guarantee the key is active or authorized.
     */
    fun validateKeyFormat(key: String): Boolean {
        val cleaned = cleanKey(key)
        if (cleaned.length < 15) return false
        // Google AI Studio keys typically start with AIzaSy, but allow standard project keys too
        val allowedChars = cleaned.all { it.isLetterOrDigit() || it == '-' || it == '_' }
        return allowedChars
    }

    /**
     * Redacts any API key from log messages or diagnostic outputs.
     */
    fun redactString(input: String, apiKey: String): String {
        val cleaned = cleanKey(apiKey)
        if (cleaned.isBlank() || cleaned.length < 8) return input
        return input.replace(cleaned, maskKey(cleaned))
    }
}
