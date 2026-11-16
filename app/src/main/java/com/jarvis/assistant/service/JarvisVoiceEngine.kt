package com.jarvis.assistant.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

class JarvisVoiceEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.IO)

    var onSpeechStarted: (() -> Unit)? = null
    var onSpeechCompleted: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isInitialized = true
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeechStarted?.invoke()
                }

                override fun onDone(utteranceId: String?) {
                    onSpeechCompleted?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    onSpeechCompleted?.invoke()
                }
            })
        }
    }

    fun speak(text: String, speed: Float = 1.0f, pitch: Float = 1.0f) {
        if (!isInitialized) return
        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)
        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "JARVIS_SPEECH_${System.currentTimeMillis()}")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "JARVIS_SPEECH")
    }

    fun stop() {
        tts?.stop()
        onSpeechCompleted?.invoke()
    }

    /**
     * Plays a pleasant harmonic chime earcon to acknowledge voice wake / state change
     */
    fun playActivationChime() {
        scope.launch {
            try {
                val sampleRate = 44100
                val durationMs = 220
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                val freq1 = 587.33 // D5 note
                val freq2 = 880.00 // A5 note

                for (i in 0 until numSamples) {
                    val time = i.toDouble() / sampleRate
                    val envelope = (1.0 - (i.toDouble() / numSamples)) // Decay envelope
                    val sample = ((sin(2.0 * Math.PI * freq1 * time) + 0.6 * sin(2.0 * Math.PI * freq2 * time)) * envelope * 0.4 * Short.MAX_VALUE).toInt()
                    buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
