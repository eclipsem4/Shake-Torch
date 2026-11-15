package com.example.domain.model

enum class GestureMode {
    DOUBLE_CHOP,
    SINGLE_SHAKE
}

enum class SamplingRateOption {
    NORMAL,
    UI
}

enum class AppThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

enum class PauseReason {
    NONE,
    IN_CALL,
    LOW_BATTERY,
    NO_FLASH_HARDWARE
}

data class ShakeSettings(
    val sensitivity: Int = 6,
    val gestureMode: GestureMode = GestureMode.DOUBLE_CHOP,
    val cooldownMillis: Long = 1500L,
    val startOnBoot: Boolean = false,
    val vibrateOnToggle: Boolean = true,
    val pauseDuringCalls: Boolean = true,
    val batterySaverEnabled: Boolean = false,
    val batteryThresholdPercent: Int = 15,
    val samplingRate: SamplingRateOption = SamplingRateOption.NORMAL,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val onboardingCompleted: Boolean = false
)

data class ServiceStateSnapshot(
    val isServiceRunning: Boolean = false,
    val isTorchOn: Boolean = false,
    val hasFlashUnit: Boolean = true,
    val liveShakeMagnitude: Float = 0f,
    val peakMagnitude: Float = 0f,
    val pauseReason: PauseReason = PauseReason.NONE,
    val batteryLevelPercent: Int = 100,
    val isCharging: Boolean = false,
    val lastTriggerTimestampMs: Long = 0L
)

data class PermissionStatus(
    val cameraGranted: Boolean = false,
    val notificationsGranted: Boolean = true,
    val phoneStateGranted: Boolean = false,
    val ignoringBatteryOptimizations: Boolean = false
)

data class TiltOffset(
    val x: Float = 0f,
    val y: Float = 0f
)
