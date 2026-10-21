package me.kavishdevar.librepods.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen: NavKey {
    val showTopBar: Boolean
        get() = true

    @Serializable
    data object Onboarding: Screen {
        override val showTopBar: Boolean = false
    }

    @Serializable
    data object AirPodsSettings: Screen

    @Serializable
    data object Rename: Screen

    @Serializable
    data object AppSettings: Screen

    @Serializable
    data object HeadTracking: Screen

    @Serializable
    data object Accessibility: Screen

    @Serializable
    data object AdaptiveStrength: Screen

    @Serializable
    data object VersionInfo: Screen

    @Serializable
    data object HearingProtection: Screen

    @Serializable
    data object Equalizer: Screen

    @Serializable
    data class LongPress(
        val bud: String
    ): Screen

    @Serializable
    data class CallControl(
        val action: String
    ): Screen

    @Serializable
    data object MicrophoneSettings: Screen

    @Serializable
    data object ReleaseNotes: Screen {
        override val showTopBar: Boolean = false
    }
}
