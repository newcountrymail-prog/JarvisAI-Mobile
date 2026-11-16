package com.jarvis.assistant.agent

import org.json.JSONArray
import org.json.JSONObject

data class AgentToolDeclaration(
    val name: String,
    val description: String,
    val parametersJson: JSONObject
)

object AgentToolRegistry {

    val tools: List<AgentToolDeclaration> = listOf(
        AgentToolDeclaration(
            name = "terminal_execute",
            description = "Plan and execute a terminal/shell command via the Autonomous Termux Engine.",
            parametersJson = JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("command", JSONObject().apply {
                        put("type", "string")
                        put("description", "Shell command or natural language instruction to execute in Termux/shell.")
                    })
                })
                put("required", JSONArray().apply { put("command") })
            }
        ),
        AgentToolDeclaration(
            name = "device_open_app",
            description = "Launch an installed Android application by name.",
            parametersJson = JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("appName", JSONObject().apply {
                        put("type", "string")
                        put("description", "Name of the application to launch (e.g. WhatsApp, YouTube, Spotify, Chrome, Settings).")
                    })
                })
                put("required", JSONArray().apply { put("appName") })
            }
        ),
        AgentToolDeclaration(
            name = "device_send_whatsapp",
            description = "Send a WhatsApp message to a phone number or named contact.",
            parametersJson = JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("target", JSONObject().apply {
                        put("type", "string")
                        put("description", "Contact name or phone number with country code.")
                    })
                    put("message", JSONObject().apply {
                        put("type", "string")
                        put("description", "Text message body to send.")
                    })
                })
                put("required", JSONArray().apply {
                    put("target")
                    put("message")
                })
            }
        ),
        AgentToolDeclaration(
            name = "device_search_youtube",
            description = "Search and stream video/music on YouTube.",
            parametersJson = JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("query", JSONObject().apply {
                        put("type", "string")
                        put("description", "Search query or video title.")
                    })
                })
                put("required", JSONArray().apply { put("query") })
            }
        ),
        AgentToolDeclaration(
            name = "device_telemetry",
            description = "Query real-time hardware status including battery percentage, charging state, WiFi/Cellular, internal storage free/used, and RAM memory.",
            parametersJson = JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject())
            }
        ),
        AgentToolDeclaration(
            name = "device_adjust_volume",
            description = "Adjust device media speaker volume (0 to 100 percent).",
            parametersJson = JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("volumePercent", JSONObject().apply {
                        put("type", "integer")
                        put("description", "Target percentage from 0 (mute) to 100 (maximum).")
                    })
                })
                put("required", JSONArray().apply { put("volumePercent") })
            }
        ),
        AgentToolDeclaration(
            name = "security_audit_device",
            description = "Execute a local defensive cybersecurity posture audit (root status, ADB state, hardware lock screen, permissions).",
            parametersJson = JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject())
            }
        ),
        AgentToolDeclaration(
            name = "security_inspect_domain",
            description = "Perform a DNS and domain lookup inspection (A records, canonical host, latency).",
            parametersJson = JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("domain", JSONObject().apply {
                        put("type", "string")
                        put("description", "Host or domain name to inspect (e.g. google.com).")
                    })
                })
                put("required", JSONArray().apply { put("domain") })
            }
        ),
        AgentToolDeclaration(
            name = "neural_memory_save",
            description = "Permanently remember a fact, preference, user rule, or note in JARVIS Neural Memory.",
            parametersJson = JSONObject().apply {
                put("type", "object")
                put("properties", JSONObject().apply {
                    put("fact", JSONObject().apply {
                        put("type", "string")
                        put("description", "Information or preference to record.")
                    })
                })
                put("required", JSONArray().apply { put("fact") })
            }
        )
    )

    /**
     * Converts tools into OpenAI-compatible functions/tools JSON schema format.
     */
    fun toOpenAiToolsJson(): JSONArray {
        val array = JSONArray()
        for (t in tools) {
            array.put(JSONObject().apply {
                put("type", "function")
                put("function", JSONObject().apply {
                    put("name", t.name)
                    put("description", t.description)
                    put("parameters", t.parametersJson)
                })
            })
        }
        return array
    }
}
