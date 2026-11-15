package com.example.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.ShakeLightApp
import com.example.data.repository.ServiceStateBus
import com.example.data.repository.SettingsRepository
import com.example.domain.model.PauseReason
import com.example.domain.model.SamplingRateOption
import com.example.domain.model.ShakeSettings
import com.example.hardware.BatteryMonitor
import com.example.hardware.BatterySnapshot
import com.example.hardware.CallStateMonitor
import com.example.hardware.FlashlightController
import com.example.hardware.ShakeDetector
import com.example.hardware.VibrationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ShakeService : Service() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var stateBus: ServiceStateBus
    @Inject lateinit var shakeDetector: ShakeDetector
    @Inject lateinit var flashlightController: FlashlightController
    @Inject lateinit var vibrationHelper: VibrationHelper
    @Inject lateinit var callStateMonitor: CallStateMonitor
    @Inject lateinit var batteryMonitor: BatteryMonitor
    @Inject lateinit var powerManager: PowerManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observerJob: Job? = null
    private var latestSettings: ShakeSettings = ShakeSettings()
    private var currentPauseReason: PauseReason = PauseReason.NONE

    private val shakeListener = object : ShakeDetector.Listener {
        override fun onMagnitudeChanged(magnitudeG: Float) {
            stateBus.updateShakeMagnitude(magnitudeG)
        }

        override fun onShakeDetected() {
            if (currentPauseReason == PauseReason.IN_CALL || currentPauseReason == PauseReason.LOW_BATTERY) {
                return
            }
            performTorchToggleWithWakeLock()
        }
    }

    override fun onCreate() {
        super.onCreate()
        batteryMonitor.startMonitoring()
        flashlightController.refreshHardwareAndRegisterCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopServiceCleanly()
            return START_NOT_STICKY
        }

        val initialNotification = buildNotification(
            isTorchOn = flashlightController.isTorchOn(),
            pauseReason = PauseReason.NONE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                initialNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }
        stateBus.setServiceRunning(true)

        startObservingState()
        return START_STICKY
    }

    override fun onDestroy() {
        observerJob?.cancel()
        shakeDetector.stop()
        callStateMonitor.stopMonitoring()
        batteryMonitor.stopMonitoring()
        stateBus.setServiceRunning(false)
        stateBus.setPauseReason(PauseReason.NONE)
        stateBus.resetPeakMagnitude()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startObservingState() {
        if (observerJob != null) return

        val torchStateFlow = stateBus.state.map { it.isTorchOn }.distinctUntilChanged()

        observerJob = serviceScope.launch {
            combine(
                settingsRepository.settingsFlow,
                callStateMonitor.isCallActive,
                batteryMonitor.batterySnapshot,
                torchStateFlow
            ) { settings, isInCall, battery, isTorchOn ->
                ServiceEvalSnapshot(settings, isInCall, battery, isTorchOn)
            }.collect { eval ->
                applyConfiguration(eval)
            }
        }
    }

    private fun applyConfiguration(eval: ServiceEvalSnapshot) {
        val settings = eval.settings
        latestSettings = settings

        if (settings.pauseDuringCalls) {
            callStateMonitor.startMonitoring()
        } else {
            callStateMonitor.stopMonitoring()
        }

        stateBus.setBatteryStatus(eval.battery.levelPercent, eval.battery.isCharging)

        val hasFlash = flashlightController.hasFlashUnit()
        val pauseReason = when {
            !hasFlash -> PauseReason.NO_FLASH_HARDWARE
            settings.pauseDuringCalls && eval.isInCall -> PauseReason.IN_CALL
            settings.batterySaverEnabled &&
                !eval.battery.isCharging &&
                eval.battery.levelPercent < settings.batteryThresholdPercent -> PauseReason.LOW_BATTERY
            else -> PauseReason.NONE
        }
        currentPauseReason = pauseReason
        stateBus.setPauseReason(pauseReason)

        val effectiveSamplingRate = if (settings.batterySaverEnabled) {
            SamplingRateOption.NORMAL
        } else {
            settings.samplingRate
        }

        shakeDetector.updateConfig(
            sensitivity = settings.sensitivity,
            gestureMode = settings.gestureMode,
            cooldownMillis = settings.cooldownMillis,
            samplingRate = effectiveSamplingRate
        )

        if (pauseReason == PauseReason.IN_CALL || pauseReason == PauseReason.LOW_BATTERY) {
            shakeDetector.stop()
        } else {
            shakeDetector.start(shakeListener)
        }

        updateNotification(
            isTorchOn = eval.isTorchOn,
            pauseReason = pauseReason
        )
    }

    private fun performTorchToggleWithWakeLock() {
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKE_LOCK_TAG
        ).apply {
            setReferenceCounted(false)
        }

        try {
            wakeLock.acquire(1500L)
            val newState = flashlightController.toggleTorch()
            if (newState != null) {
                stateBus.markTriggered(SystemClock.elapsedRealtime())
                if (latestSettings.vibrateOnToggle) {
                    vibrationHelper.vibrateForTorchState(
                        isNowOn = newState,
                        intensity = latestSettings.hapticIntensity
                    )
                }
            }
        } finally {
            if (wakeLock.isHeld) {
                runCatching { wakeLock.release() }
            }
        }
    }

    private fun updateNotification(isTorchOn: Boolean, pauseReason: PauseReason) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(isTorchOn, pauseReason))
    }

    private fun buildNotification(isTorchOn: Boolean, pauseReason: PauseReason): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ShakeService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val (title, text) = when {
            pauseReason == PauseReason.IN_CALL -> {
                getString(R.string.notification_title_paused_call) to
                    getString(R.string.notification_text_paused_call)
            }
            pauseReason == PauseReason.LOW_BATTERY -> {
                getString(R.string.notification_title_paused_battery) to
                    getString(R.string.notification_text_paused_battery)
            }
            isTorchOn -> {
                getString(R.string.notification_title_torch_on) to
                    getString(R.string.notification_text_torch_on)
            }
            else -> {
                getString(R.string.notification_title_listening) to
                    getString(R.string.notification_text_listening)
            }
        }

        return NotificationCompat.Builder(this, ShakeLightApp.CHANNEL_SHAKE_SERVICE)
            .setSmallIcon(R.drawable.ic_flashlight_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                0,
                getString(R.string.notification_action_stop),
                stopPendingIntent
            )
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun stopServiceCleanly() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private data class ServiceEvalSnapshot(
        val settings: ShakeSettings,
        val isInCall: Boolean,
        val battery: BatterySnapshot,
        val isTorchOn: Boolean
    )

    companion object {
        const val ACTION_START = "com.example.service.action.START"
        const val ACTION_STOP = "com.example.service.action.STOP"
        private const val NOTIFICATION_ID = 1042
        private const val WAKE_LOCK_TAG = "ShakeLight:TorchToggleWakeLock"

        fun createStartIntent(context: Context): Intent =
            Intent(context, ShakeService::class.java).apply { action = ACTION_START }

        fun createStopIntent(context: Context): Intent =
            Intent(context, ShakeService::class.java).apply { action = ACTION_STOP }
    }
}
