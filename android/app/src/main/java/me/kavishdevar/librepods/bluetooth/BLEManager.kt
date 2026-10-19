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

package me.kavishdevar.librepods.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import me.kavishdevar.librepods.data.AirPodsNotifications
import me.kavishdevar.librepods.utils.BluetoothCryptography
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.iterator
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Manager for Bluetooth Low Energy scanning operations specifically for AirPods
 */
@OptIn(ExperimentalEncodingApi::class)
class BLEManager(private val context: Context) {

    data class AirPodsStatus(
        val address: String,
        val lastSeen: Long = System.currentTimeMillis(),
        val paired: Boolean = false,
        val model: String = "Unknown",
        val leftBattery: Int? = null,
        val rightBattery: Int? = null,
        val caseBattery: Int? = null,
        val isLeftInEar: Boolean = false,
        val isRightInEar: Boolean = false,
        val isLeftCharging: Boolean = false,
        val isRightCharging: Boolean = false,
        val isCaseCharging: Boolean = false,
        val lidOpen: Boolean = false,
        val color: String = "Unknown",
        val connectionState: String = "Unknown"
    )

    fun getMostRecentStatus(): AirPodsStatus? {
        return deviceStatusMap.values.maxByOrNull { it.lastSeen }
    }

    interface AirPodsStatusListener {
        /** Called for every processed advertisement, changed or not. Keep it cheap. */
        fun onDeviceSeen(device: AirPodsStatus)
        fun onDeviceStatusChanged(device: AirPodsStatus, previousStatus: AirPodsStatus?)
        fun onBroadcastFromNewAddress(device: AirPodsStatus)
        fun onLidStateChanged(lidOpen: Boolean)
        fun onEarStateChanged(device: AirPodsStatus, leftInEar: Boolean, rightInEar: Boolean)
        fun onBatteryChanged(device: AirPodsStatus)
        fun onDeviceDisappeared()
    }

    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mScanCallback: ScanCallback? = null
    private var airPodsStatusListener: AirPodsStatusListener? = null
    private val deviceStatusMap = mutableMapOf<String, AirPodsStatus>()
    private val verifiedAddresses = mutableSetOf<String>()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private var currentGlobalLidState: Boolean? = null
    private var lastBroadcastTime: Long = 0
    private val processedAddresses = mutableSetOf<String>()

    // Addresses that failed RPA verification (for example, someone else's AirPods nearby).
    // Remembered so we don't run AES on every advertisement they send.
    private val rejectedAddresses = mutableSetOf<String>()

    // Decoded keys, cached so each advertisement doesn't decode Base64 and build a new Cipher.
    private var cachedIrkBase64: String? = null
    private var cachedIrk: ByteArray? = null
    private var cachedEncKeyBase64: String? = null
    private var cachedDecryptCipher: Cipher? = null

    /**
     * How hard we scan. Scanning is the main battery cost of this class, so we only use
     * the fast mode while the screen is on and AirPods aren't connected yet, which is when
     * the lid-open popup has to appear quickly.
     */
    private enum class ScanProfile(
        val scanMode: Int,
        val reportDelayMs: Long,
        val lidCloseTimeoutMs: Long,
        val staleDeviceTimeoutMs: Long
    ) {
        // Screen on, AirPods not connected.
        FAST(ScanSettings.SCAN_MODE_LOW_LATENCY, 500L, 2_500L, 15_000L),
        // Screen on, AirPods connected. Battery and state come over the AACP socket instead.
        BALANCED(ScanSettings.SCAN_MODE_BALANCED, 1_000L, 6_000L, 20_000L),
        // Screen off, or AirPods connected and both in the ears (so the case lid can't be opened
        // to take them out). A slow scan is enough.
        LOW_POWER(ScanSettings.SCAN_MODE_LOW_POWER, 5_000L, 15_000L, 30_000L)
    }

    private var scanningRequested = false
    private var activeProfile: ScanProfile? = null
    private var isScreenOn = true
    private var isAudioLinkConnected = false
    private var bothInEar = false
    private val scanStartTimes = ArrayDeque<Long>()
    private val currentProfile: ScanProfile get() = activeProfile ?: ScanProfile.FAST

    private val profileHandler = Handler(Looper.getMainLooper())
    private val applyProfileRunnable = Runnable { applyDesiredProfile() }

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    // Switch right away so opening the case right after waking the phone still shows the popup quickly.
                    profileHandler.removeCallbacks(applyProfileRunnable)
                    applyDesiredProfile()
                    return
                }
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    // Wait a while, so quickly checking the phone doesn't restart the scan
                    // twice and use up Android's scan-start allowance.
                    scheduleProfileUpdate(SCREEN_OFF_DEBOUNCE_MS)
                    return
                }
                AirPodsNotifications.AIRPODS_CONNECTED -> isAudioLinkConnected = true
                AirPodsNotifications.AIRPODS_DISCONNECTED -> {
                    isAudioLinkConnected = false
                    bothInEar = false
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_OFF) {
                        // Stop explicitly (some phones keep BLE scans alive with Bluetooth "off"),
                        // and forget the scan so it's restarted when Bluetooth comes back.
                        stopScanInternal()
                        return
                    }
                    if (state != BluetoothAdapter.STATE_ON) return
                }
                else -> return
            }
            scheduleProfileUpdate()
        }
    }
    private var stateReceiverRegistered = false

    // Without the IRK every advertisement is thrown away, so there's no point scanning until we
    // have one. It's saved after the first connection to the AirPods.
    private val keyListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == AacpIrkKey) scheduleProfileUpdate()
    }

    private val lastValidCaseBatteryMap = mutableMapOf<String, Int>()
    private val modelNames = mapOf(
        0x0E20 to "AirPods Pro",
        0x1420 to "AirPods Pro 2",
        0x2420 to "AirPods Pro 2 (USB-C)",
        0x0220 to "AirPods 1",
        0x0F20 to "AirPods 2",
        0x1320 to "AirPods 3",
        0x1920 to "AirPods 4",
        0x1B20 to "AirPods 4 (ANC)",
        0x0A20 to "AirPods Max",
        0x1F20 to "AirPods Max (USB-C)"
    )

    val colorNames = mapOf(
        0x00 to "White", 0x01 to "Black", 0x02 to "Red", 0x03 to "Blue",
        0x04 to "Pink", 0x05 to "Gray", 0x06 to "Silver", 0x07 to "Gold",
        0x08 to "Rose Gold", 0x09 to "Space Gray", 0x0A to "Dark Blue",
        0x0B to "Light Blue", 0x0C to "Yellow"
    )

    val connStates = mapOf(
        0x00 to "Disconnected", 0x04 to "Idle", 0x05 to "Music",
        0x06 to "Call", 0x07 to "Ringing", 0x09 to "Hanging Up", 0xFF to "Unknown"
    )

    private val cleanupHandler = Handler(Looper.getMainLooper())
    private val cleanupRunnable = object : Runnable {
        override fun run() {
            cleanupStaleDevices()
            checkLidStateTimeout()
            cleanupHandler.postDelayed(this, CLEANUP_INTERVAL_MS)
        }
    }

    /** The scan profile to use right now, or null if we shouldn't scan at all. */
    private fun desiredProfile(): ScanProfile? {
        if (getIrkFromPreferences() == null) return null
        val linkUp = isAudioLinkConnected && BluetoothConnectionManager.aacpSocket?.isConnected == true
        return when {
            !isScreenOn -> ScanProfile.LOW_POWER
            linkUp && bothInEar -> ScanProfile.LOW_POWER
            linkUp -> ScanProfile.BALANCED
            else -> ScanProfile.FAST
        }
    }

    /** Called by the service when the in-ear state reported over the AACP socket changes. */
    fun setBothInEar(inEar: Boolean) {
        profileHandler.post {
            if (bothInEar == inEar) return@post
            bothInEar = inEar
            // Wait a bit so adjusting an AirPod doesn't restart the scan each time.
            scheduleProfileUpdate(EAR_STATE_DEBOUNCE_MS)
        }
    }

    private fun scheduleProfileUpdate(delayMs: Long = PROFILE_CHANGE_DEBOUNCE_MS) {
        profileHandler.removeCallbacks(applyProfileRunnable)
        profileHandler.postDelayed(applyProfileRunnable, delayMs)
    }

    private fun applyDesiredProfile() {
        if (!scanningRequested) return
        val desired = desiredProfile()
        if (desired == null) {
            if (mScanCallback != null) {
                Log.d(TAG, "No IRK saved, stopping BLE scan")
                stopScanInternal()
            }
            return
        }
        if (desired == activeProfile && mScanCallback != null) return

        // Android silently stops delivering results to apps that start more than
        // 5 scans in 30 seconds, so space out restarts if the screen is toggled a lot.
        val now = SystemClock.elapsedRealtime()
        while (scanStartTimes.isNotEmpty() && now - scanStartTimes.first() > SCAN_START_WINDOW_MS) {
            scanStartTimes.removeFirst()
        }
        if (scanStartTimes.size >= MAX_SCAN_STARTS_PER_WINDOW) {
            val wait = SCAN_START_WINDOW_MS - (now - scanStartTimes.first()) + 500L
            Log.d(TAG, "Delaying scan profile change to $desired by ${wait}ms to avoid scan throttling")
            profileHandler.removeCallbacks(applyProfileRunnable)
            profileHandler.postDelayed(applyProfileRunnable, wait)
            return
        }

        Log.d(TAG, "Switching BLE scan profile: $activeProfile -> $desired")
        startScanInternal(desired)
    }

    private fun registerStateReceiver() {
        if (stateReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(AirPodsNotifications.AIRPODS_CONNECTED)
            addAction(AirPodsNotifications.AIRPODS_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        try {
            context.registerReceiver(stateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            stateReceiverRegistered = true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to register scan state receiver", t)
        }
    }

    private fun unregisterStateReceiver() {
        if (!stateReceiverRegistered) return
        try {
            context.unregisterReceiver(stateReceiver)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to unregister scan state receiver", t)
        }
        stateReceiverRegistered = false
    }

    fun setAirPodsStatusListener(listener: AirPodsStatusListener) {
        airPodsStatusListener = listener
    }

    fun startScanning() {
        profileHandler.post {
            scanningRequested = true
            isScreenOn = context.getSystemService(PowerManager::class.java)?.isInteractive ?: true
            isAudioLinkConnected = BluetoothConnectionManager.aacpSocket?.isConnected == true
            registerStateReceiver()
            sharedPreferences.registerOnSharedPreferenceChangeListener(keyListener)
            profileHandler.removeCallbacks(applyProfileRunnable)
            val profile = desiredProfile()
            if (profile != null) {
                startScanInternal(profile)
            } else {
                Log.d(TAG, "No IRK saved yet, not scanning until the AirPods connect once")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startScanInternal(profile: ScanProfile) {
        try {
            Log.d(TAG, "Starting BLE scanner with profile $profile")

            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val btAdapter = btManager.adapter

            if (btAdapter == null) {
                Log.d(TAG, "No Bluetooth adapter available")
                return
            }

            if (mBluetoothLeScanner != null && mScanCallback != null) {
                try {
                    // Deliver anything still batched before restarting, so it isn't lost.
                    if (btAdapter.isEnabled) mBluetoothLeScanner?.flushPendingScanResults(mScanCallback)
                    mBluetoothLeScanner?.stopScan(mScanCallback)
                } catch (t: Throwable) {
                    // Throws if Bluetooth is off; the old scan is gone anyway.
                    Log.w(TAG, "Couldn't stop previous BLE scan: ${t.message}")
                }
                mScanCallback = null
                activeProfile = null
            }

            if (!btAdapter.isEnabled) {
                Log.d(TAG, "Bluetooth is disabled")
                activeProfile = null
                cleanupHandler.removeCallbacks(cleanupRunnable)
                return
            }

            mBluetoothLeScanner = btAdapter.bluetoothLeScanner

            val scanSettings = ScanSettings.Builder()
                .setScanMode(profile.scanMode)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setReportDelay(profile.reportDelayMs)
                .build()

            val manufacturerData = ByteArray(27)
            val manufacturerDataMask = ByteArray(27)

            manufacturerData[0] = 7
            manufacturerData[1] = 25

            manufacturerDataMask[0] = -1
            manufacturerDataMask[1] = -1

            val scanFilter = ScanFilter.Builder()
                .setManufacturerData(76, manufacturerData, manufacturerDataMask)
                .build()

            val callback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    processScanResult(result)
                }

                override fun onBatchScanResults(results: List<ScanResult>) {
                    processedAddresses.clear()
                    // Results are oldest first and only one per address is processed,
                    // so go newest first to act on the latest state.
                    for (result in results.asReversed()) {
                        processScanResult(result)
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Log.e(TAG, "BLE scan failed with error code: $errorCode")
                    val failed = this
                    profileHandler.post {
                        if (mScanCallback !== failed) return@post
                        // Forget the dead scan so the next profile check starts a new one, and retry later.
                        mScanCallback = null
                        activeProfile = null
                        cleanupHandler.removeCallbacks(cleanupRunnable)
                        if (scanningRequested) scheduleProfileUpdate(SCAN_RETRY_DELAY_MS)
                    }
                }
            }

            mBluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, callback)
            mScanCallback = callback
            activeProfile = profile
            scanStartTimes.addLast(SystemClock.elapsedRealtime())
            Log.d(TAG, "BLE scanner started successfully")

            // Remove first so repeated starts don't stack up several cleanup loops.
            cleanupHandler.removeCallbacks(cleanupRunnable)
            cleanupHandler.postDelayed(cleanupRunnable, CLEANUP_INTERVAL_MS)
        } catch (t: Throwable) {
            Log.e(TAG, "Error starting BLE scanner", t)
            if (scanningRequested) scheduleProfileUpdate(SCAN_RETRY_DELAY_MS)
        }
    }

    fun stopScanning() {
        profileHandler.post {
            scanningRequested = false
            profileHandler.removeCallbacks(applyProfileRunnable)
            unregisterStateReceiver()
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(keyListener)
            stopScanInternal()
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanInternal() {
        activeProfile = null
        cleanupHandler.removeCallbacks(cleanupRunnable)
        try {
            if (mBluetoothLeScanner != null && mScanCallback != null) {
                Log.d(TAG, "Stopping BLE scanner")
                mBluetoothLeScanner?.stopScan(mScanCallback)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error stopping BLE scanner", t)
        }
        mScanCallback = null
    }

    /** Returns a ready-to-use AES decrypt cipher for the stored encryption key, or null if there is none. */
    @SuppressLint("GetInstance")
    @OptIn(ExperimentalEncodingApi::class)
    private fun getDecryptCipher(): Cipher? {
        val keyBase64 = sharedPreferences.getString(AACPManager.Companion.ProximityKeyType.ENC_KEY.name, null)
        if (keyBase64 != cachedEncKeyBase64) {
            cachedEncKeyBase64 = keyBase64
            cachedDecryptCipher = keyBase64?.let {
                try {
                    Cipher.getInstance("AES/ECB/NoPadding").apply {
                        init(Cipher.DECRYPT_MODE, SecretKeySpec(Base64.decode(it), "AES"))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load encryption key", e)
                    null
                }
            }
        }
        return cachedDecryptCipher
    }

    private fun decryptLastBytes(data: ByteArray, cipher: Cipher): ByteArray? {
        return try {
            if (data.size < 16) {
                return null
            }

            val block = data.copyOfRange(data.size - 16, data.size)
            cipher.doFinal(block)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting data", e)
            null
        }
    }

    private fun formatBattery(byteVal: Int): Pair<Boolean, Int> {
        val charging = (byteVal and 0x80) != 0
        val level = byteVal and 0x7F
        return Pair(charging, level)
    }

    private fun processScanResult(result: ScanResult) {
        try {
            val scanRecord = result.scanRecord ?: return
            val address = result.device.address

            if (processedAddresses.contains(address)) {
                return
            }

            val manufacturerData = scanRecord.getManufacturerSpecificData(76) ?: return
            if (manufacturerData.size <= 20) return

            if (!verifiedAddresses.contains(address)) {
                val irk = getIrkFromPreferences() ?: return
                if (rejectedAddresses.contains(address)) return
                if (!BluetoothCryptography.verifyRPA(address, irk)) {
                    // Addresses rotate, so keep this from growing forever.
                    if (rejectedAddresses.size >= MAX_REJECTED_ADDRESSES) rejectedAddresses.clear()
                    rejectedAddresses.add(address)
                    return
                }
                verifiedAddresses.add(address)
                Log.d(TAG, "RPA verified and added to trusted list: $address")
            }

            processedAddresses.add(address)
            lastBroadcastTime = System.currentTimeMillis()

            val decryptCipher = getDecryptCipher()
            val decryptedData = if (decryptCipher != null) decryptLastBytes(manufacturerData, decryptCipher) else null
            val parsedStatus = if (decryptedData != null && decryptedData.size == 16) {
                parseProximityMessageWithDecryption(address, manufacturerData, decryptedData)
            } else {
                parseProximityMessage(address, manufacturerData)
            }

            val previousStatus = deviceStatusMap[address]
            deviceStatusMap[address] = parsedStatus

            airPodsStatusListener?.let { listener ->
                listener.onDeviceSeen(parsedStatus)

                if (previousStatus == null) {
                    listener.onBroadcastFromNewAddress(parsedStatus)
                    Log.d(TAG, "New AirPods device detected: $address")

                    if (currentGlobalLidState == null || currentGlobalLidState != parsedStatus.lidOpen) {
                        currentGlobalLidState = parsedStatus.lidOpen
                        listener.onLidStateChanged(parsedStatus.lidOpen)
                        Log.d(TAG, "Lid state ${if (parsedStatus.lidOpen) "opened" else "closed"} (detected from new device)")
                    }

                    // Report the first status too, so battery shows up without waiting for a change.
                    listener.onDeviceStatusChanged(parsedStatus, null)
                } else {
                    // Ignore lastSeen: it changes on every advertisement, which made this fire
                    // (and redraw widgets and notifications) for every scan result.
                    if (parsedStatus.copy(lastSeen = 0L) != previousStatus.copy(lastSeen = 0L)) {
                        listener.onDeviceStatusChanged(parsedStatus, previousStatus)
                    }

                    if (parsedStatus.lidOpen != previousStatus.lidOpen) {
                        val previousGlobalState = currentGlobalLidState
                        currentGlobalLidState = parsedStatus.lidOpen

                        if (previousGlobalState != parsedStatus.lidOpen) {
                            listener.onLidStateChanged(parsedStatus.lidOpen)
                            Log.d(TAG, "Lid state changed from $previousGlobalState to ${parsedStatus.lidOpen}")
                        }
                    }

                    if (parsedStatus.isLeftInEar != previousStatus.isLeftInEar ||
                        parsedStatus.isRightInEar != previousStatus.isRightInEar) {
                        listener.onEarStateChanged(
                            parsedStatus,
                            parsedStatus.isLeftInEar,
                            parsedStatus.isRightInEar
                        )
                        Log.d(TAG, "Ear state changed - Left: ${parsedStatus.isLeftInEar}, Right: ${parsedStatus.isRightInEar}")
                    }

                    if (parsedStatus.leftBattery != previousStatus.leftBattery ||
                        parsedStatus.rightBattery != previousStatus.rightBattery ||
                        parsedStatus.caseBattery != previousStatus.caseBattery) {
                        listener.onBatteryChanged(parsedStatus)
                        Log.d(TAG, "Battery changed - Left: ${parsedStatus.leftBattery}, Right: ${parsedStatus.rightBattery}, Case: ${parsedStatus.caseBattery}")
                    }
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Error processing scan result", t)
        }
    }

    private fun parseProximityMessageWithDecryption(address: String, data: ByteArray, decrypted: ByteArray): AirPodsStatus {
        val paired = data[2].toInt() == 1
        val modelId = ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)
        val model = modelNames[modelId] ?: "Unknown ($modelId)"

        val status = data[5].toInt() and 0xFF
//        val flagsCase = data[7].toInt() and 0xFF
        val lid = data[8].toInt() and 0xFF
        val color = colorNames[data[9].toInt()] ?: "Unknown"
        val conn = connStates[data[10].toInt()] ?: "Unknown (${data[10].toInt()})"

        val primaryLeft = ((status shr 5) and 0x01) == 1
        val thisInCase = ((status shr 6) and 0x01) == 1
        val xorFactor = primaryLeft xor thisInCase

        val isLeftInEar = if (xorFactor) (status and 0x08) != 0 else (status and 0x02) != 0
        val isRightInEar = if (xorFactor) (status and 0x02) != 0 else (status and 0x08) != 0

        val isFlipped = !primaryLeft

        val leftByteIndex = if (isFlipped) 2 else 1
        val rightByteIndex = if (isFlipped) 1 else 2

        val (isLeftCharging, leftBattery) = formatBattery(decrypted[leftByteIndex].toInt() and 0xFF)
        val (isRightCharging, rightBattery) = formatBattery(decrypted[rightByteIndex].toInt() and 0xFF)

        val rawCaseBatteryByte = decrypted[3].toInt() and 0xFF
        val (isCaseCharging, rawCaseBattery) = formatBattery(rawCaseBatteryByte)

        val caseBattery = if (rawCaseBatteryByte == 0xFF || (isCaseCharging && rawCaseBattery == 127)) {
            lastValidCaseBatteryMap[address]
        } else {
            lastValidCaseBatteryMap[address] = rawCaseBattery
            rawCaseBattery
        }

        val lidOpen = ((lid shr 3) and 0x01) == 0

        return AirPodsStatus(
            address = address,
            lastSeen = System.currentTimeMillis(),
            paired = paired,
            model = model,
            leftBattery = leftBattery,
            rightBattery = rightBattery,
            caseBattery = caseBattery,
            isLeftInEar = isLeftInEar,
            isRightInEar = isRightInEar,
            isLeftCharging = isLeftCharging,
            isRightCharging = isRightCharging,
            isCaseCharging = isCaseCharging,
            lidOpen = lidOpen,
            color = color,
            connectionState = conn
        )
    }

    private fun cleanupStaleDevices() {
        val now = System.currentTimeMillis()
        val staleCutoff = now - currentProfile.staleDeviceTimeoutMs
        val hadDevices = deviceStatusMap.isNotEmpty()

        val staleDevices = deviceStatusMap.filter { it.value.lastSeen < staleCutoff }

        for (device in staleDevices) {
            deviceStatusMap.remove(device.key)
            Log.d(TAG, "Removed stale device from tracking: ${device.key}")
        }

        if (hadDevices && deviceStatusMap.isEmpty()) {
            airPodsStatusListener?.onDeviceDisappeared()
        }
    }

    private fun checkLidStateTimeout() {
        val currentTime = System.currentTimeMillis()
        val lidCloseTimeoutMs = currentProfile.lidCloseTimeoutMs
        if (currentTime - lastBroadcastTime > lidCloseTimeoutMs && currentGlobalLidState == true) {
            Log.d(TAG, "No broadcasts for ${lidCloseTimeoutMs}ms, forcing lid state to closed")
            currentGlobalLidState = false
            airPodsStatusListener?.onLidStateChanged(false)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun getIrkFromPreferences(): ByteArray? {
        val irkBase64 = sharedPreferences.getString(AacpIrkKey, null)
        if (irkBase64 != cachedIrkBase64) {
            cachedIrkBase64 = irkBase64
            // A new key may match addresses we rejected before.
            rejectedAddresses.clear()
            cachedIrk = irkBase64?.let {
                try {
                    Base64.decode(it)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode IRK", e)
                    null
                }
            }
        }
        return cachedIrk
    }

    private fun parseProximityMessage(address: String, data: ByteArray): AirPodsStatus {
        val paired = data[2].toInt() == 1
        val modelId = ((data[3].toInt() and 0xFF) shl 8) or (data[4].toInt() and 0xFF)
        val model = modelNames[modelId] ?: "Unknown ($modelId)"

        val status = data[5].toInt() and 0xFF
        val podsBattery = data[6].toInt() and 0xFF
        val flagsCase = data[7].toInt() and 0xFF
        val lid = data[8].toInt() and 0xFF
        val color = colorNames[data[9].toInt()] ?: "Unknown"
        val conn = connStates[data[10].toInt()] ?: "Unknown (${data[10].toInt()})"

        val primaryLeft = ((status shr 5) and 0x01) == 1
        val thisInCase = ((status shr 6) and 0x01) == 1
        val xorFactor = primaryLeft xor thisInCase

        val isLeftInEar = if (xorFactor) (status and 0x08) != 0 else (status and 0x02) != 0
        val isRightInEar = if (xorFactor) (status and 0x02) != 0 else (status and 0x08) != 0

        val isFlipped = !primaryLeft

        val leftBatteryNibble = if (isFlipped) (podsBattery shr 4) and 0x0F else podsBattery and 0x0F
        val rightBatteryNibble = if (isFlipped) podsBattery and 0x0F else (podsBattery shr 4) and 0x0F

        val caseBattery = flagsCase and 0x0F
        val flags = (flagsCase shr 4) and 0x0F

        val isLeftCharging = if (isFlipped) (flags and 0x02) != 0 else (flags and 0x01) != 0
        val isRightCharging = if (isFlipped) (flags and 0x01) != 0 else (flags and 0x02) != 0
        val isCaseCharging = (flags and 0x04) != 0

        val lidOpen = ((lid shr 3) and 0x01) == 0

        fun decodeBattery(n: Int): Int? = when (n) {
            in 0x0..0x9 -> n * 10
            in 0xA..0xE -> 100
            0xF -> null
            else -> null
        }

        return AirPodsStatus(
            address = address,
            lastSeen = System.currentTimeMillis(),
            paired = paired,
            model = model,
            leftBattery = decodeBattery(leftBatteryNibble),
            rightBattery = decodeBattery(rightBatteryNibble),
            caseBattery = decodeBattery(caseBattery),
            isLeftInEar = isLeftInEar,
            isRightInEar = isRightInEar,
            isLeftCharging = isLeftCharging,
            isRightCharging = isRightCharging,
            isCaseCharging = isCaseCharging,
            lidOpen = lidOpen,
            color = color,
            connectionState = conn
        )
    }

    companion object {
        private const val TAG = "AirPodsBLE"
        private val AacpIrkKey = AACPManager.Companion.ProximityKeyType.IRK.name
        private const val CLEANUP_INTERVAL_MS = 10000L
        private const val PROFILE_CHANGE_DEBOUNCE_MS = 1_000L
        private const val SCREEN_OFF_DEBOUNCE_MS = 15_000L
        private const val EAR_STATE_DEBOUNCE_MS = 10_000L
        private const val SCAN_RETRY_DELAY_MS = 30_000L
        private const val SCAN_START_WINDOW_MS = 30_000L
        private const val MAX_SCAN_STARTS_PER_WINDOW = 4
        private const val MAX_REJECTED_ADDRESSES = 256
    }
}
