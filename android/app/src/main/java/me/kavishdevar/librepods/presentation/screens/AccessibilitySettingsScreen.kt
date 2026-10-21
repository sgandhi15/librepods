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

package me.kavishdevar.librepods.presentation.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.presentation.components.StyledButton
import me.kavishdevar.librepods.presentation.components.StyledList
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.components.StyledSlider
import me.kavishdevar.librepods.presentation.components.StyledToggle
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.milliseconds

private var phoneMediaDebounceJob: Job? = null

@SuppressLint("DefaultLocale")
@ExperimentalHazeMaterialsApi
@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class, FlowPreview::class)
@Composable
fun AccessibilitySettingsScreen(viewModel: AirPodsViewModel) {
    val state by viewModel.uiState.collectAsState()

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(modifier = Modifier.height(topPadding))


//            val phoneMediaEQ = remember { mutableStateOf(FloatArray(8) { 0.5f }) }
//            val phoneEQEnabled = remember { mutableStateOf(false) }
//            val mediaEQEnabled = remember { mutableStateOf(false) }

        val pressSpeedOptions = mapOf(
            0.toByte() to stringResource(R.string.default_option),
            1.toByte() to stringResource(R.string.slower),
            2.toByte() to stringResource(R.string.slowest)
        )

        val selectedPressSpeedValue =
            state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.DOUBLE_CLICK_INTERVAL]?.getOrNull(
                0
            )
        var selectedPressSpeed by remember {
            mutableStateOf(
                pressSpeedOptions[selectedPressSpeedValue] ?: pressSpeedOptions[0]
            )
        }

        val pressAndHoldDurationOptions = mapOf(
            0.toByte() to stringResource(R.string.default_option),
            1.toByte() to stringResource(R.string.slower),
            2.toByte() to stringResource(R.string.slowest)
        )

        val selectedPressAndHoldDurationValue =
            state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.CLICK_HOLD_INTERVAL]?.getOrNull(
                0
            )
        var selectedPressAndHoldDuration by remember {
            mutableStateOf(
                pressAndHoldDurationOptions[selectedPressAndHoldDurationValue]
                    ?: pressAndHoldDurationOptions[0]
            )
        }

        val volumeSwipeSpeedOptions = mapOf(
            1.toByte() to stringResource(R.string.default_option),
            2.toByte() to stringResource(R.string.longer),
            3.toByte() to stringResource(R.string.longest)
        )
        val selectedVolumeSwipeSpeedValue =
            state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.VOLUME_SWIPE_INTERVAL]?.getOrNull(
                0
            )
        var selectedVolumeSwipeSpeed by remember {
            mutableStateOf(
                volumeSwipeSpeedOptions[selectedVolumeSwipeSpeedValue]
                    ?: volumeSwipeSpeedOptions[1]
            )
        }

        val phoneMediaEQ = remember { mutableStateOf(FloatArray(8) { 0.5f }) }
        val phoneEQEnabled = remember { mutableStateOf(false) }
        val mediaEQEnabled = remember { mutableStateOf(false) }

        LaunchedEffect(phoneMediaEQ.value, phoneEQEnabled.value, mediaEQEnabled.value) {
            phoneMediaDebounceJob?.cancel()
            phoneMediaDebounceJob = CoroutineScope(Dispatchers.IO).launch {
                delay(150.milliseconds)
                try {
                    val phoneByte = if (phoneEQEnabled.value) 0x01.toByte() else 0x02.toByte()
                    val mediaByte = if (mediaEQEnabled.value) 0x01.toByte() else 0x02.toByte()
                    Log.d(
                        "AccessibilitySettingsScreen",
                        "Sending phone/media EQ (phoneEnabled=${phoneEQEnabled.value}, mediaEnabled=${mediaEQEnabled.value})"
                    )
                    viewModel.sendPhoneMediaEQ(phoneMediaEQ.value, phoneByte, mediaByte)
                } catch (e: Exception) {
                    Log.w(
                        "AccessibilitySettingsScreen",
                        "Error sending phone/media EQ: ${e.message}"
                    )
                }
            }
        }

        StyledList(
            title = stringResource(R.string.press_speed),
            description = stringResource(R.string.press_speed_description)
        ) {
            pressSpeedOptions.forEach { (value, label) ->
                StyledListItem(
                    name = label,
                    selected = selectedPressSpeed == label,
                    onClick = {
                        selectedPressSpeed = label

                        viewModel.setControlCommandByte(
                            identifier = AACPManager.Companion.ControlCommandIdentifiers.DOUBLE_CLICK_INTERVAL,
                            value = value
                        )
                    }
                )
            }
        }

        StyledList(
            title = stringResource(R.string.press_and_hold_duration),
            description = stringResource(R.string.press_and_hold_duration_description)
        ) {
            pressAndHoldDurationOptions.forEach { (value, label) ->
                StyledListItem(
                    name = label,
                    selected = selectedPressAndHoldDuration == label,
                    onClick = {
                        selectedPressAndHoldDuration = label

                        viewModel.setControlCommandByte(
                            identifier = AACPManager.Companion.ControlCommandIdentifiers.CLICK_HOLD_INTERVAL,
                            value = value
                        )
                    }
                )
            }
        }

        StyledToggle(
            title = stringResource(R.string.listening_mode),
            label = stringResource(R.string.noise_cancellation_single_airpod),
            description = stringResource(R.string.noise_cancellation_single_airpod_description),
            checked = state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.ONE_BUD_ANC_MODE]?.getOrNull(
                0
            ) == 0x01.toByte(),
            onCheckedChange = {
                viewModel.setControlCommandBoolean(
                    AACPManager.Companion.ControlCommandIdentifiers.ONE_BUD_ANC_MODE, it
                )
            }
        )



        val toneVolumeValue = remember { mutableFloatStateOf(state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.CHIME_VOLUME]?.getOrNull(0)?.toFloat() ?: 75f) }

        LaunchedEffect(toneVolumeValue) {
            snapshotFlow {
                toneVolumeValue.floatValue
            }
                .debounce(100.milliseconds)
                .collect {
                    viewModel.setControlCommandValue(
                        AACPManager.Companion.ControlCommandIdentifiers.CHIME_VOLUME,
                        byteArrayOf(it.toInt().toByte(), 0x50)
                    )
                }
        }

        StyledSlider(
            label = stringResource(R.string.tone_volume),
            description = stringResource(R.string.tone_volume_description),
            value = toneVolumeValue.floatValue,
            onValueChange = {
                toneVolumeValue.floatValue = it
            },
            valueRange = 0f..100f,
            snapPoints = listOf(75f),
            startIcon = "\uDBC0\uDEA1",
            endIcon = "\uDBC0\uDEA9",
            independent = true
        )

        if (state.capabilities.contains(Capability.SWIPE_FOR_VOLUME)) {
            val volumeSwipeEnabled =
                state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.VOLUME_SWIPE_MODE]?.getOrNull(
                    0
                )?.toInt() == 0x01
            StyledToggle(
                label = stringResource(R.string.volume_control),
                description = stringResource(R.string.volume_control_description),
                checked = volumeSwipeEnabled,
                onCheckedChange = {
                    viewModel.setControlCommandBoolean(
                        AACPManager.Companion.ControlCommandIdentifiers.VOLUME_SWIPE_MODE, it
                    )
                }
            )

            StyledList(
                title = stringResource(R.string.volume_swipe_speed),
                description = stringResource(R.string.volume_swipe_speed_description)
            ) {
                volumeSwipeSpeedOptions.forEach { (value, label) ->
                    StyledListItem(
                        name = label,
                        selected = selectedVolumeSwipeSpeed == label,
                        onClick = {
                            selectedVolumeSwipeSpeed = label

                            viewModel.setControlCommandByte(
                                identifier = AACPManager.Companion.ControlCommandIdentifiers.VOLUME_SWIPE_INTERVAL,
                                value = value
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(bottomPadding))
    }
}
