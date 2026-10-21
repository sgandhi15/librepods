# LibrePods screen gallery

Open `index.html` in a browser. It works offline, including with a `file://` URL. Keep this directory inside the repository, since artwork and original screenshots use relative paths. There are no dependencies, build steps or CDN requests.

Alternatively, serve the repository root:

```sh
python -m http.server 8765 --bind 127.0.0.1
```

Then visit <http://127.0.0.1:8765/docs/ui-preview/>.

## What is included

- All active Android navigation routes in `Screen.kt`, including both press-and-hold sides and call actions.
- All four onboarding pages, including the unsupported-device page.
- Connected, free-tier, disconnected, reconnecting, remote-audio and loading examples. Separate battery and Play migration notice variants.
- The release-notes page, with one card per update.
- Troubleshooting with and without saved logs, all five collection steps, log sheet and deletion confirmations.
- Hearing Aid confirmation, contact sheet, control-center states, connection popup and each Dynamic Island event type.
- Battery and listening-mode widgets, notification layouts and a representative reconnect action and Quick Settings tile.
- The Linux Qt main window, adaptive and disconnected states, complete settings, QR dialog and error state, and tray menu.
- Disabled camera code, explicitly separated from active screens.
- All 15 Android screenshots and the Linux screenshot already in the repository, in the References section.

This is a source-based design inventory, not an emulator or a new design. The HTML preserves the existing control order, labels and grouping where practical. Fonts, icons, native sliders, glass effects, chart gestures, animations and platform chrome are approximate. The app now has only the Apple look, so every Android preview is drawn in that style.

Conditional controls are shown together so they are not missed. No single AirPods model necessarily supports every control in the main preview. Some variants depend on premium access, firmware, permissions or vendor-ID spoofing. This gallery is not an exhaustive set of every combination of device capability and runtime value.

The Linux Rust rewrite is on another branch and is **not included**. Android system permission dialogs, keyboard, file picker, share sheet, external email/GitHub/store pages and OS-rendered notification chrome are not app-owned screens and are not recreated. Dynamic content such as firmware, build information, device details, logs, prices and battery values is sample data. No device keys are included; the QR area is a labeled placeholder.

## Review and edit

1. Filter by platform or section, or search for a setting. Use **Open** for a larger preview with source links.
2. Try toggles, selection rows and sliders. Links between app pages open the destination in the detail view. No changes reach the real app or system.
3. Switch dark/light mode. Original screenshots do not change.
4. Enable **Full page height** to see settings below the fold, or scroll inside a preview.
5. Add notes to a screen in its detail view. Notes save in browser storage. Export them before clearing browser data or moving between `file://` and HTTP, which have separate storage.

To start changing the HTML:

- `styles.css`: shared app tokens begin at `.device`. Change spacing, colors, typography, corner radius and component rules here.
- `screens.js`: screen definitions, shared content helpers, source mapping and sample data.
- `app.js`: gallery filters, detail view, notes and browser-only interactions.
- `index.html`: gallery page itself.

The app's Kotlin, QML and Bluetooth code are untouched. Editing this gallery does not modify the actual application.

## Refresh source text and verify coverage

`source-data.js` contains generated English strings, full privacy text, dependencies, route names and the source commit. After updating the app source:

```sh
python docs/ui-preview/generate-data.py
node docs/ui-preview/verify.cjs
```

The coverage check compares the gallery against the current source, not just the generated snapshot. New routes, screen files or XML layouts fail the check until a preview or explicit exclusion is added. Generated source text is committed with the gallery so opening the page never requires running Python.
