package me.kavishdevar.librepods.screenshots

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.edit
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.QuickSettingsDialogActivity
import me.kavishdevar.librepods.data.ControlCommandRepository
import me.kavishdevar.librepods.presentation.navigation.NavigationRoot
import me.kavishdevar.librepods.presentation.navigation.Screen
import me.kavishdevar.librepods.presentation.screens.LoadingScreen
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme
import me.kavishdevar.librepods.presentation.viewmodel.AirPodsViewModel
import me.kavishdevar.librepods.presentation.overlays.PopupWindow
import me.kavishdevar.librepods.presentation.widgets.BatteryWidget
import me.kavishdevar.librepods.presentation.widgets.NoiseControlWidget
import me.kavishdevar.librepods.services.AirPodsService

/** Renders the real app, with its existing demo mode, for adb framebuffer captures. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
class NativeScreenshotActivity : ComponentActivity() {
    private val airPodsViewModel: AirPodsViewModel by viewModels()
    private var ready by mutableStateOf(false)
    private var bound = false
    private var widgetView by mutableStateOf<AppWidgetHostView?>(null)
    private var widgetHost: AppWidgetHost? = null
    private var widgetId: Int? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as AirPodsService.LocalBinder).getService()
            val prefs = getSharedPreferences("settings", MODE_PRIVATE)
            if (intent.getBooleanExtra("demo", true)) airPodsViewModel.activateDemoMode()
            airPodsViewModel.init(service, ControlCommandRepository(service.aacpManager), prefs, applicationContext)
            ready = true
            val surface = intent.getStringExtra("surface")
            if (surface != null) {
                service.batteryNotification.setBatteryDirect(80, false, 18, true, 76, false)
                service.widgetMobileBatteryEnabled = surface == "battery-widget-phone"
            }
            Log.i("NativeScreenshot", "READY ${intent.getStringExtra("capture_token")}")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            ready = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(Build.HARDWARE in setOf("ranchu", "goldfish")) {
            "Native screenshot fixtures may only be launched on an Android emulator."
        }
        enableEdgeToEdge()
        val demo = intent.getBooleanExtra("demo", true)
        val scene = intent.getStringExtra("screen") ?: "home"
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        prefs.edit {
            putBoolean("connection_successful", demo)
            putString("name", "[DEMO] AirPods Pro")
        }

        val target = when (scene) {
            "home", "disconnected" -> Screen.AirPodsSettings
            "onboarding" -> Screen.Onboarding
            "rename" -> Screen.Rename
            "settings" -> Screen.AppSettings
            "head-tracking" -> Screen.HeadTracking
            "accessibility" -> Screen.Accessibility
            "adaptive" -> Screen.AdaptiveStrength
            "version" -> Screen.VersionInfo
            "hearing-protection" -> Screen.HearingProtection
            "equalizer" -> Screen.Equalizer
            "long-press-left" -> Screen.LongPress("Left")
            "long-press-right" -> Screen.LongPress("Right")
            "call-mute" -> Screen.CallControl(getString(R.string.mute_unmute))
            "call-hangup" -> Screen.CallControl(getString(R.string.hang_up))
            "microphone" -> Screen.MicrophoneSettings
            "release-notes" -> Screen.ReleaseNotes
            "loading" -> null
            else -> error("Unknown screenshot scene: $scene")
        }

        setContent {
            LibrePodsTheme {
                LaunchedEffect(ready) {
                    if (ready) {
                        // Wait for the destination composition and its first rendered frames,
                        // rather than capturing the temporary LoadingScreen after service binding.
                        repeat(3) { withFrameNanos { } }
                        intent.getStringExtra("surface")?.let { surface ->
                            showSurface(surface, me.kavishdevar.librepods.services.ServiceManager.getService()!!)
                        }
                        Log.i("NativeScreenshot", "DRAWN ${intent.getStringExtra("capture_token")}")
                    }
                }
                val nativeWidget = widgetView
                if (nativeWidget != null) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer).padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(factory = { nativeWidget }, modifier = Modifier.fillMaxWidth().height(150.dp))
                    }
                } else if (ready && target != null) {
                    NavigationRoot(
                        showOnboarding = target == Screen.Onboarding,
                        airPodsViewModel = airPodsViewModel,
                        initialScreen = target
                    )
                } else {
                    LoadingScreen()
                }
            }
        }
        startForegroundService(Intent(this, AirPodsService::class.java))
        bound = bindService(Intent(this, AirPodsService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        widgetHost?.stopListening()
        widgetId?.let { widgetHost?.deleteAppWidgetId(it) }
        if (bound) unbindService(connection)
        super.onDestroy()
    }

    private fun showSurface(surface: String, service: AirPodsService) {
        when {
            surface == "notification" || surface == "qs-tile" -> service.sendBatteryNotification()
            surface == "quick-settings" -> startActivity(Intent(this, QuickSettingsDialogActivity::class.java))
            surface == "connection-popup" -> PopupWindow(this).open("[DEMO] AirPods Pro", service.batteryNotification)
            surface.endsWith("widget") || surface == "battery-widget-phone" -> {
                val provider = if (surface == "noise-widget") NoiseControlWidget::class.java else BatteryWidget::class.java
                val manager = AppWidgetManager.getInstance(this)
                val host = AppWidgetHost(this, 27017).also { widgetHost = it }
                val id = host.allocateAppWidgetId().also { widgetId = it }
                check(manager.bindAppWidgetIdIfAllowed(id, ComponentName(this, provider))) {
                    "Grant widget binding to this debug app with adb shell appwidget grantbind."
                }
                widgetView = host.createView(this, id, manager.getAppWidgetInfo(id))
                host.startListening()
                service.updateBatteryWidget()
                service.updateNoiseControlWidget()
            }
            else -> error("Unknown native surface: $surface")
        }
    }
}
