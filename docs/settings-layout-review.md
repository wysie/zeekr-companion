# Settings layout review — 2026-10-15

The owner reported the Surround switch being partly hidden by its camera name.
A native API 32 tablet emulator at 2560×1600 reproduced the same clipping. This
pass checks shared layout patterns rather than identifying an unseen screenshot
from memory. The unphotographed misaligned button cannot be identified with certainty.

| Finding | Change |
| --- | --- |
| Long camera names/custom badges could consume space needed by the switch | Reserve a full switch touch target independently; wrap the name/badge in a weighted column and align switches at the top |
| Quality-preset name and “current” badge could crowd each other | Put the badge below the full-width title |
| Three cards stayed squeezed into a row in narrower panes | Use a responsive row that stacks cards when needed, with equal row heights and correctly reset margins on reflow |
| Shared option controls used fixed-height/single-line or unconstrained trailing layouts | Put options below their explanation; wrap labels, grow to equal heights, retain at least 48 dp touch targets and disabled state |
| Long Diagnostics action labels could outgrow fixed 52 dp buttons | Allow wrapping and equal-height growth with a 52 dp minimum |
| The main action rail could push Settings/Minimise below a short window | Make the existing action column scrollable when necessary; retain its side-setting hook and wide-window layout |
| Changing category from the profile editor left its title/Back history attached | Clear nested detail history before showing the new category |
| Fresh installs requested an undeclared, unused microphone permission and closed when it could not be granted | Remove the stale startup request and unavailable Developer microphone row; no microphone permission is added |

The microphone correction is isolated on `fix/unused-microphone-permission` and
included in `fix/settings-layout-overflow`. Existing upstream PR branches are
unchanged. Saved camera enablement and recording/profile policy are not reset.
No UI fix adds vehicle controls or changes camera processing.

## Verification

- Native Android screenshots/hierarchy inspection at 2560×1600 and 1920×1200,
  density 320, reproduced the clipped switch and checked reflow/short-window reachability.
- Three real Android instrumentation tests pass at a 1.3 font scale: full switch
  bounds beside a long camera name + custom badge; wide → narrow → wide card
  reflow without retained margins; multi-line option alignment, click delivery
  and disabled-state propagation.
- MainActivity retains its existing 1.2 font override. Changing the emulator's
  system font alone is not evidence that the main screen uses 1.3. The native
  measurement tests use independent 1.3-scale contexts explicitly.
- JVM regression checks, signed release assembly and vital release lint are run
  on the combined build. Emulator layout/interaction checks do not establish
  vehicle camera, HUD, USB or driving behavior.

The native tests can be run on a disposable emulator:

```sh
source ~/.config/android-dev/env.sh
ANDROID_SERIAL=emulator-5554 bash ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.kooo.evcam.settings.SettingsLayoutTest
```

Gradle's connected-test runner installs/removes its target app, so these tests are
for a disposable emulator with no owner footage or settings. They do not require
ADB access to the 009.

## Owner follow-up

Confirm the Surround switch is completely visible and tappable, check custom
badges and long option labels, and verify the category title/Back button after
leaving the editor. If the other remembered button remains misplaced, its page
name is enough to target the next check; a screenshot would help but is not
required to use these shared fixes.
