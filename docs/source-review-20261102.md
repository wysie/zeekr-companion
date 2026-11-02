# Source and outgoing PR review — 2026-11-02

Companion base `5b902ba4` (local main, nine commits ahead of origin). Review only:
no runtime import, PR submission/update, source-mirror advance or release.

## Reviewed boundary

- GitHub dts88 main and the fetched local ref agree at
  `4d68857a882e3bf9d67b3ff52cd0d61802df6638`, source version 2.93.4-alpha.
  This is a source observation, not a new release-publication check.
- Since the general October 29 boundary `2ddbef0c`, the range has 82 commits,
  63 non-merge commits, and changes 83 files. Inventoried that range; inspected
  selected camera lifecycle, profile-editor, recovery, guide and storage code.
  This is not an exhaustive audit or a built/tested integration.
- Ferix CAMCORDER's recorded URL again returns authenticated GitHub 404.
  Last verified head remains `2dce2510c373d43cd850d8dc040d701311b63bb6`;
  current source availability and newer changes cannot be established.

## Incoming decisions

| Scope | Decision and fork-specific requirements |
| --- | --- |
| Camera rounds / live-round ledger, `cc4e1fbd`, `7bad47eb`, `d8a248a5` | **Priority adaptation candidate, not imported.** Current upstream tracks outstanding open callbacks and device closes by round, including late-arriving devices, rather than letting a timeout permit another open. Companion still has the older capped closing model. Preserve extra exterior roles, retained GPU outputs, manual Apply, recording intent and factory-camera priority when adapting. A genuinely hung vendor call can still leave that camera unavailable; this is lifecycle containment, not proof of a fix for the owner's incident or App Lab disappearance. |
| One process scheduler and serial pipeline handover, `a6493ff9` with follow-ups through `502364b1`; rescue as close then open, `c0bfa3bc` with follow-ups through `835c5b4d` | **Review/adapt with the round model as a coherent series.** Upstream's `ChannelFacts`, `ChannelScheduler.install/app`, pipeline handles and orphan bookkeeping differ substantially from the fork. Do not cherry-pick one timeout or replace the fork scheduler wholesale without porting its additional outputs and pending configuration semantics. No integration build or vehicle test in this review. |
| Former priority PR [#117](https://github.com/dts88/zeekr-shortcut-car/pull/117), head `93eae8e5d1b29359f014a01dd82bf9bc64cc8f56` | **Replaced as a recommendation.** GitHub now reports closed, unmerged. Current main has the later round-ledger/shared-scheduler approach above; the old PR documented unresolved late-device and old-pipeline ownership gaps. Do not describe #117 as open or recommend its head instead of reviewing its successors. |
| Settings application / profile edit rebasing, `286e2fd1`, `ddb5fe8a` | **Selective adaptation candidate.** The editor now overlays only locally changed keys on the latest stored profile, avoiding a stale editor restoring independently changed fields. Companion's editor still saves its complete profile. Compare this small rebasing behavior separately. Skip wholesale automatic application on return to main: Companion has an explicit pending/Apply flow, independent preview and recording choices, and owner-visible interruption behavior. Source front-cabin one-shot policy is not imported implicitly. |
| Selected USB return and actual destination visibility, merged #110 / `ae47ec7d` | **Carry forward the October 29 selective-storage recommendation.** Current source still has `returnHomeIfBack` / `reviveRemounted`; Companion lacks these. Preserve off-by-default internal fallback, relay and footage protections. This handles fallback/remount recovery, not demonstrated elimination of multi-second write/sync pauses on a still-mounted drive. No new USB-stall fix has been established by this scoped review. |
| Playback drawer, merged #109 / `f710237e` | **Carry forward UX candidate, not imported.** Use the existing October 29 scope and adapt to Companion's galleries and switches. |
| Bluetooth smart buttons / restart and sleep handling, through `2b4985c0`; exterior courtesy sound and drive-mode/shift actions | **Deferred product expansion.** These add another hardware integration and are not a demonstrated remedy for camera/USB stalls. Source changes inventoried, not fully audited or tested; no Bluetooth or vehicle-control feature imported. |
| Per-size line-height tokens, `2caa4e40` | **UI reference only.** Retain Companion's compact type scale; inspect individual clipping cases before adopting blanket token/theme changes. |

Earlier parking/Sentry, audio, cabin and signing decisions remain as recorded in
the October 29/23 reviews. No new compatibility or on-car claim is made here.

## Outgoing contributions

GitHub refreshed all 24 owner submissions: **11 open, 3 merged, 10 closed**.
Existing heads are unchanged. The live mergeability response now shows:

- Mergeable: #32, #44, #47, #50, #51.
- Conflicting: #29, #34, #37, **#52, #53**, #54. #52/#53 newly conflict relative
  to the October 29 snapshot; #53 and #54 depend on #52.
- Previously merged: #17, #28, #38. No newly merged owner PR identified.

No fresh check-run query or combined build was performed; mergeability is a
point-in-time GitHub result, not validation against today's base. Prefer refreshing
the existing exterior-source/recording/preview submissions before duplicating them.

New, scoped candidates (not prepared or submitted):

1. **Recover wanted recording after a latched access denial**, fork `6d64fea1`.
   Current upstream `ChannelRules.usable()` respects `unusableSince`, but
   `MultiCameraManager.cameraUsableNow()` still considers only foreground/service
   eligibility; `RecordingCoordinator.resume()` exits for an already logical
   recording and its recovery helper reads only that eligibility check. This is
   the same structural mismatch behind the owner's November 1 zero-live-camera
   incident. Port against the new pipeline/scheduler API, preserving one accepted
   foreground request per denial episode, screen/factory/quit guards and manual
   stop. Fork tests are implementation evidence, not tests of an upstream port.
2. **Localized illustrated mirror/install guidance**, fork `5a59c95c` /
   `ed8523e3`. Upstream still uses image-based guides; the fork's native diagrams
   keep illustrations while labels/body text follow the selected language and
   theme. An upstream port must preserve its actual gestures and installer
   confirmation/cancel behavior and omit Companion branding/extra controls.

The continuous-framing GPU fix (`4dc93b21`) is not a small standalone upstream
bug fix: upstream lacks the fork's `SideGpuOutput` / `SideGlRequest` side-output
path. Offer it only as part of a separately scoped renderer contribution, with
its freshness/lifecycle tests. Do not submit fork-specific code against absent APIs.

## Owner storage investigation and preference

The owner declined internal recording; keep direct USB recording and do not
enable relay staging. The subsequent [USB-only source/log trace](009-diagnostics-20261102.md#usb-only-follow-up-writesync-and-cleanup-trace)
identifies repeated all-camera native write stalls overlapping clip deletion
under the app's 10 GB cap, despite approximately 228 GB free. Separate long syncs
also occur, including one starting before that segment's deletion decision.
These observations do not prove faulty hardware, a GPU cause or cleanup as the
sole cause. A possible USB-only comparison is raising the video cap to 100 GB
while preserving other settings; no setting was changed for the owner and no
unvalidated sync/queue policy was introduced. This delays cap-driven cleanup,
retains more footage and does not guarantee removal of sync stalls.

## Validation limits

Read-only GitHub head/PR/availability queries and local source comparisons;
documentation-only edits. No app settings were changed remotely, no runtime
changes imported, no new app tests/builds, no physical-car test and no release.
