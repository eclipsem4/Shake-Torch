package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import kotlin.random.Random

/**
 * Builds a seamless 128x128 tileable procedural grain ImageShader wrapped in a ShaderBrush
 * for authentic frosted-glass micro-texture without per-frame allocation.
 */
@Composable
private fun rememberFrostedGrainShaderBrush(isDark: Boolean): ShaderBrush {
    return remember(isDark) {
        val tileSize = 128
        val pixels = IntArray(tileSize * tileSize)
        val rng = Random(20261115)
        for (i in pixels.indices) {
            val roll = rng.nextFloat()
            pixels[i] = if (roll < 0.42f) {
                val alpha = if (isDark) rng.nextInt(4, 18) else rng.nextInt(3, 13)
                android.graphics.Color.argb(alpha, 255, 255, 255)
            } else if (roll < 0.72f) {
                val alpha = if (isDark) rng.nextInt(4, 15) else rng.nextInt(4, 14)
                android.graphics.Color.argb(alpha, 12, 18, 32)
            } else {
                android.graphics.Color.TRANSPARENT
            }
        }
        val bitmap = android.graphics.Bitmap.createBitmap(
            pixels,
            tileSize,
            tileSize,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        ShaderBrush(
            ImageShader(
                image = bitmap.asImageBitmap(),
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated
            )
        )
    }
}

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
        // Real RenderEffect blur layer for luminous aurora mesh orbs
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 84.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val tiltPxX = tilt.x * 48f
                val tiltPxY = tilt.y * 48f

                // Top-left sapphire orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.auroraOrbPrimary, Color.Transparent),
                        center = Offset(w * (0.24f + driftA) + tiltPxX, h * 0.20f + tiltPxY),
                        radius = w * 0.68f
                    ),
                    center = Offset(w * (0.24f + driftA) + tiltPxX, h * 0.20f + tiltPxY),
                    radius = w * 0.68f
                )

                // Mid-right violet orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.auroraOrbSecondary, Color.Transparent),
                        center = Offset(w * (0.82f + driftB) - tiltPxX, h * 0.44f + tiltPxY),
                        radius = w * 0.64f
                    ),
                    center = Offset(w * (0.82f + driftB) - tiltPxX, h * 0.44f + tiltPxY),
                    radius = w * 0.64f
                )

                // Bottom teal orb
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(palette.auroraOrbTertiary, Color.Transparent),
                        center = Offset(w * (0.35f - driftB) + tiltPxX, h * 0.82f - tiltPxY),
                        radius = w * 0.70f
                    ),
                    center = Offset(w * (0.35f - driftB) + tiltPxX, h * 0.82f - tiltPxY),
                    radius = w * 0.70f
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
                            radius = w * 0.72f
                        ),
                        center = Offset(w * 0.5f + tiltPxX * 0.6f, h * 0.36f + tiltPxY * 0.6f),
                        radius = w * 0.72f
                    )
                }
            }
        }

        // Subtle frosted background emblem from user-supplied flashlight icon
        Image(
            painter = painterResource(id = R.drawable.user_pfp_flashlight),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.Center)
                .size(280.dp)
                .offset(x = (tilt.x * 16f).dp, y = (tilt.y * 16f).dp)
                .blur(10.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .alpha(if (palette.isDark) 0.14f else 0.11f)
        )

        // Procedural frosted-grain Canvas shader overlay over the aurora mesh gradient
        val grainShaderBrush = rememberFrostedGrainShaderBrush(isDark = palette.isDark)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = grainShaderBrush)
        }

        content()
    }
}

/**
 * Translucent Liquid Glass card featuring:
 * - Real Modifier.blur inner caustic sheen layer (keeping foreground text sharp)
 * - Diagonal specular refraction fill
 * - Tilt-responsive gradient border (bright top-left, subtle bottom-right)
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
        targetValue = if (pressed && onClick != null) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 450f),
        label = "cardScale"
    )

    val shiftX = (tilt.x * 0.25f).coerceIn(-0.25f, 0.25f)
    val shiftY = (tilt.y * 0.25f).coerceIn(-0.25f, 0.25f)

    val surfaceBrush = Brush.linearGradient(
        colors = listOf(palette.glassFillTop, palette.glassFillBottom),
        start = Offset(0f, 0f),
        end = Offset(900f + shiftX * 300f, 900f + shiftY * 300f)
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            palette.glassBorderHighlight,
            palette.glassBorderTrailing.copy(alpha = 0.16f),
            palette.glassBorderTrailing
        ),
        start = Offset(0f + shiftX * 200f, 0f + shiftY * 200f),
        end = Offset(800f, 800f)
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
        // Blurred internal specular highlight layer (API 31+ RenderEffect)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .blur(26.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val glowColor = accentGlow?.copy(alpha = 0.24f) ?: palette.glassInnerGlow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = Offset(
                            x = size.width * (0.20f + shiftX),
                            y = size.height * (0.12f + shiftY)
                        ),
                        radius = size.maxDimension * 0.75f
                    ),
                    radius = size.maxDimension * 0.75f
                )
            }
        }

        // Crisp specular gradient border + subtle frosted surface micro-grain
        val cardGrainBrush = rememberFrostedGrainShaderBrush(isDark = palette.isDark)
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
        ) {
            drawRect(brush = cardGrainBrush, alpha = 0.45f)
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .border(width = 1.dp, brush = borderBrush, shape = shape)
        )

        // Unblurred foreground content
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/**
 * Large central morphing Liquid Glass power orb that glows warm amber when the flashlight is ON,
 * cool ice-blue when ShakeLight is armed, and frosted neutral when inactive.
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

    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 420f),
        label = "orbPress"
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
            isTorchOn -> TorchGold.copy(alpha = 0.92f)
            isServiceRunning -> palette.accentCool.copy(alpha = 0.85f)
            else -> palette.glassBorderHighlight
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "orbBorder"
    )

    val cornerRadius by animateDpAsState(
        targetValue = when {
            isTorchOn -> 56.dp
            isServiceRunning -> 88.dp
            else -> 112.dp
        },
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 320f),
        label = "orbMorphRadius"
    )

    val orbShape = RoundedCornerShape(cornerRadius)
    val orbCd = stringResource(R.string.cd_power_orb_toggle)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(236.dp)
            .scale(pressScale)
    ) {
        // Real blurred outer aura halo
        Box(
            modifier = Modifier
                .size(216.dp)
                .scale(if (isServiceRunning || isTorchOn) pulse else 0.92f)
                .blur(34.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = if (isTorchOn) 0.65f else if (isServiceRunning) 0.45f else 0.16f),
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
                .size(196.dp)
                .testTag("power_orb_button")
                .semantics { contentDescription = orbCd }
                .clip(orbShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            glowColor.copy(alpha = if (isTorchOn) 0.36f else if (isServiceRunning) 0.24f else 0.12f),
                            palette.glassFillBottom
                        ),
                        start = Offset(0f + tilt.x * 80f, 0f + tilt.y * 80f),
                        end = Offset(600f, 600f)
                    ),
                    shape = orbShape
                )
                .border(
                    width = 1.6.dp,
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
                                Color.White.copy(alpha = if (isTorchOn) 0.45f else 0.25f),
                                Color.Transparent
                            ),
                            center = Offset(
                                size.width * (0.32f + tilt.x * 0.14f),
                                size.height * (0.24f + tilt.y * 0.14f)
                            ),
                            radius = size.width * 0.55f
                        )
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = if (isTorchOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                    contentDescription = stringResource(R.string.cd_flashlight_icon),
                    tint = if (isTorchOn) TorchAmber else if (isServiceRunning) palette.accentCool else palette.textPrimary,
                    modifier = Modifier.size(54.dp)
                )
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

    androidx.compose.runtime.LaunchedEffect(currentMagnitudeG) {
        if (history.size >= historySize) {
            history.removeAt(0)
        }
        history.add(currentMagnitudeG.coerceIn(0f, maxG))
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
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 460f),
        label = "switchThumb"
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) palette.accentCool.copy(alpha = 0.85f) else palette.textPrimary.copy(alpha = 0.14f),
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
                interactionSource = remember { MutableInteractionSource() },
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
                    color = palette.glassBorderHighlight.copy(alpha = if (checked) 0.8f else 0.4f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset, y = 2.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        width = 0.75.dp,
                        color = Color.White.copy(alpha = 0.9f),
                        shape = CircleShape
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
    var widthPx by remember { mutableFloatStateOf(1f) }

    fun snapAndEmit(rawFraction: Float) {
        val clamped = rawFraction.coerceIn(0f, 1f)
        val snapped = if (steps > 0) {
            val segments = steps + 1
            (clamped * segments).roundToInt() / segments.toFloat()
        } else {
            clamped
        }
        onValueChange(valueRange.start + snapped * span)
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
            }
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(valueRange, steps) {
                detectTapGestures { offset ->
                    snapAndEmit(offset.x / widthPx)
                }
            }
            .pointerInput(valueRange, steps) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    snapAndEmit(change.position.x / widthPx)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(28.dp)) {
            val trackHeight = 10.dp.toPx()
            val thumbRadius = 12.dp.toPx()
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
 * Floating Liquid Glass bottom navigation pill respecting WindowInsets.navigationBars.
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
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(palette.glassBorderHighlight, palette.glassBorderTrailing)
                    ),
                    shape = pillShape
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val activePillBg by animateColorAsState(
                        targetValue = if (isSelected) palette.accentCool.copy(alpha = 0.22f) else Color.Transparent,
                        animationSpec = spring(stiffness = 400f),
                        label = "navPill"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) palette.accentCool else palette.textSecondary,
                        animationSpec = spring(stiffness = 400f),
                        label = "navTint"
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
                            .background(activePillBg)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) palette.accentCool.copy(alpha = 0.45f) else Color.Transparent,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true),
                                onClick = { onSelectTab(tab) }
                            )
                            .padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(21.dp)
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
