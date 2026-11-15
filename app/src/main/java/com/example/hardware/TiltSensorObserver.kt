package com.example.hardware

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.domain.model.TiltOffset
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Provides smoothed device tilt values while the UI is active to drive subtle parallax
 * and specular highlight shifts across Liquid Glass surfaces.
 */
class TiltSensorObserver @Inject constructor(
    private val sensorManager: SensorManager
) {
    fun observeTilt(): Flow<TiltOffset> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensor == null) {
            trySend(TiltOffset())
            awaitClose { }
            return@callbackFlow
        }

        var smoothX = 0f
        var smoothY = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rawX = (event.values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                val rawY = (event.values[1] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                smoothX = smoothX * 0.85f + rawX * 0.15f
                smoothY = smoothY * 0.85f + rawY * 0.15f
                trySend(TiltOffset(x = smoothX, y = smoothY))
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
