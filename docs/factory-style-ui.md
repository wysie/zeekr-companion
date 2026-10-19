# Factory-style UI, 2026-10-19

The owner supplied five 009 factory menu photos and approved separate main-screen
and Blind spot camera Picture concepts. The native app implements their layout
language, retaining the Companion mark and real preferences. Concept camera photos
and approximate generated icons are not shipping assets. No source/Ferix code was
imported in this change.

## Main and shared settings

- Four equal compact inline toolbar utilities, flexible space and a driver-side
  recording control. Recording retains click-to-start/stop, progress, native
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

## Validation

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
