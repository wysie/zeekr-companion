(() => {
  const $ = (id) => document.getElementById(id);
  const screens = {
    main: [
      "Main screen",
      "Keep surround, front and rear views together, with recording and photo controls along the bottom.",
    ],
    "preview-layout": [
      "Preview & layout",
      "Choose the recording sources preset or a different camera combination, then arrange separate source panes side by side or stacked.",
    ],
    menu: [
      "Side menu",
      "Reach photos, videos and settings, or toggle Super mirror, the 360° window and the floating button.",
    ],
    recording: [
      "Recording & photos",
      "Choose recording sources and per-camera quality. Preview choices stay separate from the sources you record.",
    ],
    "blind-camera": [
      "Blind spot camera",
      "Choose where the turn-signal side camera appears, and adjust its picture, window and HUD settings.",
    ],
    hud: [
      "HUD",
      "Keep HUD position, size and picture settings independent, with visibility calibration for your viewing position. Physical HUD support depends on the vehicle.",
    ],
    "360-camera": [
      "360° camera",
      "Configure the experimental DIY view and popup. Alignment and distances are approximate; this is separate from the factory stitched camera.",
    ],
    "floating-button": [
      "Floating button",
      "Assign tap, double-tap and long-press actions for recording, photos, footage locks and camera windows.",
    ],
    storage: [
      "Storage",
      "Choose a USB destination and retention budgets. Internal storage and relay write require Developer mode.",
    ],
    interface: [
      "Interface",
      "Switch between light and dark themes, choose your language and adjust the controls that appear on screen.",
    ],
    system: [
      "System",
      "Back up preferences, choose your vehicle profile and find GPU processing and troubleshooting tools.",
    ],
    "gpu-processing": [
      "GPU processing",
      "Choose Off, All or Custom for supported camera views and playback. Renderer changes use Apply changes for camera routes.",
    ],
    "super-mirror": [
      "Super mirror",
      "Choose a surround camera for the floating mirror window and configure its picture, controls and framing.",
    ],
  };
  const tabs = [...document.querySelectorAll('[data-screen][role="tab"]')];
  let currentScreen = "main";
  let request = 0;
  let theme = document.documentElement.dataset.theme;
  const source = (screen, full = false) =>
    "assets/screenshots/" +
    screen +
    "-" +
    theme +
    (full ? "" : matchMedia("(max-width: 767px)").matches ? "-800" : "-1600") +
    ".webp";
  const variants = (screen) =>
    "assets/screenshots/" +
    screen +
    "-" +
    theme +
    "-800.webp 800w, assets/screenshots/" +
    screen +
    "-" +
    theme +
    "-1600.webp 1600w";
  const syncTheme = () => {
    const dark = theme === "dark";
    document.documentElement.dataset.theme = theme;
    $("theme-toggle").textContent = dark ? "Light view" : "Dark view";
    $("theme-toggle").setAttribute(
      "aria-label",
      dark
        ? "Light view: switch to light theme"
        : "Dark view: switch to dark theme",
    );
    document.querySelector('meta[name="theme-color"]').content = dark
      ? "#171a1b"
      : "#f1f2ef";
    for (const [name, screen] of [
      ["hero", "main"],
      ["perspective", "360-camera"],
    ]) {
      $(name + "-image").srcset = variants(screen);
      $(name + "-dark-source").media = "not all";
      $(name + "-image").src = source(screen);
    }
    selectScreen(currentScreen);
  };
  const selectScreen = async (screen, focus = false) => {
    if (!screens[screen]) return;
    currentScreen = screen;
    const ticket = ++request;
    for (const tab of tabs) {
      const active = tab.dataset.screen === screen;
      tab.setAttribute("aria-selected", String(active));
      tab.tabIndex = active ? 0 : -1;
      if (active && focus) {
        tab.focus();
        tab.scrollIntoView({ block: "nearest", inline: "nearest" });
      }
    }
    $("screen-panel").setAttribute("aria-labelledby", "tab-" + screen);
    $("screen-panel").setAttribute("aria-busy", "true");
    $("zoom-screen").disabled = true;
    $("gallery-error").hidden = true;
    const next = new Image();
    const url = source(screen);
    next.src = url;
    try {
      await next.decode();
      if (ticket !== request) return;
      $("gallery-image").srcset = variants(screen);
      $("gallery-dark-source").media = "not all";
      $("gallery-image").src = url;
      $("gallery-image").alt =
        screens[screen][0] +
        " in Zeekr Companion, " +
        theme +
        " theme. Example settings; camera scenes are simulated.";
      $("screen-name").textContent = screens[screen][0];
      $("screen-description").textContent = screens[screen][1];
      $("zoom-screen").disabled = false;
    } catch {
      if (ticket !== request) return;
      $("gallery-error").hidden = false;
      $("zoom-screen").disabled = true;
    } finally {
      if (ticket === request)
        $("screen-panel").setAttribute("aria-busy", "false");
    }
  };
  tabs.forEach((tab, index) => {
    tab.addEventListener("click", () => selectScreen(tab.dataset.screen));
    tab.addEventListener("keydown", (event) => {
      let next = index;
      if (event.key === "ArrowDown" || event.key === "ArrowRight")
        next = (index + 1) % tabs.length;
      else if (event.key === "ArrowUp" || event.key === "ArrowLeft")
        next = (index - 1 + tabs.length) % tabs.length;
      else if (event.key === "Home") next = 0;
      else if (event.key === "End") next = tabs.length - 1;
      else return;
      event.preventDefault();
      selectScreen(tabs[next].dataset.screen, true);
    });
  });
  $("retry-image").addEventListener("click", () => selectScreen(currentScreen));
  $("theme-toggle").addEventListener("click", () => {
    theme = theme === "dark" ? "light" : "dark";
    try {
      localStorage.setItem("companion-site-theme", theme);
    } catch {}
    syncTheme();
  });
  matchMedia("(prefers-color-scheme: dark)").addEventListener(
    "change",
    (event) => {
      let stored;
      try {
        stored = localStorage.getItem("companion-site-theme");
      } catch {}
      if (!stored) {
        theme = event.matches ? "dark" : "light";
        syncTheme();
      }
    },
  );
  document
    .querySelectorAll("[data-explore]")
    .forEach((link) =>
      link.addEventListener("click", () => selectScreen(link.dataset.explore)),
    );
  const menu = $("main-nav");
  const setMenu = (open) => {
    menu.classList.toggle("open", open);
    $("menu-toggle").setAttribute("aria-expanded", String(open));
    $("menu-toggle").textContent = open ? "Close" : "Menu";
  };
  $("menu-toggle").addEventListener("click", () =>
    setMenu(!menu.classList.contains("open")),
  );
  menu
    .querySelectorAll("a")
    .forEach((link) => link.addEventListener("click", () => setMenu(false)));
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && menu.classList.contains("open")) {
      const restoreFocus = menu.contains(document.activeElement);
      setMenu(false);
      if (restoreFocus) $("menu-toggle").focus();
    }
  });
  document.addEventListener("click", (event) => {
    if (!event.target.closest(".nav-wrap")) setMenu(false);
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
  const phoneLayout = matchMedia("(max-width: 767px)");
  const syncOrientation = () =>
    document
      .querySelector('[role="tablist"]')
      .setAttribute(
        "aria-orientation",
        phoneLayout.matches ? "horizontal" : "vertical",
      );
  phoneLayout.addEventListener("change", syncOrientation);
  syncOrientation();
  syncTheme();
})();
