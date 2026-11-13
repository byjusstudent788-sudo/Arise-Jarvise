package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.NovaHudOrb
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudDeepNavy
import com.example.ui.theme.HudSurface
import com.example.ui.theme.HudSurfaceElevated
import com.example.ui.theme.OptimalEmerald
import com.example.ui.theme.TacticalAmber
import com.example.ui.viewmodel.CoreAiStatus
import com.example.ui.viewmodel.CoreOrbPhase
import com.example.ui.viewmodel.HudChatMessage
import com.example.ui.viewmodel.NovaUiState

@Composable
fun CoreScreen(
    uiState: NovaUiState,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onOrbTap: () -> Unit,
    onSendCommand: (String) -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onToggleSpeakPlayback: () -> Unit,
    onRetryLastCommand: () -> Unit,
    onResetSession: () -> Unit,
    onClearVoiceError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.aiStatus) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    val primaryGlow = MaterialTheme.colorScheme.primary
    val secondaryGlow = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryGlow.copy(alpha = 0.14f),
                            secondaryGlow.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.24f),
                        radius = size.maxDimension * 0.55f
                    )
                )
            }
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(horizontal = 16.dp)
        ) {
            // Top Tactical HUD Header
            HudTopBar(
                uiState = uiState,
                onToggleAutoSpeak = onToggleAutoSpeak,
                onResetSession = onResetSession
            )

            // Main scrollable conversation & orb stage
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hero Neural Core Stage
                item(key = "orb_stage") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        NovaHudOrb(
                            phase = uiState.orbPhase,
                            voiceAmplitude = uiState.voiceAmplitude,
                            onClick = onOrbTap,
                            orbSize = 188.dp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        PhaseStatusBadge(
                            phase = uiState.orbPhase,
                            isOverdrive = uiState.preferences.ariseOverdrive,
                            isSpeaking = uiState.isSpeaking,
                            onStopSpeaking = onToggleSpeakPlayback
                        )

                        // Live partial voice transcript when listening
                        AnimatedVisibility(
                            visible = uiState.isListening || uiState.partialTranscript.isNotBlank(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth(),
                                color = OptimalEmerald.copy(alpha = 0.10f),
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, OptimalEmerald.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.GraphicEq,
                                        contentDescription = null,
                                        tint = OptimalEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = uiState.partialTranscript.ifBlank { "Listening… speak your command" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Voice error or Mic permission notice
                        val voiceBanner = uiState.voiceError
                        if (voiceBanner != null) {
                            Surface(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth(),
                                color = TacticalAmber.copy(alpha = 0.12f),
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, TacticalAmber.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = voiceBanner,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TacticalAmber,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = onClearVoiceError,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Dismiss voice notice",
                                            tint = TacticalAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        } else if (!hasMicPermission) {
                            Surface(
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth(),
                                color = HudSurface,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, HudBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.mic_permission_needed),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = onRequestMicPermission,
                                        modifier = Modifier.testTag("grant_mic_button")
                                    ) {
                                        Text(stringResource(R.string.grant_mic_permission))
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Prompt Chips inspired by the Short
                item(key = "quick_chips") {
                    QuickCommandStrip(
                        isOverdrive = uiState.preferences.ariseOverdrive,
                        onSelectPrompt = { onSendCommand(it) }
                    )
                }

                // Conversation Log
                items(
                    items = uiState.messages,
                    key = { it.id }
                ) { msg ->
                    HudMessageCard(
                        message = msg,
                        isLatestNovaReply = !msg.isUser && msg.id == uiState.messages.lastOrNull { !it.isUser }?.id,
                        isSpeaking = uiState.isSpeaking,
                        onToggleSpeak = onToggleSpeakPlayback
                    )
                }

                // Streaming / Thinking / Error live feedback card
                when (val status = uiState.aiStatus) {
                    is CoreAiStatus.Thinking -> {
                        item(key = "ai_thinking") {
                            StreamingHudCard(
                                text = "Synchronizing with Gemini neural core…",
                                isLoading = true
                            )
                        }
                    }

                    is CoreAiStatus.Streaming -> {
                        item(key = "ai_streaming") {
                            StreamingHudCard(
                                text = status.partialText,
                                isLoading = false
                            )
                        }
                    }

                    is CoreAiStatus.Error -> {
                        item(key = "ai_error") {
                            ErrorHudCard(
                                message = status.message,
                                onRetry = onRetryLastCommand
                            )
                        }
                    }

                    CoreAiStatus.Idle -> Unit
                }
            }

            // Bottom Command Dock
            CommandInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                isListening = uiState.isListening,
                isBusy = uiState.aiStatus is CoreAiStatus.Thinking,
                onSend = {
                    val trimmed = inputText.trim()
                    if (trimmed.isNotEmpty()) {
                        onSendCommand(trimmed)
                        inputText = ""
                    }
                },
                onMicClick = onOrbTap
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HudTopBar(
    uiState: NovaUiState,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onResetSession: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = if (uiState.telemetry.isNetworkConnected) OptimalEmerald else TacticalAmber,
                            shape = CircleShape
                        )
                )
                Text(
                    text = "NOVA // ARISE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "${uiState.preferences.personaMode.tag} • BAT ${uiState.telemetry.batteryPercent}% • ${uiState.telemetry.networkStatus}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onToggleAutoSpeak(!uiState.preferences.autoSpeak) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("toggle_voice_button")
            ) {
                Icon(
                    imageVector = if (uiState.preferences.autoSpeak) {
                        Icons.AutoMirrored.Filled.VolumeUp
                    } else {
                        Icons.AutoMirrored.Filled.VolumeOff
                    },
                    contentDescription = stringResource(R.string.cd_toggle_voice),
                    tint = if (uiState.preferences.autoSpeak) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            IconButton(
                onClick = onResetSession,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("reset_session_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.cd_clear_session),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PhaseStatusBadge(
    phase: CoreOrbPhase,
    isOverdrive: Boolean,
    isSpeaking: Boolean,
    onStopSpeaking: () -> Unit
) {
    val label = when (phase) {
        CoreOrbPhase.LISTENING -> stringResource(R.string.status_listening)
        CoreOrbPhase.THINKING -> stringResource(R.string.status_thinking)
        CoreOrbPhase.SPEAKING -> stringResource(R.string.status_speaking)
        CoreOrbPhase.OVERDRIVE -> stringResource(R.string.status_arise)
        CoreOrbPhase.ERROR -> "DIAGNOSTIC ALERT"
        CoreOrbPhase.ONLINE -> if (isOverdrive) stringResource(R.string.status_arise) else stringResource(R.string.status_online)
    }
    val badgeColor = when (phase) {
        CoreOrbPhase.LISTENING -> OptimalEmerald
        CoreOrbPhase.ERROR -> AlertCrimson
        CoreOrbPhase.OVERDRIVE -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = badgeColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isOverdrive) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = badgeColor
                )
            }
        }

        if (isSpeaking) {
            OutlinedButton(
                onClick = onStopSpeaking,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = badgeColor),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = stringResource(R.string.cd_stop_speech),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("MUTE", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun QuickCommandStrip(
    isOverdrive: Boolean,
    onSelectPrompt: (String) -> Unit
) {
    val prompts = listOf(
        "Jarvis, is everything okay?" to "Jarvis, is everything okay?",
        (if (isOverdrive) "Return ARISE to standby" else "Initiate ARISE Protocol") to
            (if (isOverdrive) "Return ARISE to standby." else "Initiate ARISE Protocol!"),
        "System diagnostic" to "Run a live system diagnostic on my battery, RAM, storage, and network.",
        "Flashlight ON" to "Turn on tactical flashlight."
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        prompts.forEach { (label, fullPrompt) ->
            AssistChip(
                onClick = { onSelectPrompt(fullPrompt) },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = HudSurface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            )
        }
    }
}

@Composable
private fun HudMessageCard(
    message: HudChatMessage,
    isLatestNovaReply: Boolean,
    isSpeaking: Boolean,
    onToggleSpeak: () -> Unit
) {
    val isUser = message.isUser
    val borderColor = if (isUser) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    }
    val containerColor = if (isUser) HudSurfaceElevated else HudDeepNavy

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 520.dp),
            color = containerColor,
            shape = RoundedCornerShape(
                topStart = if (isUser) 18.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            border = BorderStroke(1.dp, borderColor)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isUser) "OPERATOR" else "NOVA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUser) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                        if (message.systemTag != null) {
                            Text(
                                text = "[${message.systemTag}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = message.timestampLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isLatestNovaReply) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onToggleSpeak,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Filled.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = stringResource(R.string.cd_replay_speech),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSpeaking) "STOP VOICE" else "SPEAK",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingHudCard(
    text: String,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HudDeepNavy,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = if (isLoading) "NOVA // COMPUTING" else "NOVA // STREAMING",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ErrorHudCard(
    message: String,
    onRetry: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AlertCrimson.copy(alpha = 0.12f),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, AlertCrimson.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = AlertCrimson
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NEURAL LINK ERROR",
                    style = MaterialTheme.typography.labelMedium,
                    color = AlertCrimson
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            OutlinedButton(
                onClick = onRetry,
                border = BorderStroke(1.dp, AlertCrimson),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCrimson),
                modifier = Modifier.testTag("retry_ai_button")
            ) {
                Text(stringResource(R.string.retry_action))
            }
        }
    }
}

@Composable
private fun CommandInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    isListening: Boolean,
    isBusy: Boolean,
    onSend: () -> Unit,
    onMicClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .testTag("command_input"),
            placeholder = {
                Text(
                    text = stringResource(R.string.hint_command_input),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = HudSurface,
                unfocusedContainerColor = HudDeepNavy,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = HudBorder,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            trailingIcon = {
                if (text.isNotBlank()) {
                    IconButton(
                        onClick = onSend,
                        enabled = !isBusy,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_command_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.cd_send_button),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )

        FilledIconButton(
            onClick = onMicClick,
            modifier = Modifier
                .size(52.dp)
                .testTag("mic_command_button"),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isListening) OptimalEmerald else MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = if (isListening) Icons.Filled.GraphicEq else Icons.Filled.Mic,
                contentDescription = stringResource(R.string.cd_mic_button),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
