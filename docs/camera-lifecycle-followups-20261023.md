# Camera lifecycle follow-ups — 2026-10-23

Selective adaptation of dts88 #65 (`fc04167c`) and #66 (`be614c00`), following
owner authorization of the source review recommendations. No complete upstream
merge, audio capture, signing migration or source UI replacement.

- `SingleCamera.stillCarries` includes the carried map until device closure, so
  a cleared session snapshot cannot release a texture still used by the camera.
- Mirror output configuration failures withdraw the declaration. Watchdog ticks
  leave that output withdrawn; reopen/resume, screen-on, undocking or a changed
  pipeline/texture permits another attempt. Late callbacks for old outputs are
  ignored. The declaration's owner identity is retained independently of binding.
- Scheduler drop delivery covers RECORD, MIRROR and the fork's SIDE_POPUP,
  SIDE_HUD and BIRD_EYE kinds, preserving their existing refusal/recovery rules.
- Holder changes unlock only affected cameras/refused recording outputs. Recording
  plans still govern independent exterior roles; side/mirror/360 holders only
  affect surround. Screen-on/service-free recovery remains. An exclusive developer
  probe affects all lanes and remains an explicit all-lane change.
- Unlike a direct source copy, changed-holder collection/drain uses a short lock.
  The observed holder baseline is initialized and reconciled before notifications,
  preventing a first overlay callback from replaying an existing preview claim.
  Release/reclaim within one coalesced round remains visible.

Saved camera mappings, independent preview/recording selection, Apply semantics,
GPU processing, encoding settings and existing window gestures are retained.

Implementation `062c3ea2`, integrated in main `3d0d8e3a`; main CI
[39922297118](https://github.com/wysie/zeekr-shortcut-car/actions/runs/39922297118)
passed. Subsequently published in [v2.11.62-alpha.1](https://github.com/wysie/zeekr-shortcut-car/releases/tag/v2.11.62-alpha.1); tag `dbae67bd` includes both the adaptation and main integration.

Validation: 1,564 JVM tests; debug, release and instrumentation assembly; zero-error
Android lint; five packaging/release-note tests. Twelve native Android checks cover
actual carried SurfaceTexture ownership after session clearing, independent output
listeners, mirror refusal identity/resume, popup lifecycle and mirror controls.
Final counts and integration references are recorded with the completed source
recommendations. No on-car contention/performance, USB or physical HUD claim.
