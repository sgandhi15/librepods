/*
    LibrePods - AirPods liberated from Apple’s ecosystem
    Copyright (C) 2025 LibrePods contributors

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/

@file:OptIn(ExperimentalEncodingApi::class)

package me.kavishdevar.librepods.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.kavishdevar.librepods.R
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun AudioSettings(
    adaptiveVolumeCapability: Boolean,
    conversationAwarenessCapability: Boolean,
    adaptiveAudioCapability: Boolean,
    customEqCapability: Boolean,

    adaptiveVolumeChecked: Boolean,
    onAdaptiveVolumeCheckedChange: (Boolean) -> Unit,

    conversationAwarenessChecked: Boolean,
    onConversationAwarenessCheckedChange: (Boolean) -> Unit,

    navigateToAdaptiveStrength: () -> Unit,
    navigateToEqualizer: () -> Unit
) {
    if (adaptiveVolumeCapability || conversationAwarenessCapability || adaptiveAudioCapability) {
        StyledList(title = stringResource(R.string.audio)) {
            if (adaptiveVolumeCapability) {
                StyledToggle(
                    label = stringResource(R.string.personalized_volume),
                    description = stringResource(R.string.personalized_volume_description),
                    checked = adaptiveVolumeChecked,
                    onCheckedChange = onAdaptiveVolumeCheckedChange,
                )
            }

            if (conversationAwarenessCapability) {
                StyledToggle(
                    label = stringResource(R.string.conversation_awareness),
                    description = stringResource(R.string.conversation_awareness_description),
                    checked = conversationAwarenessChecked,
                    onCheckedChange = onConversationAwarenessCheckedChange,
                )
            }

            if (adaptiveAudioCapability) {
                StyledListItem(
                    name = stringResource(R.string.adaptive_audio),
                    onClick = navigateToAdaptiveStrength,
                )
            }

            if (customEqCapability) {
                StyledListItem(
                    name = stringResource(R.string.equalizer),
                    onClick = navigateToEqualizer,
                )
            }
        }
    }
}
