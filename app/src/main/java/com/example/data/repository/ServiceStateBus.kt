package com.example.data.repository

import com.example.domain.model.PauseReason
import com.example.domain.model.ServiceStateSnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Shared singleton reactive bus synchronizing hardware/service status between ShakeService and Compose UI.
 */
@Singleton
class ServiceStateBus @Inject constructor() {

    private val _state = MutableStateFlow(ServiceStateSnapshot())
    val state: StateFlow<ServiceStateSnapshot> = _state.asStateFlow()

    fun setServiceRunning(running: Boolean) {
        _state.update { it.copy(isServiceRunning = running) }
    }

    fun setTorchState(on: Boolean) {
        _state.update { it.copy(isTorchOn = on) }
    }

    fun setHasFlashUnit(hasFlash: Boolean) {
        _state.update { current ->
            val updatedPause = if (!hasFlash) PauseReason.NO_FLASH_HARDWARE else {
                if (current.pauseReason == PauseReason.NO_FLASH_HARDWARE) PauseReason.NONE else current.pauseReason
            }
            current.copy(hasFlashUnit = hasFlash, pauseReason = updatedPause)
        }
    }

    fun updateShakeMagnitude(magnitudeG: Float) {
        _state.update { current ->
            val decayedPeak = maxOf(magnitudeG, current.peakMagnitude * 0.96f)
            current.copy(
                liveShakeMagnitude = magnitudeG,
                peakMagnitude = decayedPeak
            )
        }
    }

    fun resetPeakMagnitude() {
        _state.update { it.copy(peakMagnitude = 0f, liveShakeMagnitude = 0f) }
    }

    fun setPauseReason(reason: PauseReason) {
        _state.update { it.copy(pauseReason = reason) }
    }

    fun setBatteryStatus(levelPercent: Int, isCharging: Boolean) {
        _state.update { it.copy(batteryLevelPercent = levelPercent, isCharging = isCharging) }
    }

    fun markTriggered(timestampMs: Long) {
        _state.update { it.copy(lastTriggerTimestampMs = timestampMs) }
    }
}
