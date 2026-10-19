# Recording startup blocked by a pending preview

## Owner evidence, 2026-10-19

The supplied drive export identifies the installed app as **2.11.52-alpha.1**.
At 13:02:29, 13:02:59 and 13:03:29, RecordingCoordinator refuses startup with
“Camera selection changed. Exit and reopen the app to apply it before recording.”
The export also contains earlier occurrences. Cameras 0, 1 and 3 have active
sessions and capture results, but no RECORD outputs are requested. An external
USB volume is detected. This establishes a configuration refusal before recorder
startup; it does not establish a USB or camera hardware failure.

Saved preview selection is Surround, while the active pipeline includes the
separate road sources. The exact user action that first produced the pending
configuration cannot be reconstructed from this snapshot. Launch recording is
off, driver-arrival recording is on, and the exported recording intent also has
the user-stopped flag set. Those are independent reasons why opening the app or
remaining in the same drive does not itself force an automatic recording.
This fix preserves those choices and their existing stop/recovery rules.

Raw exports, footage, vehicle identifiers and private paths are not committed.

## Cause and correction

The camera holder's broad shape key covers recording roles, preview roles,
overlay needs and manual source choices. RecordingCoordinator used that whole
key as its restart gate before and after asynchronous storage checks. Consequently,
a saved preview change could reject manual and automatic starts even when all
requested recording sources were already present in the current pipeline.

The holder now captures a separate immutable `RecordingSetup` when it creates
the pipeline. This compares the vehicle camera model, physical 009 mapping,
enabled recording-role identities and manual source assignments for those roles.
Preview choices, source-pane arrangements, overlay switches and inactive-role
assignments do not affect recording compatibility. Recording-source or active
mapping changes still refuse new starts until reopening adopts them. Both sides
of the asynchronous storage check use this recording-specific gate.

The broader main/settings pending state is retained: preview changes are still
saved for reopening, rather than silently rebuilding cameras during recording.
No preference migration, automatic-start default, source resolution, camera
ownership, factory priority, storage policy or signing identity changes here.

Diagnostics now distinguish `mainPreviewPending` and `recordingPending`, and
include built/requested recording identities plus the requested preview mode.

## Verification

- JVM regression coverage: pending preview sets, identical camera unions with
  different recording selections, immutable active mappings, Automatic reset,
  inactive mappings, role ordering and vehicle/mapping identity.
- Disposable Android emulator: pending preview and overlay edits keep the same
  pipeline and do not mark recording pending; active recording/mapping edits do.
- Disposable Android emulator: USER and DRIVE requests both write separate MP4
  files while a different preview choice is pending. Each finalized file has a
  readable video track and media sample. Only test-created footage is removed.
- Debug app/test builds, signed release build, all 1,499 JVM tests and Android
  lint pass. The two new native regressions pass, as do eight existing source-group
  regressions and the existing pending-preview/reopen regression (11 native tests).

Emulator recording uses its virtual Camera2 feed and internal storage with a
temporary Developer setting. This verifies recorder startup, actual encoded
output and file finalization, not the 009 hardware, USB drive or an entire
vehicle drive. The owner still needs to validate the updated build in the car.

For an installed older build, **Quit app → reopen** applies saved camera/preview
choices before trying Record again. Quitting stops recording. Launch automatic
recording and driver-arrival recording remain separate opt-in settings.
