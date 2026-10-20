---
name: "Zeekr Companion"
description: "Native Android camera and vehicle utility: incumbent semantic themes and Material controls."
colors:
  light-bg: "#F2F2F3"
  light-surface: "#FAFAFB"
  light-sunken: "#E6E6E8"
  light-panel: "#FAFAFB"
  light-card: "#E6E6E8"
  light-settings-row: "#FAFAFB"
  light-line: "#DDDDE0"
  light-text-primary: "#414347"
  light-text-secondary: "#5F6166"
  light-text-tertiary: "#9EA0A5"
  light-energy: "#F0622D"
  light-energy-text: "#B83E15"
  light-energy-quiet: "#FDEDE6"
  light-on-energy: "#FFFFFF"
  light-recording: "#B32A2F"
  light-on-recording: "#FFFFFF"
  light-recording-quiet: "#FCEBEB"
  light-recording-track: "#F3D3D4"
  light-recording-text: "#B32A2F"
  light-locked: "#E0A100"
  light-preview-frame-background: "#E2E2E4"
  light-nav-ripple-color: "#2A2B2E1F"
  light-dialog-guide-button-ripple: "#00000030"
  light-black: "#000000"
  light-white: "#FFFFFF"
  light-status-overlay-text: "#FFFFFFF2"
  light-status-overlay-text-dim: "#FFFFFFB3"
  light-side-control-background: "#F1F2F4E6"
  light-side-control-icon: "#202124"
  light-side-control-pressed: "#20212430"
  light-camera-label-background: "#F1F2F4E6"
  light-camera-label-text: "#202124"
  light-side-arrow-background: "#F1F2F4E6"
  light-side-arrow-lit: "#087B2C"
  light-side-arrow-unlit: "#61656B"
  dark-bg: "#000000"
  dark-surface: "#2A2C30"
  dark-sunken: "#2E3034"
  dark-panel: "#000000"
  dark-card: "#1F2023"
  dark-settings-row: "#222326"
  dark-line: "#34373B"
  dark-text-primary: "#F0F1F2"
  dark-text-secondary: "#A3A6AB"
  dark-text-tertiary: "#75787D"
  dark-energy: "#F26B38"
  dark-energy-text: "#F58A5E"
  dark-energy-quiet: "#3A2419"
  dark-on-energy: "#FFFFFF"
  dark-recording: "#F59A9D"
  dark-on-recording: "#202124"
  dark-recording-quiet: "#33191A"
  dark-recording-track: "#52282A"
  dark-recording-text: "#F59A9D"
  dark-locked: "#F2B630"
  dark-preview-frame-background: "#1C1D20"
  dark-nav-ripple-color: "#F0F1F21F"
  dark-dialog-guide-button-ripple: "#FFFFFF30"
  dark-black: "#000000"
  dark-white: "#FFFFFF"
  dark-status-overlay-text: "#FFFFFFF2"
  dark-status-overlay-text-dim: "#FFFFFFB3"
  dark-side-control-background: "#212121E6"
  dark-side-control-icon: "#FFFFFF"
  dark-side-control-pressed: "#FFFFFF40"
  dark-camera-label-background: "#000000B3"
  dark-camera-label-text: "#FFFFFF"
  dark-side-arrow-background: "#000000CC"
  dark-side-arrow-lit: "#2BD15B"
  dark-side-arrow-unlit: "#92959B"
typography:
  micro:
    fontFamily: "sans-serif-medium"
    fontSize: "14sp"
    fontWeight: 500
    letterSpacing: "0.03em"
  caption:
    fontFamily: "sans-serif-medium"
    fontSize: "16sp"
    fontWeight: 500
    letterSpacing: "0.03em"
  body:
    fontFamily: "sans-serif-medium"
    fontSize: "18sp"
    fontWeight: 500
    letterSpacing: "0.03em"
  row:
    fontFamily: "sans-serif-medium"
    fontSize: "21sp"
    fontWeight: 500
    letterSpacing: "0.03em"
  title:
    fontFamily: "sans-serif-medium"
    fontSize: "26sp"
    fontWeight: 500
    letterSpacing: "0.03em"
  display:
    fontFamily: "sans-serif-medium"
    fontSize: "28sp"
    fontWeight: 500
    letterSpacing: "0.03em"
rounded:
  control: "10dp"
  thumbnail-small: "6dp"
  settings: "5dp"
spacing:
  control-gap: "8dp"
  gutter: "12dp"
  panel-padding: "16dp"
components:
  light-home-action:
    backgroundColor: "{colors.light-card}"
    textColor: "{colors.light-text-primary}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
    padding: "10dp 8dp"
  light-home-toolbar:
    backgroundColor: "transparent"
    textColor: "{colors.light-text-primary}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
    padding: "10dp 8dp"
  light-record-card-idle:
    backgroundColor: "transparent"
    textColor: "{colors.light-text-primary}"
    rounded: "{rounded.control}"
    padding: "12dp 16dp 12dp 12dp"
  light-layout-choice:
    backgroundColor: "{colors.light-sunken}"
    textColor: "{colors.light-text-primary}"
    typography: "{typography.caption}"
    rounded: "{rounded.control}"
  light-layout-choice-selected:
    backgroundColor: "{colors.light-energy-quiet}"
    textColor: "{colors.light-energy-text}"
    typography: "{typography.caption}"
    rounded: "{rounded.control}"
  light-camera-status:
    backgroundColor: "{colors.light-sunken}"
    textColor: "{colors.light-text-primary}"
    typography: "{typography.caption}"
    rounded: "{rounded.control}"
    padding: "8dp"
  light-camera-status-active:
    backgroundColor: "{colors.light-recording-quiet}"
    textColor: "{colors.light-text-primary}"
    typography: "{typography.caption}"
    rounded: "{rounded.control}"
    padding: "8dp"
  dark-home-action:
    backgroundColor: "{colors.dark-card}"
    textColor: "{colors.dark-text-primary}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
    padding: "10dp 8dp"
  dark-home-toolbar:
    backgroundColor: "transparent"
    textColor: "{colors.dark-text-primary}"
    typography: "{typography.body}"
    rounded: "{rounded.control}"
    padding: "10dp 8dp"
  dark-record-card-idle:
    backgroundColor: "transparent"
    textColor: "{colors.dark-text-primary}"
    rounded: "{rounded.control}"
    padding: "12dp 16dp 12dp 12dp"
  dark-layout-choice:
    backgroundColor: "{colors.dark-sunken}"
    textColor: "{colors.dark-text-primary}"
    typography: "{typography.caption}"
    rounded: "{rounded.control}"
  dark-layout-choice-selected:
    backgroundColor: "{colors.dark-energy-quiet}"
    textColor: "{colors.dark-energy-text}"
    typography: "{typography.caption}"
    rounded: "{rounded.control}"
  dark-camera-status:
    backgroundColor: "{colors.dark-sunken}"
    textColor: "{colors.dark-text-primary}"
    typography: "{typography.caption}"
    rounded: "{rounded.control}"
    padding: "8dp"
  dark-camera-status-active:
    backgroundColor: "{colors.dark-recording-quiet}"
    textColor: "{colors.dark-text-primary}"
    typography: "{typography.caption}"
    rounded: "{rounded.control}"
    padding: "8dp"
---

# Design System: Zeekr Companion

## Overview

**Creative North Star: "The live camera is the main surface"**

Zeekr Companion uses a quiet native Android surface with neutral light and dark themes, orange selection and action accents, and a separate recording-red vocabulary. The owner-approved linked-companions mark provides identity alongside familiar Material controls. The factory-style settings/main refinement uses regular native sans-serif variants, flatter setting surfaces and small corners; the palette and approved logo remain compatible.

The current main-screen composition is documented in `docs/main-screen-design.md`; it is a surface-specific direction, not a mandate to give every settings or media screen a camera stage. `PRODUCT.md` owns product context. Android XML and Java remain the implementation source of truth; these extracted tokens describe them.

**Key Characteristics:**
- Semantic day/night colors resolved by Android resources.
- Native system typography in six named sp roles.
- Flat tonal controls, modest rounded corners and clear touch spacing.
- Truthful camera, recording and pending-change states.

## Colors

The palette combines neutral surfaces with three distinct functional accent families. Frontmatter keys prefix the existing resource name with `light-` or `dark-`; these are documentation names, not resource renames. Opaque Android `#AARRGGBB` values are represented as CSS `#RRGGBB`; translucent colors use CSS `#RRGGBBAA`. The Android values remain authoritative.

### Primary

- **Energy orange:** `energy` is the solid action/switch accent, `energy_text` is the theme-specific text accent, `energy_quiet` is the selected soft fill, and `on_energy` is the contrasting foreground. The approved logo's orange path remains its authored fixed fill; its other path follows `text_primary`.

### Secondary

- **Recording red:** `recording` is the single foreground red for indicators, progress, status and destructive actions in each theme; `recording_text` aliases it for compatibility. `recording_quiet` and `recording_track` remain subdued fills/tracks. `on_recording` contrasts with a solid red fill. Floating controls resolve the app-selected palette live, including forced Light/Dark overrides. Red is not an orange selection state.

### Tertiary

- **Locked amber:** `locked` identifies locked footage.
- **Indicator green:** `side_arrow_lit` identifies an illuminated turn arrow; `side_arrow_unlit` is its inactive counterpart.

### Neutral

- **Background and surfaces:** `bg`, `surface`, `sunken`, `panel`, `card` and `line` supply screen, content, secondary control, settings and separator layers. `panel` is not a generic elevated card: its dark value matches the black background.
- **Text:** `text_primary`, `text_secondary` and `text_tertiary` provide the existing hierarchy. Light primary text uses softer charcoal `#414347`, following the owner’s factory-menu reference photos; regular settings type and the night palette are retained. This is a visual approximation, not a colour sampled from factory software. Legacy resource names alias these semantic roles; do not fork a second palette.
- **Camera chrome:** `preview_frame_background`, `side_control_*`, `camera_label_*` and `side_arrow_background` distinguish controls and labels over imagery. `status_overlay_text` and its dim variant remain light in both themes because they sit over camera masks. `black` and `white` are explicit constants.
- **Native interaction:** `nav_ripple_color` and `dialog_guide_button_ripple` preserve theme-aware press feedback.

**The Separate Meanings Rule.** Orange marks ordinary action and selection. Red marks recording and destructive operations; amber marks locked footage.

## Typography

**Display, body and label font:** Android `sans-serif-medium`, inherited through `TextAppearance.Cam` and the theme. The named base roles use medium weight and the existing letter spacing. Explicit bold brand, recording and section text remains bold. No custom font is introduced. Native Material parent styles supply unspecified line metrics; no new line-height is asserted here.

### Hierarchy

| Native role | Resource | Size | Observed use |
| --- | --- | --- | --- |
| Micro | `text_micro` | 14sp | Counters and secondary recording hint |
| Caption | `text_caption` | 16sp | Status, supporting detail, arrangement tiles |
| Body | `text_body` | 18sp | Home actions, brand, source presets |
| Row | `text_row` | 21sp | Sections, navigation rows, dialog body |
| Title | `text_title` | 26sp | Page and dialog titles |
| Display | `text_display` | 28sp | Large timing and count values |

This is an incumbent six-step native ramp, not an imposed geometric scale. Settings use denser existing role assignments than glanceable home controls. MainActivity's existing local font-resource configuration also affects the rendered main screen; the listed sp values are resource values, not screenshot pixels.

**The Native Scale Rule.** Use the existing named sp roles and native system font scaling; keep literal text sizes out of new core layouts.

## Layout

The current home has an 8dp outer inset, a header above the retained full-width camera stage, and a bottom action dock. The header contains the menu, a 32dp Companion mark and brand, Preview, direct 360° access, Settings and Minimise. Camera status, optional recording statistics and storage sit outside the picture. Minimise uses a horizontal minus glyph, with tap to background and long-press to quit; its accessible name/actions remain explicit. Utility icon buttons are 48dp square; header text actions have a 48dp minimum height. Control and source-pane gaps are 8dp.

The dock is a horizontally scrollable native container with a 272dp recording control and 72dp minimum action height. Visible utility actions share equal widths, filling the remaining viewport beside recording with consistent 8dp gaps. Their readable natural width is bounded to 120–208dp before expansion; narrower viewports scroll instead of shrinking labels. Hidden Straighten consumes no slot or gap, and the remaining utilities refill the space. The dock has one shared top divider, transparent backgrounds and native ripple/focus feedback. Saved driver side reverses the order so recording is nearest that side. Labels wrap and controls grow; Straighten only appears for the applied surround preview outside 360° mode.

The Preview dialog is end-aligned and sized from current usable host bounds: 600dp wide at the verified 1.3 text scale, growing for larger text, capped to the host bounds minus 24dp. Its source presets share a row, Other sources follows, then the three labeled layout choices. Constrained hosts stack presets and layout choices vertically. Content scrolls with a persistent scrollbar; Save/Cancel remain dialog actions. Its internal side padding is 16dp. This is native bounds logic, not a web breakpoint system.

With surround plus other sources, Automatic uses 22:14 source-group weights, Side by side uses equal widths, and Stacked uses equal heights with other sources alongside one another. Without surround, Side by side lays road/cabin panes horizontally; Automatic and Stacked lay them vertically. Single-source previews have no split. Surround retains its existing four-lane grid and saved framing. Reflow changes existing view bounds in place and preserves camera textures and pipeline identity.

Expanded preview hides header and dock while retaining camera status, recording/stop and restore access; Back restores chrome. Recreated Activity state retains expansion and saved source layout. These are the current home conventions, not global layouts for other screens.

## Elevation & Depth

Header controls use tonal separation and zero explicit elevation. The bottom toolbar uses spacing and a shared divider, rather than individual filled tiles. The theme disables elevation color overlays, including dialog surface tinting, to retain neutral surfaces. Native Material dialogs retain their platform-provided window and modal treatment; no CSS shadow or fabricated elevation ramp is prescribed.

**The Flat Controls Rule.** Keep the bottom toolbar transparent at rest, with native press/focus feedback. Recording uses its red indicator and progress; Straighten uses orange text/icon and a checkmark when selected. Header and dialog actions retain their tonal surfaces.

## Shapes

The default control/card radius is 10dp. Factory-style settings rows, sidebar selections and main header actions use 5dp corners. Small gallery thumbnails also have an existing 6dp shape variant. Source-layout choice tiles use the same 10dp corners, with an observed 1dp neutral stroke or 2dp selected orange stroke. Recording uses native circular progress and a drawn circle/stop-square indicator; this geometry conveys action and state. Keep authored vector icons and the approved linked-companions silhouette. This change adds native vector geometry and no shipping raster assets.

## Components

### Native action buttons

`Widget.Cam.HomeAction` extends the existing Material tonal button family: `card` fill, primary text/icon tint, Body text, 26dp inline icon, 8dp icon gap, 10dp corners, 10dp vertical and 8dp horizontal padding, and native theme ripple. Header actions and icon utilities retain their different observed sizes. The dock uses `Widget.Cam.HomeToolbarAction`: the same type, spacing and hit areas, with transparent fill and no stroke. Preserve native click, focus, selected, disabled and accessibility behavior; do not substitute browser hover conventions.

### Recording control and camera status

The wider recording control is a clickable/focusable transparent FrameLayout with native foreground ripple, a 42dp progress indicator with 3dp track, a 26dp drawn record indicator, regular Body title and Caption detail. Screen-off behavior lives in the native long-press Recording details dialog and updates while open. Timing uses tabular figures. Runtime recording state owns title, detail, indicator and progress; it does not fill the toolbar hit area. A 1dp neutral separator sits between recording and the utilities and follows driver side. The main Straighten button opts into flat appearance via an XML attribute; playback Straighten controls keep their existing appearance. The status action wraps its label up to 320dp, with a transparent background, secondary Caption text at rest, recording text colour for live counts, 8dp padding, a 48dp minimum touch height and native ripple. Pending source changes use a separate polite live-region notice.

### Preview selection panel

Quick presets use native AppCompat radio buttons with Body labels and a 100dp minimum height. Other sources uses a full-width Material button, neutral or selected orange-soft fill with the corresponding text accent. Source-layout choices are equal-width native Material buttons with Caption labels, 36dp vector icons and 100dp minimum height. The selected choice is communicated by fill, stroke and accessibility state description; all three names are initially visible in the verified capture. Selection is staged: Save writes preview preferences; Cancel, Back and outside dismissal leave them unchanged. Source changes with different preview keys stay visibly pending until Quit app and reopen; the selector continues to name the applied preview and the notice names the saved next choice. Equivalent source sets do not need a preview restart. A single source explains that source layout does not rearrange Surround’s four views; inactive arrangement choices are hidden and their saved value is retained. Arrangement saves reflow immediately without editing recording.

### Gallery, windows and contextual controls

Gallery opens Videos and Photos. Windows exposes independent Super mirror and 360° sessions with Open/Closed/Paused state; direct header 360° changes the main picture. Keep those destinations and independent session states distinct. Straighten preview changes preview presentation only. These controls reuse the native button/dialog system.

### Documentation and verification

The sidecar's HTML/CSS swatches are explicitly illustrative token previews, not Android widgets, screenshot evidence or shipping assets. Their 1dp-to-1px and 1sp-to-1px translation is for the documentation panel only; native scaling, focus/ripple and font metrics remain governed by Android.

The finish record is in `docs/main-screen-validation.md`. The parent verification reports 1,494 passing JVM tests, eight passing native main-page regressions, successful debug/release/instrumentation builds and debug lint with zero errors. The independent review disposition is SHIP within this home-screen scope after fixing road-only orientation and initially hidden layout labels. Evidence is from an API 32 ARM64 emulator at 2560×1600, 320dpi and font scale 1.3. `.impeccable/review/car-{dark,light}.png`, `car-{dark,light}-preview.png` and `car-dark-windows.png` record the native UI. Black stages and waiting text are expected without a Zeekr feed. This establishes UI behavior, not on-car startup, feed framing, HUD fit, performance or factory-app handover. A browser HTML/CSS detector does not validate this native app.

## Do's and Don'ts

### Do:
- Do resolve colors through the incumbent semantic Android resources in both themes.
- Do use the six TextAppearance.Cam roles and preserve system font scaling.
- Do keep the approved linked-companions vector mark and its geometry.
- Do keep recording, preview selection and independent windows visually and behaviorally distinct.
- Do keep long translated labels readable by wrapping and allowing controls to grow.

### Don't:
- Don't replace missing or stale camera output with a fabricated vehicle scene.
- Don't use recording red as an interchangeable selection accent.
- Don't change recording mapping, quality, source ownership or calibrated projection when arranging preview panes.
- Don't rename the Android package, signing identity, storage folder or repository as a branding side effect.
- Don't claim vehicle fit, startup, frame rate or factory-camera coexistence from emulator screenshots.

### Native interaction polish (2026-10-19)

Main Straighten uses a short label and native long-press tooltip; it shares preview/playback correction and never changes recorded files. Preview keeps the source/reopen warning in the fixed heading, with detailed layout/recording guidance in scrollable content. Windows buttons name their next action and retain live Open/Closed/Paused status. Driver side uses spatial Left/Right options and preserves the existing saved value/default. Playback Home explicitly returns to the camera stage; Back keeps ordinary gallery navigation. Empty video playback keeps navigation/Refresh available and disables media-dependent controls.

Segmented settings use an orange outline with `energy_text` on the neutral track and expose checked radio semantics. The light orange text is #B83E15 for sufficient contrast on tinted surfaces; dark text remains #F58A5E. Main and hosted settings follow the system font scale without forcing a minimum or mutating application resources. Gallery opens directly on Videos with a Photos tab; tiles group date/time/duration, lock and selection announcements. Native XML/Java remains authoritative; browser detector does not apply.

## Factory-style settings surface

Owner-provided 009 factory menu photos and approved main/Picture mockups are the
visual authority for this refinement. Settings rows use `settings_row`
(#FAFAFB light / #222326 dark), 5dp corners and regular sans-serif with zero added
letter spacing. Sidebar labels use the same 16sp Caption-derived variant for all
categories/actions; dense explanations use 14sp, rows 18sp and the page title
21sp. System font scaling is preserved without a forced 1.2× minimum. Do not introduce
per-theme font sizes or autoshrink. The navigation column is 240dp.

Settings rows have a 64dp minimum and 10dp vertical content padding; long content
still grows. Keep 48dp interactive hit areas and the existing slider hit region.
The settings header has a 56dp minimum and its metadata footer a 40dp minimum,
both with content-driven height. Secondary main header/metadata use Caption/Micro
roles; retain the recording control's primary emphasis. Waiting text uses
StaticLayout centring with a left-origin Paint, so centring is applied once.

Blind spot settings use native scrolling tabs: Behavior, Picture, Window and HUD;
selected tabs keep dark/light foreground text and a thin orange underline. The
Picture layout uses a live read-only preview at the right with Left/Right selection
and native Expand/Hide controls. At less than 760dp detail width, controls and
preview stack. Preserve real content, tuning, priority, source and freshness states.
No generated concept camera imagery ships. Missing vehicle output is a truthful
paused/unavailable state. See `docs/factory-style-ui.md` for functional boundaries.


### Concise preview states, 2026-10-19

The camera stage uses a short, distinct label for starting, waiting, busy, reconnecting, unavailable, paused, inactive and unassigned states. A longer wait adds one supporting line with elapsed seconds and a Camera status hint. Accessibility announces state/hint changes rather than every elapsed second. Camera status retains the full scheduler/mapping explanation and read-only diagnostics route. Preview and recording authority stay independent.

### Navigation drawer refinement

The modal drawer shares the Settings palette and typography: neutral `nav_background`,
regular 18sp destination labels (`NavigationMenuTextAppearance` derives from SettingsBody),
14sp secondary/group text, 21sp regular brand title and the existing Companion mark.
Selected rows use the Settings 5dp corner radius. Disable Material active-label bolding;
use a flat 320dp panel without drawer elevation or rounded outer corners. Both main
source layouts include `layout_navigation_drawer.xml`. Rows remain at least 52dp tall,
wrap to two lines and scroll for larger system fonts. The three window/control rows
share native switches and the existing feature authorities. Quit app uses `recording_text`
for its label and power icon in both themes, matching the Settings quit action;
ordinary destinations retain their neutral checked/pressed icon states. 360° window is a manual
service session, rather than a persisted boot toggle or the main preview's 360° mode.

### Preview quick presets, 2026-10-20

Selected recording sources is the first quick preset, followed by Surround · four
views, Front + Rear and Surround + Front + Rear. All four use full-width native
radio rows with 56dp minimum touch targets, avoiding four cramped columns. Other
sources retains the individual front/rear road and cabin choices. Saved values,
staging, Cancel and Save behavior stay unchanged.

### Blind-side expansion, 2026-10-20

Expand uses a temporary tall half-screen panel, independently of the saved popup
aspect ratio. Fill the usable overlay height with existing control margins; use
half the available width, capped at the panel height on extremely wide displays.
Anchor to the half nearest the normal window. Restore its exact original geometry;
never save the expanded size or aspect. Apply this to the separate popup and a
blind-side replacement in Super mirror and the 360° window. The 360° picture stays
proportional inside the larger panel. HUD and normal mirror keep their own geometry
policies. Native overlay bounds/insets drive expanded sizing
and refits while system bars change.


### Shared GPU controls, 2026-10-20

System → GPU image processing owns Off / All / Custom and the five existing/new
rendering paths. Off/All display each effective switch state without editing;
Custom enables individual selection. Retain the custom selection through mode
round trips. The 360° picture section links to this page; remove the competing
Developer switches. Keep the native Settings row styles and theme tokens. The
short visible note explains Straighten and Apply; detailed compatibility/recording
information lives under About GPU image processing. The blind-camera settings
picture follows its renderer and uses the shared raw-input shader when enabled.

### Settings backup, 2026-10-20

System → Settings backup uses the existing native Settings detail page and row typography.
A short non-interactive note names coverage and local-only state. Export reports its saved
path in a persistent dialog. Import lists backups, then uses the shared destructive
confirmation to explain replacement, recording stop and exit/reopen. Cancel preserves
all choices. The last restore result stays on the page after reopening.

### Update offer readability

The available-update dialog uses the existing neutral Material surface and title,
a SettingsTitle version label and SettingsCaption metadata. Notes are a separate
scroll region capped at 42% of the visible activity height and 360dp, with 16sp
regular SettingsCaption text, 4dp extra line spacing, 12dp between changes and
hanging native bullet spans. Existing release-section and version headings use
18sp SettingsBody medium type. The dialog is capped at 760dp width and 94% of
the visible activity width. Later and Download and install use the shared 52dp
actions outside the scrolling notes. No notes are discarded or rewritten.

### HUD body-priority trim, 2026-10-20

HUD road-edge trim retains the car-body edge by camera side and final picture
mirroring, independent of HUD window location. Mirrored Left trims left and anchors
its final-resolution label bottom-right; mirrored Right trims right and labels
bottom-left. Disabling mirroring reverses these picture/label edges. The visible
window retains its existing centre-facing shrink and outer placement, preserving
scale and saved layout. The arrow stays centred after image processing. Popup and
normal mirror labels retain their incumbent positions. The preference key, percent,
vehicle defaults and follow/custom framing choices are retained. Physical HUD
visibility remains subject to projector masking and independent factory graphics.
