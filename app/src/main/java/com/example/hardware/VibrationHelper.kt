package com.example.hardware

import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import com.example.domain.model.HapticIntensity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides distinct haptic feedback via VibratorManager (API 31+) when the flashlight toggles.
 * ON -> single crisp pulse; OFF -> double short pulse, scaled by the user's HapticIntensity setting.
 */
@Singleton
class VibrationHelper @Inject constructor(
    private val vibratorManager: VibratorManager
) {
    fun vibrateForTorchState(
        isNowOn: Boolean,
        intensity: HapticIntensity = HapticIntensity.MEDIUM
    ) {
        val defaultVibrator = vibratorManager.defaultVibrator
        if (!defaultVibrator.hasVibrator()) return

        val supportsAmplitude = defaultVibrator.hasAmplitudeControl()
        val effect = if (isNowOn) {
            val amplitude = if (supportsAmplitude) intensity.amplitude else VibrationEffect.DEFAULT_AMPLITUDE
            VibrationEffect.createOneShot(intensity.onDurationMs, amplitude)
        } else {
            val timings = longArrayOf(0L, intensity.offPulseMs, 70L, intensity.offPulseMs)
            if (supportsAmplitude) {
                val secondAmplitude = (intensity.amplitude * 0.82f).toInt().coerceAtLeast(1)
                val amplitudes = intArrayOf(0, intensity.amplitude, 0, secondAmplitude)
                VibrationEffect.createWaveform(timings, amplitudes, -1)
            } else {
                VibrationEffect.createWaveform(timings, -1)
            }
        }

        runCatching {
            vibratorManager.vibrate(CombinedVibration.createParallel(effect))
        }
    }

    fun previewIntensity(intensity: HapticIntensity) {
        vibrateForTorchState(isNowOn = true, intensity = intensity)
    }
}
