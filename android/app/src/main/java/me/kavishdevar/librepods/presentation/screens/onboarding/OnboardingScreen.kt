package me.kavishdevar.librepods.presentation.screens.onboarding

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.StyledIconButton
import me.kavishdevar.librepods.presentation.components.StyledIntroButton
import me.kavishdevar.librepods.presentation.components.StyledIntroPage
import me.kavishdevar.librepods.presentation.components.StyledIntroSubtitle
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.utils.bypassDeviceCheck
import me.kavishdevar.librepods.utils.isSupported
import me.kavishdevar.librepods.utils.removeDeviceCheckBypass

private const val PAGE_WELCOME = 0
private const val PAGE_PRIVACY = 1
private const val PAGE_NOT_SUPPORTED = 2
private const val PAGE_PERMISSIONS = 3

@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
) {
    val sharedPreferences = LocalContext.current.getSharedPreferences("settings", Context.MODE_PRIVATE)
    // Read once: bypassing the check flips the stored flag, but going back
    // should still return to the unsupported page on devices that needed it.
    val isSupported = remember { isSupported(sharedPreferences) }

    var page by rememberSaveable { mutableIntStateOf(PAGE_WELCOME) }
    var forward by remember { mutableStateOf(true) }

    fun goTo(target: Int) {
        forward = target > page
        page = target
    }

    fun goBack() {
        when (page) {
            PAGE_PERMISSIONS -> {
                if (!isSupported) removeDeviceCheckBypass(sharedPreferences)
                goTo(if (isSupported) PAGE_PRIVACY else PAGE_NOT_SUPPORTED)
            }
            PAGE_NOT_SUPPORTED -> goTo(PAGE_PRIVACY)
            PAGE_PRIVACY -> goTo(PAGE_WELCOME)
        }
    }

    BackHandler(enabled = page != PAGE_WELCOME) { goBack() }

    val backdrop = rememberLayerBackdrop()
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                // Same push/pop motion as the rest of the app's navigation.
                if (forward) {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 4 }
                } else {
                    (slideInHorizontally { -it / 4 } togetherWith slideOutHorizontally { it })
                        .apply { targetContentZIndex = -1f }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdrop),
            label = "onboardingPage"
        ) { current ->
            when (current) {
                PAGE_WELCOME -> WelcomePage(onContinue = { goTo(PAGE_PRIVACY) })
                PAGE_PRIVACY -> PrivacyPolicyPage(
                    onForward = { goTo(if (isSupported) PAGE_PERMISSIONS else PAGE_NOT_SUPPORTED) }
                )
                PAGE_NOT_SUPPORTED -> NotSupportedPage(
                    bypassCompatibilityCheck = {
                        bypassDeviceCheck(sharedPreferences)
                        goTo(PAGE_PERMISSIONS)
                    }
                )
                PAGE_PERMISSIONS -> PermissionsPage(onForward = onOnboardingComplete)
            }
        }

        AnimatedVisibility(
            visible = page != PAGE_WELCOME,
            enter = fadeIn() + scaleIn(initialScale = 0f, animationSpec = tween()),
            exit = fadeOut() + scaleOut(targetScale = 0.5f, animationSpec = tween(100)),
            modifier = Modifier
                .zIndex(1f)
                .padding(top = topPadding, start = 8.dp)
                .align(Alignment.TopStart)
        ) {
            StyledIconButton(
                onClick = ::goBack,
                icon = "􀯶",
                backdrop = backdrop
            )
        }
    }
}

@Composable
private fun WelcomePage(onContinue: () -> Unit) {
    val appName = stringResource(R.string.app_name)
    StyledIntroPage(
        title = stringResource(R.string.welcome_to_app, appName),
        scrollable = false,
        header = { AppIcon() },
        subtitle = { StyledIntroSubtitle(stringResource(R.string.app_description)) },
        buttons = {
            StyledIntroButton(
                text = stringResource(R.string.continue_label),
                onClick = onContinue
            )
        }
    )
}

@Composable
private fun AppIcon() {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .size(108.dp)
            .shadow(elevation = 12.dp, shape = shape)
            .clip(shape)
            .background(colorResource(R.color.colorBackground)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_monochrome),
            contentDescription = null,
            // Adaptive icons are drawn on a larger canvas that launchers crop to the centre two thirds.
            modifier = Modifier.requiredSize(162.dp)
        )
    }
}

@Preview
@Composable
fun OnboardingScreenPreview() {
    LibrePodsTheme {
        OnboardingScreen {}
    }
}
