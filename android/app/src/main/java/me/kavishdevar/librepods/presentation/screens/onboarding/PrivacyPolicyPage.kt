package me.kavishdevar.librepods.presentation.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.components.StyledIntroButton
import me.kavishdevar.librepods.presentation.components.StyledIntroPage
import me.kavishdevar.librepods.presentation.components.StyledIntroSubtitle
import me.kavishdevar.librepods.presentation.theme.sectionHeader

@Composable
fun PrivacyPolicyPage(
    onForward: () -> Unit
) {
    StyledIntroPage(
        title = stringResource(R.string.privacy_policy),
        subtitle = { StyledIntroSubtitle("Last updated: 20 June 2026") },
        buttons = {
            StyledIntroButton(
                text = stringResource(R.string.i_agree),
                onClick = onForward
            )
        }
    ) {
        PolicySection("Overview") {
            PolicyText("LibrePods does not collect, store, sell, or share personal information for advertising, analytics, tracking, or profiling purposes. The app does not include analytics, crash reporting, telemetry, advertising SDKs, or tracking services.")
            PolicyText("All information remains on your device unless you explicitly choose to contact me or create a GitHub issue from the app.")
        }

        PolicySection("Third Party Services") {
            PolicyText("LibrePods provides several ways to contact me, including email, Discord, and GitHub Issues.")

            PolicyHeading("Email")
            PolicyText("If you contact me by email, I receive your email address and any information you choose to include in your message. When using the contact form within LibrePods, your email client will open with a pre-filled email address, the subject line and body that you fill out. The body will also include LibrePods version information and device information to help with troubleshooting.")
            PolicyText("You can edit or remove any of this information before sending the email.")

            PolicyHeading("Discord")
            PolicyText("The app provides a link to the LibrePods Discord server. If you choose to join the Discord server, you will be subject to Discord's privacy policy.")
            PolicyText("I do not receive any information about you from Discord other than what is publicly visible in the Discord server, such as your username, joining date, common servers, and any messages or content you post in the server, unless you choose to share it with me in the Discord server.")

            PolicyHeading("GitHub Issues")
            PolicyText("When creating a GitHub issue through LibrePods, the app will pre-fill the issue form with:")
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                PolicyText("• LibrePods version name and version code")
                PolicyText("• Device manufacturer and model")
                PolicyText("• Android build information")
                PolicyText("• Installation source (Google Play or GitHub)")
            }
            PolicyText("This information helps diagnose bugs and provide support. No information is sent automatically. The information is only submitted if you choose to create the GitHub issue.")
        }

        PolicySection("Contact") {
            PolicyText("If you have questions about this privacy policy, please contact me via email at privacy@kavish.xyz.")
        }
    }
}

/** Grouped card with a small grey header above it, like an iOS settings section. */
@Composable
private fun PolicySection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.sectionHeader,
            style = MaterialTheme.typography.labelSmallEmphasized
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun PolicyHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMediumEmphasized,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun PolicyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    )
}
