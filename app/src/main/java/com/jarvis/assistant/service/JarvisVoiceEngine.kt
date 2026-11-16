package com.jarvis.assistant.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

class JarvisVoiceEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsInitialized = false
    private val scope = CoroutineScope(Dispatchers.IO)

    // Callbacks
    var onSpeechStarted: (() -> Unit)? = null
    var onSpeechCompleted: (() -> Unit)? = null
    var onTranscriptionResult: ((String) -> Unit)? = null
    var onRmsDbChanged: ((Float) -> Unit)? = null
    var onListeningStarted: (() -> Unit)? = null
    var onListeningEnded: (() -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
        initializeSpeechRecognizer()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsInitialized = true
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

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        scope.launch(Dispatchers.Main) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        onListeningStarted?.invoke()
                    }

                    override fun onBeginningOfSpeech() {
                        // User voice interruption: stop active TTS if speaking
                        stopSpeaking()
                        onListeningStarted?.invoke()
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        onRmsDbChanged?.invoke(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        onListeningEnded?.invoke()
                    }

                    override fun onError(error: Int) {
                        val errorDesc = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client speech error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network required for cloud speech"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak clearly."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout. Tap mic to retry."
                            else -> "Recognition error (code $error)"
                        }
                        onError?.invoke(errorDesc)
                        onListeningEnded?.invoke()
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            onTranscriptionResult?.invoke(text)
                        }
                        onListeningEnded?.invoke()
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val partials = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        partials?.firstOrNull()?.let { onTranscriptionResult?.invoke(it) }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    fun startListening(language: String = "en-US") {
        stopSpeaking()
        val tag = when (language.lowercase()) {
            "hindi", "hi" -> "hi-IN"
            "hinglish" -> "hi-IN" // Android handles mixed Hindi/English acoustics well on Indian locale
            else -> "en-US"
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        scope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                onError?.invoke(e.localizedMessage ?: "Unable to activate speech recognizer.")
            }
        }
    }

    fun stopListening() {
        scope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun speak(text: String, speed: Float = 1.0f, pitch: Float = 1.0f) {
        if (!isTtsInitialized) return
        tts?.setSpeechRate(speed)
        tts?.setPitch(pitch)
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "JARVIS_SPEECH_${System.currentTimeMillis()}")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "JARVIS_SPEECH")
    }

    fun stopSpeaking() {
        tts?.stop()
        onSpeechCompleted?.invoke()
    }

    fun playActivationChime() {
        scope.launch {
            try {
                val sampleRate = 44100
                val durationMs = 220
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                val freq1 = 587.33 // D5
                val freq2 = 880.00 // A5

                for (i in 0 until numSamples) {
                    val time = i.toDouble() / sampleRate
                    val envelope = (1.0 - (i.toDouble() / numSamples))
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

        scope.launch(Dispatchers.Main) {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }
}
