package com.example.ui.viewmodel

import com.example.data.NovaPreferences
import com.example.data.SystemTelemetry
import java.util.UUID

sealed interface CoreAiStatus {
    data object Idle : CoreAiStatus
    data object Thinking : CoreAiStatus
    data class Streaming(val partialText: String) : CoreAiStatus
    data class Error(val message: String, val lastPrompt: String? = null) : CoreAiStatus
}

sealed interface AuditState {
    data object Idle : AuditState
    data object Running : AuditState
    data class Streaming(val report: String) : AuditState
    data class Success(val report: String) : AuditState
    data class Error(val message: String) : AuditState
}

data class HudChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestampLabel: String,
    val systemTag: String? = null
)

data class NovaProtocol(
    val id: String,
    val code: String,
    val title: String,
    val subtitle: String,
    val commandPrompt: String,
    val activatesOverdrive: Boolean = false
)

enum class CoreOrbPhase {
    ONLINE,
    LISTENING,
    THINKING,
    SPEAKING,
    ERROR,
    OVERDRIVE
}

data class NovaUiState(
    val messages: List<HudChatMessage> = emptyList(),
    val aiStatus: CoreAiStatus = CoreAiStatus.Idle,
    val auditState: AuditState = AuditState.Idle,
    val telemetry: SystemTelemetry = SystemTelemetry(),
    val preferences: NovaPreferences = NovaPreferences(),
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val partialTranscript: String = "",
    val voiceAmplitude: Float = 0f,
    val voiceError: String? = null,
    val isSpeechAvailable: Boolean = true,
    val hardwareBanner: String? = null,
    val lastSpokenOrDisplayedText: String = "",
    val protocols: List<NovaProtocol> = defaultProtocols()
) {
    val orbPhase: CoreOrbPhase
        get() = when {
            isListening -> CoreOrbPhase.LISTENING
            aiStatus is CoreAiStatus.Thinking || aiStatus is CoreAiStatus.Streaming -> CoreOrbPhase.THINKING
            isSpeaking -> CoreOrbPhase.SPEAKING
            aiStatus is CoreAiStatus.Error -> CoreOrbPhase.ERROR
            preferences.ariseOverdrive -> CoreOrbPhase.OVERDRIVE
            else -> CoreOrbPhase.ONLINE
        }
}

fun defaultProtocols(): List<NovaProtocol> = listOf(
    NovaProtocol(
        id = "jarvis_check",
        code = "SYS-01",
        title = "Jarvis System Check",
        subtitle = "Full status confirmation in Nova's signature Hinglish style",
        commandPrompt = "Jarvis, is everything okay?"
    ),
    NovaProtocol(
        id = "arise_overdrive",
        code = "ARS-99",
        title = "Initiate ARISE Protocol",
        subtitle = "Switch core to Overdrive mode and arm tactical readiness",
        commandPrompt = "Initiate ARISE Protocol and confirm tactical readiness.",
        activatesOverdrive = true
    ),
    NovaProtocol(
        id = "telemetry_brief",
        code = "TEL-04",
        title = "Live Telemetry Briefing",
        subtitle = "Analyze battery, RAM, storage, and link quality right now",
        commandPrompt = "Run a live system diagnostic using my real battery, memory, storage, and network stats."
    ),
    NovaProtocol(
        id = "focus_lock",
        code = "FOC-07",
        title = "Deep Focus Protocol",
        subtitle = "Kick off a 25-minute distraction-free execution sprint",
        commandPrompt = "Activate Deep Focus Protocol. Give me a sharp tactical kickoff and 3 crisp rules for the next 25 minutes."
    ),
    NovaProtocol(
        id = "hinglish_copilot",
        code = "DEV-12",
        title = "Hinglish Tech Co-Pilot",
        subtitle = "Ask for fast engineering breakdown in English + Hindi",
        commandPrompt = "Explain how to optimize my Android phone performance and battery life in crisp Hinglish."
    )
)
