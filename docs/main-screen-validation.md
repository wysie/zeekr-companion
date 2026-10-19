# Camera-first home screen validation — 2026-10-19

## Scope

Replaces the permanent action column with a full-width camera stage, header and
bottom dock. New source-layout preferences only change existing view bounds;
they are excluded from camera-pipeline shape and recording profiles. Existing
source presets, mapping, per-lane framing, window placement and recording authority
remain in their existing stores. The feature itself did not change the release version or update destination.

## Checks

- All 1,494 JVM tests pass, including source-selection and resource-parity regressions.
- Debug APK, instrumentation APK, release APK and debug lint complete successfully;
  lint reports 0 errors. No lint finding remains in the changed home-screen files.
- Native `MainPageControlsAndroidTest` covers retained SurfaceTextures and pipeline
  across expansion and source-pane reflow, recreation/Back restoration, source
  selection cancellation/save/pending notice, profile/tuning isolation, individual
  source selection and contextual controls, and independent 360° window status.
- The sizing matrix inflates both main layouts at 900/1280dp widths, font scale 1.3,
  English/Malay/Chinese and both themes. It includes the optional vehicle-status
  panel and waiting-recording state, checks untruncated labels and reserves at least
  200dp of camera-stage height.
- Road-only layout verification distinguishes Side by side (horizontal), Stacked
  (vertical) and Automatic (original vertical), retaining the existing textures
  and camera pipeline.
- Native light/dark Preview captures assert that all three named layout choices
  are completely displayed immediately on opening, without scrolling.

## Design review corrections

An independent native design review identified road-only layout directions that
contradicted their labels, plus layout labels below the initial Preview viewport.
The orientation now follows the visible source composition. Quick presets share
one row, the title uses less padding and the scroller has a persistent scrollbar;
the corrected panel retains normal-size text and anchored Save/Cancel actions.

## Evidence limits

The disposable API 32 ARM64 emulator uses a 2560×1600 override, 320dpi and system
font scale 1.3. Native screenshots are local under `.impeccable/review/`. Black
surround panes and waiting messages are expected because it has no Zeekr composite
camera. They prove UI layout/interaction only. Real 009/7X feed framing, on-car
startup, GPU/CPU performance, HUD and factory-app handover remain vehicle checks.
The redesign does not claim to fix those hardware-dependent behaviors.

## Final result

All eight native main-page tests pass. The independent reviewer scored both
reported findings resolved and returned **SHIP** for that correction pass. This
is a scoped UI review, not certification of the whole app or either vehicle.
Included in **v2.11.51-alpha.1**; the separate release preparation increments the
Android version to 2.11.51-alpha.1 / 334 and retains the existing package/signing identity.

## Flat toolbar refinement (after alpha 51)

The bottom dock now uses transparent icon/label actions and one shared divider.
Recording retains its existing state controller and red indicator/progress in a
transparent native hit area; press/focus feedback remains native. The main
Straighten widget opts into transparent styling and orange/checkmark selection;
playback widgets retain their previous appearance.

Debug/instrumentation builds and lint pass. Reused native sizing and screenshot
checks pass (2 tests), including both main layouts, English/Malay/Chinese, 900/1280dp
widths, font scale 1.3 and both themes. Captures cover neutral and selected toolbar
states under `.impeccable/review/flat-toolbar/`. No camera/recording settings change.
This visual refinement is on main after alpha 51 and is not part of that APK.
