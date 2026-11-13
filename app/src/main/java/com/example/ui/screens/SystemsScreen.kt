package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.HudBorder
import com.example.ui.theme.HudDeepNavy
import com.example.ui.theme.HudSurface
import com.example.ui.theme.OptimalEmerald
import com.example.ui.theme.TacticalAmber
import com.example.ui.viewmodel.AuditState
import com.example.ui.viewmodel.NovaUiState
import java.util.Locale

@Composable
fun SystemsScreen(
    uiState: NovaUiState,
    onRefreshTelemetry: () -> Unit,
    onToggleFlashlight: () -> Unit,
    onToggleAriseOverdrive: (Boolean) -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onRunAiAudit: () -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val telemetry = uiState.telemetry

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
            item(key = "systems_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SYSTEM DIAGNOSTICS",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${telemetry.deviceModel} • ${telemetry.androidVersion}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onRefreshTelemetry,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("refresh_telemetry_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh device telemetry",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (uiState.hardwareBanner != null) {
                item(key = "hardware_banner") {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.hardwareBanner,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onDismissBanner) {
                                Text("OK")
                            }
                        }
                    }
                }
            }

            // Live Telemetry Grid (2x2 cards)
            item(key = "telemetry_grid_top") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryGaugeCard(
                        title = "POWER CORE",
                        value = "${telemetry.batteryPercent}%",
                        subtitle = "${if (telemetry.isCharging) telemetry.chargingSource else "DISCHARGING"} • ${
                            String.format(Locale.US, "%.1f", telemetry.batteryTempCelsius)
                        }°C",
                        progress = telemetry.batteryPercent / 100f,
                        accentColor = if (telemetry.batteryPercent <= 20 && !telemetry.isCharging) {
                            TacticalAmber
                        } else {
                            OptimalEmerald
                        },
                        icon = Icons.Filled.BatteryChargingFull,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryGaugeCard(
                        title = "NEURAL RAM",
                        value = "${telemetry.ramUsagePercent}%",
                        subtitle = "${String.format(Locale.US, "%.1f", telemetry.availableRamGb)} / ${
                            String.format(Locale.US, "%.1f", telemetry.totalRamGb)
                        } GB FREE",
                        progress = telemetry.ramUsagePercent / 100f,
                        accentColor = MaterialTheme.colorScheme.primary,
                        icon = Icons.Filled.Memory,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "telemetry_grid_bottom") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryGaugeCard(
                        title = "STORAGE MATRIX",
                        value = "${telemetry.storageUsagePercent}%",
                        subtitle = "${String.format(Locale.US, "%.1f", telemetry.availableStorageGb)} GB FREE",
                        progress = telemetry.storageUsagePercent / 100f,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        icon = Icons.Filled.Storage,
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryGaugeCard(
                        title = "UPLINK & UPTIME",
                        value = telemetry.networkStatus,
                        subtitle = "UPTIME ${telemetry.uptimeFormatted} • ${telemetry.batteryHealthLabel}",
                        progress = if (telemetry.isNetworkConnected) 1f else 0.15f,
                        accentColor = if (telemetry.isNetworkConnected) OptimalEmerald else AlertCrimson,
                        icon = Icons.Filled.NetworkCheck,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // AI Diagnostic Audit Card
            item(key = "ai_audit_card") {
                Surface(
                    color = HudDeepNavy,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "GEMINI SYSTEM AUDIT",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Button(
                                onClick = onRunAiAudit,
                                enabled = uiState.auditState !is AuditState.Running,
                                modifier = Modifier.testTag("run_ai_audit_button")
                            ) {
                                if (uiState.auditState is AuditState.Running) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("SCAN NOW")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        when (val audit = uiState.auditState) {
                            AuditState.Idle -> {
                                Text(
                                    text = "Tap SCAN NOW to stream a live Gemini diagnostic grounded in your real battery, RAM, storage, and network sensors.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AuditState.Running -> {
                                Text(
                                    text = "Analyzing live hardware telemetry…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            is AuditState.Streaming -> {
                                Text(
                                    text = audit.report,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            is AuditState.Success -> {
                                Text(
                                    text = audit.report,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            is AuditState.Error -> {
                                Text(
                                    text = audit.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AlertCrimson
                                )
                            }
                        }
                    }
                }
            }

            // Hardware & Protocol Controls
            item(key = "hardware_controls") {
                Surface(
                    color = HudSurface,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, HudBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ACTIVE PROTOCOLS & HARDWARE",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        HardwareSwitchRow(
                            icon = Icons.Filled.FlashlightOn,
                            title = "Tactical Flashlight",
                            subtitle = if (telemetry.hasFlashlight) {
                                if (telemetry.isFlashlightOn) "Beam active" else "Hardware torch ready"
                            } else {
                                "No flash module reported on device"
                            },
                            checked = telemetry.isFlashlightOn,
                            enabled = telemetry.hasFlashlight,
                            onCheckedChange = { onToggleFlashlight() },
                            testTag = "flashlight_switch"
                        )

                        HardwareSwitchRow(
                            icon = Icons.Filled.Bolt,
                            title = "ARISE Overdrive Mode",
                            subtitle = "Switches core HUD & Gemini tone to peak tactical readiness",
                            checked = uiState.preferences.ariseOverdrive,
                            enabled = true,
                            onCheckedChange = onToggleAriseOverdrive,
                            testTag = "overdrive_switch"
                        )

                        HardwareSwitchRow(
                            icon = Icons.Filled.VolumeUp,
                            title = "Voice Synthesis Output",
                            subtitle = "Automatically speak Nova responses aloud",
                            checked = uiState.preferences.autoSpeak,
                            enabled = true,
                            onCheckedChange = onToggleAutoSpeak,
                            testTag = "autospeak_switch"
                        )
                    }
                }
            }

            // Android System Quick Launchers
            item(key = "system_shortcuts") {
                Surface(
                    color = HudDeepNavy,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, HudBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "DEVICE CONTROL BRIDGES",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SystemShortcutButton(
                                label = "Wi-Fi",
                                icon = Icons.Filled.Wifi,
                                onClick = { openSystemSettings(context, Settings.ACTION_WIFI_SETTINGS) },
                                modifier = Modifier.weight(1f)
                            )
                            SystemShortcutButton(
                                label = "Display",
                                icon = Icons.Filled.PhoneAndroid,
                                onClick = { openSystemSettings(context, Settings.ACTION_DISPLAY_SETTINGS) },
                                modifier = Modifier.weight(1f)
                            )
                            SystemShortcutButton(
                                label = "Settings",
                                icon = Icons.Filled.Settings,
                                onClick = { openSystemSettings(context, Settings.ACTION_SETTINGS) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryGaugeCard(
    title: String,
    value: String,
    subtitle: String,
    progress: Float,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = HudSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, HudBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = accentColor,
                trackColor = HudDeepNavy
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HardwareSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SystemShortcutButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, HudBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

private fun openSystemSettings(context: Context, action: String) {
    try {
        context.startActivity(
            Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
    }
}
