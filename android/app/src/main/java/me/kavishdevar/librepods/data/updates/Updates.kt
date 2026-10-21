package me.kavishdevar.librepods.data.updates

import androidx.compose.runtime.Composable
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.screens.EqualizerScreenPreviewApple

val update1_0_0 = listOf(
        UpdateItem(
            titleRes = R.string.equalizer,
            descriptionRes = R.string.update_equalizer_description,
            demoComposeable = @Composable {
                EqualizerScreenPreviewApple()
            }
        ),
    )

val updates = update1_0_0
