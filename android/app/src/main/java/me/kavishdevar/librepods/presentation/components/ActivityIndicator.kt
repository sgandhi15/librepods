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

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val SPOKES = 8

/** iOS-style spinner: eight spokes whose brightness steps around the circle. */
@Composable
fun ActivityIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    color: Color = if (isSystemInDarkTheme()) Color(0xFFEBEBF5) else Color(0xFF3C3C43)
) {
    val transition = rememberInfiniteTransition(label = "activityIndicator")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = SPOKES.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spoke"
    )
    val head = progress.toInt() % SPOKES

    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val strokeWidth = radius * 0.22f
        val inner = radius * 0.45f
        val outer = radius - strokeWidth / 2f
        for (i in 0 until SPOKES) {
            // The newest spoke is fully opaque; the trail fades out behind it.
            val age = (head - i + SPOKES) % SPOKES
            val alpha = 1f - age * (0.75f / (SPOKES - 1))
            rotate(degrees = i * 360f / SPOKES) {
                drawLine(
                    color = color.copy(alpha = alpha),
                    start = Offset(center.x, center.y - inner),
                    end = Offset(center.x, center.y - outer),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
