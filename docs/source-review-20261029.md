# Source and outgoing PR review — 2026-10-29

Companion base `1cc2ed11d0c9d25f9e186a04cd5ca21749fd8a1f`, after publication of
`v3.0.0-beta.1`. This is incoming-change triage and selected source comparison;
no runtime import, PR modification, source-mirror advance or new release.

## Sources and availability

- **dts88 main:** `2ddbef0cac65daac7abc9d7ba379c0a2b72fc32f`, dated October 27,
  version **2.31.0-alpha in source**. There are 98 commits including merges
  (80 non-merge) since the full October 23 review at `91a4282e`. Sixteen commits
  follow the October 25 App Lab-only inspection at `ed6f6419`.
  The GitHub release list still reports **v2.25.0-alpha** as the newest published
  release; a source version bump is not publication of that version.
- **Ferix CAMCORDER:** fetching the recorded URL fails with “Repository not
  found”; authenticated repository/commit queries return 404. Its last locally
  fetched head remains `2dce2510c373d43cd850d8dc040d701311b63bb6` (2.14.3).
  Current availability/updates cannot be verified. Do not interpret unchanged
  local refs as proof that there are no new commits, or infer deletion/private
  status from a 404. A cached browser copy of the old public README is not a
  current source-head check. A separate anonymous API attempt was rate-limited.
- Ferix's public user/repository API currently lists
  [Veil](https://github.com/FerixzTanz/Veil), an AirPlay receiver/dimming overlay,
  created October 28. Its head is
  `179b94b18e8d64de730e31d24323b468fcfb1a3e` (0.2.0-alpha). Its README describes
  a separate app; it does not establish where CAMCORDER went. No Veil source
  import is proposed or audited.

## Incoming decisions

| Source scope | Comparison and recommendation |
| --- | --- |
| **Open [#117](https://github.com/dts88/zeekr-shortcut-car/pull/117)**, exact head `93eae8e5d1b29359f014a01dd82bf9bc64cc8f56`: never reopen before an outstanding close returns | **Highest-priority adaptation candidate; not imported.** Companion `SingleCamera.openOnCameraThread()` still waits up to 60 seconds and then attempts open even if the old close has not returned; `finishClosing()` also uses a capped wait. The PR replaces this with an outstanding-close chain and indefinite camera-thread wait, lets other lanes proceed after the initial wait, avoids counting rescue against a held lane, and handles a multi-thread close-chain deadlock. Reviewed the PR body, file inventory, `CloseChain` and `SingleCamera` patch plus local call sites. CI is green, but the author explicitly reports no car test and two remaining lifetime gaps. It is still open and absent from upstream main. This addresses a real overlapping-close/open risk, **not a proven App Lab disappearance cause**. Port as a coherent camera/scheduler/recorder change with regressions, rather than simply removing one timeout. |
| **Merged [#115](https://github.com/dts88/zeekr-shortcut-car/pull/115)/[#116](https://github.com/dts88/zeekr-shortcut-car/pull/116)**; `b5de9b1b`, `5a260f56`, `e03d489a`, `44df7cda`, `e080b848`, follow-ups through `bff355bd` | **Recommend a separate parking/recovery adaptation review.** A remote wake with ignition ACC or lower no longer necessarily counts as the driver returning. After 30 seconds dark, surround retention requires the keep-recording setting and Sentry on/armed; cabin outputs are released, and all cameras release when the head unit sleeps. The recording coordinator consumes the scheduler's shared decision rather than independently trying to record released cameras. It also limits dark-start/photo behavior. Our `VehicleScreen.verdict` lacks the historical “lit needs evidence” input and our `ScreenOffPlan` lacks this channel verdict. These are coupled policy changes that can shorten existing parked recording; do not silently import an isolated condition or assume 009 signal compatibility from 7X logs. |
| **Merged [#107](https://github.com/dts88/zeekr-shortcut-car/pull/107)/[#110](https://github.com/dts88/zeekr-shortcut-car/pull/110)**, final `ae47ec7d564039d3cc8c5ff829d5ca9fb8017e52` | **Recommend selective storage enhancement.** Upstream shows the actual fallback destination and returns to the selected USB at a segment boundary once remounted. Failed drives are retried only after observed removal/remount, avoiding repeated attempts on a mounted broken drive. Our `FallbackVolumes.rank` ranks rescue choices, but our recorder lacks upstream `returnHomeIfBack`/`reviveRemounted`. Preserve our explicit **off-by-default internal-fallback setting**, active-footage/lock protections, bounded USB probes and relay behavior; upstream's Developer-mode-only fallback policy is not a drop-in replacement. The intermediate #107 policy was superseded by #110. |
| **Merged [#109](https://github.com/dts88/zeekr-shortcut-car/pull/109)**, `f710237eaa42`: drawer directly on playback screens | **Useful UX candidate.** Upstream extracts a shared `NavDrawer` and keeps the drawer on the current photo/video page. Companion video playback still routes its menu through `openDrawerOnMain()`. Adapt navigation to our galleries, System settings and drawer switches; do not replace our drawer styling wholesale. |
| **Merged [#97](https://github.com/dts88/zeekr-shortcut-car/pull/97)**, `9e6e7d0a35e29670edf677af6a8cdcd0cc34de07`: hazard auto-lock | **Optional enhancement, deferred.** Off by default; protects all-camera footage from one minute before hazard activation through switch-off, renewing while held. Companion has manual/flash/braking protection but no hazard source. A port needs our event-budget/lock-ownership rules and verified 009 hazard signals. |
| **Merged [#108](https://github.com/dts88/zeekr-shortcut-car/pull/108)**, `b3c0e343be21ad3a8fb2cfa1b107b7b155139b04`: Media3/ExoPlayer | **Defer pending playback need/audio work.** Replaces MediaPlayer to avoid head-unit audio-clock lag freezing video, forces video onto the system clock and caps buffering. Adds roughly 4.7 MB in upstream's measured APK. Companion still uses MediaPlayer and currently records silent footage; that audio-related failure has not been reproduced here. Full decoder/lane/GPU playback interaction is not audited or built in this pass. |
| **Merged #100/#101 settings alignment and controls**, `7e70bcca`, `9ac82a0b`, `01f14228` | **UX reference, not a wholesale import.** Consistent row alignment, single-line values when space permits and dialog text consistency are useful review cases. Retain owner-approved compact typography and current settings architecture. This is an inventory, not a new reproduction of clipping or a complete UI audit. |
| **Merged #102–#106/#112 drive sound, audio defaults and vehicle panel** | **Defer.** Synthesized engine/jet sound, speaker routing, pedal tracking, moving recording audio to the normal page/on by default, and panel changes are separate product scope. They do not directly fix our blind-camera latency. Only inventory/release notes and selected metadata were reviewed; the large audio implementation is unassessed beyond this triage. Do not enable microphone recording or exterior sound as part of lifecycle repairs. |
| **Open #113/#114** | #113 adds drive-sound measurements, inapplicable without that feature. #114 is parking evidence: on the source owner's 7X, Sentry-on cycles did not power down and no App Lab issue was observed. Useful research context, not proof or a permanent keep-Sentry-on recommendation. Both remain unmerged. |

Previously deferred rear-cabin mirror/audio/HUD/style scopes retain their
[October 23 decisions](source-review-20261023.md) unless changed above. The
App Lab code trace gains a more concrete lifetime candidate through #117; no
source here establishes a fix for actual App Lab removal/catalogue loss.

## Our upstream PRs

All 24 submissions checked through GitHub, including mergeability and checks of
the eleven open heads. **11 open / 3 merged / 10 closed**, unchanged; no new
merged or closed submission since the October 23 follow-up. Every open head has
a successful build check; this does not test today's base combined with its diff.

| Current mergeability | PRs |
| --- | --- |
| Mergeable | #32, #44, #47, #50, #51, #52, #53 |
| Conflicting | #29, #34, #37, #54 |
| Previously merged | #17, #28, #38 |

The four conflicts have appeared since the follow-up reported all eleven clean.
Refresh them in separate contribution branches if requested, preserving #53/#54's
#52 dependency and existing heads. No PR head, title, body or comment was changed.

## Validation limits

Fetched dts88 main and queried GitHub heads, releases, PR bodies/checks and
repository metadata. Inspected named source diffs and current Companion code;
the 98-commit range was triaged, not exhaustively audited. Documentation and local
links checked. No runtime edits, app build, emulator/car validation or release
inclusion claim. Source-mirror branches remain unchanged; a fetched remote ref
is not an app integration.
