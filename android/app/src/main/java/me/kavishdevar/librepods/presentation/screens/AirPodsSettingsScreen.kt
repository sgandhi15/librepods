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

package me.kavishdevar.librepods.presentation.screens

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import kotlinx.coroutines.delay
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.bluetooth.AACPManager
import me.kavishdevar.librepods.data.AirPodsPro3
import me.kavishdevar.librepods.data.Capability
import me.kavishdevar.librepods.presentation.components.AudioSettings
import me.kavishdevar.librepods.presentation.components.BatteryView
import me.kavishdevar.librepods.presentation.components.CallControlSettings
import me.kavishdevar.librepods.presentation.components.ConnectionSettings
import me.kavishdevar.librepods.presentation.components.HearingHealthSettings
import me.kavishdevar.librepods.presentation.components.NoiseControlSettings
import me.kavishdevar.librepods.presentation.components.PressAndHoldSettings
import me.kavishdevar.librepods.presentation.components.StyledButton
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.components.StyledToggle
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsUiState
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.presentation.viewmodel.demoState
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration.Companion.seconds

@Composable
fun AirPodsSettingsRoute(
    viewModel: AirPodsViewModel,
    navigateToRename: () -> Unit,
    navigateToHearingProtection: () -> Unit,
    navigateToLeftLongPress: () -> Unit,
    navigateToRightLongPress: () -> Unit,
    navigateToAdaptiveStrength: () -> Unit,
    navigateToEqualizer: () -> Unit,
    navigateToHeadTracking: () -> Unit,
    navigateToAccessibility: () -> Unit,
    navigateToCallControlScreen: (action: String) -> Unit,
    navigateToMicrophoneSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 84.dp
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp

    Box (
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        AirPodsSettingsScreen(
            state = state,

            topPadding = topPadding,
            bottomPadding = bottomPadding,

            setControlCommandInt = viewModel::setControlCommandInt,
            setControlCommandBoolean = viewModel::setControlCommandBoolean,
//        setControlCommandValue = viewModel::setControlCommandValue,
            setControlCommandByte = viewModel::setControlCommandByte,


            onAutomaticEarDetectionChanged = viewModel::setAutomaticEarDetectionEnabled,
            onAutomaticConnectionChanged = viewModel::setAutomaticConnectionEnabled,
            setDynamicEndOfCharge = viewModel::setDynamicEndOfCharge,
            setOffListeningMode = viewModel::setOffListeningMode,
            disconnect = viewModel::disconnect,

            navigateToRename = navigateToRename,
            navigateToHearingProtection = navigateToHearingProtection,
            navigateToLeftLongPress = navigateToLeftLongPress,
            navigateToRightLongPress = navigateToRightLongPress,
            navigateToAdaptiveStrength = navigateToAdaptiveStrength,
            navigateToEqualizer = navigateToEqualizer,
            navigateToHeadTracking = navigateToHeadTracking,
            navigateToAccessibility = navigateToAccessibility,
            navigateToCallControlScreen = navigateToCallControlScreen,
            navigateToMicrophoneSettings = navigateToMicrophoneSettings,

            activateDemoMode = viewModel::activateDemoMode,
            reconnectFromSavedMac = viewModel::reconnectFromSavedMac
        )
    }
}

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@SuppressLint("MissingPermission", "UnspecifiedRegisterReceiverFlag")
@Composable
fun AirPodsSettingsScreen(
        state: AirPodsUiState,

        topPadding: Dp = 16.dp,
        bottomPadding: Dp = 16.dp,

        setControlCommandInt: (AACPManager.Companion.ControlCommandIdentifiers, Int) -> Unit,
        setControlCommandBoolean: (AACPManager.Companion.ControlCommandIdentifiers, Boolean) -> Unit,
//        setControlCommandValue: (AACPManager.Companion.ControlCommandIdentifiers, ByteArray) -> Unit,
        setControlCommandByte: (AACPManager.Companion.ControlCommandIdentifiers, Byte) -> Unit,

        onAutomaticEarDetectionChanged: (Boolean) -> Unit,
        onAutomaticConnectionChanged: (Boolean) -> Unit,
        setDynamicEndOfCharge: (Boolean) -> Unit,
        setOffListeningMode: (Boolean) -> Unit,
        disconnect: () -> Unit,

        navigateToRename: () -> Unit,
        navigateToHearingProtection: () -> Unit,
        navigateToLeftLongPress: () -> Unit,
        navigateToRightLongPress: () -> Unit,
        navigateToAdaptiveStrength: () -> Unit,
        navigateToEqualizer: () -> Unit,
        navigateToHeadTracking: () -> Unit,
        navigateToAccessibility: () -> Unit,
        navigateToCallControlScreen: (action: String) -> Unit,
        navigateToMicrophoneSettings: () -> Unit,

        activateDemoMode: () -> Unit,
        reconnectFromSavedMac: () -> Unit,
) {
    val sharedPreferences = LocalContext.current.getSharedPreferences("settings", MODE_PRIVATE)
    var deviceName by remember {
        mutableStateOf(
            TextFieldValue(
                sharedPreferences.getString("name", state.deviceName).toString()
            )
        )
    }

    val nameChangeListener = remember {
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "name") {
                deviceName =
                    TextFieldValue(sharedPreferences.getString("name", "AirPods Pro").toString())
            }
        }
    }

    DisposableEffect(Unit) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(nameChangeListener)
        onDispose {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(nameChangeListener)
        }
    }

    if (state.isLocallyConnected) {
        val capabilities = state.capabilities

        LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp)
        ) {
            item(key = "top_padding") { Spacer(modifier = Modifier.height(topPadding)) }

            item(key = "battery") {
                BatteryView(
                    batteryList = state.battery,
                    budsRes = state.instance?.model?.budsRes ?: R.drawable.airpods_pro_2_buds,
                    caseRes = state.instance?.model?.caseRes ?: R.drawable.airpods_pro_2_case
                )
            }
            item(key = "spacer_battery") {
                Spacer(modifier = Modifier.height(32.dp))
            }

            item(key = "name") {
                StyledListItem(
                    name = stringResource(R.string.name),
                    description = deviceName.text,
                    onClick = navigateToRename,
                )
            }

            val hasPPECapability =
                state.instance?.model?.capabilities?.contains(Capability.PPE) == true

            if (hasPPECapability) {
                item(key = "spacer_hearing_health") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item(key = "hearing_health") {
                    HearingHealthSettings(navigateToHearingProtection = navigateToHearingProtection)
                }
            }

            if (capabilities.contains(Capability.LISTENING_MODE)) {
                item(key = "spacer_noise") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item(key = "listening_mode") {
                    NoiseControlSettings(
                        showOffListeningMode = state.offListeningMode,
                        noiseControlModeValue = state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE]?.getOrNull(
                            0
                        )?.toInt() ?: 3,
                        onNoiseControlModeChanged = {
                            setControlCommandInt(
                                AACPManager.Companion.ControlCommandIdentifiers.LISTENING_MODE, it
                            )
                        },
                    )
                }
            }

            if (capabilities.contains(Capability.STEM_CONFIG)) {
                item(key = "spacer_press_hold") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
                item(key = "press_hold") {
                    PressAndHoldSettings(
                        leftAction = state.leftAction,
                        rightAction = state.rightAction,
                        navigateToLeftLongPress = navigateToLeftLongPress,
                        navigateToRightLongPress = navigateToRightLongPress
                    )
                }
            }

            item(key = "spacer_call") {
                Spacer(modifier = Modifier.height(16.dp))
            }
            item(key = "call_control") {
                val bytes =
                    state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.CALL_MANAGEMENT_CONFIG]?.take(
                        2
                    )?.toByteArray() ?: byteArrayOf(0x00, 0x00)
                val flipped = try {
                    bytes[1] == 0x02.toByte()
                } catch (_: Exception) {
                    false
                }
                CallControlSettings(
                    flipped = flipped,
                    navigateToCallControlScreen = navigateToCallControlScreen
                )
            }


            item(key = "spacer_audio") { Spacer(modifier = Modifier.height(16.dp)) }
            item(key = "audio") {
                val model = state.instance?.model ?: AirPodsPro3()
                val adaptiveVolumeCapability =
                    model.capabilities.contains(Capability.ADAPTIVE_VOLUME)
                val conversationAwarenessCapability =
                    model.capabilities.contains(Capability.CONVERSATION_AWARENESS)
                val adaptiveAudioCapability =
                    model.capabilities.contains(Capability.ADAPTIVE_VOLUME)

                val adaptiveVolumeChecked =
                    state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.ADAPTIVE_VOLUME_CONFIG]?.getOrNull(
                        0
                    ) == 0x01.toByte()
                val conversationAwarenessChecked =
                    state.controlStates[AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG]?.getOrNull(
                        0
                    ) == 0x01.toByte()

                AudioSettings(
                    adaptiveVolumeCapability = adaptiveVolumeCapability,
                    conversationAwarenessCapability = conversationAwarenessCapability,
                    adaptiveAudioCapability = adaptiveAudioCapability,
                    customEqCapability = true,
                    adaptiveVolumeChecked = adaptiveVolumeChecked,
                    onAdaptiveVolumeCheckedChange = { checked ->
                        setControlCommandBoolean(
                            AACPManager.Companion.ControlCommandIdentifiers.ADAPTIVE_VOLUME_CONFIG,
                            checked
                        )
                    },
                    conversationAwarenessChecked = conversationAwarenessChecked,
                    onConversationAwarenessCheckedChange = { checked ->
                        setControlCommandBoolean(
                            AACPManager.Companion.ControlCommandIdentifiers.CONVERSATION_DETECT_CONFIG,
                            checked
                        )
                    },
                    navigateToAdaptiveStrength = navigateToAdaptiveStrength,
                    navigateToEqualizer = navigateToEqualizer
                )
            }

            item(key = "spacer_connection") { Spacer(modifier = Modifier.height(16.dp)) }
            item(key = "connection") {
                ConnectionSettings(
                    automaticEarDetectionEnabled = state.automaticEarDetectionEnabled,
                    onAutomaticEarDetectionChanged = onAutomaticEarDetectionChanged,
                    automaticConnectionEnabled = state.automaticConnectionEnabled,
                    onAutomaticConnectionChanged = onAutomaticConnectionChanged
                )
            }

            item(key = "spacer_microphone") { Spacer(modifier = Modifier.height(16.dp)) }
            item(key = "microphone") {
                val id = AACPManager.Companion.ControlCommandIdentifiers.MIC_MODE

                val selectedModeText = when (state.controlStates[id]?.getOrNull(0) ?: 0x00.toByte()) {
                    0x00.toByte() -> stringResource(R.string.microphone_automatic)
                    0x01.toByte() -> stringResource(R.string.microphone_always_right)
                    0x02.toByte() -> stringResource(R.string.microphone_always_left)
                    else -> stringResource(R.string.microphone_automatic)
                }

                StyledListItem(
                    name = stringResource(R.string.microphone_mode),
                    description = selectedModeText,
                    onClick = navigateToMicrophoneSettings
                )
            }

            if (capabilities.contains(Capability.SLEEP_DETECTION)) {
                item(key = "spacer_sleep") { Spacer(modifier = Modifier.height(16.dp)) }
                item(key = "sleep_detection") {
                    val id = AACPManager.Companion.ControlCommandIdentifiers.SLEEP_DETECTION_CONFIG
                    StyledToggle(
                        label = stringResource(R.string.sleep_detection),
                        checked = state.controlStates[id]?.getOrNull(0) == 0x01.toByte(),
                        onCheckedChange = { setControlCommandBoolean(id, it) }
                    )
                }
            }

            if (capabilities.contains(Capability.HEAD_GESTURES)) {
                item(key = "spacer_head_tracking") { Spacer(modifier = Modifier.height(16.dp)) }
                item(key = "head_tracking") {
                    StyledListItem(
                        name = stringResource(R.string.head_gestures),
                        description = if (sharedPreferences.getBoolean(
                                "head_gestures", false
                            )
                        ) stringResource(R.string.on) else stringResource(R.string.off),
                        onClick = navigateToHeadTracking
                    )
                }
            }

            item(key = "spacer_dynamic_end_of_charge") { Spacer(modifier = Modifier.height(16.dp)) }
            item(key = "dynamic_end_of_charge") {
                StyledToggle(
                    label = stringResource(R.string.optimized_charging),
                    description = stringResource(R.string.optimized_charging_description),
                    checked = state.dynamicEndOfCharge,
                    onCheckedChange = setDynamicEndOfCharge
                )
            }

            item(key = "spacer_accessibility") { Spacer(modifier = Modifier.height(16.dp)) }
            item(key = "accessibility") {
                StyledListItem(
                    name = stringResource(R.string.accessibility), onClick = navigateToAccessibility
                )
            }

            if (capabilities.contains(Capability.LOUD_SOUND_REDUCTION)) {
                item(key = "spacer_off_listening") { Spacer(modifier = Modifier.height(16.dp)) }
                item(key = "off_listening") {
                    val id = AACPManager.Companion.ControlCommandIdentifiers.ALLOW_OFF_OPTION
                    StyledToggle(
                        label = stringResource(R.string.off_listening_mode),
                        description = stringResource(R.string.off_listening_mode_description),
                        checked = state.controlStates[id]?.getOrNull(0) == 0x01.toByte(),
                        onCheckedChange = setOffListeningMode
                    )
                }
            }

            item(key = "spacer_disconnect") { Spacer(modifier = Modifier.height(28.dp)) }
            item(key = "disconnect_button") {
                StyledButton(
                    onClick = disconnect,
                    backdrop = rememberLayerBackdrop(),
                    isInteractive = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Text(
                        text = stringResource(R.string.disconnect),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

//                item(key = "spacer_debug") { Spacer(modifier = Modifier.height(16.dp)) }
//                item(key = "debug") { StyledListItem("debug", "Debug", navController) }

            item(key = "bottom_padding") { Spacer(modifier = Modifier.height(bottomPadding)) }
        }
    } else {
        val backdrop = rememberLayerBackdrop()
        Box(
            modifier = Modifier
                .drawBackdrop(
                    backdrop = rememberLayerBackdrop(),
                    exportedBackdrop = backdrop,
                    shape = { RoundedCornerShape(0.dp) },
                    highlight = {
                        Highlight.Ambient.copy(alpha = 0f)
                    },
                    effects = {}
                )
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = bottomPadding),
            contentAlignment = Alignment.Center
        ) {
            val tapCount = remember { mutableIntStateOf(0) }
            val lastTapTime = remember { mutableLongStateOf(0L) }

            var reconnecting by remember { mutableStateOf(false) }

            LaunchedEffect(reconnecting) {
                if (reconnecting) {
                    delay(5.seconds)
                    reconnecting = false
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    val now = System.currentTimeMillis()

                                    if (now - lastTapTime.longValue > 400) {
                                        tapCount.intValue = 0
                                    }

                                    tapCount.intValue++
                                    lastTapTime.longValue = now

                                    if (tapCount.intValue >= 5) {
                                        tapCount.intValue = 0
                                        activateDemoMode()
                                    }
                                })
                        }) {
                    Text(
                        text = stringResource(R.string.airpods_not_connected),
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.airpods_not_connected_description),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.connectionSuccessful) {
                    StyledButton(
                        onClick = { reconnectFromSavedMac(); reconnecting = true },
                        backdrop = backdrop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .widthIn(max = 200.dp),
                        enabled = !reconnecting
                    ) {
                        Text(
                            text = stringResource(R.string.reconnect_to_last_device),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

        }
    }
}

@Preview(name = "Apple")
@Composable
fun AirPodsSettingsScreenPreviewApple() {
    LibrePodsTheme {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            AirPodsSettingsScreen(
                state = demoState,

                setControlCommandInt = { _, _ -> },
                setControlCommandBoolean = { _, _ -> },
                setControlCommandByte = { _, _ -> },

                onAutomaticEarDetectionChanged = {},
                onAutomaticConnectionChanged = {},
                setDynamicEndOfCharge = {},
                setOffListeningMode = {},
                disconnect = {},

                navigateToRename = {},
                navigateToHearingProtection = {},
                navigateToLeftLongPress = {},
                navigateToRightLongPress = {},
                navigateToAdaptiveStrength = {},
                navigateToEqualizer = {},
                navigateToHeadTracking = {},
                navigateToAccessibility = {},
                navigateToCallControlScreen = {},
                navigateToMicrophoneSettings = {},

                activateDemoMode = {},
                reconnectFromSavedMac = {}
            )
        }
    }
}
