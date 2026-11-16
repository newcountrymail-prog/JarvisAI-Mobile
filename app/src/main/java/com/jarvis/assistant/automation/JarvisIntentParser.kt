package com.jarvis.assistant.automation

sealed class JarvisIntent {
    data class OpenApp(val appName: String, val packageName: String?) : JarvisIntent()
    data class CommunicationCall(val targetContact: String) : JarvisIntent()
    data class CommunicationWhatsApp(val targetContact: String, val message: String) : JarvisIntent()
    data class YouTubeAction(val query: String, val isSearch: Boolean = true) : JarvisIntent()
    data class SpotifyAction(val trackOrArtist: String) : JarvisIntent()
    data class DeviceTelemetryAction(val target: String) : JarvisIntent() // "battery", "network", "settings"
    data class MemoryRecall(val query: String) : JarvisIntent()
    data class MemorySave(val fact: String) : JarvisIntent()
    data class WeatherQuery(val location: String = "Atmosphere Core") : JarvisIntent()
    data class GeneralAiConversation(val prompt: String) : JarvisIntent()
}

object JarvisIntentParser {

    fun parse(input: String): JarvisIntent {
        val clean = input.trim().lowercase()

        // 1. Weather Intent (English, Hindi, Hinglish)
        if (clean.contains("weather") || clean.contains("mausam") || clean.contains("barish") || clean.contains("temperature") || clean.contains("aaj ka weather")) {
            return JarvisIntent.WeatherQuery()
        }

        // 2. Battery & Device Telemetry Intent
        if (clean.contains("battery") || clean.contains("charge") || clean.contains("charging") || clean.contains("power status")) {
            return JarvisIntent.DeviceTelemetryAction("battery")
        }
        if (clean.contains("network") || clean.contains("wifi") || clean.contains("internet connection")) {
            return JarvisIntent.DeviceTelemetryAction("network")
        }
        if (clean.contains("settings kholo") || clean.contains("open settings") || clean.contains("display settings") || clean.contains("brightness")) {
            return JarvisIntent.DeviceTelemetryAction("settings")
        }

        // 3. WhatsApp Intent (English, Hindi, Hinglish)
        // e.g. "Sarah ko WhatsApp message bhejo", "whatsapp pe message karo", "send message to Sarah on whatsapp"
        if (clean.contains("whatsapp")) {
            val contact = extractTargetName(clean, listOf("to", "ko", "bhejo", "send", "message"))
            val messageText = extractMessageBody(input)
            return JarvisIntent.CommunicationWhatsApp(
                targetContact = contact.ifBlank { "Sarah" },
                message = messageText.ifBlank { "Greetings from Master via Jarvis AI." }
            )
        }

        // 4. Phone Call Intent (English, Hindi, Hinglish)
        // e.g. "Amit bhaiya ko call karo", "call Sarah", "phone milao", "make a phone call"
        if (clean.contains("call karo") || clean.contains("call karna") || clean.startsWith("call ") || clean.contains("phone karo") || clean.contains("phone milao")) {
            val contact = extractTargetName(clean, listOf("call", "karo", "karna", "ko", "hai", "phone", "milao"))
            return JarvisIntent.CommunicationCall(targetContact = contact.ifBlank { "Sarah" })
        }

        // 5. YouTube Intent (English, Hindi, Hinglish)
        // e.g. "Jarvis, YouTube kholo", "youtube pe search karo", "search youtube for kotlin"
        if (clean.contains("youtube")) {
            val query = clean
                .replace("jarvis", "")
                .replace("youtube", "")
                .replace("kholo", "")
                .replace("pe", "")
                .replace("search", "")
                .replace("karo", "")
                .replace("play", "")
                .replace("for", "")
                .trim()
            return JarvisIntent.YouTubeAction(query = query.ifBlank { "trending technology" })
        }

        // 6. Spotify Intent (English, Hindi, Hinglish)
        // e.g. "Spotify pe synthwave chalao", "play music on spotify", "song chalao"
        if (clean.contains("spotify") || clean.contains("gana chalao") || clean.contains("play song") || clean.contains("music chalao")) {
            val track = clean
                .replace("jarvis", "")
                .replace("spotify", "")
                .replace("pe", "")
                .replace("chalao", "")
                .replace("play", "")
                .replace("music", "")
                .replace("song", "")
                .replace("on", "")
                .trim()
            return JarvisIntent.SpotifyAction(trackOrArtist = track.ifBlank { "Synthwave Deep Focus" })
        }

        // 7. Neural Memory Intent
        if (clean.contains("yaad karo") || clean.contains("recall") || clean.contains("remembered") || clean.contains("memories")) {
            return JarvisIntent.MemoryRecall(query = clean.replace("yaad karo", "").replace("recall", "").trim())
        }
        if (clean.startsWith("remember that") || clean.startsWith("yaad rakhna ki") || clean.contains("save memory")) {
            val fact = input.substringAfter("remember that", input).substringAfter("yaad rakhna ki", input).trim()
            return JarvisIntent.MemorySave(fact = fact)
        }

        // 8. General AI Conversation & Reasoning
        return JarvisIntent.GeneralAiConversation(prompt = input)
    }

    private fun extractTargetName(text: String, keywordsToRemove: List<String>): String {
        var result = text.replace("jarvis", "")
        for (kw in keywordsToRemove) {
            result = result.replace(Regex("\\b$kw\\b"), "")
        }
        return result.trim().capitalizeWords()
    }

    private fun extractMessageBody(rawText: String): String {
        if (rawText.contains(":")) {
            return rawText.substringAfter(":").trim()
        }
        if (rawText.contains("saying", ignoreCase = true)) {
            return rawText.substringAfter("saying", "").trim()
        }
        return ""
    }

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() } }
}
