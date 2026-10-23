# Recording storage stalls — 22 October 2026

The owner's alpha 61 drive log covers 13:11–13:42 Singapore time. It contains
seven writer-queue-full warnings, a measured 5,184 ms USB file sync and writer
stack samples waiting in `ActiveFootage.Lease.close`/`acquire`. The 13:35 cleanup
check inventories 892 clips; a writer reports recovery after a 3,166 ms wait.
Raw logs/photos and private analysis remain outside tracked source.

Two app changes address this evidence:

- Recorder ownership now uses a short, private in-memory monitor. Durable footage
  metadata has its own guard. Canonical path resolution, directory/statfs scans,
  metadata reads/writes and unlink operations do not hold the ownership monitor.
  An unlink atomically reserves only its closed path, excludes concurrent opens
  and duplicate unlink attempts, and releases its reservation even on failure.
  Unrelated writer close/open operations do not wait for that filesystem work.
  MediaRecorder creation follows successful ownership admission.
- The encoded write queue now admits up to six seconds rather than three, still
  subject to its existing 24 MiB queue ceiling and 64 MiB total retained-data cap
  per recorder. A deterministic 30 fps / 24 Mbps replay of the observed 5.2-second
  pause retains every encoded sample in order. A longer pause still reaches a
  finite admission limit. Existing flushes, keyframe-safe rescue, saved quality
  choices and bounded stop deadlines are retained. Late-file event locking uses
  the longer backlog plus the full writer stop/grace allowance; its second pass
  waits 28 seconds after the ten-second forward window.

Cleanup inventories outside the metadata guard, then rereads current protection
before each unlink. Active/open/reserved files remain protected independently of
whether persistent footage locking is enabled. A newly protected or failed candidate
is kept and excluded from retry within that sweep; remaining candidates are replanned
instead of claiming space was freed. Successful delete/metadata forgetting serialize
with lock edits. Playback uses the same active-path deletion admission.

The file-list snapshot at recorder stop is also copied under its list monitor before
resolving filesystem aliases, keeping slow path checks out of that shared monitor.

Validation: 1,560 JVM tests, ten targeted native Android checks, five Python
release/packaging checks, all APK variants and Android lint (zero errors) passed.


- JVM tests cover reference counts, abandoned writer protection, filesystem aliases,
  blocked unlink and alias resolution, failed deletion reservation release, short-
  stall recovery with sample/command ordering, sustained backpressure/memory bounds,
  and delayed event-lock timing.
- Native Android regressions inject blocked inventory and held metadata guards,
  then require independent recorder close/admission to finish within one second.
  Locks and writers created after an inventory snapshot survive real cleanup.
  Failed unlinks are attempted once and cleanup can replan to another eligible group.
- Existing native cleanup and gallery tests retain active older-named recordings,
  same-named media in different folders, newly locked files and unreadable lock lists.
  Manual/flash locks retain their ownership; a real delayed second pass still runs
  after recording stops with the extended timing.

Limits: these are source, deterministic replay and disposable-emulator checks. They
do not speed up a USB device's native sync, guarantee lossless sustained overload,
or prove on-car blind-popup/HUD/GPU smoothness. High bitrate, Unlimited FPS and
concurrent USB research logging remain additional load in the supplied session.
Real-car validation is still needed; no resolution/FPS/bitrate or GPU preference
was silently reduced to make the tests pass.

## Publication

Published in [v2.11.62-alpha.1](https://github.com/wysie/zeekr-shortcut-car/releases/tag/v2.11.62-alpha.1), version code 345.
Tag `dbae67bd88f963fd1ca8bfb44884627bd8e7786c` includes implementation
`98b81176` and main integration `29a280d5`. The combined release passes 1,564
JVM tests, zero-error lint, five packaging/note checks and signed assembly.
Downloaded public APK/signature/checksum verification adds no vehicle-performance
claim; the physical USB and smoothness limits above still apply.
