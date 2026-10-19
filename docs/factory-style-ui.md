# Factory-style UI, 2026-10-19

The owner supplied five 009 factory menu photos and approved separate main-screen
and Blind spot camera Picture concepts. The native app implements their layout
language, retaining the Companion mark and real preferences. Concept camera photos
and approximate generated icons are not shipping assets. No source/Ferix code was
imported in this change.

## Main and shared settings

- Equal inline toolbar utilities fill the space beside a driver-side
  recording control, with consistent gaps and no flexible spacer. Recording retains click-to-start/stop, progress, native
  accessibility and live long-press details. Narrow widths can scroll.
- Regular native settings type; all sidebar entries including About/Quit use the
  same 16sp label, no autoshrink, with system scaling and wrapping. A 240dp sidebar
  and 5dp row/header corners reduce visual weight. Other screen typography stays
  in its existing named roles. Theme changes never choose different text sizes.
- Flat `settings_row` surfaces (#FAFAFB light / #222326 dark), neutral navigation
  selection, orange-underlined native tabs and outlined selected segments. Selected
  segments preserve checked RadioButton accessibility semantics.

## Blind spot camera

Behavior, Picture, Window and HUD are peer tabs. Switching them does not create
Back history or reset values. The selected fragment/tab survives Activity
recreation. Existing deep routes and other settings categories remain available.
Behavior keeps automatic enable, output, mirror relationship, timing, minimum speed
and reset. Picture groups framing and side-image tuning. Window keeps geometry;
HUD keeps its independent follow/custom picture, placement, tone and advanced
routing. The owner’s saved 009 values are not reapplied or reset by this redesign.

Long technical explanations remain in About picture/behavior settings. Essential
factory priority, HUD fallback and unchanged-recording consequences remain visible.
Picture tuning can be adjusted before enabling the automatic popup. Sliders retain
the existing save/apply-on-release policy; no new Save-all step exists.

## Read-only settings preview

Picture has Left/Right selection, temporary Expand/Restore and Hide/Show controls.
They do not enable, dismiss or reconfigure actual popup/HUD/Super mirror services.
The frame fits the saved popup aspect and uses the same geometry/projection,
rotation, mirroring, Auto-fit and label helpers as the existing side view. A wide
detail pane uses a 44:56 controls/preview split; below 760dp it stacks.

The sampler reads `MainActivity`'s already-attached native surround TextureView,
never calls camera initialization/declare/claim APIs, and never adds Camera2 outputs.
Opaque settings keep native consumers attached behind them; the hidden main
composite draws its native texture once, suspending four-lane correction and DIY
360 work while preserving its selected mode. Returning to cameras restores that
presentation. Recording authority, source/profile mappings, HAL sizes, HUD routing,
factory arbitration and actual side-popup rendering are unchanged.

Sampling runs only while Picture is started and visible, at up to five samples per
second with a reused bitmap capped at 2048 pixels along the strip (about 512 per
lane for the known composite). It skips duplicate timestamps. This is a tuning
preview, not a quality/FPS change to the actual blind camera. Closing/pausing the
page stops callbacks and releases the sample reference.

Reverse/factory camera views, screen-off, missing car connection, unavailable
composite, failed readback and stale/nonprogressing textures cover the preview.
A source must advance before it becomes live. GPU-corrected main textures are
explicitly unavailable for this raw sampler to avoid double correction. Select a
main preview containing Surround and quit/reopen if that source is not attached;
turn experimental GPU preview correction off and reopen when applicable.

## Density and navigation follow-up

After comparing native captures with the same factory photos, the owner approved
a denser refinement. MainActivity no longer forces at least 1.2× font scaling;
Main and its hosted settings inherit the system size, including enlarged text.
Base sidebar/row/explanation type remains 16/18/14sp with identical metrics across
themes. Content rows change from 72dp to 64dp minimum height and vertical padding
from 14dp to 10dp; slider hit regions and 48dp action targets are retained. Header
and footer minimums are 56dp and 40dp with content-driven height. Secondary main
brand and technical metadata use the existing Caption/Micro roles.

Gallery opens directly on Videos; Photos is a peer tab. The explicit photo and
record controls and expanded-preview Restore are retained. No gallery preference
or camera/source configuration is migrated. Camera-state text now uses a left
Paint origin with centred StaticLayout lines: the former double centring shifted
the already-centred glyphs left and cut off the beginning of the message.

This follows the factory's density and hierarchy; the photographed font family,
exact OEM dimensions and physical fit are not established from oblique photos.

Follow-up validation: debug/release/instrumentation builds, all 1,502 JVM tests
and lint pass. Native runs pass 7/7 at system text 1.0 and 11/11 at 1.3 (11
distinct methods). Coverage includes direct Gallery → Photos → Back with retained
pipeline/manual-stop state, unchanged system font scale in the actual Activity,
same Picture textures/holders across settings, long values/translations, equal
toolbar widths and recycled About/Quit typography. Rendered-pixel checks cover
320/640/900px camera panes at both text scales and retain live/stale accessibility.

Paired actual captures are local-only under
`artifacts/factory-density-20261019/{normal,large}/`. The bounded independent UI
review passed: no further material changes requested. Native pixel inspection
also confirmed white waiting glyphs in the actual dark PNG despite a discrepancy
in the image viewer; no speculative Force Dark or renderer changes were made.
Emulator captures do not establish on-car physical readability or camera quality.

## Initial validation

Validation completed on API 32 ARM64, 2560×1600 at density 320 and 1.3× system
font scale. Debug/release APKs and instrumentation compile; 1,494 JVM tests pass;
lint reports zero errors (272 warnings). Native checks passed 34 distinct methods
across scoped runs. The wider run passed 32/33; the new renderer fixture initially
omitted the registered composite ID. That fixture was corrected and passed in the
final 6/6 confirmation together with tabs, responsive capture, repeated driver-side
toolbar sizing, translations and source-arrangement preservation.

Coverage includes same TextureView/SurfaceTexture/pipeline/holder identity across
real settings navigation, unchanged profile/preferences, tab restoration, tuning
while automatic popup is off, Expand/Hide behavior, both themes, 900/1280dp home
layouts with English/Malay/Chinese, a 640dp stacked settings pane, uniform recycled
sidebar text, existing HUD follow/custom semantics and existing side-rendering
coverage. A synthetic colored source fixture verifies actual Left/Right selection,
straighten on/off and black stale frames; it is test-only imagery. The final narrow
status check asserts complete text and no vertical clipping.

The batched visual review covered light/dark main, Picture, Behavior and HUD;
functional corrections addressed brand wrapping, pre-enable tuning, long help,
attached-button measurement and narrow status clipping. Named captures stay in
ignored `.impeccable/review/factory-style-20261019/`; logs stay in local `/tmp/`
`companion-factory-…` build/native files. No private owner dump or concept photo
is staged. Verdict: native UI ready for vehicle testing.
An emulator establishes UI, geometry/state and ownership behavior, not vehicle
camera quality, smoothness, HUD fitting or firmware compatibility. The 009/7X
live settings sampler and retained-camera behavior still need an on-car check.


## Main status and toolbar follow-up

The owner approved removing the remaining oversized Camera status treatment,
unbalanced toolbar gap and long technical preview messages. The status action
now wraps its label instead of filling half the header, with a transparent native
ripple and the same 48dp touch target, live counts and troubleshooting destination.

The flexible spacer is removed. Visible utility actions fill the remaining width
equally beside the driver-side recording control, with 8dp gaps. Visibility changes
refill the row, including when Straighten is unavailable. Readable natural widths
and horizontal scrolling remain for narrow hosts and enlarged/translated labels.
Preview error/wait states use concise labels and, after ten seconds, one supporting
line with elapsed time and a Camera status hint. Full diagnostic explanations are
retained in Camera status; announcements exclude per-second elapsed updates.

This is presentation only: recording requests, camera sessions, source/profile
choices, HUD placement, factory priority and freshness checks retain their rules.

Validation for this follow-up: all 1,505 JVM tests, debug lint and debug/release
assembly pass. Native checks pass 11/11 at normal system text and 5/5 at 1.3×
(11 distinct methods). They cover equal utility widths and fixed gaps on both
driver sides, full toolbar width with Straighten hidden, compact status navigation
with the same camera manager, recording details, Gallery/Photos/Back, expanded
preview recreation, translated layouts and drawn/accessible waiting messages.
Actual light/dark captures at both text sizes were reviewed together under ignored
`artifacts/main-polish-20261019/{normal,large}/`. The white-on-black glyph display
discrepancy noted above can still affect the image viewer at normal text size;
production paint colour and native pixel assertions remain unchanged/passing.
The release certificate is unchanged. These checks establish UI layout/navigation,
not vehicle-camera quality or physical HUD clearance.


## Minimise icon clarification

The header’s down-arrow-and-baseline glyph resembled a download action. It now
uses a standard horizontal minus, in the existing 24dp vector grid and stroke.
Tap still calls `moveTaskToBack(true)`; long-press still invokes Quit app. The
accessible name and named Quit app long-click action are retained. Debug assembly
and lint pass; this artwork-only correction adds no interaction or camera changes.

## Navigation drawer follow-up

The drawer now uses the Settings palette and regular 18sp labels, including About
and Quit. A compact logo header, neutral non-elevated 320dp panel, consistent native
switches and wrapping 52dp-minimum rows replace its older independent presentation.
Both main camera layouts share one drawer XML. The selected destination respects
restored Settings state after Activity/theme recreation.

A manual **360° window** switch sits below Super mirror and controls the same
`BirdEyePopupService` session as Settings and Windows. The visible drawer reconciles
that service's state through the existing main-page refresh; external stop/X and
start/refusal update the switch. Factory/screen suspension leaves the requested
session enabled. No boot preference, recording choice, pipeline rebuild or new
camera mapping is introduced. Existing overlay permission and factory-priority
checks stay in the shared popup path.

Validation uses actual native NavigationView rows on a disposable API 32 tablet
emulator: light/dark/1.3× text screenshots, stable destination font size/weight,
switch accessibility, shared popup start/stop, unchanged pipeline/preferences and
restored Settings selection. The emulator has no vehicle composite feed; its
waiting view is not presented as car-camera output. Vehicle appearance still needs
an owner check. Both native tests, all 1,514 JVM tests, lint (zero errors), and
debug/release builds pass. The scoped UI detector reports no findings. Evidence:
`/tmp/companion-drawer-native-final.log` and
`/tmp/companion-drawer-validation-final.log`.
