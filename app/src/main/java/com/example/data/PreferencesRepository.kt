package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.novaDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "nova_arise_prefs"
)

enum class PersonaMode(
    val displayName: String,
    val tag: String,
    val subtitle: String
) {
    HINGLISH_NOVA(
        displayName = "Nova Hinglish",
        tag = "HINGLISH",
        subtitle = "Sharp bilingual English + Hindi HUD style ('All systems online. Sab theek hai.')"
    ),
    CLASSIC_JARVIS(
        displayName = "Classic J.A.R.V.I.S.",
        tag = "JARVIS",
        subtitle = "Calm, witty, composed tactical intelligence briefing style."
    ),
    TACTICAL_ARISE(
        displayName = "ARISE Tactical",
        tag = "ARISE",
        subtitle = "High-intensity Solo-Leveling / cyberpunk command execution style."
    )
}

data class NovaPreferences(
    val personaMode: PersonaMode = PersonaMode.HINGLISH_NOVA,
    val autoSpeak: Boolean = true,
    val speechRate: Float = 1.05f,
    val speechPitch: Float = 0.95f,
    val ariseOverdrive: Boolean = false
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val PERSONA_MODE = stringPreferencesKey("persona_mode")
        val AUTO_SPEAK = booleanPreferencesKey("auto_speak")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val ARISE_OVERDRIVE = booleanPreferencesKey("arise_overdrive")
    }

    val preferencesFlow: Flow<NovaPreferences> = context.novaDataStore.data.map { prefs ->
        val personaName = prefs[Keys.PERSONA_MODE] ?: PersonaMode.HINGLISH_NOVA.name
        val personaMode = PersonaMode.entries.firstOrNull { it.name == personaName }
            ?: PersonaMode.HINGLISH_NOVA
        NovaPreferences(
            personaMode = personaMode,
            autoSpeak = prefs[Keys.AUTO_SPEAK] ?: true,
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.05f,
            speechPitch = prefs[Keys.SPEECH_PITCH] ?: 0.95f,
            ariseOverdrive = prefs[Keys.ARISE_OVERDRIVE] ?: false
        )
    }

    suspend fun setPersonaMode(mode: PersonaMode) {
        context.novaDataStore.edit { it[Keys.PERSONA_MODE] = mode.name }
    }

    suspend fun setAutoSpeak(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.AUTO_SPEAK] = enabled }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.novaDataStore.edit { it[Keys.SPEECH_RATE] = rate.coerceIn(0.7f, 1.5f) }
    }

    suspend fun setSpeechPitch(pitch: Float) {
        context.novaDataStore.edit { it[Keys.SPEECH_PITCH] = pitch.coerceIn(0.6f, 1.4f) }
    }

    suspend fun setAriseOverdrive(enabled: Boolean) {
        context.novaDataStore.edit { it[Keys.ARISE_OVERDRIVE] = enabled }
    }
}
