package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.AiTurnMessage
import com.example.data.DeviceTelemetryRepository
import com.example.data.NovaAiRepository
import com.example.data.NovaPreferences
import com.example.data.PersonaMode
import com.example.data.PreferencesRepository
import com.example.voice.VoiceEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "NovaViewModel"

class NovaViewModel(application: Application) : AndroidViewModel(application) {

    private val telemetryRepo = DeviceTelemetryRepository(application)
    private val prefsRepo = PreferencesRepository(application)
    private val aiRepo = NovaAiRepository()
    private val voiceEngine = VoiceEngine(application)

    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val initialGreeting = application.getString(R.string.initial_greeting)

    private val _uiState = MutableStateFlow(
        NovaUiState(
            messages = listOf(
                HudChatMessage(
                    isUser = false,
                    text = initialGreeting,
                    timestampLabel = timeFormatter.format(Date()),
                    systemTag = "BOOT"
                )
            ),
            telemetry = telemetryRepo.readTelemetry(),
            isSpeechAvailable = voiceEngine.isRecognitionAvailable,
            lastSpokenOrDisplayedText = initialGreeting
        )
    )
    val uiState: StateFlow<NovaUiState> = _uiState.asStateFlow()

    private var activeAiJob: Job? = null
    private var activeAuditJob: Job? = null

    init {
        observePreferences()
        observeVoiceState()
        startTelemetryTicker()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            prefsRepo.preferencesFlow.collect { prefs ->
                voiceEngine.updateTtsConfig(prefs.speechRate, prefs.speechPitch)
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    private fun observeVoiceState() {
        viewModelScope.launch {
            combine(
                voiceEngine.isListening,
                voiceEngine.isSpeaking,
                voiceEngine.partialTranscript,
                voiceEngine.voiceAmplitude,
                voiceEngine.voiceError
            ) { listening, speaking, partial, amp, err ->
                VoiceSnapshot(listening, speaking, partial, amp, err)
            }.collect { snap ->
                _uiState.update { state ->
                    state.copy(
                        isListening = snap.isListening,
                        isSpeaking = snap.isSpeaking,
                        partialTranscript = snap.partialTranscript,
                        voiceAmplitude = snap.voiceAmplitude,
                        voiceError = snap.voiceError
                    )
                }
            }
        }
    }

    private fun startTelemetryTicker() {
        viewModelScope.launch {
            while (isActive) {
                refreshTelemetry()
                delay(5_000L)
            }
        }
    }

    fun refreshTelemetry() {
        val snapshot = telemetryRepo.readTelemetry()
        _uiState.update { it.copy(telemetry = snapshot) }
    }

    fun startVoiceListening() {
        voiceEngine.clearVoiceError()
        voiceEngine.startListening { transcript ->
            sendCommand(transcript)
        }
    }

    fun stopVoiceListening() {
        voiceEngine.stopListening()
    }

    fun clearVoiceError() {
        voiceEngine.clearVoiceError()
    }

    fun toggleSpeechPlayback(text: String? = null) {
        if (_uiState.value.isSpeaking) {
            voiceEngine.stopSpeaking()
        } else {
            val target = text ?: _uiState.value.lastSpokenOrDisplayedText
            if (target.isNotBlank()) {
                voiceEngine.speak(target)
            }
        }
    }

    fun testVoiceGreeting() {
        voiceEngine.speak(initialGreeting)
    }

    fun toggleFlashlight() {
        val next = !_uiState.value.telemetry.isFlashlightOn
        val result = telemetryRepo.setFlashlight(next)
        result.fold(
            onSuccess = { enabled ->
                refreshTelemetry()
                _uiState.update {
                    it.copy(
                        hardwareBanner = if (enabled) {
                            "Tactical flashlight ENGAGED."
                        } else {
                            "Tactical flashlight STANDBY."
                        }
                    )
                }
            },
            onFailure = { error ->
                _uiState.update {
                    it.copy(
                        hardwareBanner = error.localizedMessage ?: "Flashlight unavailable on this device."
                    )
                }
            }
        )
    }

    fun dismissHardwareBanner() {
        _uiState.update { it.copy(hardwareBanner = null) }
    }

    fun setPersonaMode(mode: PersonaMode) {
        viewModelScope.launch {
            prefsRepo.setPersonaMode(mode)
        }
    }

    fun setAutoSpeak(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAutoSpeak(enabled)
            if (!enabled) {
                voiceEngine.stopSpeaking()
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        viewModelScope.launch {
            prefsRepo.setSpeechRate(rate)
        }
    }

    fun setSpeechPitch(pitch: Float) {
        viewModelScope.launch {
            prefsRepo.setSpeechPitch(pitch)
        }
    }

    fun setAriseOverdrive(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAriseOverdrive(enabled)
        }
    }

    fun executeProtocol(protocol: NovaProtocol) {
        if (protocol.activatesOverdrive) {
            setAriseOverdrive(true)
        }
        sendCommand(protocol.commandPrompt, systemTag = protocol.code)
    }

    fun retryLastCommand() {
        val lastPrompt = (_uiState.value.aiStatus as? CoreAiStatus.Error)?.lastPrompt ?: return
        sendCommand(lastPrompt)
    }

    fun resetSession() {
        activeAiJob?.cancel()
        voiceEngine.stopSpeaking()
        aiRepo.resetSession()
        _uiState.update { state ->
            state.copy(
                messages = listOf(
                    HudChatMessage(
                        isUser = false,
                        text = initialGreeting,
                        timestampLabel = timeFormatter.format(Date()),
                        systemTag = "BOOT"
                    )
                ),
                aiStatus = CoreAiStatus.Idle,
                lastSpokenOrDisplayedText = initialGreeting
            )
        }
    }

    fun sendCommand(rawPrompt: String, systemTag: String? = null) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty()) return

        voiceEngine.stopSpeaking()
        voiceEngine.clearVoiceError()

        // Apply direct local hardware/protocol triggers if explicitly commanded
        val lower = prompt.lowercase(Locale.US)
        var actionTag = systemTag
        var localActionNote: String? = null

        if (lower.contains("flashlight") || lower.contains("torch")) {
            val turnOn = !lower.contains("off") && !lower.contains("disable")
            val result = telemetryRepo.setFlashlight(turnOn)
            localActionNote = result.fold(
                onSuccess = { enabled ->
                    actionTag = "TORCH"
                    if (enabled) "Hardware action executed: Flashlight turned ON."
                    else "Hardware action executed: Flashlight turned OFF."
                },
                onFailure = { err ->
                    "Hardware notice: Flashlight failed (${err.localizedMessage ?: "unavailable"})."
                }
            )
        } else if (lower.contains("arise") && (lower.contains("initiate") || lower.contains("activate") || lower.contains("overdrive"))) {
            setAriseOverdrive(true)
            actionTag = "ARISE"
            localActionNote = "Protocol state updated: ARISE OVERDRIVE activated."
        } else if (lower.contains("standby") && lower.contains("arise")) {
            setAriseOverdrive(false)
            actionTag = "STBY"
            localActionNote = "Protocol state updated: ARISE returned to standby."
        }

        val freshTelemetry = telemetryRepo.readTelemetry()
        val nowLabel = timeFormatter.format(Date())
        val userMsg = HudChatMessage(
            isUser = true,
            text = prompt,
            timestampLabel = nowLabel,
            systemTag = actionTag
        )

        val historyBeforeTurn = _uiState.value.messages
            .drop(1) // Exclude synthetic boot greeting from raw history
            .takeLast(12)
            .map { AiTurnMessage(isUser = it.isUser, text = it.text) }

        _uiState.update { state ->
            state.copy(
                telemetry = freshTelemetry,
                messages = state.messages + userMsg,
                aiStatus = CoreAiStatus.Thinking
            )
        }

        val promptForModel = if (localActionNote != null) {
            "$prompt\n[$localActionNote]"
        } else {
            prompt
        }

        activeAiJob?.cancel()
        activeAiJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                val prefs: NovaPreferences = _uiState.value.preferences
                withTimeout(60_000L) {
                    aiRepo.streamAssistantReply(
                        userPrompt = promptForModel,
                        telemetry = freshTelemetry,
                        mode = prefs.personaMode,
                        ariseOverdrive = prefs.ariseOverdrive,
                        history = historyBeforeTurn
                    ).collect { chunk ->
                        if (chunk.isNotEmpty()) {
                            accumulated.append(chunk)
                            _uiState.update {
                                it.copy(aiStatus = CoreAiStatus.Streaming(accumulated.toString()))
                            }
                        }
                    }
                }

                val finalReply = accumulated.toString().trim()
                if (finalReply.isBlank()) {
                    _uiState.update {
                        it.copy(
                            aiStatus = CoreAiStatus.Error(
                                message = "Nova received an empty response from Gemini.",
                                lastPrompt = prompt
                            )
                        )
                    }
                } else {
                    val replyMsg = HudChatMessage(
                        isUser = false,
                        text = finalReply,
                        timestampLabel = timeFormatter.format(Date()),
                        systemTag = actionTag ?: prefs.personaMode.tag
                    )
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + replyMsg,
                            aiStatus = CoreAiStatus.Idle,
                            lastSpokenOrDisplayedText = finalReply
                        )
                    }
                    if (_uiState.value.preferences.autoSpeak) {
                        voiceEngine.speak(finalReply)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini AI request timed out", e)
                _uiState.update {
                    it.copy(
                        aiStatus = CoreAiStatus.Error(
                            message = "Neural link timed out after 60s. Check connection and retry.",
                            lastPrompt = prompt
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini AI generation failed", e)
                _uiState.update {
                    it.copy(
                        aiStatus = CoreAiStatus.Error(
                            message = e.localizedMessage ?: "Neural link error. Please retry.",
                            lastPrompt = prompt
                        )
                    )
                }
            }
        }
    }

    fun runAiSystemAudit() {
        val freshTelemetry = telemetryRepo.readTelemetry()
        _uiState.update {
            it.copy(
                telemetry = freshTelemetry,
                auditState = AuditState.Running
            )
        }

        activeAuditJob?.cancel()
        activeAuditJob = viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    aiRepo.streamSystemAudit(freshTelemetry).collect { chunk ->
                        if (chunk.isNotEmpty()) {
                            accumulated.append(chunk)
                            _uiState.update {
                                it.copy(auditState = AuditState.Streaming(accumulated.toString()))
                            }
                        }
                    }
                }
                val report = accumulated.toString().trim()
                if (report.isBlank()) {
                    _uiState.update {
                        it.copy(auditState = AuditState.Error("Diagnostic engine returned an empty report."))
                    }
                } else {
                    _uiState.update {
                        it.copy(auditState = AuditState.Success(report))
                    }
                    if (_uiState.value.preferences.autoSpeak) {
                        voiceEngine.speak(report)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "System audit timed out", e)
                _uiState.update {
                    it.copy(auditState = AuditState.Error("System audit timed out after 60s."))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "System audit failed", e)
                _uiState.update {
                    it.copy(
                        auditState = AuditState.Error(
                            e.localizedMessage ?: "Unable to complete AI diagnostic."
                        )
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceEngine.release()
    }

    private data class VoiceSnapshot(
        val isListening: Boolean,
        val isSpeaking: Boolean,
        val partialTranscript: String,
        val voiceAmplitude: Float,
        val voiceError: String?
    )
}
