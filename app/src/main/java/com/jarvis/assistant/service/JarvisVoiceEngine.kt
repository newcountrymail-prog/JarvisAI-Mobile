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
import android.speech.tts.Voice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.sin

data class VoiceOption(
    val name: String,
    val displayName: String,
    val locale: Locale,
    val isMaleRecommended: Boolean,
    val quality: String = "Normal"
)

class JarvisVoiceEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsInitialized = false
    private val scope = CoroutineScope(Dispatchers.IO)
    private var ttsWaveJob: Job? = null

    // Real-time Audio Amplitude (0.0f - 1.0f) for 3D Orb and reactive UI
    private val _audioAmplitudeFlow = MutableStateFlow(0f)
    val audioAmplitudeFlow: StateFlow<Float> = _audioAmplitudeFlow.asStateFlow()

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
                applyOptimalMaleVoice()
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    startTtsAmplitudeSimulation()
                    onSpeechStarted?.invoke()
                }

                override fun onDone(utteranceId: String?) {
                    stopTtsAmplitudeSimulation()
                    onSpeechCompleted?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    stopTtsAmplitudeSimulation()
                    onSpeechCompleted?.invoke()
                }
            })
        }
    }

    private fun applyOptimalMaleVoice() {
        try {
            val voices = tts?.voices ?: return
            // Look for high-quality male voices in US, UK, or IN English
            val preferredMale = voices.firstOrNull { v ->
                val name = v.name.lowercase()
                (name.contains("male") || name.contains("sfg") || name.contains("rjs") || name.contains("en-us-x-iom") || name.contains("en-in-x-cxx")) &&
                        !v.isNetworkConnectionRequired && !name.contains("female")
            } ?: voices.firstOrNull { it.locale.language == "en" && it.name.lowercase().contains("male") }

            if (preferredMale != null) {
                tts?.voice = preferredMale
            }
        } catch (_: Exception) {}
    }

    fun getAvailableVoices(): List<VoiceOption> {
        val list = mutableListOf<VoiceOption>()
        try {
            val voices = tts?.voices ?: return getDefaultVoicePresets()
            for (v in voices) {
                val nameLower = v.name.lowercase()
                val isMale = nameLower.contains("male") || nameLower.contains("sfg") || nameLower.contains("rjs") ||
                        nameLower.contains("cxx") || nameLower.contains("man") || !nameLower.contains("female")
                val isTargetLocale = v.locale.language == "en" || v.locale.language == "hi"

                if (isTargetLocale) {
                    val quality = if (v.quality >= Voice.QUALITY_VERY_HIGH) "Ultra" else if (v.quality >= Voice.QUALITY_HIGH) "High" else "Standard"
                    val label = "${v.locale.displayLanguage} (${v.locale.country}) - ${if (isMale) "Masculine" else "Feminine"}"
                    list.add(
                        VoiceOption(
                            name = v.name,
                            displayName = label,
                            locale = v.locale,
                            isMaleRecommended = isMale,
                            quality = quality
                        )
                    )
                }
            }
        } catch (_: Exception) {
            return getDefaultVoicePresets()
        }

        return if (list.isEmpty()) getDefaultVoicePresets() else list.sortedByDescending { it.isMaleRecommended }
    }

    fun getDefaultVoicePresets(): List<VoiceOption> {
        return listOf(
            VoiceOption("en-us-x-sfg#male", "English (US) - Executive Male (JARVIS)", Locale.US, true, "Ultra"),
            VoiceOption("en-gb-x-rjs#male", "English (UK) - British Butler Male", Locale.UK, true, "High"),
            VoiceOption("en-in-x-cxx#male", "English (India) - Modern Indian Male", Locale("en", "IN"), true, "High"),
            VoiceOption("hi-in-x-cfc#male", "Hindi (India) - Professional Male", Locale("hi", "IN"), true, "High")
        )
    }

    fun setVoiceByName(voiceName: String): Boolean {
        try {
            val v = tts?.voices?.firstOrNull { it.name == voiceName }
            if (v != null) {
                tts?.voice = v
                return true
            }
        } catch (_: Exception) {}
        return false
    }

    fun previewVoice(voiceName: String, pitch: Float = 0.95f, speed: Float = 1.05f, text: String = "All systems operational, sir. JARVIS X online.") {
        if (!isTtsInitialized) return
        setVoiceByName(voiceName)
        speak(text, speed = speed, pitch = pitch)
    }

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        scope.launch(Dispatchers.Main) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _audioAmplitudeFlow.value = 0.05f
                        onListeningStarted?.invoke()
                    }

                    override fun onBeginningOfSpeech() {
                        stopSpeaking()
                        _audioAmplitudeFlow.value = 0.35f
                        onListeningStarted?.invoke()
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize -2dB..10dB to 0..1
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
                        _audioAmplitudeFlow.value = normalized
                        onRmsDbChanged?.invoke(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _audioAmplitudeFlow.value = 0f
                        onListeningEnded?.invoke()
                    }

                    override fun onError(error: Int) {
                        _audioAmplitudeFlow.value = 0f
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
                        _audioAmplitudeFlow.value = 0f
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

    private fun startTtsAmplitudeSimulation() {
        ttsWaveJob?.cancel()
        ttsWaveJob = scope.launch {
            var step = 0
            while (isActive) {
                // Generates smooth rhythmic organic fluctuation (speech cadence)
                val base = 0.45f + 0.35f * sin(step * 0.3).toFloat()
                val jitter = (sin(step * 0.77) * 0.15).toFloat()
                _audioAmplitudeFlow.value = (base + jitter).coerceIn(0.15f, 0.95f)
                delay(60)
                step++
            }
        }
    }

    private fun stopTtsAmplitudeSimulation() {
        ttsWaveJob?.cancel()
        ttsWaveJob = null
        _audioAmplitudeFlow.value = 0f
    }

    fun startListening(language: String = "en-US") {
        stopSpeaking()
        val tag = when (language.lowercase()) {
            "hindi", "hi" -> "hi-IN"
            "hinglish" -> "hi-IN"
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
                _audioAmplitudeFlow.value = 0f
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
        stopTtsAmplitudeSimulation()
        onSpeechCompleted?.invoke()
    }

    fun stop() {
        stopListening()
        stopSpeaking()
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
        stopTtsAmplitudeSimulation()
        tts?.stop()
        tts?.shutdown()
        tts = null

        scope.launch(Dispatchers.Main) {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
    }
}
