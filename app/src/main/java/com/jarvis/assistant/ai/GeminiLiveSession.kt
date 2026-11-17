package com.jarvis.assistant.ai

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.util.concurrent.TimeUnit

import com.jarvis.assistant.ai.gemini.GeminiKeyValidator

class GeminiLiveSession(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var webSocket: WebSocket? = null
    private var recordingJob: Job? = null
    private var isConnected = false

    var onTranscriptionReceived: ((String) -> Unit)? = null
    var onAiTextReceived: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun connect(apiKey: String, model: String = "gemini-1.5-flash") {
        val cleanedKey = GeminiKeyValidator.cleanKey(apiKey)
        if (cleanedKey.isBlank()) {
            onError?.invoke("Gemini API key is required to establish Live WebSocket stream.")
            return
        }

        val cleanModel = model.trim().removePrefix("models/").ifBlank { "gemini-1.5-flash" }
        // Gemini Live Realtime Bidi WebSocket endpoint
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent?key=$cleanedKey"
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                onConnectionStateChanged?.invoke(true)

                // Send setup handshake configuration
                val setupJson = JSONObject().apply {
                    put("setup", JSONObject().apply {
                        put("model", "models/$cleanModel")
                        put("generationConfig", JSONObject().apply {
                            put("responseModalities", "TEXT")
                        })
                    })
                }
                webSocket.send(setupJson.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val root = JSONObject(text)
                    val serverContent = root.optJSONObject("serverContent")
                    val modelTurn = serverContent?.optJSONObject("modelTurn")
                    val parts = modelTurn?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val replyText = parts.getJSONObject(0).optString("text", "")
                        if (replyText.isNotEmpty()) {
                            onAiTextReceived?.invoke(replyText)
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient non-JSON frame
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                onConnectionStateChanged?.invoke(false)
                onError?.invoke("Live connection failure: ${t.localizedMessage ?: "WebSocket disconnected"}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                isConnected = false
                onConnectionStateChanged?.invoke(false)
            }
        })
    }

    fun startAudioStream() {
        if (recordingJob?.isActive == true) return

        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_IN_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                val bufferSize = (minBufferSize * 2).coerceAtLeast(1024)

                val audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    onError?.invoke("Microphone initialization restricted by Android system.")
                    return@launch
                }

                audioRecord.startRecording()
                val audioBuffer = ByteArray(bufferSize)

                while (isActive && isConnected) {
                    val bytesRead = audioRecord.read(audioBuffer, 0, audioBuffer.size)
                    if (bytesRead > 0) {
                        // Stream realtime realtimeinput chunk to Gemini Live WebSocket
                        val audioByteString = audioBuffer.copyOf(bytesRead).toByteString()
                        val chunkObj = JSONObject().apply {
                            put("realtimeInput", JSONObject().apply {
                                put("mediaChunks", org.json.JSONArray().put(JSONObject().apply {
                                    put("mimeType", "audio/pcm;rate=16000")
                                    put("data", audioByteString.base64())
                                }))
                            })
                        }
                        webSocket?.send(chunkObj.toString())
                    }
                }

                audioRecord.stop()
                audioRecord.release()
            } catch (e: SecurityException) {
                onError?.invoke("Microphone permission denied for Gemini Live.")
            } catch (e: Exception) {
                onError?.invoke("Live audio stream error: ${e.localizedMessage}")
            }
        }
    }

    fun stopAudioStream() {
        recordingJob?.cancel()
        recordingJob = null
    }

    fun disconnect() {
        stopAudioStream()
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        isConnected = false
        onConnectionStateChanged?.invoke(false)
    }
}
