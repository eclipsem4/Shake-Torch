package com.example.receiver

import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.data.repository.SettingsRepository
import com.example.service.ShakeService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restarts ShakeService after device reboot or app update when "Start on Device Boot" is enabled.
 */
class BootReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootReceiverEntryPoint {
        fun settingsRepository(): SettingsRepository
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != ACTION_QUICKBOOT_POWERON &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    appContext,
                    BootReceiverEntryPoint::class.java
                )
                val settings = entryPoint.settingsRepository().settingsFlow.first()
                if (settings.startOnBoot) {
                    val serviceIntent = ShakeService.createStartIntent(appContext)
                    try {
                        ContextCompat.startForegroundService(appContext, serviceIntent)
                    } catch (_: ForegroundServiceStartNotAllowedException) {
                        // Gracefully ignore if OS/OEM blocks background FGS start on this device.
                    } catch (_: SecurityException) {
                        // Gracefully ignore missing runtime prerequisites.
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
    }
}
