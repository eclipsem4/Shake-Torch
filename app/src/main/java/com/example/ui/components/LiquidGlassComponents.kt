package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.TiltOffset
import com.example.hardware.ShakeDetector
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.theme.TorchAmber
import com.example.ui.theme.TorchGold
import kotlin.math.roundToInt

/**
 * Animated aurora mesh background using real API 31+ Modifier.blur (RenderEffect).
 * Shifts subtly with device tilt and blooms warm amber when the flashlight is active.
 */
@Composable
fun AuroraGlassBackground(
    isTorchOn: Boolean,
    tilt: TiltOffset,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalGlassPalette.current
    val infiniteTransition = rememberInfiniteTransition(label = "aurora")

    val driftA by infiniteTransition.animateFloat(
        initialValue = -0.15f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftA"
    )
    val driftB by infiniteTransition.animateFloat(
        initialValue = 0.16f,
        targetValue = -0.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(11500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "driftB"
    )
    val torchBloomAlpha by animateFloatAsState(
        targetValue = if (isTorchOn) 0.42f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "torchBloom"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(palette.backdropBase)
    ) {
        // Clear, luminous ambient light backdrop with real API 31+ RenderEffect blur
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 96.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val tiltPxX = tilt.x * 52f
                val tiltPxY = tilt.y * 52f

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.auroraOrbPrimary, Color.Transparent),
                        center = Offset(w * (0.22f + driftA) + tiltPxX, h * 0.18f + tiltPxY),
                        radius = w * 0.64f
                    ),
                    center = Offset(w * (0.22f + driftA) + tiltPxX, h * 0.18f + tiltPxY),
                    radius = w * 0.64f
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.auroraOrbSecondary, Color.Transparent),
                        center = Offset(w * (0.82f + driftB) - tiltPxX, h * 0.44f + tiltPxY),
                        radius = w * 0.58f
                    ),
                    center = Offset(w * (0.82f + driftB) - tiltPxX, h * 0.44f + tiltPxY),
                    radius = w * 0.58f
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.auroraOrbTertiary, Color.Transparent),
                        center = Offset(w * (0.35f - driftB) + tiltPxX, h * 0.84f - tiltPxY),
                        radius = w * 0.62f
                    ),
                    center = Offset(w * (0.35f - driftB) + tiltPxX, h * 0.84f - tiltPxY),
                    radius = w * 0.62f
                )

                // Warm amber glow when flashlight is ON
                if (torchBloomAlpha > 0.01f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                TorchAmber.copy(alpha = torchBloomAlpha),
                                Color.Transparent
                            ),
                            center = Offset(w * 0.5f + tiltPxX * 0.6f, h * 0.36f + tiltPxY * 0.6f),
                            radius = w * 0.68f
                        ),
                        center = Offset(w * 0.5f + tiltPxX * 0.6f, h * 0.36f + tiltPxY * 0.6f),
                        radius = w * 0.68f
                    )
                }
            }
        }

        content()
    }
}

/**
 * Translucent Liquid Glass card featuring:
 * - Real Modifier.blur inner caustic sheen layer (keeping foreground text sharp)
 * - Subtle animated specular light sweep + tilt-reactive refraction highlight
 * - Dual-layer glass bevel border (crisp top-left catchlight + delicate inner rim)
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    tilt: TiltOffset = TiltOffset(),
    accentGlow: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val palette = LocalGlassPalette.current
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.982f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 460f),
        label = "cardScale"
    )

    val sheenTransition = rememberInfiniteTransition(label = "cardSheen")
    val sheenPhase by sheenTransition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(7500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sheenPhase"
    )

    val shiftX = (tilt.x * 0.28f).coerceIn(-0.28f, 0.28f)
    val shiftY = (tilt.y * 0.28f).coerceIn(-0.28f, 0.28f)

    val surfaceBrush = Brush.linearGradient(
        colors = listOf(
            palette.glassFillTop,
            palette.glassFillBottom.copy(alpha = (palette.glassFillBottom.alpha + 0.03f).coerceAtMost(1f)),
            palette.glassFillBottom
        ),
        start = Offset(0f, 0f),
        end = Offset(900f + shiftX * 320f, 900f + shiftY * 320f)
    )

    val outerBorderBrush = Brush.linearGradient(
        colors = listOf(
            palette.glassBorderHighlight,
            palette.glassBorderTrailing.copy(alpha = 0.18f),
            palette.glassBorderHighlight.copy(alpha = 0.22f)
        ),
        start = Offset(0f + shiftX * 220f, 0f + shiftY * 220f),
        end = Offset(820f, 820f)
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(surfaceBrush, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true),
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        // Blurred internal specular & caustic glow layer (API 31+ RenderEffect)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .blur(28.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val glowColor = accentGlow?.copy(alpha = 0.26f) ?: palette.glassInnerGlow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = Offset(
                            x = size.width * (0.22f + shiftX),
                            y = size.height * (0.14f + shiftY)
                        ),
                        radius = size.maxDimension * 0.78f
                    ),
                    radius = size.maxDimension * 0.78f
                )

                // Moving subtle specular light reflection band
                val sweepCenter = Offset(
                    x = size.width * (sheenPhase + shiftX * 0.4f),
                    y = size.height * (0.35f + shiftY * 0.3f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (palette.isDark) 0.09f else 0.26f),
                            Color.Transparent
                        ),
                        center = sweepCenter,
                        radius = size.maxDimension * 0.55f
                    ),
                    center = sweepCenter,
                    radius = size.maxDimension * 0.55f
                )
            }
        }

        // Crisp dual-rim glass border
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(width = 1.15.dp, brush = outerBorderBrush, shape = shape)
                .padding(1.dp)
                .border(
                    width = 0.6.dp,
                    color = Color.White.copy(alpha = if (palette.isDark) 0.10f else 0.35f),
                    shape = RoundedCornerShape((cornerRadius - 1.dp).coerceAtLeast(1.dp))
                )
        )

        // Unblurred foreground content
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/**
 * Large central morphing Liquid Glass power orb with expanding sonar ripples,
 * rotating liquid specular rim light, and warm amber glow when the flashlight is ON.
 */
@Composable
fun MorphingGlassPowerOrb(
    isServiceRunning: Boolean,
    isTorchOn: Boolean,
    tilt: TiltOffset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "orbAnimations")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val ripplePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripplePhase"
    )
    val rimAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rimAngle"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.91f else 1f,
        animationSpec = spring(dampingRatio = 0.52f, stiffness = 440f),
        label = "orbPress"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (isTorchOn) 1.10f else if (isServiceRunning) 1.03f else 0.96f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 360f),
        label = "orbIconScale"
    )

    val glowColor by animateColorAsState(
        targetValue = when {
            isTorchOn -> TorchAmber
            isServiceRunning -> palette.accentCool
            else -> palette.textSecondary.copy(alpha = 0.35f)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "orbGlowColor"
    )

    val ringBorderColor by animateColorAsState(
        targetValue = when {
            isTorchOn -> TorchGold.copy(alpha = 0.95f)
            isServiceRunning -> palette.accentCool.copy(alpha = 0.90f)
            else -> palette.glassBorderHighlight
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "orbBorder"
    )

    val cornerRadius by animateDpAsState(
        targetValue = when {
            isTorchOn -> 54.dp
            isServiceRunning -> 86.dp
            else -> 112.dp
        },
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 300f),
        label = "orbMorphRadius"
    )

    val orbShape = RoundedCornerShape(cornerRadius)
    val orbCd = stringResource(R.string.cd_power_orb_toggle)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(248.dp)
            .scale(pressScale)
    ) {
        // Expanding concentric sonar ripple rings when armed or lit
        if (isServiceRunning || isTorchOn) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val baseRadius = 98.dp.toPx()
                val maxExpand = 26.dp.toPx()
                listOf(ripplePhase, (ripplePhase + 0.5f) % 1f).forEach { phase ->
                    val alpha = (1f - phase) * (if (isTorchOn) 0.42f else 0.28f)
                    val radius = baseRadius + phase * maxExpand
                    drawRoundRect(
                        color = glowColor.copy(alpha = alpha),
                        topLeft = Offset(
                            (size.width - radius * 2f) / 2f,
                            (size.height - radius * 2f) / 2f
                        ),
                        size = Size(radius * 2f, radius * 2f),
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
            }
        }

        // Real blurred outer aura halo
        Box(
            modifier = Modifier
                .size(218.dp)
                .scale(if (isServiceRunning || isTorchOn) pulse else 0.90f)
                .blur(36.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = if (isTorchOn) 0.68f else if (isServiceRunning) 0.46f else 0.14f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Main Liquid Glass Orb Body
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(198.dp)
                .testTag("power_orb_button")
                .semantics { contentDescription = orbCd }
                .clip(orbShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            glowColor.copy(alpha = if (isTorchOn) 0.36f else if (isServiceRunning) 0.24f else 0.13f),
                            palette.glassFillBottom
                        ),
                        start = Offset(0f + tilt.x * 80f, 0f + tilt.y * 80f),
                        end = Offset(600f, 600f)
                    ),
                    shape = orbShape
                )
                .border(
                    width = 1.8.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            ringBorderColor,
                            palette.glassBorderTrailing
                        )
                    ),
                    shape = orbShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true),
                    role = Role.Switch,
                    onClick = onClick
                )
        ) {
            // Internal blurred caustic highlight inside orb
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(orbShape)
                    .blur(18.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isTorchOn) 0.48f else 0.28f),
                                Color.Transparent
                            ),
                            center = Offset(
                                size.width * (0.30f + tilt.x * 0.15f),
                                size.height * (0.22f + tilt.y * 0.15f)
                            ),
                            radius = size.width * 0.56f
                        )
                    )
                }
            }

            // Rotating liquid specular highlight along the inner rim
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(orbShape)
            ) {
                rotate(degrees = rimAngle) {
                    drawRoundRect(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (isTorchOn || isServiceRunning) 0.55f else 0.25f),
                                Color.Transparent,
                                Color.Transparent
                            )
                        ),
                        topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                        size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Crossfade(
                    targetState = isTorchOn,
                    animationSpec = tween(220),
                    label = "torchIconCrossfade"
                ) { torchOn ->
                    Icon(
                        imageVector = if (torchOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                        contentDescription = stringResource(R.string.cd_flashlight_icon),
                        tint = if (torchOn) TorchAmber else if (isServiceRunning) palette.accentCool else palette.textPrimary,
                        modifier = Modifier
                            .size(54.dp)
                            .scale(iconScale)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isServiceRunning) "ACTIVE" else "STANDBY",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.6.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = palette.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        if (isServiceRunning) R.string.orb_tap_to_stop else R.string.orb_tap_to_start
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Real-time Canvas shake meter visualization rendering:
 * - Rolling accelerometer G-force waveform & spectrum bars from live sensor events
 * - Dynamic horizontal threshold line that moves immediately as the user drags the sensitivity slider
 * - Peak hold indicator and glowing threshold-crossing highlights
 */
@Composable
fun LiveShakeMeter(
    currentMagnitudeG: Float,
    peakMagnitudeG: Float,
    sensitivity: Int,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val thresholdG = ShakeDetector.sensitivityToThresholdG(sensitivity)
    val maxG = ShakeDetector.MAX_METER_G

    val historySize = 44
    val history = remember {
        androidx.compose.runtime.mutableStateListOf<Float>().apply {
            repeat(historySize) { add(0f) }
        }
    }
    val latestMagnitudeG by androidx.compose.runtime.rememberUpdatedState(currentMagnitudeG)

    androidx.compose.runtime.LaunchedEffect(peakMagnitudeG == 0f && currentMagnitudeG == 0f) {
        if (peakMagnitudeG == 0f && currentMagnitudeG == 0f) {
            for (i in history.indices) {
                history[i] = 0f
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            if (history.size >= historySize) {
                history.removeAt(0)
            }
            history.add(latestMagnitudeG.coerceIn(0f, maxG))
            kotlinx.coroutines.delay(50L)
        }
    }

    val animatedMagnitude by animateFloatAsState(
        targetValue = (currentMagnitudeG / maxG).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.70f, stiffness = 550f),
        label = "shakeFill"
    )
    val animatedThreshold by animateFloatAsState(
        targetValue = (thresholdG / maxG).coerceIn(0.08f, 0.95f),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 460f),
        label = "shakeThreshold"
    )
    val animatedPeak by animateFloatAsState(
        targetValue = (peakMagnitudeG / maxG).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 320f),
        label = "shakePeak"
    )
    val exceededThreshold = currentMagnitudeG >= thresholdG

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        // Real-time Canvas Waveform + Energy Bars + Movable Threshold Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(palette.textPrimary.copy(alpha = 0.06f))
                .border(
                    width = 1.dp,
                    color = if (exceededThreshold) {
                        TorchAmber.copy(alpha = 0.75f)
                    } else {
                        palette.glassBorderHighlight.copy(alpha = 0.38f)
                    },
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                val w = size.width
                val h = size.height
                val stepX = w / (historySize - 1).coerceAtLeast(1)
                val thresholdY = h * (1f - animatedThreshold)

                // Subtle horizontal reference grid lines
                val gridColor = palette.textPrimary.copy(alpha = 0.08f)
                listOf(0.25f, 0.5f, 0.75f).forEach { fraction ->
                    val y = h * fraction
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Spectrum bars + smooth waveform path
                val barWidth = (stepX * 0.52f).coerceAtLeast(3.dp.toPx())
                val wavePath = androidx.compose.ui.graphics.Path()
                val fillPath = androidx.compose.ui.graphics.Path()

                history.forEachIndexed { index, sampleG ->
                    val norm = (sampleG / maxG).coerceIn(0f, 1f)
                    val x = index * stepX
                    val y = h * (1f - norm)
                    val isAboveThreshold = sampleG >= thresholdG

                    // Vertical energy bar
                    val barHeight = (h - y).coerceAtLeast(2.dp.toPx())
                    drawRoundRect(
                        color = if (isAboveThreshold) {
                            TorchAmber.copy(alpha = 0.45f)
                        } else {
                            palette.accentCool.copy(alpha = 0.24f)
                        },
                        topLeft = Offset(x - barWidth / 2f, h - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    if (index == 0) {
                        wavePath.moveTo(x, y)
                        fillPath.moveTo(x, h)
                        fillPath.lineTo(x, y)
                    } else {
                        val prevNorm = (history[index - 1] / maxG).coerceIn(0f, 1f)
                        val prevX = (index - 1) * stepX
                        val prevY = h * (1f - prevNorm)
                        val midX = (prevX + x) / 2f
                        wavePath.cubicTo(midX, prevY, midX, y, x, y)
                        fillPath.cubicTo(midX, prevY, midX, y, x, y)
                    }

                    if (isAboveThreshold) {
                        drawCircle(
                            color = TorchAmber,
                            radius = 3.2.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }

                fillPath.lineTo(w, h)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            (if (exceededThreshold) TorchAmber else palette.accentCool).copy(alpha = 0.28f),
                            Color.Transparent
                        )
                    )
                )

                drawPath(
                    path = wavePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            palette.accentCool.copy(alpha = 0.65f),
                            if (exceededThreshold) TorchAmber else palette.accentCool
                        )
                    ),
                    style = Stroke(
                        width = 2.2.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )

                // Movable dashed sensitivity threshold line across the Canvas
                drawLine(
                    color = TorchAmber,
                    start = Offset(0f, thresholdY),
                    end = Offset(w, thresholdY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(12f, 8f),
                        0f
                    )
                )

                // Glowing dot at the live leading sample
                val latestY = h * (1f - animatedMagnitude)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            (if (exceededThreshold) TorchAmber else palette.accentCool).copy(alpha = 0.55f),
                            Color.Transparent
                        ),
                        center = Offset(w, latestY),
                        radius = 12.dp.toPx()
                    ),
                    center = Offset(w, latestY),
                    radius = 12.dp.toPx()
                )
                drawCircle(
                    color = if (exceededThreshold) TorchAmber else Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(w, latestY)
                )
            }
        }

        // Instant horizontal gauge bar with movable threshold & peak ticks
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(palette.textPrimary.copy(alpha = 0.09f))
                .border(
                    width = 1.dp,
                    color = palette.glassBorderHighlight.copy(alpha = 0.40f),
                    shape = RoundedCornerShape(7.dp)
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val fillWidth = size.width * animatedMagnitude
                val barBrush = Brush.horizontalGradient(
                    colors = if (exceededThreshold) {
                        listOf(palette.accentCool, TorchAmber, TorchGold)
                    } else {
                        listOf(palette.accentCool.copy(alpha = 0.72f), palette.accentCool)
                    }
                )
                if (fillWidth > 2f) {
                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset.Zero,
                        size = Size(fillWidth, size.height),
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                    )
                }

                val peakX = (size.width * animatedPeak).coerceIn(3f, size.width - 3f)
                if (animatedPeak > 0.04f) {
                    drawLine(
                        color = palette.textPrimary.copy(alpha = 0.65f),
                        start = Offset(peakX, 2.dp.toPx()),
                        end = Offset(peakX, size.height - 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                val thresholdX = size.width * animatedThreshold
                drawLine(
                    color = TorchAmber,
                    start = Offset(thresholdX, 0f),
                    end = Offset(thresholdX, size.height),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }
    }
}

/**
 * Pill-shaped Liquid Glass switch with spring thumb animation and minimum 48dp touch target.
 */
@Composable
fun GlassPillSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescriptionText: String? = null
) {
    val palette = LocalGlassPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val thumbWidth by animateDpAsState(
        targetValue = if (pressed) 34.dp else 28.dp,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 500f),
        label = "switchThumbWidth"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) (52.dp - thumbWidth) else 2.dp,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 460f),
        label = "switchThumb"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) palette.accentCool.copy(alpha = 0.88f) else palette.textPrimary.copy(alpha = 0.14f),
        animationSpec = spring(stiffness = 380f),
        label = "switchTrack"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .alpha(if (enabled) 1f else 0.45f)
            .semantics {
                if (contentDescriptionText != null) {
                    contentDescription = contentDescriptionText
                }
            }
            .clickable(
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
    ) {
        Box(
            modifier = Modifier
                .width(54.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(trackColor)
                .border(
                    width = 1.dp,
                    color = palette.glassBorderHighlight.copy(alpha = if (checked) 0.88f else 0.42f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset, y = 2.dp)
                    .width(thumbWidth)
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(
                        width = 0.75.dp,
                        color = Color.White.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(14.dp)
                    )
            )
        }
    }
}

/**
 * Liquid Glass slider with a luminous frosted track and glowing interactive thumb.
 */
@Composable
fun GlassSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    accentColor: Color? = null,
    contentDescriptionText: String? = null
) {
    val palette = LocalGlassPalette.current
    val activeColor = accentColor ?: palette.accentCool
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.001f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val thumbRadiusDp = 12.dp
    val thumbRadiusPx = with(LocalDensity.current) { thumbRadiusDp.toPx() }
    var widthPx by remember { mutableFloatStateOf(1f) }
    val latestOnValueChange by androidx.compose.runtime.rememberUpdatedState(onValueChange)
    val latestValue by androidx.compose.runtime.rememberUpdatedState(value)

    fun snapValue(rawFraction: Float): Float {
        val clamped = rawFraction.coerceIn(0f, 1f)
        val snapped = if (steps > 0) {
            val segments = steps + 1
            (clamped * segments).roundToInt() / segments.toFloat()
        } else {
            clamped
        }
        return valueRange.start + snapped * span
    }

    fun emitFromPositionX(posX: Float) {
        val usableWidth = (widthPx - thumbRadiusPx * 2f).coerceAtLeast(1f)
        val rawFraction = ((posX - thumbRadiusPx) / usableWidth).coerceIn(0f, 1f)
        val nextValue = snapValue(rawFraction)
        if (kotlin.math.abs(nextValue - latestValue) > 0.0001f) {
            latestOnValueChange(nextValue)
        }
    }

    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics {
                if (contentDescriptionText != null) {
                    contentDescription = contentDescriptionText
                }
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange, steps)
                setProgress { targetValue ->
                    val clamped = targetValue.coerceIn(valueRange.start, valueRange.endInclusive)
                    val snapped = snapValue((clamped - valueRange.start) / span)
                    latestOnValueChange(snapped)
                    true
                }
            }
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(valueRange, steps, thumbRadiusPx) {
                detectTapGestures { offset ->
                    emitFromPositionX(offset.x)
                }
            }
            .pointerInput(valueRange, steps, thumbRadiusPx) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    emitFromPositionX(change.position.x)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
            val trackHeight = 10.dp.toPx()
            val thumbRadius = thumbRadiusDp.toPx()
            val usableWidth = (size.width - thumbRadius * 2f).coerceAtLeast(1f)
            val centerY = size.height / 2f
            val thumbX = thumbRadius + usableWidth * fraction

            // Background frosted track
            drawRoundRect(
                color = palette.textPrimary.copy(alpha = 0.14f),
                topLeft = Offset(thumbRadius, centerY - trackHeight / 2f),
                size = Size(usableWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            )

            // Active glowing track
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(activeColor.copy(alpha = 0.65f), activeColor)
                ),
                topLeft = Offset(thumbRadius, centerY - trackHeight / 2f),
                size = Size((thumbX - thumbRadius).coerceAtLeast(0f), trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            )

            // Subtle step tick marks along the track when discrete steps are enabled
            if (steps > 0) {
                val segments = steps + 1
                for (i in 0..segments) {
                    val tickFraction = i / segments.toFloat()
                    val tickX = thumbRadius + usableWidth * tickFraction
                    val isCovered = tickFraction <= fraction
                    drawCircle(
                        color = if (isCovered) {
                            Color.White.copy(alpha = 0.65f)
                        } else {
                            palette.textSecondary.copy(alpha = 0.38f)
                        },
                        radius = 1.6.dp.toPx(),
                        center = Offset(tickX, centerY)
                    )
                }
            }

            // Soft outer glow around thumb
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(activeColor.copy(alpha = 0.48f), Color.Transparent),
                    center = Offset(thumbX, centerY),
                    radius = thumbRadius * 2.1f
                ),
                center = Offset(thumbX, centerY),
                radius = thumbRadius * 2.1f
            )

            // Crisp frosted thumb
            drawCircle(
                color = Color.White,
                radius = thumbRadius,
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = activeColor,
                radius = thumbRadius * 0.42f,
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = palette.glassBorderHighlight,
                radius = thumbRadius,
                center = Offset(thumbX, centerY),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

enum class BottomNavTab(
    val routeKey: String,
    val labelRes: Int,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("home", R.string.nav_home, Icons.Rounded.Home, "nav_tab_home"),
    SETTINGS("settings", R.string.nav_settings, Icons.Rounded.Tune, "nav_tab_settings"),
    SYSTEM("system", R.string.nav_battery_permissions, Icons.Rounded.BatteryChargingFull, "nav_tab_system")
}

/**
 * Floating Liquid Glass bottom navigation pill with a spring-sliding glass capsule indicator,
 * frosted inner refraction, and proper WindowInsets.navigationBars padding.
 */
@Composable
fun FloatingGlassNavBar(
    selectedTab: BottomNavTab,
    onSelectTab: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val navCd = stringResource(R.string.cd_navigation_bar)
    val pillShape = RoundedCornerShape(30.dp)
    val selectedIndex = BottomNavTab.entries.indexOf(selectedTab).coerceAtLeast(0)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .semantics { contentDescription = navCd }
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .height(66.dp)
                .clip(pillShape)
                .background(palette.navBarFill, pillShape)
                .border(
                    width = 1.1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(palette.glassBorderHighlight, palette.glassBorderTrailing)
                    ),
                    shape = pillShape
                )
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            // Subtle frosted refraction layer inside navbar shell
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .blur(18.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
                    .background(palette.glassInnerGlow.copy(alpha = 0.08f))
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val tabWidth = maxWidth / BottomNavTab.entries.size
                val indicatorOffset by animateDpAsState(
                    targetValue = tabWidth * selectedIndex,
                    animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
                    label = "navSlideIndicator"
                )

                // Sliding Liquid Glass Capsule Indicator
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    palette.accentCool.copy(alpha = 0.28f),
                                    palette.accentCool.copy(alpha = 0.14f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    palette.accentCool.copy(alpha = 0.65f),
                                    palette.glassBorderTrailing
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomNavTab.entries.forEach { tab ->
                        val isSelected = tab == selectedTab
                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) palette.accentCool else palette.textSecondary,
                            animationSpec = spring(stiffness = 400f),
                            label = "navTint"
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.12f else 1f,
                            animationSpec = spring(dampingRatio = 0.58f, stiffness = 420f),
                            label = "navIconScale"
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .minimumInteractiveComponentSize()
                                .testTag(tab.testTag)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = true),
                                    onClick = { onSelectTab(tab) }
                                )
                                .padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier
                                    .size(21.dp)
                                    .scale(iconScale)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) palette.textPrimary else contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
