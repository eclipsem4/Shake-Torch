package com.example.hardware

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BatterySnapshot(
    val levelPercent: Int = 100,
    val isCharging: Boolean = false
)

/**
 * Tracks live battery level, charging state, and system battery optimization exemption status.
 */
@Singleton
class BatteryMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val powerManager: PowerManager
) {
    private val _batterySnapshot = MutableStateFlow(readCurrentBattery())
    val batterySnapshot: StateFlow<BatterySnapshot> = _batterySnapshot.asStateFlow()

    private var registered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            _batterySnapshot.value = extractBatterySnapshot(intent)
        }
    }

    fun startMonitoring() {
        _batterySnapshot.value = readCurrentBattery()
        if (registered) return
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
        registered = true
    }

    fun stopMonitoring() {
        if (!registered) return
        runCatching { context.unregisterReceiver(batteryReceiver) }
        registered = false
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun refreshBatterySnapshot(): BatterySnapshot {
        return readCurrentBattery().also { _batterySnapshot.value = it }
    }

    private fun readCurrentBattery(): BatterySnapshot {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return if (intent != null) extractBatterySnapshot(intent) else BatterySnapshot()
    }

    private fun extractBatterySnapshot(intent: Intent): BatterySnapshot {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val percent = if (level >= 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 100
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return BatterySnapshot(levelPercent = percent, isCharging = charging)
    }
}
