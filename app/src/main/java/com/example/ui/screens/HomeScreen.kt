package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.GestureMode
import com.example.domain.model.PauseReason
import com.example.hardware.ShakeDetector
import com.example.ui.components.GlassSlider
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiveShakeMeter
import com.example.ui.components.MorphingGlassPowerOrb
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.theme.TorchAmber
import com.example.ui.viewmodel.MainUiState
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    uiState: MainUiState,
    onToggleService: () -> Unit,
    onToggleManualTorch: () -> Unit,
    onResetPeak: () -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onSetMeterPreviewActive: (Boolean) -> Unit,
    bottomContentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val settings = uiState.settings
    val serviceState = uiState.serviceState

    DisposableEffect(serviceState.isServiceRunning) {
        onSetMeterPreviewActive(!serviceState.isServiceRunning)
        onDispose {
            onSetMeterPreviewActive(false)
        }
    }

    val statusColor by animateColorAsState(
        targetValue = when {
            !serviceState.hasFlashUnit -> palette.accentDanger
            serviceState.isTorchOn -> TorchAmber
            serviceState.pauseReason == PauseReason.IN_CALL -> palette.accentWarning
            serviceState.pauseReason == PauseReason.LOW_BATTERY -> palette.accentWarning
            serviceState.isServiceRunning -> palette.accentSuccess
            else -> palette.textSecondary
        },
        animationSpec = spring(),
        label = "statusPillColor"
    )

    val statusText = when {
        !serviceState.hasFlashUnit -> stringResource(R.string.status_no_flash)
        serviceState.pauseReason == PauseReason.IN_CALL -> stringResource(R.string.status_paused_call)
        serviceState.pauseReason == PauseReason.LOW_BATTERY -> stringResource(
            R.string.status_paused_battery,
            serviceState.batteryLevelPercent
        )
        serviceState.isTorchOn -> stringResource(R.string.status_torch_on)
        serviceState.isServiceRunning -> stringResource(R.string.status_listening)
        else -> stringResource(R.string.status_inactive)
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = bottomContentPadding.calculateBottomPadding() + 16.dp)
        ) {
            // Header + App PFP + Live Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("app_pfp_badge")
                            .clip(RoundedCornerShape(13.dp))
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        palette.glassBorderHighlight,
                                        palette.glassBorderTrailing
                                    )
                                ),
                                shape = RoundedCornerShape(13.dp)
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.user_pfp_flashlight),
                            contentDescription = stringResource(R.string.cd_app_pfp),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = palette.textPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = stringResource(R.string.home_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Live status glass pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(alpha = 0.16f))
                        .border(
                            width = 1.dp,
                            color = statusColor.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    val badgeIcon = when (serviceState.pauseReason) {
                        PauseReason.IN_CALL -> Icons.Rounded.PhoneInTalk
                        PauseReason.LOW_BATTERY -> Icons.Rounded.BatteryAlert
                        PauseReason.NO_FLASH_HARDWARE -> Icons.Rounded.WarningAmber
                        PauseReason.NONE -> Icons.Rounded.Sensors
                    }
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Friendly warning card if hardware lacks a camera flash unit
            AnimatedVisibility(visible = !serviceState.hasFlashUnit) {
                LiquidGlassCard(
                    tilt = uiState.tilt,
                    accentGlow = palette.accentDanger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = palette.accentDanger,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.no_flash_banner_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.no_flash_banner_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Centerpiece Morphing Liquid Glass Toggle Orb
            MorphingGlassPowerOrb(
                isServiceRunning = serviceState.isServiceRunning,
                isTorchOn = serviceState.isTorchOn,
                tilt = uiState.tilt,
                onClick = onToggleService
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Manual Torch Chip + Current Telemetry Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val torchActive = serviceState.isTorchOn
                val chipAccent = if (torchActive) TorchAmber else palette.accentCool

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("manual_torch_button")
                        .clip(RoundedCornerShape(50))
                        .background(chipAccent.copy(alpha = if (torchActive) 0.24f else 0.14f))
                        .border(
                            width = 1.dp,
                            color = chipAccent.copy(alpha = 0.50f),
                            shape = RoundedCornerShape(50)
                        )
                        .clickable(
                            enabled = serviceState.hasFlashUnit,
                            onClick = onToggleManualTorch
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = if (torchActive) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
                        contentDescription = null,
                        tint = chipAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(if (torchActive) R.string.quick_torch_on else R.string.quick_torch_off),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick summary badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassInfoBadge(
                    text = stringResource(
                        if (settings.gestureMode == GestureMode.DOUBLE_CHOP) {
                            R.string.home_badge_gesture_double
                        } else {
                            R.string.home_badge_gesture_single
                        }
                    )
                )
                GlassInfoBadge(
                    text = stringResource(R.string.home_badge_cooldown, settings.cooldownMillis / 1000f)
                )
                GlassInfoBadge(
                    text = stringResource(R.string.home_badge_battery, serviceState.batteryLevelPercent)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live Shake Meter Card
            LiquidGlassCard(
                tilt = uiState.tilt,
                accentGlow = if (serviceState.isTorchOn) TorchAmber else palette.accentCool,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shake_meter_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(palette.accentCool.copy(alpha = 0.16f))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = palette.accentCool,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.shake_meter_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = stringResource(
                                        if (settings.gestureMode == GestureMode.DOUBLE_CHOP) {
                                            R.string.shake_meter_hint_double
                                        } else {
                                            R.string.shake_meter_hint_single
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = onResetPeak,
                            modifier = Modifier.testTag("reset_peak_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Reset peak reading",
                                tint = palette.textSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LiveShakeMeter(
                        currentMagnitudeG = serviceState.liveShakeMagnitude,
                        peakMagnitudeG = serviceState.peakMagnitude,
                        sensitivity = settings.sensitivity
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val thresholdG = ShakeDetector.sensitivityToThresholdG(settings.sensitivity)
                    val sensitivityTier = when {
                        settings.sensitivity <= 3 -> stringResource(R.string.sensitivity_low)
                        settings.sensitivity <= 7 -> stringResource(R.string.sensitivity_medium)
                        else -> stringResource(R.string.sensitivity_high)
                    }
                    val thresholdReached = serviceState.liveShakeMagnitude >= thresholdG

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.shake_meter_live_g, serviceState.liveShakeMagnitude),
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = if (thresholdReached) TorchAmber else palette.accentCool,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.shake_meter_trigger_g, thresholdG),
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = TorchAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.shake_meter_peak_label, serviceState.peakMagnitude),
                            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            color = palette.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(
                                R.string.shake_meter_threshold_label,
                                settings.sensitivity,
                                sensitivityTier
                            ),
                            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                            color = palette.textPrimary
                        )

                        AnimatedVisibility(visible = thresholdReached) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(TorchAmber.copy(alpha = 0.22f))
                                    .border(
                                        width = 1.dp,
                                        color = TorchAmber.copy(alpha = 0.65f),
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.shake_meter_triggered_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TorchAmber,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    GlassSlider(
                        value = settings.sensitivity.toFloat(),
                        valueRange = 1f..10f,
                        steps = 8,
                        onValueChange = { onSensitivityChange(it.roundToInt()) },
                        accentColor = TorchAmber,
                        contentDescriptionText = stringResource(R.string.cd_sensitivity_slider),
                        modifier = Modifier.testTag("home_sensitivity_slider")
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassInfoBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(palette.glassFillTop)
            .border(
                width = 1.dp,
                color = palette.glassBorderHighlight.copy(alpha = 0.35f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = palette.textSecondary
        )
    }
}
