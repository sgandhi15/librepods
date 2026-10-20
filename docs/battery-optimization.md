# Battery optimization notes

Work on branch `perf/battery-optimization`. This file tracks what was changed, what reviewers found, and the ideas left for later.

## Done

These keep existing features working. The trade-off is that some things are slower while the screen is off.

### Bluetooth scanning (`BLEManager.kt`)

The app used to scan at full power (`SCAN_MODE_LOW_LATENCY`) all the time. It now picks a level from the phone's state:

| Situation | Scan mode | Why it's enough |
|---|---|---|
| No IRK saved yet (AirPods never connected) | No scan | Every result is thrown away without the IRK anyway |
| Screen off | Low power | The popup can't be seen |
| Screen on, AirPods connected | Balanced | Battery and state come over the direct connection; the popup is still caught within a few seconds |
| Screen on, not connected, within 1 min of the screen turning on or 5 min of last hearing your AirPods | Full speed | The lid-open popup must be fast when the AirPods are likely to be picked up |
| Screen on, not connected, otherwise | Balanced | Your AirPods probably aren't around, for example left at home. Balanced still hears them within a few seconds, and hearing them switches straight back to full speed |

AirPods in use with another device nearby, such as a Mac on the same desk, keep advertising, so scanning stays at full speed; the saving applies when they're out of range.

The trade-off: if the screen has been on for over a minute and your AirPods haven't been heard for 5 minutes (for example, in a closed case in a pocket), the first popup can take about 3–4 s instead of under 1 s.

When the profile switches:
- Switching to full speed happens immediately when the screen turns on, or when your AirPods are heard while scanning at balanced.
- Other switches wait first: 15 s after the screen turns off, 1 s for other events.
- A limiter allows at most 4 scan starts per 30 s. Android fails scans (`SCAN_FAILED_SCANNING_TOO_FREQUENTLY`) for apps that start more than 5.

Restarts and retries:
- After a failure the scan is retried: after 30 s for "too frequently", and for other errors after 5 s, backing off to at most 5 min while failures keep happening. It is not retried when the feature is unsupported or the permission is missing.
- It restarts when Bluetooth turns back on.

Device tracking:
- In low-power mode, devices are kept for 90 s, because the incoming-call takeover reads the last known in-ear state when the phone rings.
- Batch results are sorted by time and all of them are processed, so short changes like lid open → close aren't lost.
- Takeover decisions use only the newest status from each batch.
- Repeats of an identical packet skip decrypting and parsing.

The "status changed" check no longer compares the timestamp. Before, while the AirPods weren't connected, it fired for every scan result and redrew the widget, notification and Bluetooth metadata. The first status from a new address is still reported, and after the direct connection drops, the next BLE status is reported even if unchanged, so the widget gets fresh battery data.

Caches:
- the decoded IRK and AES cipher;
- addresses that failed verification (other people's AirPods), so they aren't re-checked;
- verified addresses.

The address caches are size-limited and are cleared when the IRK changes.

### Service (`AirPodsService.kt`)

**Notification**
- Updated only when its content changes: battery, name, or the reverse button.
- While packets arrive it's also re-posted at most once a minute, which brings it back if it was dismissed. It's not on a timer, because a timer would cost battery.
- Updates are synchronized; they come from the main thread and from the socket thread.
- Before, it was rebuilt for every packet, which is many per second during head tracking.

**Per-packet work**
- The per-packet `AIRPODS_DATA` broadcast was removed. Nothing listened to it.
- The packet log is written to disk at most every 10 s instead of after every packet.
- Packet hex is formatted once per packet instead of twice.

**Connecting**
- Only one socket connection attempt runs at a time, whichever path starts it. Before, the BLE takeover, the ACL/A2DP receivers and `takeOver()` could connect in parallel and fight over the socket. A request that arrives during an attempt is queued and run afterwards on a background thread if that attempt didn't connect, so the main thread is never blocked by a queued attempt.
- Sockets from failed attempts are closed, both AACP and ATT; they used to leak.
- When the reader ends, its dead socket and the ATT socket of the same connection are closed, so nothing keeps treating the link as up. That disconnect is tagged with its socket, so if a new connection is already up when it's handled, it's ignored instead of tearing the new one down.
- The BLE "nobody is connected, take over" path runs in the background instead of blocking the main thread. Failed attempts back off from 15 s up to 5 min. The back-off resets after any successful connection, when the lid opens, when the in-ear state changes, or when the AirPods report a state other than "Disconnected". Attempts always stay at least 15 s apart.

**Battery updates**
- A BLE battery change no longer runs the battery/widget/metadata update twice.
- A battery packet from the AirPods only updates metadata, the widget, the system battery broadcast and the notification when the values changed. The in-app `BATTERY_DATA` broadcast still goes out for every packet.
- Audio is only connected or disconnected when "both pods charging" changes, instead of on every battery packet. Each call bound a profile proxy, and repeating it also undid deliberate audio disconnects, such as "disconnect when not wearing". Audio is also re-allowed explicitly when ownership comes back from another device and on "Reconnect" after a reverse, which the per-packet calls used to cover.
- If HFP reconnects while the socket stays up, the battery is republished, because Android clears a headset's battery when HFP disconnects.

**Other fixes**
- If the lid opens while the popup can't be seen (screen off, or on the lock screen, which covers overlays), the popup is shown once the phone is on and unlocked, if that happens within a minute. Before, it was shown where nobody could see it, and since only one popup is shown per connection, you didn't get one at all.
- A manual disconnect now cancels the "connected" notification and closes the ATT socket.
- On start, the service only connects to the saved AirPods that are actually connected. Before, if any audio device was connected, it tried (and paged) every saved AirPods pair, and saved whichever came last as `mac_address`, which the BLE takeover then used. The connection queue keeps one request per device, so none is dropped. If the service starts after the AirPods' Bluetooth link is up but before audio connects, the audio-connected event now starts the connection; before, nothing did until the next reconnect.
- With the vendor-ID hook, the ATT socket is now set before its reader starts. Before, the reader stopped at once on a new connection and the first reads failed.

## Review findings

In October 2026, 12 independent reviews checked this work: Claude Opus 5.5 and Fable 5.1, Codex GPT-6-Astra and GPT-6.1-Sol, and 8 free OpenCode models. Nemotron 3 Ultra failed to run. Claude Opus and GPT-6.1-Sol then ran two verification rounds on the fixes, and a final full review by Claude Fable 5.1 found it ready to merge, with only small cleanups, which are done. Every confirmed issue above is fixed.

Claims checked and rejected:
- **"`RECEIVER_NOT_EXPORTED` blocks the Bluetooth and screen broadcasts."** No. They come from system_server, which can deliver to non-exported receivers.
- **"The 10 s cleanup timer wakes the phone thousands of times a day."** No. Handler timers don't wake a sleeping device.
- **"Stop scanning entirely with the screen off while connected."** Not done. The incoming-call takeover reads BLE in-ear state when the phone rings.
- **"No scan without an IRK breaks BLE-only mode."** No. Results were already discarded without an IRK, and `bleOnlyMode` can't currently be turned on anywhere in the app.
- **"Slow scan while both AirPods are in the ears."** Tried, then removed. You open the case while still wearing them to put them away, so it made that popup 5–10 s slower.

## Ideas for later

Each of these saves more battery but costs a feature, so they were left out.

1. **Low power instead of balanced while connected with the screen on.** Almost nothing uses BLE then except the lid popup.
   - *Cost:* the popup when putting the AirPods away is about 5 s slower.
2. **Respect the "take over when disconnected" setting in the BLE takeover path.**
   - `takeOverFromBle()` ignores it, as the old code did. The setting is off by default, is premium-only, and today controls the music/call takeover (`takeOver()`).
   - *Cost:* free users would lose the automatic connect with no way to turn it back on.
3. **Stop running a permanent foreground service.** Android's Companion Device Manager (`startObservingDevicePresence` / `CompanionDeviceService`) could wake the app only when the AirPods are around.
   - *Cost:* a large rewrite and an association step. It's unclear whether it works with AirPods' rotating addresses, so prototype it first.

Bugs that already existed and aren't fixed here:
- "Reconnect" after a reverse can call `connectAudio` twice: once inside `takeOver()` and once explicitly. That's harmless, apart from an extra profile bind.
- `withTimeout(5000)` inside `runBlocking` in `connectToSocketLocked` can't fire, because the block never suspends, so a slow `connect()` isn't cut off at 5 s. Making it work means aborting `connect()` (closing the socket from a timer), which could cut off slow but successful connections, so it's left as is.

Side effects of this branch:
- After Bluetooth is turned back on with the case open, the popup shows again, because tracked lid state is reset when scanning stops. On `main` the scan never restarted after a Bluetooth toggle at all, so BLE features stopped working until the service restarted.

## Baseline (release 1.0.4, before this branch)

Measured on a Galaxy S25 (SM-S931W, Android 17) on 2026-10-19: 5 h 52 min on battery since the last charge, 1285 mAh used by the whole phone.

| What | LibrePods |
|---|---|
| Estimated power use | 9.96 mAh (0.8% of the drain, 14th of all apps) |
| Bluetooth power blamed | 5.34 mAh |
| CPU | 0.24 mAh (about 6 s of CPU time) |
| BLE scan running | The whole 5 h 52 min, at full speed |
| Scan results delivered | 5500 |
| Notification posts (wakelocks) | 273 |

Notes:
- Android splits Bluetooth scan blame between every app scanning at the same time. Here that was Google Play services and a system service, which also scanned non-stop, plus some Samsung services part of the time.
- The phone's total Bluetooth drain was 31.5 mAh. The radio runs at the most aggressive mode any app asks for, so LibrePods' full-speed scan probably set the pace. Its real cost is somewhere between its blamed 5 mAh and most of that 31.5 mAh.

## Measuring

Battery stats work on the release build; no debug build is needed.

```sh
adb shell dumpsys batterystats --reset        # start a clean measurement
# ... use the phone normally for a few hours or a day ...
adb shell dumpsys batterystats me.kavishdevar.librepods > batterystats.txt
adb bugreport bugreport.zip                    # optional: load into Battery Historian for graphs
```

For `me.kavishdevar.librepods`, look at:
- "Bluetooth scan" time, especially in the background
- CPU time
- wakelocks
- the estimated power use

Compare a run of the old build with a run of this branch.

### Side-by-side test build

To test without replacing the official app, build the debug APK under another package name and app label, using a Gradle init script kept outside the repo:

```groovy
// separate-app.gradle
allprojects {
    pluginManager.withPlugin('com.android.application') {
        android.buildTypes.debug.applicationIdSuffix = '.battery'
        android.sourceSets.debug.res.srcDirs += '/path/to/res'  // values/strings.xml with app_name "LibrePods Battery"
    }
}
```

```sh
./gradlew --init-script separate-app.gradle :app:assembleFossDebug
adb install -r app/build/outputs/apk/foss/debug/app-foss-debug.apk
adb shell am force-stop me.kavishdevar.librepods   # only one app may talk to the AirPods
```

Things to watch:
- Don't open the official app while testing. Opening it restarts its service, and the two apps then fight over the AirPods.
- To go back, uninstall `me.kavishdevar.librepods.battery` and open the official app.

