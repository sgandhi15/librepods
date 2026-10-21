"""Check native image integrity and current Android navigation coverage."""
import hashlib
import json
import re
import struct
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
data = json.loads((HERE / "manifest.json").read_text())
assert data["captures"], "No native captures recorded"
source = (ROOT / "android/app/src/main/java/me/kavishdevar/librepods/presentation/navigation/Screen.kt").read_text()
source = re.sub(r"//[^\n]*", "", source)
routes = set(re.findall(r"data (?:object|class) (\w+)", source))
covered = {capture["route"] for capture in data["captures"] if capture["route"]}
assert routes <= covered, f"Missing native routes: {sorted(routes - covered)}"
files = set()
native_routes_by_image = {}
titles = {
    "home": "[DEMO] AirPods Pro", "disconnected": "LibrePods", "rename": "Name",
    "settings": "Settings", "troubleshooting": "Troubleshooting", "head-tracking": "Head Tracking",
    "accessibility": "Accessibility", "transparency": "Customize Transparency Mode",
    "hearing-aid": "Hearing Aid", "hearing-adjustments": "Adjustments",
    "adaptive": "Customize Adaptive Audio", "licenses": "Open Source Licenses",
    "hearing-test": "Update Hearing Test Result", "version": "Version",
    "hearing-protection": "Hearing Protection", "purchase": "Unlock advanced features",
    "equalizer": "Equalizer", "long-press-left": "Left", "long-press-right": "Right",
    "call-mute": "Mute/Unmute", "call-hangup": "Hang Up", "microphone": "Microphone Mode",
    "hearing-confirmation": "Enable Hearing Aid", "contact-sheet": "Describe your issue",
    "delete-all-logs-dialog": "Delete All Logs", "delete-log-dialog": "Delete Log File",
    "troubleshooting-collecting": "Stop Collection", "troubleshooting-complete": "Done",
    "saved-log-sheet": "Share",
}
for capture in data["captures"]:
    assert capture["file"] not in files, f"Duplicate image entry: {capture['file']}"
    files.add(capture["file"])
    png = (HERE / capture["file"]).read_bytes()
    assert png[:8] == b"\x89PNG\r\n\x1a\n", capture["file"]
    assert struct.unpack(">II", png[16:24]) == (capture["width"], capture["height"]), capture["file"]
    assert hashlib.sha256(png).hexdigest() == capture["sha256"], capture["file"]
    assert capture["width"] == 1080 and capture["height"] == 2400, capture["file"]
    expected = titles.get(capture["scene"])
    if expected:
        assert expected in "\n".join(capture.get("nativeText", [])), f"Expected native destination {expected!r} was not visible: {capture['file']}"
    if capture["scene"] in {"battery-widget", "battery-widget-phone"}:
        for level in ["80%", "18%", "76%"]:
            assert level in "\n".join(capture.get("nativeText", [])), f"Native widget was not populated: {capture['file']}"
    if capture["scene"] == "quick-settings-expanded":
        for mode in ["Transparency", "Adaptive"]:
            assert mode in "\n".join(capture.get("nativeText", [])), f"Native mode selector was not expanded: {capture['file']}"
    # Distinct navigation pages must not all be screenshots of the same loading frame.
    if capture["position"] == 1 and capture["route"] and capture["scene"] not in {"disconnected", "release-notes"}:
        key = (capture["variant"], capture["sha256"])
        previous_route = native_routes_by_image.get(key)
        assert previous_route in {None, capture["route"]}, f"Identical framebuffer for distinct routes: {previous_route}, {capture['route']}"
        native_routes_by_image[key] = capture["route"]
print(f"PASS: {len(files)} original emulator PNGs, checksums and dimensions verified; all {len(routes)} Android routes covered.")
