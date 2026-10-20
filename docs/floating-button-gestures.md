# Floating-button gestures

Settings → Floating button → Actions now has Tap, Double tap and Long press.
All three offer, in order: Open the app, Start / stop recording, Take a photo,
Lock current recording, Super mirror on / off, 360° on / off and Do nothing.
The 360° action uses the manual popup service’s session state, including temporary
factory/screen suspension, to open or close it. It does not toggle the main
preview mode, bring the app forward or save a startup preference. It reuses the
existing permission gate, main-view handoff and camera/factory priority. Existing tap/hold keys and saved choices are unchanged.
The new `floating_double_tap_action` defaults to `none`; unknown values also
resolve to none. The existing layout reset still resets layout only.

With Double tap set to Do nothing, a short release immediately dispatches Tap,
as before. Otherwise, a single release waits for Android's double-tap timeout
(normally about 300 ms). A nearby second tap within that interval runs only Double
tap, without executing Tap first. Widely separated touches or slow taps remain
separate single taps. A third tap starts a new sequence.

Holding for 600 ms runs Long press once; release does not also tap. Holding the
second touch of a double-tap candidate runs only Long press. The action settings
are captured at touch-down. Changing an action cancels any pending gesture;
changes apply to subsequent touches without restarting the app.

Dragging, movement while position-locked, multiple fingers, ACTION_CANCEL,
window removal/service shutdown and position reset cancel pending gesture timers.
Dragging still saves the button position at release; locking position does not
disable stationary gestures. Gesture recognition and the recording coordinator
are unchanged; each action reuses its existing feature authority. No camera mapping, automatic recording trigger or HUD behavior is
changed by this feature.

## Validation

- 12 deterministic gesture tests cover immediate/delayed single taps, double and
  triple taps, distance/timeout boundaries, long press, second-touch hold and
  cancellation before/after release; optional-action parsing also has a regression.
- Three native API 32 tests exercise the production service's actual overlay touch
  listener with real Handler timers, real preference persistence/recreation,
  immediate taps with double tap off, drag/lock/cancel and window removal. An
  observing action sink substitutes for recording/photo operations; these tests
  do not open vehicle cameras or prove on-car interaction timing.
- The full 1,472 JVM tests, Android lint, debug/test/release APK builds and a native
  settings screenshot inspection passed. New strings cover Chinese, English and
  Malay. Vehicle touch timing still needs owner testing.

The seven-action extension is included in 2.11.56-alpha.1. Earlier release tags
and downloads remain unchanged.

## 360° action and ordering — 2026-10-20

`toggle_360` is a new saved key. All six previous keys are unchanged, so moving
Lock current recording before Super mirror and Do nothing to the end preserves
existing assignments. All three pickers use the same ordered action list. With a
live MainActivity, opening reuses its 360-main-to-popup handoff without bringing
it forward. Without one, the existing popup service opens directly. Closing uses
actual service session state even while factory/screen rules suspend the picture;
X/other entry-point closure is respected on the next gesture. Overlay permission
and camera priority are unchanged; no automatic startup preference is created.

Validation: 1,528 JVM tests, zero lint errors and debug/test builds pass. Five
existing/extended native floating-button cases pass: real gesture timing and
cancellation, foreground visibility, all three picker orders, 360 assignment
persistence/recreation with unchanged tap/hold choices, plus actual popup-session
start/stop with and without MainActivity and after external closure. Camera-source
and popup-layout preferences remain unchanged. The seven-row native picker was
visually inspected. Emulator suspension/session tests do not validate vehicle
image quality, concurrent camera support or performance.
