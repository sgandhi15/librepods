/* Screen content lives here. Shared appearance lives in styles.css. No app logic is used. */
(() => {
  "use strict";
  const D = window.SOURCE_DATA;
  const esc = (value) =>
    String(value ?? "").replace(
      /[&<>"']/g,
      (c) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#39;",
        })[c],
    );
  const t = (key) => D.strings[key] ?? key;
  const P = "android/app/src/main/java/me/kavishdevar/librepods/presentation/";
  const A = "../../android/app/src/main/res-apple/drawable/";
  const asset = (name) => `${A}airpods_pro_2_${name}.png`;
  const screens = [];
  function add(id, name, category, file, body, extra = {}) {
    screens.push({
      id,
      name,
      category,
      platform: "android",
      sources: [`${P}${file}`],
      body,
      ...extra,
    });
  }
  const button = (label, target, primary = false) =>
    `<button type="button" class="${primary ? "primary" : "soft-button"}" ${target ? `data-go="${esc(target)}"` : 'data-demo="Preview only. No device or external service is connected."'}>${esc(label)}</button>`;
  const link = (label, value = "", target = "") =>
    `<button type="button" class="row" ${target ? `data-go="${esc(target)}"` : 'data-demo="This value is sample data."'}><span class="row-main">${esc(label)}</span>${value ? `<span class="row-value">${esc(value)}</span>` : ""}${target ? '<span class="chevron" aria-hidden="true">›</span>' : ""}</button>`;
  const toggle = (label, checked = true, description = "", extra = "") =>
    `<div class="row"><span class="row-main">${esc(label)}${description ? `<small>${esc(description)}</small>` : ""}</span><label class="toggle"><input type="checkbox" aria-label="${esc(label)}" ${checked ? "checked" : ""} ${extra}></label></div>`;
  const group = (title, content, description = "") =>
    `<section class="group">${title ? `<h5 class="group-title">${esc(title)}</h5>` : ""}<div class="rows">${content}</div>${description ? `<p class="description">${esc(description)}</p>` : ""}</section>`;
  const description = (text) => `<p class="description">${esc(text)}</p>`;
  const options = (title, labels, selected = 0, desc = "") =>
    group(
      title,
      labels
        .map(
          (label, i) =>
            `<button type="button" class="row" data-option aria-pressed="${i === selected}"><span class="row-main">${esc(label)}</span><span class="checkmark" aria-hidden="true">${i === selected ? "✓" : ""}</span></button>`,
        )
        .join(""),
      desc,
    );
  const slider = (label, value = 50, start = "Less", end = "More", desc = "") =>
    `<section class="group">${label ? `<h5 class="group-title">${esc(label)}</h5>` : ""}<div class="range-card"><div class="range-labels"><span>${esc(start)}</span><output>${value}</output><span>${esc(end)}</span></div><input type="range" min="0" max="100" value="${value}" aria-label="${esc(label || "Level")}"></div>${desc ? description(desc) : ""}</section>`;
  const toggleGroup = (key, checked = true, title = "") =>
    group(title, toggle(t(key), checked), t(`${key}_description`));
  const field = (label, value = "", type = "text") =>
    `<label>${label ? `<p class="description">${esc(label)}</p>` : ""}<input type="${type}" aria-label="${esc(label || "Device name")}" value="${esc(value)}"></label>`;
  const modes = (selected = 1, linux = false) => {
    const labels = linux
      ? ["Off", "Noise Cancellation", "Transparency", "Adaptive"]
      : ["Off", "Transparency", "Adaptive", "Noise Cancellation"];
    return `<section class="mode-group">${!linux ? `<h5 class="group-title">${esc(t("listening_mode"))}</h5>` : ""}<div class="mode-buttons" role="group" aria-label="Listening mode">${labels.map((m, i) => `<button type="button" class="${i === selected ? "selected" : ""}" aria-pressed="${i === selected}" data-mode><span class="mode-icon" aria-hidden="true">${["◯", "◉", "✦", "◎"][i]}</span>${esc(m)}</button>`).join("")}</div></section>`;
  };
  const batteries = (separate = false) =>
    `<div class="battery-view">${(separate
      ? [
          ["left", "Left · 96%"],
          ["right", "Right · 94%"],
          ["case", "Case · 78%"],
        ]
      : [
          ["buds", "96%"],
          ["case", "78%"],
        ]
    )
      .map(
        ([img, label]) =>
          `<div class="battery-unit"><img loading="lazy" src="${asset(img)}" alt="AirPods ${img}"><div class="battery-ring" aria-hidden="true"></div><span>${label}</span></div>`,
      )
      .join("")}</div>`;
  const upgrade = () => button(t("unlock_advanced_features"), "purchase", true);
  const steps = (current) =>
    `<div class="steps" aria-label="Step ${current + 1} of 5">${[0, 1, 2, 3, 4].map((i) => `<span class="${i <= current ? "active" : ""}"></span>`).join("")}</div>`;
  const sampleLog =
    "Sample log, not collected from a device\n12:04:01  LibrePods service started\n12:04:02  Bluetooth connection requested\n12:04:03  AirPods connected\n12:04:03  Battery notification received";
  const overlay = (content, sheet = false) =>
    `<div class="overlay-backdrop ${sheet ? "sheet-stage" : ""}"><div class="${sheet ? "sheet" : "alert"}">${content}</div></div>`;
  const alert = (title, message, confirm, target, destructive = false) =>
    overlay(
      `<h5>${esc(title)}</h5><p>${esc(message)}</p><div class="alert-actions"><button type="button" data-go="${target}">Cancel</button><button type="button" ${destructive ? 'class="danger"' : ""} data-go="${target}">${esc(confirm)}</button></div>`,
    );
  const onboard = (
    title,
    content,
    next,
    nextLabel = "Continue",
    secondary = "",
  ) =>
    `<div class="onboard"><h5 class="onboard-title">${esc(title)}</h5><div class="onboard-content">${content}</div><div class="step-footer">${secondary}<button type="button" class="primary" data-go="${esc(next)}">${esc(nextLabel)}</button></div></div>`;
  const permission = (label, detail, glyph, color, checked = false) =>
    `<div class="row"><span class="settings-icon" style="background:${color}" aria-hidden="true">${glyph}</span><span class="row-main">${esc(label)}${detail ? `<small>${esc(detail)}</small>` : ""}</span><label class="toggle"><input type="checkbox" aria-label="${esc(label)}" ${checked ? "checked" : ""}></label></div>`;
  const graph = () =>
    `<div class="graph"><svg viewBox="0 0 300 180" role="img" aria-label="Sample horizontal and vertical head velocity graph"><line x1="22" y1="30" x2="290" y2="30"/><line x1="22" y1="90" x2="290" y2="90"/><line x1="22" y1="150" x2="290" y2="150"/><text x="0" y="34">60</text><text x="6" y="94">0</text><text x="0" y="154">-60</text><path d="M22 90 L50 88 L65 95 L80 88 L100 90 L115 50 L130 120 L145 75 L165 94 L190 88 L210 92 L235 88 L260 90 L290 90" fill="none" stroke="#4b9cfa" stroke-width="2"/><path d="M22 95 L65 92 L90 98 L115 91 L140 100 L160 92 L190 94 L215 65 L230 115 L245 88 L265 96 L290 93" fill="none" stroke="#ff7279" stroke-width="2"/><text x="140" y="178">Horizontal</text><text x="230" y="178">Vertical</text></svg></div>`;
  const eq = () =>
    `<section class="range-card eq-card"><svg viewBox="0 0 300 200" role="img" aria-label="Sample three-band equalizer curve"><path d="M0 50H300 M0 100H300 M0 150H300" stroke="var(--separator)" stroke-dasharray="5 5" fill="none"/><path class="eq-path" d="M20 100 Q85 100 150 100 T280 100" stroke="#00dfb5" stroke-width="5" fill="none"/><g fill="#00dfb5"><circle cx="20" cy="100" r="9"/><circle cx="150" cy="100" r="9"/><circle cx="280" cy="100" r="9"/></g></svg><div class="eq-labels"><span>LOW</span><span>MID</span><span>HIGH</span></div><div class="eq-sliders">${["Low", "Mid", "High"].map((b) => `<label>${b}<input type="range" aria-label="${b} equalizer" min="0" max="100" value="50" data-eq></label>`).join("")}</div></section>`;

  // Four onboarding pages in an iOS setup-assistant layout: centred title,
  // grouped cards and a pinned action.
  add(
    "welcome",
    "Welcome",
    "Onboarding",
    "screens/onboarding/OnboardingScreen.kt",
    () =>
      `<div class="onboard welcome"><h5 class="onboard-title">Welcome to<br>LibrePods</h5><div class="onboard-content"><p>${esc(t("app_description"))}</p></div><div class="step-footer"><button type="button" class="primary" data-go="privacy">Continue</button></div></div>`,
    { route: "Onboarding", noNav: true },
  );
  add(
    "privacy",
    "Privacy policy",
    "Onboarding",
    "screens/onboarding/PrivacyPolicyPage.kt",
    () =>
      onboard(
        t("privacy_policy"),
        `<div class="card">${D.privacy
          .map((text) =>
            text.length < 35 ? `<h6>${esc(text)}</h6>` : `<p>${esc(text)}</p>`,
          )
          .join("")}</div>`,
        "permissions",
        t("i_agree"),
      ),
    {
      noNav: true,
      note: "Complete policy text extracted from PrivacyPolicyPage.kt. Unsupported devices see the compatibility page before permissions.",
    },
  );
  add(
    "unsupported",
    "Not supported",
    "Onboarding",
    "screens/onboarding/NotSupportedPage.kt",
    () =>
      onboard(
        t("not_supported"),
        `<p>${esc(t("not_compatible_update"))}</p><p>${esc(t("not_compatitble_description"))}</p>${group("", link(t("bypass_compatibility_check"), "", "permissions"))}`,
        "permissions",
      ),
    { noNav: true },
  );
  add(
    "permissions",
    "Permissions",
    "Onboarding",
    "screens/onboarding/PermissionsPage.kt",
    () =>
      onboard(
        t("permissions"),
        group(
          "Required",
          permission(
            "Bluetooth",
            "Communicate with your AirPods",
            "ᛒ",
            "#0a84ff",
          ),
        ) +
          group(
            "Optional",
            permission(
              "Notifications",
              "Show battery status",
              "◉",
              "#ff3b30",
            ) +
              permission(
                "Phone",
                "Respond to phone calls with head gestures",
                "✆",
                "#34c759",
              ) +
              permission(
                "Display over other apps",
                "Show popups when AirPods are nearby or audio switches to them.",
                "▣",
                "#ff9500",
              ),
          ),
        "home",
        "Continue",
        '<button type="button" class="soft-button" data-grant>Grant All</button>',
      ),
    {
      noNav: true,
      note: "Permission switches are local stand-ins. Android system permission prompts are outside this app-owned inventory.",
    },
  );

  const home = (free = false, split = false, banner = false) =>
    `${banner ? `<div class="banner">${esc(t("play_foss_premium_banner").replace("%1$d", "3"))}</div>` : ""}${batteries(split)}${group("", link(t("name"), "Alex's AirPods Pro", "rename"))}${group(t("hearing_health"), link(t("hearing_protection"), "", "hearing-protection") + link(t("hearing_aid"), "", "hearing-aid"))}${modes()}${group(t("press_and_hold_airpods"), link(t("left"), t("listening_mode"), "long-press-left") + link(t("right"), t("digital_assistant"), "long-press-right"))}${group(t("call_controls"), link(t("answer_call"), t("press_once")) + link(t("mute_unmute"), t("press_once"), "call-mute") + link(t("hang_up"), t("press_twice"), "call-hangup"))}${free ? upgrade() : ""}${group(t("audio"), toggle(t("personalized_volume"), true, t("personalized_volume_description"), free ? "disabled" : "") + toggle(t("conversation_awareness"), !free, t("conversation_awareness_description"), free ? "disabled" : "") + toggle(t("loud_sound_reduction"), true, t("loud_sound_reduction_description")) + link(t("adaptive_audio"), "", "adaptive") + link(t("equalizer"), "", "equalizer"))}${group("", toggle(t("ear_detection"), true) + toggle(t("automatically_connect"), true, t("automatically_connect_description")))}${group("", link(t("microphone_mode"), t("microphone_automatic"), "microphone"))}${group("", toggle(t("sleep_detection"), false))}${group("", link(t("head_gestures"), t("on"), "head-tracking"))}${toggleGroup("optimized_charging")}${group("", link(t("accessibility"), "", "accessibility"))}${toggleGroup("off_listening_mode")}${group(t("about"), link(t("model_name"), "AirPods Pro") + link(t("model_number"), "Sample model") + link(t("serial_number"), "SAMPLE0001") + link(t("version"), "Sample firmware", "version"))}${button(t("disconnect"), "disconnected")}`;
  const homeNote =
    "Scrollable full page. Capability-dependent controls are shown together for review; an actual model may show fewer. Device names, battery, firmware and serials are sample values.";
  add(
    "home",
    "AirPods settings · connected",
    "Device & connection",
    "screens/AirPodsSettingsScreen.kt",
    () => home(),
    {
      route: "AirPodsSettings",
      title: "Alex's AirPods Pro",
      home: true,
      note: homeNote,
    },
  );
  add(
    "home-free",
    "AirPods settings · free tier",
    "Device & connection",
    "screens/AirPodsSettingsScreen.kt",
    () => home(true),
    {
      title: "Alex's AirPods Pro",
      home: true,
      note: "Upgrade action and disabled premium controls. " + homeNote,
    },
  );
  add(
    "home-separate",
    "AirPods settings · separate batteries",
    "Device & connection",
    "components/BatteryView.kt",
    () => home(false, true),
    {
      title: "Alex's AirPods Pro",
      home: true,
      note: "Left and right batteries are shown separately when the app cannot combine them.",
    },
  );
  add(
    "home-migration",
    "AirPods settings · Play migration notice",
    "Device & connection",
    "screens/AirPodsSettingsScreen.kt",
    () => home(true, false, true),
    {
      title: "Alex's AirPods Pro",
      home: true,
      note: "Conditional FOSS-to-Play billing notice. No billing action is performed.",
    },
  );
  add(
    "disconnected",
    "AirPods not connected",
    "Device & connection",
    "screens/AirPodsSettingsScreen.kt",
    () =>
      `<div class="center-state"><img class="big-pods" src="${asset("buds")}" alt="AirPods"><h5>${esc(t("airpods_not_connected"))}</h5><p>${esc(t("airpods_not_connected_description"))}</p>${button(t("reconnect_to_last_device"), "reconnecting", true)}${button(t("troubleshooting"), "troubleshooting")}</div>`,
    { title: "LibrePods", home: true },
  );
  add(
    "remote",
    "Remote connection · tap to reconnect",
    "Device & connection",
    "screens/AirPodsSettingsScreen.kt",
    () =>
      `<div class="center-state"><img class="big-pods" src="${asset("buds")}" alt="AirPods"><h5>${esc(t("tap_to_reconnect"))}</h5><p>${esc(t("airpods_not_connected"))}</p>${button(t("tap_to_reconnect"), "home", true)}${button(t("troubleshooting"), "troubleshooting")}</div>`,
    {
      home: true,
      title: "LibrePods",
      note: "AACP connection retained while audio is connected elsewhere. Tap-to-reconnect layout is approximate.",
    },
  );
  add(
    "reconnecting",
    "Reconnecting",
    "Device & connection",
    "screens/AirPodsSettingsScreen.kt",
    () =>
      `<div class="center-state"><div class="spinner" aria-label="Reconnecting"></div><h5>${esc(t("reconnecting"))}</h5>${button("Show connected state", "home")}</div>`,
    {
      title: "LibrePods",
      home: true,
      note: "Static representation of the transient reconnect state.",
    },
  );
  add(
    "loading",
    "Loading",
    "Device & connection",
    "screens/LoadingScreen.kt",
    () =>
      '<div class="center-state"><div class="spinner" role="status" aria-label="Loading"></div></div>',
    { note: "CSS spinner approximates the native circular wavy indicator." },
  );

  add(
    "rename",
    "Name",
    "Device settings",
    "screens/RenameScreen.kt",
    () =>
      '<div class="input-wrap"><input type="text" aria-label="Device name" value="Alex\'s AirPods Pro" maxlength="32"><button type="button" class="clear-input" data-clear aria-label="Clear name">×</button></div>',
    {
      route: "Rename",
      note: "The native screen opens the keyboard and saves as you type. The browser preview edits local input only.",
    },
  );
  for (const [id, side] of [
    ["long-press-left", "Left"],
    ["long-press-right", "Right"],
  ]) {
    add(
      id,
      `Press and hold · ${side.toLowerCase()}`,
      "Device settings",
      "screens/PressAndHoldSettingsScreen.kt",
      () =>
        options(
          "",
          [t("listening_mode"), t("digital_assistant")],
          side === "Left" ? 0 : 1,
        ) +
        options(
          t("listening_mode"),
          [t("off"), t("transparency"), t("adaptive"), t("noise_cancellation")],
          1,
          t("press_and_hold_listening_mode_description"),
        ),
      {
        route: side === "Left" ? "LongPress" : undefined,
        title: side,
        note: "The action selector is single-choice; listening modes below support multiple selections.",
        multiLast: true,
      },
    );
  }
  add(
    "call-mute",
    "Mute/Unmute",
    "Device settings",
    "screens/CallControlScreen.kt",
    () => options("", [t("press_once"), t("press_twice")]),
    { route: "CallControl" },
  );
  add(
    "call-hangup",
    "Hang Up",
    "Device settings",
    "screens/CallControlScreen.kt",
    () => options("", [t("press_once"), t("press_twice")], 1),
  );
  add(
    "microphone",
    "Microphone Mode",
    "Device settings",
    "screens/MicrophoneSettingsScreen.kt",
    () =>
      options("", [
        t("microphone_automatic"),
        t("microphone_always_right"),
        t("microphone_always_left"),
      ]),
    { route: "MicrophoneSettings" },
  );
  add(
    "adaptive",
    "Customize Adaptive Audio",
    "Device settings",
    "screens/AdaptiveStrengthScreen.kt",
    () =>
      slider(
        t("customize_adaptive_audio"),
        50,
        "◖",
        "◀",
        t("adaptive_audio_description"),
      ),
    { route: "AdaptiveStrength" },
  );
  add(
    "equalizer",
    "Equalizer",
    "Device settings",
    "screens/EqualizerScreen.kt",
    () =>
      options("", [t("recommended"), t("custom")], 1) +
      "<div data-custom-eq>" +
      eq() +
      '<button type="button" class="soft-button" data-reset-eq>Reset</button></div>',
    {
      route: "Equalizer",
      note: "Custom mode shows the three-band control. Choose Recommended to hide it. Sliders are browser-only stand-ins for native draggable handles.",
    },
  );
  add(
    "head-tracking",
    "Head Tracking",
    "Device settings",
    "screens/HeadTrackingScreen.kt",
    () =>
      group("", toggle(t("head_gestures"), true), t("head_gestures_details")) +
      '<h5 class="group-title">Velocity</h5>' +
      graph() +
      '<button type="button" class="soft-button" data-gesture>Test Head Gestures</button><p class="description" data-gesture-result></p>',
    {
      route: "HeadTracking",
      note: "Graph is sample data. The test button demonstrates a result without using sensors.",
    },
  );
  add(
    "version",
    "Version",
    "Device settings",
    "screens/VersionInfoScreen.kt",
    () =>
      group(
        t("version"),
        [1, 2, 3]
          .map((i) => link(`${t("version")} ${i}`, "Sample firmware"))
          .join(""),
      ),
    { route: "VersionInfo" },
  );

  add(
    "accessibility",
    "Accessibility",
    "Accessibility & hearing",
    "screens/AccessibilitySettingsScreen.kt",
    () =>
      options(
        t("press_speed"),
        [t("default_option"), t("slower"), t("slowest")],
        0,
        t("press_speed_description"),
      ) +
      options(
        t("press_and_hold_duration"),
        [t("default_option"), t("longer"), t("longest")],
        0,
        t("press_and_hold_duration_description"),
      ) +
      toggleGroup(
        "noise_cancellation_single_airpod",
        false,
        t("listening_mode"),
      ) +
      toggleGroup("loud_sound_reduction") +
      group("", link(t("customize_transparency_mode"), "", "transparency")) +
      slider(t("tone_volume"), 75, "◖", "◀", t("tone_volume_description")) +
      toggleGroup("volume_control") +
      options(
        t("volume_swipe_speed"),
        [t("default_option"), t("slower"), t("slowest")],
        0,
        t("volume_swipe_speed_description"),
      ),
    {
      route: "Accessibility",
      note: "Shows vendor-spoofing and premium-dependent settings for inventory purposes. Commented-out phone/media EQ controls are not included.",
    },
  );
  const hearingSliders = () =>
    slider(t("amplification"), 50, "◖", "◀") +
    slider(t("balance"), 50, t("left"), t("right")) +
    slider(t("tone"), 50, t("darker"), t("brighter")) +
    slider(t("ambient_noise_reduction"), 20, t("less"), t("more"));
  add(
    "transparency",
    "Customize Transparency Mode",
    "Accessibility & hearing",
    "screens/TransparencySettingsScreen.kt",
    () =>
      group(
        "",
        toggle(t("transparency_mode"), true),
        t("customize_transparency_mode_description"),
      ) +
      hearingSliders() +
      toggleGroup("conversation_boost", false) +
      '<h5 class="group-title">Equalizer</h5>' +
      Array.from({ length: 8 }, (_, i) =>
        slider(t("band_label").replace("%d", i + 1), 50, "0", "1"),
      ).join(""),
    {
      route: "TransparencyCustomization",
      note: "Includes the eight independent EQ bands below the fold.",
    },
  );
  add(
    "hearing-protection",
    "Hearing Protection",
    "Accessibility & hearing",
    "screens/HearingProtectionScreen.kt",
    () =>
      toggleGroup("loud_sound_reduction", true, t("environmental_noise")) +
      group(
        t("workspace_use"),
        toggle(t("ppe"), true),
        t("workspace_use_description"),
      ),
    { route: "HearingProtection" },
  );
  add(
    "hearing-aid",
    "Hearing Aid",
    "Accessibility & hearing",
    "screens/HearingAidScreen.kt",
    () =>
      group(
        t("hearing_aid"),
        toggle(t("hearing_aid"), false, "", "data-hearing-aid") +
          link(t("adjustments"), "", "hearing-adjustments"),
        t("hearing_aid_description"),
      ) + group("", link(t("update_hearing_test"), "", "hearing-test")),
    { route: "HearingAid" },
  );
  add(
    "hearing-confirm",
    "Enable Hearing Aid · confirmation",
    "Accessibility & hearing",
    "screens/HearingAidScreen.kt",
    () =>
      alert(
        "Enable Hearing Aid",
        "Enabling Hearing Aid will disable Headphone Accommodation and Customized Transparency Mode.",
        "Enable",
        "hearing-aid",
      ),
    {
      title: "Hearing Aid",
      note: "App confirmation dialog. Preview buttons do not change hearing settings.",
    },
  );
  add(
    "hearing-adjustments",
    "Adjustments",
    "Accessibility & hearing",
    "screens/HearingAidAdjustmentsScreen.kt",
    () =>
      slider(t("amplification"), 50, "◖", "◀") +
      group(
        "",
        toggle(t("swipe_to_control_amplification"), true),
        t("swipe_amplification_description"),
      ) +
      slider(t("balance"), 50, t("left"), t("right")) +
      slider(t("tone"), 50, t("darker"), t("brighter")) +
      slider(t("ambient_noise_reduction"), 20, t("less"), t("more")) +
      toggleGroup("conversation_boost", false),
    { route: "HearingAidAdjustments" },
  );
  add(
    "hearing-test",
    "Update Hearing Test Result",
    "Accessibility & hearing",
    "screens/UpdateHearingTestScreen.kt",
    () =>
      description(t("hearing_test_value_instruction")) +
      `<div class="audiogram"><span></span><span class="table-heading">Left</span><span class="table-heading">Right</span>${["250Hz", "500Hz", "1kHz", "2kHz", "3kHz", "4kHz", "6kHz", "8kHz"].map((freq) => `<span>${freq}</span><input type="number" step="0.1" value="0.0" aria-label="${freq} left hearing loss dbHL"><input type="number" step="0.1" value="0.0" aria-label="${freq} right hearing loss dbHL">`).join("")}</div>`,
    {
      route: "UpdateHearingTest",
      note: "All eight frequencies and both ears. Values do not get sent to AirPods.",
    },
  );

  function settings(unavailable = false) {
    return (
      upgrade() +
      toggleGroup("show_phone_battery_in_widget", true, t("widget")) +
      group(
        t("popup_animations"),
        toggle(
          t("show_bottom_sheet_popup"),
          true,
          t("show_bottom_sheet_popup_description"),
        ) +
          toggle(
            t("show_island_popup"),
            true,
            t("show_island_popup_description"),
          ),
      ) +
      group(
        t("conversation_awareness"),
        toggle(
          t("conversation_awareness_pause_music"),
          false,
          t("conversation_awareness_pause_music_description"),
        ) +
          toggle(
            t("relative_conversation_awareness_volume"),
            true,
            t("relative_conversation_awareness_volume_description"),
          ),
      ) +
      slider(t("conversation_awareness_volume"), 55, "10%", "85%") +
      toggleGroup("disconnect_when_not_wearing", false, t("ear_detection")) +
      group(
        t("takeover_airpods_state"),
        ["disconnected", "idle", "music", "call"]
          .map((key, i) =>
            toggle(t(`takeover_${key}`), i < 2, t(`takeover_${key}_desc`)),
          )
          .join(""),
      ) +
      group(
        t("takeover_phone_state"),
        ["ringing_call", "media_start"]
          .map((key) =>
            toggle(t(`takeover_${key}`), true, t(`takeover_${key}_desc`)),
          )
          .join(""),
      ) +
      toggleGroup(
        "use_alternate_head_tracking_packets",
        false,
        t("advanced_options"),
      ) +
      (unavailable
        ? description(t("customizations_unavailable"))
        : group(
            "",
            toggle(
              `${t("act_as_an_apple_device")} · ${t("requires_xposed")}`,
              false,
            ),
            t("act_as_an_apple_device_description"),
          ) + group("", link(t("troubleshooting"), "", "troubleshooting"))) +
      group(
        t("contact"),
        link(t("email"), "", "contact") +
          link(t("discord"), "") +
          link(t("github_issues"), ""),
      ) +
      group(
        t("device_info"),
        link(t("manufacturer"), "Sample phone") +
          link(t("model_number"), "Sample model") +
          link(t("build_id"), "Sample build") +
          link(t("version"), "Android sample") +
          link(t("xposed_available"), t("yes")) +
          link(t("app_enabled_in_xposed"), t("yes")),
      ) +
      group(
        t("about"),
        link(t("version"), "Sample build", "release-notes") +
          link(t("version_code"), "Sample") +
          link(t("flavor"), "FOSS") +
          link(t("build_type"), "debug"),
      ) +
      group("", link(t("open_source_licenses"), "", "licenses"))
    );
  }
  add(
    "settings",
    "Settings",
    "App & support",
    "screens/AppSettingsScreen.kt",
    () => settings(),
    {
      route: "AppSettings",
      note: "Full scrollable settings page. Phone and build details are fictional. Discord, email and GitHub actions do not launch external services.",
    },
  );
  add(
    "settings-unavailable",
    "Settings · first connection required",
    "App & support",
    "screens/AppSettingsScreen.kt",
    () => settings(true),
    {
      title: "Settings",
      note: "Conditional customizations-unavailable message before the first AirPods connection.",
    },
  );
  add(
    "contact",
    "Describe your issue · email sheet",
    "App & support",
    "screens/AppSettingsScreen.kt",
    () =>
      overlay(
        '<div class="sheet-actions"><button type="button" data-go="settings">×</button><strong>Describe your issue</strong><button type="button" data-demo="Preview only. No email was sent.">↑</button></div>' +
          field("Subject") +
          '<label><p class="description">Describe your issue</p><textarea aria-label="Describe your issue" placeholder="Describe your issue"></textarea></label>',
        true,
      ),
    { title: "Settings" },
  );
  add(
    "purchase",
    "Unlock advanced features",
    "App & support",
    "screens/PurchaseScreen.kt",
    () =>
      group(
        t("free_features"),
        ["ear_detection", "battery", "listening_mode", "hearing_aid"]
          .map(
            (key) =>
              `<div class="row"><span class="row-main">${esc(t(key))}<small>${esc(t(`${key}_description`).split("\n\n")[0])}</small></span></div>`,
          )
          .join(""),
      ) +
      group(
        t("advanced_features"),
        [
          "conversation_awareness",
          "digital_assistant_on_long_press",
          "head_gestures",
          "advanced_device_settings",
          "automatic_connection",
          "customizations",
          "support_the_development",
        ]
          .map(
            (key) =>
              `<div class="row"><span class="row-main">${esc(t(key))}<small>${esc(t({ head_gestures: "head_gestures_details", support_the_development: "support_development_description" }[key] || `${key}_description`))}</small></span></div>`,
          )
          .join(""),
        t("feature_availability_disclaimer"),
      ) +
      button("Buy · sample price", null, true) +
      button(t("restore_purchases")),
    {
      route: "Purchase",
      note: "Play billing layout. Price and availability come from the store at runtime; no purchase is possible here. FOSS builds use sponsorship instead.",
    },
  );
  add(
    "licenses",
    "Open Source Licenses",
    "App & support",
    "screens/OpenSourceLicensesScreen.kt",
    () =>
      `<input type="search" class="license-search" aria-label="Search libraries" placeholder="Search libraries" data-license-search><div class="library-list">${D.libraries.map((l) => `<article class="library" data-library-name="${esc(l.name.toLowerCase())}"><p class="license-title">${esc(l.name)} <small>${esc(l.artifactVersion)}</small></p><p class="license-description">${esc(l.description)}</p><span class="license-tag">${esc((l.licenses || []).join(", "))}</span></article>`).join("")}</div>`,
    {
      route: "OpenSourceLicenses",
      note: "Complete dependency list from res/raw/aboutlibraries.json. Native third-party library UI is approximated.",
    },
  );

  // One Apple-style "What's New" page with a card per update.
  const releaseUpdates = [["equalizer", () => eq()]];
  add(
    "release-notes",
    "What's New",
    "Release notes",
    "screens/ReleaseNotesScreen.kt",
    () =>
      `<div class="onboard"><h5 class="page-title">${esc(t("what_s_new"))}</h5><p class="release-version"><button type="button" class="plain-link" data-demo="Sample build. The installed app shows its real version here.">${esc(t("version"))} · sample build</button></p>${releaseUpdates
        .map(
          ([key, preview]) =>
            `<div class="release-card"><div class="release-mini">${preview()}</div><h5>${esc(t(key))}</h5><p>${esc(t(`update_${key}_description`))}</p></div>`,
        )
        .join("")}<div class="step-footer"><button type="button" class="primary" data-go="home">Continue</button></div></div>`,
    {
      route: "ReleaseNotes",
      noNav: true,
      note: "All update cards are shown on one page. The native animated mask and demo are approximated.",
    },
  );

  const savedLogs = (populated) =>
    group(
      t("saved_logs"),
      populated
        ? link("Oct 17, 2026 12:04", "Sample log", "log-view") +
            link("Oct 16, 2026 09:18", "Sample log", "log-view")
        : `<p class="description" style="padding:25px 0;text-align:center">${esc(t("no_logs_found"))}</p>`,
    ) +
    (populated
      ? description("Total Logs: 2") + button("Delete All", "delete-all-logs")
      : "") +
    button(t("collect_logs"), "troubleshoot-scope", true);
  add(
    "troubleshooting",
    "Troubleshooting · no saved logs",
    "Troubleshooting",
    "screens/TroubleshootingScreen.kt",
    () => savedLogs(false),
    { route: "Troubleshooting", title: "Troubleshooting" },
  );
  add(
    "troubleshooting-saved",
    "Troubleshooting · saved logs",
    "Troubleshooting",
    "screens/TroubleshootingScreen.kt",
    () => savedLogs(true) + button("Delete a log", "delete-log"),
    {
      title: "Troubleshooting",
      note: "Sample files and timestamps, no filesystem access.",
    },
  );
  const logSteps = [
    [
      "scope",
      "First, let's ensure Xposed module is properly configured. Tap the button below to check Xposed scope settings.",
      "Open Xposed Settings",
    ],
    [
      "disconnect",
      "Please put your AirPods in the case and close it, so they disconnect completely.",
      "Continue",
    ],
    ["preparing", "Preparing to collect logs... Please wait.", "Preparing..."],
    [
      "collecting",
      "Now, open the AirPods case and connect your AirPods. Logs are being collected. Connection will be detected automatically, or you can manually stop logging when you're done.",
      "Stop Collection",
    ],
    [
      "complete",
      "Log collection complete! You can now save or share the logs.",
      "Done",
    ],
  ];
  logSteps.forEach(([id, instruction, label], i) =>
    add(
      `troubleshoot-${id}`,
      `Collect logs · ${id}`,
      "Troubleshooting",
      "screens/TroubleshootingScreen.kt",
      () =>
        `<h5 class="group-title">Troubleshooting Steps</h5>${steps(i)}<div class="log-stage"><h5>${i === 2 ? "Preparing..." : i === 3 ? "Collecting logs..." : "Troubleshooting Steps"}</h5><p>${esc(instruction)}</p>${i === 2 || i === 3 ? '<div class="spinner" aria-label="In progress"></div>' : ""}${i === 4 ? `<pre class="log-text">${esc(sampleLog)}</pre>${button("Share")}${button("Save")}` : ""}${button(label, i < 4 ? `troubleshoot-${logSteps[i + 1][0]}` : "troubleshooting-saved", true)}</div>${i === 0 ? button("Continue", "troubleshoot-disconnect") : ""}`,
      {
        title: "Troubleshooting",
        note: "Wizard states are navigable samples. This gallery does not open Xposed, disconnect devices or collect logs.",
      },
    ),
  );
  add(
    "log-view",
    "Saved log · bottom sheet",
    "Troubleshooting",
    "screens/TroubleshootingScreen.kt",
    () =>
      overlay(
        "<h5>Sample log file</h5>" +
          description("Oct 17, 2026 12:04") +
          `<pre class="log-text">${esc(sampleLog)}</pre>` +
          button("Share") +
          button("Save") +
          button("Close", "troubleshooting-saved"),
        true,
      ),
    { title: "Troubleshooting" },
  );
  add(
    "delete-log",
    "Delete Log File · confirmation",
    "Troubleshooting",
    "screens/TroubleshootingScreen.kt",
    () =>
      alert(
        "Delete Log File",
        "Are you sure you want to delete this log file? This action cannot be undone.",
        "Delete",
        "troubleshooting-saved",
        true,
      ),
    { title: "Troubleshooting" },
  );
  add(
    "delete-all-logs",
    "Delete All Logs · confirmation",
    "Troubleshooting",
    "screens/TroubleshootingScreen.kt",
    () =>
      alert(
        "Delete All Logs",
        "Are you sure you want to delete all log files? This action cannot be undone and will remove 2 log files.",
        "Delete All",
        "troubleshooting-saved",
        true,
      ),
    { title: "Troubleshooting" },
  );

  // System-owned surroundings are intentionally omitted. These are app-owned surfaces.
  const systemStage = (content) => `<div class="system-stage">${content}</div>`;
  const widgetBatteries = (phone) =>
    `<div class="widget-batteries">${(phone
      ? [
          ["Phone", "62%"],
          ["Left", "96%"],
          ["Right", "94%"],
          ["Case", "78%"],
        ]
      : [
          ["Left", "96%"],
          ["Right", "94%"],
          ["Case", "78%"],
        ]
    )
      .map(
        ([name, level]) =>
          `<div><div class="battery-ring">${level}</div><small>${name}</small></div>`,
      )
      .join("")}</div>`;
  add(
    "battery-widget",
    "Battery widget",
    "Widgets & notifications",
    "widgets/BatteryWidget.kt",
    () =>
      systemStage(
        '<div class="system-card">' + widgetBatteries(false) + "</div>",
      ),
    {
      noNav: true,
      sources: [
        P + "widgets/BatteryWidget.kt",
        "android/app/src/main/res/layout/battery_widget.xml",
      ],
    },
  );
  add(
    "battery-widget-phone",
    "Battery widget · phone battery",
    "Widgets & notifications",
    "widgets/BatteryWidget.kt",
    () =>
      systemStage(
        '<div class="system-card">' + widgetBatteries(true) + "</div>",
      ),
    {
      noNav: true,
      sources: [
        P + "widgets/BatteryWidget.kt",
        "android/app/src/main/res/layout/battery_widget.xml",
      ],
    },
  );
  add(
    "noise-widget",
    "Listening mode widget",
    "Widgets & notifications",
    "widgets/NoiseControlWidget.kt",
    () => systemStage(modes()),
    {
      noNav: true,
      sources: [
        P + "widgets/NoiseControlWidget.kt",
        "android/app/src/main/res/layout/listening_mode_widget.xml",
      ],
    },
  );
  const serviceSource =
    "android/app/src/main/java/me/kavishdevar/librepods/services/AirPodsService.kt";
  for (const expanded of [false, true]) {
    add(
      expanded ? "notification-expanded" : "notification",
      "Notification · " + (expanded ? "expanded" : "collapsed"),
      "Widgets & notifications",
      "screens/AirPodsSettingsScreen.kt",
      () =>
        systemStage(
          `<div class="system-card notification"><small>LibrePods · now</small><h5>Alex's AirPods Pro</h5>${expanded ? `<div class="notification-batteries">${["left", "right", "case"].map((img, i) => `<span><img src="${asset(img)}" alt="${img}"> ${[96, 94, 78][i]}%</span>`).join("")}</div>` : ""}</div>`,
        ),
      {
        noNav: true,
        sources: [
          serviceSource,
          `android/app/src/main/res/layout/notification${expanded ? "_expanded" : ""}.xml`,
        ],
        note: "Native notification chrome varies by Android version. Device state is sample data.",
      },
    );
  }
  add(
    "notification-reconnect",
    "Notification · reconnect action",
    "Widgets & notifications",
    "screens/AirPodsSettingsScreen.kt",
    () =>
      systemStage(
        '<div class="system-card notification"><small>LibrePods · now</small><h5>AirPods Pro</h5><p class="description">Moved to another device</p>' +
          button("Reconnect", "home") +
          "</div>",
      ),
    {
      noNav: true,
      sources: [serviceSource],
      note: "Representative notification with reconnect action after another device takes over.",
    },
  );
  add(
    "qs-tile",
    "Quick Settings tile",
    "Widgets & notifications",
    "widgets/NoiseControlWidget.kt",
    () =>
      systemStage(
        '<div class="system-card"><small>Quick Settings</small>' +
          button("AirPods · Transparency", "quick-settings", true) +
          "</div>",
      ),
    {
      noNav: true,
      sources: [
        "android/app/src/main/java/me/kavishdevar/librepods/services/AirPodsQSService.kt",
      ],
      note: "Tile appearance is controlled by Android. Only the app label and action are represented.",
    },
  );
  for (const expanded of [false, true]) {
    add(
      expanded ? "quick-settings-expanded" : "quick-settings",
      `Control center · ${expanded ? "expanded modes" : "volume"}`,
      "Popups & control center",
      "components/VerticalVolumeSlider.kt",
      () =>
        `<div class="qs-stage"><img class="big-pods" style="height:45px" src="${asset("buds")}" alt="AirPods"><h5>Alex's AirPods Pro</h5><div class="volume-pillar"><div class="volume-fill">◖</div></div><input type="range" min="0" max="100" value="65" aria-label="Media volume" data-volume>${expanded ? modes() : `<div class="qs-shortcuts"><button type="button" data-go="quick-settings-expanded">Noise Control<br>Transparency</button><button type="button" data-local-toggle aria-pressed="false">Conversation<br>Awareness</button></div>`}</div>`,
      {
        noNav: true,
        sources: [
          "android/app/src/main/java/me/kavishdevar/librepods/QuickSettingsDialogActivity.kt",
          P + "components/VerticalVolumeSlider.kt",
        ],
        note: "Dim/blur backdrop and elastic drag are approximated. Volume does not affect your computer.",
      },
    );
  }
  add(
    "connection-popup",
    "Connection · bottom popup",
    "Popups & control center",
    "overlays/PopupWindow.kt",
    () =>
      overlay(
        '<div class="sheet-actions"><span></span><strong>AirPods Pro</strong><button type="button" data-demo="Close the full-screen preview to dismiss this sample.">×</button></div>' +
          batteries(true),
        true,
      ),
    {
      noNav: true,
      sources: [
        P + "overlays/PopupWindow.kt",
        "android/app/src/main/res/layout/popup_window.xml",
      ],
      note: "Uses static device artwork instead of the connection video.",
    },
  );
  for (const [id, label, undo] of [
    ["connected", "Connected", false],
    ["taking-over", "Connected", false],
    ["linux", "Moved to Linux", true],
    ["other", "Moved to MacBook", true],
    ["reversed", "Reconnect from notification", false],
  ]) {
    add(
      `island-${id}`,
      `Dynamic Island · ${id}`,
      "Popups & control center",
      "overlays/IslandWindow.kt",
      () =>
        `<div class="system-stage" style="justify-content:flex-start;padding-top:20px"><div class="island"><img src="${asset("buds")}" alt="AirPods"><div class="island-name"><small>${esc(label)}</small>AirPods Pro</div>${undo ? '<button type="button" class="circle" data-go="island-connected" aria-label="Undo takeover">↶</button>' : '<div class="battery-ring">96%</div>'}</div></div>`,
      {
        noNav: true,
        sources: [
          P + "overlays/IslandWindow.kt",
          "android/app/src/main/res/layout/island_window.xml",
        ],
        note: "One of the app overlay event states. Native video, drag and auto-dismiss are not simulated.",
      },
    );
  }

  add(
    "camera-disabled",
    "Camera Control · disabled code",
    "Disabled & legacy",
    "screens/CameraControlScreen.kt",
    () =>
      '<div class="banner">Disabled in this checkout. The screen and route are commented out.</div>' +
      options("", ["Off", "Press once", "Press and hold AirPods"]),
    {
      title: "Camera Control",
      note: "Documented for completeness, not an active destination.",
    },
  );
  add(
    "camera-package-disabled",
    "Custom Camera appid · unreachable dialog",
    "Disabled & legacy",
    "screens/AppSettingsScreen.kt",
    () =>
      overlay(
        "<h5>Set Custom Camera appid</h5><p>Enter the application id of the camera app:</p>" +
          field("Custom Camera appid", "com.example.camera") +
          '<div class="alert-actions"><button type="button" data-go="settings">Cancel</button><button type="button" data-demo="Disabled feature. Nothing was saved.">Save</button></div>',
      ),
    {
      title: "Settings",
      note: "Dialog code remains, but the settings entry that opens it is commented out.",
    },
  );

  const linuxAdd = (id, name, body, sources = ["linux/Main.qml"], extra = {}) =>
    screens.push({
      id,
      name,
      category: "Linux Qt",
      platform: "linux",
      sources,
      body,
      ...extra,
    });
  const linuxHome = (adaptive = false, disconnected = false) =>
    `${disconnected ? '<div style="text-align:center;margin:0 auto 20px;padding:5px;background:#ff453a;border-radius:12px;width:120px;color:white">Disconnected</div>' : ""}<div style="text-align:right">${button("⚙", "linux-settings")}</div>${disconnected ? "" : batteries(true) + modes(adaptive ? 3 : 2, true) + (adaptive ? slider("Adaptive Noise Level", 50, "0", "100") : "") + group("", toggle("Conversational Awareness", false) + toggle("Hearing Aid", true))}`;
  linuxAdd(
    "linux-home",
    "Linux · main window",
    () => linuxHome(),
    [
      "linux/Main.qml",
      "linux/PodColumn.qml",
      "linux/BatteryIndicator.qml",
      "linux/SegmentedControl.qml",
    ],
    {
      note: "Qt widget styling depends on the desktop theme. Reconstructed from QML and the repo screenshot.",
    },
  );
  linuxAdd("linux-adaptive", "Linux · adaptive mode", () => linuxHome(true));
  linuxAdd("linux-disconnected", "Linux · disconnected", () =>
    linuxHome(false, true),
  );
  linuxAdd(
    "linux-settings",
    "Linux · settings",
    () =>
      `<button type="button" class="circle" data-go="linux-home" aria-label="Back">‹</button><h5 class="linux-heading">Settings</h5><label>Pause Behavior When Removing AirPods:<select aria-label="Pause behavior"><option>One Removed</option><option>Both Removed</option><option>Never</option></select></label>${group("", toggle("Cross-Device Connectivity with Android", true) + toggle("Auto-Start on Login", false) + toggle("Enable System Notifications", true) + toggle("One Bud ANC Mode", true))}<div class="linux-form-row"><label>Bluetooth Retry Attempts:</label><input aria-label="Bluetooth Retry Attempts" type="number" min="1" max="10" value="3"></div><div class="linux-form-row">${field("", "AirPods Pro")}${button("Rename")}</div><div class="linux-form-row">${field("Phone MAC", "00:00:00:00:00:00")}${button("Change Phone MAC")}</div>${button("Show Magic Cloud Keys QR", "linux-qr")}`,
    ["linux/Main.qml"],
    {
      note: "All controls from the scrollable settings page. MAC address is a placeholder.",
    },
  );
  linuxAdd(
    "linux-qr",
    "Linux · Magic Cloud Keys QR",
    () =>
      '<div class="qr-placeholder">QR code area<br><br>No real keys included.<br>Placeholder, not a scannable code.</div><p style="text-align:center;line-height:1.5">Scan this QR code to transfer<br>the Magic Cloud Keys to another device</p>',
    ["linux/KeysQRDialog.qml"],
    {
      note: "Intentionally uses a labeled placeholder instead of inventing a working QR code or exposing device keys.",
    },
  );
  linuxAdd(
    "linux-qr-error",
    "Linux · QR generation error",
    () =>
      '<div class="qr-placeholder">Failed to generate QR code</div><p style="text-align:center">Scan this QR code to transfer<br>the Magic Cloud Keys to another device</p>',
    ["linux/KeysQRDialog.qml"],
  );
  linuxAdd(
    "linux-tray",
    "Linux · tray menu",
    () =>
      `<div class="tray-list">${["Open", "Settings"].map((name, i) => `<button type="button" data-go="${i ? "linux-settings" : "linux-home"}">${name}</button>`).join("")}<hr><button type="button" data-local-toggle aria-pressed="false">Toggle Conversational Awareness</button><hr>${["Adaptive", "Transparency", "Noise Cancellation", "Off"].map((name, i) => `<button type="button" data-option aria-pressed="${i === 1}"><span class="checkmark">${i === 1 ? "✓" : ""}</span>${name}</button>`).join("")}<hr><button type="button" data-demo="Preview only. No application was closed.">Quit</button></div>`,
    ["linux/trayiconmanager.cpp"],
  );

  // Existing screenshots are historical references, not newly captured or guaranteed current.
  const refs = [
    ["settings-1", "AirPods settings · upper page"],
    ["settings-2", "AirPods settings · lower page"],
    ["accessibility", "Accessibility"],
    ["customizations-1", "Customizations · upper page"],
    ["customizations-2", "Customizations · lower page"],
    ["debug", "Debug / troubleshooting"],
    ["head-tracking-and-gestures", "Head tracking and gestures"],
    ["hearing-aid", "Hearing Aid"],
    ["hearing-aid-adjustments", "Hearing Aid adjustments"],
    ["hearing-test", "Hearing test"],
    ["long-press", "Press and hold"],
    ["notification-and-qs", "Notification and Quick Settings"],
    ["popup", "Connection popup"],
    ["transparency", "Transparency customization"],
    ["widget", "Widgets"],
  ];
  refs.forEach(([file, name]) =>
    screens.push({
      id: `ref-${file}`,
      name: `Reference · ${name}`,
      category: "Original screenshots",
      platform: "reference",
      sources: [`android/imgs/${file}.png`],
      body: () =>
        `<div class="reference-image"><img loading="lazy" src="../../android/imgs/${file}.png" alt="Original repository screenshot: ${esc(name)}"></div>`,
      note: "Existing repo screenshot. May predate the current navigation, wording and styling. Not editable HTML.",
    }),
  );
  screens.push({
    id: "ref-linux",
    name: "Reference · Linux main window",
    category: "Original screenshots",
    platform: "reference",
    sources: ["linux/imgs/main-app.png"],
    body: () =>
      '<div class="reference-image"><img loading="lazy" src="../../linux/imgs/main-app.png" alt="Original repository screenshot of the Linux Qt app"></div>',
    note: "Existing Qt app screenshot from the repo. Not the Rust rewrite.",
  });
  window.UI_GALLERY = { screens, esc, t };
})();
