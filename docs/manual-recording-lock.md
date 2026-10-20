# Manual recording lock — 2.11.55-alpha.1, 2026-10-20

Floating button → Tap / Double tap / Long press now offers Lock current recording.
Existing assignments and default-off double tap are preserved. Lock footage must
be enabled and a recording active. The flash/braking automatic switches need not
be enabled. All matching recording sources are protected over ±10 seconds using
the existing clip-window and two-pass logic; the second pass survives recording
stopping and includes a clip created after the press. Relay waits keep their bounded
retry policy. The result reports success (including already locked), nothing to
lock, disabled protection or failure to save, with one reply per button press.

Adapted dts88 #61 implementation `e8dedd81` and approved text `ccd441a4`, with
source authorship retained. The fork keeps its FLASH/BRAKE source distinction,
braking-only automatic budget, native source/file roles and atomic lock lists.
BUTTON is a third source. An explicit press also rewrites already protected
matches as user locks, removing any automatic-brake classification so the budget
cannot later release the user's requested protection. Braking keeps its full event
set while deciding which earlier automatic locks may be released.

Validation: 1,516 JVM tests, debug lint with zero errors, debug/test/signed release
assembly, and six focused API32 native tests pass. The two storage cases exercise
the public AutoLock entry, inactive/disabled gates, four footage roles, automatic
lock promotion and the real delayed second pass after Stop. The four floating
tests check gesture arbitration, cancellation, foreground visibility and persistence
of the sixth action through native Settings/recreation. Local logs:
`/tmp/companion-manual-lock-build.log` and `/tmp/companion-manual-lock-native.log`.
Emulator storage/touch tests do not prove USB durability, vehicle timing or every
simultaneous recording combination. No release published by this change.
