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

## Astra/native polish (2026-10-19, after alpha 51)

Read-only Astra design review plus an isolated native accessibility audit retained
this composition and produced the bounded fixes in
[the review record](ui-ux-review-2026-10-19.md). The short Straighten label now has
native long-press help. Preview keeps the restart/recording-stop warning fixed,
uses a 600dp font-aware width and stacks choices when constrained. No named layout
choice or English word splits in the final day/night 1.3-scale captures.

Verification: 1,494 JVM tests pass; debug, instrumentation and release builds pass;
lint reports zero errors (267 warnings remain). The initial 18-test native batch
passed 16: two failures exposed insufficient Preview space and a mismatched
ActivityScenario launcher-intent fixture. Both were corrected and focused Home /
day-night confirmation passed; the final width/word-wrap capture check also passed.
Five additional native gallery/playback/segment checks passed. Existing header
recycle/scroll, local font-resource, staging, texture identity, layout, reset and
translation checks passed in the review batch.

Final local captures: `.impeccable/review/astra-20261019/final/`. Main/Preview/Windows
and About/Quit typography cover both themes; actual empty playback and Interface
are dark-theme captures. The paired playback filename does not prove a separate
light render. Floating-button overlay in playback captures is a separate enabled
window, not part of the new empty state. Native UI source and emulator evidence do
not establish vehicle camera, HUD, 360° speed or full TalkBack behavior. Constrained
vertical reflow and more extreme text scales still need device confirmation.

Final reviewer verdict: Astra approved the refreshed native main, Preview, Windows,
Interface, header typography and empty-playback captures/source, with all six design
findings resolved and no further material UI change requested in the reviewed scope.

## Alpha 52 release validation

`2.11.52-alpha.1` / code 335 packages the flat toolbar and reviewed native UI
polish above. No additional camera or recording pipeline change is introduced by
the version bump. The signed release rebuild, 1,494 JVM tests, three packaging
tests and lint (zero errors) pass. Native evidence is the reviewed implementation
above; the version bump does not imply a new vehicle test. Local APK metadata
confirms package `io.github.dts88.zeekrshortcut`, min SDK 28, target 36, the canonical
`ZeekrCompanion-2.11.52-alpha.1.apk` name and the retained signing certificate
SHA-256 `a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc`.

The tag workflow builds the downloadable artifact independently and stages a
draft for asset, checksum, metadata, signing and notes verification before alpha
publication. Update delivery requires the Alpha channel; Beta/Stable exclude it.

## Source-state and dock follow-up (after alpha 52)

The owner’s car photo shows the combined applied preview with Front + Rear saved
and pending. Source selection is staged; source arrangement only changes separate
source panes, while Surround itself remains the four-view container. The selector
now reports the applied source mode; the notice names the saved next choice.
Equivalent preview-key sets update the label without an unnecessary preview reopen
warning. A single source exposes explanatory copy instead of inactive arrangement
tiles, preserving the saved arrangement. No pipeline or recording selection change
is introduced.

The toolbar keeps the recording action and clip detail, with a 1dp neutral
separator following driver side and a 1.45 recording weight. Screen-off behavior
moves into native long-press Recording details using the existing live hint view
while recording; idle details say behavior is shown during recording. Tap remains
record/stop. No type reduction or individual filled cards are introduced.

Validation uses the existing main-page matrix in day/night, EN/MS/ZH and
900/1280dp widths at 1.3 text scale, plus real staged selection, equivalent-source,
long-press, retained SurfaceTexture/pipeline, expanded-view, mapping-preservation
and playback Home checks. Behavioral checks and final captures were verified in
separate runs. A separator initially used match-parent height inside a wrap-content
control; the native matrix caught the lost preview height and it was corrected to
48dp before completion. Local captures are ignored under
`.impeccable/review/preview-dock-20261019/`; black camera stages remain expected on
the emulator. This is UI evidence, not vehicle camera or recording validation.
All 11 distinct main-page checks passed across focused runs, including the final
day/night capture check. Debug/release/instrumentation builds, 1,494 JVM tests
and lint (zero errors) pass;
no downloadable APK containing this follow-up has been published.


## Factory-style UI follow-up, 2026-10-19

See [factory-style UI validation](factory-style-ui.md). Shared settings typography,
compact equal-width utilities and driver-side anchoring are verified alongside
preserved source/profile choices. A dedicated repeated-driver-side regression
covers native text layout after returning from settings. The native retained-source
Picture preview and its guards still need on-car performance validation. This
follow-up is newer than the alpha 52 downloadable release.

## Shared recording red — 2026-10-20

The owner’s photos showed the main/floating recording indicator and header stop
control using different reds. `recording_text` now aliases `recording`: one
foreground red per palette, retaining the previous readable text shades
(light `#B32A2F`, dark `#F59A9D`). Main dots/progress, header stop glyphs, live
status, gallery and Quit accents therefore resolve to the same colour. Muted
tracks/backgrounds remain separate. `on_recording` supplies white in light mode
and dark text in dark mode on solid red destructive buttons; `colorOnError` uses
that role too. Encoded telemetry artwork/vehicle lamp colours are not UI palette
roles and are unchanged.

The retained floating window observes app-local theme changes. Its button resolves
AppTheme.palette, its progress paint refreshes along with the stop glyph, and its
duration label changes palette in place. Parent and child hardware display lists
are invalidated on a theme transition, without replacing the overlay or changing
recording state, placement, opacity or gestures. Saved whole-window opacity still
blends the selected colour with whatever is underneath it.

Validation:

- 1,528 JVM tests pass; lint has zero errors; signed release/debug/test builds pass.
- Eight targeted native checks pass for floating gestures/actions, theme-aware
  camera controls and drawer authority, including actual composed overlay pixels
  across Light → Dark → Light on the same attached window. Main record-dot/ring
  and header-stop colours match the overlay palette. Text and solid-red action
  foregrounds meet 4.5:1 in the tested palettes; waiting remains neutral. Actual
  light/dark overlay captures were inspected on the disposable API 32 emulator.
- An initial broader UI run passed 22 of 23 cases. The inherited
  `captureLightAndDarkMainPage` case failed because the Preview dialog’s Automatic
  tile was only 70% visible. Running that case on unchanged alpha 57
  (`f82578861e788c696e821d9ef2f16f3f99f221a0`) reproduced the same assertion.
  This is a separate existing dialog-layout issue, not resolved by the colour work.

No physical-car colour matching or new downloadable release is claimed here.
