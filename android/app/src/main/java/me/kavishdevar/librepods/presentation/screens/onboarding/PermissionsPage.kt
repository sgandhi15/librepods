package me.kavishdevar.librepods.presentation.screens.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.presentation.MaterialIcons
import me.kavishdevar.librepods.presentation.components.ListItemOrientation
import me.kavishdevar.librepods.presentation.components.StyledIntroButton
import me.kavishdevar.librepods.presentation.components.StyledIntroPage
import me.kavishdevar.librepods.presentation.components.StyledIntroSubtitle
import me.kavishdevar.librepods.presentation.components.StyledList
import me.kavishdevar.librepods.presentation.components.StyledListItem
import me.kavishdevar.librepods.presentation.theme.sfProFamily

// iOS system colours used for Settings-style icon tiles.
private val BluetoothBlue = Color(0xFF007AFF)
private val NotificationRed = Color(0xFFFF3B30)
private val PhoneGreen = Color(0xFF34C759)
private val OverlayIndigo = Color(0xFF5856D6)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsPage(
    onForward: () -> Unit
) {

    var grantingAll = false

    val context = LocalContext.current
    val canDrawOverlays = remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val phonePermissionState = rememberMultiplePermissionsState(
        listOf(
            "android.permission.READ_PHONE_STATE",
            "android.permission.ANSWER_PHONE_CALLS"
        )
    ) {
        if (grantingAll) {
            if (!canDrawOverlays.value) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()
                )
                context.startActivity(intent)
            }
        }
    }


    val notificationPermissionState = rememberPermissionState("android.permission.POST_NOTIFICATIONS") {
        if (grantingAll) {
            if (!phonePermissionState.allPermissionsGranted) phonePermissionState.launchMultiplePermissionRequest()
            else if (!canDrawOverlays.value) canDrawOverlays.value = Settings.canDrawOverlays(context)
        }
    }


    val bluetoothPermissionsState = rememberMultiplePermissionsState(
        listOf(
            "android.permission.BLUETOOTH_CONNECT",
            "android.permission.BLUETOOTH_SCAN",
            "android.permission.BLUETOOTH",
            "android.permission.BLUETOOTH_ADMIN",
            "android.permission.BLUETOOTH_ADVERTISE"
        )
    ) {
        if (grantingAll) {
            if (!notificationPermissionState.status.isGranted) notificationPermissionState.launchPermissionRequest()
            else if (!phonePermissionState.allPermissionsGranted) phonePermissionState.launchMultiplePermissionRequest()
            else if (!canDrawOverlays.value) canDrawOverlays.value = Settings.canDrawOverlays(context)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canDrawOverlays.value = Settings.canDrawOverlays(context)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val bluetoothGranted = bluetoothPermissionsState.allPermissionsGranted
    val notificationsGranted = notificationPermissionState.status.isGranted
    val phoneGranted = phonePermissionState.allPermissionsGranted
    val overlayGranted = canDrawOverlays.value

    StyledIntroPage(
        title = stringResource(R.string.permissions),
        subtitle = { StyledIntroSubtitle("LibrePods needs Bluetooth to talk to your AirPods. Everything else is optional.") },
        buttons = {
            StyledIntroButton(
                text = stringResource(R.string.grant_all),
                primary = false,
                enabled = !bluetoothGranted || !notificationsGranted || !phoneGranted || !overlayGranted,
                onClick = {
                    grantingAll = true
                    if (!bluetoothPermissionsState.allPermissionsGranted) bluetoothPermissionsState.launchMultiplePermissionRequest()
                    else if (!notificationPermissionState.status.isGranted) notificationPermissionState.launchPermissionRequest()
                    else if (!phonePermissionState.allPermissionsGranted) phonePermissionState.launchMultiplePermissionRequest()
                    else if (!canDrawOverlays.value) canDrawOverlays.value =
                        Settings.canDrawOverlays(context)
                }
            )
            StyledIntroButton(
                text = stringResource(R.string.continue_label),
                enabled = bluetoothGranted,
                onClick = onForward
            )
        }
    ) {
        StyledList(title = "Required") {
            StyledListItem(
                name = "Bluetooth",
                onClick = if (!bluetoothGranted) {
                    {
                        grantingAll = false
                        bluetoothPermissionsState.launchMultiplePermissionRequest()
                    }
                } else null,
                description = "Required to communicate with AirPods",
                orientation = ListItemOrientation.Vertical,
                leadingContent = { PermissionIcon(MaterialIcons.bluetooth, BluetoothBlue) },
                trailingContent = { PermissionStatus(bluetoothGranted) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        StyledList(title = "Optional") {
            StyledListItem(
                name = "Notifications",
                onClick = if (!notificationsGranted) {
                    {
                        grantingAll = false
                        notificationPermissionState.launchPermissionRequest()
                    }
                } else null,
                description = "Show battery status",
                orientation = ListItemOrientation.Vertical,
                leadingContent = { PermissionIcon(MaterialIcons.notifications, NotificationRed) },
                trailingContent = { PermissionStatus(notificationsGranted) }
            )
            StyledListItem(
                name = "Phone",
                onClick = if (!phoneGranted) {
                    {
                        grantingAll = false
                        phonePermissionState.launchMultiplePermissionRequest()
                    }
                } else null,
                description = "Respond to phone calls with head gestures",
                orientation = ListItemOrientation.Vertical,
                leadingContent = { PermissionIcon(MaterialIcons.call, PhoneGreen) },
                trailingContent = { PermissionStatus(phoneGranted) }
            )
            StyledListItem(
                name = "Display over other apps",
                onClick = if (!overlayGranted) {
                    {
                        grantingAll = false
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:${context.packageName}".toUri()
                        )
                        context.startActivity(intent)
                    }
                } else null,
                description = "Show a popup when AirPods are nearby.",
                orientation = ListItemOrientation.Vertical,
                leadingContent = { PermissionIcon(MaterialIcons.stack, OverlayIndigo) },
                trailingContent = { PermissionStatus(overlayGranted) }
            )
        }
    }
}

/** Rounded-square coloured tile with a white glyph, like the icons in iOS Settings. */
@Composable
private fun PermissionIcon(icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(color, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(19.dp),
            tint = Color.White
        )
    }
}

/** "Allow" until the permission is granted, then a checkmark. */
@Composable
private fun PermissionStatus(granted: Boolean) {
    Crossfade(targetState = granted, label = "permissionStatus") { isGranted ->
        if (isGranted) {
            Text(
                text = "􀆅",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontFamily = sfProFamily,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(start = 8.dp, end = 4.dp)
            )
        } else {
            Text(
                text = stringResource(R.string.allow),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
