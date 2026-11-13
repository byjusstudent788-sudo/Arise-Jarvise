package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "NovaVoiceEngine"

class VoiceEngine(private val context: Context) {

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _voiceAmplitude = MutableStateFlow(0f)
    val voiceAmplitude: StateFlow<Float> = _voiceAmplitude.asStateFlow()

    private val _voiceError = MutableStateFlow<String?>(null)
    val voiceError: StateFlow<String?> = _voiceError.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false
    private var speechRate: Float = 1.05f
    private var speechPitch: Float = 0.95f
    private var pendingUtterance: String? = null
    private var onFinalTranscript: ((String) -> Unit)? = null

    val isRecognitionAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = tts?.setLanguage(Locale.forLanguageTag("en-IN"))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.US)
                    }
                    tts?.setSpeechRate(speechRate)
                    tts?.setPitch(speechPitch)
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _isSpeaking.value = false
                        }
                    })
                    ttsReady = true
                    pendingUtterance?.let { queued ->
                        pendingUtterance = null
                        speak(queued)
                    }
                } else {
                    Log.w(TAG, "TextToSpeech initialization failed with status: $status")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create TextToSpeech engine", e)
        }
    }

    fun updateTtsConfig(rate: Float, pitch: Float) {
        speechRate = rate.coerceIn(0.7f, 1.5f)
        speechPitch = pitch.coerceIn(0.6f, 1.4f)
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(speechPitch)
    }

    fun speak(text: String) {
        val cleanText = text
            .replace(Regex("[*_`#]+"), "")
            .trim()
        if (cleanText.isEmpty()) return

        if (!ttsReady) {
            pendingUtterance = cleanText
            return
        }
        try {
            val params = Bundle()
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, UUID.randomUUID().toString())
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak failed", e)
            _isSpeaking.value = false
        }
    }

    fun stopSpeaking() {
        pendingUtterance = null
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "TTS stop failed", e)
        }
        _isSpeaking.value = false
    }

    fun startListening(onResult: (String) -> Unit) {
        if (!isRecognitionAvailable) {
            _voiceError.value = "Speech recognition service is unavailable on this device. Use text input below."
            return
        }
        stopSpeaking()
        _voiceError.value = null
        _partialTranscript.value = ""
        onFinalTranscript = onResult

        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
            speechRecognizer = recognizer

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListening.value = true
                }

                override fun onBeginningOfSpeech() {
                    _isListening.value = true
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _voiceAmplitude.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    _voiceAmplitude.value = 0f
                }

                override fun onError(error: Int) {
                    _isListening.value = false
                    _voiceAmplitude.value = 0f
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                        SpeechRecognizer.ERROR_CLIENT -> null
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice recognition network timeout."
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Didn't catch that. Tap mic and try again."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy, resetting."
                        else -> "Voice capture interrupted (code $error)."
                    }
                    if (message != null) {
                        _voiceError.value = message
                    }
                }

                override fun onResults(results: Bundle?) {
                    _isListening.value = false
                    _voiceAmplitude.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val transcript = matches?.firstOrNull()?.trim().orEmpty()
                    _partialTranscript.value = ""
                    if (transcript.isNotEmpty()) {
                        onFinalTranscript?.invoke(transcript)
                    } else {
                        _voiceError.value = "No speech detected. Try again."
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (partial.isNotBlank()) {
                        _partialTranscript.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _isListening.value = false
            _voiceError.value = "Unable to start microphone capture: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop SpeechRecognizer", e)
        }
        _isListening.value = false
        _voiceAmplitude.value = 0f
    }

    fun clearVoiceError() {
        _voiceError.value = null
    }

    fun release() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {
        }
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {
        }
    }
}
