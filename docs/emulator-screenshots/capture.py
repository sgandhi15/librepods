"""Capture unchanged native app screens using adb. Requires the debug screenshot activity."""
import argparse
import hashlib
import json
import re
import struct
import shutil
import subprocess
import time
import uuid
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
PACKAGE = "me.kavishdevar.librepods"
ACTIVITY = PACKAGE + "/.screenshots.NativeScreenshotActivity"
# Scene, navigation route, maximum additional scroll positions.
SCENES = [
    ("home", "AirPodsSettings", 6),
    ("disconnected", "AirPodsSettings", 0),
    ("rename", "Rename", 0),
    ("settings", "AppSettings", 7),
    ("troubleshooting", "Troubleshooting", 0),
    ("head-tracking", "HeadTracking", 1),
    ("accessibility", "Accessibility", 4),
    ("transparency", "TransparencyCustomization", 6),
    ("hearing-aid", "HearingAid", 1),
    ("hearing-adjustments", "HearingAidAdjustments", 2),
    ("adaptive", "AdaptiveStrength", 0),
    ("licenses", "OpenSourceLicenses", 1),
    ("hearing-test", "UpdateHearingTest", 2),
    ("version", "VersionInfo", 0),
    ("hearing-protection", "HearingProtection", 1),
    ("purchase", "Purchase", 4),
    ("equalizer", "Equalizer", 0),
    ("long-press-left", "LongPress", 1),
    ("long-press-right", "LongPress", 1),
    ("call-mute", "CallControl", 0),
    ("call-hangup", "CallControl", 0),
    ("microphone", "MicrophoneSettings", 0),
    ("loading", None, 0),
]


def write_atomic(path, text):
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_text(text)
    temporary.replace(path)


class Capture:
    def __init__(self, args):
        self.args = args
        if not args.serial.startswith("emulator-"):
            raise ValueError("Use an emulator serial, not a physical phone.")
        unsupported = [v for v in args.variants.split(",") if v.startswith("material")]
        if unsupported:
            raise ValueError(f"Unsupported variants: {', '.join(unsupported)}. Use apple-dark or apple-light.")
        self.adb = ["adb", "-s", args.serial]
        self.logs = Path.home() / ".local/state/librepods-design/capture-logs"
        self.logs.mkdir(parents=True, exist_ok=True)
        self.manifest_path = HERE / "manifest.json"
        self.data = json.loads(self.manifest_path.read_text()) if self.manifest_path.exists() else {"captures": []}
        self.variant = ""
        self.scene = ""
        self.route = None
        self.counter = 0
        self.surface = None
        self.reset_groups = set()
        self.commit = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()

    def cmd(self, *parts, binary=False, input=None):
        result = subprocess.run(self.adb + list(parts), capture_output=True, input=input, timeout=90)
        if result.returncode:
            raise RuntimeError(f"adb {' '.join(parts)}: {result.stderr.decode(errors='replace')}")
        return result.stdout if binary else result.stdout.decode(errors="replace").strip()

    def shell(self, *parts):
        return self.cmd("shell", *parts)

    def hierarchy(self):
        for _ in range(4):
            status = self.shell("uiautomator", "dump", "/sdcard/librepods-capture.xml")
            if "dumped to:" in status:
                text = self.cmd("exec-out", "cat", "/sdcard/librepods-capture.xml")
                try:
                    return text, ET.fromstring(text)
                except ET.ParseError:
                    pass
            time.sleep(0.8)
        raise RuntimeError(f"UIAutomator did not return a hierarchy: {status}")

    def signature(self, tree):
        return [(n.get("text"), n.get("content-desc"), n.get("bounds")) for n in tree.iter("node") if n.get("package") == PACKAGE]

    def swipe(self, horizontal=False):
        if horizontal:
            self.shell("input", "swipe", "930", "1200", "150", "1200", "450")
        else:
            self.shell("input", "swipe", "540", "1970", "540", "700", "450")
        time.sleep(0.4)

    def tap(self, text, scrolls=0, partial=False):
        for attempt in range(scrolls + 1):
            _, tree = self.hierarchy()
            matches = [n for n in tree.iter("node") if text in (n.get("text"), n.get("content-desc"))]
            if not matches and partial:
                matches = [n for n in tree.iter("node") if n.get("text", "").startswith(text)]
            if matches:
                parents = {child: parent for parent in tree.iter() for child in parent}
                actions = []
                for node in matches:
                    candidate = node
                    while candidate is not None:
                        if candidate.get("clickable") == "true":
                            actions.append(candidate)
                            break
                        candidate = parents.get(candidate)
                # Prefer a real action ancestor over a same-named toolbar title.
                def area(node):
                    b = list(map(int, re.findall(r"\d+", node.get("bounds"))))
                    return (b[2] - b[0]) * (b[3] - b[1])
                actions = [node for node in actions if area(node) > 0]
                chosen = min(actions, key=area) if actions else matches[-1]
                bounds = list(map(int, re.findall(r"\d+", chosen.get("bounds"))))
                self.shell("input", "tap", str((bounds[0] + bounds[2]) // 2), str((bounds[1] + bounds[3]) // 2))
                time.sleep(0.8)
                return
            if attempt < scrolls:
                self.swipe()
        raise RuntimeError(f"Cannot find native control: {text}")

    def launch(self, scene, variant, route=None, surface=None):
        self.variant, self.scene, self.route, self.counter = variant, scene, route, 0
        self.surface = surface
        self.shell("cmd", "statusbar", "collapse")
        self.shell("cmd", "uimode", "night", "yes" if variant.endswith("dark") else "no")
        token = uuid.uuid4().hex
        command = ["shell", "am", "start", "-W", "-S", "-n", ACTIVITY,
                   "--es", "screen", "home" if surface else scene,
                   "--es", "capture_token", token,
                   "--ez", "demo", str(scene != "disconnected").lower()]
        if surface:
            command += ["--es", "surface", surface]
        self.cmd(*command)
        deadline = time.monotonic() + 45
        while time.monotonic() < deadline:
            logs = self.cmd("logcat", "-d", "-s", "NativeScreenshot:I", "AndroidRuntime:E")
            if f"DRAWN {token}" in logs:
                time.sleep(0.8)
                return
            if "FATAL EXCEPTION" in logs and token in logs:
                raise RuntimeError(logs)
            time.sleep(0.4)
        (self.logs / "launch-failure.txt").write_text(self.cmd("logcat", "-d"))
        raise RuntimeError(f"Native activity did not become ready: {scene}. See {self.logs}.")

    def save(self, suffix=None, hierarchy=None, note=""):
        # Capture transient overlays before UIAutomator's idle wait; normal destinations
        # are captured after the hierarchy confirms that the native screen has settled.
        transient = self.surface and (self.surface.startswith("island-") or self.surface == "connection-popup")
        if transient:
            png = self.cmd("exec-out", "screencap", "-p", binary=True)
            xml, tree = hierarchy or self.hierarchy()
        else:
            xml, tree = hierarchy or self.hierarchy()
            png = self.cmd("exec-out", "screencap", "-p", binary=True)
        activity = self.shell("dumpsys", "activity", "activities")
        if "NativeScreenshotActivity" not in activity and "QuickSettingsDialogActivity" not in activity:
            raise RuntimeError("Native screenshot activity is no longer running.")
        if png[:8] != b"\x89PNG\r\n\x1a\n":
            raise RuntimeError("adb did not return a PNG framebuffer capture.")
        width, height = struct.unpack(">II", png[16:24])
        self.counter += 1
        scene = suffix or self.scene
        group = (self.variant, scene)
        if group not in self.reset_groups:
            old = [x for x in self.data["captures"] if (x["variant"], x["scene"]) == group]
            for entry in old:
                old_file = HERE / entry["file"]
                if old_file.exists():
                    if hashlib.sha256(old_file.read_bytes()).hexdigest() != entry["sha256"]:
                        raise RuntimeError(f"Refusing to replace an image changed outside the recorder: {old_file}")
                    archive = self.logs.parent / "replaced-captures" / str(time.time_ns()) / entry["file"]
                    archive.parent.mkdir(parents=True, exist_ok=True)
                    shutil.move(old_file, archive)
            self.data["captures"] = [x for x in self.data["captures"] if (x["variant"], x["scene"]) != group]
            self.reset_groups.add(group)
        relative = Path("captures") / self.variant / scene / f"{self.counter:02}.png"
        destination = HERE / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        if destination.exists():
            raise RuntimeError(f"Refusing to overwrite an untracked screenshot: {destination}")
        destination.write_bytes(png)
        (self.logs / f"{self.variant}-{scene}-{self.counter:02}.xml").write_text(xml)
        entry = {
            "file": relative.as_posix(), "scene": scene, "route": self.route,
            "variant": self.variant, "position": self.counter,
            "width": width, "height": height, "sha256": hashlib.sha256(png).hexdigest(),
            "capturedAt": datetime.now(timezone.utc).isoformat(), "sourceCommit": self.commit,
            "data": "disconnected emulator" if self.scene == "disconnected" else "built-in AirPods demo mode",
            "note": note,
            "nativeText": [n.get("text") for n in tree.iter("node") if n.get("text")],
        }
        self.data["captures"] = [x for x in self.data["captures"] if x["file"] != entry["file"]] + [entry]
        self.write_manifest()
        print(f"Captured {relative}", flush=True)
        return tree

    def write_manifest(self):
        for capture in self.data["captures"]:
            if "nativeText" not in capture:
                xml = self.logs / f"{capture['variant']}-{capture['scene']}-{capture['position']:02}.xml"
                if xml.exists():
                    tree = ET.fromstring(xml.read_text())
                    capture["nativeText"] = [n.get("text") for n in tree.iter("node") if n.get("text")]
            if capture["scene"] == "notification" and "Background Service Running" in capture.get("nativeText", []):
                capture["note"] = "Background-service notification. Battery and reconnect notifications require a connected AACP socket and are not shown in this emulator."
        self.data.update({"device": self.args.serial, "avd": "LibrePods_Design_API36", "api": self.shell("getprop", "ro.build.version.sdk"),
                          "method": "adb exec-out screencap -p; original emulator framebuffer, no HTML rendering or retouching"})
        write_atomic(self.manifest_path, json.dumps(self.data, indent=2) + "\n")
        write_atomic(HERE / "catalog.js", "window.NATIVE_CAPTURES = " + json.dumps(self.data) + ";\n")

    def pages(self, additional):
        previous = self.signature(self.save())
        for _ in range(additional):
            self.swipe()
            hierarchy = self.hierarchy()
            signature = self.signature(hierarchy[1])
            if signature == previous:
                break
            self.save(hierarchy=hierarchy)
            previous = signature

    def onboarding(self, variant):
        self.launch("onboarding", variant, "Onboarding")
        self.save("onboarding-welcome")
        self.tap("Continue")
        self.counter = 0
        self.scene = "onboarding-privacy"
        self.pages(10)
        self.tap("I agree")
        _, tree = self.hierarchy()
        if any(n.get("text") == "Not supported" for n in tree.iter("node")):
            self.counter = 0
            self.scene = "onboarding-unsupported"
            self.pages(3)
            self.tap("Bypass compatibility check", scrolls=3)
        self.counter = 0
        self.scene = "onboarding-permissions"
        self.pages(2)

    def extras(self, variant):
        self.launch("release-notes", variant, "ReleaseNotes")
        self.counter = 0
        self.scene = "release-notes"
        self.pages(4)
        self.launch("hearing-aid", variant, "HearingAid")
        self.tap("Hearing Aid")
        self.save("hearing-confirmation")
        self.launch("settings", variant, "AppSettings")
        self.tap("E-Mail", scrolls=7)
        self.save("contact-sheet")
        self.launch("troubleshooting", variant, "Troubleshooting")
        self.tap("Collect Logs")
        self.save("troubleshooting-steps")
        self.tap("Open Xposed Settings")
        time.sleep(2.5)
        self.counter = 0
        self.save("troubleshooting-disconnect")
        self.tap("Continue")
        self.counter = 0
        self.save("troubleshooting-collecting", note="Actual emulator log collection UI. The emulator has no Xposed/su; any logged errors are real emulator errors.")
        self.tap("Stop Collection")
        time.sleep(2)
        self.counter = 0
        self.save("troubleshooting-complete")
        self.tap("Done")
        self.counter = 0
        self.save("troubleshooting-saved-logs")
        _, tree = self.hierarchy()
        dates = [n.get("text") for n in tree.iter("node") if re.match(r"[A-Z][a-z]{2} \d{2}, \d{4}", n.get("text", ""))]
        if dates:
            self.tap(dates[0])
            self.counter = 0
            self.save("saved-log-sheet", note="Real log file generated in the screenshot emulator, not physical-AirPods diagnostics.")
            self.shell("input", "keyevent", "4")
            time.sleep(0.6)
            _, tree = self.hierarchy()
            if any(n.get("text") == "Delete All" for n in tree.iter("node")):
                self.tap("Delete All")
                self.counter = 0
                self.save("delete-all-logs-dialog")
                self.tap("Cancel")
            self.tap("Delete")
            self.counter = 0
            self.save("delete-log-dialog")
        self.launch("licenses", variant, "OpenSourceLicenses")
        self.tap("Activity", partial=True)
        self.counter = 0
        self.save("library-details")

    def surfaces(self, variant, selected=None):
        names = ["quick-settings", "connection-popup", "island-connected", "island-taking-over", "island-linux", "island-other", "island-reversed", "battery-widget", "battery-widget-phone", "noise-widget", "notification", "qs-tile"]
        if selected and not set(selected) <= set(names):
            raise ValueError(f"Unknown native surfaces: {set(selected) - set(names)}")
        for surface in names:
            if selected and surface not in selected:
                continue
            self.launch(surface, variant, surface=surface)
            if surface == "notification":
                self.shell("cmd", "statusbar", "expand-notifications")
                time.sleep(0.8)
            elif surface == "qs-tile":
                self.shell("cmd", "statusbar", "add-tile", PACKAGE + "/.services.AirPodsQSService")
                self.shell("cmd", "statusbar", "expand-settings")
                time.sleep(0.8)
                for _ in range(3):
                    _, tree = self.hierarchy()
                    if any(n.get("text") == "AirPods" for n in tree.iter("node")):
                        break
                    self.swipe(horizontal=True)
                else:
                    raise RuntimeError("The app's native Quick Settings tile was not visible.")
            self.save(note="Native app surface opened by the debug entry point with demo battery values.")
            if surface == "quick-settings":
                _, tree = self.hierarchy()
                labels = {"Off", "Transparency", "Adaptive", "Noise Cancel"}
                mode = next((n.get("content-desc") for n in tree.iter("node") if n.get("content-desc") in labels), None)
                if mode is None:
                    raise RuntimeError("Cannot find the native noise-control action.")
                self.tap(mode)
                self.counter = 0
                self.save("quick-settings-expanded")

    def run(self):
        if self.args.apk:
            self.cmd("install", "-r", "-g", "-t", str(Path(self.args.apk).resolve()))
        self.shell("appops", "set", PACKAGE, "SYSTEM_ALERT_WINDOW", "allow")
        self.shell("appwidget", "grantbind", "--package", PACKAGE, "--user", "0")
        for key in ["window_animation_scale", "transition_animation_scale", "animator_duration_scale"]:
            self.shell("settings", "put", "global", key, "0")
        selected = self.args.scenes.split(",") if self.args.scenes else None
        for variant in self.args.variants.split(","):
            for scene, route, scrolls in SCENES:
                if self.args.extras_only or self.args.surfaces:
                    continue
                if selected and scene not in selected:
                    continue
                self.launch(scene, variant, route)
                self.pages(0 if self.args.first_page_only else scrolls)
        if self.args.extras or self.args.extras_only:
            for variant in self.args.variants.split(","):
                self.onboarding(variant)
                self.extras(variant)
                self.surfaces(variant)
        elif self.args.surfaces:
            for variant in self.args.variants.split(","):
                self.surfaces(variant, self.args.surfaces.split(","))
        (self.logs / "last-logcat.txt").write_text(self.cmd("logcat", "-d"))
        self.write_manifest()
        covered = sorted({x["route"] for x in self.data["captures"] if x["route"]})
        print(f"Saved {len(self.data['captures'])} screenshots; {len(covered)} navigation routes covered.", flush=True)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", default="emulator-5582")
    parser.add_argument("--apk")
    parser.add_argument("--variants", default="apple-dark,apple-light")
    parser.add_argument("--scenes", help="Comma-separated scene IDs; omit for all navigation pages.")
    parser.add_argument("--extras", action="store_true", help="Also capture onboarding, dialogs, overlays, widgets and release notes.")
    parser.add_argument("--extras-only", action="store_true", help="Capture only the additional native surfaces.")
    parser.add_argument("--surfaces", help="Capture only comma-separated native surfaces, such as quick-settings,battery-widget.")
    parser.add_argument("--first-page-only", action="store_true")
    args = parser.parse_args()
    Capture(args).run()
