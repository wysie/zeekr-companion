# Source recommendations completed — 2026-10-23

The owner authorized the review recommendations and explicitly approved merging
and pushing the Companion camera fixes and review records to main. No new APK
release was requested or published in this follow-up.

## Companion camera lifecycle fixes

Selected dts88 #65 (`fc04167c`) and #66 (`be614c00`) changes are adapted in
`062c3ea2` and merged/pushed through `3d0d8e3a17d5f283bb328260569af8f27d94e6d7`.
[Main CI 39922297118](https://github.com/wysie/zeekr-shortcut-car/actions/runs/39922297118)
passed. Source authorship is retained, with Companion adaptation credit in the
commit and NOTICE. [Detailed scope](camera-lifecycle-followups-20261023.md).

Camera surfaces remain owned through device-close completion. Rejected mirror
outputs stay withdrawn through watchdog ticks and retry on explicit resume,
screen-on, undocking or a changed pipeline/texture. All auxiliary owners receive
drop notifications. Recovery unlocks only lanes affected by a changed consumer.
Fork additions preserve popup/HUD/360 kinds, developer probes, independent
recording/preview, Apply and GPU settings. Holder-change collection is synchronized
and its initial observed baseline is reconciled to avoid replaying old claims.

Validation: **1,564 JVM tests**, **12 native Android checks**, debug/release/test
assembly, zero lint errors and five packaging/release-note tests. Native checks
use actual SurfaceTexture ownership and output declarations without opening a
camera, plus popup lifecycle and mirror controls. These changes remain unreleased;
no included-by release or on-car timing/performance claim.

## Existing upstream PR refresh

Final source base: `91a4282e75b39364685389a2ce5e73e43301832f` (2.24.0-alpha).
The later #96 System-settings commit arrived during validation and is included
in every refreshed PR head. Its UI is a reference for Companion, not a runtime
import. Ferix stays reviewed through `2dce2510c373d43cd850d8dc040d701311b63bb6`;
its alternate-architecture free-report/side-yield patch is not copied here.

Eight existing PR branches were updated with regular merges and non-forced
pushes. #53 and #54 each include the refreshed #52 foundation and remain independent
of one another. Three already-clean heads (#34/#37/#44), merged submissions and
closed submissions were preserved. Descriptions now explain the final scope,
dependencies and validation. #51 is renamed to make its remaining contribution
clear: one channel selector, not newly adding alpha support.

| PR | Pushed head | Independent JVM tests |
| --- | --- | --- |
| [#29](https://github.com/dts88/zeekr-shortcut-car/pull/29) | `3fa23631242d25e1e3bf367b2e5790a11505dd36` | 1268 |
| [#32](https://github.com/dts88/zeekr-shortcut-car/pull/32) | `f73b8ddb2110ddc830b89c7cf8f00994a5e0c07b` | 1266 |
| [#47](https://github.com/dts88/zeekr-shortcut-car/pull/47) | `31b72fa1eba7794ee6df82fffb698072f113cfc4` | 1270 |
| [#50](https://github.com/dts88/zeekr-shortcut-car/pull/50) | `31303850bbc96fb0e62e39f1b47c069e537d2be1` | 1271 |
| [#51](https://github.com/dts88/zeekr-shortcut-car/pull/51) | `c685b5f9972a17dc19e78c18f0f0fc67b7828476` | 1273 |
| [#52](https://github.com/dts88/zeekr-shortcut-car/pull/52) | `fb431bf12d3d985a2661db3d0334eb46596905c1` | 1272 |
| [#53](https://github.com/dts88/zeekr-shortcut-car/pull/53) | `cc97b13395f08c9e00a19b9dbe5cd86d2c0ae43d` | 1277 |
| [#54](https://github.com/dts88/zeekr-shortcut-car/pull/54) | `00e9a9dfb42dffbd1c5ad8e528cf061720ffca40` | 1275 |

Final GitHub verification: **all eleven open PRs are mergeable and their build
checks pass**. Every refreshed remote head matches the validated table above.
No new merged/closed submission: 11 open / 3 merged / 10 closed.

Each final head passes its JVM suite and debug, instrumentation and release builds.
Application/release/workflow files match upstream exactly on all eight heads.
The combined upstream-only validation branch `review/upstream-refresh-20261023`
at local commit `4f9bc6edf86a10dd7ca2ff03b68c6bcda2663c4f` includes all eleven open
contributions: **1,293 JVM tests and 24 native Android checks pass**, with debug,
release and instrumentation builds. Full debug lint retains upstream errors:
**951 baseline / 946 combined, zero new error fingerprints** (ID, message and
source path, independent of shifted line numbers). Release lint/build passes.
The unchanged template package-name instrumentation test is excluded.

Native tests cover responsive controls/disabled states, developer mode, source
mapping and reset/cancel, all initialized-source ownership, recording groups,
actual road-only playback, independent preview selection/recreation, stale-session
results, lane-clipped unfitted drawing, popup layout and channel migration/picker
confirmation. Additional independent runs pass all eleven preview/source checks
and all three update-channel checks. The final combined run passes in one process.

Integration caught and corrected a preview recreation loop introduced by the new
upstream run-lane check: it now compares preview keys with preview keys. Preview-only
roads remain distinct from recording selection; mirror-only cabin and front-cabin
retirement are retained in these upstream contributions. #51 migrates all four
legacy beta/alpha combinations by effective eligibility, retaining upstream's Beta
default and explicit alpha choices. Larger upstream text required scrolling actual
adapter lists and editor controls in tests; expectations were retained. Shared
resource labels are deduplicated in the combined tree, and test-only developer
setup follows #32's API there without adding that dependency to other PRs.

Owner logs/photos and private artifacts were not published. No upstream comment,
new PR, signing/package change, source-mirror movement, Companion audio/cabin/UI
import or new release was made. Emulator checks do not establish 009/7X vehicle
behavior or physical storage/HUD performance.
