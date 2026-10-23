# Source and outgoing PR review — 2026-10-23

Companion base: `29a280d56d05a935f77ec8643000215eff6bec9d`.
Fetched main refs and read current GitHub PR states, head checks and maintainer
comments. This is an incoming-change triage and selected code comparison, not an
exhaustive audit or validation of the new source trees. No runtime imports,
source-mirror fast-forwards, PR edits/comments or release in this review.

| Source | Previous review → current main | Scope |
| --- | --- | --- |
| dts88 | `eb7d5732cf8d20f75439d78da5a76f4254f684fc` → `a8d9d5a56f2984f531ae3d4c0e5af69cf29718c0` | 89 commits including merges; current release 2.23.0-alpha |
| Ferix | `130e7f9a1f5c2117fc9f5f03e019407b0d68281c` → `2dce2510c373d43cd850d8dc040d701311b63bb6` | Two commits: camera handover/side-yield correction and 2.14.3 release |

## Incoming decisions

| Source | Decision and Companion comparison |
| --- | --- |
| [#65 channel fixes](https://github.com/dts88/zeekr-shortcut-car/pull/65), `fc04167c276e5eedbc059ded62aed405f3840d6c` | **Recommend selective adaptation**, not yet imported. `sessionCarries` currently checks only the session snapshot in Companion; source adds `stillCarries` covering the carried map until device close. Source also forwards MIRROR output drops and stops re-declaring a configuration-refused mirror until an explicit recovery event. Companion still declares its mirror without a drop listener and dispatches only RECORD/SIDE variants. These are concrete missing lifecycle protections. Source enabled-lane/Holder changes need separate adaptation to our independent preview, recording groups, mapping and Apply semantics; do not copy the whole patch. |
| [#66 lane-specific retry unlock](https://github.com/dts88/zeekr-shortcut-car/pull/66), `be614c008c39d2fb780ceda79c6cb590d0a0354f` | **Recommend adaptation**, not yet imported. Companion `liftStops` still unlocks all wanted stopped lanes on the global needs change. Source records changed holders and unlocks only lanes those holders want, including refused recording outputs. Adapt for our extra roles/holders and retain screen-on/service-free recovery. |
| [#67 stalled preview](https://github.com/dts88/zeekr-shortcut-car/pull/67), `91c8e69e` / `cb60b734` | Our #36 was closed with the core idea reimplemented. Companion already has stalled-preview protection, so no wholesale replacement. **Defer focused comparison** of producer-timestamp freshness and hidden-pane handling across native/GPU paths. This review does not establish a new Companion defect in those paths. |
| [#63 rear-cabin mirror](https://github.com/dts88/zeekr-shortcut-car/pull/63), merge `a139b075`, follow-ups `db366262` / `5a5f7656` / `2d2d682d` | **Still deferred** pending a confirmed cabin source and integration design. PR is now merged upstream, superseding its prior open status. Read selected lifecycle follow-ups; full rear-cabin service/view rewrite is not audited or ported. Our Super mirror still uses the surround stream. |
| [#64 settings backup](https://github.com/dts88/zeekr-shortcut-car/pull/64), merge `109469dc` | **Already adapted** via `b94a3cb2`, with five-store Companion restore/rollback behavior and later audit fixes. Source PR now merged. The original reviewed implementation remains `35bdfc197c208a983eb4fb5dc1c8ede4813db265`; do not replace the richer fork implementation. |
| [#73–#76 recording audio](https://github.com/dts88/zeekr-shortcut-car/pull/76), implementations `95119527`, `943d3c83`, `5315dedc`, `269b0e04`; [#89 playback focus](https://github.com/dts88/zeekr-shortcut-car/pull/89), `489bdf9a` | **Defer as a complete optional feature**. Cabin-microphone capture/mux, ring/repair changes, permission/service behavior and audio-focus playback are new upstream scope, not a small recorder fix. Read release/change inventory; codec/audio implementation not exhaustively audited or built. Companion microphone-permission removal was correct for its current silent recordings. |
| [#72 drive mode](https://github.com/dts88/zeekr-shortcut-car/pull/72), `cd920c96`; [#77 gear colors](https://github.com/dts88/zeekr-shortcut-car/pull/77), `c8889d61` / `8d587826` | **Deferred enhancement**. Source adds drive-mode telemetry/display. Requires head-unit signal verification; no claimed 009 compatibility or import. |
| [#79/#80 simulated drive sound](https://github.com/dts88/zeekr-shortcut-car/pull/79), `a74a2df3` / `a5f7bc9e` / `309641ec` / `8f1d50e7` | **Deferred optional feature**. Not needed for observed recording/preview faults; substantial new sound/telemetry code is unassessed beyond scope inventory. |
| [#83 HUD view](https://github.com/dts88/zeekr-shortcut-car/pull/83), `3bcf29bc`; credits #71/#85/#86 | **Fork equivalent; no wholesale import**. Companion already has HUD output, owner-confirmed 009 routing, safe-area calibration and GPU support. Source HUD implementation is not exhaustively compared here; no claim of full parity. Retain existing contributor attribution. |
| [#87/#90 cabin one-shot policy](https://github.com/dts88/zeekr-shortcut-car/pull/90), final `e2116d69` | **Defer**. Latest source policy affects front cabin only: disable its next-start selection and retire it after leaving the car. Earlier policy also limited rear cabin and was superseded. Read final `FrontCabinOneShot`; do not silently disable Companion camera choices based on another head unit's issue. |
| [#69/#88 typography](https://github.com/dts88/zeekr-shortcut-car/pull/88), `b9fc3929` then `ceadb300` | **Skip blanket size import**. Upstream removed inherited 1.2× scaling, then increased declared sizes to 16/20/22/24/32sp. Our owner requested denser factory-like typography and we already isolate Activity resources. #45 closure explains earlier source policy, not a reason to undo Companion fixes. |
| [#91–#95 settings](https://github.com/dts88/zeekr-shortcut-car/pull/95), `e5bdbde6`, `7531ede5`, `87b7f352`, `06cdf9e7`, `81036d0c`, `5bcc19b2` | **Deferred UX reference**: combined switch/navigation rows, grouped slider dialogs, mirror window editor and fewer headings. Retain our native-style categories and additional controls. UI implementation not fully audited or ported. `81036d0c` also adds opt-in alpha updates with beta gating: Companion already supports Stable/Beta/Alpha. Our still-open #51 now overlaps and needs rescoping before refresh. |
| Ferix [camera handover fix](https://github.com/FerixzTanz/CAMCORDER/commit/ebc9d0baa6a76186f0bdebf28aac64ed5604b46d) | **Skip direct import; different scheduler**. Patch trusts a new “free” report for two seconds, then steady frames; caps side-view waiting for recording startup at ten seconds. Companion popup/HUD bind paths have no `isAboutToStart` gate, and its ChannelRules do not indefinitely block a streaming device on a free report. No corresponding new failure reproduced here. Keep the scenario as a regression reference; do not add an unnecessary wait. |

Release metadata, obsolete icon deletion and owner-copy approval records are not
application integrations. Signing-rotation PR #60 remains open at
`924c9d54eff045dd16365d01e08252152faedf6e`, unchanged and deferred. Ferix's only
open PR is the old #13 release proposal; it is not a new post-review feature.

## All 24 outgoing PRs

GitHub snapshot: **11 open, 3 merged, 10 closed**. All eleven open heads have
passing `build` checks, but these validate their submitted heads, not compatibility
with today's upstream base. Mergeability was refreshed after GitHub initially
returned UNKNOWN.

| Current status | PRs |
| --- | --- |
| Merged, unchanged | #17, #28, #38 |
| Open, mergeable/CLEAN | #34, #37, #44 |
| Open, conflicting/DIRTY | #29, #32, #47, #50, #51, #52, #53, #54 |
| Previously closed | #19, #31, #33, #35 |
| Newly observed closed | #30, #36, #45, #46, #48, #49 |

Maintainer explanations on newly closed submissions:

- [#30](https://github.com/dts88/zeekr-shortcut-car/pull/30#issuecomment-6282702275): playback fits the 7X width; wrapping changes the desired appearance; a separate redesign is planned.
- [#36](https://github.com/dts88/zeekr-shortcut-car/pull/36#issuecomment-6282834325): adopts the core main-preview stall idea in #67, using the existing 2.5-second/80% scrim rule, not our full patch.
- [#45](https://github.com/dts88/zeekr-shortcut-car/pull/45#issuecomment-6285721125): chose to remove global font enlargement (#69); does not consider recycled-row changes necessary. Subsequent #88 changes declared sizes separately.
- [#46](https://github.com/dts88/zeekr-shortcut-car/pull/46#issuecomment-6284628533): declines double tap because delayed single-tap arbitration is undesirable while driving.
- [#48](https://github.com/dts88/zeekr-shortcut-car/pull/48#issuecomment-6286020981): declines app theme selection in favor of the car's system theme setting.
- [#49](https://github.com/dts88/zeekr-shortcut-car/pull/49#issuecomment-6287935148): declines permanent overlay buttons; prefers existing edge-tuck gestures.

These are product-policy decisions/adaptations, not failed head builds. No new
merged wysie PR since the last review. Preserve existing heads and #53/#54's #52
dependency. Recommend refresh of useful bug fixes first, scope review for #51,
then the larger source/mapping/recording/preview stack. Latest upstream still
updates liveness from replaced-session callbacks outside its session-identity
check, so #44 remains a concrete candidate.

Validation: GitHub API/remote-head verification, selected source diffs and current
fork code comparison; documentation diff check. No app changes, build, emulator,
new vehicle test or release-inclusion claim.
