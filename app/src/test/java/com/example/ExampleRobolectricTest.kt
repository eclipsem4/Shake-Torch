package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.SettingsRepository
import com.example.domain.model.GestureMode
import com.example.domain.model.HapticIntensity
import com.example.domain.model.SamplingRateOption
import com.example.hardware.ShakeDetector
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ShakeLight", appName)
    }

    @Test
    fun `higher sensitivity lowers g-force threshold`() {
        val lowSensitivityThreshold = ShakeDetector.sensitivityToThresholdG(1)
        val mediumSensitivityThreshold = ShakeDetector.sensitivityToThresholdG(5)
        val highSensitivityThreshold = ShakeDetector.sensitivityToThresholdG(10)

        assertTrue(lowSensitivityThreshold > mediumSensitivityThreshold)
        assertTrue(mediumSensitivityThreshold > highSensitivityThreshold)
        assertTrue(highSensitivityThreshold > 0.5f)
    }

    @Test
    fun `settings repository persists sensitivity threshold in datastore`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testFile = File(context.filesDir, "test_sensitivity.preferences_pb").apply { delete() }
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { testFile }
        )
        val repository = SettingsRepository(dataStore)

        assertEquals(6, repository.settingsFlow.first().sensitivity)
        repository.setSensitivity(9)
        assertEquals(9, repository.settingsFlow.first().sensitivity)

        // Clamping out-of-range sensitivity values keeps bounds 1..10
        repository.setSensitivity(99)
        assertEquals(10, repository.settingsFlow.first().sensitivity)
        testFile.delete()
    }

    @Test
    fun `settings repository persists haptic toggle and intensity in datastore`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testFile = File(context.filesDir, "test_haptics.preferences_pb").apply { delete() }
        val dataStore = PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { testFile }
        )
        val repository = SettingsRepository(dataStore)

        val initial = repository.settingsFlow.first()
        assertTrue(initial.vibrateOnToggle)
        assertEquals(HapticIntensity.MEDIUM, initial.hapticIntensity)

        repository.setHapticIntensity(HapticIntensity.STRONG)
        assertEquals(HapticIntensity.STRONG, repository.settingsFlow.first().hapticIntensity)

        repository.setVibrateOnToggle(false)
        assertEquals(false, repository.settingsFlow.first().vibrateOnToggle)

        repository.setVibrateOnToggle(true)
        repository.setHapticIntensity(HapticIntensity.LIGHT)
        val updated = repository.settingsFlow.first()
        assertTrue(updated.vibrateOnToggle)
        assertEquals(HapticIntensity.LIGHT, updated.hapticIntensity)

        testFile.delete()
    }

    @Test
    fun `double chop triggers once on two crisp impulses and respects cooldown`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        Shadows.shadowOf(sensorManager).addSensor(ShadowSensor.newInstance(Sensor.TYPE_ACCELEROMETER))

        val detector = ShakeDetector(sensorManager)
        detector.updateConfig(
            sensitivity = 8,
            gestureMode = GestureMode.DOUBLE_CHOP,
            cooldownMillis = 1000L,
            samplingRate = SamplingRateOption.NORMAL
        )

        var triggerCount = 0
        val listener = object : ShakeDetector.Listener {
            override fun onMagnitudeChanged(magnitudeG: Float) = Unit
            override fun onShakeDetected() {
                triggerCount++
            }
        }
        assertTrue(detector.start(listener))

        // Baseline gravity calibration at 1g on Z axis
        detector.processAcceleration(0f, 0f, 9.81f, 0L, 0L)
        // First chop impulse (~2.5g linear)
        detector.processAcceleration(24.5f, 0f, 9.81f, 200_000_000L, 200L)
        assertEquals(0, triggerCount)

        // Valley between chops
        detector.processAcceleration(0f, 0f, 9.81f, 400_000_000L, 400L)
        assertEquals(0, triggerCount)

        // Second chop impulse within 950ms window -> should trigger
        detector.processAcceleration(24.5f, 0f, 9.81f, 600_000_000L, 600L)
        assertEquals(1, triggerCount)

        // Immediate follow-up double chop inside 1000ms cooldown -> should be ignored
        detector.processAcceleration(0f, 0f, 9.81f, 800_000_000L, 800L)
        detector.processAcceleration(24.5f, 0f, 9.81f, 1_000_000_000L, 1000L)
        detector.processAcceleration(0f, 0f, 9.81f, 1_200_000_000L, 1200L)
        detector.processAcceleration(24.5f, 0f, 9.81f, 1_400_000_000L, 1400L)
        assertEquals(1, triggerCount)

        detector.stop()
    }
}
