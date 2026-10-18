# Owner dump findings from 2026-10-18

The exported drive sessions include versions 45, 46 and 47. The latest session,
starting at 17:11:24 local vehicle time, identifies `2.11.47-alpha.1` and retains
the post-install startup history. Raw reports, photos, vehicle signals and private
paths remain outside the repository.

## Slow camera startup

The history confirms a real failure to produce frames, not just a delayed waiting
cover. The upgrade stopped the old recording process at 16:58:52; release 47
started at 16:58:55. Cameras 0 and 3 were initially unavailable, then reported
available roughly 0.4 seconds later. The scheduler started rear road first while
Surround was unavailable. Rear road configured but produced no frames within its
15-second startup window; Surround's first attempt also produced no frames.

First successful source/recording recovery in this attempt:

| Source | First recovered frames | Approximate delay from initial camera startup |
| --- | --- | ---: |
| Front road 0 | 16:59:26.797 | 30 seconds |
| Surround 3 | 16:59:41.720 | 45 seconds |
| Rear road 1 | 16:59:47.565 | 51 seconds |

Only Front road was writing when the initial recording settled at 1/3 sources.
Surround and Rear road joined afterward. Surround's recovery included a 14.8-second
camera-service disconnect; Rear road's included a 5.6-second disconnect. Quitting
and reopening while the previous pipeline was still closing caused further waits;
another rear-road exit close took 15.9 seconds.

Gear P was present, but main preview/manual recording has no Park eligibility
gate. These records establish stalled Camera2/HAL requests and slow disconnects;
they do not prove the underlying trigger or that release 47 introduced it.
The optional main-preview GPU method was enabled earlier and was disabled during
the subsequent recovery period; that correlation alone does not establish cause.
Do not bypass closure ownership or shorten retry timers blindly.

Later snapshots show Surround, Front road and Rear road streaming together, and
the history records all three writing files. Concurrent operation is demonstrated
for these observed 009 sessions/settings; it is not a guarantee for every
resolution, output combination, firmware or 7X. The dump has no Videos directory,
so file contents and continuity cannot be independently verified here.

### Startup corrections

The startup scheduler now gives a requested, initially unavailable Surround source
up to three seconds to finish its availability handover before opening another
ready source. If it stays factory-held, other sources proceed after that window;
a single requested source and already available Surround add no new wait.

A configured, repeating session that returns Camera2 errors and no successful
results uses the existing eight-second no-progress budget instead of occupying
the whole 15-second opening slot. Flushed requests, previous-session errors and
slow starts without errors keep the original window. Results from a replaced
capture session no longer advance the current session's liveness/FPS counters.
Factory ownership, close completion, rescue limits and owner picture values are
retained. Slow native disconnects still have to finish; these changes cannot
guarantee a particular first-frame time on the vehicle.

Validation: 1,491 JVM tests, debug lint, debug/test/release assembly, and three
native API 32 regressions for source ownership and stale-session progress. The
startup scenarios replay availability and session evidence in controlled tests;
on-car confirmation after an update/cold start remains necessary.

## Road and manual camera ownership lookup

The extra source roles were missing from the ownership lookup used by
`CameraAvailabilityWatch` and `CameraContention`. A road camera could therefore
appear as not ours while the same logs reported its successful open and frames.
These messages must not be used as proof that another application held it.

The correction uses a published immutable snapshot of every initialized
SingleCamera instead of enumerating only front/back/left/right. Availability
replay membership and the diagnostic open list now use the same complete
inventory. Service-held/open/externally-lost distinctions remain unchanged.
This corrects ownership classification/reporting, not the observed HAL stalls.

Validation: 1,482 JVM tests pass, along with debug lint and debug/test/release
assembly. Two native API 32 regressions cover road/manual ownership and the
diagnostic open/replay lists, externally lost versus still-opening sources, and
immutable initialized membership with duplicate/unassigned IDs and release
cleanup. The ownership fixture uses controlled source states; it does not claim
to reproduce the vehicle's HAL disconnect failure.

## Blind side image detail

Camera 3 is still selected at 1280×5140: four 1280×1280 images plus separator
bands. Side popup/HUD/mirror outputs receive the native buffer, restored after
layout changes. Their correction path does not create a small bitmap first.
The optional main-preview GPU setting does not switch side-popup rendering.

Saved popup values are 90° view width, 50° aim back, 15° aim up, ±30° rotation,
mirroring on and aspect 11:5. With the default Straighten/Auto-fit behavior, the
production projection model gives these approximate results for the left view:

| Framing comparison | Effective horizontal FOV | Source-coordinate bounding span within the 1280 lane |
| --- | ---: | ---: |
| Current aim 50, Auto-fit on | 75.6° | 583×496 pixels |
| Aim 50, Auto-fit off | 90° | 686×587 pixels, with missing coverage |
| Aim 40, Auto-fit on | 90° | 686×565 pixels |

These are sampled mathematical bounds, not a rectangular optical-resolution
measurement or a sharpness score. Turning fitting off retains the requested view
but can expose black missing corners. Reducing rear aim changes the viewing
direction. Compare those options explicitly; do not silently replace the owner's
preferred angles. Finer correction/per-pixel rendering could reduce geometric
approximation but cannot restore detail absent from the cropped source.

The new developer export tried numeric IDs 0–15. IDs 0–3 captured images;
4–15 returned unknown-device characteristics errors. No additional exposed
higher-resolution side-camera source was found by that bounded scan. Advertised
3840×2160 sizes are not evidence of a higher-resolution four-lane side composite.

## Next checks

- Verify complete ownership reporting for road/manual sources and actual external
  loss, retaining factory priority and existing open/close scheduling.
- Compare side framing at aim 50 versus 40, or explicitly disable Auto-fit, at the
  same popup size. Retain original saved settings until the owner chooses.
- Reproduce cold startup and post-update/reopen handover separately, with and
  without the optional main-preview GPU pipeline. Keep camera session/output
  details and first-frame timing; do not attribute the failure to Park or another
  application from the erroneous old ownership messages.
- Keep the deferred UI review separate. Persistent per-source recording status
  would make the observed 1/3 startup behavior understandable, but no menu or
  waiting-cover redesign is part of this ownership correction.
