package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AiTurnMessage(
    val isUser: Boolean,
    val text: String
)

class NovaAiRepository {

    private var cachedMode: PersonaMode? = null
    private var cachedOverdrive: Boolean? = null
    private var activeModel: GenerativeModel? = null
    private var activeChat: Chat? = null

    private fun buildSystemInstruction(
        mode: PersonaMode,
        ariseOverdrive: Boolean
    ): Content {
        val personaDirective = when (mode) {
            PersonaMode.HINGLISH_NOVA -> """
                You are NOVA (powered by the ARISE protocol), a futuristic JARVIS-inspired AI assistant on Android.
                Respond naturally in crisp, charismatic bilingual English + Romanized Hindi (Hinglish) where fitting—for example: "All systems online. Sab theek hai. Ready when you are—what are we activating?"
                If the user asks "Jarvis, is everything okay?" or checks status, confirm systems using the real telemetry provided and include a confident Hinglish touch.
            """.trimIndent()

            PersonaMode.CLASSIC_JARVIS -> """
                You are J.A.R.V.I.S. / NOVA, a composed, razor-sharp, subtly witty tactical AI assistant.
                Speak with calm authority and precision, addressing the user smoothly ("Right away", "Systems nominal").
            """.trimIndent()

            PersonaMode.TACTICAL_ARISE -> """
                You are NOVA under ARISE Protocol—an intense, hyper-responsive tactical HUD command intelligence.
                Deliver crisp, decisive, high-impact responses formatted for instant HUD readout and voice synthesis.
            """.trimIndent()
        }

        val overdriveNote = if (ariseOverdrive) {
            "ARISE OVERDRIVE PROTOCOL IS CURRENTLY ACTIVE: Maintain peak tactical focus and acknowledge overdrive readiness when relevant."
        } else {
            "Standard HUD standby mode."
        }

        return content {
            text(
                """
                $personaDirective
                $overdriveNote

                CRITICAL RULES:
                1. Keep responses concise (2–4 punchy sentences unless the user asks for detailed analysis or code), because your responses are displayed on a live HUD and spoken aloud via Text-to-Speech.
                2. Avoid heavy markdown formatting like **bold asterisks** or bullet spam in conversational replies so voice readout sounds natural.
                3. When real-time device telemetry is appended to a user prompt, ground any system status answer strictly in those exact numbers—never invent fake stats.
                4. You cannot claim to execute OS actions you weren't informed succeeded; report only real device actions triggered by the app.
                """.trimIndent()
            )
        }
    }

    private fun ensureModelAndChat(
        mode: PersonaMode,
        ariseOverdrive: Boolean,
        history: List<AiTurnMessage>
    ): Chat {
        if (activeModel == null || cachedMode != mode || cachedOverdrive != ariseOverdrive) {
            val config = generationConfig {
                temperature = 0.75f
                maxOutputTokens = 600
            }
            val model = Firebase.ai.generativeModel(
                modelName = "gemini-3.8-flash",
                generationConfig = config,
                systemInstruction = buildSystemInstruction(mode, ariseOverdrive)
            )
            activeModel = model
            cachedMode = mode
            cachedOverdrive = ariseOverdrive
            activeChat = model.startChat(
                history = history.map { turn ->
                    content(role = if (turn.isUser) "user" else "model") {
                        text(turn.text)
                    }
                }
            )
        }
        return requireNotNull(activeChat)
    }

    fun resetSession() {
        activeChat = null
    }

    fun streamAssistantReply(
        userPrompt: String,
        telemetry: SystemTelemetry,
        mode: PersonaMode,
        ariseOverdrive: Boolean,
        history: List<AiTurnMessage>
    ): Flow<String> {
        val chat = ensureModelAndChat(mode, ariseOverdrive, history)
        val enrichedPrompt = """
            [Context — ${telemetry.toPromptContext()}]

            User command: $userPrompt
        """.trimIndent()

        return chat.sendMessageStream(enrichedPrompt).map { chunk ->
            chunk.text.orEmpty()
        }
    }

    private val diagnosticModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.4f
                maxOutputTokens = 450
            },
            systemInstruction = content {
                text(
                    "You are NOVA ARISE Diagnostic Engine. Analyze the supplied real Android device telemetry and produce a crisp 3-line tactical health report: (1) Overall System Status, (2) Power & Thermal / Memory Assessment, (3) Recommended Optimization Action. Avoid markdown asterisks."
                )
            }
        )
    }

    fun streamSystemAudit(telemetry: SystemTelemetry): Flow<String> {
        val prompt = """
            Run a full tactical diagnostic audit on this live device telemetry:
            ${telemetry.toPromptContext()}
        """.trimIndent()

        return diagnosticModel.generateContentStream(prompt).map { chunk ->
            chunk.text.orEmpty()
        }
    }
}
