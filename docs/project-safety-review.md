# Whole-project safety and reliability review — 2026-10-14

For the later combined application and recording/rendering follow-ups, see the
[21 October codebase audit](codebase-audit-20261021.md). The baseline and historical
findings below describe the 14 October review.

Baseline: `f785483309dbc5b2bcf58be0f7cfa8082761db6e`, combined `main`,
2.11.18-personal-alpha. Corrections are on `review/project-safety` for integration
into the combined build. Existing upstream PR branches are unchanged.

**Several concrete defects were found. Their correction does not establish that
this application is safe for driving.** This review covers the application as a
whole, extending the separate [camera display review](camera-safety-review.md).
It is a source review and local regression exercise, not a functional-safety
assessment of the car, vendor SDK, Android platform or camera hardware.

## Findings fixed in this change

| Priority | Reproducible trigger or source finding | Correction and practical limit |
| --- | --- | --- |
| P1 | Relay transfer deletes an existing destination before copying. Archive treats equal filename/size as identical, deletes the source without comparing bytes, and may delete an existing destination after a failed copy. | Shared `VerifiedFileCopy` uses an exclusive partial file, syncs and verifies SHA-256, then publishes without knowingly replacing a destination. Existing different-content files cause failure; both files are retained. Equal-size different-content collisions are regression-tested. Sources must be closed; this is not a USB-controller or power-loss durability guarantee. |
| P1 | Relay uses Android cache, discards files smaller than 500 KB and schedules deletion of untransferred files after one hour. Short footage can be valid; USB unavailability can outlast that hour. | New buffers use private `files/temp_video`; age expiration and the arbitrary size rejection are removed. Empty files are retained as failures. Recovery scans also check the legacy cache. App-data clearing/uninstall still removes private buffers, and a full internal volume can stop recording. |
| P1 | Developer MP4 repair appends an index and patches the only original file. Its normal rollback cannot protect against process death or storage failure during the patch. | Create a verified original backup first under `ZeekrShortcutApp/Videos/repair-originals/<name>.original`. Abort before editing if backup fails or conflicts. Retain backups and describe their space cost in the confirmation/result. The working file can still become damaged; recovery from the backup is manual. |
| P1 | Main surround/cabin previews lack the overlays' stale-picture cover and can show a retained frame after source loss. | Main containers now consume the shared progression guard and cover stalled pictures after 500 ms, with 100 ms checks. They continue drawing/draining the native texture underneath. Reattachment and texture destruction reset the guard; unknown surround geometry stays covered except in explicit RAW display. Native composition and timing still need vehicle tests. |
| P2 | A failed lock-list rename falls back to deleting the existing list before retrying, potentially losing protection for recorded events. | Synced exclusive temporary list and atomic replacement; unsupported/failed replacement keeps the old list. Failed flash/braking protection now produces the existing failure message. Atomic replacement support on the 009's USB filesystem remains untested. |
| P2 | Empty-seat countdown survives an unknown seat value; false-seat time accumulated in D can also cause a stop immediately on entering P. Armed Sentry treats unknown seat as empty. | Reset countdown on unknown seat/gear, non-P or known contradictory native movement. Sentry stopping requires a known empty seat. Vehicle disconnect clears the watcher-away gate. These changes do not suppress the independent screen-off recording rules. |
| P2 | Automatic recovery can raise the main activity over a known reverse/factory-camera/parking-assist view. | Guard both recovery entry points and the silent launch helper; apply the same check to the driver's fallback launch. Recording may wait for a later retry. Unknown or cached vendor flags remain a limitation. |
| P2 | NanoHTTPD's default runner can create an unbounded thread per client while phone sharing is open. Filename text is inserted directly into a response header. | At most four clients, no queued sockets; excess clients close, server dismissal closes clients. Sharing tokens increase from 48 to 128 bits. Restrict methods to GET/HEAD and sanitize header control/quote characters. Selected-file responses request no-store. HTTP remains unencrypted and links are bearer credentials. |
| P2 | Updater checks a response's length but not GitHub's asset digest or recorded asset size; redirects follow implicitly. | Require valid GitHub SHA-256 metadata, match downloaded digest and asset size, enforce HTTPS through at most five redirects and delete a failed download before installer launch. Old assets without a digest are refused. Digest integrity does not authenticate the public APK signing key. |
| P2 | Default Android backup rules permit private footage/diagnostic files and transient recording intent to be backed up/restored. | Exclude private/external app files and recording intent from backup and device transfer. Ordinary preferences remain eligible. This does not revoke any prior backups or redact explicitly exported diagnostics. |
| P2 | Screen-off recording has an unbounded WakeLock whose duration relies on UI Handler delivery. | Use the configured remaining duration plus the existing bounded camera-close allowance, an Android timeout and an additional off-UI expiration timer. A generation guard prevents an old expiration releasing a new lease. Whole-process or vendor power-service stalls can still defeat software timing. |
| P2 | Corrupt/oversized MP4 input can grow Java sample tables without a limit or advertise a multi-gigabyte validation buffer. | Cap repair at 100,000 samples, 8 MiB reference index, 1 MiB decoder description and 8 MiB validation buffer; reject oversized inputs and roll back failed post-repair validation. Adversarial sample-count rejection preserves source bytes in a regression test. Native parser internals remain outside these Java limits. |
| P2 | Scanner's 60,000-line/three-hour limits do not bound callbacks waiting in its Handler queue. | At most 256 pending vendor events; drop excess callbacks and stop admitting events after the line limit. Scanner output is a research sample, not a complete event history. |
| P2 | CI grants repository write permission to the compilation/test job, including ordinary branch builds. A configured but missing custom keystore silently falls back to the public key. | Build job uses contents:read; tag publication has a separate contents:write job. An explicitly configured missing custom keystore now fails configuration. Default public-key builds remain enabled for existing-installation compatibility. |

Android compatibility corrections also migrate three custom Back handlers to the
Back dispatcher, use private AndroidX dynamic receiver registration, and use
compat tint attributes. Explicit permission exception handling/API annotations
clarify paths already protected by catches or SDK checks; those lint findings
were not all independently reproducible crashes.

## Whole-project coverage

The baseline contains 228 main Java source files (236 after these additions).
Inventory and risk-pattern searches cover all main Java files. Detailed reads
focus on entry points, ownership, destructive I/O, vehicle callbacks, asynchronous
publication and network boundaries. This does not mean every line was formally
verified or that every possible failure was exercised.

| Area | Paths and properties examined | Evidence and remaining limits |
| --- | --- | --- |
| Camera and recording | `ChannelScheduler`, `ChannelRules`, `SingleCamera`, `MultiCameraManager`, recorder/EGL/codec paths, `RecordingCoordinator`, durable recording intent and stall recovery | Single process ownership, generation checks, optional-output shedding and bounded closing/retry rules. Held files from stuck writers are excluded from transfer. Native drivers, real encoder closure, factory contention and continuous footage need 009 tests. |
| Storage and events | `StorageHelper`, `StorageGuard`, `StoragePlan`, cleanup, transfer, `FootageLocks`, automatic braking/flash locks, archive | Cleanup recognizes the app's naming pattern, holds the protection guard through read/decision/delete, preserves each slot's newest recording and declines deletion when the lock list is unreadable. Automatic-lock budget can release old automatic locks as designed. USB I/O/power faults are not simulated by JVM tests. |
| Signals and screens | `Telemetry`, `Readings`, `EcarxSource`, `VehicleState`, scanner, `ScreenState`, driver sessions | Read/get APIs and listener registration; no vehicle actuator-write calls found in reviewed app paths. SDK-loss handling clears readings, and new automatic side-view triggers also require connection. Individual cached readings have no independent age limit while the link reports connected. |
| Displays and overlays | Main surround/cabin previews, Super mirror, side popup/dismissal, labels/arrows, HUD, projection/calibration | Shared freshness cover, factory priority and output rejection order; DIY factory-priority and freshness guards remain; the owner removed the gear/speed gate on 2026-10-15. Unknown HUD geometry stays unselected. Delivered composite progression cannot establish freshness/synchronization of each physical camera. |
| Startup, exit and power | Application, provider, boot/keepalive receivers, accessibility, foreground services, recovery, screen-off lease | Exit/recording-intent gates and current factory-view checks. Accessibility observes lifecycle rather than controlling steering/brakes; no relevant actuator or secure/global settings writes were found. Boot/kill/doze/battery behavior is vendor-dependent. |
| Playback, export and repair | Main playback activities, `ManagedVideoPlayer`, timeline, clip export/reservation, MP4 scanner/repair and archive | Player generation/state checks; export owns an exclusive partial file and avoids replacing published exports. Repair now retains a verified original before editing. Corrupt-media handling still invokes native media parsers, outside local Java proof. |
| Sharing and privacy | `PhoneShare`, `FileShareServer`, address selection, diagnostics, logging and FileProvider | Temporary selected-file/token routes; no general directory browser or path traversal route found. Stop tied to dialog lifetime, bounded new server runner. Diagnostics can contain location, VIN/device data, storage paths and logs; no automatic upload found. Trusted local network remains necessary. |
| Permissions and updates | Manifest, dynamic receivers, URI grants, update downloader/installer, signing | File/keepalive providers and camera/overlay services unexported; explicit grants for chosen content. FileProvider roots remain broad but no arbitrary-caller grant endpoint was identified. Android installer still decides whether an APK can update the installed package. |
| Profiles, settings and build | Profile parsing/resolution/validation, default/experimental switches, locale resources, Gradle/dependencies and GitHub workflow | Device-reported stream dimensions guide resolution; validation cannot prove vendor bandwidth. Developer options gate diagnostic/internal-write tools. Full lint flags 15 intentional Malay About-page fallbacks; actions still use mutable version tags. Runtime Maven scan excludes vendor SDK/firmware. |

## Important open risks and decisions

1. **Public private signing key (P1).** `keystore/release.jks` is distributed with
   its password. Anyone possessing it can sign an Android-accepted replacement
   with this identity; a matching certificate is not evidence of this maintainer.
   The existing key is retained under the fork's compatibility policy. A private
   key requires a separate migration/install plan and safe handling of data and
   signatures. This review does not make that decision or silently rotate the key.
2. **Vehicle data and camera latency are unverified (P1 for reliance while
   driving).** Connected does not prove every cached signal is current. A packed
   composite can advance while a single lane freezes. Factory flags can be absent
   or differently mapped. The app does not verify per-sensor exposure age,
   end-to-end latency or certified obstacle alerts; the side view is indicator
   driven. DIY projection remains estimated and uncalibrated; its gear/speed gate was removed at the owner’s request on 2026-10-15.
3. **Relay recovery lacks a durable destination/lock journal (P2).** Original
   buffers now survive failures, but transfer queues and pending automatic-lock
   retries remain process memory. Retrying after restart uses the current relay
   target; keep the original USB attached. One-hour automatic-lock retries can
   expire while footage itself remains retained. A persistent destination/event
   journal and explicit buffer-recovery UI are follow-up work.
4. **Native/storage/power timing is not certified (P2).** SHA verification adds
   USB reads; load-test while recording. Sync/rename operations depend on the
   filesystem/controller and do not prove durability under abrupt power loss.
   Camera/media/Binder calls can stall. The new wake timeout is an application
   backstop, not a hard real-time or battery guarantee. Keep the opt-in
   screen-off wake feature disabled unless its parked behavior is verified.
5. **Privacy and supply chain (P2).** Sharing is plain HTTP on available interfaces;
   possession of the active link grants access. FileProvider roots are broader
   than the minimum necessary, though the provider is not exported. Diagnostics
   need review before external sharing. CI actions use mutable tags; pinning and
   periodic dependency audits remain useful follow-up work. Previously reported
   App Lab disappearance has not been reproduced or attributed to an app write.
6. **Locale/lint configuration (P3).** Full lint still flags 15 About-page strings
   without explicit Malay entries; the source intentionally uses its English
   default About page for other languages. Before locale metadata was corrected,
   933 findings also requested Chinese entries already supplied by Chinese
   default resources. No baseline or blanket suppression was added. This is not
   evidence of 948 missing runtime labels or 948 safety defects.

## Validation

- **1,217 JVM tests passed**, zero failures/errors/skips. New cases test
  different-content same-size collisions; identical-copy reuse; short-file
  preservation; byte-exact copy and failure cleanup; failed lock-list replacement;
  retained repair originals after simulated patch damage; adversarial sample count and allocation-budget rejection; unknown seat/gear,
  movement contradicting P and unknown-seat Sentry; HTTPS/digest/size rejection;
  sharing worker saturation and header sanitization.
- **Signed `assembleRelease` and release vital lint passed.** Version
  2.11.19-personal-alpha, code 302; established signing identity retained.
- **Full Android lint fails honestly.** Baseline: 967 errors (948 translation,
  19 other) and 203 warnings. Runtime/API corrections first left 948 translation
  errors; declaring the actual default Chinese UI and English About locales
  leaves 15 MissingTranslation findings (Malay About fallbacks), and 202 warnings.
  Full lint is not equivalent to release vital lint.
- **Runtime dependency lookup:** OSV querybatch returned no vulnerability matches
  for 91 resolved Maven package/version pairs on the review date. This is a
  database lookup, not proof that dependencies are vulnerability-free; vendor
  SDK/firmware and CI/build tools were not covered. Query/results are in
  [the recorded dependency evidence](project-safety-dependencies.json).
- The explicitly configured missing-keystore scenario fails configuration as intended.
- Resource XML and workflow YAML parse locally. Workflow read/write job boundaries
  and publication references were checked; real remote CI is a separate result.
- No vehicle or emulator was attached. Native window composition, Camera2/HUD,
  MediaExtractor playback/repair, real phone transfers, battery consumption,
  USB unplug/full-disk faults and installer behavior are not established by these
  tests.

## Next vehicle evidence

Start stationary in P: open each camera mode with recording active, trigger the
factory view, cancel/re-signal after closing the popup, and verify covers during
source loss and return. Confirm lane orientation and HUD selection. Test driver
seat/gear/Sentry behavior, USB transfer/collision/restart recovery, repair backups
and Android phone sharing. Check configured screen-off wake expiry and recording
closure while parked. Export Diagnostics for signal/display inventory; inspect
it for personal information before sharing. Existing owner reports for older
labels and mirror replacement remain valid, but are not tests of these new fixes.

## Primary references

- [Android app-specific storage](https://developer.android.com/training/data-storage/app-specific):
  cache reclamation and removal of app-private data on uninstall.
- [Android app signing](https://developer.android.com/studio/publish/app-signing):
  signing identity and private-key management.
- [GitHub release assets](https://docs.github.com/en/rest/releases/assets):
  asset size and SHA-256 digest metadata.
- [OSV batch API](https://google.github.io/osv.dev/post-v1-querybatch/):
  package/version vulnerability lookup.
- [Android WakeLock](https://developer.android.com/reference/android/os/PowerManager.WakeLock)
  and [AOSP implementation](https://android.googlesource.com/platform/frameworks/base/+/android-9.0.0_r45/core/java/android/os/PowerManager.java):
  timeouts are software mechanisms; platform/vendor behavior needs testing.

Locale metadata uses the [Android tools locale attribute](https://developer.android.com/studio/write/tool-attributes#toolslocale), preserving runtime strings and fallback behavior.
