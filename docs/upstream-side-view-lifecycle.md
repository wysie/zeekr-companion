# Upstream blind-spot lifecycle port

Adapted from dts88 PR #41 (`95f5b989`, source `bf718089`), compared with upstream
main `0697bc76` on 2026-10-17. This is a selective port into the existing fork
service, not a second service or a wholesale merge of upstream main.

## Changes

1. An attached popup has a two-second reconnection check after its quick startup
   retries end. A new pipeline/camera receives the existing texture declaration;
   the old camera's declaration is withdrawn first. Unchanged checks do not wake
   the scheduler or repeatedly log a new binding. A replacement camera clears the
   old image geometry/freshness and supplies its own dimensions before reveal.
2. Reopening a window can reclaim the texture retained by its pipeline, avoiding
   a new output when the same one is still available. The buffer size is restored
   after the first reclaimed frame, as in upstream. Reclamation resets freshness;
   it never presents the previous activation's image as a new live picture.
3. Camera/configuration or window-attachment refusal is latched for the current
   readiness period. A factory/reverse/disconnect/screen interruption re-arms it;
   leaving D must complete the 30-second grace while no side-view request remains.
   Merely cancelling a signal while still in D does not retry a refused output.
   Explicit settings changes can also re-arm it, as before.
4. Failed window attachment does not repeat on every telemetry event. The recovery
   check and quick retries stop when the window detaches or the output is rejected.
   A successfully attached new window starts a fresh quick-retry budget.

Service-owned removal is immediate, matching upstream: a rapid reopen can reclaim
its predecessor's texture before creating another one. X latches dismissal at once
and queues the actual teardown after touch dispatch, making immediate removal safe.
A reclaimed window removed before first layout explicitly hands its orphan texture
back to the pipeline even if Android sends no destruction callback.

## Retained fork behavior

The existing scheduler still exclusively opens/reconfigures cameras, and safely
releases handed-over textures only after they leave a session. Optional popup,
HUD and DIY 360 output priorities are unchanged. This port adds the popup's missing
reconnection/reuse paths; it does not substitute upstream's SIDE output enum or
change the current HUD/360 output implementation.

Square/default and other saved aspect ratios, pinch/drag, expand/restore, shared
placement, temporary dismissal, mirror replacement/hiding, HUD destinations,
prewarming preference, close delay, aim and FOV defaults stay intact. The popup
continues to drain hidden frames and require advancing source timestamps; the
upstream invisible-canvas and fixed-placement choices are not imported. There is
no new recording trigger or blind-spot warning interpretation.

## Validation

- Five JVM refusal-episode tests cover rapid updates, factory/screen boundaries,
  D-exit grace, return to D and requests continuing outside D.
- Native Android tests use real overlay windows and retained texture pools with
  declaration-only fake cameras: recovery after quick retries, pipeline replacement
  and old-output withdrawal, same-texture reopening, pre-layout orphan handover,
  rejection cancelling recovery, actual overlay-permission denial/re-arming, and
  exact bound-pipeline ownership when the global pipeline changes during removal.
- Existing native startup/freshness, popup shape/gestures, corrected rendering and
  HUD window tests are retained. JVM scheduler tests also cover popup/HUD/360
  handover during recording, output priority and factory-view decisions.

These checks establish software behavior on a disposable emulator, not on-car
latency or a guarantee of uninterrupted recording. On-car testing should include
factory camera takeover/return, popup close/cancel/re-signal, simultaneous HUD and
screen modes, and recording across popup reopen or profile/pipeline replacement.
No new public release is created by this feature commit.

Final validation: 1,438 JVM tests, 17 distinct native Android tests across the
lifecycle/startup/aspect/rendering/HUD suites, Android lint and signed release
assembly passed. The existing signing certificate is retained. No on-car test
or performance improvement beyond these software checks is claimed.
