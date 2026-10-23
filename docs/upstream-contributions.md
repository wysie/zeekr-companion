# Upstream contributions

This index tracks outgoing PRs. Incoming source reviews, imports, adaptations and
exclusions are tracked in the [source integration ledger](source-integration-ledger.md).

Latest status review (**2026-10-23**, upstream `a8d9d5a5`): **11 open,
3 merged, 10 closed**. No new merged wysie PR. Open #34/#37/#44 are mergeable;
#29/#32/#47/#50/#51/#52/#53/#54 conflict with current main. All eleven submitted
heads have passing GitHub builds; that does not establish compatibility with the
new base. Newly observed closures: #30/#36/#45/#46/#48/#49. #36's core idea was
reimplemented in #67; other closures explain upstream layout, gesture, font and
theme choices. Upstream's new alpha-update switch overlaps #51, which needs a
scope review before refresh. No PR heads, descriptions or comments changed.
See [current findings, maintainer comments and recommendations](source-review-20261023.md).

## Previous status review — 2026-10-20

Status review (**2026-10-20**, upstream `eb7d5732`): **17 open, 3 merged,
4 closed**. #17/#28/#38 are merged (#28/#38 through maintainer #58/#59).
#19 remains closed in favour of adapted #41/#43. #31 is now closed in favour of
merged #62; #33/#35 were declined for storage-retention/repair-space policy reasons.
All 17 open heads now have passing GitHub builds and are mergeable against current
main. Refreshed **#46, #48, #52, #53, #54** with non-forced merge updates; other
published heads were preserved. PR descriptions include current dependency and
validation details. See [completed recommendations](recommendations-completed-20261020.md).
The [initial review](source-review-20261020.md) retains the earlier conflict snapshot.

The [earlier maintainer differences, adopted follow-ups and verification](upstream-contribution-followups-20261019.md)
are recorded separately. Historical open-status statements below describe their
original review times. The refreshed heads are listed below; unrelated heads remain unchanged.

## Five PRs refreshed — 2026-10-20

Each includes upstream `eb7d5732`; #53/#54 each include the updated #52 prerequisite
and remain independent of one another. Published history is preserved without force pushes.

| PR | Updated head | Result |
| --- | --- | --- |
| [#46](https://github.com/dts88/zeekr-shortcut-car/pull/46) | `455e9a897d72664b5f6abc1491bc81bcc13a869c` | Open, mergeable, GitHub build passing |
| [#48](https://github.com/dts88/zeekr-shortcut-car/pull/48) | `6acd823e92fbb5188344f9be7dab692025392279` | Open, mergeable, GitHub build passing |
| [#52](https://github.com/dts88/zeekr-shortcut-car/pull/52) | `4f521d02ecfa4776a6c6054974dd66e5eb395845` | Open, mergeable, GitHub build passing |
| [#53](https://github.com/dts88/zeekr-shortcut-car/pull/53) | `c1935b2c8f05bce78b90cf6baf9247b21c8667b3` | Open, mergeable, GitHub build passing |
| [#54](https://github.com/dts88/zeekr-shortcut-car/pull/54) | `77aa8a7f7a0aae19779c48bbc4b61df58cf31899` | Open, mergeable, GitHub build passing |

Each updated branch passes its JVM suite and debug/test/release assembly. Combined
validation `a910c669` passes 1,172 JVM and 19 native API32 tests, plus builds/release
lint. Shared README/resource additions still need integration resolutions when
combining independent PRs; the validation branch resolves those and deduplicates
`record_group_roads`, including its Chinese override. No Companion branding,
release configuration or signing change was added to these contribution diffs.

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

## Initial assessment after the first batch (historical)

Reviewed on 2026-10-18 against upstream main
`0697bc76b6f1c57a2e66f7b79112dca19050c477` (2.13.1-alpha), with fork main at
`3992cedc`. GitHub still reports all eleven PRs #28–#38 open; label PR #17 is
merged and side-view PR #19 is closed in favour of upstream's implementation.
The following was the initial proposal. The submitted and validated second batch is recorded below.

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

Each of those eight candidates can start from current upstream main and build without the
other new proposals or our open batch. This does not promise conflict-free merges:
several touch shared settings/resources. Extract only the relevant behavior, keep
upstream defaults, credits, application ID and update destination, and validate
each port separately before submission. Fork test results establish implementation
evidence, not that an unbuilt upstream port passes.

### Dedicated exterior recording and independent preview

This is also a proposed contribution and deserves priority in the next batch.
Comparing merged PR #25/current main with our implementation confirms that it is
additional scope: upstream enables individual composite/cabin profiles and their
lanes, but does not provide our dedicated `roadfront`/`roadrear` roles, four
recording groups, shared source picker or the independent named preview selector.
Reuse PR #25's enabled-source infrastructure instead of undoing it.

| Proposed PR | Scope | Dependency |
| --- | --- | --- |
| Exterior source roles and shared camera mapping | Add dedicated front/rear road roles, Automatic / Unassigned / explicit-ID selection and reset, duplicate-assignment checks, and inventory-based ownership. Preserve composite detection, existing cabin/manual choices, source geometry, quality profiles and historical file compatibility. A listed ID is not proof of its physical role; confirmed 009 IDs must not become generic 7X defaults. | Base on current upstream main; no dependency on the eight smaller proposals. |
| Recording source groups | Allow Surround, Front + Rear, Cabin and Manual mapping to be combined; produce separate road clips alongside Surround when selected. Preserve stored enabled choices and tuning, start new groups off, and carry the new file roles through photos, playback, sharing and protection. | Requires the exterior-source/mapping foundation. |
| Independent preview layout selector | Offer recording-source-following default, Surround, dedicated Front + Rear, Front only, Rear only, cabin and Surround + Front + Rear. Preview selection never changes which sources record or rewrites quality profiles; preserve existing saved layouts. | Requires the exterior-source/mapping foundation, but can be implemented without the recording-group UI PR. |

These are separate review scopes, not three fully independent patches: the latter
two share the first prerequisite. Fork evidence is `0e46f581`, `6fe6524d`,
`ae3d64e3` and the inventory fix `a9d2b3b9`; strip their release/model-specific
changes and adapt upstream's current pipeline instead of cherry-picking whole
commits. Tests must cover source-only previews, recording-only combinations,
duplicate IDs, migration, old/new footage, unavailable sources and factory
handover. Source exports and later 009 logs establish available feeds and observed
concurrent sessions; they do not validate every quality combination or 7X mapping.
The new startup scheduling experiments remain outside this contribution.

Continuous USB drive logging is another larger follow-up, but upstream lacks the
underlying signal scanner, so scanner integration and the drive logger should be
separate, explicitly dependent work with Ferix attribution. Our complete-source
ownership fix currently concerns extra fork roles; upstream's lookup still covers
its four defined slots. Include that correction with new exterior roles, rather
than presenting it alone as a proven bug in the existing upstream source set.

Hold the new handover/shorter failed-startup scheduling changes for on-car
confirmation, and keep DIY 360 calibration, guessed warning triggers and 009 HUD
routing outside this small contribution batch. Blind-spot side/state decoding
remains research; neither approximate owner timing nor SDK names justify an
automatic warning trigger.

## Second batch submitted — 2026-10-18

All eleven PRs below are open submissions against dts88 main
`0697bc76b6f1c57a2e66f7b79112dca19050c477`. They contain upstream-only ports,
without wysie branding, version numbers or updater destinations. Existing PR heads
#28–#38, #17 and #19 were preserved. Optional popup resizing and Alpha updates
retain upstream defaults and are presented as opt-in product choices.

| PR | Branch / submitted head | Dependency |
| --- | --- | --- |
| [#44 — Ignore replaced-session results when tracking camera liveness](https://github.com/dts88/zeekr-shortcut-car/pull/44) | `contrib/12-stale-session-progress` · `1d1643fe` | Current upstream main |
| [#45 — Keep sidebar typography and Activity font resources consistent](https://github.com/dts88/zeekr-shortcut-car/pull/45) | `contrib/13-menu-typography` · `bd274ed4` | Current upstream main |
| [#46 — Add optional double-tap actions to the floating button](https://github.com/dts88/zeekr-shortcut-car/pull/46) | `contrib/14-floating-double-tap` · `7d219ec6` | Current upstream main |
| [#47 — Make side-camera auto-fit optional while retaining safe lane clipping](https://github.com/dts88/zeekr-shortcut-car/pull/47) | `contrib/15-side-auto-fit` · `f25f3966` | Current upstream main |
| [#48 — Add Follow system, Light and Dark app theme choices](https://github.com/dts88/zeekr-shortcut-car/pull/48) | `contrib/16-app-theme` · `7ea6b296` | Current upstream main |
| [#49 — Add Close and temporary Expand/Restore controls to Super mirror](https://github.com/dts88/zeekr-shortcut-car/pull/49) | `contrib/17-mirror-controls` · `5009ccd3` | Current upstream main |
| [#50 — Offer configurable popup shapes and opt-in drag/pinch resizing](https://github.com/dts88/zeekr-shortcut-car/pull/50) | `contrib/18-popup-layout` · `a985a97a` | Current upstream main |
| [#51 — Offer Stable, Beta and Alpha update channels](https://github.com/dts88/zeekr-shortcut-car/pull/51) | `contrib/19-update-channels` · `2a83de5f` | Current upstream main |
| [#52 — Add exterior source roles and shared camera mapping](https://github.com/dts88/zeekr-shortcut-car/pull/52) | `contrib/20-camera-sources` · `e4d12add` | Current upstream main |
| [#53 — Combine recording source groups and support road-only playback](https://github.com/dts88/zeekr-shortcut-car/pull/53) | `contrib/21-recording-groups` · `6faedfa2` | #52 |
| [#54 — Select preview layouts independently of recording sources](https://github.com/dts88/zeekr-shortcut-car/pull/54) | `contrib/22-preview-layout` · `b98340df` | #52 |

#53 and #54 each include #52; they do not depend on each other. The first eight
ports and #52 build independently from upstream main. Shared files can still
require merge resolutions: README and resource additions, settings registration,
debug test-host declarations, popup/auto-fit binding and identical generic preview
plumbing. When combining #53/#54, deduplicate `record_group_roads` in default and
Chinese resources. Existing camera choices and tuning survive migration; unknown
exterior IDs stay unassigned rather than inheriting 009 assumptions on a 7X.

Each head passed its own JVM suite, debug/test/release assembly, release lint and
relevant native tests before submission. The combined
[review branch](https://github.com/wysie/zeekr-shortcut-car/tree/review/upstream-pr-round2-20261018)
at `42ac97d766756dba7f1613f8cc5a1eb3a33ed1fc` passed 1,180 JVM tests and 29
relevant API 32 native regressions in one process, plus all builds and release lint.
The unchanged template instrumentation test has an incorrect historical application-ID
expectation and was excluded. Full debug lint retains upstream errors; comparison
found zero new error fingerprints (856 baseline, 854 combined). This does not claim
a clean full-debug lint run or physical camera validation.

No incoming upstream/Ferix runtime change was imported by this outgoing batch.
USB signal logging remains a separate prospective contribution requiring scanner
prerequisites and Ferix attribution. DIY 360 fitting, 009 HUD routing and unconfirmed
blind-spot warning decoding are excluded.

## Additional opportunities reviewed — 2026-10-19

Compared fork main `40d85c74` with freshly fetched dts88 main
`020e2b0bd8899ac6ed6fa774c217b76779783342`. Upstream has merged README-only
PRs #55/#56 since the second batch, recommending CAMCORDER and updating the name
of this project to Zeekr Companion. All 22 PRs #28–#38 and #44–#54 remain open
according to GitHub. This pass changes no published PR head or runtime code.

Four additional scopes can be extracted directly from current upstream main:

| Candidate | Evidence and upstream gap | Contribution boundary |
| --- | --- | --- |
| Respect automatic-recording opt-in across waits and recovery | Fork `f78bf8ad`; upstream retains a boolean recording intent without its originating setting and lacks the fork's settings-recheck path | Port the Record on launch/manual request-origin distinction, cancellation of pending automatic starts after opt-out and persistence/recovery tests. Preserve manual recovery and upstream defaults; do not introduce the fork's driver-arrival feature as an unstated prerequisite. |
| Read-only Camera status | Fork `d1865008`; upstream exposes recorder summaries but lacks the source-status page | Add planned/writing counts and per-source producer/recovery evidence with Diagnostics access. Adapt the existing upstream screen/settings rather than importing our camera-first home. Preview-cover enhancements can follow #36 separately. No new camera opens or retry-policy changes. |
| Accessible segmented settings choices | Fork `ba7f0349`; upstream's SegmentedBar exposes activated styling without checked radio semantics | Add checkable/checked RadioButton accessibility state and matching native tests. Validate selected-state contrast; any outline/tint adaptation should be scoped, without the full factory-style redesign. |
| Scoped confirmation for layout resets | Fork `db79d7b2`; upstream mirror/floating resets execute immediately (profile reset already confirms) | Confirm the exact affected settings; Cancel must make no changes. Cover Super mirror and floating button only where upstream has them. Do not require our HUD/360 settings, menu rearrangement or branding. |

These are candidate scopes, not validated upstream ports or new submissions.
They can build independently of each other after adaptation; shared files may
still cause merge conflicts. Fork tests do not prove that an unbuilt port passes.

Follow-ups should stay with existing review scopes:

- **#36, stalled-preview covers:** add wrapping, bounded waiting/accessibility
  feedback and narrow-pane pixel coverage when porting the richer cover. The
  published head uses centred single-line drawText, not our later StaticLayout;
  it does not contain the same double-centering defect. Keep a left-origin Paint
  if introducing centred StaticLayout, rather than claiming an existing duplicate bug.
- **#29, responsive settings:** long title/value readability and larger-text
  regressions fit this existing layout scope; avoid a duplicate clipping PR.
- **#45, menu typography:** preserve its resource-isolation/recycled-row fix.
  Removing forced 1.2× sizing or adopting denser factory-style defaults is a
  maintainer design decision, not an automatic amendment to that bug fix.

Do not offer the recent fork recording/preview refusal as an upstream bug fix:
upstream and published #54 lack our broad pending-channel start gate. A recording-
specific compatibility check would need to be justified/tested in the preview
port's own lifecycle, rather than cherry-picked unchanged from `585e0c36`.

Companion branding/storage rename, the new home-screen Gallery shortcut and DIY
360 optimization are fork-specific. Current upstream has separate Videos/Photos
actions and no equivalent DIY 360 renderer, so these are not independent drop-in
fixes. Signal scanning/USB drive logging remains the larger attributed follow-up
already noted above; HUD mapping and warning decoding still require vehicle evidence.
