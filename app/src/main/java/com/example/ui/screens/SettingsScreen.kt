package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AppThemeMode
import com.example.domain.model.GestureMode
import com.example.domain.model.SamplingRateOption
import com.example.ui.components.GlassPillSwitch
import com.example.ui.components.GlassSlider
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.theme.TorchAmber
import com.example.ui.viewmodel.MainUiState
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    uiState: MainUiState,
    onSensitivityChange: (Int) -> Unit,
    onGestureModeChange: (GestureMode) -> Unit,
    onCooldownChange: (Long) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onPauseDuringCallsChange: (Boolean) -> Unit,
    onRequestPhonePermission: () -> Unit,
    onStartOnBootChange: (Boolean) -> Unit,
    onBatterySaverChange: (Boolean) -> Unit,
    onBatteryThresholdChange: (Int) -> Unit,
    onSamplingRateChange: (SamplingRateOption) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onReplayOnboarding: () -> Unit,
    bottomContentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val settings = uiState.settings

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = bottomContentPadding.calculateBottomPadding() + 16.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.displaySmall,
                color = palette.textPrimary
            )
            Text(
                text = stringResource(R.string.settings_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader(text = stringResource(R.string.settings_section_gesture))

            // Motion & Gesture Glass Card
            LiquidGlassCard(
                tilt = uiState.tilt,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Gesture Mode Selector
                    Text(
                        text = stringResource(R.string.settings_gesture_mode_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.settings_gesture_mode_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    GlassSegmentedSelector(
                        options = listOf(
                            GestureMode.DOUBLE_CHOP to stringResource(R.string.settings_gesture_double),
                            GestureMode.SINGLE_SHAKE to stringResource(R.string.settings_gesture_single)
                        ),
                        selected = settings.gestureMode,
                        onSelect = onGestureModeChange,
                        testTagPrefix = "gesture_mode"
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Shake Intensity Sensitivity Slider (1..10)
                    val sensitivityLabel = when {
                        settings.sensitivity <= 3 -> stringResource(R.string.sensitivity_low)
                        settings.sensitivity <= 7 -> stringResource(R.string.sensitivity_medium)
                        else -> stringResource(R.string.sensitivity_high)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_sensitivity_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.textPrimary
                            )
                            Text(
                                text = stringResource(R.string.settings_sensitivity_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                        ValueChip(text = "${settings.sensitivity}/10 • $sensitivityLabel")
                    }

                    GlassSlider(
                        value = settings.sensitivity.toFloat(),
                        valueRange = 1f..10f,
                        steps = 8,
                        onValueChange = { onSensitivityChange(it.roundToInt()) },
                        accentColor = TorchAmber,
                        contentDescriptionText = stringResource(R.string.cd_sensitivity_slider),
                        modifier = Modifier.testTag("sensitivity_slider")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Cooldown Slider (0.5s .. 5.0s)
                    val cooldownSeconds = settings.cooldownMillis / 1000f
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = palette.accentCool,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.settings_cooldown_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = stringResource(R.string.settings_cooldown_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        ValueChip(text = stringResource(R.string.settings_cooldown_value, cooldownSeconds))
                    }

                    GlassSlider(
                        value = cooldownSeconds,
                        valueRange = 0.5f..5.0f,
                        steps = 8,
                        onValueChange = { raw ->
                            val snappedTenths = ((raw * 2f).roundToInt() * 500L).coerceIn(500L, 5000L)
                            onCooldownChange(snappedTenths)
                        },
                        contentDescriptionText = stringResource(R.string.cd_cooldown_slider),
                        modifier = Modifier.testTag("cooldown_slider")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(text = stringResource(R.string.settings_section_behavior))

            // Feedback & Automation Card
            LiquidGlassCard(
                tilt = uiState.tilt,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SettingSwitchRow(
                        icon = Icons.Rounded.Vibration,
                        title = stringResource(R.string.settings_vibrate_title),
                        subtitle = stringResource(R.string.settings_vibrate_desc),
                        checked = settings.vibrateOnToggle,
                        onCheckedChange = onVibrateChange,
                        testTag = "vibrate_switch"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchRow(
                        icon = Icons.Rounded.PhoneDisabled,
                        title = stringResource(R.string.settings_pause_calls_title),
                        subtitle = stringResource(R.string.settings_pause_calls_desc),
                        checked = settings.pauseDuringCalls,
                        onCheckedChange = { enabled ->
                            onPauseDuringCallsChange(enabled)
                            if (enabled && !uiState.permissions.phoneStateGranted) {
                                onRequestPhonePermission()
                            }
                        },
                        testTag = "pause_calls_switch"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SettingSwitchRow(
                        icon = Icons.Rounded.PowerSettingsNew,
                        title = stringResource(R.string.settings_boot_title),
                        subtitle = stringResource(R.string.settings_boot_desc),
                        checked = settings.startOnBoot,
                        onCheckedChange = onStartOnBootChange,
                        testTag = "start_on_boot_switch"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(text = stringResource(R.string.settings_section_power))

            // Power & Sampling Card
            LiquidGlassCard(
                tilt = uiState.tilt,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SettingSwitchRow(
                        icon = Icons.Rounded.BatterySaver,
                        title = stringResource(R.string.settings_battery_saver_title),
                        subtitle = stringResource(R.string.settings_battery_saver_desc),
                        checked = settings.batterySaverEnabled,
                        onCheckedChange = onBatterySaverChange,
                        testTag = "battery_saver_switch"
                    )

                    AnimatedVisibility(visible = settings.batterySaverEnabled) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.settings_battery_threshold_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.settings_battery_threshold_desc,
                                            settings.batteryThresholdPercent
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.textSecondary
                                    )
                                }
                                ValueChip(text = "${settings.batteryThresholdPercent}%")
                            }

                            GlassSlider(
                                value = settings.batteryThresholdPercent.toFloat(),
                                valueRange = 5f..50f,
                                steps = 8,
                                onValueChange = { raw ->
                                    val snapped = ((raw / 5f).roundToInt() * 5).coerceIn(5, 50)
                                    onBatteryThresholdChange(snapped)
                                },
                                accentColor = palette.accentSuccess,
                                contentDescriptionText = stringResource(R.string.cd_battery_threshold_slider),
                                modifier = Modifier.testTag("battery_threshold_slider")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = palette.accentCool,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_sampling_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    GlassSegmentedSelector(
                        options = listOf(
                            SamplingRateOption.NORMAL to stringResource(R.string.settings_sampling_normal),
                            SamplingRateOption.UI to stringResource(R.string.settings_sampling_ui)
                        ),
                        selected = if (settings.batterySaverEnabled) SamplingRateOption.NORMAL else settings.samplingRate,
                        onSelect = onSamplingRateChange,
                        enabled = !settings.batterySaverEnabled,
                        testTagPrefix = "sampling_rate"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionHeader(text = stringResource(R.string.settings_section_appearance))

            LiquidGlassCard(
                tilt = uiState.tilt,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = palette.accentCool,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.settings_theme_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    GlassSegmentedSelector(
                        options = listOf(
                            AppThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                            AppThemeMode.DARK to stringResource(R.string.settings_theme_dark),
                            AppThemeMode.LIGHT to stringResource(R.string.settings_theme_light)
                        ),
                        selected = settings.themeMode,
                        onSelect = onThemeModeChange,
                        testTagPrefix = "theme_mode"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("replay_onboarding_button")
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.textPrimary.copy(alpha = 0.08f))
                            .border(
                                width = 1.dp,
                                color = palette.glassBorderHighlight.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(onClick = onReplayOnboarding)
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_replay_onboarding),
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    val palette = LocalGlassPalette.current
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = palette.accentCool,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun ValueChip(text: String) {
    val palette = LocalGlassPalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(palette.accentCool.copy(alpha = 0.16f))
            .border(
                width = 1.dp,
                color = palette.accentCool.copy(alpha = 0.35f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = palette.textPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val palette = LocalGlassPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = palette.accentCool,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        GlassPillSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            contentDescriptionText = title,
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun <T> GlassSegmentedSelector(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
    testTagPrefix: String
) {
    val palette = LocalGlassPalette.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.textPrimary.copy(alpha = 0.07f))
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, (option, label) ->
            val isSelected = option == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .testTag("${testTagPrefix}_$index")
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) palette.accentCool.copy(alpha = 0.24f) else Color.Transparent
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) palette.accentCool.copy(alpha = 0.55f) else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable(enabled = enabled, onClick = { onSelect(option) })
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) palette.textPrimary else palette.textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}
