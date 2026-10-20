# Recommended follow-ups completed — 2026-10-20

The owner authorized the [review recommendations](source-review-20261020.md).
All three work items are complete; no new release was requested or published.

## Existing upstream PRs

Refreshed #46/#48/#52/#53/#54 against dts88 main
`eb7d5732cf8d20f75439d78da5a76f4254f684fc`, preserving original published
history through merge commits and non-forced pushes. #53/#54 include refreshed
#52; they still do not depend on one another. Upstream's new lock action and grouped
settings are retained in each contribution. Version/signing/workflow files match
upstream, and Companion-only implementation/branding is absent from these diffs.

| PR | Previous head | Updated head |
| --- | --- | --- |
| #46 Double tap | `7d219ec6` | `455e9a89` |
| #48 Theme choice | `7ea6b296` | `6acd823e` |
| #52 Camera sources | `e4d12add` | `4f521d02` |
| #53 Recording groups | `6faedfa2` | `c1935b2c` |
| #54 Independent preview | `b98340df` | `77aa8a7f` |

Each updated branch passed its own JVM suite and debug/test/release builds.
Combined validation branch `review/upstream-refresh-20261020` at `a910c669`
passed 1,172 JVM and 19 native API32 cases plus builds and release lint. Its shared
README/resource resolutions preserve both feature additions; the shared road-group
label is deduplicated across default/translated resources including Chinese.
#46's settings regression now assigns upstream's sixth action, Lock current
recording, to double tap and checks persistence.

PR descriptions include refresh, validation and prerequisite details. Final GitHub
snapshot: 24 wysie PRs total, 17 open / 3 merged / 4 closed. All 17 open PRs are
mergeable and have passing head builds. The five updated head hashes match the
table; unrelated published PR heads and their discussions were preserved.

Evidence: `/tmp/companion-pr{46,48,52,53,54}-refresh-build.log`,
`/tmp/companion-pr-refresh-combined-build.log`,
`/tmp/companion-pr-refresh-combined-native.log`, and
`/tmp/companion-recommendations-final-pr-status.json`.

## Companion manual recording lock

Adapted dts88 #61 commits `e8dedd81` / `ccd441a4` with authorship retained in
`2adee171` / `c302fa7e`, plus Companion follow-up `d1d39a09`; integrated into
main by `63c943f4`. Tap/double tap/hold offer the new action. Existing settings,
braking-only budgets and atomic list writes remain. Explicit manual protection
promotes matching automatic brake locks so the budget cannot later unlock them.
See [feature behavior and six native checks](manual-recording-lock.md).

## Companion settings backup

Adapted dts88's open #64 at `35bdfc197c208a983eb4fb5dc1c8ede4813db265`
with authorship retained in `1736a39c`, plus selected-map loading `3c8b4351`;
integrated into main by `b94a3cb2`. Covers all five Companion preference stores,
including calibration/window/appearance and vehicle/GPU choices, with distinct
file identity and local runtime/access state excluded. A confirmed import stages
the backup, exits normally, and applies it before configuration/services initialize
on the next process start. Durable preimage/commit-marker recovery handles partial
multi-store writes and interrupted startup. See [behavior and tests](settings-backup.md).

Final Companion validation: 1,528 JVM tests, zero lint errors, debug/test/signed
release builds, six manual-lock/floating Android checks, five backup Android cases
and two recording/Apply cases. The final metadata-only picker path was rerun
successfully. A separate fresh-process check verified the earliest startup restore
and returned the emulator's preference files to their original bytes. The native
backup page, import confirmation and lock-action dialog were inspected in both
themes in one batch; detector returned no findings.

No application ID, signer, release version/tag or source-mirror branch was changed.
No Ferix recovery patch, wholesale upstream #62 XML, unverified rear-cabin mapping
or signing-key migration was imported. Vehicle/USB performance, physical power-loss
durability and a real 009/7X restore remain unverified. These changes are in source,
not a newly published downloadable release.
