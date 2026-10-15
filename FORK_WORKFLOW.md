# Fork workflow

The remotes are `origin` (wysie's fork) and `upstream` (dts88's repository).

| Branch | Purpose |
| --- | --- |
| `main` | Combined installable fork: completed features, fork version numbers, update source and build configuration |
| `upstream-main` | Unmodified dts88 code; tracks `upstream/main` and updates by fast-forward only |
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
| `feature/009-hud-display` | Opt-in 009 HUD mapping for display 3 at 1280×640, based on the owner's Diagnostics; physical destination and video output await testing |
| `fix/profile-camera-selection` | Honor individual enabled channels, cabin-only previews and recording; reject unapplied channel changes with a restart instruction |
| `fix/automatic-recording-permission` | Recording request origin, current-setting checks and opt-in driver-start defaults; existing PR heads preserved |
| `feature/side-view-mirroring` | Fork side-camera horizontal flip, including mirror replacement; existing PR heads preserved |
| `integration/upstream-about` | Upstream 2.11.4 integration and fork-only About attribution; existing PR heads preserved |
| `review/project-safety` | Whole-project audit and recording, storage, sharing, update, privacy and lifecycle corrections; based on combined main |
| `feature/diy-360-safety` | Camera safety review fixes: parked-only 360, readback validation and shared stale-frame guards; based on main |
| `feature/diy-360-view` | Fork prototype of live ground-plane stitching, with independent manual calibration; based on main |
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
`ZeekrShortcut-apk`. Increment `versionCode` and update `versionName` before each new
installable release. The existing signing key preserves update compatibility.

Fetch dts88's updates into `upstream/main`, fast-forward `upstream-main`, and then
merge `upstream-main` into `main`. Resolve conflicts while retaining the fork's
features and build configuration. Push each branch to `origin` explicitly; never
push the combined fork to the `upstream` remote. Keep `main` buildable and use
release tags to identify the exact source of downloadable APKs. Existing
`personal-build-…` release tags and downloads remain valid.

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

Corrected side views automatically narrow the requested FOV when high rear/up
aiming would cross the camera image edge. Aim and saved settings are retained.
At 80° rear / 15° up, a requested 90° square view fits at approximately 20.5°;
this avoids collapsed black tiles but greatly reduces visible coverage.

## Ferix feature ports

The seven port branches above are integrated into `main`. Independent ports start
from `upstream/main`; arrows depend on `feature/turn-signal-side-view`. Existing
PR #17 and #19 branches are unchanged. Source authorship and original commit links
are recorded in port commits and `NOTICE.md`. See `docs/ferix-feature-ports.md`.
New combined prereleases use `fork-…` tags pointing to `main`; older release tags
and APK download URLs remain valid.
