package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppThemeMode
import com.example.ui.navigation.ShakeLightRoot
import com.example.ui.theme.ShakeLightTheme
import com.example.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: MainViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val systemDark = isSystemInDarkTheme()
            val isDark = when (uiState.settings.themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
            }

            LaunchedEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }

            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSystemState()
            }

            val singlePermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) {
                viewModel.refreshSystemState()
            }

            val multiPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) {
                viewModel.refreshSystemState()
            }

            ShakeLightTheme(themeMode = uiState.settings.themeMode) {
                ShakeLightRoot(
                    uiState = uiState,
                    onToggleService = {
                        if (!uiState.serviceState.isServiceRunning &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !uiState.permissions.notificationsGranted
                        ) {
                            singlePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        viewModel.toggleShakeService()
                    },
                    onToggleManualTorch = viewModel::toggleManualTorch,
                    onResetPeak = viewModel::resetPeakMagnitude,
                    onSetMeterPreviewActive = viewModel::setMeterPreviewActive,
                    onSensitivityChange = viewModel::setSensitivity,
                    onGestureModeChange = viewModel::setGestureMode,
                    onCooldownChange = viewModel::setCooldownMillis,
                    onVibrateChange = viewModel::setVibrateOnToggle,
                    onPauseDuringCallsChange = viewModel::setPauseDuringCalls,
                    onStartOnBootChange = viewModel::setStartOnBoot,
                    onBatterySaverChange = viewModel::setBatterySaverEnabled,
                    onBatteryThresholdChange = viewModel::setBatteryThresholdPercent,
                    onSamplingRateChange = viewModel::setSamplingRate,
                    onThemeModeChange = viewModel::setThemeMode,
                    onCompleteOnboarding = viewModel::completeOnboarding,
                    onReplayOnboarding = viewModel::replayOnboarding,
                    onRequestCorePermissions = {
                        val permissions = buildList {
                            add(Manifest.permission.CAMERA)
                            add(Manifest.permission.READ_PHONE_STATE)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }.toTypedArray()
                        multiPermissionLauncher.launch(permissions)
                    },
                    onRequestCameraPermission = {
                        singlePermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onRequestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            singlePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onRequestPhonePermission = {
                        singlePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                    }
                )
            }
        }
    }
}
