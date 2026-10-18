# Source integration ledger

This is the maintained record of incoming dts88 and Ferix work: what wysie Edition
uses, what was changed during the port, and what remains outside the fork.
[NOTICE](../NOTICE.md) retains attribution and original source references;
[outgoing contributions](upstream-contributions.md) records our PRs to dts88.
Neither a source-tracking branch nor an upstream PR's “Merged” badge means that
the change is included in our APK.

Backfilled on **2026-10-18** from Git history, existing port notes and the current
implementation. This is a feature-level record, not a claim that every historical
source commit has been audited individually. An unlisted change is **unassessed**,
not implicitly adopted or rejected. Where no earlier decision was recorded, say
so rather than inventing a reason.

## Status meanings

| Status | Meaning |
| --- | --- |
| Merged | Source history is integrated into `main`; subsequent fork edits may differ. |
| Adapted | The feature was ported with deliberate fork changes; source ancestry alone will not identify it. |
| Partial | Named portions were adopted; the remaining scope is explicitly outside the port. |
| Deferred | Not imported; a future review may adopt it. This is not a permanent rejection. |
| Skipped | Deliberately excluded for the stated reason; revisit if that reason changes. |
| Retired | Previously imported behavior was subsequently removed or replaced. |
| Replaced | An earlier approach or exclusion was superseded by the explicitly linked follow-up. |

Local commit links identify implementation/integration evidence. **Included by**
means a verified release tag contains that local commit, not necessarily its first
release. “Not imported” does not mean the fork lacks similar independently
implemented behavior.

## Reviewed source snapshot

Last checked **2026-10-18**. The source heads were unchanged from the previous
review. These are reviewed boundaries, not a statement of full feature parity.

| Repository | Reviewed main head | Relationship to this fork |
| --- | --- | --- |
| [dts88/zeekr-shortcut-car](https://github.com/dts88/zeekr-shortcut-car) | [0697bc76](https://github.com/dts88/zeekr-shortcut-car/commit/0697bc76b6f1c57a2e66f7b79112dca19050c477), 2.13.1-alpha | Full upstream ancestry through `76138b8d`, plus selective PR #41 adaptations below. `upstream-main` currently points to `0f145f7a`; it is a source mirror, not the combined fork's integration boundary. |
| [FerixzTanz/CAMCORDER](https://github.com/FerixzTanz/CAMCORDER) | [3f9c7e40](https://github.com/FerixzTanz/CAMCORDER/commit/3f9c7e4033627aad53cd3f614c5643de7ac78040), following 2.14.1 | `ferix-main` matches this head. Selected features are ported below; the complete CAMCORDER tree is not merged. |

Current release evidence: [v2.11.46-alpha.1](https://github.com/wysie/zeekr-shortcut-car/releases/tag/v2.11.46-alpha.1)
at [588d334b](https://github.com/wysie/zeekr-shortcut-car/commit/588d334be95e59659329ee7de5cede20e11ddd22).
All local integration commits linked below are included by that tag. Deferred,
skipped and retired source behavior must not be described as active merely
because its source commits are reachable or mentioned in documentation.

## Incoming dts88 changes

| Source | Current status | What we use / differences and decision | Local evidence |
| --- | --- | --- | --- |
| Upstream baseline through [76138b8d](https://github.com/dts88/zeekr-shortcut-car/commit/76138b8dc687d2e0c9d397b1c678e2ba8c7e85be) | Merged | Includes the current upstream recording/channel framework, camera scheduling, physical screen-state rules, closure/diagnostic follow-ups and configurable Super mirror labels (PR #17). Fork outputs and configuration are retained during integration. This boundary covers earlier upstream history without duplicating every commit here. | [14a92273](https://github.com/wysie/zeekr-shortcut-car/commit/14a92273fe05ec0dd0a04f5f003254e17c052b80), [8209f91c](https://github.com/wysie/zeekr-shortcut-car/commit/8209f91c4fd2f30501ed064cd9b12d151ca3835d), [fb2e60fe](https://github.com/wysie/zeekr-shortcut-car/commit/fb2e60fe1a2c06a9cff26b8355ef2cf8f7ef6094) |
| [PR #41: centre-screen blind spot view](https://github.com/dts88/zeekr-shortcut-car/pull/41), merge `95f5b989`, implementation `bf718089` | Partial / adapted | Adopted retained-texture reuse, immediate safe removal, reconnection checks and refusal episodes. Kept our resizing/aspect ratios, dismissal, mirror relationship, HUD/360 outputs, freshness checks and framing defaults. Did not replace the service/output architecture or import fixed placement/invisible-canvas behavior. | [51ccf4c0](https://github.com/wysie/zeekr-shortcut-car/commit/51ccf4c00d9b8a99c2500e1d2c5c49a292b24aa9), integration [2f480a25](https://github.com/wysie/zeekr-shortcut-car/commit/2f480a25d79f0a443a4ece917b3ec0ea0d983f2d); [scope and tests](upstream-side-view-lifecycle.md) |
| [PR #43: wider popup and tuned angles](https://github.com/dts88/zeekr-shortcut-car/pull/43), implementation `d73ce518` | Deferred | Not adopted as defaults. Source uses 2.2:1, one-third screen height, back 33°, outward 10°, FOV 90°, based on 7X feedback. Fork retains saved shapes/size and back 50° / up 15° / FOV 90° defaults. A separate 7X preset is a candidate, not implemented or validated on our 009. | Reviewed against `0697bc76`; no import commit |
| [PR #25: enabled lanes instead of lane count](https://github.com/dts88/zeekr-shortcut-car/pull/25), merge `5821708e` | Deferred | Now merged upstream; older notes calling it open describe the earlier review. Its source patch is not imported. Fork camera selection subsequently gained independent recording groups, preview choices and shared source mappings; importing the source plan/scheduler/UI changes needs comparison with those additions. No blanket rejection was recorded. | Fork alternatives: [7fd6351b](https://github.com/wysie/zeekr-shortcut-car/commit/7fd6351b30742a4f0272911e66c1a830cbdc0303), [885e3f2e](https://github.com/wysie/zeekr-shortcut-car/commit/885e3f2efd85dbfd53c1fbae2c960cfb87e46bc7), [588d334b](https://github.com/wysie/zeekr-shortcut-car/commit/588d334be95e59659329ee7de5cede20e11ddd22); these are not PR #25 imports |
| [PR #27: contributor credits](https://github.com/dts88/zeekr-shortcut-car/pull/27), merge `0516fa4d` | Partial / fork equivalent | The upstream patch is not merged. Our About page separately credits dts88, Ferix and EVCam in wysie's voice. Upstream's recommendation of this fork is not copied into our own introduction. | [d83a6bc2](https://github.com/wysie/zeekr-shortcut-car/commit/d83a6bc2f36fe7820d0e122adb4541c1d710a9c8); [credit wording](../README.md) |
| [PR #39: Chinese Super mirror rename](https://github.com/dts88/zeekr-shortcut-car/pull/39), implementation `907c1b26` | Deferred | Not imported: current Chinese resources still use 超级后视镜. No earlier reason to omit it was recorded. Candidate cosmetic sync must include the translated guide artwork, not just a string replacement. | Current `app/src/main/res/values/strings.xml`; no import commit |

Upstream release/version commits after the integrated baseline are not copied as
fork release metadata. wysie Edition has its own version, updater repository,
signing-compatible APKs and release process.

## Incoming Ferix changes

Original authors and complete source hashes for the initial seven ports are in
[NOTICE](../NOTICE.md); detailed adaptations and historical validation are in
[Ferix port notes](ferix-feature-ports.md). These rows describe current disposition,
including follow-ups that changed the initial decision.

| Source feature / boundary | Current status | Fork modifications / excluded scope | Local integration evidence |
| --- | --- | --- | --- |
| Phone sharing: `1539ead4`, `f00788a6`, `78461a20` / [PR #12](https://github.com/FerixzTanz/CAMCORDER/pull/12) | Adapted | Reachable addresses and HEAD/compression/status fixes; retain token redaction and connection diagnostics. Android timeout reports are not proven resolved on every phone. | [e1d74af9](https://github.com/wysie/zeekr-shortcut-car/commit/e1d74af91ff666b9db9810feade285dd68737c28) |
| Signal scanner: `85bc7a34`, `7bdde73d`, `ade63cf5`; callbacks [945ee4ed](https://github.com/FerixzTanz/CAMCORDER/commit/945ee4ed160bb8d91ca5e54411bab2ffa68cf3d3); restart [f56bd12c](https://github.com/FerixzTanz/CAMCORDER/commit/f56bd12c6f2267de8a9ef5b96399a23101a56840) | Adapted | Reconnectable vehicle interface, known-sensor filtering, bounded callback queue/shapes and Developer shutdown. Restart appends by explicit session identity with the original deadline/event budget; source's 30-second session heuristic is not used. Does not establish a blind-spot-warning trigger. | [44e32c2a](https://github.com/wysie/zeekr-shortcut-car/commit/44e32c2a9046f233cc36253426edf0b7cb180fd7), [b899f511](https://github.com/wysie/zeekr-shortcut-car/commit/b899f5110e643f1b85e6c9b1ceeda9147fe9e1d7), [fb2e60fe](https://github.com/wysie/zeekr-shortcut-car/commit/fb2e60fe1a2c06a9cff26b8355ef2cf8f7ef6094) |
| Hard-braking locks: `75d8b854`, `a2c7143c`, `978ba15c` | Adapted | Keep current second-pass timing, event protection and a bounded automatic-lock budget/list; preserve manual locks. | [484062a9](https://github.com/wysie/zeekr-shortcut-car/commit/484062a959c15c9b216fce6380eaacb117aa8355) |
| Range clip export: `c611deb1`, `d7dc998b` | Adapted | Range remux and camera swipes integrated with existing drive groups; reserve partial outputs and never replace an existing/in-flight export. | [6c7ec46b](https://github.com/wysie/zeekr-shortcut-car/commit/6c7ec46b81e5178cc3d88cca55cd158ad8000d78), follow-up [5fb80823](https://github.com/wysie/zeekr-shortcut-car/commit/5fb808237b2579bdbc7ddb4d010c3f1b61b24054) |
| Driver sessions: `289d66c3`, `657dcbdd`, `f41028c8` | Adapted | Use current recording coordinator, physical screen-state and wake guards; durable resume-intent clearing. Driver-start defaults off. Older coordinator and surround-start delays excluded. | [8f87faca](https://github.com/wysie/zeekr-shortcut-car/commit/8f87faca1627b2df31d51bd4c4618f080eea3140) |
| Original turn-signal view [dts88 PR #4](https://github.com/dts88/zeekr-shortcut-car/pull/4); arrows `45e07ae7`, `2415bac9` | Adapted | Current scheduler owns camera actions; arrows support our popup and mirror replacement. Later fork additions include hiding the mirror, shapes, labels, freshness and auto-fit controls. | [872647ca](https://github.com/wysie/zeekr-shortcut-car/commit/872647ca7613cb6898d693f23fc3ca392951c066), [323a21a3](https://github.com/wysie/zeekr-shortcut-car/commit/323a21a35fb41095ed75df97474e3663d5561cfb) |
| Night palette / floating button: `c91739e1`, `beacea7f`, `f931d11c`, `19986ac5` | Adapted | Semantic night colors and icon dimensions; retain current waiting/live recording states. Later side-popup controls follow the system theme. | [6cc94d50](https://github.com/wysie/zeekr-shortcut-car/commit/6cc94d50356b5b330fa26a0f04ce0122d9da464c), [448b1a32](https://github.com/wysie/zeekr-shortcut-car/commit/448b1a32ab19f2b750d450242ba8a196c620d155) |
| [PR #14 HUD/screen/both](https://github.com/FerixzTanz/CAMCORDER/pull/14), `becbfe10`, `426c5fa3` | Adapted | Separate scheduler-owned HUD output; preserve screen fallback, mirroring, labels, framing and fresh-frame checks. No old direct camera opening/rebuild path. Later 009 display matching is our follow-up below. | [6c4d7429](https://github.com/wysie/zeekr-shortcut-car/commit/6c4d7429a7d057e534fa58db133a9c6f5e6b4c2a) |
| [PR #15 HUD picture controls](https://github.com/FerixzTanz/CAMCORDER/pull/15), follow-ups through `d6193888`; fades `3176bae5`; night contrast [f3ade087](https://github.com/FerixzTanz/CAMCORDER/commit/f3ade087e9814984d4b0043c0d159c6e6e8bc380) | Adapted | Independent HUD contrast/night/edge/position controls with neutral defaults; fades retain startup grace, cancellation, factory handover and fresh-side checks. Higher-layer experiment is retired below. | [b899f511](https://github.com/wysie/zeekr-shortcut-car/commit/b899f5110e643f1b85e6c9b1ceeda9147fe9e1d7), [fb2e60fe](https://github.com/wysie/zeekr-shortcut-car/commit/fb2e60fe1a2c06a9cff26b8355ef2cf8f7ef6094) |
| Shared pipeline lookup [57797edd](https://github.com/FerixzTanz/CAMCORDER/commit/57797edd4b52c1aa2ee554d5eea6adf589e066e3) | Adapted | Keep foreground/relay services across rapid Activity recreation. Existing upstream framework continues to own cameras outside the Activity. | [95a28401](https://github.com/wysie/zeekr-shortcut-car/commit/95a28401cf6b1b1540b0b121e292c0ac3e637a94), [b899f511](https://github.com/wysie/zeekr-shortcut-car/commit/b899f5110e643f1b85e6c9b1ceeda9147fe9e1d7) |
| Gallery / filmstrip / adjustment UI, [285f2b79](https://github.com/FerixzTanz/CAMCORDER/commit/285f2b790280737844d2ba97e028aef1403ba0e3), reviewed through `3f9c7e40` | Partial / adapted | Date groups, thumbnails, selection/sharing, filmstrips and Activity-hosted picture controls. Preserve legacy folders, path-qualified identities, guarded deletion/protection and current timeline ownership. Did not replace our entire main screen/menu. | [7e1449f5](https://github.com/wysie/zeekr-shortcut-car/commit/7e1449f55d80b0fe840c6c3cf092dfcafee24b47), integration [fb2e60fe](https://github.com/wysie/zeekr-shortcut-car/commit/fb2e60fe1a2c06a9cff26b8355ef2cf8f7ef6094) |

## Deliberate exclusions and changed decisions

| Source / behavior | Current status | Decision and history | Evidence |
| --- | --- | --- | --- |
| Ferix owner-specific defaults [e992d000](https://github.com/FerixzTanz/CAMCORDER/commit/e992d000bf4a4ed6ad1e3bd8f7024c6751aa946e) | Skipped | No blanket import of boot/recording, angles, geometry, HUD destination or mirroring preferences. Preserve explicit user choices and this fork's defaults. | [Port decisions](ferix-feature-ports.md) |
| Factory-view HUD bypass [830f2d0d](https://github.com/FerixzTanz/CAMCORDER/commit/830f2d0dd1e2b1c1b9a57b70dcd86e00fb4cd8e3) | Partial / adapted | Initially excluded; subsequently added as **off-by-default HUD coexistence**. It changes HUD visibility only. Reverse suppression, screen-popup yielding and physical camera arbitration remain. This does not force access to a camera held by the factory. | [820f2367](https://github.com/wysie/zeekr-shortcut-car/commit/820f2367d3fffef6311c2872650648d1a869bf08) |
| Higher HUD window layer, removal following [92c61185](https://github.com/FerixzTanz/CAMCORDER/commit/92c611852dd8cad2ac0607b32da4869def6e5152) | Retired | Imported as an experiment, then removed after Ferix's unsuccessful 7X test. Ordinary Presentation output remains; old overlay preferences are ignored. Factory HUD graphics can still appear over our image. | Import [51cc990d](https://github.com/wysie/zeekr-shortcut-car/commit/51cc990deaf731430f6e35b9e70409d9f9b45446); removal [98bb7365](https://github.com/wysie/zeekr-shortcut-car/commit/98bb7365b682814f4a277ffd14d696e8861d5ef7) |
| Anonymous display-3 fallback from early HUD work | Replaced | Initially excluded while the 009 destination was unknown. Owner later confirmed identification and live HUD video. Fork now automatically matches public, powered-on display 3 at 1280×640 on that head unit, with independent right-side fitting. This does not validate every 009 firmware or remove factory graphics. | [79b01323](https://github.com/wysie/zeekr-shortcut-car/commit/79b01323db46b13bc5fbf80c1614625ca2637c55); [009 HUD evidence](009-hud-live-video.md) |
| Older Ferix camera lifecycle / coordinator | Skipped | Keep the newer dts88 scheduler/coordinator plus scoped adaptations. Do not import obsolete lifecycle code merely to match the source tree. | [Port decisions](ferix-feature-ports.md), [PR #41 scope](upstream-side-view-lifecycle.md) |
| Complete CAMCORDER Tools-only navigation, README and release identity | Skipped | Keep wysie Edition's task-based settings, action rail, credits, updater and release scheme. Selected gallery/tuning work is imported separately. | [fb2e60fe](https://github.com/wysie/zeekr-shortcut-car/commit/fb2e60fe1a2c06a9cff26b8355ef2cf8f7ef6094), [fork workflow](../FORK_WORKFLOW.md) |

## Fork follow-ups are separate work

USB drive logging, DIY 360 projection/calibration and vehicle artwork, confirmed
009 HUD matching/fitting, popup aspect ratios/auto-fit, update channels, shared
source mappings, recording groups and preview selection have their own fork
commits and documentation. They are not evidence that a similarly named source PR
was imported. The [README](../README.md) describes current settings and limits.
Neither the source ports nor our camera probes establish a factory stitched-360
stream, calibrated 7X projection or an automatic blind-spot-warning trigger.

Validation belongs to the specific change. Port notes retain earlier checks;
release 2.11.46-alpha.1 passed 1,459 JVM tests, 23 distinct native Android
regressions, lint, packaging checks and debug/release builds. Emulator checks do
not establish vehicle performance. Owner-confirmed 009 HUD output supersedes
older notes saying its physical destination was unconfirmed; factory-layer overlap
remains. See [009 test notes](009-test-notes.md).

## Review journal and update rule

| Review date | Source boundaries | Outcome |
| --- | --- | --- |
| 2026-10-18 | dts88 `0697bc76`; Ferix `3f9c7e40` | No new main heads since the preceding review. Backfilled this ledger; PR #41 remains partial/adapted, #43 and #25 remain outside the fork, #39 is an unimported cosmetic candidate. No runtime import or new APK in this documentation change. |
| 2026-10-18, outgoing contribution review | dts88 `0697bc76b6f1c57a2e66f7b79112dca19050c477`; Ferix `3f9c7e4033627aad53cd3f614c5643de7ac78040` | GitHub main heads remain unchanged. Compared post-batch fork work through `3992cedc` with current upstream and PR status: #28–#38 are still open. Recorded eight scoped candidates and larger/deferred work in the outgoing index. Upstream's fixed popup and exclusion of alpha updates are deliberate policies, requiring discussion before those feature PRs. No source import, tracker movement, outgoing PR creation, runtime change or APK. Existing release-48 tests do not validate unbuilt upstream ports. |

For **every external update review**, even when nothing is imported:

1. Record the date and exact source head(s). List the reviewed range/PRs; identify
   anything still unassessed. Inspect merged source work separately from open PRs.
2. Add/update each decision row: source PR/commits, status, adopted scope,
   exclusions, reason and remaining work. Record “reason not previously recorded”
   for an uncertain historical omission.
3. For imports, link the local adaptation and integration commits, relevant tests
   and known vehicle limits. Preserve attribution in NOTICE and update README
   when behavior changes. For removal/replacement, retain the original decision
   and add its successor; do not erase the fact it was once imported.
4. When publishing, record an **included-by** release only after verifying its tag
   contains the integration commit. Pending branch work is not “in main” or
   “released.” A fast-forward source-mirror update is not an app import.
5. Append a dated journal entry. Correct the current status tables when decisions
   change; historical port notes remain supporting evidence, not a competing
   current-status list. Do not use raw commit counts as a feature-parity measure.

New-row template:

| Source PR / exact commit(s) | Status and location (pending branch / main / release) | Adopted scope; modifications; exclusions and reason | Local commit(s), validation, vehicle limits and included-by tag |
| --- | --- | --- | --- |
