package com.example.di

import android.content.Context
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.os.PowerManager
import android.os.VibratorManager
import android.telephony.TelephonyManager
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.shakeLightDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "shakelight_preferences"
)

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.shakeLightDataStore

    @Provides
    @Singleton
    fun provideSensorManager(
        @ApplicationContext context: Context
    ): SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    @Provides
    @Singleton
    fun provideCameraManager(
        @ApplicationContext context: Context
    ): CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    @Provides
    @Singleton
    fun provideVibratorManager(
        @ApplicationContext context: Context
    ): VibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager

    @Provides
    @Singleton
    fun provideTelephonyManager(
        @ApplicationContext context: Context
    ): TelephonyManager? = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    @Provides
    @Singleton
    fun providePowerManager(
        @ApplicationContext context: Context
    ): PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
}
