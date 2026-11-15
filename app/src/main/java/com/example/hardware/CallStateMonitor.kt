package com.example.hardware

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors active or ringing phone calls via TelephonyCallback (API 31+) when READ_PHONE_STATE is granted.
 */
@Singleton
class CallStateMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val telephonyManager: TelephonyManager?
) {
    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

    private var registered = false
    private val executor: Executor = ContextCompat.getMainExecutor(context)

    private val callStateCallback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            _isCallActive.value = state == TelephonyManager.CALL_STATE_RINGING ||
                state == TelephonyManager.CALL_STATE_OFFHOOK
        }
    }

    fun hasPhoneStatePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startMonitoring() {
        if (registered || telephonyManager == null || !hasPhoneStatePermission()) {
            return
        }
        try {
            telephonyManager.registerTelephonyCallback(executor, callStateCallback)
            registered = true
            @Suppress("DEPRECATION")
            val currentState = telephonyManager.callState
            _isCallActive.value = currentState == TelephonyManager.CALL_STATE_RINGING ||
                currentState == TelephonyManager.CALL_STATE_OFFHOOK
        } catch (_: SecurityException) {
            registered = false
            _isCallActive.value = false
        }
    }

    fun stopMonitoring() {
        if (registered) {
            runCatching {
                telephonyManager?.unregisterTelephonyCallback(callStateCallback)
            }
        }
        registered = false
        _isCallActive.value = false
    }
}
