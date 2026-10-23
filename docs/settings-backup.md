# Companion settings backup — 2.11.55-alpha.1, 2026-10-20

System → Settings backup offers Export settings and Import settings. Files are
`ZeekrCompanion-settings_yyyyMMdd_HHmmss_SSS.json`, beside diagnostics reports,
following the configured export storage. Export reports the saved path in a dialog.
The import picker lists backup files newest first and requires confirmation.
It retains file metadata while listing and loads only the selected settings map
for confirmation, so many backup files do not retain many complete maps in memory.
Cancel changes nothing. Confirmation stops recording and exits normally; reopen
Companion to apply the saved settings. Last restore status remains on this page.

This adapts dts88's open #64 at
`35bdfc197c208a983eb4fb5dc1c8ede4813db265`, with original authorship retained.
It is a Companion adaptation, not a claim that #64 has merged upstream.

## Included and excluded state

All five preference stores are carried with their Android types:
`app_config`, `profiles`, `bird_eye_calibration`, `bird_eye_window` and
`bird_eye_appearance`. This covers vehicle/source overrides, recording/preview
choices, quality profiles, theme, floating gestures, GPU policy, side/HUD tuning,
360 calibration/intent, vehicle illustration selection/colour and window settings.
Explicit-choice migration markers travel with the choices; for example, a fresh
install must not reset an imported non-default JPEG choice during initialization.

Developer access, platform permissions, boot/first-use state, current recording
intent, detected camera/slot inventory, diagnostic history and signing identity
stay local. Settings files contain preferences, not recordings or external artwork
files. Saved text such as plate stamps is included. Diagnostics schema 3 includes
the same complete settings snapshot, with no 80-character truncation.

Only matching-format, same-major Companion files with this application ID import,
with one explicit bridge: Companion 3 also accepts Companion 2.11 backups from
2.11.55-alpha.1 onward, since their five-store schema is unchanged. This does not
allow 3.x backups on 2.x or imports from unrelated 2.x/4.x versions.
Upstream's settings-file kind is deliberately distinct. Missing stores, malformed
or oversized files, invalid declared types, fractional/overflowing integers and
type changes to already-stored keys are refused before writing. Files are bounded
to 1 MiB while reading and writing. Export uses a verified, synced atomic file and
avoids overwriting a same-name export. Import preserves the currently detected
hardware rather than copying detection caches from another unit.

## Restore publication and recovery

Import confirmation writes a private, durable pending snapshot; it does not change
live preferences. Normal exit finalizes the current clips and shuts cameras down
using their current settings. The earliest KeepAliveProvider invokes restore before
reading AppConfig or starting services. Application.onCreate has an idempotent
fallback before theme, language, recording intent, source mapping and watchers load.

Preferences span several files, so one SharedPreferences.commit is not a transaction
across them. SettingsRestoreTransaction first persists their complete settings
preimage. It commits each store synchronously, then writes a durable committed
marker before deleting the journal. A failed write rolls every store back. A process
interruption with a prepared journal rolls back on next startup; a committed journal
retains the new settings even if its deletion had failed. Runtime keys are not
replaced during either restore or rollback. Services cannot read a half-restored
configuration during this startup path.

A rollback that cannot persist keeps its recovery journal and prevents ordinary
startup rather than using a partial configuration. It retries recovery on the next
start. Private journal corruption or persistent storage failure therefore requires
storage/recovery attention; no claim is made to recover arbitrary lost storage.
The public UI never applies a backup to a live pipeline. Tests call the synchronous
engine directly to verify its preference and journal boundaries.

## Verification

- Companion: 1,528 JVM tests pass, including six fault-injected transaction tests
  and upstream backup/version rules; debug lint has zero errors and debug/test/
  signed release assembly passes.
- Five focused API32 native cases pass: typed five-store round trip with local
  runtime/access state; foreign/future/malformed/mistyped/oversized refusal;
  prepared/committed journal recovery; distinct exports/full diagnostic snapshot;
  and actual System export/import-Cancel controls with unchanged settings.
- Fresh-process emulator check stages a real pending payload, force-stops and
  reopens the app, and verifies restored app, calibration and window values plus
  cleared pending/journal files. The original preference files are restored after
  the check. This exercises the earliest Provider/Application startup path.
- Actual Settings backup, import confirmation and floating lock-action picker were
  inspected in both light and dark themes in one batched pass. No clipping or
  missing actions found; scoped Impeccable detector reports no findings.

Local evidence: `/tmp/companion-settings-backup-build.log`,
`/tmp/companion-settings-backup-native.log`,
`/tmp/companion-settings-backup-picker-native.log` (final selected-file loading),
`/tmp/companion-backup-fresh-process.log`,
`/tmp/companion-recommendations-ui-detect.json`, and six
`/tmp/companion-{backup,import-confirm,lock-action}-{light,dark}.png` captures.
These are temporary test files, not release assets. Vehicle storage, full USB/power
loss durability and a physical restore on 009/7X still need vehicle testing.

## Companion 3.0 numbering transition — 23 October 2026

The 3.0 release keeps the application ID, signer and preference schema. An explicit
format-1 import bridge accepts this fork's 2.11 backups beginning with the first
settings-backup release, 2.11.55-alpha.1. Unrelated 2.x versions, earlier 2.11
versions, wrong formats, invalid versions and 3.x-to-2.x downgrades remain outside
that bridge. Kind, application ID, store, typed-value and bounded-file validation
still run before any preference writes. Import summaries/rejection wording are
updated in English, Malay and Simplified Chinese. Existing installed settings do
not require export/import for the update.

Version/update and compatibility tests cover the 2.11 → 3.0 ordering, Alpha channel
filtering and the narrow backup bridge. The 3.0 APK restores a real typed five-store
2.11.64-labelled backup in a disposable emulator, including GPU and framing values.
1,566 JVM tests, builds and zero-error lint pass. Twenty-one backup/inherited UI
checks pass at the intended 2560×1600 viewport; an initial run exposed a leftover
1920×1080 emulator override that hid unrelated controls, then passed after reset.
The four affected import/export/UI cases are rerun after final translated wording.
These checks make no vehicle-performance claim.

Published as [3.0.0-alpha.1](https://github.com/wysie/zeekr-shortcut-car/releases/tag/v3.0.0-alpha.1),
version code 348, tag `1ee321431748723ca642da216ce13324d1769a2a` including
transition/compatibility commit `cc03ccd1`. Tag CI
[39931968564](https://github.com/wysie/zeekr-shortcut-car/actions/runs/39931968564)
passes. Downloaded draft/public APKs verify the retained application/signing
identity, exact version, checksum/GitHub digest and all three note languages.
APK size 23,359,902 bytes; SHA-256
`aad9aa15d133a4d367cdd2b5950e76e35217d1c38275b3d836ea734ef048aad1`.
Published Pre-release with Latest off. The final four native import/UI reruns pass.
