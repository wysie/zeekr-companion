# Fork workflow

The remotes are `origin` (wysie's fork) and `upstream` (dts88's repository).

| Branch | Purpose |
| --- | --- |
| `main` | Unmodified upstream `main`; update by fast-forward only |
| `side-view` | Installable fork containing PR #4, our improvements, personal version numbers, update source and build configuration |
| `upstream-main` | Unmodified upstream `main`; update by fast-forward only |
| `feature/turn-signal-side-view` | Contribution branch based directly on upstream, containing the PR #4 port and temporary Super mirror side view |
| `feature/camera-labels` | Camera-label toggle and floating direction labels, based on the side-view contribution branch |
| `feature/side-view-display-mode` | Replace-or-separate display choice, independent side output and pinch resizing; based on camera-labels |
| `research/blind-spot-alerts` | Read-only investigation and implementation plan; no active blind-spot trigger |

Submit upstream PRs from a clean `feature/...` branch. The existing contribution branch deliberately
excludes the fork's version number, update owner, workflow changes and this document. Those stay on `side-view`. The original
PR #4 contributor is credited in `NOTICE.md`. The branch contains two focused commits: the port to
the current scheduler, followed by temporary side viewing in the existing mirror window.

To submit that branch when ready:

```sh
gh pr create --repo dts88/zeekr-shortcut-car \
  --base main --head wysie:feature/turn-signal-side-view
```

For a new independent contribution:

```sh
git fetch upstream
git switch upstream-main
git merge --ff-only upstream/main
git switch -c feature/my-change
# Implement and test the change, then commit it.
git push -u origin feature/my-change
```

A feature that depends on the side-view work can start from `feature/turn-signal-side-view` instead.
Keep personal version and update settings on `side-view`, then merge completed feature branches into it.
Keep `main` free of personal features. The previous integrated `main` history is retained on `side-view`;
future upstream syncs should fast-forward `main` rather than merge personal feature branches into it.

Local Java and Android environment:

```sh
source ~/.config/android-dev/env.sh
bash ./gradlew :app:testDebugUnitTest assembleRelease
```

The side-view feature remains off by default. With Replace Super mirror enabled (default), it temporarily
shows the corresponding side camera in the same window, then restores the normal mirror after the
configured close delay. With Super mirror off, the separate pop-up is used. No new camera output
is added when the existing mirror hosts the side view. With Replace Super mirror off, a separate
side output shares the camera stream with the mirror and main preview; the independent window
supports pinch resizing. If its output is rejected, the side window closes and the session retries
without it. Concurrent windows and resize gestures still need validation on the 009. Car testing of this interaction remains
necessary; unit tests and APK compilation do not exercise the head unit's window manager.

The camera-label branch adds the label implementation and a Malay wording correction on top
of the side-view contribution branch. Once the side-view dependency is upstream, these commits
can be cherry-picked onto upstream
for a separate PR. The blind-spot research stays independent; see
[the investigation](docs/blind-spot-side-view.md) for the missing 009 signal information.
