# Native UI review — 2026-10-19

The owner requested Astra review and accepted the shorter main Straighten label. Two isolated, read-only assessments reviewed the current native Android source and emulator evidence: GPT-6 Astra (high reasoning) for design/interaction, and an independent native accessibility/implementation audit. Astra completed its baseline assessment before receiving the other audit findings. No browser/HTML/CSS detector applies to this Android Views app.

## Verdict

Retain the camera-first composition, linked Companion mark, semantic grey/orange day/night palette, native type roles and settings categories. The reviewers found a coherent foundation; the gaps below warranted a bounded interaction/accessibility pass. No new visual direction or camera pipeline change was justified. Astra approved the final native screenshots/source: all six design findings resolved, no further material design change requested.

## Findings and disposition

| Baseline finding | Implemented correction |
| --- | --- |
| Playback Home only finishes the viewer and reveals Gallery | Explicit return to MainActivity camera screen; ordinary Back still returns to Gallery; internal-return intent preserves manual-stop state if recreated |
| Source restart warning below initial Preview viewport | Fixed heading explains Quit app → reopen and that quitting stops recording; detailed recording/layout help remains scrollable |
| Empty playback describes surround-only media and leaves controls usable-looking | Source-neutral persistent storage guidance; disabled and visually subdued media actions, retained Refresh/Home/Menu |
| Windows state labels fail to describe the next tap, especially Paused | Explicit Open / Close / Turn off action with separate Open / Closed / Paused state |
| Settings icon reads as brightness | Cog geometry with incumbent icon stroke/tint |
| Driver-side setting describes the retired side rail | Driver side wording and spatial Left / Right options, same key/default/saved choice |
| Main replaces system font scale with 1.2 | Preserve the existing minimum while respecting larger system text; keep Activity resources local |
| Segments communicate selection visually only | Radio/checkable/checked accessibility semantics |
| Gallery video identity omits date/time and lock/selection detail | Grouped accessible video identity and lock/selection state, without lengthening thumbnail badges |
| Selected small text is white on orange (~3:1) | Tonal selection surface, semantic orange ink; dark contrast 6.00:1 and light 4.93:1 after a bounded light-ink adjustment |

The accepted main label is now **Straighten**. Native long-press help explains preview/playback correction and unchanged recorded files. The switch and saved setting remain shared as before.

Honoring real font scale 1.3 exposed mid-word wrapping in the previously fixed-width Preview panel. The panel now starts at 600dp at that scale, grows for larger text and stacks choices on constrained hosts. Heading spacing was tightened without reducing type or touch targets. Source-layout names remain initially visible in the verified large-screen configuration.

## Evidence and validation

Local, ignored captures live under `.impeccable/review/astra-20261019/`: baseline settings and final main day/night, Preview, Windows, Interface, About/Quit typography and actual empty video playback. Captures are native emulator output, not design mockups. The emulator is API 32 ARM64 with effective 2560×1600 display, density 320 and system font scale 1.3. Black camera panes are expected without Zeekr hardware. Playback captures use the real separate Activity; the old capture-only fixture replaced MainActivity content and was corrected rather than treated as a production crash.

Existing checks cover retained texture/pipeline identity, preview staging and source-layout geometry, settings semantics and resets, text/header recycling, day/night and long translations. The new navigation regression takes the actual Gallery → photo/video viewer → Home path, checks settings hidden/cameras visible, Gallery destroyed and manual-stop intent retained. Its initial ActivityScenario fixture used a launcher intent which stopped matching after onNewIntent; the fixture now uses the same plain explicit-intent shape as production. The exception was corrected, not suppressed.

JVM suite: 1,494 tests, zero failures/errors. Debug, instrumentation and release builds plus lint pass. The native review batch initially passed 16 of 18 checks; the two exposed issues (Preview fit and launcher-intent fixture) were corrected and passed focused reruns. Additional real gallery/playback/segment confirmation passed all five tests. The final 600dp day/night Preview capture check passed; Astra confirmed complete words, visible layout names/actions and the corrected playback empty state. The companion validation note records the exact limits.

## Limits

This is an emulator/source UI verdict, not proof of on-car camera quality, 360° frame rate, HUD fit, factory-camera coexistence, 7X calibration or blind-spot warning triggers. Full TalkBack traversal and extreme font/window combinations need device validation. The intermittent owner About/Quit size report was not reproduced; the existing recycle/scroll regressions pass in both themes.
