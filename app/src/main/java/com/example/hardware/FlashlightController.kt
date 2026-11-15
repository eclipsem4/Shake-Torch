package com.example.hardware

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import com.example.data.repository.ServiceStateBus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls the device's rear LED torch via CameraManager and stays synchronized
 * with external torch state changes using TorchCallback.
 */
@Singleton
class FlashlightController @Inject constructor(
    private val cameraManager: CameraManager,
    private val stateBus: ServiceStateBus
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var torchCameraId: String? = null

    @Volatile
    private var currentTorchOn: Boolean = false

    private var callbackRegistered = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchCameraId) {
                currentTorchOn = enabled
                stateBus.setTorchState(enabled)
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == torchCameraId) {
                currentTorchOn = false
                stateBus.setTorchState(false)
            }
        }
    }

    init {
        refreshHardwareAndRegisterCallback()
    }

    fun refreshHardwareAndRegisterCallback(): Boolean {
        val foundId = findBackFlashCameraId()
        torchCameraId = foundId
        val hasFlash = foundId != null
        stateBus.setHasFlashUnit(hasFlash)

        if (hasFlash && !callbackRegistered) {
            runCatching {
                cameraManager.registerTorchCallback(torchCallback, mainHandler)
                callbackRegistered = true
            }
        }
        return hasFlash
    }

    fun hasFlashUnit(): Boolean {
        if (torchCameraId == null) {
            torchCameraId = findBackFlashCameraId()
            stateBus.setHasFlashUnit(torchCameraId != null)
        }
        return torchCameraId != null
    }

    fun isTorchOn(): Boolean = currentTorchOn

    /**
     * Toggles the flashlight and returns the resulting torch state (true = ON, false = OFF),
     * or null if no flash hardware is available or camera access failed.
     */
    fun toggleTorch(): Boolean? {
        val targetState = !currentTorchOn
        return if (setTorch(targetState)) targetState else null
    }

    fun setTorch(enabled: Boolean): Boolean {
        val id = torchCameraId ?: findBackFlashCameraId()?.also {
            torchCameraId = it
            stateBus.setHasFlashUnit(true)
        }

        if (id == null) {
            stateBus.setHasFlashUnit(false)
            return false
        }

        return try {
            cameraManager.setTorchMode(id, enabled)
            currentTorchOn = enabled
            stateBus.setTorchState(enabled)
            true
        } catch (_: CameraAccessException) {
            false
        } catch (_: IllegalArgumentException) {
            stateBus.setHasFlashUnit(false)
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun findBackFlashCameraId(): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) {
            null
        }
    }
}
