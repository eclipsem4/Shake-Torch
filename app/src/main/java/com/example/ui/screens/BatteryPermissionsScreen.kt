package com.example.ui.screens

import android.os.Build
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.utils.SystemIntents
import com.example.ui.viewmodel.MainUiState

@Composable
fun BatteryPermissionsScreen(
    uiState: MainUiState,
    onRequestCameraPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestPhonePermission: () -> Unit,
    bottomContentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val context = LocalContext.current
    val perms = uiState.permissions
    var oemExpanded by remember { mutableStateOf(true) }

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
                text = stringResource(R.string.system_title),
                style = MaterialTheme.typography.displaySmall,
                color = palette.textPrimary
            )
            Text(
                text = stringResource(R.string.system_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            PermissionStatusCard(
                icon = Icons.Rounded.CameraAlt,
                title = stringResource(R.string.perm_camera_title),
                description = stringResource(R.string.perm_camera_desc),
                isSatisfied = perms.cameraGranted,
                satisfiedText = stringResource(R.string.status_granted),
                missingText = stringResource(R.string.status_missing),
                actionLabel = stringResource(R.string.action_grant),
                onActionClick = onRequestCameraPermission,
                tilt = uiState.tilt,
                testTag = "perm_camera_card"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Spacer(modifier = Modifier.height(12.dp))
                PermissionStatusCard(
                    icon = Icons.Rounded.NotificationsActive,
                    title = stringResource(R.string.perm_notifications_title),
                    description = stringResource(R.string.perm_notifications_desc),
                    isSatisfied = perms.notificationsGranted,
                    satisfiedText = stringResource(R.string.status_granted),
                    missingText = stringResource(R.string.status_missing),
                    actionLabel = stringResource(R.string.action_grant),
                    onActionClick = onRequestNotificationPermission,
                    tilt = uiState.tilt,
                    testTag = "perm_notifications_card"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            PermissionStatusCard(
                icon = Icons.Rounded.PhoneInTalk,
                title = stringResource(R.string.perm_phone_title),
                description = stringResource(R.string.perm_phone_desc),
                isSatisfied = perms.phoneStateGranted,
                satisfiedText = stringResource(R.string.status_granted),
                missingText = stringResource(R.string.status_missing),
                actionLabel = stringResource(R.string.action_grant),
                onActionClick = onRequestPhonePermission,
                tilt = uiState.tilt,
                testTag = "perm_phone_card"
            )

            Spacer(modifier = Modifier.height(12.dp))
            PermissionStatusCard(
                icon = Icons.Rounded.BatteryChargingFull,
                title = stringResource(R.string.perm_battery_title),
                description = stringResource(R.string.perm_battery_desc),
                isSatisfied = perms.ignoringBatteryOptimizations,
                satisfiedText = stringResource(R.string.status_exempt),
                missingText = stringResource(R.string.status_restricted),
                actionLabel = stringResource(R.string.action_allow_unrestricted),
                onActionClick = { SystemIntents.openBatteryOptimizationExemption(context) },
                tilt = uiState.tilt,
                testTag = "perm_battery_card"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // OEM Background Restriction Guide
            LiquidGlassCard(
                tilt = uiState.tilt,
                onClick = { oemExpanded = !oemExpanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("oem_tips_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.oem_tips_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = palette.textPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.oem_tips_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary
                            )
                        }
                        Icon(
                            imageVector = if (oemExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = palette.textSecondary
                        )
                    }

                    AnimatedVisibility(visible = oemExpanded) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            OemTipBlock(
                                brand = stringResource(R.string.oem_xiaomi_title),
                                steps = stringResource(R.string.oem_xiaomi_body)
                            )
                            OemTipBlock(
                                brand = stringResource(R.string.oem_oppo_title),
                                steps = stringResource(R.string.oem_oppo_body)
                            )
                            OemTipBlock(
                                brand = stringResource(R.string.oem_vivo_title),
                                steps = stringResource(R.string.oem_vivo_body)
                            )
                            OemTipBlock(
                                brand = stringResource(R.string.oem_samsung_title),
                                steps = stringResource(R.string.oem_samsung_body)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("open_app_settings_button")
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(palette.accentCool.copy(alpha = 0.18f))
                                    .border(
                                        width = 1.dp,
                                        color = palette.accentCool.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable { SystemIntents.openAppDetailsSettings(context) }
                                    .padding(vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = null,
                                    tint = palette.accentCool,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_open_settings),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = palette.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    icon: ImageVector,
    title: String,
    description: String,
    isSatisfied: Boolean,
    satisfiedText: String,
    missingText: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    tilt: com.example.domain.model.TiltOffset,
    testTag: String
) {
    val palette = LocalGlassPalette.current
    val statusColor = if (isSatisfied) palette.accentSuccess else palette.accentWarning

    LiquidGlassCard(
        tilt = tilt,
        accentGlow = statusColor,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(statusColor.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = if (isSatisfied) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSatisfied) satisfiedText else missingText,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary
            )

            if (!isSatisfied) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("${testTag}_fix_button")
                        .clip(RoundedCornerShape(14.dp))
                        .background(statusColor.copy(alpha = 0.22f))
                        .border(
                            width = 1.dp,
                            color = statusColor.copy(alpha = 0.50f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable(onClick = onActionClick)
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun OemTipBlock(
    brand: String,
    steps: String
) {
    val palette = LocalGlassPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.textPrimary.copy(alpha = 0.06f))
            .padding(12.dp)
    ) {
        Text(
            text = brand,
            style = MaterialTheme.typography.labelLarge,
            color = palette.accentCool,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = steps,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textSecondary
        )
    }
}
