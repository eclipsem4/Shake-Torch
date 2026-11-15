package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.hardware.ShakeDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
