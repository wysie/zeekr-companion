# Fork workflow

The remotes are `origin` (wysie's fork) and `upstream` (dts88's repository).

| Branch | Purpose |
| --- | --- |
| `main` | Installable fork, with personal version numbers, update source and build configuration |
| `upstream-main` | Unmodified upstream `main`; update by fast-forward only |
| `feature/turn-signal-side-view` | Contribution branch based directly on upstream, containing the PR #4 port and temporary Super mirror side view |

Submit upstream PRs from a clean `feature/...` branch. The existing contribution branch deliberately
excludes the fork's version number, update owner, workflow changes and this document. The original
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
Keep personal version and update settings on `main`, then merge completed feature branches into it.
Do not reset or force-push `main` to upstream; it contains the installable fork's history.

Local Java and Android environment:

```sh
source ~/.config/android-dev/env.sh
bash ./gradlew :app:testDebugUnitTest assembleRelease
```

The side-view feature remains off by default. When enabled alongside Super mirror, it temporarily
shows the corresponding side camera in the same window, then restores the normal mirror after the
configured close delay. With Super mirror off, the separate pop-up is used. No new camera output
is added when the existing mirror hosts the side view. Car testing of this interaction remains
necessary; unit tests and APK compilation do not exercise the head unit's window manager.
