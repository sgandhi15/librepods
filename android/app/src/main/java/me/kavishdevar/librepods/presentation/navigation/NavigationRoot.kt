package me.kavishdevar.librepods.presentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.kyant.backdrop.backdrops.LayerBackdrop
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.StyledIconButton
import me.kavishdevar.librepods.presentation.components.StyledScaffold
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel

@Composable
fun NavigationRoot(
    showReleaseNotes: Boolean = false,
    updatesShown: () -> Unit = {},
    showOnboarding: Boolean = false,
    onboardingComplete: () -> Unit = {},
    airPodsViewModel: AirPodsViewModel,
    initialScreen: Screen? = null
) {
    val backStack = remember {
        mutableStateListOf(
            when {
                showOnboarding -> Screen.Onboarding
                showReleaseNotes -> Screen.ReleaseNotes
                else -> Screen.AirPodsSettings
            }
        ).also { stack ->
            // Allows debug screenshot tooling to open the real navigation destination.
            if (initialScreen != null && initialScreen != stack.last()) stack.add(initialScreen)
        }
    }

    val currentScreen = backStack.last()

    val state by airPodsViewModel.uiState.collectAsState()

    val title = when (currentScreen) {
        Screen.Onboarding -> ""
        Screen.AirPodsSettings -> if (state.isLocallyConnected) state.deviceName else stringResource(R.string.app_name)
        Screen.Accessibility -> stringResource(R.string.accessibility)
        Screen.AdaptiveStrength -> stringResource(R.string.customize_adaptive_audio)
        Screen.AppSettings -> stringResource(R.string.settings)
        Screen.Equalizer -> stringResource(R.string.equalizer)
        Screen.HeadTracking -> stringResource(R.string.head_tracking)
        Screen.HearingProtection -> stringResource(R.string.hearing_protection)
        is Screen.LongPress -> currentScreen.bud
        Screen.Rename -> stringResource(R.string.name)
        Screen.VersionInfo -> stringResource(R.string.version)
        is Screen.CallControl -> currentScreen.action
        Screen.MicrophoneSettings -> stringResource(R.string.microphone_mode)
        Screen.ReleaseNotes -> ""
    }

    // is this a bad idea? probably. I can't think of a better way without having to pass around a shouldShowBackButton to each screen to pass to each scaffold
    val actionButtons = when (currentScreen) {
        Screen.AirPodsSettings -> listOf<@Composable (backdrop: LayerBackdrop) -> Unit>(
                { scaffoldBackdrop ->
                    StyledIconButton(
                        onClick = { backStack.add(Screen.AppSettings) },
                        icon = "􀍟",
                        backdrop = scaffoldBackdrop
                    )
                }
            )
        Screen.HeadTracking -> listOf<@Composable (backdrop: LayerBackdrop) -> Unit>(
            { scaffoldBackdrop ->
                StyledIconButton(
                    onClick = {
                        if (!state.headTrackingActive) {
                            airPodsViewModel.startHeadTracking()
                            Log.d("HeadTrackingScreen", "Head tracking started")
                        } else {
                            airPodsViewModel.stopHeadTracking()
                            Log.d("HeadTrackingScreen", "Head tracking stopped")
                        }
                    },
                    icon = if (state.headTrackingActive) "􀊅" else "􀊃",
                    backdrop = scaffoldBackdrop
                )
            }
        )
        else -> listOf()
    }

    StyledScaffold(
        visible = currentScreen.showTopBar,
        title = title,
        showBackButton = backStack.size > 1,
        onNavigateBack = { backStack.removeAt(backStack.lastIndex) },
        actionButtons = actionButtons
    ) {
        AppNavGraph(
            showReleaseNotes = showReleaseNotes,
            updatesShown = updatesShown,
            showOnboarding = showOnboarding,
            onboardingComplete = onboardingComplete,
            backStack = backStack,
            airPodsViewModel = airPodsViewModel,
        )
    }
}
