# Codebase audit — 21 October 2026

Baseline: `4d8cf63b2a8707bddb95938168f87b0c7a6a9c55`, published
2.11.60-alpha.1. Corrections are on `fix/codebase-audit-20261021`; this audit does
not publish another release. Android identity, signing, saved camera tuning and
the release tag remain unchanged.

The request was a whole-codebase audit after the owner observed that stopping
recording made the blind-side picture smoother. Astra independently reviewed the
recording/rendering paths, with parallel lifecycle/settings and storage/update
reviews. This is a source and local regression audit, not a claim that every line
or vehicle behavior has been verified.

## Confirmed findings and corrections

| Priority | Trigger and defect | Correction | Regression evidence |
| --- | --- | --- | --- |
| P2 | A docked/covered mirror, or a retained main/popup/HUD output, can remain in a camera session kept alive by recording. Its GL pipe still renders/swaps every input, sometimes a full 1280×5140 strip after the projection request is cleared. | Suspend primary presentation on hide/retention; resume on every reclaim path. Input acquisition/draining and active settings taps continue. No session is rebuilt and transparent standby startup is preserved. | Real GL tests send native frames while suspended, verify no primary swaps, keep settings tap working, then resume the latest frame at native dimensions with the same producer. Frozen inputs do not become fresh merely by resuming. |
| P2 | The frame callback's normal `drainEncoderWithResult(false)` waits up to 10 ms for future codec output after exhausting available output, and has no batch limit. The separate shutdown drain already had a nonblocking normal policy. | One shared output drain: zero timeout and at most eight polls during capture, bounded 10 ms polls only during the existing EOS deadline. Always return a dequeued buffer even if copying fails; retain incomplete-EOS diagnostics. | Pure-Java scripted codec tests cover empty, ready, repeating-status, delayed EOS and absent EOS cases. |
| P2 | Recording reads `SurfaceTexture.getTimestamp()` before acquiring its pixels. A rendered frame therefore receives the previous acquired frame's time. Throttled/coalesced callbacks also count as encoded frames. | Map the timestamp after `updateTexImage`, suppress duplicate acquired timestamps, and count only successful EGL submissions. Retain the recorder-lifetime origin across segment/rearm. Real render failures reach existing recorder recovery instead of masquerading as healthy skips. | Production EGL bridge native test verifies acquired and output timestamps, duplicate rejection and acquisition failure propagation. |
| P2 | Source picker/editor and indirect non-009 profile-size resolution call the vendor camera service from UI work. A stalled Binder call freezes controls and overlay freshness timers. | Load advertised IDs/characteristics on a worker and read cached facts on UI. One stable picker shows cached/saved choices immediately and appends discovered IDs without changing existing indexes. Dismissed dialogs and destroyed views cannot publish stale UI. | Native blocked-service simulation leaves the main looper responsive; cached non-009/manual lookups never enter that service. Existing source/editor flows remain covered. |
| P2 | Selecting another settings category while a nested page is open starts its Z-axis return transition and a new category transition on the same underlying fragment. The replacement page can remain STARTED and unresponsive to later navigation. | Clear only the abandoned detail return/category reenter transitions before removing that detail history. Normal Back still retains its transition/history. | Native Source → Developer → Source → Back verifies resumed pages and that abandoned history cannot reappear. |
| P2 | Quit during Apply invalidates holder callbacks without completing the process-owned Apply transaction. Reopening before process exit leaves Apply busy and recording gated forever. Confirmation dismissal/recreation can also leave the separate confirmation flag latched. | Cancel once when releasing the holder, clear both gates without resuming, and reject old transaction completions and delayed drains. Track confirmation owner/dialog; clear stale/dismissed confirmation immediately and prevent an old dismissal from clearing a newer dialog. | Held setup worker, exit cancellation, same-process recreation, stale completion rejection and subsequent actual recording, plus immediate cancel/retry and confirmation-owner recreation are exercised natively. |
| P1 | Cleanup assumes the lexically newest file in a slot is the active recording. Clock rollback or a future-dated closed clip makes the genuinely open segment deletable. | Shared process-local open-file leases are registered before file opening and checked under the same guard as deletion. Codec/MediaRecorder and rescue writers retain leases until actual close; uncertain native close failures retain protection. Protection does not depend on the footage-lock switch. | Planner, lease/reference-count and actual-cleanup tests retain an older-named open segment despite a newer closed clip. |
| P2 | Timeline deletion uses a scanned lock cache outside the lock-edit guard. A later lock can be ignored; open recordings also lack an explicit deletion exclusion. Its lock reads/deletion also block UI on USB I/O. | Both Timeline actions use shared guarded live lock/open-file checks on a single in-flight worker, with UI/lifecycle-checked completion. Report deleted/protected/failed counts. | Native Timeline test invokes both real actions using stale scanned state and verifies newly locked/open files survive while eligible files are deleted. |
| P2 | A flash on already brake-locked footage is filtered out, leaving the lock eligible for automatic-budget eviction. Manual promotion can report success even when removing automatic ownership failed. | Promote brake-owned clips on explicit flash/manual requests. Remove automatic ownership durably first and report a failure if that metadata cannot be read/written. Existing user-owned flash locks need no repeated action. | Native flash/budget and unreadable automatic-metadata tests. Interrupted promotion fails conservatively with protection retained. |
| P2 | Backup parsing checks incoming types against values already present locally. A known absent key can accept the wrong encoded type, commit, then crash its typed getter at startup. | Validate known consumed key types across all five stores before staging/commit, including unset keys. Retain unknown extension keys for forward compatibility and the existing restore journal/rollback. | JVM schema/consumer coverage plus native absent-key wrong-type rejection and existing multi-store rollback tests. |

Implementation entry points:

- Presentation: [FisheyeGlPipe](../app/src/main/java/com/kooo/evcam/zeekr/FisheyeGlPipe.java#L323),
  [PreviewDewarp](../app/src/main/java/com/kooo/evcam/zeekr/PreviewDewarp.java), retained-manager hooks and mirror visibility.
- Recording: [CodecVideoRecorder](../app/src/main/java/com/kooo/evcam/camera/CodecVideoRecorder.java#L1901),
  [CodecOutputDrain](../app/src/main/java/com/kooo/evcam/camera/CodecOutputDrain.java),
  [EglSurfaceEncoder](../app/src/main/java/com/kooo/evcam/camera/EglSurfaceEncoder.java#L482).
- Settings/lifecycle: [CameraSettingsFacts](../app/src/main/java/com/kooo/evcam/settings/CameraSettingsFacts.java),
  [CameraChanges](../app/src/main/java/com/kooo/evcam/camera/CameraChanges.java),
  [SettingsShellFragment](../app/src/main/java/com/kooo/evcam/settings/SettingsShellFragment.java).
- Footage/restore: [ActiveFootage](../app/src/main/java/com/kooo/evcam/storage/ActiveFootage.java),
  [StorageGuard](../app/src/main/java/com/kooo/evcam/camera/StorageGuard.java#L113),
  [GalleryDelete](../app/src/main/java/com/kooo/evcam/playback/GalleryDelete.java),
  [AutoLock](../app/src/main/java/com/kooo/evcam/storage/AutoLock.java#L311),
  [FootageLocks](../app/src/main/java/com/kooo/evcam/storage/FootageLocks.java),
  [SettingsBackupTypes](../app/src/main/java/com/kooo/evcam/settings/SettingsBackupTypes.java).

## Coverage

The baseline inventory contains 333 main Java files and 229 JVM/instrumentation
test files. Whole-tree resource/risk-pattern and lint checks were combined with
detailed reads of these boundaries:

- Camera channel scheduling/ownership, source mappings, retained surfaces,
  lifecycle, Apply, recording intent, quit/reopen, boot/foreground services.
- EGL/codec acquisition and output, throttle/drain, park/rearm/segment behavior,
  preview/360/side/mirror/HUD rendering, freshness, factory priority and fallback.
- Storage plans/cleanup, current writer handles, file copy/archive/repair,
  event/manual locks, gallery/timeline deletion and sharing/export boundaries.
- Profile/settings validation and reset/import/backup, language resources,
  vendor-signal wrappers, cached readings, updater integrity/redirects,
  manifest/providers and build/release rules.
- Playback generation/surface lifetime, exporter publication and cancellation,
  diagnostics and developer tools were reviewed at their relevant I/O/lifecycle
  boundaries. Native vendor/platform implementations were not audited.

The earlier [project safety review](project-safety-review.md) and
[camera review](camera-safety-review.md) were consulted to distinguish prior
fixes and intentional decisions from new findings. No upstream/Ferix import or
source-head review was performed; this audit does not change the integration
ledger or existing upstream PR branches.

## Validation and limits

The final source passes:

- **1,556 JVM tests**, zero failures/errors/skips, including bounded normal/EOS
  codec draining, open-file leases/aliases, rollback planning and typed restore.
- **43 unique targeted native tests together**, zero failures, on the disposable
  API 32 SwiftShader emulator (`153.779s`). This covers the affected EGL/side/mirror/
  360 paths, hidden draining/resume/freshness/taps, actual acquired/submitted
  timestamps, real playable recording after Apply, quit/reopen, settings/confirmation
  navigation, cleanup, explicit lock promotion, restore and both Timeline deletes.
- Full debug lint: **zero errors** (existing warnings remain); debug, signed
  release and instrumentation APK builds succeed. The three release-packaging
  Python checks also pass. No version/package/signing change or release publication.

Initial native runs exposed the confirmation/navigation races described above,
plus invalid test assumptions about an in-flight TextureView latch and below-fold
scrollable layout controls. The final GL tests fence the actual GL handler and
verify successful swap counts and live acquired timestamps, retaining the exact
latest-frame/native-size/producer assertions. The UI test scrolls to the existing
layout controls before asserting full visibility. A large inherited-test batch
was stopped during diagnosis; the final 43-test run deliberately selects unique
affected methods rather than claiming the entire instrumentation suite passed.

Private validation logs/manifests are retained under
`artifacts/codebase-audit-20261021/`; owner media and test artifacts are not staged.

The owner's recording-on/off comparison supports shared-resource contention. It
does not identify a single GPU/HAL/codec/USB bottleneck, and emulator GL tests do
not establish a real-car performance gain. Source cadence alone does not prove
individual physical-camera freshness. In particular, the old 30-input/15-output
recording log is not proof of overload: the frame limiter can produce that rate.
No FPS cap, image quality, resolution or GPU preference is silently changed.

Retained open-file protection is process-local: after process death the kernel
closes those handles. Failed closes may conservatively retain a lease for the
rest of the process; this favors retaining footage over reclaiming its space.
Filesystem durability, power loss, USB-controller behavior and sustained
recording while all car displays are active still require on-car testing.

Known decisions remain: the compatibility signing key is public, sharing is
temporary bearer-link HTTP, relay destination/event retry journals remain a
follow-up, connected vendor signals can retain cached values, and 360 geometry
is experimental. The audit does not silently rotate signing identity or claim
factory calibration/eye tracking. Dependency vulnerability discovery and native
vendor/OS review are not established by the passing app test suite.

Localization still has English Malay About fallback, seven Traditional Chinese
upscaling entries in an otherwise Simplified Chinese UI, and English release
notes. Completeness/placeholder checks do not certify natural wording. These
content issues are recorded separately from runtime/data-integrity corrections.
