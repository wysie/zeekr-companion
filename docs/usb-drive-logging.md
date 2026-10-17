# USB drive logging

Developer options → Drive logging to USB is an explicit session action (not a
persisted boot preference). With a writable USB present, it starts a low-importance
foreground service and a unique `ZeekrShortcutApp/Logs/Drive_<time>_<id>` folder.
The UI reports Checking USB, Saving logs to USB, Finishing, Saved or an error and
shows the exact destination. Stop is also available in its ongoing notification.
Wait for Saved and any other recording/export tasks to finish before unplugging.

## What is saved

- `signals-*.log`: scanner headers and raw observed function, customize-value,
  notification, byte, unfamiliar sensor-event and supported polled changes.
- `app-*.log`: AppLog messages after session activation.
- `lifecycle-*.log`: BlackBox events after activation (retaining its aggregation).
- `session-*.log`: start/end, limits, queue-loss markers and any skipped snapshot.
- `diagnostics-start.json` and `diagnostics-stop.json`: automatic read-only
  Diagnostics snapshots. The stop snapshot has a 15-second completion deadline;
  failure retains the log parts and reports the problem. Snapshots over 4 MiB are
  omitted with an explicit session marker rather than creating invalid JSON.

Files are written on a dedicated worker, flush and fsync every two seconds,
and roll into 8 MiB parts. They remain separate from the private scanner’s
60,000-event sample and filtered 4,000-event Diagnostics timeline. Raw USB capture
continues after that private cap. No previous session or part is automatically
deleted. App/lifecycle capture is not a full-system logcat dump, and the mode does
not open cameras, start video recording, change camera eligibility or write car
actuators. It does not backfill events from before activation.

## Ownership and limits

The drive session subscribes to the existing read-only scanner without setting its
manual toggle. Starting or stopping either owner re-evaluates the scanner deadline;
an independent manual scan keeps its preference, original deadline and private
sample budget. Summary session changes are serialized on the scanner worker and
retain callback subscriptions, avoiding duplicate registration. The private sample
now flushes at fixed intervals even under a continuously busy callback feed.

The drive session stops at 8 hours, 256 MiB total written, under 64 MiB free on USB,
USB/I/O failure or Developer mode shutdown. The selected volume is fixed for that
session; its device identity and writability are checked regularly. Primary
emulated/internal paths are rejected, with no internal or alternate-volume fallback.
Start/Stop never uploads logs. They remain private owner data and should not be
committed to GitHub.

The callback queue remains capped at 256 pending events; the USB writer queue has
1,024 entries. Scanner/writer overload is counted in explicit GAP markers. Individual
text entries are bounded to 8,192 characters (marked when truncated), and vendor
byte callbacks retain the scanner’s existing 64-byte preview/full-length evidence.
A sudden process death can also lose queued events and the most recent unsynced
writes. This is bounded research collection, not a lossless bus recorder.

It is START_NOT_STICKY, never boot-started and never automatically resumed after
process death. A persisted in-progress marker makes a later launch report an
interrupted session instead of Saved. Android may terminate the app when ejecting
the USB. A normal Activity closure keeps the logging foreground service alive;
force-stop, full app exit, reboot, power loss and firmware restrictions can still
interrupt it. Android 15+ also imposes a dataSync service budget and calls
onTimeout; the logger stops on that callback. See [Android foreground-service
timeouts](https://developer.android.com/develop/background-work/services/fgs/timeout).
Eight hours is the app cap, not a guarantee against an earlier Android limit.

Only signals actually exposed by the head unit can be recorded. No decoded
per-side blind-spot alarm has been established; callback IDs/payloads are evidence,
not a warning state. Recording approximate times of factory warnings still helps
correlate a drive, without interacting with these tools while driving.

## Validation

- JVM tests verify 65,000 raw events survive part rotation in order, older sessions
  remain intact, caps reject further writes without deleting evidence, disconnected
  health checks fail, and snapshots cannot overwrite earlier files.
- Disposable Android emulator with a virtual removable public volume: live USB
  writes before Stop, native scanner events beyond the private cap, Activity close,
  automatic snapshots, app/lifecycle capture, queue-gap evidence, repeated session
  preservation, internal-path rejection, truthful settings state, Developer-mode
  shutdown, independent manual-scanner ownership/expiry, immediate Start/Stop
  cancellation, and USB evidence when the private summary cannot be written.
- Manual virtual-volume eject experiment: Android terminated the app process; no
  Java crash was reported. Remount retained previously synced files. This is why
  interrupted-session status is persisted before activating capture.

Native fixture setup uses Android emulator `sm set-virtual-disk true`, partitions
that disposable disk as public, and passes its UUID as `-e driveLogUsbUuid <uuid>`
to DriveLogAndroidTest. Tests skip without the explicit emulator fixture. Never
partition an owner drive. No on-car capture or blind-spot warning decoding is claimed.

Final checks on 2026-10-17: 1,433 JVM tests, seven native logger tests, two existing
native popup settings/geometry regressions, Android lint and signed release
assembly passed. Signing identity is unchanged. No new GitHub release is created
by this feature commit; the APK still needs vehicle testing.
