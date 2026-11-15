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
    private var lastEventTimestampNs: Long = 0L

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
        lastEventTimestampNs = 0L
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
        lastEventTimestampNs = 0L
        firstChopTimestampMs = 0L
        droppedBetweenChops = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        processAcceleration(
            ax = event.values[0],
            ay = event.values[1],
            az = event.values[2],
            eventTimestampNs = event.timestamp,
            elapsedRealtimeMs = SystemClock.elapsedRealtime()
        )
    }

    internal fun processAcceleration(
        ax: Float,
        ay: Float,
        az: Float,
        eventTimestampNs: Long,
        elapsedRealtimeMs: Long
    ) {
        if (!gravityInitialized) {
            gravity[0] = ax
            gravity[1] = ay
            gravity[2] = az
            gravityInitialized = true
            lastEventTimestampNs = eventTimestampNs
            return
        }

        // Dynamic alpha based on actual sample delta keeps gravity isolation consistent across NORMAL & UI rates.
        val dtSeconds = if (lastEventTimestampNs > 0L && eventTimestampNs > lastEventTimestampNs) {
            ((eventTimestampNs - lastEventTimestampNs) / 1_000_000_000f).coerceIn(0.005f, 0.5f)
        } else {
            0.06f
        }
        lastEventTimestampNs = eventTimestampNs
        val tau = 0.80f
        val alpha = tau / (tau + dtSeconds)

        gravity[0] = alpha * gravity[0] + (1f - alpha) * ax
        gravity[1] = alpha * gravity[1] + (1f - alpha) * ay
        gravity[2] = alpha * gravity[2] + (1f - alpha) * az

        val linearX = ax - gravity[0]
        val linearY = ay - gravity[1]
        val linearZ = az - gravity[2]

        val linearMagnitude = sqrt(linearX * linearX + linearY * linearY + linearZ * linearZ)
        val gForce = linearMagnitude / SensorManager.GRAVITY_EARTH

        listener?.onMagnitudeChanged(gForce)

        if (lastTriggerTimestampMs > 0L && elapsedRealtimeMs - lastTriggerTimestampMs < cooldownMillis) {
            firstChopTimestampMs = 0L
            droppedBetweenChops = false
            return
        }

        val thresholdG = sensitivityToThresholdG(sensitivity)

        when (gestureMode) {
            GestureMode.SINGLE_SHAKE -> {
                if (gForce >= thresholdG) {
                    lastTriggerTimestampMs = elapsedRealtimeMs
                    listener?.onShakeDetected()
                }
            }

            GestureMode.DOUBLE_CHOP -> {
                if (firstChopTimestampMs == 0L) {
                    if (gForce >= thresholdG) {
                        firstChopTimestampMs = elapsedRealtimeMs
                        droppedBetweenChops = false
                    }
                } else {
                    val elapsedSinceFirst = elapsedRealtimeMs - firstChopTimestampMs
                    if (elapsedSinceFirst > DOUBLE_CHOP_WINDOW_MS) {
                        // Window expired; start fresh if still above threshold.
                        firstChopTimestampMs = if (gForce >= thresholdG) elapsedRealtimeMs else 0L
                        droppedBetweenChops = false
                    } else if (!droppedBetweenChops) {
                        if (gForce < thresholdG * 0.70f) {
                            droppedBetweenChops = true
                        }
                    } else if (gForce >= thresholdG && elapsedSinceFirst >= MIN_CHOP_GAP_MS) {
                        firstChopTimestampMs = 0L
                        droppedBetweenChops = false
                        lastTriggerTimestampMs = elapsedRealtimeMs
                        listener?.onShakeDetected()
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val DOUBLE_CHOP_WINDOW_MS = 950L
        private const val MIN_CHOP_GAP_MS = 100L
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
