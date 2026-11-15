package com.example.hardware

import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.VibratorManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides distinct haptic feedback via VibratorManager (API 31+) when the flashlight toggles.
 * ON -> single crisp pulse; OFF -> double short pulse.
 */
@Singleton
class VibrationHelper @Inject constructor(
    private val vibratorManager: VibratorManager
) {
    fun vibrateForTorchState(isNowOn: Boolean) {
        val defaultVibrator = vibratorManager.defaultVibrator
        if (!defaultVibrator.hasVibrator()) return

        val effect = if (isNowOn) {
            VibrationEffect.createOneShot(55L, VibrationEffect.DEFAULT_AMPLITUDE)
        } else {
            val timings = longArrayOf(0L, 40L, 75L, 45L)
            val amplitudes = intArrayOf(0, 190, 0, 160)
            VibrationEffect.createWaveform(timings, amplitudes, -1)
        }

        runCatching {
            vibratorManager.vibrate(CombinedVibration.createParallel(effect))
        }
    }
}
