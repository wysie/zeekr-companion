(() => {
  const $ = (id) => document.getElementById(id);
  const screens = {
    main: [
      "Main screen",
      "Surround, front and rear previews with recording controls.",
    ],
    "preview-layout": [
      "Preview & layout",
      "Preview sources and pane layout, independent of recording.",
    ],
    menu: [
      "Side menu",
      "Galleries, settings and camera-window switches.",
    ],
    recording: [
      "Recording & photos",
      "Recording sources, per-camera quality and automatic recording.",
    ],
    "blind-camera": [
      "Blind spot camera",
      "Turn-signal side views, framing and output settings.",
    ],
    hud: [
      "HUD",
      "Independent HUD placement and visibility calibration. Vehicle support varies.",
    ],
    "360-camera": [
      "360° camera",
      "Experimental 360° setup. Alignment is approximate; this is not the factory view.",
    ],
    "floating-button": [
      "Floating button",
      "Tap, double-tap and long-press actions.",
    ],
    storage: [
      "Storage",
      "USB recording destination and footage retention limits.",
    ],
    interface: [
      "Interface",
      "Theme, language and on-screen controls.",
    ],
    system: [
      "System",
      "Vehicle profile, backups, GPU processing and troubleshooting.",
    ],
    "gpu-processing": [
      "GPU processing",
      "Off, All or Custom processing for supported camera views and playback.",
    ],
    "super-mirror": [
      "Super mirror",
      "Floating surround-camera window with adjustable framing.",
    ],
  };
  const picker = $("screen-select");
  let currentScreen = "main";
  let request = 0;
  let theme = document.documentElement.dataset.theme;
  const source = (screen, full = false) =>
    `assets/screenshots/${screen}-${theme}${full ? "" : matchMedia("(max-width: 767px)").matches ? "-800" : "-1600"}.webp`;
  const variants = (screen) =>
    `assets/screenshots/${screen}-${theme}-800.webp 800w, assets/screenshots/${screen}-${theme}-1600.webp 1600w`;
  const selectScreen = async (screen) => {
    if (!screens[screen]) return;
    currentScreen = screen;
    const ticket = ++request;
    $("screen-panel").setAttribute("aria-busy", "true");
    $("zoom-screen").disabled = true;
    $("gallery-error").hidden = true;
    // Hide the previous image until the requested screen/theme is ready.
    $("gallery-image").style.visibility = "hidden";
    $("screen-description").textContent = "Loading screenshot…";
    const next = new Image();
    const url = source(screen);
    next.src = url;
    try {
      await next.decode();
      if (ticket !== request) return;
      $("gallery-dark-source").media = "not all";
      $("gallery-image").srcset = variants(screen);
      $("gallery-image").src = url;
      $("gallery-image").alt = `${screens[screen][0]} in Zeekr Companion, ${theme} theme. Camera scenes are simulated.`;
      $("gallery-image").style.visibility = "visible";
      $("screen-description").textContent = screens[screen][1];
      $("zoom-screen").disabled = false;
    } catch {
      if (ticket !== request) return;
      $("gallery-error").hidden = false;
      $("screen-description").textContent = "";
    } finally {
      if (ticket === request) $("screen-panel").setAttribute("aria-busy", "false");
    }
  };
  const syncTheme = () => {
    const dark = theme === "dark";
    document.documentElement.dataset.theme = theme;
    $("theme-toggle").hidden = false;
    $("theme-toggle").textContent = dark ? "Light theme" : "Dark theme";
    $("theme-toggle").setAttribute("aria-label", dark ? "Switch to light theme" : "Switch to dark theme");
    document.querySelector('meta[name="theme-color"]').content = dark ? "#171a1b" : "#f1f2ef";
    selectScreen(currentScreen);
  };
  picker.disabled = false;
  picker.addEventListener("change", () => selectScreen(picker.value));
  $("retry-image").addEventListener("click", () => selectScreen(currentScreen));
  $("theme-toggle").addEventListener("click", () => {
    theme = theme === "dark" ? "light" : "dark";
    try { localStorage.setItem("companion-site-theme", theme); } catch {}
    syncTheme();
  });
  matchMedia("(prefers-color-scheme: dark)").addEventListener("change", (event) => {
    let stored;
    try { stored = localStorage.getItem("companion-site-theme"); } catch {}
    if (!stored) {
      theme = event.matches ? "dark" : "light";
      syncTheme();
    }
  });
  const dialog = $("image-dialog");
  const zoom = $("image-zoom");
  const imageWrap = $("dialog-image-wrap");
  zoom.addEventListener("click", () => {
    const enlarged = !imageWrap.classList.contains("actual-size");
    imageWrap.classList.toggle("actual-size", enlarged);
    zoom.setAttribute("aria-pressed", String(enlarged));
    zoom.textContent = enlarged ? "Fit image" : "Actual size";
  });
  let dialogRequest = 0;
  $("zoom-screen").addEventListener("click", async () => {
    const ticket = ++dialogRequest;
    imageWrap.classList.remove("actual-size");
    imageWrap.scrollLeft = imageWrap.scrollTop = 0;
    zoom.disabled = true;
    zoom.setAttribute("aria-pressed", "false");
    zoom.textContent = "Actual size";
    $("dialog-title").textContent = screens[currentScreen][0] + " · " + theme;
    $("dialog-image").alt =
      screens[currentScreen][0] +
      " in Zeekr Companion. Camera scenes are simulated.";
    $("dialog-image").hidden = true;
    $("dialog-status").textContent = "Loading full-size screenshot…";
    $("dialog-status").hidden = false;
    const full = new Image();
    full.src = source(currentScreen, true);
    dialog.showModal();
    document.body.classList.add("no-scroll");
    try {
      await full.decode();
      if (!dialog.open || ticket !== dialogRequest) return;
      $("dialog-image").src = full.src;
      $("dialog-image").hidden = false;
      zoom.disabled = false;
      $("dialog-status").hidden = true;
    } catch {
      if (dialog.open && ticket === dialogRequest)
        $("dialog-status").textContent =
          "The full-size screenshot could not load. Close this view and try again.";
    }
  });
  $("close-dialog").addEventListener("click", () => dialog.close());
  dialog.addEventListener("close", () => {
    dialogRequest++;
    document.body.classList.remove("no-scroll");
  });
  dialog.addEventListener("click", (event) => {
    if (event.target === dialog) {
      const bounds = dialog.getBoundingClientRect();
      if (
        event.clientX < bounds.left ||
        event.clientX > bounds.right ||
        event.clientY < bounds.top ||
        event.clientY > bounds.bottom
      )
        dialog.close();
    }
  });
  syncTheme();
})();
