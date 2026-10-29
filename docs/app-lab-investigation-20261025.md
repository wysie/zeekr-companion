# App Lab disappearance: code trace, 2026-10-25

**No incident root cause established.** The owner reports needing to reinstall
App Lab while using Companion. A guest-process crash, an unavailable App Lab
launcher, loss of its virtual-app catalogue, and actual host-package removal are
different failures. The incident has not yet been classified between them.

**October 29 follow-up:** upstream's open [PR #117](https://github.com/dts88/zeekr-shortcut-car/pull/117)
at `93eae8e5d1b29359f014a01dd82bf9bc64cc8f56` addresses reopening while a
previous close is still outstanding. Companion retains the capped wait described
below, so this is a concrete new adaptation candidate. It is unmerged, not
car-tested by its author, and does not establish the App Lab root cause. See the
[current source comparison](source-review-20261029.md).

Reviewed Companion `ab43b65e896de745cfe34a8f0ed5c1ea37e54429` and the relevant
runtime paths in published `v3.0.0-alpha.2`
(`55af026f2c1cac3c91cf5216ab735384fa405f3e`). Compared selected shared code and
history with dts88 `ed6f64195f3414b25aa139f2c8d15c95cf1ed8bf`. This is a scoped
App Lab investigation, not an audit or integration of every new upstream commit.

## Public evidence

- [Upstream issue #12](https://github.com/dts88/zeekr-shortcut-car/issues/12)
  reports repeated loss after head-unit restarts, including after downgrading.
  The maintainer suspected surround/cabin camera contention and recommended
  2.11.7-beta. Closure is not proof of the proposed cause or universal recovery.
- Upstream's current README still lists App Lab disappearing after a restart as
  an unresolved known issue. Its separate instruction to press **Back**, rather
  than **Open**, after installation concerns a stuck installer; there is no
  evidence here that this explains host removal or catalogue loss.
- The historical surround-before-cabin shutdown correction
  `6d504cae91fe5f6fdb5cd3f0040a405e8aba233b` is already an ancestor of our alpha.2
  release and its ordered-close logic remains in our `ChannelRules`. Reapplying
  that old fix is not a new remedy.

## Relevant code paths

| Area | Trace and finding | What it establishes |
| --- | --- | --- |
| Automatic resurrection | `AppConfig.isAutoStartOnBoot()` defaults **true**. `KeepAliveProvider.onCreate()` schedules an own foreground-service start after one second on Android 12; `BootReceiver` also starts it on boot. `KeepAliveReceiver` reacts to numerous events with a three-second start throttle. `KeepAliveManager` schedules a 15-minute WorkManager task. `CameraForegroundService.onDestroy/onTaskRemoved` can request restart by broadcast and after one second. | A failing guest can be started again without the user opening it. There is no persistent crash-loop circuit breaker in these paths. This is a plausible amplifier of a host/guest failure, not evidence of catalogue deletion. |
| Restored workload | `CameraForegroundService.onStartCommand()` calls `Recovery.restore()`, which posts to the main thread and waits for known screen state before applying saved recording/overlay choices. Camera opening still goes through scheduler eligibility and recording intent. | A service restart can restore work; it is not merely an idle notification. It does not mean every boot immediately opens every camera. |
| Camera/encoder lifetime | `ChannelRules` serializes camera operations and orders planned surround/cabin closure. `SingleCamera.closeCamera()` moves native close work to its camera thread and records outstanding closes. Some scheduler waits are capped at 60 seconds; this cannot cancel a hung vendor Binder/HAL call. `MainActivity.finishExit()` ultimately uses `System.exit(0)` after bounded cleanup, unless the user reopened the app. | Native camera/encoder stalls and forced process termination remain possible interactions with App Lab. No trace connects them to removal of the host. These paths are substantially inherited, not introduced by the branding change. |
| Foreground-service startup | Provider, broadcasts and boot can request a foreground service from the background. `CameraForegroundService.start()` catches rejected starts and tries an ordinary service start; actual `startForeground()` is later in `onStartCommand()`. | Android restrictions, delayed initialization or App Lab's service proxy can cause startup failures. An ordinary Android FGS failure causes a crash/ANR; it does not by itself establish an uninstall. See [Android's troubleshooting guide](https://developer.android.com/develop/background-work/services/fgs/troubleshooting). |
| Installer | `UpdateFlow.install()` sends an own FileProvider APK URI to the installer. Its permission fallback opens Settings for our own package. Downloads live under our cache subdirectory. | No host-package uninstall/disable request was found. Fork and upstream both use `io.github.dts88.zeekrshortcut`, so they update the same guest and can retain settings when installed over one another. App Lab's observed host identity, `com.puneetlj.box`, is different. |
| File deletion | `StorageGuard`/`StorageCleanupManager` filter recognizable footage names in current/legacy footage directories. Transfers and archive moves remove selected source files after verified copying; updater, log rotation and backup cleanup remove their own staged files. No recursive host-data wipe path was found. | The inspected cleanup paths do not explain deleting App Lab's virtual-package database. File hooks inside the closed-source container still cannot be audited from this repository. |
| Vehicle APIs and shell | Inspected vehicle paths read values and register/unregister listeners. The two `Runtime.exec` sites in `StallWatch` and `DiagnosticsCollector` run read-only logcat. No package-delete, app-disable, clear-user-data, reboot or destructive shell operation was found in app sources/manifest. | No direct removal mechanism identified. This is a source-trace result, not a guarantee about Android, vendor or App Lab internals. |

## Available owner evidence and its limits

The newest supplied drive session is **2026-10-22**, app **2.11.61-alpha.1**;
it predates the current report. Its diagnostics show keep-alive enabled, active
surround/front/rear recording with popup/HUD consumers, and cabin channels
disabled in the active profile. They contain VLite file-hook frames and a
`com.puneetlj.box:vlapp0` process entry. Existing platform notes also record
foreground and Job services being proxied through VLite StubService.

This confirms container involvement, **not that Companion shares the App Lab UI
process or that one guest crash wipes every guest**. The old report has a prior
SIGKILL (status 9); that does not identify who sent it or why. Its sampled Java
heap/system-available memory is not exhausted, and no `OutOfMemoryError` or
`FATAL EXCEPTION` marker was found in that report. Neither fact excludes later
GPU/native exhaustion, host failure, storage corruption or firmware action.
It is especially inappropriate to claim the historical cabin conflict occurred
in this owner's current setup solely because issue #12 mentions cabins.

## Working hypotheses and discriminating evidence

1. **App Lab recovery/virtual-app state failure exposed during shutdown or
   restart.** Consistent with the historical report; neither the actual host
   failure nor database corruption has been observed here. Establish whether
   the host icon vanished, its guest list reset, or the host merely would not
   open; obtain the incident time, head-unit/App Lab versions and host logs if
   available.
2. **Keep-alive repeatedly restores a failing workload.** Concrete default-on
   code path. For a controlled comparison, turn **Auto-start on boot** off and
   use **Quit app** before the next normal parked shutdown. This is an isolation
   test, not a fix, and does not require intentionally rebooting into a failure.
   Automatic recovery is unavailable while that switch is off.
3. **Shared camera/encoder/render resource failure.** Relevant to this app's
   workload and upstream history, but not proven by the supplied old logs.
   Compare a reduced camera/overlay workload separately from keep-alive; changing
   GPU alone would not isolate camera HAL or encoder pressure.
4. **Installer flow or unrelated head-unit/App Lab policy.** Correlate with an
   update/installer event versus a normal parked restart. Continue using the
   documented Back-then-launch installation flow; do not call it a proven cure.

Useful next hardening is a persistent, conservative recovery-failure backoff and
better host/guest process attribution in exported diagnostics. Designing either
requires preserving normal recording recovery and distinguishing a normal
system stop from a crash. Do not silently disable recovery or switch camera
policies under the label of a confirmed App Lab fix.

## Validation and disposition

Read issue comments, current upstream warning, relevant history, startup/service/
recovery/exit/installer/storage/vehicle call sites, and existing owner diagnostics.
Verified old ordered-close fix ancestry in the published tag. Source searches
covered Java/Kotlin/native-source extensions and the manifest; no bundled app
JAR/native library files were present in this checkout (Gradle dependencies and
closed-source platform/container code were not decompiled). Documentation diff
checked. **No runtime change, app build, new reproduction, vehicle test, upstream
import, PR update or release.** Owner photos/logs were not copied into the repo.
