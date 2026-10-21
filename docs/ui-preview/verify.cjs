// Offline structural checks. Run from any directory with Node; no packages needed.
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const assert = require("node:assert/strict");
const here = __dirname;
const root = path.resolve(here, "../..");
const context = { window: {} };
vm.createContext(context);
vm.runInContext(
  fs.readFileSync(path.join(here, "source-data.js"), "utf8"),
  context,
  { filename: "source-data.js" },
);
const missingStrings = new Set();
context.window.SOURCE_DATA.strings = new Proxy(
  context.window.SOURCE_DATA.strings,
  {
    get(target, key) {
      if (!(key in target)) missingStrings.add(key);
      return target[key];
    },
  },
);
vm.runInContext(
  fs.readFileSync(path.join(here, "screens.js"), "utf8"),
  context,
  { filename: "screens.js" },
);
const screens = context.window.UI_GALLERY.screens;
const ids = new Set();
for (const screen of screens) {
  assert(!ids.has(screen.id), `Duplicate id: ${screen.id}`);
  ids.add(screen.id);
  assert(screen.body().length > 0, `Empty screen: ${screen.id}`);
  for (const source of screen.sources)
    assert(fs.existsSync(path.join(root, source)), `Missing source: ${source}`);
}
assert.equal(
  missingStrings.size,
  0,
  `Unresolved app strings: ${[...missingStrings].join(", ")}`,
);
for (const screen of screens) {
  for (const [, target] of screen.body().matchAll(/data-go="([^"]+)"/g))
    assert(ids.has(target), `Broken navigation ${screen.id} → ${target}`);
  for (const [, asset] of screen.body().matchAll(/src="([^"]+)"/g))
    assert(fs.existsSync(path.resolve(here, asset)), `Missing asset ${asset}`);
}
const presentation =
  "android/app/src/main/java/me/kavishdevar/librepods/presentation";
const routeSource = fs
  .readFileSync(path.join(root, presentation, "navigation/Screen.kt"), "utf8")
  .replace(/\/\/[^\n]*/g, "");
const routes = [...routeSource.matchAll(/data (?:object|class) (\w+)/g)].map(
  (match) => match[1],
);
for (const route of routes)
  assert(
    screens.some((s) => s.route === route),
    `Uncovered active route: ${route}`,
  );
assert.deepEqual(
  [...context.window.SOURCE_DATA.routes].sort(),
  [...routes].sort(),
  "Refresh generated route snapshot",
);
const mapped = new Set(screens.flatMap((s) => [...s.sources]));
for (const dir of ["screens", "screens/onboarding"]) {
  for (const file of fs
    .readdirSync(path.join(root, presentation, dir))
    .filter((f) => f.endsWith(".kt"))) {
    assert(
      mapped.has(`${presentation}/${dir}/${file}`),
      `Uncovered screen file: ${dir}/${file}`,
    );
  }
}
for (const file of fs
  .readdirSync(path.join(root, "android/app/src/main/res/layout"))
  .filter((f) => f.endsWith(".xml"))) {
  assert(
    mapped.has(`android/app/src/main/res/layout/${file}`),
    `Uncovered XML layout: ${file}`,
  );
}
for (const file of fs
  .readdirSync(path.join(root, "android/imgs"))
  .filter((f) => f.endsWith(".png")))
  assert(mapped.has(`android/imgs/${file}`), `Uncovered reference: ${file}`);
// Inventory all top-level QML pages/dialogs; shared components are covered by main.
for (const file of ["Main.qml", "KeysQRDialog.qml"])
  assert(mapped.has(`linux/${file}`), `Uncovered Linux page: ${file}`);
for (const file of ["app.js", "screens.js", "source-data.js"])
  new vm.Script(fs.readFileSync(path.join(here, file), "utf8"), {
    filename: file,
  });
console.log(
  `PASS: ${screens.length} previews, ${routes.length} Android routes, all screen files and XML layouts, source paths, assets and navigation targets.`,
);
