# Fork workflow

The remotes are `origin` (wysie's fork), `upstream` (dts88's repository), and
`ferix` (FerixzTanz/CAMCORDER).

The [source integration ledger](docs/source-integration-ledger.md) is the maintained
incoming-change record. Consult it before proposing imports: it distinguishes full
merges, adapted/partial ports, deferred/skipped changes and retired experiments.
The [upstream contribution index](docs/upstream-contributions.md) instead tracks
outgoing PRs; neither index substitutes for the other.

| Branch | Purpose |
| --- | --- |
| `fix/recording-preview-isolation` | Separate recording compatibility from pending preview/overlay choices, retain recording-source/mapping restart checks and export distinct configuration evidence |
| `feature/factory-style-settings` | Owner-approved factory-style settings/main toolbar, blind-camera tabs and read-only shared-texture Picture preview |
| `main` | Combined installable fork: completed features, fork version numbers, update source and build configuration |
| `feature/companion-icon` | Approved v3 linked-companions adaptive, themed and notification icons; compatible package/signing identity |
| `feature/companion-branding` | Zeekr Companion display name, About attribution and compatible APK/release naming; logo selection remains separate |
| `fix/preview-state-and-dock-rhythm` | Applied/saved preview clarity, single-source layout explanation and compact recording action with live long-press details |
| `fix/native-ui-polish` | Astra/native UI review fixes: truthful preview/window/playback actions, accessible settings/gallery and retained camera/recording behavior |
| `fix/flat-main-toolbar` | Transparent native bottom-toolbar actions, shared divider and retained recording/selection feedback |
| `feature/companion-camera-home` | Approved camera-first home screen, staged preview presets, in-place source-pane layouts and retained recording/surface behavior |
| `feature/main-page-controls` | Compact main page, shared preview selector, explicit popup controls and expanded preview with retained camera views and accessible recording controls |
| `fix/camera-status-and-recovery` | Read-only recorder/producer status, persistent live/planned counts and truthful accessible waiting states; no retry-policy changes |
| `fix/settings-semantics-and-setup` | Scoped confirmed resets, canonical setup order, live actions and persistent pending-source notice |
| `fix/hud-and-troubleshooting-ui` | HUD groups, explicit USB troubleshooting, persistent diagnostic paths and foreground stop-control policy |
| `fix/ui-clarity-verification` | Evidence-backed long-value wrapping, translated clarity copy, theme/font/native verification and outgoing contribution record |
| `feature/automatic-360-projection` | Automatic/Manual/009 projection intent with model-aware defaults, preserved explicit/legacy manual tuning and inactive-7X reporting |
| `feature/floating-double-tap` | Optional double-tap action with delayed single-tap arbitration only when assigned; preserve existing tap/hold choices, dragging and cancellation |
| `upstream-main` | Unmodified dts88 code; tracks `upstream/main` and updates by fast-forward only |
| `ferix-main` | Unmodified CAMCORDER code; tracks `ferix/main` and updates by fast-forward only |
| `contrib/01-*` through `contrib/11-*` | Independent upstream contributions; each starts directly from dts88 main, with no fork versions or updater configuration |
| `feature/shared-camera-sources` | Canonical source mappings with conservative legacy migration, shared page/picker, Automatic reset and detected-surround priority; alpha test release |
| `feature/preview-layout-selector` | Visible independent main preview selector with current-choice confirmation, combined surround/road mode and preservation of saved preview/recording/profile tuning |
| `feature/vehicle-model-profiles` | Functional Auto/009/7X model selection, conservative migration/defaults and visible evidence; preserve overrides and actual HUD matching |
| `feature/recording-source-groups` | Independent recording groups and preview, direct source choices, standalone playback and bounded extended camera-ID export; defaults preserve existing recording selections |
| `feature/side-view-auto-fit` | Default-on corrected side-picture fitting switch; off retains requested framing with black source-lane clipping across screen, HUD and mirror replacement |
| `fix/about-wysie-credits` | First-person wysie Edition introduction, prominent dts88/Ferix/EVCam credit and clearly attributed original community thanks |
| `fix/signal-scan-callback-details` | Read-only notification IDs/times and bounded byte payload evidence for blind-spot research; no automatic warning trigger |
| `fix/side-camera-quality` | Fit corrected side-camera roll in the final window without a second cover zoom; bounded finer mesh for larger views |
| `research/side-camera-native-sampling` | Audit the native-buffer path and retain an EGL/TextureView fine-detail regression; no camera-resolution or routing changes |
| `feature/7x-360-car-overlay` | Original 7X artwork in three colours, conservative Auto/009/7X/Outline selection and explicit vehicle-validation limits; no 7X calibration claim |
| `feature/360-car-colours` | Saved Green / White / Black 009 illustration choice, live shared main/popup/photo repaint and independent calibration reset |
| `feature/360-car-overlay` | Original 009-style vehicle artwork over the existing DIY 360 footprint, shared by main/popup/photo rendering; no transparent-underbody claim |
| `feature/009-360-polish` | Wider independent 009 fitted framing, higher-detail Balanced sampling and subtler opaque car surround; retains fitted transforms and vehicle-testing limits |
| `feature/009-image-fitted-360` | Opt-in empirical 009 ray/plane transforms fitted from owner images, independent of manual geometry; common main/popup/photo projection |
| `feature/update-channels` | Owner-selected Stable / Beta / Alpha update filtering, legacy switch migration and single-choice settings |
| `feature/usb-drive-logging` | Explicit USB-only foreground research sessions, raw event/app/lifecycle parts, start/stop Diagnostics and visible interruption/failure status; no boot start or camera changes |
| `feature/side-popup-aspect-ratio` | Saved square/portrait/landscape shapes for the separate blind-spot window, preserving pinch/drag/expand and independent HUD/Super mirror geometry |
| `feature/turn-signal-side-view` | One complete side-view feature: PR #4 port, independent Blind spot camera settings, temporary mirror view, Use Super mirror when active option, independent popup and pinch resizing |
| `feature/camera-labels` | Optional camera labels, developed separately from side-view display modes |
| `personal-build` | Historical snapshot before promotion to `main`; no new feature integration or build triggers |
| `pr4-followup` | Credited consolidated blind spot camera contribution, building on upstream PR #4 and preserving the original author's commit; excludes labels and personal build settings |
| `feature/phone-sharing-fixes` | Ferix phone sharing fixes and connection diagnostics |
| `feature/vehicle-signal-scanner` | Read-only scanner, sensor/polling fixes and developer-mode shutdown |
| `feature/hard-braking-lock` | Hard-braking footage protection with automatic-lock budget |
| `feature/clip-export` | Range exports, camera swipes and protection against file collisions |
| `feature/driver-recording` | Driver sessions integrated with current upstream recording/persistence rules |
| `feature/side-view-arrows` | Turn arrows for both display modes; starts from the existing side-view feature |
| `feature/side-view-hud` | Ferix PR14 adapted to current scheduler and fork screen behavior, with cautious display selection; based on main after mirroring |
| `fix/009-hud-fit-and-detection` | Owner-confirmed automatic 009 HUD signature, independent fixed-right layout and fitting to measured Presentation content; factory HUD graphics remain independent |
| `feature/009-hud-display` | Opt-in 009 HUD mapping for display 3 at 1280×640, based on the owner's Diagnostics; physical destination and video output await testing |
| `fix/side-popup-startup-flash` | Prepare the separate side popup transparently until live frames arrive, with a delayed waiting notice and retained stalled-frame cover |
| `fix/playback-action-layout` | Shared content-sized wrapping photo/video controls, retaining native text size and same-row alignment |
| `fix/settings-layout-overflow` | Camera/preset card sizing, wrapping options/actions, short-window action-rail scrolling and settings category navigation; native Android layout regression tests |
| `fix/unused-microphone-permission` | Remove undeclared, unused microphone startup gate and its unavailable Developer permission row |
| `fix/remove-park-only-gates` | Remove gear/speed eligibility from developer tools and DIY 360 preview/calibration; retain factory priority, lifecycle and freshness guards |
| `feature/developer-camera-display-tools` | Batch Camera2 snapshots and numbered public-display identification, with exclusive native camera closure and Developer-folder exports |
| `fix/profile-camera-selection` | Honor individual enabled channels, cabin-only previews and recording; reject unapplied channel changes with a restart instruction |
| `fix/automatic-recording-permission` | Recording request origin, current-setting checks and opt-in driver-start defaults; existing PR heads preserved |
| `feature/vehicle-name-labels` | Cosmetic Auto/009/7X vehicle names, conservative Android-name detection and generic fallback; camera profile IDs and HUD mapping stay independent |
| `feature/side-view-system-theme` | System day/night styling for popup and mirror controls/labels, with live configuration repaint and retained HUD backing |
| `feature/side-view-mirroring` | Fork side-camera horizontal flip, including mirror replacement; existing PR heads preserved |
| `fix/display-identification-visibility` | Main-screen control check, individual larger secondary markers and explicit physical-display observations; no automatic HUD mapping |
| `fix/usb-internal-fallback-policy` | Explicit off-by-default internal fallback, USB startup grace and cancellable waiting state; keep direct internal/relay choices separate |
| `fix/remove-hud-overlay` | Remove the unsuccessful higher HUD window layer while retaining ordinary HUD routing and 009 research settings |
| `feature/settings-menu-organisation` | Task-based settings navigation and live manual 360 popup switch, preserving saved configuration and existing PR heads |
| `feature/side-popup-hide-mirror` | Three-way mirror relationship with presentation-driven temporary hiding and retained mirror surface/settings |
| `feature/hud-factory-coexistence` | Off-by-default HUD coexistence with factory centre-screen views, keeping reverse, screen output and hardware arbitration independent |
| `feature/developer-mode-toggle` | Password-free System developer switch with warning, preserved mode persistence and immediate category refresh |
| `integration/upstream-side-view-lifecycle` | Selective dts88 PR #41 lifecycle adaptation: two-second reconnection, same-texture reuse, safe immediate removal and refusal episodes; retains fork controls and existing PR heads |
| `integration/merged-upstream-20261016` | dts88 merged main through 76138b8d, selected merged Ferix work through 3f9c7e40, and owner-log fixes; existing PR heads preserved |
| `integration/ferix-2.12.5` | Combined CAMCORDER follow-ups through d6193888, adapted to dts88 2.11.6 and existing fork behaviour |
| `feature/ferix-scan-callbacks` | Customize-value callbacks and bounded callback-shape reporting |
| `fix/ferix-shared-pipeline` | Retain foreground/relay services across rapid Activity recreation using the shared pipeline |
| `feature/ferix-hud-refinements` | Opt-in contrast/night/edge controls, indicator-side position and display-window-context overlay |
| `feature/ferix-side-transitions` | Popup/HUD fades, retained startup grace and fresh-side/cancellation guards with native regressions |
| `integration/upstream-2.11.6` | Upstream camera closure ordering and diagnostic updates, retaining optional fork outputs and original contribution heads |
| `integration/upstream-about` | Upstream 2.11.4 integration and fork-only About attribution; existing PR heads preserved |
| `review/project-safety` | Whole-project audit and recording, storage, sharing, update, privacy and lifecycle corrections; based on combined main |
| `fix/diy360-projection-repair` | Narrow DIY 360 joins with real-raw-frame reproduction; does not install a guessed/fitted calibration |
| `feature/diy-360-safety` | Camera safety review fixes: readback validation and shared stale-frame guards; based on main |
| `feature/diy-360-view` | Fork prototype of live ground-plane stitching, with independent manual calibration; based on main |
| `feature/diy360-popup` | Dedicated 360 settings and manual overlay, with shared renderer/calibration, independent saved layout and lowest-priority camera surface |
| `feature/diy360-cadence` | Faster bounded CPU preview pacing at unchanged resolution, duplicate-frame readback avoidance and presentation timing diagnostics |
| `feature/diy360-refinement` | Bilinear preview sampling and fresh stitched-photo/calibration exports; geometric calibration remains unresolved |
| `feature/app-storage-folders` | Unified public app folder with legacy playback, per-directory protection and combined cleanup; no automatic migration |
| `feature/diy360-resolution` | Sharper default and native-size preview-copy option, bounded allocation/performance fallback and retained quality diagnostics |
| `feature/night-ui` | Ferix night palette and floating-button styling |
| `research/blind-spot-alerts` | Alert-signal investigation; no active obstacle trigger |
| `research/factory-360-view` | Investigation of access to the factory stitched view |

The original turn-signal side-view contribution keeps its related display behavior
on one feature branch. Fork follow-ups for mirroring and HUD are separate branches
based on main, preserving that existing upstream PR head. Camera labels remain a
separate feature. The label branch is based directly on upstream main
and contributes configurable Super mirror labels through [upstream PR #17](https://github.com/dts88/zeekr-shortcut-car/pull/17).
The personal integration also retains side-view labels from the earlier label work;
that hookup can follow when upstream has the side-view feature. Merge finished
features into `main` and build that branch when an APK containing all of
them is wanted. Research notes do not activate features.

The former combined `side-view` branch and separate `feature/side-view-display-mode`
branch have been consolidated into this layout. Their history is retained in
`main` and the historical `personal-build` snapshot; the existing 2.11.5-sideview-alpha APK still contains both features.

For the consolidated blind spot camera PR, use `pr4-followup`. It builds on PR #4
and retains the original FerixzTanz commit and authorship, with attribution in
`NOTICE.md`. The PR description should link PR #4 so the maintainer can decide
how to incorporate the contributions. Other independent contributions use their
own feature branches, without fork build settings.

For new independent contributions to dts88, start from the upstream code rather
than the combined fork:

```sh
git fetch upstream
git switch upstream-main
git merge --ff-only upstream/main
git switch -c feature/my-change
# Implement and test the change, then commit and push the feature branch.
```

A dependent feature can start from its prerequisite feature branch. Keep personal
version numbers and updater configuration on `main`. Fork-only work can start from
`main`; do not use that branch as the base for an independent upstream PR.
The [upstream contribution index](docs/upstream-contributions.md) records the submitted
PRs and their validation. Preserve these published heads unless updating that PR.

Fetch Ferix into `ferix/main`, then fast-forward `ferix-main`; import selected changes
through separate feature/integration branches, preserving attribution. Do not merge
personal fork changes into either source tracker. Updating a tracker does not update
the combined APK until those changes are explicitly integrated into `main`.

For a combined build:

```sh
git switch main
git merge --no-ff feature/turn-signal-side-view
git merge --no-ff feature/camera-labels
source ~/.config/android-dev/env.sh
bash ./gradlew :app:testDebugUnitTest assembleRelease
```

The build workflow runs on pushes to `main` and `feature/**`, and on pull requests
targeting `main`; its APK artifact is
`ZeekrCompanion-apk`. Increment `versionCode` and update `versionName` before each new
installable release. The existing signing key preserves update compatibility.

Fetch dts88's updates into `upstream/main` and fast-forward `upstream-main`. Review
the incoming changes against the ledger before integrating. For a complete import,
merge the source mirror on an integration branch based on `main`; for selective
ports, apply/adapt only the recorded scope there. Resolve conflicts while retaining
the fork's features and build configuration. Validate, commit and merge the finished
integration branch into `main` with `--no-ff`. Push the integration branch and `main`
to `origin` explicitly; never
push the combined fork to the `upstream` remote. Keep `main` buildable and use
release tags to identify the exact source of downloadable APKs. Existing
`personal-build-…` release tags and downloads remain valid.

Every external update review must update `docs/source-integration-ledger.md`, even
when no source changes are imported. Record the date, exact heads/range reviewed,
decisions and reasons, adopted/excluded scope, fork modifications, local commits
and validation/vehicle limits. Distinguish still-unassessed work from deliberate
omissions. Keep the history when a skipped change is later adapted or an experiment
is removed. Verify release-tag ancestry before marking an import included in an
APK; do not infer integration from source-mirror position, a merged source PR or a
raw count of commits ahead/behind.

Whenever a feature is added or changed, update `README.md` in the same change:
describe the behavior, where to enable/configure it, defaults that matter, and
known limitations. Keep the additions list accurate, credit imported work in the
README and `NOTICE.md`, and distinguish implemented features from research.
Update the changelog and test notes when the change affects them. This policy is
also recorded in `AGENTS.md` for future development sessions.

Automatic blind spot camera is under Settings → Blind spot camera and is off by
default. Use Super mirror when active is on by default and temporarily
uses the existing mirror canvas. With it off, an independent side popup shares the
camera stream and supports pinch resizing. A rejected side output yields before the
mirror and recording. Both side cameras share one saved popup location; drag it to position
and pinch to resize. The owner reports labels and mirror replacement working on the 009;
the shared placement and drag changes still need an on-car check.

With Auto-fit to avoid black patches on (default), corrected side views narrow
the requested FOV when high rear/up aiming would cross the camera image edge. Aim and saved settings are retained.
At 80° rear / 15° up, a requested 90° square view fits at approximately 20.5°;
this avoids collapsed black tiles but greatly reduces visible coverage. Turning
Auto-fit off preserves the requested corrected FOV with black missing areas; it
never samples the neighbouring composite camera.

## Ferix feature ports

The seven port branches above are integrated into `main`. Independent ports start
from `upstream/main`; arrows depend on `feature/turn-signal-side-view`. Existing
PR #17 and #19 branches are unchanged. Source authorship and original commit links
are recorded in port commits and `NOTICE.md`. See `docs/ferix-feature-ports.md`.
New combined releases use `vX.Y.Z-alpha.N`, `vX.Y.Z-beta.N` or `vX.Y.Z` tags on
`main`. Historical `fork-…` and `personal-build-…` release tags/downloads remain valid.

## Zeekr Companion release naming

- Display/release identity: **Zeekr Companion** in every language, without an author suffix.
- Android `versionName`: `X.Y.Z-alpha.N`, `X.Y.Z-beta.N`, or stable `X.Y.Z`; increment `versionCode` for every installable release. Keep application ID and signing identity.
- Git tag: exactly `v` plus the built `versionName`, e.g. `v2.11.42-alpha.1`. Do not rename existing tags or assets.
- Public APK: `ZeekrCompanion-<versionName>.apk`, plus the matching `.apk.sha256`. Stage it locally with `python3 .github/scripts/package_release.py`; CI uses that same script and rejects a tag/version mismatch. Gradle's `app-release.apk` is an intermediate build output.
- Release title: `Zeekr Companion <versionName>`. Generate its body with `.github/scripts/release_notes.py`; retain dts88, FerixzTanz and EVCam credits.
- The tag workflow creates a draft with canonical asset names. Review validation/notes, then publish alpha/beta with Pre-release on and Latest off. Stable uses Pre-release off; beta/stable require the owner's explicit release-channel decision. Alpha remains the default **publication channel**; this is independent of the in-app Update channel preference.
- The updater reads this fork's releases and requires consistent pre-release flags/tags. Its Update channel setting permits Stable only, Beta plus Stable, or Alpha plus Beta plus Stable. Fresh installs start on Stable; upgrades preserve the legacy beta choice, and Alpha is explicit opt-in. Unknown channels stay excluded. Historical fork tags/installed personal versions remain comparable. Upstream contribution branches retain upstream branding and release rules.
