(() => {
  "use strict";
  const { screens, esc } = window.UI_GALLERY;
  const $ = (id) => document.getElementById(id);
  const categories = [...new Set(screens.map((s) => s.category))];
  const settingsKey = "librepods-ui-gallery-settings-v1";
  const notesKey = "librepods-ui-gallery-notes-v1";
  let notes = {};
  let activeScreen;
  let toastTimeout;
  let storageWorks = true;
  function load(key, fallback) {
    try {
      return JSON.parse(localStorage.getItem(key)) || fallback;
    } catch {
      storageWorks = false;
      return fallback;
    }
  }
  function save(key, value) {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch {
      storageWorks = false;
      toast("Browser storage unavailable. Export notes before closing.");
    }
  }
  function toast(message) {
    $("toast").textContent = message;
    $("toast").style.display = "block";
    clearTimeout(toastTimeout);
    toastTimeout = setTimeout(() => {
      $("toast").style.display = "none";
    }, 3500);
  }
  notes = load(notesKey, {});
  if (!notes || typeof notes !== "object" || Array.isArray(notes)) notes = {};
  const preferences = load(settingsKey, {});
  categories.forEach((category) =>
    $("category").add(new Option(category, category)),
  );
  for (const key of ["theme"]) {
    if ([...$(key).options].some((o) => o.value === preferences[key]))
      $(key).value = preferences[key];
  }
  const sectionId = (category) =>
    "section-" + category.toLowerCase().replace(/[^a-z0-9]+/g, "-");
  function preview(screen) {
    if (screen.platform === "reference") return screen.body();
    const linux = screen.platform === "linux";
    const title = screen.title || screen.name;
    return `<div class="device ${linux ? "linux-device" : ""}" data-theme="${$("theme").value}" data-screen="${screen.id}">${linux ? '<div class="linux-titlebar"><span>◈</span><span>LibrePods</span><span>⌄ ⌃ ×</span></div>' : '<div class="statusbar"><span>9:41</span><span>▴ ▰ 87%</span></div>'}${!screen.noNav && !linux ? `<div class="app-nav">${screen.home ? '<span class="nav-spacer"></span>' : '<button type="button" class="circle" data-go="home" aria-label="Back to AirPods settings">‹</button>'}<h4>${esc(title)}</h4>${screen.home ? '<button type="button" class="circle" data-go="settings" aria-label="Settings">⚙</button>' : screen.id === "head-tracking" ? '<button type="button" class="circle" data-local-toggle aria-pressed="false" aria-label="Start or pause sample tracking">▷</button>' : '<span class="nav-spacer"></span>'}</div>` : ""}<div class="screen-body">${screen.body()}</div>${!linux ? '<div class="homebar" aria-hidden="true"></div>' : ""}</div>`;
  }
  function sourceLinks(screen) {
    return `<div class="source-list">${screen.sources.map((path) => `<a href="../../${esc(path)}" target="_blank" rel="noopener">${esc(path)}</a>`).join("")}</div>`;
  }
  function render() {
    const query = $("search").value.trim().toLowerCase();
    const filtered = screens.filter((screen) => {
      const haystack = [
        screen.name,
        screen.category,
        screen.route,
        screen.note,
        ...screen.sources,
        screen.body().replace(/<[^>]*>/g, " "),
      ]
        .join(" ")
        .toLowerCase();
      return (
        ($("platform").value === "all" ||
          screen.platform === $("platform").value) &&
        ($("category").value === "all" ||
          screen.category === $("category").value) &&
        (!query || haystack.includes(query))
      );
    });
    $("count").textContent =
      `${filtered.length} of ${screens.length} previews · ${window.SOURCE_DATA.routes.length} Android routes covered · source ${window.SOURCE_DATA.commit}`;
    $("empty").hidden = filtered.length > 0;
    $("gallery").classList.toggle("expanded", $("expand").checked);
    const visibleCategories = categories.filter((c) =>
      filtered.some((s) => s.category === c),
    );
    $("section-links").innerHTML = visibleCategories
      .map((c) => `<a href="#${sectionId(c)}">${esc(c)}</a>`)
      .join("");
    $("gallery").innerHTML = visibleCategories
      .map((category) => {
        const entries = filtered.filter((s) => s.category === category);
        return `<section class="gallery-section" id="${sectionId(category)}"><div class="section-title"><h2>${esc(category)}</h2><span>${entries.length} previews</span></div><div class="screen-grid">${entries.map((screen) => `<article class="screen-card" id="card-${screen.id}"><div class="card-heading"><div><span class="number">${String(screens.indexOf(screen) + 1).padStart(2, "0")} / ${screen.platform === "reference" ? "REPO IMAGE" : screen.platform.toUpperCase()}</span><h3>${esc(screen.name)}</h3></div><button type="button" data-open="${screen.id}" aria-label="Open ${esc(screen.name)}">Open ↗</button></div>${preview(screen)}<p class="caption">${esc(screen.note || "Source-based HTML preview. Sample settings; controls are local only.")}</p><details><summary>Source files</summary>${sourceLinks(screen)}</details></article>`).join("")}</div></section>`;
      })
      .join("");
    initializeOptions($("gallery"));
  }
  function initializeOptions(root) {
    root.querySelectorAll("[data-screen]").forEach((device) => {
      const screen = screens.find((s) => s.id === device.dataset.screen);
      if (screen.multiLast) {
        const groups = device.querySelectorAll(".group .rows");
        const group = groups[groups.length - 1];
        group.dataset.multi = "true";
        group.querySelectorAll("[data-option]").forEach((option, i) => {
          option.setAttribute("aria-pressed", String(i > 0));
          option.querySelector(".checkmark").textContent = i > 0 ? "✓" : "";
        });
      }
    });
  }
  function openScreen(id, updateHash = true) {
    const screen = screens.find((s) => s.id === id);
    if (!screen) return;
    // Keep in-progress notes when a preview is replaced by a linked destination.
    persistNote();
    activeScreen = screen;
    $("detail-title").textContent = screen.name;
    $("detail-meta").textContent =
      `${screen.platform} / ${screen.category} / ${screen.id}`;
    $("detail-preview").innerHTML = preview(screen);
    $("detail-info").innerHTML =
      `<p class="muted">${esc(screen.note || "HTML approximation of the existing screen. All values are sample data and controls are local.")}</p>${screen.route ? `<p class="muted">Android route: <code>${esc(screen.route)}</code></p>` : ""}<p class="source-title">Source files</p>${sourceLinks(screen)}<label class="notes-label" for="screen-notes">Changes you want on this screen</label><textarea id="screen-notes" class="notes" placeholder="Layout, spacing, colors, anything to change…"></textarea><p class="muted" style="font-size:12px">Notes ${storageWorks ? "save in this browser" : "are in memory only"}. Use Export notes to keep a copy.</p><button type="button" data-copy-link="${screen.id}">Copy screen link</button>`;
    $("screen-notes").value =
      typeof notes[screen.id] === "string" ? notes[screen.id] : "";
    initializeOptions($("detail-preview"));
    if (!$("detail").open) $("detail").showModal();
    $("detail").scrollTop = 0;
    if (updateHash)
      history.replaceState(null, "", `#screen=${encodeURIComponent(id)}`);
  }
  function persistNote() {
    const input = $("screen-notes");
    if (!activeScreen || !input) return;
    const value = input.value.trim();
    if (value) notes[activeScreen.id] = value;
    else delete notes[activeScreen.id];
    save(notesKey, notes);
  }
  function clearDetailState() {
    persistNote();
    if (location.hash.startsWith("#screen="))
      history.replaceState(null, "", location.pathname + location.search);
  }
  function openHash() {
    if (location.hash.startsWith("#screen="))
      openScreen(decodeURIComponent(location.hash.slice(8)), false);
  }
  ["search", "platform", "category"].forEach((id) =>
    $(id).addEventListener(id === "search" ? "input" : "change", render),
  );
  $("theme").addEventListener("change", () => {
    save(settingsKey, { theme: $("theme").value });
    render();
    if ($("detail").open && activeScreen) openScreen(activeScreen.id, false);
  });
  $("expand").addEventListener("change", () =>
    $("gallery").classList.toggle("expanded", $("expand").checked),
  );
  $("reset").addEventListener("click", () => {
    $("search").value = "";
    $("platform").value = "all";
    $("category").value = "all";
    render();
  });
  $("close-detail").addEventListener("click", () => {
    clearDetailState();
    $("detail").close();
  });
  $("detail").addEventListener("close", clearDetailState);
  document.addEventListener("input", (event) => {
    const target = event.target;
    if (target.id === "screen-notes") persistNote();
    if (target.matches("input[type=range]")) {
      const output = target.closest(".range-card")?.querySelector("output");
      if (output) output.value = target.value;
      if (target.hasAttribute("data-volume"))
        target.closest(".qs-stage").querySelector(".volume-fill").style.height =
          `${target.value}%`;
      if (target.hasAttribute("data-eq")) {
        const card = target.closest(".eq-card");
        const values = [...card.querySelectorAll("[data-eq]")].map(
          (input) => 180 - Number(input.value) * 1.6,
        );
        card
          .querySelector(".eq-path")
          .setAttribute(
            "d",
            `M20 ${values[0]} Q85 ${values[0]} 150 ${values[1]} T280 ${values[2]}`,
          );
        card
          .querySelectorAll("circle")
          .forEach((circle, i) => circle.setAttribute("cy", values[i]));
      }
    }
    if (target.hasAttribute("data-license-search")) {
      const query = target.value.toLowerCase();
      target
        .closest(".screen-body")
        .querySelectorAll(".library")
        .forEach(
          (lib) => (lib.hidden = !lib.dataset.libraryName.includes(query)),
        );
    }
  });
  document.addEventListener("change", (event) => {
    if (event.target.hasAttribute("data-hearing-aid") && event.target.checked) {
      event.target.checked = false;
      openScreen("hearing-confirm");
    }
  });
  document.addEventListener("click", async (event) => {
    const target = event.target.closest("button");
    if (!target) return;
    if (target.dataset.open) openScreen(target.dataset.open);
    if (target.dataset.go) openScreen(target.dataset.go);
    if (target.dataset.demo) toast(target.dataset.demo);
    if (target.hasAttribute("data-clear")) {
      target.previousElementSibling.value = "";
      target.previousElementSibling.focus();
    }
    if (target.hasAttribute("data-grant")) {
      target
        .closest(".onboard")
        .querySelectorAll("input[type=checkbox]")
        .forEach((input) => (input.checked = true));
      toast("Preview permissions enabled. System permissions are unchanged.");
    }
    if (target.hasAttribute("data-gesture"))
      target.parentElement.querySelector("[data-gesture-result]").textContent =
        '"Yes" gesture detected. Sample result.';
    if (target.hasAttribute("data-mode")) {
      target.parentElement.querySelectorAll("[data-mode]").forEach((button) => {
        button.classList.toggle("selected", button === target);
        button.setAttribute("aria-pressed", String(button === target));
      });
    }
    if (target.hasAttribute("data-option")) {
      const parent = target.parentElement;
      const selected = target.getAttribute("aria-pressed") === "true";
      parent.querySelectorAll("[data-option]").forEach((option) => {
        const on = parent.hasAttribute("data-multi")
          ? option === target
            ? !selected
            : option.getAttribute("aria-pressed") === "true"
          : option === target;
        option.setAttribute("aria-pressed", String(on));
        option.querySelector(".checkmark").textContent = on ? "✓" : "";
      });
      const eqSection = target
        .closest(".screen-body")
        .querySelector("[data-custom-eq]");
      if (eqSection)
        eqSection.hidden =
          target.querySelector(".row-main").textContent.trim() !== "Custom";
    }
    if (target.hasAttribute("data-reset-eq"))
      target
        .closest(".screen-body")
        .querySelectorAll("[data-eq]")
        .forEach((input) => {
          input.value = 50;
          input.dispatchEvent(new Event("input", { bubbles: true }));
        });
    if (target.hasAttribute("data-local-toggle")) {
      const on = target.getAttribute("aria-pressed") !== "true";
      target.setAttribute("aria-pressed", String(on));
      target.style.opacity = on ? "1" : ".5";
    }
    if (target.dataset.copyLink) {
      const url = new URL(location.href);
      url.hash = `screen=${target.dataset.copyLink}`;
      try {
        await navigator.clipboard.writeText(url.href);
        toast("Screen link copied.");
      } catch {
        toast("Copy this page address from your browser to share this screen.");
      }
    }
  });
  $("export-notes").addEventListener("click", () => {
    persistNote();
    const data = {
      project: "LibrePods",
      sourceCommit: window.SOURCE_DATA.commit,
      exportedAt: new Date().toISOString(),
      notes: Object.entries(notes).map(([id, text]) => ({
        id,
        name: screens.find((s) => s.id === id)?.name || id,
        notes: text,
      })),
    };
    const url = URL.createObjectURL(
      new Blob([JSON.stringify(data, null, 2)], { type: "application/json" }),
    );
    const a = document.createElement("a");
    a.href = url;
    a.download = "librepods-design-notes.json";
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  });
  window.addEventListener("hashchange", openHash);
  render();
  openHash();
})();
