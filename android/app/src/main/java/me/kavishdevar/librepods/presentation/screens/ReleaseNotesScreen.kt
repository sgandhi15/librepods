package me.kavishdevar.librepods.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.kavishdevar.librepods.BuildConfig
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.updates.UpdateItem
import me.kavishdevar.librepods.data.updates.updates
import me.kavishdevar.librepods.presentation.components.StyledIntroButton
import me.kavishdevar.librepods.presentation.components.StyledIntroPage
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme

private const val PREVIEW_SCALE = 0.8f
private val PreviewHeight = 400.dp

@Composable
fun ReleaseNotesScreen(
    updates: List<UpdateItem>,
    releaseNotesShown: () -> Unit
) {
    StyledIntroPage(
        title = stringResource(R.string.what_s_new),
        subtitle = { VersionLink() },
        buttons = {
            StyledIntroButton(
                text = stringResource(R.string.continue_label),
                onClick = releaseNotesShown
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            updates.forEach { UpdateCard(it) }
        }
    }
}

@Composable
private fun VersionLink() {
    val versionName = BuildConfig.VERSION_NAME.removeSuffix("-debug").removeSuffix("-play")
    val url = "https://github.com/kavishdevar/librepods/releases/v$versionName"
    val fullText = "${stringResource(R.string.version)} $versionName"
    val linkColor = MaterialTheme.colorScheme.primary

    val annotatedString = buildAnnotatedString {
        append(fullText)
        addLink(
            url = LinkAnnotation.Url(
                url = url,
                styles = TextLinkStyles(style = SpanStyle(color = linkColor))
            ),
            start = 0,
            end = fullText.length
        )
    }
    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 17.sp),
        textAlign = TextAlign.Center
    )
}

/** One update: a scaled-down, non-interactive preview of the feature, then its title and description. */
@Composable
private fun UpdateCard(updateItem: UpdateItem) {
    val cardShape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surface, cardShape)
    ) {
        val outerDensity = LocalDensity.current
        Box(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .height(PreviewHeight)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            CompositionLocalProvider(
                LocalDensity provides Density(outerDensity.density * PREVIEW_SCALE, outerDensity.fontScale)
            ) {
                updateItem.demoComposeable()
            }
            // Takes touches so the preview can't be changed, while drags still scroll the page.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {}
            )
        }
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(updateItem.titleRes),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(updateItem.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview
@Composable
fun ReleaseNotesScreenPreview() {
    LibrePodsTheme {
        ReleaseNotesScreen(
            updates = updates,
            releaseNotesShown = { }
        )
    }
}
