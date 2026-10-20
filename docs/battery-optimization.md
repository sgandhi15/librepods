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
| Screen on, not connected | Full speed | The lid-open popup must be fast |

When the profile switches:
- Switching to full speed happens immediately when the screen turns on.
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

## Review findings

In October 2026, 12 independent reviews checked this work: Claude Opus 5.5 and Fable 5.1, Codex GPT-6-Astra and GPT-6.1-Sol, and 8 free OpenCode models. Nemotron 3 Ultra failed to run. Claude Opus and GPT-6.1-Sol then ran two verification rounds on the fixes. Every confirmed issue above is fixed.

Claims checked and rejected:
- **"`RECEIVER_NOT_EXPORTED` blocks the Bluetooth and screen broadcasts."** No. They come from system_server, which can deliver to non-exported receivers.
- **"The 10 s cleanup timer wakes the phone thousands of times a day."** No. Handler timers don't wake a sleeping device.
- **"Stop scanning entirely with the screen off while connected."** Not done. The incoming-call takeover reads BLE in-ear state when the phone rings.
- **"No scan without an IRK breaks BLE-only mode."** No. Results were already discarded without an IRK, and `bleOnlyMode` can't currently be turned on anywhere in the app.
- **"Slow scan while both AirPods are in the ears."** Tried, then removed. You open the case while still wearing them to put them away, so it made that popup 5–10 s slower.

## Ideas for later

Each of these saves more battery but costs something, or is a separate bug.

1. **Full-speed scanning while the screen is on and the AirPods aren't around.** This is now the largest remaining scan cost (for example, AirPods at home, or connected to a Mac).
   - Best version (suggested by reviewers): scan fast for a while after the screen turns on or after your AirPods were last seen, otherwise use balanced.
   - *Cost:* the first popup after they come back can take about 3–4 s.
2. **Low power instead of balanced while connected with the screen on.** Almost nothing uses BLE then except the lid popup.
   - *Cost:* the popup when putting the AirPods away is about 5 s slower.
3. **Respect the "take over when disconnected" setting in the BLE takeover path.**
   - Today `takeOverFromBle()` ignores it. The setting is off by default, and the old code ignored it too.
   - *Cost:* users on default settings would stop getting this automatic connect.
4. **Skip `updateBattery()` when a battery packet from the socket has the same values as before.** It does metadata writes, a widget update and broadcasts each time.
   - Check first that the popup and island don't rely on a `BATTERY_DATA` broadcast for every packet.
5. **`connectAudio`/`disconnectAudio` runs on every battery packet,** binding a profile proxy and using reflection each time. Only call them when the charging state changes.
6. **Stop running a permanent foreground service.** Android's Companion Device Manager (`startObservingDevicePresence` / `CompanionDeviceService`) could wake the app only when the AirPods are around.
   - *Cost:* a large rewrite and an association step. It's unclear whether it works with AirPods' rotating addresses, so prototype it first.

Bugs that already existed, found during review and not fixed here:
- `disconnectAirPods()` clears the socket before calling `updateNotificationContent(false)`, so the "connected" notification isn't cancelled on a manual disconnect.
- If the lid opens while the screen is off, the low-power scan can fire the popup in the dark, where it auto-closes before anyone sees it.
- With the vendor-ID hook enabled, `connectToSocket` calls `attManager.startReader()` and `readCharacteristic()` before `BluetoothConnectionManager.attSocket` is assigned. On a fresh connection the reader stops at once and the reads return null.
- After Bluetooth is turned back on with the case open, the popup shows again, because tracked lid state is reset when scanning stops.

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
