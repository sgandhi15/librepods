# Native emulator screenshots

Open `index.html` to browse the PNG captures. Unlike `docs/ui-preview`, this page does not render or reconstruct any app screens. Every image is a framebuffer PNG returned by `adb exec-out screencap -p`.

The captures use a Pixel 7 Android 16 emulator, 1080 × 2400 pixels. AirPods-dependent screens use the app's existing `[DEMO] AirPods Pro` dataset. Battery, serial, firmware and connection values in those screens are demo data, not readings from physical AirPods. Charts have no real sensor input.

Variants are `apple-dark` and `apple-light`. Onboarding and release notes use the same Apple styling as the rest of the app. The control-center activity, widgets and notification chrome follow their own native styling.

`manifest.json` records each image's destination, theme, scroll position, source commit, timestamp, dimensions and SHA-256 checksum. `catalog.js` is the same metadata packaged for offline browser use. The original PNGs are not cropped, stitched or retouched. Numbered images are consecutive scroll positions, not different screens.

## How the app is opened

`NativeScreenshotActivity` lives under `android/app/src/debug/`, so release APKs do not contain it. It binds the existing app service, activates the existing demo mode and uses the real `NavigationRoot` and `AppNavGraph`. A nullable initial destination was added to `NavigationRoot`; normal app launches keep their existing behavior.

The debug entry point can also open the original connection popup, Dynamic Island and control-center classes. Widget screenshots use a real Android `AppWidgetHost` bound to the app's widget providers. Those are actual native widget layouts, not Compose or HTML imitations. Demo battery values are supplied to the existing service update methods.

Capture tooling grants runtime permissions, overlay access and widget-host binding only on the dedicated emulator. It rejects physical-device serials. It does not install Xposed, connect to real AirPods or perform purchases. FOSS premium access is enabled in the emulator's own local preferences to expose controls for review.

The purchase destination is captured with that local premium flag disabled, since the real app closes the purchase page for premium users. Recapturing a screen archives the recorder's previous images under the local diagnostic directory. Images modified outside the recorder are not overwritten.

Disabled/commented-out camera code has no runnable screen to capture. Linux Qt and the separate Linux Rust branch are not Android apps and are not included in this emulator set. Store billing, email, external websites and hardware events are not fabricated. UI states that cannot be reached in this environment should remain gaps, not replacement HTML images.

The notification capture shows the real background-service notification. The connected battery notification and its reconnect action check for a live AACP Bluetooth socket, which this emulator does not have. Those hardware-gated notification states are not captured. The Quick Settings tile likewise correctly shows Disconnected; the separate native control-center dialog is captured with demo data.

## Reproduce

SDK and AVD are reusable local setup:

- SDK: `~/Android/Sdk`
- AVD: `~/.config/.android/avd/LibrePods_Design_API36.avd`
- Emulator serial: `emulator-5582`
- Diagnostic logs and UI hierarchies: `~/.local/state/librepods-design/capture-logs/`

Start the existing AVD:

```sh
ANDROID_AVD_HOME="$HOME/.config/.android/avd" emulator \
  -avd LibrePods_Design_API36 -port 5582 \
  -no-window -no-audio -no-boot-anim -no-snapshot \
  -gpu swiftshader -memory 3072 -cores 4
```

Build from `android/`:

```sh
./gradlew :app:assembleFossDebug \
  -Pandroid.injected.build.abi=x86_64 \
  '-Dorg.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g' \
  --max-workers=4 --console=plain
```

The checkout compiles against SDK 37. The SDK package is named `platforms;android-37.0` in the current Google repository, not `platforms;android-37`.

From the repository root, capture every destination and additional surfaces:

```sh
python docs/emulator-screenshots/capture.py \
  --apk android/app/build/intermediates/apk/foss/debug/app-foss-debug.apk \
  --extras
python docs/emulator-screenshots/verify.py
```

For a focused recapture:

```sh
python docs/emulator-screenshots/capture.py \
  --scenes home,settings,equalizer --variants apple-dark
```

AGP puts this ABI-injected, test-only debug APK under `intermediates/apk/`. The capture script installs with `-t` as required by Android.

The gallery works with `file://` URLs, or through the repository's local HTTP server at `/docs/emulator-screenshots/`.

For a focused native-surface recapture:

```sh
python docs/emulator-screenshots/capture.py \
  --surfaces quick-settings,battery-widget,notification --variants apple-dark
```

Coverage is by navigation destination and distinct app-owned surface, not every combination of device model, firmware, runtime values and entitlement state. The repeated open-source dependency list is sampled at two scroll positions; the native license dialog is captured separately. Disabled camera code and external system/store pages are not runnable app destinations in this checkout.
