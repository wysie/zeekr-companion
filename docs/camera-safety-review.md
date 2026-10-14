# Camera display safety review — 2026-10-14

Resolution follow-up (2026-10-15): the newer prototype has Low/Balanced/Full
choices instead of the original fixed 320×480 output discussed below. Slow
readback/render or allocation failure now reduces resolution, pausing at Low if
necessary. The first native copy remains uninterruptible. Higher-resolution
memory/copy speed and recording continuity remain untested on the 009; see
[current bounds and behavior](diy-360-view.md#resolution). Parked-only and
factory/stale guards remain in force; increased detail does not establish calibration.

Reviewed baseline: `3900b51e3b41216800042ab4209c5a9883d22564`
(2.11.17-personal-alpha). Corrections are on `feature/diy-360-safety` for
integration into `main`. Existing upstream PR heads remain unchanged.

**Outcome: real display-safety defects were found and fixed. This is not a
vehicle safety certification. The DIY stitched view remains an uncalibrated,
parked-only calibration tool.**

## Findings and corrections

| Priority | Trigger and defect in baseline | Correction |
| --- | --- | --- |
| P1 | `TextureView.getBitmap(Bitmap)` fails while texture timestamps continue. Android can leave the reused destination unchanged, so the previous picture could be presented as current. | Clear the destination to transparent before every copy; reject a rendered result if any sampled source pixel is not opaque. Copy exceptions, unavailable textures and incomplete results blank the picture. |
| P1 | An independent side popup stops receiving frames while the indicator remains active. There was no stale cover in that view, so the retained picture could remain visible indefinitely. | Shared timestamp progression guard, opaque waiting screen at a 500 ms threshold, 100 ms timer, reset on texture destruction/detachment. Drawing continues underneath the cover so new buffers can recover the view. Close/resize controls remain available. HUD inherits this guard as well as its existing cover. |
| P1 | Super mirror, including side-camera replacement, loses frames. Its 2.5-second threshold plus one-second polling could expose a stopped image for about 3.5 seconds. Reopening also reset its clock without receiving a frame, and its cover was translucent. | Shared 500 ms guard with 100 ms polling, opaque cover, no picture before source timestamp progression, reset on reopening/destruction. Unknown composite geometry remains covered while buffers are drained. |
| P1 | An uncalibrated DIY stitched view is opened while driving, or controls change calibration while moving. Estimated poses can misrepresent objects and clearance; known factory-camera takeover was not explicitly handled. | Require a connected vehicle interface reporting P, with no known contradictory native speed. Unknown/non-P gear blocks the prototype and calibration. Calibration closes when that condition is lost or the screen darkens. Reverse, known factory surround/popup/park-assist activity, hidden/dark screens and invalid calibration suppress the DIY picture and copying; pending results recheck the policy before publishing. |
| P2 | Source selection assigns the same camera to two physical sides. The resulting view can invent duplicate coverage. | Validate all four source lanes are distinct; selecting another source swaps assignments atomically. Previously invalid saved mappings stay covered until corrected/reset. |
| P2 | DIY readback blocks the shared UI thread, delaying camera windows and control responses. | Run lookup/blending at background thread priority with one frame in flight. A measured readback over 40 ms pauses further copies in that activation. This detects a slow call after it returns and cannot interrupt the first native stall. |

The documented silent-copy behavior is in the
[Android TextureView API](https://developer.android.com/reference/android/view/TextureView#getBitmap(android.graphics.Bitmap)).
Camera timestamps should progress strictly, but their origin is source-dependent;
see [SurfaceTexture timestamps](https://developer.android.com/reference/android/graphics/SurfaceTexture#getTimestamp()).

## Paths checked

The review examined DIY projection, confidence blending and lane bounds; source
copy ownership; UI/worker publication and configuration revisions; frame clocks;
attachment, visibility, cancellation and recovery; main-screen integration;
camera calibration and reset; side-view decisions and dismissal; popup and HUD
frame handling; Super mirror rendering; HUD display selection; and the camera
scheduler's optional-output rejection order.

The DIY renderer adds no Camera2 output or vehicle-control write. It samples the
existing preview. Optional HUD and popup outputs yield before the mirror and
recording on session-configuration failure. Factory/reverse priority remains in
the existing side-view decision rules. User dismissal still lasts until cancelling
the indicator and signalling again. These are source-code and regression-test
findings, not observations of the 009's firmware.

## Remaining limits

- **Actual geometry is unknown.** Estimated equidistant lenses, heights and poses
  are not 009 calibration. Ground-plane projection distorts tall objects, and
  blending can ghost obstacles. The five-fps/320×480 output is deliberately
  limited. It must not be used to judge road hazards or parking clearance.
- **Progression is not exposure age.** Timestamp checks detect a stopped delivered
  stream, but do not establish end-to-end camera latency. Individual cameras may
  freeze while the packed composite keeps advancing. No per-camera exposure
  metadata has been verified. One delivered buffer does not prove synchronized
  sensor exposures.
- **Telemetry is not independently verified.** The guards require a connected
  interface and reported P. These signals are reverse-engineered, not certified
  on this owner's 009. Individual cached readings have no independent freshness
  expiry; a connected but faulty interface could retain an incorrect value.
  Unknown factory flags also cannot establish that a factory view is absent.
- **Timers are not real-time guarantees.** The 500 ms threshold plus the 100 ms
  checking interval gives an approximately 600 ms cover bound only while the UI
  thread is responsive. A stalled UI or vendor/native call can exceed it. The
  readback pause prevents repeated slow copies, not the first stall.
- **Hardware performance is untested.** Native texture readback, orientation,
  real overlay/HUD presentation, recording continuity, CPU/GPU/memory load,
  source loss and factory takeover/recovery need on-car tests. A JVM test count
  cannot establish these properties.

This focused review does not audit every line of the recorder, Android platform,
vendor firmware or original upstream app, and cannot establish the entire app as
safe for driving.

## Validation and next evidence

The full JVM suite and signed release build are run before publication. New
regressions exercise silent-copy/incomplete-buffer rejection, distinct feeds,
stationary/factory/visibility/performance policy, retained first frames, duplicate
and decreasing timestamps, texture replacement, reset, invalid timestamps and
the 500 ms expiry boundary. The synthetic fisheye-grid scene checks projection
only; it provides no hardware safety evidence.

Begin on-car checks stationary in P: first-frame cover, source order/orientation,
calibration/reset, copy timing diagnostics and return to normal preview. Verify
source loss and recovery, then factory camera priority and transition out of P
while remaining stationary. Check simultaneous recording and each screen/HUD
mode before considering any driving use of the existing side-camera features.
The DIY view stays parked-only pending accurate calibration and further review.
