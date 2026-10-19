---
name: "Zeekr Companion"
description: "Native Android camera and vehicle utility: incumbent semantic themes and Material controls."
colors:
  light-bg: "#F2F2F3"
  light-surface: "#FAFAFB"
  light-sunken: "#E6E6E8"
  light-panel: "#FAFAFB"
  light-card: "#E6E6E8"
  light-line: "#DDDDE0"
  light-text-primary: "#2A2B2E"
  light-text-secondary: "#5F6166"
  light-text-tertiary: "#9EA0A5"
  light-energy: "#F0622D"
  light-energy-text: "#C9491A"
  light-energy-quiet: "#FDEDE6"
  light-on-energy: "#FFFFFF"
  light-recording: "#E0393E"
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
  dark-line: "#34373B"
  dark-text-primary: "#F0F1F2"
  dark-text-secondary: "#A3A6AB"
  dark-text-tertiary: "#75787D"
  dark-energy: "#F26B38"
  dark-energy-text: "#F58A5E"
  dark-energy-quiet: "#3A2419"
  dark-on-energy: "#FFFFFF"
  dark-recording: "#F0555A"
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
  light-record-card-idle:
    backgroundColor: "{colors.light-sunken}"
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
  dark-record-card-idle:
    backgroundColor: "{colors.dark-sunken}"
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

Zeekr Companion uses a quiet native Android surface with neutral light and dark themes, orange selection and action accents, and a separate recording-red vocabulary. The owner-approved linked-companions mark provides identity alongside familiar Material controls. This records the built system; it introduces no replacement font, palette or logo.

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

- **Recording red:** `recording`, `recording_quiet`, `recording_track` and `recording_text` distinguish the live recording control, progress and status. They are not another orange selection state.

### Tertiary

- **Locked amber:** `locked` identifies locked footage.
- **Indicator green:** `side_arrow_lit` identifies an illuminated turn arrow; `side_arrow_unlit` is its inactive counterpart.

### Neutral

- **Background and surfaces:** `bg`, `surface`, `sunken`, `panel`, `card` and `line` supply screen, content, secondary control, settings and separator layers. `panel` is not a generic elevated card: its dark value matches the black background.
- **Text:** `text_primary`, `text_secondary` and `text_tertiary` provide the existing hierarchy. Legacy resource names alias these semantic roles; do not fork a second palette.
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

The current home has an 8dp outer inset, a header above the retained full-width camera stage, and a bottom action dock. The header contains the menu, a 32dp Companion mark and brand, Preview, direct 360° access, Settings and Minimise. Camera status, optional recording statistics and storage sit outside the picture. Utility icon buttons are 48dp square; header text actions have a 56dp minimum height. Control and source-pane gaps are 8dp.

The dock is a horizontally scrollable native container. The recording card has weight 1.6; the visible secondary actions have equal weight 1 and 80dp minimum height. They are not all equal-width cards. Saved driver side reverses their order so recording is nearest that side: recording, photo, gallery, windows, contextual Straighten preview from the chosen edge. Labels can wrap to four lines and controls can grow. Straighten preview only appears for the applied surround preview outside 360° mode.

The Preview dialog is end-aligned and sized from current usable host bounds: at most 540dp wide, with 24dp removed from host width/height. Its source presets share a row, Other sources follows, then the three labeled layout choices. Content scrolls with a persistent scrollbar; Save/Cancel remain dialog actions. Its internal side padding is 16dp. This is native bounds logic, not a web breakpoint system.

With surround plus other sources, Automatic uses 22:14 source-group weights, Side by side uses equal widths, and Stacked uses equal heights with other sources alongside one another. Without surround, Side by side lays road/cabin panes horizontally; Automatic and Stacked lay them vertically. Single-source previews have no split. Surround retains its existing four-lane grid and saved framing. Reflow changes existing view bounds in place and preserves camera textures and pipeline identity.

Expanded preview hides header and dock while retaining camera status, recording/stop and restore access; Back restores chrome. Recreated Activity state retains expansion and saved source layout. These are the current home conventions, not global layouts for other screens.

## Elevation & Depth

Home controls use tonal separation and zero explicit elevation. The theme disables elevation color overlays, including dialog surface tinting, to retain neutral surfaces. Native Material dialogs retain their platform-provided window and modal treatment; no CSS shadow or fabricated elevation ramp is prescribed.

**The Flat Controls Rule.** Home action controls and the recording card remain flat at rest; tonal contrast provides separation.

## Shapes

The shared control/card radius is 10dp. Small gallery thumbnails also have an existing 6dp shape variant. Source-layout choice tiles use the same 10dp corners, with an observed 1dp neutral stroke or 2dp selected orange stroke. Recording uses native circular progress and a drawn circle/stop-square indicator; this geometry conveys action and state. Keep authored vector icons and the approved linked-companions silhouette. This change adds native vector geometry and no shipping raster assets.

## Components

### Native action buttons

`Widget.Cam.HomeAction` extends the existing Material tonal button family: `card` fill, primary text/icon tint, Body text, 26dp inline icon, 8dp icon gap, 10dp corners, 10dp vertical and 8dp horizontal padding, and native theme ripple. Header actions, dock actions and icon utilities retain their different observed sizes. Preserve native click, focus, selected, disabled and accessibility behavior; do not substitute browser hover conventions.

### Recording card and camera status

The wider recording card is a clickable/focusable flat MaterialCardView using `sunken`, a 48dp progress indicator with 4dp track, a 26dp drawn record indicator, bold Body title and Caption detail. Its secondary hint uses Micro. Timing uses tabular figures. Runtime recording state owns title, detail, progress and active colors. The status action uses `sunken` at rest and `recording_quiet` when activated, primary Caption text, 8dp padding, 48dp minimum height and a native ripple. Pending source changes use a separate polite live-region notice.

### Preview selection panel

Quick presets use native AppCompat radio buttons with Body labels and a 100dp minimum height. Other sources uses a full-width Material button, neutral or selected orange-soft fill with the corresponding text accent. Source-layout choices are equal-width native Material buttons with Caption labels, 36dp vector icons and 100dp minimum height. The selected choice is communicated by fill, stroke and accessibility state description; all three names are initially visible in the verified capture. Selection is staged: Save writes preview preferences; Cancel, Back and outside dismissal leave them unchanged. Source changes stay visibly pending until Quit app and reopen; arrangement saves reflow immediately without editing recording.

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
