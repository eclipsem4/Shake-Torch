package com.example.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.domain.model.AppThemeMode
import com.example.domain.model.GestureMode
import com.example.domain.model.SamplingRateOption
import com.example.domain.model.ShakeSettings
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    private object Keys {
        val SENSITIVITY = intPreferencesKey("sensitivity")
        val GESTURE_MODE = stringPreferencesKey("gesture_mode")
        val COOLDOWN_MILLIS = longPreferencesKey("cooldown_millis")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val VIBRATE_ON_TOGGLE = booleanPreferencesKey("vibrate_on_toggle")
        val PAUSE_DURING_CALLS = booleanPreferencesKey("pause_during_calls")
        val BATTERY_SAVER_ENABLED = booleanPreferencesKey("battery_saver_enabled")
        val BATTERY_THRESHOLD_PERCENT = intPreferencesKey("battery_threshold_percent")
        val SAMPLING_RATE = stringPreferencesKey("sampling_rate")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val settingsFlow: Flow<ShakeSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            ShakeSettings(
                sensitivity = (prefs[Keys.SENSITIVITY] ?: 6).coerceIn(1, 10),
                gestureMode = prefs[Keys.GESTURE_MODE]?.let {
                    runCatching { GestureMode.valueOf(it) }.getOrNull()
                } ?: GestureMode.DOUBLE_CHOP,
                cooldownMillis = (prefs[Keys.COOLDOWN_MILLIS] ?: 1500L).coerceIn(500L, 5000L),
                startOnBoot = prefs[Keys.START_ON_BOOT] ?: false,
                vibrateOnToggle = prefs[Keys.VIBRATE_ON_TOGGLE] ?: true,
                pauseDuringCalls = prefs[Keys.PAUSE_DURING_CALLS] ?: true,
                batterySaverEnabled = prefs[Keys.BATTERY_SAVER_ENABLED] ?: false,
                batteryThresholdPercent = (prefs[Keys.BATTERY_THRESHOLD_PERCENT] ?: 15).coerceIn(5, 50),
                samplingRate = prefs[Keys.SAMPLING_RATE]?.let {
                    runCatching { SamplingRateOption.valueOf(it) }.getOrNull()
                } ?: SamplingRateOption.NORMAL,
                themeMode = prefs[Keys.THEME_MODE]?.let {
                    runCatching { AppThemeMode.valueOf(it) }.getOrNull()
                } ?: AppThemeMode.SYSTEM,
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
            )
        }

    suspend fun setSensitivity(value: Int) {
        dataStore.edit { it[Keys.SENSITIVITY] = value.coerceIn(1, 10) }
    }

    suspend fun setGestureMode(mode: GestureMode) {
        dataStore.edit { it[Keys.GESTURE_MODE] = mode.name }
    }

    suspend fun setCooldownMillis(millis: Long) {
        dataStore.edit { it[Keys.COOLDOWN_MILLIS] = millis.coerceIn(500L, 5000L) }
    }

    suspend fun setStartOnBoot(enabled: Boolean) {
        dataStore.edit { it[Keys.START_ON_BOOT] = enabled }
    }

    suspend fun setVibrateOnToggle(enabled: Boolean) {
        dataStore.edit { it[Keys.VIBRATE_ON_TOGGLE] = enabled }
    }

    suspend fun setPauseDuringCalls(enabled: Boolean) {
        dataStore.edit { it[Keys.PAUSE_DURING_CALLS] = enabled }
    }

    suspend fun setBatterySaverEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.BATTERY_SAVER_ENABLED] = enabled }
    }

    suspend fun setBatteryThresholdPercent(percent: Int) {
        dataStore.edit { it[Keys.BATTERY_THRESHOLD_PERCENT] = percent.coerceIn(5, 50) }
    }

    suspend fun setSamplingRate(option: SamplingRateOption) {
        dataStore.edit { it[Keys.SAMPLING_RATE] = option.name }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }
}
