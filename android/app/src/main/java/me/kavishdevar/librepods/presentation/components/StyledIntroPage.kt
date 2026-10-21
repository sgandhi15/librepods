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

package me.kavishdevar.librepods.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/** iOS-style large title, used at the top of onboarding and What's New pages. */
val introTitleStyle: TextStyle
    @Composable get() = MaterialTheme.typography.displaySmall.copy(
        fontSize = 34.sp,
        lineHeight = 41.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    )

/**
 * Full-screen page in the style of iOS setup screens: a large centred title,
 * scrolling content on the grouped background, and buttons pinned to the bottom.
 */
@Composable
fun StyledIntroPage(
    title: String,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    subtitle: (@Composable ColumnScope.() -> Unit)? = null,
    buttons: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        val background = MaterialTheme.colorScheme.surfaceContainer
        val isDarkTheme = isSystemInDarkTheme()
        val hazeState = rememberHazeState(blurEnabled = true)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (scrollable) Arrangement.Top else Arrangement.Center
            ) {
                // Leaves room for the floating back button.
                if (scrollable) Spacer(modifier = Modifier.height(topPadding + 64.dp))
                header?.let {
                    it()
                    Spacer(modifier = Modifier.height(24.dp))
                }
                Text(
                    text = title,
                    style = introTitleStyle,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                subtitle?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    it()
                }
                Spacer(modifier = Modifier.height(28.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                ) {
                    content()
                }
                if (scrollable) Spacer(modifier = Modifier.height(24.dp))
            }
            if (scrollable) {
                // Blurred bar under the status bar and back button, matching StyledScaffold's top bar.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(topPadding + 64.dp)
                        .hazeEffect(state = hazeState) {
                            backgroundColor = background
                            tints = listOf(
                                HazeTint(if (isDarkTheme) Color.Black.copy(0.55f) else Color(0xFFF2F2F7).copy(alpha = 0.85f))
                            )
                            blurRadius = 6.dp
                        }
                )
                // Fade content out above the buttons.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(Brush.verticalGradient(listOf(background.copy(alpha = 0f), background)))
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = bottomPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            buttons()
        }
    }
}

/** Grey centred text shown under an intro page title. */
@Composable
fun StyledIntroSubtitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 17.sp, lineHeight = 22.sp),
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

/** Full-width glass button. Primary is filled with the accent colour; secondary is tinted. */
@Composable
fun StyledIntroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
    enabled: Boolean = true
) {
    val accent = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(28.dp)
    StyledButton(
        onClick = onClick,
        backdrop = rememberLayerBackdrop(),
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            // StyledButton draws no fill when disabled, so paint a grey one underneath.
            .then(if (enabled) Modifier else Modifier.background(onSurface.copy(alpha = 0.08f), shape)),
        maxScale = 0.05f,
        enabled = enabled,
        surfaceColor = if (primary) accent else accent.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMediumEmphasized.copy(fontWeight = FontWeight.SemiBold),
            color = when {
                !enabled -> onSurface.copy(alpha = 0.3f)
                primary -> MaterialTheme.colorScheme.onPrimary
                else -> accent
            }
        )
    }
}
