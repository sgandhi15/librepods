# Battery optimization notes

Work on branch `perf/battery-optimization`. This file tracks what was changed and the ideas that were left for later.

## Done

All of these keep existing features working. Some things become a few seconds slower in situations where nobody is looking at the screen.

**Bluetooth scanning (`BLEManager.kt`)**

The app used to scan at full power (`SCAN_MODE_LOW_LATENCY`) all the time. It now picks a level based on what's happening:

| Situation | Scan mode | Why it's enough |
|---|---|---|
| No IRK saved yet (AirPods never connected) | No scan | Every result is thrown away without the IRK anyway |
| Screen off | Low power | The popup can't be seen |
| AirPods connected, both in ears | Low power | The case can't be opened to take them out |
| AirPods connected | Balanced | Battery and state come over the direct connection |
| Otherwise (screen on, not connected) | Full speed | The lid-open popup must be fast |

How the switching works:
- Switching to full speed happens immediately when the screen turns on.
- Other switches wait a bit: 15 s after screen off, 10 s after an in-ear change. This stays within Android's limit of 5 scan starts per 30 s. Past that limit, Android silently stops delivering results.
- Scanning restarts by itself when Bluetooth is turned back on.

Other scan fixes:
- The "status changed" check no longer compares the timestamp. Before, it fired for every scan result and redrew the widget, notification and Bluetooth metadata about twice a second.
- The decoded IRK and AES cipher are cached. Addresses that fail verification (other people's AirPods) are remembered, so their signals aren't re-checked.

**Service (`AirPodsService.kt`)**
- **Notification:** updated only when its content changes (battery, name, reverse button). It's still re-posted once a minute in case it was dismissed. Before, it was rebuilt for every packet, which is many per second during head tracking.
- **Per-packet broadcast:** the `AIRPODS_DATA` broadcast sent for every packet was removed. Nothing listened to it.
- **Packet log:** written to disk at most every 10 s instead of after every packet.
- **Takeover:** the BLE "nobody is connected, take over" path connects in the background, one attempt at a time, at most every 15 s. Before, it blocked the main thread.

## Ideas for later

Each of these saves more battery but costs something.

1. **Slow down scanning when the AirPods haven't been seen for a while, even with the screen on.**
   For example, drop to balanced or low power after about 10 minutes with no advertisements from your AirPods: you're out and they're at home.
   *Cost:* the first popup after they come back could take about 3–4 s instead of under 1 s.

2. **Use longer scan gaps with the screen off.**
   For example, use opportunistic scanning (only results other apps' scans already produce), or a low-power scan with a longer report delay.
   *Cost:* slower auto-connect and slower detection for incoming-call takeover while the screen is dark. Ringing usually turns the screen on, which already switches back to full speed.

3. **Stop running a permanent foreground service.**
   Today the service starts at boot and runs forever. Android's Companion Device Manager (`startObservingDevicePresence` / `CompanionDeviceService`) can wake the app only when the AirPods are around.
   *Cost:* a large rewrite and a pairing/association step. It's unclear whether it works reliably with AirPods, because their BLE address keeps changing. Prototype before committing to it.

## Measuring

Battery stats work on the release build; no debug build is needed.

```sh
adb shell dumpsys batterystats --reset        # start a clean measurement
# ... use the phone normally for a few hours or a day ...
adb shell dumpsys batterystats me.kavishdevar.librepods > batterystats.txt
adb bugreport bugreport.zip                    # optional: load into Battery Historian for graphs
```

Things to look at for `me.kavishdevar.librepods`:
- "Bluetooth scan" time, especially in the background
- CPU time
- wakelocks
- the estimated power use

Compare a run with the old build against a run with this branch.
