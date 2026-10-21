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


package me.kavishdevar.librepods.presentation.overlays

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import me.kavishdevar.librepods.R
import me.kavishdevar.librepods.data.AirPodsNotifications
import me.kavishdevar.librepods.data.Battery
import me.kavishdevar.librepods.presentation.components.BatteryView
import me.kavishdevar.librepods.presentation.theme.LibrePodsTheme

/**
 * Card that slides up from the bottom when AirPods connect, like the one iOS shows:
 * the AirPods' name, the product images and battery rings.
 */
@SuppressLint("ClickableViewAccessibility")
class PopupWindow(
    private val context: Context,
    private val onCloseCallback: () -> Unit = {}
) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private val lifecycleOwner = OverlayLifecycleOwner()
    private val handler = Handler(Looper.getMainLooper())
    private val autoCloseRunnable = Runnable { close() }

    private var view: ComposeView? = null
    private var isClosing = false
    private var batteryUpdateReceiver: BroadcastReceiver? = null
    private var batteryList by mutableStateOf<List<Battery>>(emptyList())

    private val params = WindowManager.LayoutParams().apply {
        height = WindowManager.LayoutParams.WRAP_CONTENT
        val displayMetrics = context.resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        width = if (screenWidthDp >= 600) {
            (400 * displayMetrics.density).toInt()
        } else {
            WindowManager.LayoutParams.MATCH_PARENT
        }
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        dimAmount = 0.3f
        flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
    }

    fun open(
        name: String = "AirPods Pro",
        batteryNotification: AirPodsNotifications.BatteryNotification,
        budsRes: Int = R.drawable.airpods_pro_2_buds,
        caseRes: Int = R.drawable.airpods_pro_2_case
    ) {
        if (view != null || isClosing) return
        try {
            batteryList = batteryNotification.getBattery()
            // The window ignores system bar insets, so keep the card above the navigation bar ourselves.
            val navigationBarHeight = windowManager.currentWindowMetrics.windowInsets
                .getInsets(android.view.WindowInsets.Type.navigationBars()).bottom

            val composeView = ComposeView(context).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)
                setContent {
                    LibrePodsTheme {
                        ConnectionPopup(
                            name = name,
                            batteryList = batteryList,
                            budsRes = budsRes,
                            caseRes = caseRes,
                            bottomInsetPx = navigationBarHeight,
                            onClose = ::close
                        )
                    }
                }
                setOnTouchListener { _, event ->
                    if (event.action == MotionEvent.ACTION_OUTSIDE) {
                        close()
                        true
                    } else {
                        false
                    }
                }
            }

            lifecycleOwner.start()
            windowManager.addView(composeView, params)
            view = composeView

            composeView.translationY = context.resources.displayMetrics.heightPixels.toFloat()
            composeView.animate()
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(DecelerateInterpolator())
                .start()

            registerBatteryUpdateReceiver()
            handler.postDelayed(autoCloseRunnable, 12000)
        } catch (e: Exception) {
            Log.e("PopupWindow", "Error opening popup: ${e.message}")
            onCloseCallback()
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerBatteryUpdateReceiver() {
        batteryUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AirPodsNotifications.BATTERY_DATA) {
                    intent.getParcelableArrayListExtra("data", Battery::class.java)?.let {
                        batteryList = it
                    }
                }
            }
        }
        context.registerReceiver(
            batteryUpdateReceiver,
            IntentFilter(AirPodsNotifications.BATTERY_DATA),
            Context.RECEIVER_EXPORTED
        )
    }

    private fun unregisterBatteryUpdateReceiver() {
        batteryUpdateReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e("PopupWindow", "Error unregistering battery receiver: ${e.message}")
            }
            batteryUpdateReceiver = null
        }
    }

    fun close() {
        val currentView = view ?: return
        if (isClosing) return
        isClosing = true
        handler.removeCallbacks(autoCloseRunnable)
        unregisterBatteryUpdateReceiver()

        currentView.animate()
            .translationY(currentView.height.toFloat())
            .setDuration(400)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                try {
                    if (currentView.parent != null) windowManager.removeView(currentView)
                } catch (e: Exception) {
                    Log.e("PopupWindow", "Error removing view: ${e.message}")
                } finally {
                    lifecycleOwner.destroy()
                    view = null
                    isClosing = false
                    onCloseCallback()
                }
            }
            .start()
    }
}

@Composable
private fun ConnectionPopup(
    name: String,
    batteryList: List<Battery>,
    budsRes: Int,
    caseRes: Int,
    bottomInsetPx: Int,
    onClose: () -> Unit
) {
    val bottomInset = with(LocalDensity.current) { bottomInsetPx.toDp() }
    val shape = RoundedCornerShape(44.dp)
    val onSurface = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = bottomInset + 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 24.dp, shape = shape)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                // Tapping the card dismisses it, like the original popup.
                .clickable(interactionSource = null, indication = null, onClick = onClose)
                .padding(top = 20.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 40.dp)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(onSurface.copy(alpha = 0.1f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Close",
                        tint = onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                BatteryView(
                    batteryList = batteryList,
                    budsRes = budsRes,
                    caseRes = caseRes,
                    indicatorBackground = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

/** Overlay windows have no activity, so give Compose its own lifecycle and saved-state owner. */
private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun start() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
}
