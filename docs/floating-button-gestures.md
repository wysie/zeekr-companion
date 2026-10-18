# Floating-button gestures

Settings → Floating button → Actions now has Tap, Double tap and Long press.
All three offer Open the app, Start / stop recording, Take a photo, Super mirror
on / off and Do nothing. Existing tap/hold keys and saved choices are unchanged.
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
disable stationary gestures. The existing action dispatcher/recording coordinator
is unchanged. No camera mapping, automatic recording trigger or HUD behavior is
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

This feature is recorded under Unreleased; it does not change an existing release
tag or downloadable APK.
