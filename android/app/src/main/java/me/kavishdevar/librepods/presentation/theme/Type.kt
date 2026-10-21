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

package me.kavishdevar.librepods.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.R

val sfProFamily = FontFamily(Font(R.font.sf_pro))

val AppleTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = sfProFamily),
        displayMedium = displayMedium.copy(fontFamily = sfProFamily),
        displaySmall = displaySmall.copy(fontFamily = sfProFamily),

        headlineLarge = headlineLarge.copy(fontFamily = sfProFamily),
        headlineMedium = headlineMedium.copy(fontFamily = sfProFamily),
        headlineSmall = headlineSmall.copy(fontFamily = sfProFamily),

        titleLarge = titleLarge.copy(fontFamily = sfProFamily),
        titleMedium = titleMedium.copy(fontFamily = sfProFamily),
        titleSmall = titleSmall.copy(fontFamily = sfProFamily),

        bodyLarge = bodyLarge.copy(fontFamily = sfProFamily),
        bodyMedium = bodyMedium.copy(
            fontFamily = sfProFamily,
            fontSize = 16.sp
        ),
        bodySmall = bodySmall.copy(
            fontFamily = sfProFamily,
            fontSize = 14.sp,
            lineHeight = 18.sp
        ),

        labelLarge = labelLarge.copy(fontFamily = sfProFamily),

        labelMedium = labelMedium.copy(
            fontFamily = sfProFamily,
            fontSize = 16.sp,
        ),
        labelMediumEmphasized = labelMediumEmphasized.copy(
            fontFamily = sfProFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        ),
        labelSmallEmphasized = labelSmallEmphasized.copy(
            fontFamily = sfProFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    )
}
