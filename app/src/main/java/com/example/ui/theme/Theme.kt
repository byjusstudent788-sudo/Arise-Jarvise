package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NovaDarkColorScheme = darkColorScheme(
    primary = ArcCyan,
    onPrimary = HudObsidian,
    primaryContainer = HudSurfaceElevated,
    onPrimaryContainer = ArcCyan,
    secondary = ElectricCobalt,
    onSecondary = TextPrimary,
    secondaryContainer = HudSurface,
    onSecondaryContainer = TextPrimary,
    tertiary = NeuralViolet,
    onTertiary = TextPrimary,
    tertiaryContainer = NeuralVioletContainer,
    onTertiaryContainer = NeuralViolet,
    background = HudObsidian,
    onBackground = TextPrimary,
    surface = HudDeepNavy,
    onSurface = TextPrimary,
    surfaceVariant = HudSurface,
    onSurfaceVariant = TextSecondary,
    outline = HudBorder,
    outlineVariant = HudOutlineVariant,
    error = AlertCrimson,
    onError = TextPrimary
)

private val NovaAriseOverdriveScheme = NovaDarkColorScheme.copy(
    primary = NeuralViolet,
    onPrimary = TextPrimary,
    primaryContainer = OverdriveContainer,
    onPrimaryContainer = OverdriveOnContainer,
    secondary = ArcCyan,
    tertiary = AriseMagenta
)

private val NovaShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NovaAriseTheme(
    ariseOverdrive: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (ariseOverdrive) NovaAriseOverdriveScheme else NovaDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NovaTypography,
        shapes = NovaShapes,
        content = content
    )
}
