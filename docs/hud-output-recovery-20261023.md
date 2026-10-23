# HUD output recovery — 23 October 2026

The owner reports that the main-screen blind popup remains live while the HUD
says “No live picture”; turning the indicator off and on restores it, with GPU
image processing set to All. No new drive log is available for that occurrence,
so the initiating on-car event is not established.

A reproducible recovery gap exists in `SideViewHudOutput`: it only checked its
binding when showing a different/new indicator episode. The already-open or
standby HUD could retain a missing declaration or an obsolete camera-manager
reference, while the popup's independent watchdog followed the live pipeline.
Native tests against the released implementation fail for both missing-declaration
and pipeline-replacement cases without another indicator cycle.

The HUD now checks its ownership every 500 ms while attached. It repairs only a
missing declaration/claim or changed manager/camera/texture; it does not repeatedly
redeclare a healthy output or restart a camera merely because frames are late.
Old camera declarations are withdrawn, source geometry/freshness is cleared on a
pipeline change, and late callbacks from the old window/owner are ignored. Pending
startup retries retain their bound, new windows receive a fresh retry budget,
and configuration rejection retains the existing ten-second cooldown/fallback.
Detach/dispose cancels the check; screen-off and calibration pause new binding.
The actual camera session remains under the channel scheduler.

The no-live-picture cover remains until frames actually arrive. GPU selection,
HUD framing/calibration, popup routing, recording intent and saved settings are
unchanged. This introduces no new setting.

Validation: 1,564 JVM tests, debug/release/instrumentation builds and zero-error
Android lint. Seven new native HUD tests cover visible and standby recovery,
manager replacement/late failure, missing HUD ownership, healthy/refused output
behavior and teardown. Real synthetic EGL frames on a disposable secondary display
exercise both GPU All and compatibility rendering: live → stopped/covered →
rebound/still covered → resumed/live, without toggling the indicator or replacing
the texture. These run with existing HUD placement/safe-area, popup lifecycle and
camera surface-ownership regressions. A pre-existing popup test's assertions now
run after the UI binding turn completes; its former background-thread check could
observe the owner assignment before the output declaration.

This fixes the demonstrated recovery gap; emulator results do not identify the
original car-side interruption or establish physical HUD smoothness. If it recurs,
a new drive/diagnostic report is needed to distinguish a lost binding from a
native-output or GPU stall with the binding still intact.

## Publication

Published as [2.11.63-alpha.1](https://github.com/wysie/zeekr-companion/releases/tag/v2.11.63-alpha.1),
Android version code 346. Tag `25735a3c16911cb2543aa7330171700a408d0c3b`
contains implementation `2804dbb6`; tag CI
[39928957601](https://github.com/wysie/zeekr-companion/actions/runs/39928957601)
passed. All 20 combined native checks pass, including the seven new HUD cases.
Downloaded draft and public APKs match their checksum/GitHub digest, retain the
existing signing/application identity and have matching trilingual release notes.
SHA-256: `f3e1fb931a63daa92135f0b5785eaf1db50b2f50ff52410490cd9c388000ae01`.
This is an alpha with Latest off; on-car confirmation remains pending.

## Subsequent owner trigger clarification

The owner subsequently identifies All ↔ Custom GPU policy changes as the trigger,
rather than steady GPU-on/off use. Alpha 63 fixes missing camera ownership but
does not stage every renderer's saved policy. The [saved/applied GPU policy
correction](unified-gpu-rendering.md#saved-and-applied-policy--23-october-2026)
addresses that separate mismatch and adds explicit transition/handover coverage.
The original car-side interruption still has no new diagnostic log.
