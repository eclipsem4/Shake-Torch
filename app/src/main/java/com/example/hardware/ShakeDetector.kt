package com.example.hardware

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import com.example.domain.model.GestureMode
import com.example.domain.model.SamplingRateOption
import javax.inject.Inject
import kotlin.math.sqrt

/**
 * Processes TYPE_ACCELEROMETER events with a low-pass gravity estimator and high-pass filter
 * to detect single shakes or double-chop gestures while exposing live shake magnitude in G-force.
 */
class ShakeDetector @Inject constructor(
    private val sensorManager: SensorManager
) : SensorEventListener {

    interface Listener {
        fun onMagnitudeChanged(magnitudeG: Float)
        fun onShakeDetected()
    }

    private var listener: Listener? = null
    private var isListening = false

    @Volatile
    private var sensitivity: Int = 6

    @Volatile
    private var gestureMode: GestureMode = GestureMode.DOUBLE_CHOP

    @Volatile
    private var cooldownMillis: Long = 1500L

    @Volatile
    private var samplingRate: SamplingRateOption = SamplingRateOption.NORMAL

    // Low-pass gravity vector
    private val gravity = FloatArray(3)
    private var gravityInitialized = false

    // Double-chop state machine
    private var firstChopTimestampMs: Long = 0L
    private var droppedBetweenChops: Boolean = false
    private var lastTriggerTimestampMs: Long = 0L

    fun updateConfig(
        sensitivity: Int,
        gestureMode: GestureMode,
        cooldownMillis: Long,
        samplingRate: SamplingRateOption
    ) {
        val rateChanged = this.samplingRate != samplingRate
        this.sensitivity = sensitivity.coerceIn(1, 10)
        this.gestureMode = gestureMode
        this.cooldownMillis = cooldownMillis.coerceIn(500L, 5000L)
        this.samplingRate = samplingRate

        if (isListening && rateChanged) {
            val currentListener = listener
            stop()
            if (currentListener != null) {
                start(currentListener)
            }
        }
    }

    fun start(listener: Listener): Boolean {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return false
        if (isListening) {
            this.listener = listener
            return true
        }

        this.listener = listener
        gravityInitialized = false
        firstChopTimestampMs = 0L
        droppedBetweenChops = false

        val delayUs = when (samplingRate) {
            SamplingRateOption.NORMAL -> SensorManager.SENSOR_DELAY_NORMAL
            SamplingRateOption.UI -> SensorManager.SENSOR_DELAY_UI
        }
        // Batch sensor delivery briefly in low-power mode to minimize CPU wakeups.
        val maxReportLatencyUs = when (samplingRate) {
            SamplingRateOption.NORMAL -> 120_000
            SamplingRateOption.UI -> 0
        }

        isListening = sensorManager.registerListener(
            this,
            accelerometer,
            delayUs,
            maxReportLatencyUs
        )
        return isListening
    }

    fun stop() {
        if (isListening) {
            sensorManager.unregisterListener(this)
            isListening = false
        }
        listener = null
        gravityInitialized = false
        firstChopTimestampMs = 0L
        droppedBetweenChops = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        if (!gravityInitialized) {
            gravity[0] = event.values[0]
            gravity[1] = event.values[1]
            gravity[2] = event.values[2]
            gravityInitialized = true
            return
        }

        // Low-pass filter isolates gravity; subtracting it yields high-pass linear acceleration.
        val alpha = 0.80f
        gravity[0] = alpha * gravity[0] + (1f - alpha) * event.values[0]
        gravity[1] = alpha * gravity[1] + (1f - alpha) * event.values[1]
        gravity[2] = alpha * gravity[2] + (1f - alpha) * event.values[2]

        val linearX = event.values[0] - gravity[0]
        val linearY = event.values[1] - gravity[1]
        val linearZ = event.values[2] - gravity[2]

        val linearMagnitude = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)
        val gForce = linearMagnitude / SensorManager.GRAVITY_EARTH

        listener?.onMagnitudeChanged(gForce)

        val now = SystemClock.elapsedRealtime()
        if (now - lastTriggerTimestampMs < cooldownMillis) {
            firstChopTimestampMs = 0L
            droppedBetweenChops = false
            return
        }

        val thresholdG = sensitivityToThresholdG(sensitivity)

        when (gestureMode) {
            GestureMode.SINGLE_SHAKE -> {
                if (gForce >= thresholdG) {
                    lastTriggerTimestampMs = now
                    listener?.onShakeDetected()
                }
            }

            GestureMode.DOUBLE_CHOP -> {
                if (firstChopTimestampMs == 0L) {
                    if (gForce >= thresholdG) {
                        firstChopTimestampMs = now
                        droppedBetweenChops = false
                    }
                } else {
                    val elapsedSinceFirst = now - firstChopTimestampMs
                    if (elapsedSinceFirst > DOUBLE_CHOP_WINDOW_MS) {
                        // Window expired; start fresh if still above threshold.
                        firstChopTimestampMs = if (gForce >= thresholdG) now else 0L
                        droppedBetweenChops = false
                    } else if (!droppedBetweenChops) {
                        if (gForce < thresholdG * 0.62f || elapsedSinceFirst >= MIN_CHOP_GAP_MS) {
                            if (gForce < thresholdG * 0.72f) {
                                droppedBetweenChops = true
                            }
                        }
                    } else if (gForce >= thresholdG && elapsedSinceFirst >= MIN_CHOP_GAP_MS) {
                        firstChopTimestampMs = 0L
                        droppedBetweenChops = false
                        lastTriggerTimestampMs = now
                        listener?.onShakeDetected()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val DOUBLE_CHOP_WINDOW_MS = 700L
        private const val MIN_CHOP_GAP_MS = 110L
        const val MAX_METER_G = 3.5f

        /**
         * Maps sensitivity (1 = low/hard shake .. 10 = high/light shake) to G-force threshold.
         */
        fun sensitivityToThresholdG(sensitivity: Int): Float {
            val clamped = sensitivity.coerceIn(1, 10)
            return 3.02f - (clamped * 0.217f)
        }
    }
}
