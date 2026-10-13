# Fork workflow

The remotes are `origin` (wysie's fork) and `upstream` (dts88's repository).

| Branch | Purpose |
| --- | --- |
| `main` / `upstream-main` | Unmodified upstream code; update by fast-forward only |
| `feature/turn-signal-side-view` | One complete side-view feature: PR #4 port, temporary mirror view, Replace Super mirror option, independent popup and pinch resizing |
| `feature/camera-labels` | Optional camera labels, developed separately from side-view display modes |
| `personal-build` | Combined installable fork containing both implemented features, fork version numbers, update source and build configuration |
| `pr4-followup` | Credited side-view contribution for the existing upstream PR #4, preserving the original author's commit; excludes labels and personal build settings |
| `research/blind-spot-alerts` | Alert-signal investigation; no active obstacle trigger |
| `research/factory-360-view` | Investigation of access to the factory stitched view |

All behavior belonging to turn-signal side view lives on one feature branch. Camera
labels remain a separate feature. The label branch starts from the original side-view
contribution base because it labels both floating views; its additional commits contain
only label changes. Merge finished features into `personal-build` and build that branch
when an APK containing all of them is wanted. Research notes do not activate features.

The former combined `side-view` branch and separate `feature/side-view-display-mode`
branch have been consolidated into this layout. Their history is retained in
`personal-build`; the existing 2.11.5-sideview-alpha APK still contains both features.

For PR #4, use `pr4-followup` rather than creating a competing PR. The original
FerixzTanz commit and authorship are retained there, with attribution in `NOTICE.md`.
The author/maintainer can incorporate the follow-up into the existing PR. Other
independent contributions can use their own feature branches, without fork build settings.

For new independent work:

```sh
git fetch upstream
git switch main
git merge --ff-only upstream/main
git switch -c feature/my-change
# Implement and test the change, then commit and push the feature branch.
```

A dependent feature can start from its prerequisite feature branch. Keep personal
version numbers and updater configuration on `personal-build`. For a combined build:

```sh
git switch personal-build
git merge --no-ff feature/turn-signal-side-view
git merge --no-ff feature/camera-labels
source ~/.config/android-dev/env.sh
bash ./gradlew :app:testDebugUnitTest assembleRelease
```

The build workflow runs on pushes to `personal-build`; its APK artifact is
`ZeekrShortcut-apk`. Increment `versionCode` and update `versionName` before each new
installable release. The existing signing key preserves update compatibility.

Side view is off by default. Replace Super mirror is on by default and temporarily
uses the existing mirror canvas. With it off, an independent side popup shares the
camera stream and supports pinch resizing. A rejected side output yields before the
mirror and recording. Both side cameras share one saved popup location; drag it to position
and pinch to resize. The owner reports labels and mirror replacement working on the 009;
the shared placement and drag changes still need an on-car check.

Corrected side views automatically narrow the requested FOV when high rear/up
aiming would cross the camera image edge. Aim and saved settings are retained.
At 80° rear / 15° up, a requested 90° square view fits at approximately 20.5°;
this avoids collapsed black tiles but greatly reduces visible coverage.
