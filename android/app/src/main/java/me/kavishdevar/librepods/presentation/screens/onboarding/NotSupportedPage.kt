package me.kavishdevar.librepods.presentation.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.AppInfoCard
import me.kavishdevar.librepods.presentation.components.DeviceInfoCard
import me.kavishdevar.librepods.presentation.components.StyledIntroPage
import me.kavishdevar.librepods.presentation.components.StyledIntroSubtitle
import me.kavishdevar.librepods.presentation.components.StyledListItem

@Composable
fun NotSupportedPage(
    bypassCompatibilityCheck: () -> Unit
) {
    StyledIntroPage(
        title = stringResource(R.string.not_supported),
        subtitle = { StyledIntroSubtitle(stringResource(R.string.not_compatible_update)) }
    ) {
        Text(
            text = stringResource(R.string.not_compatitble_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                .padding(horizontal = 20.dp, vertical = 18.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        DeviceInfoCard()
        Spacer(modifier = Modifier.height(24.dp))
        AppInfoCard()
        if (!BuildConfig.PLAY_BUILD) {
            Spacer(modifier = Modifier.height(24.dp))
            StyledListItem(
                name = stringResource(R.string.bypass_compatibility_check),
                onClick = bypassCompatibilityCheck
            )
        }
    }
}
