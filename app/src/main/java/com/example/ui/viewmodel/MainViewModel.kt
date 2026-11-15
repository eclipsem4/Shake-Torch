package com.example.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ServiceStateBus
import com.example.data.repository.SettingsRepository
import com.example.domain.model.AppThemeMode
import com.example.domain.model.GestureMode
import com.example.domain.model.HapticIntensity
import com.example.domain.model.PermissionStatus
import com.example.domain.model.SamplingRateOption
import com.example.domain.model.ServiceStateSnapshot
import com.example.domain.model.ShakeSettings
import com.example.domain.model.TiltOffset
import com.example.hardware.BatteryMonitor
import com.example.hardware.CallStateMonitor
import com.example.hardware.FlashlightController
import com.example.hardware.ShakeDetector
import com.example.hardware.TiltSensorObserver
import com.example.hardware.VibrationHelper
import com.example.service.ShakeService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val isLoaded: Boolean = false,
    val settings: ShakeSettings = ShakeSettings(),
    val serviceState: ServiceStateSnapshot = ServiceStateSnapshot(),
    val permissions: PermissionStatus = PermissionStatus(),
    val tilt: TiltOffset = TiltOffset()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val stateBus: ServiceStateBus,
    private val flashlightController: FlashlightController,
    private val vibrationHelper: VibrationHelper,
    private val batteryMonitor: BatteryMonitor,
    private val callStateMonitor: CallStateMonitor,
    private val tiltSensorObserver: TiltSensorObserver,
    sensorManager: SensorManager
) : ViewModel() {

    private val permissionsFlow = MutableStateFlow(readPermissionSnapshot())

    // Dedicated preview detector for live Shake Meter while service is stopped
    private val previewDetector = ShakeDetector(sensorManager)
    private var previewActive = false

    val uiState: StateFlow<MainUiState> = combine(
        settingsRepository.settingsFlow,
        stateBus.state,
        permissionsFlow,
        tiltSensorObserver.observeTilt()
    ) { settings, serviceState, permissions, tilt ->
        MainUiState(
            isLoaded = true,
            settings = settings,
            serviceState = serviceState,
            permissions = permissions,
            tilt = tilt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MainUiState(permissions = readPermissionSnapshot())
    )

    init {
        refreshSystemState()
    }

    fun refreshSystemState() {
        flashlightController.refreshHardwareAndRegisterCallback()
        val battery = batteryMonitor.refreshBatterySnapshot()
        stateBus.setBatteryStatus(battery.levelPercent, battery.isCharging)
        permissionsFlow.value = readPermissionSnapshot()
        if (stateBus.state.value.isServiceRunning && uiState.value.settings.pauseDuringCalls) {
            callStateMonitor.startMonitoring()
        }
    }

    fun setMeterPreviewActive(active: Boolean) {
        if (active && !stateBus.state.value.isServiceRunning) {
            if (!previewActive) {
                syncPreviewDetectorConfig()
                previewActive = previewDetector.start(object : ShakeDetector.Listener {
                    override fun onMagnitudeChanged(magnitudeG: Float) {
                        if (!stateBus.state.value.isServiceRunning) {
                            stateBus.updateShakeMagnitude(magnitudeG)
                        }
                    }

                    override fun onShakeDetected() = Unit
                })
            }
        } else {
            if (previewActive) {
                previewDetector.stop()
                previewActive = false
                if (!stateBus.state.value.isServiceRunning) {
                    stateBus.resetPeakMagnitude()
                }
            }
        }
    }

    private fun syncPreviewDetectorConfig(
        settings: ShakeSettings = uiState.value.settings
    ) {
        previewDetector.updateConfig(
            sensitivity = settings.sensitivity,
            gestureMode = settings.gestureMode,
            cooldownMillis = settings.cooldownMillis,
            samplingRate = SamplingRateOption.UI
        )
    }

    fun toggleShakeService() {
        val currentlyRunning = stateBus.state.value.isServiceRunning
        if (currentlyRunning) {
            appContext.startService(ShakeService.createStopIntent(appContext))
        } else {
            // Stop preview detector so ShakeService owns the accelerometer cleanly
            setMeterPreviewActive(false)
            ContextCompat.startForegroundService(
                appContext,
                ShakeService.createStartIntent(appContext)
            )
        }
    }

    fun toggleManualTorch() {
        val newState = flashlightController.toggleTorch()
        if (newState != null && uiState.value.settings.vibrateOnToggle) {
            vibrationHelper.vibrateForTorchState(
                isNowOn = newState,
                intensity = uiState.value.settings.hapticIntensity
            )
        }
    }

    fun resetPeakMagnitude() {
        stateBus.resetPeakMagnitude()
    }

    fun setSensitivity(value: Int) {
        if (previewActive) {
            syncPreviewDetectorConfig(uiState.value.settings.copy(sensitivity = value))
        }
        viewModelScope.launch { settingsRepository.setSensitivity(value) }
    }

    fun setGestureMode(mode: GestureMode) {
        if (previewActive) {
            syncPreviewDetectorConfig(uiState.value.settings.copy(gestureMode = mode))
        }
        viewModelScope.launch { settingsRepository.setGestureMode(mode) }
    }

    fun setCooldownMillis(millis: Long) {
        if (previewActive) {
            syncPreviewDetectorConfig(uiState.value.settings.copy(cooldownMillis = millis))
        }
        viewModelScope.launch { settingsRepository.setCooldownMillis(millis) }
    }

    fun setStartOnBoot(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setStartOnBoot(enabled) }
    }

    fun setVibrateOnToggle(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setVibrateOnToggle(enabled) }
        if (enabled) {
            vibrationHelper.previewIntensity(uiState.value.settings.hapticIntensity)
        }
    }

    fun setHapticIntensity(intensity: HapticIntensity) {
        viewModelScope.launch { settingsRepository.setHapticIntensity(intensity) }
        if (uiState.value.settings.vibrateOnToggle) {
            vibrationHelper.previewIntensity(intensity)
        }
    }

    fun setPauseDuringCalls(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPauseDuringCalls(enabled) }
    }

    fun setBatterySaverEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBatterySaverEnabled(enabled) }
    }

    fun setBatteryThresholdPercent(percent: Int) {
        viewModelScope.launch { settingsRepository.setBatteryThresholdPercent(percent) }
    }

    fun setSamplingRate(option: SamplingRateOption) {
        viewModelScope.launch { settingsRepository.setSamplingRate(option) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(true) }
    }

    fun replayOnboarding() {
        viewModelScope.launch { settingsRepository.setOnboardingCompleted(false) }
    }

    private fun readPermissionSnapshot(): PermissionStatus {
        val cameraGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val phoneStateGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        val ignoringBattery = batteryMonitor.isIgnoringBatteryOptimizations()

        return PermissionStatus(
            cameraGranted = cameraGranted,
            notificationsGranted = notificationsGranted,
            phoneStateGranted = phoneStateGranted,
            ignoringBatteryOptimizations = ignoringBattery
        )
    }

    override fun onCleared() {
        previewDetector.stop()
        super.onCleared()
    }
}
