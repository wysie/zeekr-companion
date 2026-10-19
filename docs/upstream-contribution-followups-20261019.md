# Merged contribution follow-ups — 2026-10-19

Reviewed all 24 wysie-authored upstream PRs against dts88 main
`151040e4d770ebbaae034044fe789383f0f4ee3f`, with Companion starting at
`79adc4a30457e86abad19969d905881a8db45ddf`. Three are merged, one is closed
and twenty remain open. Also inspected the maintainer-owned replacement/adaptation
PRs below; reading an open PR is not approval to import it. Ferix was not refreshed.

## Accepted and adapted contributions

| Our contribution | Upstream result and maintainer changes | Companion decision |
| --- | --- | --- |
| [#17: Super mirror direction labels](https://github.com/dts88/zeekr-shortcut-car/pull/17), submitted `b9c2e8fd` | Direct merge `76138b8d`. The label implementation and relevant merge result match the submitted tree. Subsequent #39 is a Chinese product-name change, not a label algorithm fix. | Already present through merged ancestry. Keep our later theme-aware/popup/HUD label extensions. No missing functional label fix to port. |
| [#28: unused microphone permission](https://github.com/dts88/zeekr-shortcut-car/pull/28), submitted `8081ae30` | Included by [#58](https://github.com/dts88/zeekr-shortcut-car/pull/58), merge `0d8558bc`. Follow-up `319fef9c` deletes dead microphone title/explanation strings in all three languages, removes one blank line and drops the extra README permission sentence. The original runtime fix is retained. | Adopt dead-string/whitespace cleanup. Keep our existing feature documentation: Companion’s README documents these features, unlike upstream’s shorter README. The startup check/permissions-row fix already existed in our fork. |
| [#38: updater integrity](https://github.com/dts88/zeekr-shortcut-car/pull/38), submitted `64ddf7d3` | Included by [#59](https://github.com/dts88/zeekr-shortcut-car/pull/59), merge `e3958367`. Follow-up `ebe3e2c3` adds named constants, normal code formatting, explanatory comments and literal `"null"` digest coverage; consolidates/rewords verification errors; drops the README internals note. `87a6a8f3` credits wysie in NOTICE. No changed hash or redirect policy. | Adopt updater helper/comment/constants cleanup, the stronger known-byte/null regression, broad verification error wording and redundant Chinese override removal. Our errors already live in the main string files. Retain Companion repository/channels, version selection, signer, installer flow and our feature documentation. Add an actual Android JSON/download-boundary regression. Credit dts88’s follow-ups in NOTICE rather than copying the upstream acknowledgement of ourselves. |
| [#19: automatic side camera](https://github.com/dts88/zeekr-shortcut-car/pull/19), closed, submitted `f57cac07` | Adapted into [#41](https://github.com/dts88/zeekr-shortcut-car/pull/41), merge `95f5b989`, with Ferix #4 credit. Dismissal, fitted framing and aspect-aware Rays retain our contributed behavior; comments/structure are adapted and an old clamping convenience API is omitted. [#43](https://github.com/dts88/zeekr-shortcut-car/pull/43), `d73ce518`, changes fixed window dimensions and 7X-tested angles. | Existing `51ccf4c0` / `2f480a25` already port the useful reconnection, retained texture, refusal and immediate removal paths; see the lifecycle port notes. Keep our resizable windows, HUD, mirror relationship, freshness and optional auto-fit/rotation behavior. Do not remove a comparison API still used by the fork or replace saved 009 tuning with 7X defaults. No new camera renderer was imported in this review. |

The remaining batch PRs are still open in this snapshot: #29–#37 and #44–#54.
Their published heads are untouched. Other upstream work is outside this merged-
contribution scope: #40’s lane-change-assist info-bar correction remains deferred;
it is a setting/status source, not a confirmed obstacle-warning trigger. #60’s
signing-key rotation is open and is not imported. No signing or install migration
is made as a side effect of this review.

## Adopted port and validation

Selected follow-ups are in Companion commit
[`06f0bf42`](https://github.com/wysie/zeekr-shortcut-car/commit/06f0bf4260a9dd76d9e4555d423bad294f4950d2).
The port retains dts88 authorship and source links, with wysie’s adaptations
co-attributed. Original source commits are
[`319fef9c`](https://github.com/dts88/zeekr-shortcut-car/commit/319fef9c5b64a77d5c5f90f2a00ee50358677b39)
and [`ebe3e2c3`](https://github.com/dts88/zeekr-shortcut-car/commit/ebe3e2c398ce5b34ab2f6502a5ed9941e57792ac).
The unmodified `upstream-main` source mirror fast-forwards to the reviewed upstream
head; this is separate from the selected app port, not a wholesale app merge.

All 1,505 JVM tests, debug lint, debug/release assembly and instrumentation assembly
pass. Three native Android tests pass: actual `JSONObject.optString` behavior for
JSON-null/missing digests and download rejection before URL/network/file creation,
plus existing update-channel migration and picker-confirmation/recreation checks.
The literal-null digest was already rejected by the validator; this adds regression
coverage rather than claiming a newly discovered acceptance vulnerability. The
updated verification message covers published-size mismatch as well as checksum
or metadata failures. The signing certificate remains
`a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc`.

No live release download/install, vehicle test or new public release is claimed.
Release inclusion must be verified against a future release tag.
