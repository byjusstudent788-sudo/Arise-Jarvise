package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.AriseMagenta
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.NeuralViolet
import com.example.ui.theme.OptimalEmerald
import com.example.ui.viewmodel.CoreOrbPhase
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NovaHudOrb(
    phase: CoreOrbPhase,
    voiceAmplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    orbSize: Dp = 200.dp
) {
    val description = stringResource(R.string.cd_core_orb)
    val interactionSource = remember { MutableInteractionSource() }

    val primaryColor by animateColorAsState(
        targetValue = when (phase) {
            CoreOrbPhase.ONLINE -> ArcCyan
            CoreOrbPhase.LISTENING -> OptimalEmerald
            CoreOrbPhase.THINKING -> ElectricCobalt
            CoreOrbPhase.SPEAKING -> ArcCyan
            CoreOrbPhase.ERROR -> AlertCrimson
            CoreOrbPhase.OVERDRIVE -> NeuralViolet
        },
        animationSpec = tween(durationMillis = 350),
        label = "orbPrimaryColor"
    )

    val secondaryColor by animateColorAsState(
        targetValue = when (phase) {
            CoreOrbPhase.ONLINE -> ElectricCobalt
            CoreOrbPhase.LISTENING -> ArcCyan
            CoreOrbPhase.THINKING -> NeuralViolet
            CoreOrbPhase.SPEAKING -> OptimalEmerald
            CoreOrbPhase.ERROR -> Color(0xFFFF8A00)
            CoreOrbPhase.OVERDRIVE -> AriseMagenta
        },
        animationSpec = tween(durationMillis = 350),
        label = "orbSecondaryColor"
    )

    val smoothedAmplitude by animateFloatAsState(
        targetValue = when (phase) {
            CoreOrbPhase.LISTENING -> voiceAmplitude.coerceIn(0.15f, 1f)
            CoreOrbPhase.SPEAKING -> 0.55f
            CoreOrbPhase.THINKING -> 0.4f
            CoreOrbPhase.OVERDRIVE -> 0.35f
            else -> 0.12f
        },
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 320f),
        label = "orbAmplitude"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "novaOrbRotation")
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (phase) {
                    CoreOrbPhase.THINKING -> 4200
                    CoreOrbPhase.LISTENING -> 6000
                    CoreOrbPhase.OVERDRIVE -> 5500
                    else -> 14000
                },
                easing = LinearEasing
            )
        ),
        label = "outerRotation"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (phase) {
                    CoreOrbPhase.THINKING -> 3000
                    CoreOrbPhase.LISTENING -> 4500
                    else -> 9500
                },
                easing = LinearEasing
            )
        ),
        label = "innerRotation"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (phase) {
                    CoreOrbPhase.LISTENING -> 650
                    CoreOrbPhase.SPEAKING -> 550
                    CoreOrbPhase.THINKING -> 750
                    else -> 2200
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corePulse"
    )

    val surfaceColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .size(orbSize)
            .clip(CircleShape)
            .testTag("nova_core_orb")
            .semantics { contentDescription = description }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = primaryColor),
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = size.minDimension
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = minDim * 0.47f

            // Ambient radial reactor aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.28f * pulse),
                        secondaryColor.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 1.05f
                ),
                radius = maxRadius * 1.05f,
                center = center
            )

            // Outer tick dial
            val tickRadiusOuter = maxRadius * 0.96f
            val tickRadiusInner = maxRadius * 0.90f
            for (i in 0 until 48) {
                val angleRad = (i * 7.5) * (PI / 180.0)
                val isMajor = i % 4 == 0
                val innerR = if (isMajor) tickRadiusInner * 0.96f else tickRadiusInner
                val start = Offset(
                    center.x + (innerR * cos(angleRad)).toFloat(),
                    center.y + (innerR * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    center.x + (tickRadiusOuter * cos(angleRad)).toFloat(),
                    center.y + (tickRadiusOuter * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = primaryColor.copy(alpha = if (isMajor) 0.7f else 0.25f),
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 2.2f else 1.2f,
                    cap = StrokeCap.Round
                )
            }

            // Segmented outer rotating HUD arc
            rotate(degrees = outerRotation, pivot = center) {
                val arcRadius = maxRadius * 0.80f
                val arcRectTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
                val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
                val sweep = 68f
                for (segment in 0 until 3) {
                    drawArc(
                        color = primaryColor.copy(alpha = 0.85f),
                        startAngle = segment * 120f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = arcRectTopLeft,
                        size = arcSize,
                        style = Stroke(width = minDim * 0.024f, cap = StrokeCap.Round)
                    )
                }
            }

            // Counter-rotating mid orbital arc
            rotate(degrees = innerRotation, pivot = center) {
                val midRadius = maxRadius * 0.63f
                val midTopLeft = Offset(center.x - midRadius, center.y - midRadius)
                val midSize = Size(midRadius * 2f, midRadius * 2f)
                for (segment in 0 until 4) {
                    drawArc(
                        color = secondaryColor.copy(alpha = 0.75f),
                        startAngle = segment * 90f + 12f,
                        sweepAngle = 52f,
                        useCenter = false,
                        topLeft = midTopLeft,
                        size = midSize,
                        style = Stroke(width = minDim * 0.018f, cap = StrokeCap.Round)
                    )
                }
            }

            // Audio/Voice reactive wave ring
            val waveRadius = maxRadius * (0.46f + smoothedAmplitude * 0.14f) * pulse
            drawCircle(
                color = primaryColor.copy(alpha = 0.25f + smoothedAmplitude * 0.45f),
                radius = waveRadius,
                center = center,
                style = Stroke(width = 2.5f + smoothedAmplitude * 4f)
            )

            // Inner dark shield disc
            val coreBgRadius = maxRadius * 0.39f
            drawCircle(
                color = surfaceColor.copy(alpha = 0.92f),
                radius = coreBgRadius,
                center = center
            )

            // Hexagonal core frame
            val hexRadius = maxRadius * 0.33f * (0.94f + smoothedAmplitude * 0.12f)
            val hexPath = Path()
            for (vertex in 0 until 6) {
                val angle = ((vertex * 60f) - 90f) * (PI / 180.0)
                val px = center.x + (hexRadius * cos(angle)).toFloat()
                val py = center.y + (hexRadius * sin(angle)).toFloat()
                if (vertex == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
            }
            hexPath.close()

            drawPath(
                path = hexPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.45f),
                        secondaryColor.copy(alpha = 0.15f)
                    ),
                    center = center,
                    radius = hexRadius
                )
            )
            drawPath(
                path = hexPath,
                color = primaryColor,
                style = Stroke(width = minDim * 0.016f)
            )

            // Central luminous reactor spark
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        primaryColor,
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 0.18f * pulse
                ),
                radius = maxRadius * 0.18f * pulse,
                center = center
            )
        }
    }
}
