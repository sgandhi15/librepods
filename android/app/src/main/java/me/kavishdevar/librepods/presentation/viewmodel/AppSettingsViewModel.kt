package me.kavishdevar.librepods.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class AppSettingsUiState(
    val showPhoneBatteryInWidget: Boolean = false,
    val conversationAwarenessPauseMusicEnabled: Boolean = false,
    val relativeConversationAwarenessVolumeEnabled: Boolean = true,
    val disconnectWhenNotWearing: Boolean = false,
    val takeoverWhenDisconnected: Boolean = false,
    val takeoverWhenIdle: Boolean = false,
    val takeoverWhenMusic: Boolean = false,
    val takeoverWhenCall: Boolean = false,
    val takeoverWhenRingingCall: Boolean = false,
    val takeoverWhenMediaStart: Boolean = false,
    val useAlternateHeadTrackingPackets: Boolean = true,
    val conversationAwarenessVolume: Float = 43f,
    val connectionSuccessful: Boolean = false,
    val showBottomSheetPopup: Boolean = true,
)

class AppSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val sharedPreferences = application.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(AppSettingsUiState())
    val uiState = _uiState.asStateFlow()

    val sharedPrefListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPref, key ->
        if (key == "connection_successful") {
            _uiState.update { it.copy(connectionSuccessful = sharedPref.getBoolean(key, false)) }
        }
    }

    init {
        loadSettings()
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPrefListener)
    }

    override fun onCleared() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(sharedPrefListener)
    }

    private fun loadSettings() {
        _uiState.update { currentState ->
            currentState.copy(
                showPhoneBatteryInWidget = sharedPreferences.getBoolean("show_phone_battery_in_widget", false),
                conversationAwarenessPauseMusicEnabled = sharedPreferences.getBoolean("conversation_awareness_pause_music", false),
                relativeConversationAwarenessVolumeEnabled = sharedPreferences.getBoolean("relative_conversation_awareness_volume", true),
                disconnectWhenNotWearing = sharedPreferences.getBoolean("disconnect_when_not_wearing", false),
                takeoverWhenDisconnected = sharedPreferences.getBoolean("takeover_when_disconnected", false),
                takeoverWhenIdle = sharedPreferences.getBoolean("takeover_when_idle", false),
                takeoverWhenMusic = sharedPreferences.getBoolean("takeover_when_music", false),
                takeoverWhenCall = sharedPreferences.getBoolean("takeover_when_call", false),
                takeoverWhenRingingCall = sharedPreferences.getBoolean("takeover_when_ringing_call", false),
                takeoverWhenMediaStart = sharedPreferences.getBoolean("takeover_when_media_start", false),
                useAlternateHeadTrackingPackets = sharedPreferences.getBoolean("use_alternate_head_tracking_packets", true),
                conversationAwarenessVolume = sharedPreferences.getInt("conversation_awareness_volume", 43).toFloat(),
                connectionSuccessful = sharedPreferences.getBoolean("connection_successful", false),
                showBottomSheetPopup = sharedPreferences.getBoolean("show_bottom_sheet_popup", true),
            )
        }
    }

    fun setShowPhoneBatteryInWidget(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("show_phone_battery_in_widget", enabled) }
        _uiState.update { it.copy(showPhoneBatteryInWidget = enabled) }
    }

    fun setConversationAwarenessPauseMusicEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("conversation_awareness_pause_music", enabled) }
        _uiState.update { it.copy(conversationAwarenessPauseMusicEnabled = enabled) }
    }

    fun setRelativeConversationAwarenessVolumeEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("relative_conversation_awareness_volume", enabled) }
        _uiState.update { it.copy(relativeConversationAwarenessVolumeEnabled = enabled) }
    }

    fun setDisconnectWhenNotWearing(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("disconnect_when_not_wearing", enabled) }
        _uiState.update { it.copy(disconnectWhenNotWearing = enabled) }
    }

    fun setTakeoverWhenDisconnected(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("takeover_when_disconnected", enabled) }
        _uiState.update { it.copy(takeoverWhenDisconnected = enabled) }
    }

    fun setTakeoverWhenIdle(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("takeover_when_idle", enabled) }
        _uiState.update { it.copy(takeoverWhenIdle = enabled) }
    }

    fun setTakeoverWhenMusic(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("takeover_when_music", enabled) }
        _uiState.update { it.copy(takeoverWhenMusic = enabled) }
    }

    fun setTakeoverWhenCall(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("takeover_when_call", enabled) }
        _uiState.update { it.copy(takeoverWhenCall = enabled) }
    }

    fun setTakeoverWhenRingingCall(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("takeover_when_ringing_call", enabled) }
        _uiState.update { it.copy(takeoverWhenRingingCall = enabled) }
    }

    fun setTakeoverWhenMediaStart(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("takeover_when_media_start", enabled) }
        _uiState.update { it.copy(takeoverWhenMediaStart = enabled) }
    }

    fun setUseAlternateHeadTrackingPackets(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("use_alternate_head_tracking_packets", enabled) }
        _uiState.update { it.copy(useAlternateHeadTrackingPackets = enabled) }
    }

    fun setConversationAwarenessVolume(volume: Float) {
        sharedPreferences.edit { putInt("conversation_awareness_volume", volume.roundToInt()) }
        _uiState.update { it.copy(conversationAwarenessVolume = volume) }
    }

    fun setShowBottomSheetPopup(enabled: Boolean) {
        sharedPreferences.edit { putBoolean("show_bottom_sheet_popup", enabled) }
        _uiState.update { it.copy(showBottomSheetPopup = enabled) }
    }
}
