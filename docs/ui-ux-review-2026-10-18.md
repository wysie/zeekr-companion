# UI and UX review for later work

Status: implementation completed with the owner’s authorization on 2026-10-18. Native checks and remaining physical/manual verification are recorded below. No new release is implied by this document.

Astra 6 reviewed the combined fork at `aaf267f3` / `v2.11.47-alpha.1`
(Android version code 330), using source and existing emulator captures. Preserve
the native grey/orange design, two-pane settings, large rows, native dialogs and
light/dark palettes. Improve operational clarity rather than replacing the menu.
The captures are supporting evidence from recent feature work, not new vehicle
validation of release 47; the About capture shows release 46.

## Open owner reports

- Release 47 can take a long time to show live camera images. The supplied photo
  shows Surround and a lower preview waiting while an upper road image and a
  recording badge are visible. It does not establish which sources were writing.
  Main preview and manual recording have no Park eligibility gate. Fresh
  diagnostics from the affected session are still needed; simultaneous camera
  contention and startup/recovery delays are hypotheses, not established causes.
- About & credits and Quit app sometimes appear smaller in the Settings sidebar.
  The current binder restores the common 18sp Body dimension, disables autosizing
  and allows two lines. Existing captures/checks do not reproduce the owner's
  intermittent case. Keep it open; capture version, theme, font scale/density and
  actual TextView sizes after scrolling/recreation before another speculative fix.
- Physical HUD clipping/factory-graphic overlap and the expanded 360 bottom gap
  over Android Auto still require owner checks. UI review cannot establish those
  vehicle-specific outcomes.

## Priorities

### P1 Camera and recording state

Make partial recording persistent and prominent: for example, **Recording 1 of 3
sources**, with a per-source breakdown. Count camera streams, not the four crops
within Surround. Distinguish recording activity from preview freshness; a covered
preview does not prove that recording stopped, and one healthy recorder does not
prove that all selected sources are recording.

Partial feedback already exists as a short startup toast and selected failure
hints in the rightmost, ellipsized footer. Improve its prominence and coverage
rather than inventing a second state authority. Relevant baseline locations:
`MainActivity.java` at `onStartSettled`, `cameraHint`, and the recording chip;
`camera/RecordingLanes.java` at `hint`; `layout_status_bar.xml`.

The common specification footer reads the `front` stream's settings in
`ui/StatusLine.java`, even with independent recording groups and per-camera
quality. Label that scope, summarize mixed settings, or move those details into
the per-source view.

### P1 Truthful waiting and recovery feedback

`zeekr/PreviewFrameGuard.java` currently covers missing/stale pictures with the
same Waiting for live camera text. Differentiate known starting, reconnecting,
factory-use, unassigned-source and failed states using existing pipeline facts.
Show elapsed waiting after a threshold; if the cause is unknown, say Still
waiting for live video. Surface the stopped-retrying state explicitly.

Offer Camera status / Save diagnostics after prolonged waiting. A Retry action
must use a supported, safe pipeline operation; do not merely remove the cover,
shorten timeouts blindly, interrupt healthy recording or guess a hardware cause.

### P1 Confirm broad resets and explain their scope

Blind-spot, Super mirror and 360 calibration resets act immediately in
`settings/SettingsPreferenceFragment.java`; source/profile resets already confirm.
Use the existing native confirmation pattern for resets that erase saved tuning,
with an explicit Cancel action and a clear list of affected settings.

Consider separate HUD picture and placement resets so restoring image defaults
does not erase a good HUD position. A position-only action can remain immediate
if it offers undo. Keep enabled state, unrelated preferences and saved values
outside each documented reset scope.

### P2 Consistent actions and navigation

`settings/PreferenceRows.java` gives every selectable plain Preference the
navigation-row layout, so immediate actions can display destination chevrons.
Use chevrons for pages/choices, switches for preferences or clearly labelled
current states, and action rows/buttons for Reset, Export and Check now.

Rename the sidebar destination to Updates and retain Check for updates inside.
Keep existing drawer switches. The 360 popup switch intentionally describes a
live session, unlike persistent enable switches; show Open now / Closed and keep
its lifetime clear. USB logging should present Start logging / Stop logging with
the existing live status rather than imply automatic startup.

### P2 Camera setup order and pending changes

Make the setup order Vehicle model → Camera sources → Recording sources & quality
→ Preview layout. Add a vehicle-model shortcut to camera setup without moving
the canonical System setting. Rename Edit stream profile to Recording sources &
quality; keep technical profile/preset selection available.

Show Saved—reopen to apply camera changes persistently when the running pipeline
has not adopted saved source/preview choices. If offering restart, explain that
it stops recording. Update help to point to the shared Camera sources page.
Preserve all keys, mappings, recording switches, preview selections and tuning.
The supplied 009 identifies as Pixel 3a and needs manual model selection; selecting
7X does not prove its road feeds, concurrency, HUD routing or calibration.

### P2 HUD grouping and inherited settings

Retain follow/custom behavior and preserved custom values. Rename the child page
HUD and group it into Picture source, Custom framing, Position & size, Brightness
& contrast, and Advanced. Position/size/tone remain independent of following the
screen picture. Show inherited values or an Edit screen picture shortcut.

Shorten the initial explanation; distinguish picture inheritance from
indicator-side following. Correct the night-brightness description that refers
to opacity above when its row is below. These changes cannot promise to override
independent factory HUD graphics.

### P2 Floating control inside the app

Existing Interface/HUD/About captures show the floating recording button covering
settings text. Define an app-foreground policy: temporarily park it in reserved
space or hide it while an accessible recording stop action is already present.
Restore its saved external position afterward; do not overwrite that placement
or shrink settings around an unconstrained overlay.

### P2 Task based troubleshooting

Add a Troubleshooting section/destination within System, linking ordinary Camera
status / Save diagnostics, explicit USB troubleshooting-session logging, and
Developer-only camera/display research. Keep export completion, file location and
Send to phone available in a persistent result. Do not automatically run camera
probes or interrupt recording. Keep discovered IDs, model defaults, assignments
and physically tested feeds distinct.

### P3 Bounded layout and copy polish

- Give EVCam/GPL About links the same minimum 48dp touch targets as adjacent links.
- If no browser exists, offer a persistent URL with Copy link instead of only a toast.
- Remove release/debug-history prose such as Font sizes stay the same from ordinary
  settings; explain user-visible consequences instead of encoder implementation.
- Check narrow trailing-value rows before changing them: long summaries can crowd
  the weighted title. Stack values below titles if actual rendering confirms it.
- Include waiting-state accessibility announcements in a native TalkBack pass;
  Canvas text alone does not expose the complete camera state.

## Suggested menu arrangement

Retain the existing top-level categories.

| Category | Suggested order |
| --- | --- |
| Recording & photos | Vehicle shortcut; Camera sources; Recording sources & quality; Preview layout; Automatic recording; Screen off; Photos; Stamps & driving information |
| Storage | Destination; Capacity; Protected footage |
| Super mirror | Enable; Camera/controls; Picture; Window; Reset |
| Blind spot camera | Enable; Output; Activation/timing; Screen picture; Popup window; HUD; Reset |
| 360° camera | Experimental status; Open/display destination; Projection; Quality/illustration; Window; Calibration/reset |
| Floating button | Enable; Tap/double-tap/hold; Appearance; Placement |
| Interface | Language; Theme; Action side; Readouts/motion; Preview & playback |
| System | Vehicle model/details; Startup; Troubleshooting; Vehicle information; Developer mode |
| Developer options | Recording internals; Rendering; Research; Maintenance |
| Updates | Installed version; Channel; Check for updates |
| About & credits | Direct destination; preserve wysie/dts88/Ferix/EVCam attribution |
| Quit app | Explicit action, visually distinct from destinations |

## Implementation batches when resumed

1. **Camera truth and recovery:** persistent live/planned recording count,
   per-source details, truthful waiting/failure states and correctly scoped
   specification summaries. Test partial startup/loss/recovery, all unavailable,
   return from background and disagreements between preview and recording.
2. **Settings semantics and setup:** action-row style, reset confirmations/scopes,
   camera setup terminology/order, persistent pending-restart notice and Updates
   naming. Preserve preferences and effective defaults.
3. **HUD and troubleshooting:** grouped inherited/custom controls, concise help,
   diagnostic links/results and foreground floating-button policy.
4. **Native verification and polish:** light/dark, normal/larger fonts,
   narrow/short windows, long translations, TalkBack, sidebar recycling and
   popup controls/restore. Then owner checks for startup timing, simultaneous
   recording, physical HUD coexistence and Android Auto viewport.

Use bounded review passes and fix evidence-backed issues. Keep camera startup
diagnosis separate from cosmetic changes. Do not mark any item complete without
checking its implementation and validation; saving this backlog authorizes no
new app feature or release.

## Resumed implementation

Camera truth/recovery batch: implemented a persistent recorder live/planned count and ordinary Camera status page, per-source recording/producer facts and saved-quality scope, differentiated known waiting/recovery/stopped states, elapsed prolonged waiting, accessible state announcements and diagnostics access. The black stale-image cover and scheduler behavior are retained. Native API 32 tests verify partial/loss/recovery count presentation, read-only status/recreation and stale-cover/live-preview accessibility. JVM/debug/test/release assembly and release lint pass. Physical camera startup, HUD/factory coexistence and Android Auto remain owner checks. Other batches below remain in progress.

Settings semantics/setup batch: confirmed scoped resets for blind-spot, Super mirror, 360 calibration/window and floating-button layout, plus separate HUD picture/placement resets; action rows omit navigation chevrons. Camera setup order, Recording sources & quality naming, persistent pending-source/preview banner, Updates destination, explicit USB logging actions and live 360 window status are implemented. Native tests verify Cancel/confirm, HUD reset isolation, setup/action semantics, pending notice/adoption after reopening the preview, existing 360 illustration/calibration behavior and all seven USB logging regressions on the explicitly provisioned disposable emulator USB volume. No owner settings are reset by merely opening a page. Remaining HUD/troubleshooting/foreground-control/polish items are still in progress.

HUD/troubleshooting batch: implemented picture-source/custom-framing/placement/tone/advanced groups, an inherited screen-picture shortcut and shorter inheritance/night-brightness copy. System troubleshooting provides read-only camera status, diagnostics, explicit USB session controls and developer-only camera/display research. Export results retain saved paths/errors with Copy location and Send saved report; missing files disable sharing. Supported app screens reserve an accessible recording-stop/storage-wait-cancel control and temporarily hide the external floating button, restoring its saved placement afterward. About links have 48dp targets and a copyable no-browser fallback. Nine native API 32 tests passed across the new troubleshooting/export/link flows, existing HUD inheritance and actual floating-button gesture/hide/restore adapters. Translation/layout/accessibility verification is the final batch; physical HUD/factory overlap remains an owner check.

Final polish: native measurement reproduced a zero-width title with a long storage value at 280dp. Value rows now stack title/value only when either would clip; ordinary wide rows retain trailing values. English, Chinese and Malay checks cover 280/500/800dp, 1.0/1.3 font scales and both palettes. Inspected actual Interface light/dark and HUD follow/custom screenshots plus narrow translated row renders. Idle camera status is neutral; recording colour follows actual live sources. New text is localized in all supported languages.

Validation: 1,493 JVM tests, debug/test/release assembly, release lint and full debug lint passed. The final 13-test native batch passed for font isolation, sidebar recycling, themes, value rows, HUD inheritance, troubleshooting/export persistence and camera waiting accessibility. After the final value-alignment adjustment, the value-row regression passed again. Earlier batches separately passed scoped reset/pending-source adoption, seven USB logging tests using a disposable emulator USB volume, and the actual floating-button hide/restore/gesture adapters. Upstream contribution validation is separate and recorded in the outgoing index.

Remaining verification: the owner's intermittent font case is not reproduced; retain it as an observation to capture rather than claiming a hardware fix. Camera startup timing/concurrent sources, physical HUD clipping/factory-layer overlap and Android Auto popup bounds still require the car. Accessibility tests confirm exposed state descriptions and polite live regions; they do not substitute for a manual TalkBack listening pass. Aim back and other saved framing values were not retuned. No new guessed blind-spot trigger or factory-HUD override was introduced.
