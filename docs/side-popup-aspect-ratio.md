# Separate blind-spot popup shape

Settings → Blind spot camera → Popup window → Aspect ratio (width:height)
offers Square (1:1), Portrait (3:4), Landscape (4:3), Wide (16:9) and Extra wide
(2.2:1). Square remains the default, with the previous square size calculation.
Confirming a choice updates an open separate popup; cancelling leaves it unchanged.

The size slider controls height. Pinching keeps the ratio, dragging saves the
position, and Expand/Restore keeps the selected shape and restores the previous
geometry. Both sides share these saved values. Screen bounds scale both dimensions
together. Reset blind spot settings restores square along with the existing reset
scope, retaining the feature enable switch. Changing shape while expanded returns
to the configured normal window.

HUD and Super mirror retain their own geometry. Camera selection, recording,
field-of-view and aim defaults are unchanged. The corrected renderer already uses
the actual window ratio; a different shape changes framing rather than stretching
pixels. Extreme aim can still restrict coverage. On-car visual fit remains to be
checked; emulator validation does not establish vehicle compatibility or timing.

Validation on 2026-10-17:

- JVM geometry regressions cover legacy square sizing, all five ratios, clamping,
  minimum control space and small/portrait screen bounds.
- Two native Android tests exercise the real WindowManager popup: drag, two-finger
  pinch, side switching, expand/restore, shape changes while expanded, reset, and
  independent HUD settings. The native picker test checks Cancel/OK and Activity
  recreation, and captures the options and selected summary for visual review.
- Two existing native rendering regressions and five startup/freshness regressions
  pass with the geometry changes.

Final combined checks: 1,429 JVM tests, nine native Android tests, Android lint
and signed release assembly passed. The build retains the existing signing
identity. No new GitHub release is published by this feature change.
