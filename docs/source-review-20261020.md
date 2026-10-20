# Upstream and Ferix review — 2026-10-20

Compared Companion main `3c07e10ed78153afeff40acf02907f670a4e71ce` with
freshly fetched source heads and GitHub PR/comment/review/check data. This is a
review, not a runtime integration or release. All published contribution heads
are unchanged. The remote-tracking refs were refreshed; local unmodified source
mirror branches were not advanced in this pass.

## Incoming main commits

| Source and exact reviewed range | Finding and disposition |
| --- | --- |
| dts88 `151040e4d770ebbaae034044fe789383f0f4ee3f..eb7d5732cf8d20f75439d78da5a76f4254f684fc` | Six commits: #61 implementation/text/merge, version 2.14.0-alpha, and #62 implementation/merge. No new blind-camera quality, GPU renderer or obstacle-warning decoder in this range. |
| Ferix `3f9c7e4033627aad53cd3f614c5643de7ac78040..130e7f9a1f5c2117fc9f5f03e019407b0d68281c` | Five commits: changelog marker cleanup, watchdog lift implementation/log wording/retry cap, and version 2.14.2-alpha. No new HUD geometry, GPU renderer or blind-spot signal decoding in this range. |

### dts88 #61: floating-button recording lock — recommended adaptation

Implementation `e8dedd811687f45e9d26221348ed34ed1e59000b`, approved text
`ccd441a4df5480aaf5d584a5dc060613efdb62f6`, merged as `7505ef01`.
Adds Lock current recording to tap/long press. It protects all camera clips covering
ten seconds before/after the press, using two passes. It requires Lock footage and
an active recording, not the automatic flash-to-pass switch, and gives one result
per press (including already locked or failed-to-save).

Companion has no manual floating-button lock action. Adapt to its existing
FLASH/BRAKE AutoLock sources and optional double-tap dispatcher. Preserve brake-only
automatic-lock budgets, atomic lock-list publication, existing gesture assignments,
relay handling, new exterior file roles and folder compatibility. Do not replace
our AutoLock wholesale. Not imported or tested here.

### dts88 #62: grouped settings — equivalent already present

Implementation `2cb1867f8b81afd44761e79621c44ce6c49536f2`, merge `eb7d5732`.
Maintainer rebuilt our #31 idea against current rows and glossary, retaining Stamps
as a group rather than a subpage. Companion already has task groups, native-style
navigation, blind-camera tabs and separate HUD/GPU pages. No wholesale XML import:
it would remove fork-only settings. The explanatory headings/placing screen-off
recording beside automatic recording are possible future UX comparisons, not a
missing core feature or an adopted runtime change.

### Ferix watchdog follow-up — no direct import

Implementation `d21839329731cfd5fde659ee3bf2711a11e61a7f`, wording
`d6602267ed24010fe59af8ef2f4e936575f3a009`, cap
`b660216f26d4151a50a1754225cdd6b651090547`.
Ferix ports dts88-style stopped-watchdog recovery on screen-on/camera-free to its
older manager. Companion already has these events plus needs-change recovery in
`ChannelRules.liftStops`, with scheduler ownership and retry spacing. Ferix then
caps camera-free lifts at two per stuck episode because its SingleCamera retains
independent reconnects, which can generate its own availability loop. Companion's
SingleCamera has no such independent reconnect loop. Retain our scheduler; do not
copy the manager callback/cap as if our policy were missing. This is a code
comparison, not proof that every hardware recovery case is solved. Version metadata
and the already-absent conflict marker are not imported.

## Open upstream proposals, separate from merged main

| PR / reviewed head | Assessment |
| --- | --- |
| [#64 Settings export/import](https://github.com/dts88/zeekr-shortcut-car/pull/64), `35bdfc197c208a983eb4fb5dc1c8ede4813db265` | Useful next candidate; open, build passing, no review/comment yet. Exports app_config minus runtime keys plus profiles, restores matching major-version files, and adds a full settings snapshot to diagnostics. A Companion port must also cover its separate calibration/window stores, vehicle overrides and GPU policy, distinguish cross-fork file compatibility, and preserve runtime/identity state. Code commits the two stores sequentially, not as a transaction across both: partial-write recovery needs deliberate handling. Do not promise full Companion backup from the unmodified patch. |
| [#63 Rear + rear cabin mirror](https://github.com/dts88/zeekr-shortcut-car/pull/63), `abf1d740b861a9edb91ca22bc20ac1703b66258b` | Optional later candidate; open, build passing, no review/comment yet, explicitly not car-tested. Adds a real cabin output and mirror-specific camera demand, not a crop of the surround feed. Requires a confirmed rear-cabin source and adaptation to our GPU routes, Apply flow and independent recording groups. Our owner has not confirmed a usable rear-cabin feed; no generic 009/7X mapping assumed. |
| [#60 Release signing-key rotation](https://github.com/dts88/zeekr-shortcut-car/pull/60), `924c9d54eff045dd16365d01e08252152faedf6e` | Still open. Review only; no signing/workflow/secret change. Their private-key release job, v3 lineage and update-in-place checks require a separate Companion release migration, not copying dts88 identity or replacing our signer during a routine merge. |

The open proposals were inspected by scope/code where noted; none was built or
vehicle-tested in Companion. Passing source CI does not validate a future fork port.

## Our outgoing PRs

24 total: **17 open, 3 merged, 4 closed**. All 17 open published heads have a
successful build check and no submitted maintainer review/comment. GitHub's
mergeability snapshot is against current main; old successful head builds are not
tests of a new conflict resolution.

| Current state | PRs |
| --- | --- |
| Merged (unchanged) | #17; #28 via maintainer #58; #38 via maintainer #59 |
| Closed, adapted (unchanged) | #19 → #41/#43 |
| Newly closed, replaced | #31 → merged #62; maintainer explicitly thanks the proposal |
| Newly closed, declined | #33 and #35, reasons below |
| Open, mergeable | #29, #30, #32, #34, #36, #37, #44, #45, #47, #49, #50, #51 |
| Open, conflicting | #46, #48, #52, #53, #54 |

Five PRs conflict; the other twelve open PRs are mergeable. Recheck immediately
before updating any branch. #53/#54 include prerequisite #52 and remain independently
stacked on that foundation, not on each other.

#33: maintainer keeps relay/archive paths Developer-only and does not want private
internal buffers retained without expiry. #35: maintainer says current repair
appends the index before patching mdat, rolls back unreadable results, already caps
the index, and avoids full copies on a nearly full USB drive. Permanent unbudgeted
backups would consume space. These are explicit design objections, not failed CI.
Companion retains its existing verified-copy/repair-original behavior; no removal
is authorized by this review. Backup size/retention deserves its own audit before
extending that behavior.

Recommended order: adapt #61; design a complete Companion backup based on #64;
refresh conflicting outgoing branches while retaining their reviewed scope. Leave
rear-cabin mirror until a real source is identified. No PR comment, edit, new PR,
runtime import, signing change or release was made in this review.
