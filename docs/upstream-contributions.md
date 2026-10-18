# Upstream contributions

This index tracks outgoing PRs. Incoming source reviews, imports, adaptations and
exclusions are tracked in the [source integration ledger](source-integration-ledger.md).

Submitted on 2026-10-16 against dts88 main at `0f145f7a` (2.12.0-alpha).

Each branch starts directly from that upstream commit and builds independently.
None requires our combined fork, personal version numbers or updater configuration.
The existing label and blind spot PR heads were left unchanged.

| Batch item | Upstream PR | Branch |
| --- | --- | --- |
| 1 | [Remove unused microphone permission checks](https://github.com/dts88/zeekr-shortcut-car/pull/28) | `contrib/01-unused-microphone` |
| 2 | [Keep settings controls visible in narrow layouts](https://github.com/dts88/zeekr-shortcut-car/pull/29) | `contrib/02-settings-layout` |
| 3 | [Keep playback action labels aligned and readable](https://github.com/dts88/zeekr-shortcut-car/pull/30) | `contrib/03-playback-actions` |
| 4 | [Group existing settings by task and add a stamps subpage](https://github.com/dts88/zeekr-shortcut-car/pull/31) | `contrib/04-settings-organisation` |
| 5 | [Expose developer mode as a warned System toggle](https://github.com/dts88/zeekr-shortcut-car/pull/32) | `contrib/05-developer-toggle` |
| 6 | [Preserve footage across relay and archive transfer failures](https://github.com/dts88/zeekr-shortcut-car/pull/33) | `contrib/06-footage-transfer-integrity` |
| 7 | [Retain existing footage locks when list replacement fails](https://github.com/dts88/zeekr-shortcut-car/pull/34) | `contrib/07-protection-list-integrity` |
| 8 | [Preserve verified originals before bounded MP4 repair](https://github.com/dts88/zeekr-shortcut-car/pull/35) | `contrib/08-repair-original-backup` |
| 9 | [Cover stalled main-camera and Super mirror previews](https://github.com/dts88/zeekr-shortcut-car/pull/36) | `contrib/09-stalled-preview-covers` |
| 10 | [Yield automatic UI recovery to reverse and factory cameras](https://github.com/dts88/zeekr-shortcut-car/pull/37) | `contrib/10-factory-camera-recovery` |
| 11 | [Verify updater APK hashes and restrict redirects to HTTPS](https://github.com/dts88/zeekr-shortcut-car/pull/38) | `contrib/11-update-integrity` |

The full batch also merged cleanly on the local `review/upstream-pr-batch-20261016`
validation branch. Its debug/release builds, 1,113 JVM tests and eight native Android
regressions passed. Individual branches were tested before submission. Native tests
cover layout, navigation, developer-mode state and opaque stalled-preview covers;
they do not establish vehicle compatibility.

Full debug lint has existing upstream errors: the unchanged baseline reports 845;
the combined batch reports 844, with no new errors. Release builds and release lint
pass. The transfer and repair branches include an identical verified-copy helper,
so either can merge first.

PR #31 reorganizes only existing upstream settings. It does not include the fork’s
blind spot or 360 sections. The source-tracking branches remain unmodified; update
them by fast-forward and integrate selected changes separately into the fork.

## Candidates after the first batch

Reviewed on 2026-10-18 against upstream main
`0697bc76b6f1c57a2e66f7b79112dca19050c477` (2.13.1-alpha), with fork main at
`3992cedc`. GitHub still reports all eleven PRs #28–#38 open; label PR #17 is
merged and side-view PR #19 is closed in favour of upstream's implementation.
These are proposals, not newly submitted PRs or validated upstream port branches.

| Candidate | Fork evidence | Upstream gap and proposed independent scope |
| --- | --- | --- |
| Ignore stale capture-session completions | `5d726f4f`, SingleCamera portion only | Upstream guards counts but still advances liveness, last-capture and FPS from a replaced session's results. Port the early session-identity return and native regression alone, without startup scheduling changes. Smallest first contribution. |
| Stable menu typography and Activity-local font resources | `e5f1a1d9`, `9d29046a` | Upstream header binding does not restore text size/autosize; MainActivity still mutates resources after AppCompat initialization. Retain upstream sizing and sidebar design, isolate the existing 1.2× scale before inflation, restore recycled row typography, and port native tests. The intermittent owner font state itself remains unconfirmed. |
| Optional floating-button double tap | `4285a37f` | Upstream has tap and hold only. Keep double tap off by default, immediate single tap when off, cancellation on movement/removal and preserved saved actions. Extract gesture logic/settings/tests without fork actions or branding. |
| Optional side-view auto-fit | `e6d98297` | Upstream always narrows the requested field of view to fit. Keep that default; offer an explicit unfitted mode with opaque black missing areas and selected-lane clipping, retaining upstream's angles and fixed placement. No dependency on popup resizing or HUD. |
| App theme selection | `450e969a` | Add Follow system / Light / Dark with native confirmation and persistence. Adapt existing upstream popup/mirror controls rather than importing fork window services, HUD or 360. Test theme recreation and text-size parity. |
| Close and Expand/Restore controls for Super mirror | `b089c3a9` | Upstream already supports mirror gestures, but lacks these explicit corner controls. Preserve its touch model and restore the user's normal placement after temporary expansion. Port minimal controls without introducing fork side-view/HUD dependencies. |
| Configurable side-popup shape and resizing | `c006213b` plus existing fork gesture/geometry helpers | Upstream deliberately uses fixed, non-draggable 2.2:1 placement (PR #41/#43). This is a product-policy discussion first, not a bug fix. An opt-in port should preserve the current default and adapt upstream SideViewWindow rather than replacing its lifecycle with ours. |
| Stable / Beta / Alpha update channels | `b329e35e` | Upstream explicitly excludes alpha releases by policy. Discuss an explicit opt-in Alpha channel before submission; preserve its repository, release naming and legacy beta preference. Keep integrity PR #38 separate, though edits to shared updater files may conflict. |

Each accepted candidate can start from current upstream main and build without the
other new proposals or our open batch. This does not promise conflict-free merges:
several touch shared settings/resources. Extract only the relevant behavior, keep
upstream defaults, credits, application ID and update destination, and validate
each port separately before submission. Fork test results establish implementation
evidence, not that an unbuilt upstream port passes.

Larger follow-ups: continuous USB drive logging is useful but upstream lacks the
underlying signal scanner, so scanner integration and the drive logger should be
separate, explicitly dependent work with Ferix attribution. Vehicle profiles,
shared camera mappings and recording/preview groups need comparison with merged
PR #25 before proposing another camera-configuration model. Our complete-source
ownership fix concerns extra fork roles; upstream's lookup still covers its four
defined slots, so do not present that fork correction as a proven upstream bug.

Hold the new handover/shorter failed-startup scheduling changes for on-car
confirmation, and keep DIY 360 calibration, guessed warning triggers and 009 HUD
routing outside this small contribution batch. Blind-spot side/state decoding
remains research; neither approximate owner timing nor SDK names justify an
automatic warning trigger.
