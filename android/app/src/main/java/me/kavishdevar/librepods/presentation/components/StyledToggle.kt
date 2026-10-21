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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.theme.sectionHeader
import kotlin.io.encoding.ExperimentalEncodingApi

@Composable
fun StyledToggle(
    title: String? = null,
    label: String,
    description: String? = null,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        title?.let {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.sectionHeader,
                    style = MaterialTheme.typography.labelSmallEmphasized
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(28.dp)
                )
                .clip(RoundedCornerShape(28.dp))
        ) {
            StyledToggleContent(
                label = label,
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange,
                index = 0,
                count = 1
            )
        }
        if (description != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description, style = TextStyle(
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.6f),
                    fontFamily = FontFamily(Font(R.font.sf_pro)),
                ), modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun StyledListScope.StyledToggle(
    label: String,
    description: String? = null,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    item { index, count ->
        StyledToggleContent(
            label = label,
            description = description,
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            index = index,
            count = count
        )
    }
}

@Composable
private fun StyledToggleContent(
    label: String,
    description: String? = null,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    count: Int
) {
    val currentChecked by rememberUpdatedState(checked)

    val isDarkTheme = isSystemInDarkTheme()
    val textColor = if (isDarkTheme) Color.White else Color.Black

    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val isPressed = remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    shape = RoundedCornerShape(28.dp),
                    color = if (isPressed.value) Color(0xFFE0E0E0) else Color.Transparent
                )
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed.value = true
                            tryAwaitRelease()
                            isPressed.value = false
                        }
                    )
                }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (enabled) {
                        scope.launch { haptics.performHapticFeedback(if (!currentChecked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff) }
                        onCheckedChange(!currentChecked)
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                )

                if (description != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(0.8f)
                    )
                }
            }

            StyledSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = {
                    if (enabled) {
                        onCheckedChange(it)
                    }
                }
            )
        }
        if (index+1 != count) {
            HorizontalDivider(
                thickness = 1.dp,
                color = Color(0x40888888),
                modifier = Modifier
                    .padding(start = 12.dp,end = 12.dp)
            )
        }
    }
}

@Preview(name = "List", group = "Apple")
@Composable
fun StyledToggleAppleListPreview() {
    val checked = remember { mutableStateOf(false) }
    LibrePodsTheme {
        StyledList {
            StyledToggle(
                label = "Apple Styled List",
                description = "This is an example description for the styled toggle.",
                checked = checked.value,
                onCheckedChange = { checked.value = !checked.value }
            )
        }
    }
}

@Preview(name = "Normal", group = "Apple")
@Composable
fun StyledToggleApplePreview() {
    val checked = remember { mutableStateOf(false) }
    LibrePodsTheme {
        StyledToggle(
            label = "Apple",
            description = "This is an example description for the styled toggle.",
            checked = checked.value,
            onCheckedChange = { checked.value = !checked.value }
        )
    }
}
