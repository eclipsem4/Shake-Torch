package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LocalGlassPalette
import com.example.ui.theme.TorchAmber
import com.example.ui.utils.SystemIntents
import com.example.ui.viewmodel.MainUiState
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    uiState: MainUiState,
    onRequestCorePermissions: () -> Unit,
    onFinishOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalGlassPalette.current
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
        ) {
            // Top bar with Skip button
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("onboarding_skip_button")
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onFinishOnboarding)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.textSecondary
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OnboardingPageCard(
                        icon = Icons.Rounded.FlashlightOn,
                        accent = TorchAmber,
                        title = stringResource(R.string.onboarding_page1_title),
                        body = stringResource(R.string.onboarding_page1_body),
                        tilt = uiState.tilt,
                        actionLabel = null,
                        onActionClick = null
                    )

                    1 -> OnboardingPageCard(
                        icon = Icons.Rounded.Security,
                        accent = palette.accentCool,
                        title = stringResource(R.string.onboarding_page2_title),
                        body = stringResource(R.string.onboarding_page2_body),
                        tilt = uiState.tilt,
                        actionLabel = stringResource(R.string.onboarding_grant_core),
                        onActionClick = onRequestCorePermissions
                    )

                    else -> OnboardingPageCard(
                        icon = Icons.Rounded.BatteryChargingFull,
                        accent = palette.accentSuccess,
                        title = stringResource(R.string.onboarding_page3_title),
                        body = stringResource(R.string.onboarding_page3_body),
                        tilt = uiState.tilt,
                        actionLabel = if (uiState.permissions.ignoringBatteryOptimizations) {
                            stringResource(R.string.status_exempt)
                        } else {
                            stringResource(R.string.onboarding_exempt_battery)
                        },
                        onActionClick = {
                            if (!uiState.permissions.ignoringBatteryOptimizations) {
                                SystemIntents.openBatteryOptimizationExemption(context)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Page indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val active = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (active) 28.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) palette.accentCool else palette.textPrimary.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Next / Finish Glass Button
            val isLastPage = pagerState.currentPage == 2
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("onboarding_primary_button")
                    .clip(RoundedCornerShape(24.dp))
                    .background(palette.accentCool.copy(alpha = 0.28f))
                    .border(
                        width = 1.2.dp,
                        color = palette.accentCool.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable {
                        if (isLastPage) {
                            onFinishOnboarding()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(if (isLastPage) R.string.onboarding_finish else R.string.onboarding_next),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun OnboardingPageCard(
    icon: ImageVector,
    accent: androidx.compose.ui.graphics.Color,
    title: String,
    body: String,
    tilt: com.example.domain.model.TiltOffset,
    actionLabel: String?,
    onActionClick: (() -> Unit)?
) {
    val palette = LocalGlassPalette.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 12.dp)
    ) {
        LiquidGlassCard(
            tilt = tilt,
            accentGlow = accent,
            cornerRadius = 32.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 34.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.20f))
                        .border(
                            width = 1.2.dp,
                            color = accent.copy(alpha = 0.55f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )

                if (actionLabel != null && onActionClick != null) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(18.dp))
                            .background(accent.copy(alpha = 0.22f))
                            .border(
                                width = 1.dp,
                                color = accent.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable(onClick = onActionClick)
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
