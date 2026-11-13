package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.PersonaMode
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudDeepNavy
import com.example.ui.theme.HudSurface
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.viewmodel.NovaProtocol
import com.example.ui.viewmodel.NovaUiState
import java.util.Locale

@Composable
fun ProtocolsScreen(
    uiState: NovaUiState,
    onSelectPersona: (PersonaMode) -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onSpeechPitchChange: (Float) -> Unit,
    onTestVoice: () -> Unit,
    onExecuteProtocol: (NovaProtocol) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "protocols_header") {
                Column {
                    Text(
                        text = "NEURAL PROTOCOLS",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Configure Nova's persona, voice calibration, and one-tap activations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Persona Selection
            item(key = "persona_section_title") {
                Text(
                    text = "AI PERSONA MATRIX",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(
                items = PersonaMode.entries,
                key = { it.name }
            ) { mode ->
                val selected = uiState.preferences.personaMode == mode
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("persona_${mode.name}")
                        .clickable { onSelectPersona(mode) },
                    color = if (selected) HudSurfaceElevated else HudDeepNavy,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(
                        width = if (selected) 1.5.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else HudBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = mode.tag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = mode.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Selected persona",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Voice Synthesis Calibration
            item(key = "voice_calibration_card") {
                Surface(
                    color = HudSurface,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, HudBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "VOICE SYNTHESIS",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Switch(
                                checked = uiState.preferences.autoSpeak,
                                onCheckedChange = onToggleAutoSpeak
                            )
                        }

                        Column {
                            Text(
                                text = "SPEECH TEMPO: ${String.format(Locale.US, "%.2fx", uiState.preferences.speechRate)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = uiState.preferences.speechRate,
                                onValueChange = onSpeechRateChange,
                                valueRange = 0.75f..1.45f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Column {
                            Text(
                                text = "VOICE PITCH: ${String.format(Locale.US, "%.2fx", uiState.preferences.speechPitch)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = uiState.preferences.speechPitch,
                                onValueChange = onSpeechPitchChange,
                                valueRange = 0.7f..1.3f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        Button(
                            onClick = onTestVoice,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("test_voice_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TEST VOICE GREETING")
                        }
                    }
                }
            }

            // Executable Command Protocols
            item(key = "command_protocols_title") {
                Text(
                    text = "ONE-TAP ACTIVATION PROTOCOLS",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(
                items = uiState.protocols,
                key = { it.id }
            ) { protocol ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("protocol_${protocol.id}")
                        .clickable { onExecuteProtocol(protocol) },
                    color = HudDeepNavy,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, HudBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = protocol.code,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = protocol.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = protocol.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Execute ${protocol.title}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
