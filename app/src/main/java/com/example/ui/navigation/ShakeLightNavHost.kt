package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.AppThemeMode
import com.example.domain.model.GestureMode
import com.example.domain.model.SamplingRateOption
import com.example.ui.components.AuroraGlassBackground
import com.example.ui.components.BottomNavTab
import com.example.ui.components.FloatingGlassNavBar
import com.example.ui.screens.BatteryPermissionsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.MainUiState
import kotlinx.serialization.Serializable

@Serializable
object OnboardingRoute

@Serializable
object HomeRoute

@Serializable
object SettingsRoute

@Serializable
object BatteryPermissionsRoute

@Composable
fun ShakeLightRoot(
    uiState: MainUiState,
    onToggleService: () -> Unit,
    onToggleManualTorch: () -> Unit,
    onResetPeak: () -> Unit,
    onSetMeterPreviewActive: (Boolean) -> Unit,
    onSensitivityChange: (Int) -> Unit,
    onGestureModeChange: (GestureMode) -> Unit,
    onCooldownChange: (Long) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onPauseDuringCallsChange: (Boolean) -> Unit,
    onStartOnBootChange: (Boolean) -> Unit,
    onBatterySaverChange: (Boolean) -> Unit,
    onBatteryThresholdChange: (Int) -> Unit,
    onSamplingRateChange: (SamplingRateOption) -> Unit,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onCompleteOnboarding: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onRequestCorePermissions: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestPhonePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    AuroraGlassBackground(
        isTorchOn = uiState.serviceState.isTorchOn,
        tilt = uiState.tilt,
        modifier = modifier
    ) {
        if (!uiState.isLoaded) {
            return@AuroraGlassBackground
        }

        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        LaunchedEffect(uiState.settings.onboardingCompleted) {
            val isOnOnboarding = currentDestination?.hasRoute<OnboardingRoute>() == true
            if (!uiState.settings.onboardingCompleted && !isOnOnboarding) {
                navController.navigate(OnboardingRoute) {
                    popUpTo(0) { inclusive = true }
                }
            } else if (uiState.settings.onboardingCompleted && isOnOnboarding) {
                navController.navigate(HomeRoute) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }

        val showBottomBar = currentDestination?.hasRoute<OnboardingRoute>() != true
        val selectedTab = when {
            currentDestination?.hasRoute<SettingsRoute>() == true -> BottomNavTab.SETTINGS
            currentDestination?.hasRoute<BatteryPermissionsRoute>() == true -> BottomNavTab.SYSTEM
            else -> BottomNavTab.HOME
        }

        val navBarInsets = WindowInsets.navigationBars.asPaddingValues()
        val bottomContentPadding = PaddingValues(
            bottom = navBarInsets.calculateBottomPadding() + 92.dp
        )

        val startDestination: Any = if (uiState.settings.onboardingCompleted) {
            HomeRoute
        } else {
            OnboardingRoute
        }

        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = { fadeIn(animationSpec = tween(220)) },
                exitTransition = { fadeOut(animationSpec = tween(180)) },
                modifier = Modifier.fillMaxSize()
            ) {
                composable<OnboardingRoute> {
                    OnboardingScreen(
                        uiState = uiState,
                        onRequestCorePermissions = onRequestCorePermissions,
                        onFinishOnboarding = {
                            onCompleteOnboarding()
                            navController.navigate(HomeRoute) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable<HomeRoute> {
                    HomeScreen(
                        uiState = uiState,
                        onToggleService = onToggleService,
                        onToggleManualTorch = onToggleManualTorch,
                        onResetPeak = onResetPeak,
                        onSensitivityChange = onSensitivityChange,
                        onSetMeterPreviewActive = onSetMeterPreviewActive,
                        bottomContentPadding = bottomContentPadding
                    )
                }

                composable<SettingsRoute> {
                    SettingsScreen(
                        uiState = uiState,
                        onSensitivityChange = onSensitivityChange,
                        onGestureModeChange = onGestureModeChange,
                        onCooldownChange = onCooldownChange,
                        onVibrateChange = onVibrateChange,
                        onPauseDuringCallsChange = onPauseDuringCallsChange,
                        onRequestPhonePermission = onRequestPhonePermission,
                        onStartOnBootChange = onStartOnBootChange,
                        onBatterySaverChange = onBatterySaverChange,
                        onBatteryThresholdChange = onBatteryThresholdChange,
                        onSamplingRateChange = onSamplingRateChange,
                        onThemeModeChange = onThemeModeChange,
                        onReplayOnboarding = onReplayOnboarding,
                        bottomContentPadding = bottomContentPadding
                    )
                }

                composable<BatteryPermissionsRoute> {
                    BatteryPermissionsScreen(
                        uiState = uiState,
                        onRequestCameraPermission = onRequestCameraPermission,
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onRequestPhonePermission = onRequestPhonePermission,
                        bottomContentPadding = bottomContentPadding
                    )
                }
            }

            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                FloatingGlassNavBar(
                    selectedTab = selectedTab,
                    onSelectTab = { tab ->
                        val targetRoute: Any = when (tab) {
                            BottomNavTab.HOME -> HomeRoute
                            BottomNavTab.SETTINGS -> SettingsRoute
                            BottomNavTab.SYSTEM -> BatteryPermissionsRoute
                        }
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
