# Zeekr Companion

<img src="docs/branding/companion-icon.svg" alt="Zeekr Companion logo" width="96" height="96">

Camera recording, live camera windows and vehicle utilities for ZEEKR head units.

**Zeekr Companion** is maintained by [wysie](https://github.com/wysie/zeekr-shortcut-car)
and is a fork of [Zeekr Shortcut by dts88](https://github.com/dts88/zeekr-shortcut-car).
I primarily use and test it on my ZEEKR 009. Credit for the original Zeekr application goes
to dts88 and its contributors; the underlying EVCam project is by
[suyunkai and contributors](https://github.com/suyunkai/EVCam). Full attribution
and license details are in [NOTICE.md](NOTICE.md).

Special thanks to [FerixzTanz (Felix Tan)](https://github.com/FerixzTanz) for
[the original turn-signal side-camera implementation](https://github.com/dts88/zeekr-shortcut-car/pull/4)
and the features ported from his [CAMCORDER project](https://github.com/FerixzTanz/CAMCORDER).
Our side-camera work builds on his contribution; phone sharing, signal scanning,
hard-braking locks, clip export, driver recording, turn arrows and UI refinements
also include his project's work. Original authorship and source links are retained
in [NOTICE.md](NOTICE.md).

For the original project and its maintainer's builds, use
[dts88's repository and releases](https://github.com/dts88/zeekr-shortcut-car/releases).
dts88’s original version is the more thoroughly tested option. This custom fork
by wysie is primarily for personal use and adds experimental features; its vehicle
testing is more limited. Neither project is an official ZEEKR product.

The in-app About page introduces Zeekr Companion in my own voice, thanks dts88,
Ferix and the EVCam contributors up front, and links to their projects.
Credits links include dts88 and Ferix beside their acknowledgements; Projects and
downloads lists Zeekr Companion, dts88's original project, then Ferix's CAMCORDER.
Original community acknowledgements are retained and attributed to dts88’s project. Its
optional PayNow tip, **9271 6786**, goes to **dts88, the original author**.

Zeekr Companion was previously distributed as **Zeekr Shortcut — wysie Edition**.
The app now uses the Companion name and linked-companions logo in every language,
with matching Android themed and notification icons. [Vector artwork and colours](docs/branding/README.md) are included.
Downloads use `ZeekrCompanion-<version>.apk`. The Android package ID remains
`io.github.dts88.zeekrshortcut`, with the existing signing certificate and preferences
for in-place updates. New public files use `ZeekrCompanion`; older
`ZeekrShortcutApp` and EVCam folders remain readable without moving their files. The repository and update
source remain [wysie/zeekr-shortcut-car](https://github.com/wysie/zeekr-shortcut-car);
existing download links remain valid.

The interface supports **English, Bahasa Melayu and Simplified Chinese**, including
About & credits, the five Super mirror help pages and the installer reminder.
Help uses theme-aware, scrollable native text, so larger text and accessibility
services can read it. Malay terminology is consistent across settings, status and
floating controls; Simplified Chinese has one canonical default resource set.
Translation checks cover every text resource file, plural, array and format argument.

Update notes follow the selected app language when an authored translation is
available. English remains the original release text; older English-only notes are
explicitly marked. Release preparation requires matching entries in
`CHANGELOG.md`, `CHANGELOG.ms.md` and `CHANGELOG.zh-CN.md` for new versions.
Diagnostic logs, technical identifiers, third-party installer text and named credits
retain their original wording. The phone-download web page retains its bilingual
English/Chinese instructions.

The app-name watermark on new recordings and normal photos uses **Zeekr Companion**
plus the version; existing footage keeps its original burned-in text. The logo itself
is not added to the footage. Raw developer exports retain their existing behavior.

Current test download: [Zeekr Companion 2.11.62-alpha.1](https://github.com/wysie/zeekr-shortcut-car/releases/tag/v2.11.62-alpha.1).

Alpha 61 includes the completed translations and audit corrections. It suspends GPU presentation for hidden/docked/retained
camera views while continuing to drain their inputs, removes recording's normal
codec-output wait, and timestamps the frame actually acquired. Transparent standby
side windows still warm up normally. Camera settings load vendor capabilities off
the UI thread, and quitting during Apply clears its transaction without resuming
recording. Confirmation recreation/cancellation and nested category navigation
also retain usable controls. Open recordings are protected from cleanup/deletion independently of
filename ordering and the footage-lock switch; event/manual locks are rechecked
at deletion. Settings import also rejects wrong types for known unset keys.
No new switch is required. These changes do not establish real-car smoothness or
storage durability; see the [codebase audit and validation](docs/codebase-audit-20261021.md).

Alpha 62 storage-stall fixes separate recorder file ownership from USB directory
scans, persistent lock metadata and slow deletion. Cleanup inventories files outside
the lock-edit guard, then rechecks current locks and active writers before unlinking;
failed or newly protected candidates trigger bounded replanning. A deletion reserves
only its own path, so unrelated recorder close/open operations remain responsive.
The encoded write queue can now absorb up to **six seconds** of media, subject to
the existing **24 MiB queue** and **64 MiB total retained-data cap per recorder**.
Periodic and final flushes, the shared stop deadline and your saved recording quality
are retained. The delayed event-lock pass allows for the longer write backlog.
Sustained slow/disconnected storage can still cause frame dropping or bounded stop/
rescue; these fixes cannot make a USB device flush faster. See the
[regression evidence and remaining limits](docs/storage-contention-20261022.md).

In alpha 60, the experimental 009 fit combines the earlier garage reference with matched
flat-road features from the owner’s 21 October raw/factory captures. Held-out
image-reference alignment improves for all four cameras, including the front;
CPU/GPU share the correction. Existing Automatic/009 selection uses the revised
fit without a reset; saved Manual tuning, sources and framing stay intact.
Residual joins/raised-object distortion remain, and on-car confirmation is pending.
See the [alignment evidence and limits](docs/009-image-fit.md#roadground-alignment-refinement--21-october).
For in-app delivery, select **Updates → Update channel → Alpha**; Beta and Stable
channels deliberately exclude this release. It retains the application ID and
signing certificate for an in-place update. New behavior still needs on-car testing.

The main screen gives the camera stage the full available width, with a small
Companion logo/name, **Preview** selector and direct **360° view** action above it.
Camera status, elapsed recording statistics (when enabled) and storage information
sit outside the camera picture. Camera status is a compact text action with a
48dp touch target and native feedback; live recording counts remain visible. The bottom dock is a flat toolbar with recording, Take photo,
Gallery, Windows and contextual **Straighten** actions. A single divider
separates it from the picture; individual controls have transparent backgrounds
and native press/focus feedback. Recording keeps its red indicator and progress;
Straighten uses orange text plus a checkmark when enabled; long-press explains that
it affects preview/playback and leaves recorded files unchanged. These toolbar
refinements ship in alpha 52. Dock actions keep
normal-size wrapped labels; the saved driver-side preference puts recording on
that side. The current toolbar refinement gives the visible utility actions equal widths across the remaining space beside the driver-side recording control, with consistent 8dp gaps and a thin separator. Hiding Straighten redistributes that space; narrower hosts retain readable labels and horizontal scrolling. Recording stays focused on its action and clip detail. Long-press recording opens **Recording
details**, including live screen-off behavior. Settings and Minimise stay in the
header. Minimise uses a standard horizontal minus icon: tap hides the app to the
background; long-press quits. Quit app also remains in the menu.

**Gallery** now opens the Videos tab directly; switch to Photos in the same
gallery. There is no intermediate media chooser. Take photo and Start/Stop
recording remain direct actions. In expanded preview, Restore brings this toolbar
back. Main and hosted settings now use the system text size without the previous
forced 1.2× minimum; larger accessibility text still scales normally. Settings use
64dp minimum rows with 10dp vertical padding, retaining 16sp sidebar labels and
growing rows for long text. Smaller secondary header/footer text leaves more room
for cameras and settings. Waiting messages wrap and centre within each camera
pane without cutting off their beginning. Preview states now use brief labels such
as Camera busy or Waiting for preview, with elapsed time and a Camera status hint
after a longer wait. Full troubleshooting explanations remain in Camera status;
preview waiting still does not establish whether recording is working. These
refinements ship in alpha 53.

**Preview** opens a staged side panel: quick presets for **Selected recording sources**,
**Surround · four views**, **Front + Rear** and **Surround + Front + Rear**, in that order.
The four presets use full-width radio rows; **Other sources** offers one road source
or cabin. These use the same saved source choice as Settings;
preview changes never edit recording groups, source mappings or quality. Cancel,
Back and outside dismissal do not save. A slim **Camera changes pending · Apply changes**
banner appears above the main preview and beneath the Settings title. The selector
continues to name the applied preview; the banner names the saved next choice.
Equivalent source sets and source arrangements update without a pipeline rebuild.

**Apply changes** rebuilds cameras inside the running app. During recording, confirm
“Finish this clip, restart cameras and resume recording?” The current clip is saved,
the scheduler closes the old cameras, and the pipeline and preview views are rebuilt.
Android restores the open Settings category/editor. Recording resumes only for an
existing recording/request, keeping its manual/automatic origin; Stop during Apply
cancels that resume. Idle Apply stays idle. Factory priority still governs camera
availability. Failed cleanup/setup leaves a **Retry** action and recording stopped;
it does not claim that an unavailable vehicle feed is working.

Apply covers vehicle/camera mappings, recording groups, preview source sets, native
preview/JPEG sizes, JPEG output enablement and GPU preview/360°/blind-side/mirror
routing. Saved changes made during Apply remain pending for the next Apply. Theme
changes and UI recreation keep using the applied source set. Encoding FPS, bitrate,
codec and recording size apply to the next recording; visual geometry, theme and
arrangement controls keep their existing live behavior. Pending preview-only edits
do not block recording with the current recording sources. Structural recording
edits must be applied before starting with the new selection. Diagnostics distinguish
these states. See [Apply verification and limits](docs/apply-camera-changes.md).
All existing source choices are preserved on upgrade. **Source layout** arranges
separate source panes. With just Surround (or another single source), the panel
explains this and hides inactive arrangement choices; it does not rearrange the
four views inside Surround. The saved arrangement is retained for multi-source use.
These further clarity/toolbar changes ship in alpha 53.

Settings now use the owner-approved factory-inspired styling: regular-weight labels,
flat near-white/dark rows, small corners, neutral sidebar selection and orange outlines
for selected segments. All sidebar entries, including About and Quit, share one
16sp style and respect system text scaling. **Blind spot camera** has **Behavior ·
Picture · Window · HUD** tabs; changing tabs never resets settings. Picture has a
Left/Right live settings preview with Expand/Restore and Hide/Show controls.
It follows the Blind-side camera rendering choice under **System → GPU image processing**.
GPU uses a second display surface from the existing raw GL input, with the same per-pixel
side-camera shader and no bitmap readback or extra camera output. Compatibility samples
the existing main surround texture at up to five samples per second, retaining native
detail for known 1280×5140, 5120×1280 and 3840×2160 feeds before correction, including
when expanded. One reused compatibility sample is bounded to 32 MiB / 8192 pixels per
edge. The live popup/HUD frame-rate policy and camera source mappings are unchanged.
Choose a main preview that contains Surround and tap Apply changes if needed. The preview
pauses for factory camera use, screen-off, lost vehicle connection or stale frames;
in Custom mode with GPU surround correction but compatibility side rendering, enable
Blind-side camera GPU rendering or disable Surround preview correction and apply to
provide the raw source needed for tuning. Native camera surfaces stay attached behind opaque
settings; hidden main fisheye/360° rendering is paused. Tuning is available with the
automatic popup off and applies on slider release; recordings remain unchanged.
Long explanations remain under **About picture/behavior settings**. See
[factory-style UI behavior and validation](docs/factory-style-ui.md).


**Interface → Driver side** keeps recording nearest the selected Left or Right side,
retaining the saved preference. Larger system text is respected above the existing
1.2× main-screen minimum; settings options expose their checked state and use
higher-contrast selection colours. **Windows** names the next action (Open, Close,
or Turn off) alongside its current state. Playback Home returns directly to live
cameras, while Android Back returns to the gallery. An empty video library shows
storage guidance and disables media-dependent actions; Refresh and navigation stay
available. Gallery accessibility labels include video date/time, duration, lock and
selection state. These refinements ship in alpha 52 and need an on-car check.

The independent **Source layout** preference defaults to **Automatic** (the current
large-surround-plus-smaller-sources arrangement). **Side by side** gives the surround
and other-source groups equal width; **Stacked** puts those groups above/below one
another and arranges the other sources alongside each other. With no surround,
Side by side places the road/cabin panes horizontally and Stacked places them
vertically; Automatic retains their original vertical arrangement. These are source-pane layouts;
Surround keeps its existing four-view grid and saved per-lane framing. With one
source there is nothing to split. Layout changes apply on Save without removing
camera views, changing sources or restarting recording. Tapping a view still enlarges
it temporarily.

**Gallery** offers Videos and Photos. **Windows** shows independent Super mirror and
360° window controls with Open/Closed/Paused state. **360° view** changes the main
picture; the window action opens a separate manual session. **Straighten preview**
is shown for the applied surround preview outside 360° mode; it does not straighten
the recording. Expand preview hides header/dock while retaining camera views and
accessible recording/status/restore controls. Back restores the controls. Expanded
state and source layout survive Activity recreation; camera source, resolution,
framing and recording settings are not reset by the redesign.

The release linked above includes this main-screen redesign and the 360° CPU
renderer optimization. UI verification uses a native Android emulator;
real 009/7X feed presentation and factory-app coexistence still need on-car testing.
See the [main-screen verification notes](docs/main-screen-validation.md) for coverage and limits.

The combined fork includes upstream main through **76138b8d**, including the merged
[camera-label PR #17](https://github.com/dts88/zeekr-shortcut-car/pull/17), label wording/master-switch refinements and the 2.11.7-beta metadata. The camera framework includes cabin
camera recovery, serialized camera actions, scheduler-managed photos/exit and
camera step diagnostics. Planned cabin-camera closure now briefly closes the
surround camera first, then reopens it if still requested. Upstream reports about
0.5–1 second of surround interruption, a new recording segment and a brief mirror
interruption; timing on the 009 remains unverified. Diagnostics now include cabin-close
coupling, persisted camera history and capture-failure/buffer-loss details. `upstream-main` tracks that unmodified source; personal
release numbers remain independent of upstream.

**Alpha 62 camera lifecycle fixes:** camera surfaces remain owned until device
closure completes. If a recording session rejects Super mirror's optional output,
it stays withdrawn until you reopen/resume it, undock it, turn the screen on or
replace the camera pipeline; the watchdog no longer repeatedly adds it back.
Recovery following a consumer change is limited to cameras that consumer uses.
Blind popup, HUD and 360° output owners also receive configuration-failure
notifications. Saved camera/preview choices, GPU processing, recording quality and
normal window gestures remain unchanged. These are software regressions tested
in an emulator; on-car contention and timing still need checking.

## Additions in this fork

- **Settings backup (2.11.55-alpha.1):** **Settings → System → Settings backup** exports and restores Companion preferences, profiles, source/vehicle choices, GPU options, 360° calibration, illustration colours and window settings. Files are saved beside diagnostics and accept same-major Companion backups. Import confirms, stops recording and exits; reopening applies the backup before services start. Durable rollback protects restores across the five preference stores. Developer access, permissions, detected hardware and current recording intent stay local. Backups contain saved text such as plate stamps, but no footage or external artwork files. Diagnostics include the same full settings snapshot. See [restore behavior and verification](docs/settings-backup.md).
- **Manual recording lock (2.11.55-alpha.1):** Assign **Lock current recording** to Tap, Double tap or Long press under Floating button. With **Storage → Lock footage** enabled and recording active, it protects every source clip covering ten seconds before and after the press, including a delayed second pass for clip boundaries. Automatic flash/braking switches are not required; manual locks are outside the braking-only automatic budget. Existing gesture assignments stay unchanged.
- **Shared GPU image processing (2.11.55-alpha.1):** **Settings → System → GPU image processing (experimental)** offers **Off / All / Custom** for Surround preview correction, 360° stitching, Blind-side camera (popup/HUD/Super mirror replacement), normal Super mirror and video playback correction. All/Off show effective states with individual controls disabled; Custom enables the five switches. Custom choices survive All/Off round trips. New installs start Off; upgrades preserve the previously effective legacy mix without automatically enabling new side/mirror routes. The old Developer GPU switches are removed; the 360° picture page links to the shared controls. Camera routing changes use **Apply changes**; playback uses the selection when next opened. Straighten controls and all picture tuning remain independent. New side/mirror shaders operate on raw camera textures with per-pixel aim, roll, mirroring, Auto-fit and lane-safe sampling; the settings picture uses that same shader. Existing direct/mesh/CPU paths remain available; a GL pipeline failure may require Off and Apply. Saved video pixels and recorder surfaces stay unchanged. Normal Super mirror still uses the **Surround composite stream** for all four directions, not the dedicated road-camera feeds. No 009/7X performance or simultaneous-output claim is made. See [policy, architecture and tests](docs/unified-gpu-rendering.md).
- **Surround preview detail after layout changes:** the full native composite strip stays at its source buffer size while the parent arranges four views, including Selected recording sources. GPU rendering reasserts the normal strip/video dimensions after pane resizes; temporary 360°/blind-side projections keep their own output dimensions. Direct main previews restore their declared buffer on resize and retained-texture reattachment. This fixes a reproduced downsampling failure without Apply changes, replacing the camera producer or changing recording quality. Diagnostics now distinguish GPU input, normal output, last rendered extent and current route. Synthetic native tests verify detail retention; on-car recurrence and performance still need confirmation.
- **GPU 360° renderer (alpha 54):** Settings → System → **GPU image processing → All**, or **Custom → 360° stitching**, enables direct camera-texture projection in the main view and popup. Tap Apply changes after changing it. New installs default to **Off**; existing effective GPU choices are retained as Custom until explicitly changed. The route still needs car validation. It reuses the selected Manual/009 geometry, source assignments, resolution and vehicle illustration; no extra camera output is added. GPU rendering follows incoming frames with a 30 fps ceiling rather than the CPU quality caps, and samples the available camera buffer directly. Stitcher setup/draw failures retain raw output for CPU fallback when that route remains usable. A broken GL route may require selecting Off, or disabling 360° stitching in Custom, and applying the change. Factory takeover, screen-off and stale-frame covers remain. Stitched photos copy the GPU picture only when explicitly requested; recordings remain unchanged. Straighten remains independent of the shared rendering policy, with no measured 009/7X smoothness or power claim yet. See [implementation and validation](docs/gpu-360-renderer.md).

- **360° rendering efficiency:** pack the projection lookup into fewer arrays, skip redundant blending away from seams and use exact packed bilinear sampling. Resolution, calibration, quality caps and recording are unchanged. The complete output matches the previous renderer in synthetic Android tests; the measured compute saving is about 19% at Balanced on an ARM64 emulator, with 20% less lookup-array storage. This is not an on-car frame-rate measurement: TextureView copying still runs on the UI thread. Diagnostics now separate worker queue, publishing and total processing time. See [evidence and remaining bottlenecks](docs/360-render-performance-2026-10-19.md).

- **Consistent main-screen font resources:** retain the existing main-screen text enlargement in an Activity-local configuration before loading views. Theme changes and returning from About no longer depend on mutating resources after startup; sidebar actions retain the same Body style as categories. The intermittent owner-reported smaller labels still need confirmation on the car.
- **Camera startup handover and recovery:** briefly retain Surround-first startup while it is being released, without blocking other sources beyond three seconds. A configured session returning errors without frames uses the existing eight-second no-progress budget; slow starts without those errors retain their full startup window. Late results from a replaced session cannot postpone recovery. Hardware disconnects can still take longer; see [validation and limits](docs/owner-dump-2026-10-18.md).
- **Complete source ownership reporting:** availability checks and diagnostic open lists include dedicated road and manual sources, rather than only the legacy Surround/cabin slots. This prevents our own extra feeds being labelled as another app's cameras. It does not establish the cause of the observed 009 startup stalls. See [October 18 dump findings and side-image framing comparisons](docs/owner-dump-2026-10-18.md).

- **Gallery and filmstrip:** Photos and Videos open Ferix’s date-grouped thumbnail gallery. Select multiple groups to delete; photos can also use Android’s share chooser. Existing viewers retain Send to phone, locking and playback controls, with a horizontal thumbnail strip for switching groups/recordings. New and legacy folders are read together; matching timestamps on different storage directories stay separate. Deletion rechecks protection per directory and keeps files when lock metadata cannot be read. Video grouping keeps surround as the preferred timeline and also discovers road/cabin/manual-only recordings. Saved DIY 360 PNGs retain their existing browsing limitation.
- **In-app picture adjustments:** Settings → Interface → Preview & playback → Image adjustment opens supported camera controls inside the app. No floating-window permission or Developer mode is needed. Opening the panel leaves the saved enable state unchanged; enable explicitly or edit a value to apply it. Exposure, white balance, tone mapping, sharpening, noise reduction and effects appear only when supported. Changes affect all active camera channels, including recordings. Disable adjustments to use camera defaults; Reset restores saved tuning defaults. This is separate from fisheye correction and DIY 360 geometry.
- **Expanded Developer camera export:** the camera/display tools now check advertised Camera2 IDs, reported physical/availability IDs and numeric candidates **0–15**, capped at 48 candidates. New probes stop after three minutes; the current camera still gets its closure wait. The report distinguishes advertised IDs, candidate origins, supported sizes, images and failures. Successfully captured IDs become source-picker choices. Unavailable cameras are skipped, permissions are respected, and no private factory buffers are accessed. This remains one selected capture configuration per candidate, not an exhaustive test of every vendor mode/format. Display identification continues to use public displays.
- **Independent recording groups and preview:** Recording & photos → Edit stream profile offers independently selectable **Surround**, **Front + Rear**, **Cabin** and **Manual mapping** groups. Front + Rear uses dedicated road feeds, not surround crops; each camera retains its own quality settings and file. **Preview layout** separately chooses the selected recording sources, Surround, Front + Rear, Front only, Rear only or Cabin. Recording choices do not change when the preview changes. Tap a group, then **Change camera source**; an unassigned Manual mapping card opens the picker directly. Automatic, Unassigned, currently advertised/configured IDs and IDs successfully captured by Developer export are available. Duplicate active source assignments are rejected. Exit and reopen after changing source/preview selection. Newly added road/manual sources start off; existing recording selections and legacy files are retained. Road/cabin/manual-only recordings are discoverable in the gallery, with ordinary source playback; matching timestamps on different directories remain separate. See [source groups and validation](docs/recording-source-groups.md).
- **Preview layout selector:** Recording & photos → Preview layout shows the saved main-screen choice; the stream editor offers the same selector with the current layout in its button. Options are Selected recording sources (the unchanged default), Surround, Front + Rear, Front road, Rear road, Driver / front cabin and Surround + Front + Rear. The editor highlights the current choice and requires OK; Cancel keeps it. Preview changes save only the preview preference, preserving recording groups, camera mappings, quality and stored profile tuning. Existing saved layouts remain selected across upgrades and vehicle-model changes. Dedicated road previews require identified camera sources; 7X road feeds remain unconfirmed. Exit and reopen after changing. This selects main-screen sources; popup/HUD/Super mirror layouts stay independent.
- **Shared camera sources and reset:** Recording & photos → Camera sources, Developer → Camera sources and the stream editor share one mapping. Each source offers Automatic, Unassigned or a manual camera ID. Automatic clears that source’s manual choice, including its old Developer override. **Reset all to automatic** clears manual assignments and obsolete cached slot plans while keeping the vehicle model, preview, recording switches and tuning. Surround uses detected feed → model fallback; manual choices take priority. The page shows the resolved ID. Existing newer choices win on migration; enabled old Developer choices fill missing roles. Previously inactive Developer choices stay inactive. Manual mappings work with Developer mode off. Explicit Unassigned retains the old behavior of disabling that source’s recording (both roads for Front + Rear); resetting to Automatic does not change recording switches. An additional/manual source has no automatic default. Exit and reopen after changes. See [mapping and reset details](docs/shared-camera-sources.md).
- **Vehicle-specific source defaults:** System → Vehicle model selects Auto, Zeekr 009 or Zeekr 7X. The 009 defaults are detected Surround (fallback ID 3), front road = ID 0, rear road = ID 1 and Cabin = ID 2, based on the supplied owner export. Only one 009 cabin feed is confirmed; enabled legacy extras stay under Manual mapping. 7X retains the upstream surround/two-cabin discovery path; separate road feeds require identification and manual mapping. Explicit source overrides and recording selections are preserved. Simultaneous Front + Rear + Surround recording still needs vehicle testing. See [model selection, evidence and defaults](docs/vehicle-model-profiles.md).

- **Window teardown and recording:** side popup, HUD and 360 popup textures are handed to the camera scheduler when their views disappear, then released after the capture session removes them. This addresses the abandoned-output path correlated with two-second gaps in the supplied recordings; on-car continuity still needs verification.

- **Scan restart preservation:** restarting the app keeps the current signal scan and its original deadline/line budget, including quick restarts. Turning the scanner off and starting a new session begins a new file. Based on Ferix’s restart fix, using explicit session identity instead of elapsed-time inference.
- **HUD night contrast:** Blind spot camera → HUD has a separate night contrast slider, switched by the car’s day/night signal. Defaults to neutral 100%; it does not change display routing. The owner has now confirmed live side-camera video on their 009 HUD; layout refinement still needs a car check.

- **Explicit USB-to-internal fallback:** Developer mode alone no longer permits fallback. Settings → Developer options → Recording internals → Allow internal storage fallback is off by default, including upgrades. A requested recording waits for usable USB; the main button shows Waiting for USB / Tap to cancel. With fallback explicitly enabled, recording starts give USB 10 seconds first. Mid-recording internal failover also requires this opt-in. Direct Internal storage selection and Relay write remain separate choices.

- **Grouped settings:** retain the familiar main categories, with Blind spot camera → Behavior / Picture / Window / HUD tabs, and Recording & photos → Stamps & driving information. Screen-off recording now lives under Recording & photos. HUD and Developer tools have task-based headings; saved feature values and defaults are retained. See [settings map](docs/settings-menu.md).

- **Settings layout fixes:** camera cards reserve space for their switches; long names/custom badges wrap instead of hiding controls. Camera/quality cards stack in narrower panes, option buttons wrap at equal heights, and Diagnostics actions can grow with text. The main action column scrolls when too short to fit. Changing settings categories clears stale nested titles/Back history. See [native layout review](docs/settings-layout-review.md).

- **Fresh-install permission handling:** startup requests camera/storage access used by the app and no longer blocks on the undeclared, unused microphone permission. The Developer permissions page omits that unavailable permission.

- **Developer mode:** Settings → System → Enable developer mode exposes the Developer options category immediately, with an inline warning and no password or hidden About-page gesture. It is off on new installs; existing enabled state survives updates and restarts. The selected settings category also stays highlighted after screen recreation. Turning it off hides the category, disables developer-gated settings and stops signal scanning while retaining saved values. Camera pipeline changes use the shared Apply changes action.
- **Camera and display research tools:** Settings → Developer → Camera and display tools provides an on-demand batch snapshot of every available Camera2 ID, plus a guided numbered test for public presentation displays. Confirm a large main-screen card first, then test one secondary display at a time for up to 30 seconds and record where it appeared or that it was not seen. Images, generated display cards and JSON reports go under **ZeekrCompanion/Developer**. Both require a connected vehicle interface with recording stopped and factory camera closed; neither checks gear or speed. The scan temporarily closes this app’s normal feeds and retains exclusive ownership until native closure is confirmed; saved camera/HUD settings stay unchanged. These tools cannot screenshot factory displays or expose private factory video buffers. See [instructions and limits](docs/developer-camera-display-tools.md).

- **Independent camera selection:** Surround, Front cabin and Rear cabin are opened according to their individual profile switches, including cabin-only profiles. Tap Apply changes after changing recording switches or active camera mappings; starting a new recording with a changed source selection asks you to apply it first. A pending preview-only change does not block recording.

- **One app folder:** new recordings, photos and exported diagnostics go under **ZeekrCompanion/Videos**, **Photos**, **Logs** and on-demand **Developer** exports on selected storage. Playback, protection, storage accounting and cleanup also support older `ZeekrShortcutApp` and EVCam folders without moving existing files. Equal filenames and lock lists remain separate; archives preserve each original relative path. This folder-name change ships in alpha 53. See [storage layout](docs/app-storage-folders.md).

- **Recording and transfer integrity:** verified USB/archive copies preserve both files on collisions; relay buffers use private files instead of disposable Android cache. Failed protection-list replacement keeps the old list; developer repair first retains a verified original backup. See the [whole-project review](docs/project-safety-review.md) for fixes and remaining limits.
- **Functional vehicle model selection:** Settings → System → Vehicle model replaces the cosmetic Interface name selector and separate 009 mapping switch. Auto accepts explicit, unambiguous Android model names; unknown devices use generic discovery and show a manual-selection prompt. The supplied 009 reports Pixel 3a, so select 009 manually. An existing enabled 009 mapping migrates to 009; the old cosmetic name alone never changes camera sources. Model defaults also drive builtin labels and Auto vehicle artwork. Saved manual camera mappings, recording choices, side-picture adjustments and custom profile names are kept; tap Apply changes after changing. HUD still matches the actual display independently and retains its separate layout. The 009 image-fit calibration stays opt-in and is suspended without deletion on a selected/detected 7X; shared Manual 360 geometry is estimated, not a verified 7X calibration.
- **Rapid theme changes:** foreground recording and relay services retain the shared live camera pipeline even when a newly recreated main screen has not acquired its own camera reference yet. Camera ownership still belongs to the current upstream scheduler.
- **Camera freshness guards:** Main surround/cabin previews, Super mirror, side popup and HUD cover stalled pictures after 500 ms without advancing texture timestamps, checked at 100 ms intervals. Reopened textures require new frame progression; missing geometry stays covered. The separate side popup stays transparent and touch-through during ordinary startup, appearing as soon as geometry and advancing frames are available. If startup exceeds 750 ms (checked every 100 ms), it shows the waiting message and close control; once shown, a stalled feed remains covered rather than disappearing. This does not add a fixed opening delay or change HUD/Super mirror presentation. These software guards still need on-car timing checks. See the [camera safety review](docs/camera-safety-review.md).
- **360° camera section and popup:** Settings → 360° camera gathers display mode, quality, calibration, calibration/quality reset and independent popup layout controls. A single **Show 360° popup** switch replaces separate Open/Close actions; it tracks the current popup session, including X or service shutdown, without saving an automatic-start preference. Temporary suspension for factory/screen takeover keeps the session enabled. Closing it leaves the preferred display mode, layout and calibration intact. The main **360° view** action controls the main preview; **Windows → 360° window** opens a service-owned overlay that stays visible after minimising the app. Drag, pinch, expand/restore or close with X; size and position are saved independently. Expansion now uses the current overlay area and visible system-bar insets, with a small 8dp margin, instead of app resource height; it refits when that area changes without losing expanded mode or overwriting saved normal placement. This addresses the extra-bottom-space code path reported over Android Auto; the exact 009 projection-app behavior still needs a vehicle check. See [bounds correction](docs/diy-360-view.md#expanded-bounds-correction-2026-10-18-unreleased). Opening is manual, with no boot/indicator trigger. It shares the surround hardware stream through an additional low-priority surface; hardware limits/configuration failures close the 360 popup before other optional camera views, without withdrawing mirror/recording declarations. Factory views/reverse, screen-off and developer probes suspend the popup and release its camera claim. No P-only restriction is added. While the popup is open, the main Take photo action exports the fresh stitched popup. Actual simultaneous-output support and recording continuity need vehicle checks.
- **DIY 360 prototype (experimental):** manually switch the live surround preview to a stitched top-down ground view with a central vehicle outline, narrow blending at the four body-corner joins and bilinear sampling. Outside each join, one valid camera supplies the image, avoiding broad overlap that doubles road markings. Manual defaults remain estimated; a model-aware 009 image-fitted profile improves reference alignment as described below. Tap **360°** on the main camera screen; hold it to calibrate, choose **360° resolution** or reset. CPU preview targets are about 15 fps at Low, 10 fps at Balanced and 5 fps at Full, subject to hardware throughput. Capture follows deadlines, skips unchanged texture timestamps and retains one in-flight frame; Diagnostics report the requested interval and last presentation gap. Balanced is the sharper default. Capture slows before lowering resolution after repeated slow frames; one 40+ ms readback no longer permanently reduces detail. Cadence recovers after sustained good frames. Full requests the live frame at its native dimensions (up to 8 MP), with automatic resolution reduction if processing is slow or memory is limited. On the supplied 009 stream, Balanced uses 768×3084 → 640×960; Full uses 1280×5140 → 960×1440. While 360 is selected, the main photo action saves the fresh stitched preview at the active resolution and calibration notes to **ZeekrCompanion/Photos/360** on configured storage. Switch back to Four cameras to capture ordinary/raw composite photos. Saved 360 PNGs are separate from the regular photo-playback list. It requires a connected vehicle interface, with no P-only or speed restriction on preview or calibration. Reverse and factory-camera takeover still suppress the DIY picture. Distance accuracy remains unverified. See [setup and limits](docs/diy-360-view.md).
- **009 and 7X vehicle illustrations:** Settings → 360° camera → Picture → **Vehicle illustration** offers Auto (default), 009, 7X and Generic outline. Auto uses the functional Vehicle model selection/detection or an active 009 image-fit; unknown head units keep the outline. A historically named 7X stream profile alone does not identify the car. **Car colour** offers Green, White (default) or Black for both illustrations. The 009 Green artwork uses a light, muted silver-sage/grey-green finish based on the owner’s factory-view reference; 7X colours remain separate. Choices apply live to main/popup/stitched photos, survive calibration reset and never change camera mapping, projection geometry or recordings. A small rounded, colour-matched opaque surround replaces the exposed black backing; ground beneath the vehicle remains hidden. 7X artwork is implemented, but its DIY 360 alignment/performance remain untested on a real 7X; use Manual projection, since the 009 image-fit profile is 009-specific. See [artwork and settings](docs/009-car-overlay.md) and [7X collection/checklist](docs/7x-360-validation.md).
- **009 image-fitted 360 projection:** select **Settings → 360° camera → Calibration → Projection profile → 009 image fit (experimental)**. Four camera transforms were fitted from the owner's original raw/factory image references, reducing the oversized front band and improving ground alignment in offline comparisons. Every frame still comes from the live cameras. This is an empirical 009 reference fit, not recovered factory calibration or measured parking distances, and is not a 7X profile. Untouched installations use Automatic: selected/detected 009 uses this fit, while 7X or an unidentified car uses Manual. Existing explicit Manual/009 choices and legacy manual lens edits are preserved. Choose Automatic to follow the vehicle model again without deleting tuning; switching profiles retains manual lens values, camera assignments and quality. The fit fixes its reference footprint at 5.2×2 and now defaults to a wider margin of 3 reference units, adjustable from 0.5–3; only sources, quality and margin apply in its calibration dialog. Its view margin is saved separately from Manual, so upgrading an existing fit initially uses the new wider default; Manual values remain saved. Values are reference units, not centimetres or measured parking distances. The fitted joins use a narrower blend to reduce doubled detail; residual corner misalignment and stretching of raised objects remain. Reset calibration clears tuning/quality and returns to Automatic. Main preview, popup and stitched-photo export use the same selected profile; recording is unchanged. The owner’s 17 October export confirms this profile and the 009 artwork were active together; alignment/distance accuracy and outer-view errors remain unverified. See [fit evidence and limits](docs/009-image-fit.md) and [latest 009 export findings](docs/009-diagnostics-2026-10-17.md).
- **Automatic blind spot camera:** adapted from
  [FerixzTanz's original turn-signal side view, PR #4](https://github.com/dts88/zeekr-shortcut-car/pull/4),
  with credit and original commit authorship preserved. It works with the current
  camera scheduler and has its own settings section.
- **Super mirror integration:** temporarily show the signalling side's camera in
  the existing mirror, then restore the previous view; alternatively, show a
  separate side-camera window alongside it or temporarily hide the mirror while
  that popup is visible.
- **Playback controls:** video and photo action buttons size to their labels, keep equal heights within each row, and wrap to another row when space is tight. “Send to phone” remains aligned and fully readable without shrinking the text.
- **Side-window controls and tuning:** shared left/right placement, drag and pinch
  resizing with a saved square/portrait/landscape aspect ratio, temporary expand/close controls, per-side image rotation, defaults
  reset, and default-on optional framing adjustment to avoid black patches at extreme aim
  angles. High angles can substantially narrow the visible area.
- **Side-popup lifecycle recovery:** adapted from dts88 PR #41. A two-second recovery check reconnects an attached popup after slow camera setup or pipeline replacement, with the scheduler retaining camera ownership. Reopening can reclaim the same retained texture; a rejected camera output or window stays suppressed for the current readiness period and can retry after a factory/screen interruption or 30 seconds outside D. Repeated telemetry updates cannot force a rejected popup back into the session. Existing aspect ratios, gestures, dismissal, HUD and mirror choices remain available. The recovery check does not bypass factory views, reverse, link/freshness guards or recording priority. See [lifecycle port and validation](docs/upstream-side-view-lifecycle.md).
- **Side-window transitions:** adapted Ferix fades (130 ms in / 160 ms out) keep hidden consumers draining while reducing a previous side image flashing on reopening. The screen popup retains its 750 ms startup notice grace; changing sides resets frame progression before revealing the new crop. Factory takeover and destruction still detach immediately, and cancelled requests cannot be reopened by an animation. HUD uses the same fades; Super mirror replacement is unchanged.
- **System day/night camera controls:** side-popup buttons, turn-arrow badges and camera labels use light surfaces/dark icons by day and dark surfaces/light icons at night. Super mirror uses the same themed labels and side controls. Open windows repaint when Android reports a theme change; no separate switch is needed. This follows Android’s reported `uiMode`, so firmware that does not publish its factory theme may differ. The projected HUD keeps its dark-backed badges. Camera pixels and recordings are unaffected.
- **Optional side-camera mirroring:** flip the image in the popup or Super mirror replacement; off by default, with recordings unchanged.
- **Navigation drawer:** Quit app uses the same theme-aware red label and power icon as the Settings quit action. The menu uses a compact Companion logo header, a flat neutral panel and regular 18sp destination labels in both themes, with larger-text wrapping and the same switch styling as Settings. **Switches → 360° window**, directly below Super mirror, opens/closes the existing manual popup session. It follows the same state as Settings and Windows, including external closure and temporary factory/screen suspension; it does not change the main preview selection, recording choices or automatic-start behavior. Overlay permission is still required.
- **Update offer:** available updates show a compact version/download summary and separately scrollable release notes, with 16sp body text, hanging bullets and space between changes. Later and Download and install stay outside the notes; checking, download verification and installation behavior are unchanged.
- **Factory-style light text:** menus, settings titles and ordinary text use a shared softer charcoal grey, keeping regular settings type and the existing sizes. The owner’s factory-menu photos guide the appearance; photographic lighting prevents an exact colour-match claim. Night text, recording red and text over camera pictures retain their separate palette roles.

- **App theme choice:** Settings → Interface → Theme offers Follow system (default), Light and Dark. Confirm with OK; Cancel keeps the current choice. The choice persists and applies to app screens and camera-popup controls without changing menu font sizes, camera pixels, recordings or the vehicle’s system theme. The HUD retains its separate brightness/contrast and dark-backed badges. The sidebar uses the same 18sp labels in both themes and keeps system font scaling; the earlier About/Quit recycled-font fix is also included.
- **Consistent Super mirror controls:** normal Super mirror now has the same themed top-corner Close and Expand/Restore buttons as the blind-side popup. Close turns Super mirror off through its existing enable switch; reopen it from Settings, the menu or an assigned floating-button action. Expand is temporary, preserves the current aspect ratio and restores the previous position/size without saving the larger geometry. Restore before dragging, pinching or switching cameras with gestures. A side-camera replacement ends any normal-mirror expansion and returns to the saved normal geometry when dismissed; its Close still dismisses only that side-view activation. Docked mirrors keep their existing pull-out tab, with corner buttons hidden. Camera labels move to the bottom to leave the controls clear; recording and HUD settings are unchanged.
- **Optional side-camera auto-fit:** Blind spot camera → Picture → Auto-fit to avoid black patches defaults on. With Straighten picture enabled, on narrows the actual view near the source edge; off preserves the requested width/aim/rotation and draws missing coverage black, clipped to the selected camera lane. Shared by the popup and Super mirror replacement; the HUD can follow or customize it. Window geometry remains independent. Straighten off keeps its existing crop-to-fill rotation. Recordings and DIY 360 are unchanged. See [behaviour and validation](docs/side-view-auto-fit.md).
- **HUD side camera:** adapted from Ferix PR #14; choose Screen, HUD or both. HUD picture can follow popup tuning or keep custom view width, aim, rotation, mirroring, labels, Straighten and Auto-fit; size, aspect ratio, position and tone always stay independent. The owner has confirmed live video on their 009 windscreen HUD. Its public, powered-on display 3 at 1280×640 is now recognised automatically; the extra 009 enable switch is removed. The 009 starts with a smaller fixed-right picture and independent saved geometry. The panel fits the measured Presentation area rather than assuming physical display pixels equal drawable space. Factory graphics remain on their own layer and can appear over the camera. This confirms the owner’s head unit, not every 009 firmware. See [009 HUD evidence and controls](docs/009-hud-live-video.md).
- **HUD alongside factory views:** Settings → Blind spot camera → HUD → Keep HUD side camera during factory views is experimental and off by default. Enabling it allows only the HUD side camera during factory 360/camera popups/parking assist; reverse still hides it, and the screen popup/Super mirror side replacement still yield. It changes display eligibility, never forces camera ownership; factory camera use or hardware contention can still interrupt the feed. Reset blind spot settings turns it off again.
- **HUD picture and window controls:** Settings → Blind spot camera → HUD adds HUD contrast (100–300%), brightness at night (20–100%), edge fading (0–40%), optional positioning by indicator side. Defaults preserve the existing picture (100% contrast/night brightness, 0% fading), fixed position and ordinary Presentation window; enable each treatment explicitly. The former experimental “above factory HUD information” layer is removed, following Ferix’s unsuccessful 7X test. HUD uses the ordinary Presentation window; old overlay preferences are ignored. Factory graphics may cover the picture. The owner has now confirmed live camera video on display 3; the smaller 009 fixed-right layout is described above. These controls do not identify the physical HUD or force camera access; factory-view coexistence has its own off-by-default switch described above. Known 009 display matching is automatic; independent mirroring and screen/recording defaults are retained.
- **Optional camera labels:** identify the displayed camera in Super mirror and
  the side window. Super mirror also has a defaults reset.

The following additions include work adapted from
[FerixzTanz/CAMCORDER](https://github.com/FerixzTanz/CAMCORDER), with source credit
in [NOTICE.md](NOTICE.md) and source hashes in [port notes](docs/ferix-feature-ports.md),
alongside fork follow-ups such as USB drive logging.

- **Phone sharing fixes and diagnostics:** reachable network addresses, transfer status and HEAD/compression fixes. Android timeout reports still need device verification.
- **Automatic USB drive logs:** Settings → Developer options → Drive logging to USB starts a separate read-only logging session. Turn it off after the drive and wait for Saved; raw signal events, app/lifecycle logs and automatic start/stop Diagnostics are already in a timestamped `ZeekrCompanion/Logs/Drive_…` folder. It keeps logging when the main screen closes, bypasses the private scan’s 60,000-event/filtered-export limit, retains numbered parts and older sessions, and never falls back to internal storage. Limits are 8 hours, 256 MiB per session and a 64 MiB free-space reserve; Android may stop it earlier. It does not start on boot or resume automatically after process death. See [drive logging behavior and validation](docs/usb-drive-logging.md).
- **Read-only vehicle signal scanner:** an opt-in Developer option for finding unmapped signals, including possible blind-spot warnings; automatically stops after three hours or when Developer options are locked. It also records customize-value callbacks as C: entries and reports other callback shapes, while retaining bounded callback queues.
- **Turn arrows:** upright green/grey indicator arrows in both side-camera display modes, placed between the popup controls.
- **Hard-braking footage locks:** automatic protection around hard stops, with a separate storage budget that preserves manual locks.
- **Custom clip export:** choose a range up to 10 minutes in playback and save without re-encoding; repeated exports preserve existing files.
- **Recording follows the driver:** configurable start on getting in/leaving P and stop after leaving, integrated with the current upstream recording lifecycle.
- **Night theme and floating-button styling:** black night backgrounds, darker settings rows and a smaller, flat recording icon with the upstream waiting state preserved.

See the feature sections below and [port notes](docs/ferix-feature-ports.md) for settings, source commits and testing limits.

The blind spot camera currently responds to **turn signals**, not obstacle alerts.
Setting its minimum opening speed to 0 removes the app's speed threshold; factory
camera views, including the low-speed 360 window, still take priority. Factory
stitched 360 access and obstacle-warning signals remain research topics, not
implemented features. The scanner helps research warning signals; it does not
provide obstacle detection or the factory stitched camera feed.
The [blind-spot investigation](docs/blind-spot-side-view.md) records an unidentified
ADAS signal changing near the owner's possible right-side warning on October 18;
its side and warning/clear values remain unverified, so it does not trigger a camera.
See [factory 360 research and the collection tool](https://github.com/wysie/zeekr-shortcut-car/tree/research/factory-360-view)
for the current investigation and the device information needed next.

This fork is mainly for personal use. Automated tests and release builds are
checked, but vehicle testing is limited. I have confirmed camera labels and Super
mirror replacement on my 009; other changes still need on-car verification. See
[test notes](docs/009-test-notes.md) for the scope of that feedback. Compatibility
with other models and firmware versions is not established.

The owner's [009 popup reference values](docs/009-popup-reference.md) are recorded
for a future popup-only preset; they are not HUD recommendations or new defaults.
The latest report still has HUD image clipping and factory-graphics overlap.

**Clone `main` and build APKs from `main`.** It contains the combined installable
fork; `upstream-main` tracks dts88's unmodified code, and `ferix-main` tracks
Ferix's unmodified CAMCORDER source. Individual contribution branches stay separate;
see the [submitted upstream PRs](docs/upstream-contributions.md). See
[Fork workflow](FORK_WORKFLOW.md) for the branch layout. Whenever a feature is
added or changed, update this README with its behavior, settings and limitations.

The [source integration ledger](docs/source-integration-ledger.md) records the exact
dts88/Ferix heads reviewed, what we imported or adapted, what we deferred/skipped,
and experiments later removed. It links local commits, reasons and verified release
inclusion. Update it on every external review, including reviews with no imports;
tracking a source branch does not mean all its changes are in this fork.

Settings sidebar entries, including About & credits and Quit app, use the same
font size on every row binding. Long labels wrap onto two lines rather than
shrinking, with row height growing as needed for larger system fonts.

Release naming uses **Zeekr Companion** for the app and release title,
`vX.Y.Z-alpha.N` / `vX.Y.Z-beta.N` / `vX.Y.Z` for tags, and
`ZeekrCompanion-X.Y.Z-alpha.N.apk` (or the matching beta/stable version) for downloads.
Alpha/beta releases are marked Pre-release; Alpha updates require choosing the Alpha channel.
Each release includes a `.apk.sha256` checksum. Existing release links, application ID,
signing identity and settings are retained. New files use `ZeekrCompanion`; earlier `ZeekrShortcutApp` folders remain supported.

The original app is a surround-view dash cam for the ZEEKR 7X head unit. The
remaining sections describe its functionality together with this fork's additions.

[中文说明](README.zh-CN.md) · [Changelog](CHANGELOG.md) · [Credits](NOTICE.md) · [Platform notes](docs/zeekr-platform-notes.md)

> [!IMPORTANT]
> **Known issue: App Lab lost after a restart.** Some users report that App Lab disappears after the head unit restarts and must be reinstalled. This is likely related to this app; the cause is under investigation. If this happens, or the app stutters or camera images are unavailable, **export a report from Settings → System → Diagnostics before restarting the head unit** and report it via a [GitHub issue](https://github.com/dts88/zeekr-shortcut-car/issues) or other channels. Apologies for the inconvenience.

> [!WARNING]
> Experimental, unofficial software. Not affiliated with, approved by, or endorsed by ZEEKR,
> and not certified for any vehicle safety function. It does **not** replace the factory dash cam,
> reversing camera, or blind-spot monitor. Do not operate it while driving.
> Full safety notice: [安全须知](README.zh-CN.md#安全须知).

---

## What it does

App Lab gives a third-party app the four surround cameras as **one packed stream**: four
square views packed into a single frame, which previews as a distorted vertical strip. This is
not the factory's stitched bird's-eye 360 image. This app
splits it back into a 2×2 grid and builds on that. Chinese, English and Malay interface.

### Dash cam

- Records the surround view as a 2×2 grid, plus the front and rear cabin cameras if you turn them on.
- Three recording presets — save space (10 fps), balanced (20 fps), sharpest — each showing how
  many GB an hour it needs and how long your USB drive will last. Frame rate, bitrate, segment
  length and codec can also be tuned per camera.
- 1–10 minute segments. With a video storage cap set, the oldest unlocked clips are cleaned up when
  over the cap or low on space; with no cap, nothing is deleted and recording stops when space runs
  low.
- Photos use each camera's largest size.
- Records to a **USB drive only**. Writing to the head unit's built-in flash sits behind developer
  options, because that storage cannot be replaced once worn out.
- *Driving info bar* (experimental, off by default): turn signals, steering, gear, pedals, speed,
  lamps and more, recorded in a strip under the video.

On the main screen, tap any view to fill the preview with it and tap again to go back. The
**Straighten** button removes the fisheye bend, on the main screen and in playback. A floating
button shows whether recording is running and opens the app from anywhere.

### Super mirror

An electronic rear-view mirror: a floating, dockable window showing any one camera enlarged. Pinch
to zoom; swipe left or right to change camera; swipe up or down in the middle third to raise or
lower the framing. Or turn on *Button mode* and tap the top, bottom, left or right of the window to
switch camera. Push it half off-screen to hide it at the edge, and tap to bring it back. The
rear camera is mirrored, like a real mirror. Fisheye correction with an adjustable field of view is
optional.

Enable **Settings → Super mirror → Show camera label** (on by default) to identify the
displayed camera as Front, Rear, Left or Right. The label follows camera switching and temporary
side views, and also appears in the independent side-view pop-up. Turn it off to hide the label.
This setting does not alter recordings.

### Automatic blind spot camera

Enable **Settings → Blind spot camera → Automatic blind spot camera** (off by default). Above the configured
speed threshold (default 30 km/h), signalling shows the camera on that side. When Super mirror is
on and **When Super mirror is active → Use its window** is selected (default), it temporarily shows the side camera in
the same window and restores the previous camera,
framing and position after the signal ends and the configured close delay. A docked mirror is
brought into view temporarily and docks again afterwards. With Super mirror off, a separate
pop-up is used. **When Super mirror is active → Show both windows** keeps the mirror
alongside the separate popup. **Hide during popup** instead hides the mirror only
once the independent popup becomes visible, retaining its own shape and size.
The mirror returns when that popup closes, finishes fading out, is dismissed with
X or is detached for factory/screen takeover. Hidden startup/prewarming and a HUD picture without a screen popup do not hide it. This never changes the mirror enable switch, saved camera,
position or size. Existing replacement/on and alongside/off selections are preserved;
reset restores Use its window. Hiding keeps the mirror feed attached and does not
free an output slot. Both windows share the existing surround camera stream; the side
window has its own output and does not replace the mirror output. Reverse, factory 360, factory
side view and park assist take precedence.

Under **Picture**, **Mirror side-camera image** flips left and right in both the separate popup and Super mirror replacement. It is off by default; labels, arrows and controls remain readable and recordings are unchanged. Reset side-camera settings restores mirroring to off.

With **Straighten picture** on, per-side rotation is fitted together with the final
window shape, avoiding the extra enlargement previously applied after rotation.
This applies to the popup, Super mirror replacement and HUD. Enlarged side views
use a finer correction grid (up to 16×16); small windows retain 10×10. Requested
aim, rotation and mirroring are retained, and, with Auto-fit on, the view still narrows when needed
to stay inside the camera image. At high rear/up angles, substantial zoom can
still be necessary. This uses the existing live feed; it does not increase its
native resolution or establish factory-equivalent sharpness. With Straighten
picture off, the existing crop/rotation behaviour is retained.

Under **Blind spot camera → Picture → GPU picture quality**, **Upscaling** offers
Bilinear (default), Bicubic soft (Mitchell-Netravali) and Bicubic sharp
(Catmull-Rom). **Sharpening** is independently adjustable from 0–100%, default 0.
These experimental controls apply live to GPU blind-side popup/HUD pictures,
Super mirror's temporary side view and the settings preview. Enable the blind-side
GPU route under System → GPU image processing (or select All); compatibility
rendering retains these choices but does not apply them. No camera rebuild is
needed for sampling edits on an active GPU route. Quality is shared across these
blind-side outputs, independent of HUD framing overrides. Labels/arrows stay at
final display resolution. Cubic taps and bounded sharpening stay inside the selected
native lane, including separator edges; missing coverage remains black. Bicubic
uses more texture samples and sharpening can emphasize camera noise, so compare
moving footage on the car. They cannot recover absent camera detail. Normal Super
mirror, surround/360°, playback and recorded files are unchanged. Reset side-camera
settings restores Bilinear and zero sharpening; settings backups include both.

**Capture quality comparison** is below the live preview under **Blind spot camera →
Picture**. Keep Surround selected in the applied main preview, choose **Left** or
**Right**, and tap it while the source is live. It saves one ZIP with
`native-side.png` (the native-sized decoded side lane in camera orientation),
`popup-render.png` (the same source frame rendered with your popup framing), and
`comparison.json` (timestamps, source/output dimensions, renderer, aim, rotation,
mirroring, Auto-fit and sampling settings). The GPU path uses the popup's side
shader; compatibility uses its Canvas mesh. The native GPU reference uses nearest
texels before app picture processing, then restores the live sampler. The render
uses the currently shown same-side popup's dimensions when available, otherwise
the saved normal popup size. It is a **diagnostic render, not a live-window
screenshot**; labels, arrows, controls and HUD-specific cropping/tone are excluded.
Both files use one captured frame, not separate live reads. The action neither
opens a popup nor changes camera declarations, recording intent or saved settings.

The ZIP is saved to the diagnostics export directory (normally
`ZeekrCompanion/Logs`, with the existing internal/app-folder fallback). The result
shows its full location and offers **Send comparison to phone** and **Copy file
location**; the latest completed location survives reopening. Capture is explicit,
one at a time, and refused for missing/frozen sources, reverse, factory views or
screen-off. Leaving/hiding the preview or changing side cancels pending work;
completed ZIPs remain available. Files are lossless PNGs inside a uniquely named,
completed ZIP; partial writes are removed on failure/cancellation. Native source
and render are bounded to 4096px per edge and 8,388,608 combined pixels; the direct
full-frame read also has an 8,388,608-pixel/8192px limit. Oversized captures are
refused instead of silently downscaled. This is a one-off readback/compression
operation and can briefly load the renderer/storage; no in-car timing or optical
quality improvement is claimed. See [capture architecture and validation](docs/blind-quality-comparison.md).

The 009 export reports a 1280×5140 live composite with four 1280×1280 camera
images. The side popup receives this same full-size buffer; the drawing path does
not make a small preview bitmap before taking its camera crop. An Android
emulator regression test confirms fine native-buffer detail survives the actual
corrected popup path. This does not verify the optical resolution or internal
processing of the factory camera hardware.

**HUD output recovery:** while the HUD is showing or warming up, a half-second ownership check reconnects a missing HUD declaration or replaced camera pipeline without requiring another indicator off/on cycle. Healthy outputs remain untouched; configuration refusals keep their cooldown and fallback. Rebinding clears the previous pipeline’s picture state, and the warning stays until frames actually resume. This applies with GPU All and compatibility rendering; it adds no new switch and still needs confirmation on the car.

**Show side view on** selects Screen (default), HUD, or Screen and HUD. Screen output still follows **Use Super mirror when active**. Automatic HUD selection needs an accessible, powered-on, public presentation display with the confirmed 009 signature (display 3, 1280×640), known 7X 1440×480 dimensions, or an explicit HUD name. An unavailable or rejected HUD falls back to the screen behavior, preserving its opening-speed threshold.

The confirmed **009 display 3 at 1280×640** is detected automatically alongside the known 7X 1440×480 signature or an explicit HUD name. No extra 009 enable switch is required; old saved switch values are ignored. Private/non-presentation displays, named cluster/rear/passenger screens and ambiguous matches are excluded. An unavailable or rejected HUD falls back to the screen behavior. The observed video confirms this owner’s 009, without establishing every firmware’s mapping.

Under **HUD picture**, **Follow popup picture settings** defaults on for unconfigured settings: view width, aim back/up, left/right rotations, mirroring, direction labels, Straighten and Auto-fit track the popup. Turn it off to reveal custom HUD controls, initially copied from the popup; switching follow on and off later restores the previous custom values. Explicitly saved legacy HUD mirroring is preserved by starting in custom mode. Custom HUD edits never change the popup or Super mirror. Popup labels still share Super mirror's label preference. Auto-fit can produce different actual coverage for different window shapes even with the same requested picture settings.

Height (30–100%), shape (width/height, 100–300%), road-edge trim (0–60%), horizontal/vertical position, opacity (20–100%), contrast, night brightness and edge fade always stay HUD-only, in either mode. The 009 layout defaults to 50% height, square shape, horizontal 94% (right), vertical 50%, and Follow indicator side off, so either camera appears on the right. Its layout is saved independently from the previous generic/7X layout (74% height, horizontal 6%). **HUD road-edge trim** is under Blind spot camera → HUD → Position & size. It starts at 25% on the recognised 009 display, including upgrades without a saved crop; generic/7X HUDs keep 0%. It prioritizes the car-body edge of the displayed camera picture: mirrored Left trims from the left; mirrored Right trims from the right. Mirroring off reverses these edges. This follows the camera side and HUD picture mirroring, independently of fixed/follow-indicator window placement. The visible panel still shrinks away from the HUD centre while retaining its outer placement, height and original picture scale. Both cameras keep equal visible widths. HUD direction labels sit at the bottom of the retained car-body edge (mirrored Left at bottom-right, mirrored Right at bottom-left); labels and arrows are drawn after cropping/upscaling inside the visible window. Set 0% to restore the full picture; this setting and its reset are independent of popup picture following. Existing position, shape, aim and recording settings are retained. The 25% starting value reduces the footprint; physical projector masking and changing factory graphics still need an in-car check. HUD windows have no touch controls. Reset restores Screen output, follow-popup mode and picture/layout defaults while retaining the main feature switch and shared popup labels.

**HUD visibility calibration** under Blind spot camera → HUD → Position & size projects a
200px red square onto the same detected public HUD. From the main screen, adjust
its size and position, nudge by 1 or 10 logical pixels, or select Red/Cyan/White.
The square can cross Android's reported edges; L/R/T/B identify its image edges.
To calibrate, sit normally while parked and keep the whole square comfortably
visible through small head movements. **Set left boundary** records the square's
left edge; **Set right boundary** records its right edge. **Save calibration**
applies both limits with a default 2% of viewport-width margin at each edge
(adjustable 0–5%). The visible HUD camera panel is moved inward, or proportionally
shrunk if necessary, without adding another camera crop. Body-edge trim is retained
proportionally, and labels/arrows are drawn at the final visible size. Both cameras
use the same usable interval, for either renderer and fixed/follow-indicator mode.
No calibration is enabled by default. Selecting boundaries alone changes nothing;
leaving, stopping or restarting the square abandons unsaved selections.

Calibration is matched to the public HUD name/flags, display dimensions and exact
measured content dimensions/origin; it is ignored on a mismatch, which the square
controls report. These public attributes are not a hardware serial identity.
**Reset calibration** removes only these limits, restoring normal placement and
retaining picture, position, trim, output, recording and observation choices.
The saved record is included in settings backups and Diagnostics. It constrains
horizontal app placement; it cannot enlarge the optical viewing range, calibrate
vertical visibility or detect factory graphics. Existing position/trim controls
still handle factory-graphic clearance. Physical fit requires an owner check.

While parked, save **Fully visible** just before an edge disappears, then save
**Left/Right/Top/Bottom cut off** (or **Not visible**). Repeat at different heights
to investigate a shaped optical boundary or factory graphics. These explicit
observations are saved, with timestamps, session/display details, actual measured
viewport/origin, marker rectangle, logical visible intersection and current HUD
placement/mirroring. The latest 128 survive closing the app and appear in
Diagnostics JSON (`hud_visibility_observations`) and its text report; **Open
Diagnostics to export** leads to the existing save/send controls. Observations are
not settings backups and do not automatically change camera bounds. Normal HUD
camera video is temporarily hidden; recording intent is retained. The marker stops
when leaving the page, turning the screen off, losing its display or after three
minutes without input, and never restarts automatically. This is manual evidence:
Android cannot measure physical projector clipping or identify a factory overlay.

The HUD has no app speed threshold, but reverse, factory views, parking assist and screen-off still suppress it. Closing the screen side view also dismisses the HUD for that signal activation; it returns after cancelling and signalling again. HUD video is covered when frames stop arriving. Its optional camera output yields before screen output, mirror and recording when the session cannot support it. Diagnostics include display flags, the detected HUD, output mode, mirroring choices and recent HUD window state. The owner has confirmed live HUD video; physical fit and smoothness of these layout changes still need an on-car check. Factory speed/warnings/navigation may remain visible over the camera because the ordinary Presentation cannot mask a separate factory layer.

Side-camera correction and aim follow the selected picture mode. Under **Blind spot camera →
Popup window → Aspect ratio (width:height)**, choose Square (1:1, default), Portrait
(3:4), Landscape (4:3), Wide (16:9) or Extra wide (2.2:1). This setting applies only
to the separate popup. Left and right cameras share one saved position and shape.
Drag to place it and pinch to resize; position, height and aspect ratio are saved.
Pinch resizing retains the chosen ratio, and the normal window scales down
proportionally if needed to fit the screen. **Expand** temporarily fills the usable
screen height and half its width, on the side nearest the original window; on very
wide displays its width is capped at its height. It uses native overlay bounds and
system-bar insets, independently of the saved aspect ratio. **Restore** returns to
the exact original shape, size and position. The same expansion applies to a
blind-side replacement in Super mirror and the **360° window**; normal Super mirror
retains its existing aspect ratio. The 360° picture keeps its proportions inside
the expanded panel. Expanded bounds follow system-bar/display
changes without saving the larger geometry. Existing square sizes remain unchanged.
Corrected framing uses the actual window ratio rather than stretching the camera
pixels; changing shape can change the visible coverage. High aim angles can still
force a narrower view. Super mirror and HUD keep their own shapes and positions. If the head unit rejects the additional output,
the side window is closed and a message suggests using Use Super mirror when active. This feature
uses the turn signals; blind-spot obstacle alerts are not yet a trigger.

The side-view window has corner buttons matching the factory-style controls: **Expand**
at top-left toggles a larger view and restores the previous size; **Close** at top-right
dismisses only the current indicator activation. It returns after the indicator is turned
off and activated again, or when you signal the other side. Neither feature toggle is changed.
In replacement mode, Close restores the normal mirror. Normal-size separate windows still
support dragging and pinch resizing; expanded mode is temporary and restores on dismissal.

Use **Left image rotation** and **Right image rotation** to level a tilted side image.
Positive angles rotate clockwise, negative counterclockwise; edges are cropped to fill the
window. The controls apply in either window, including when Straighten picture is off, and
do not change recordings. Each defaults to 0°; camera orientation needs on-car calibration.

**Reset side-view settings** restores the default aim, speed threshold, display mode, timing,
size and saved position. **Reset Super mirror settings** restores its window, view and switching
modes, including camera labels shared with the side popup. The side-view reset includes all HUD tuning but leaves shared labels intact. Both reset actions are available while the feature is off and preserve its on/off state.

### USB startup and storage fallback

Use the designated dashcam/Sentry storage port: **USB-C on the 009** and
**USB-A on the 7X**. Other ports may lose power after you leave; confirm that
the selected drive remains available. The Storage page identifies both models
rather than assuming every car uses the 7X port layout.

With USB selected and **Allow internal storage fallback** off (default), an existing
recording request waits for a writable USB drive instead of using head-unit storage.
Detection runs in the background every two seconds for the first two minutes,
then every 30 seconds. Recording starts when a refresh finds the drive ready,
provided the original request is still permitted. Press **Waiting for USB → Tap
to cancel** to withdraw it; USB insertion alone does not create a recording request.

Under **Developer options → Recording internals**, explicitly enabling fallback
allows internal recording after a ten-second USB wait at recording start. If a
USB drive fails during an active recording, internal storage is offered to the
recorder only with this option enabled; other usable USB volumes retain their
existing priority. The internal-recording notification remains when fallback
actually happens. A fallback preference change controls new starts and future
failovers; it does not move or stop a file already being written internally.
Stop and restart recording if you need the current destination re-evaluated.

The choice is developer-gated and saved independently of enabling Developer mode.
Directly selecting **Storage → Storage location → Internal storage**, and explicitly
enabling **Relay write**, retain their existing separate developer permissions.
Old internal recordings are not deleted or migrated; with fallback off, select
Internal storage explicitly in Developer mode to browse them when USB is absent.

### Playback

Video playback shows every camera together, in sync; tap one to enlarge it, tap again to go back.
Photo playback uses the same layout.

### Send to your phone

While viewing a photo or a video segment, tap **Send to phone**: the app shows a QR code, your
phone's browser opens that file, and you save it. Nothing to install on the phone.

The file is served over your local network only while the dialog is open, from a random one-off
address, and only that one file. Both devices need to be on the same network — the easiest way is
to turn on the phone's hotspot and connect the car to it.

---

## Install

**Before you start:** you need to know how to sideload an app onto the head unit through App Lab.
This page does not cover that.

- **Original upstream builds:** [dts88's releases](https://github.com/dts88/zeekr-shortcut-car/releases).
- **Zeekr Companion:** download the desired APK from
  [Zeekr Companion releases](https://github.com/wysie/zeekr-shortcut-car/releases).
  New combined APKs are built from `main`; each release tag identifies its exact source commit.
  In builds with **Update channel**, choose Stable, Beta or Alpha under Settings → Check for updates. Stable includes stable releases only; Beta includes beta and stable; Alpha includes all three. Older builds with the beta switch still need manual alpha installation.

The fork retains the original application ID and public test signing key so its
APK can update an existing installation signed with that key. In-app update checks
query [wysie/zeekr-shortcut-car releases](https://github.com/wysie/zeekr-shortcut-car/releases).
The updater requires agreement between the tag and GitHub’s pre-release flag: stable tags have the flag off; alpha/beta tags have it on and must be allowed by the selected channel. Unknown and mismatched channels stay excluded. It offers the newest eligible version, never a downgrade. Fresh installs start on Stable; upgrades preserve the old beta switch, including its default-on behavior for existing installs. Alpha requires an explicit selection and is never inferred from the installed version. The setting row shows the current channel; its single-choice dialog explains the included releases. Checks still run only when you tap Check for updates. Legacy `fork-` / `personal-build-` tags and installed `-personal-alpha` versions remain comparable. The signing **private key** is public, so someone else can create
Android-accepted updates with the same identity. It does not establish that an APK
came from this maintainer. A private-key migration needs a separate installation/update
plan; the existing identity is retained for compatibility. The updater now requires
GitHub asset size and SHA-256 to match and uses HTTPS through redirects. Missing
or JSON-null digest metadata is refused before download; the verification error
covers both size and checksum failures. Old assets without a digest are refused
by the in-app downloader; manual alpha installs remain unchanged. Maintainer
follow-ups from dts88 PRs #58/#59 remove unused microphone translations and tidy
updater validation; [review and integration decisions](docs/upstream-contribution-followups-20261019.md)
record what was adopted from our merged contributions.

Download the `ZeekrCompanion-<version>.apk` asset from the release page and sideload it through App Lab.
When the installer finishes, tap **Back** at the top left, not **Open**. Otherwise the head unit's
installer gets stuck and later installs or updates fail.

Building it yourself needs JDK 17+ and the Android SDK (compileSdk 36):

```bash
git clone --branch main https://github.com/wysie/zeekr-shortcut-car.git
cd zeekr-shortcut-car
bash ./gradlew assembleRelease
```

The repo ships a public AOSP test signing key (password `android`). Override it with your own
through `ZEEKR_KEYSTORE`, `ZEEKR_KEYSTORE_PASSWORD`, `ZEEKR_KEY_ALIAS` and `ZEEKR_KEY_PASSWORD`.

---

## Recommended

**[CarPiano](https://github.com/jlunnn/CarPiano)** plays sound through the car's exterior speaker at any speed — no longer limited to Park like the factory feature. It's still early in development, but very promising.

Check your local rules before using the exterior speaker on public roads.

---

## Tips

### If the USB drive can't keep up

If recording stops working properly on the USB drive because the files have grown too large:

1. **Lower the frame rate to 10 fps first.** In **Settings → Recording & photos → Edit stream profile**, tap
   each camera under *Cameras on* and set **Frame rate** to *10 fps (cap)*.
2. **Still not working? Then lower the bitrate** for those cameras.

Changes apply from the next recording.

A dash cam's main job is a clear picture. At 10 fps motion looks choppy, but each frame stays sharp.

For anything else, [open an issue](../../issues) and attach a report from **Settings → System →
Diagnostics**.

---

## Limitations

- Built for the ZEEKR 7X. Only the camera recognised as the composite stream is split; a head unit
  that does not provide one gets nothing split.
- Factory features come first: the built-in 360° view, reversing camera and parking cameras can
  reclaim a camera at any time.
- Starting on boot and recording on launch are off by default. With the screen off, only the
  surround view keeps recording; the cabins resume when the screen turns on. Without Sentry Mode,
  recording stops when the head unit sleeps, a few minutes after you leave the car.
- Cropping a surround view is switched off for now.
- On-vehicle validation is ongoing. Automated tests cover the pure logic only; test in a stationary
  vehicle first.

---

## Credits and license

**GPL-3.0**, inherited from [EVCam](https://github.com/suyunkai/EVCam) by suyunkai — the code base
this app is forked from. The first commit in this repository is EVCam's complete working tree, so
every change since then is visible as a diff.

Splitting App Lab's composite stream into a grid and rendering it is implemented here from scratch.

Origins, third-party licences and boundaries: [NOTICE.md](NOTICE.md).

## Phone sharing improvements

Send to phone offers Wi-Fi, hotspot or tethering addresses instead of internal vehicle links. The dialog shows whether the phone connected and whether sending completed; recent attempts appear in Diagnostics. Both devices must be on the same local network. Sharing uses an unencrypted HTTP bearer link with a new 128-bit token for each selection and at most four simultaneous clients. The temporary server stops when the dialog is dismissed. Use a trusted local network; anyone with the active link can download the selected file. Includes Ferix’s HEAD/compression fixes; Android timeout reports still need device verification.

## Vehicle signal scanner

Enable Settings → Developer options → Record all vehicle signals for a read-only scan of function changes and unfamiliar sensor events. Blind-spot-like functions are polled when available. It stops after three hours or when Developer options are locked, skips sensors already used by the app, and exports results through Diagnostics. Pending callbacks are capped at 256 and excess events are dropped under overload; the export is not a complete event history. The scanner also records function-change notifications as `N:` (function ID and time, without an alarm state or side) and byte callbacks as `B:` (ID, zone, full payload length and up to 64 bytes of hex, explicitly marked when truncated). These payloads are copied into immutable text before deferred processing. They do not trigger a camera, interpret bit fields or add vehicle reads/writes. For the separate scanner, both retain the existing callback queue, time, line-count and export limits. This is a research tool: no obstacle-alert trigger or factory 360 video feed has been identified.

For an entire drive without manually saving a report, use **Drive logging to USB**
in that same Developer category. You do not need to enable the separate scanner
switch: the drive session subscribes automatically and leaves its preference alone.
Start with the USB inserted, wait for **Saving logs to USB**, drive, then switch it
off and wait for **Saved** before unplugging. The session creates `signals-*.log`,
`app-*.log`, `lifecycle-*.log`, `session-*.log` and automatic start/stop Diagnostics
in a unique `ZeekrCompanion/Logs/Drive_…` folder. No camera/video or full-system
logcat recording is added. It collects from activation onward, without requiring
the Diagnostics screen. Start/stop snapshots supplement the raw files; the normal
Diagnostics summary remains filtered.

Drive logging retains high-frequency observed events and continues beyond the
private scan cap. Writes run off the camera/UI callback threads, flush and sync
every two seconds, and roll into 8 MiB parts without deleting older evidence.
Stop limits are eight hours, 256 MiB per session, less than 64 MiB free, USB/I/O
failure, Developer mode disabled or Android service termination. It locks to the
chosen USB volume, does not silently use internal storage or another drive, and
never starts on boot or automatically resumes a killed session. Android can stop
it earlier; unplugging/ejecting may terminate the process. The last two seconds,
queued events or unfinished snapshots may then be missing; status marks the
session interrupted rather than Saved. Existing callback queues remain bounded,
with dropped-event counts written as GAP markers. Very long text entries and byte
payloads retain explicit bounds. Only signals exposed by the head unit are visible;
this still does not establish a decoded blind-spot warning or automatic trigger.

## Hard-braking footage lock

Settings → Storage → Lock footage on hard braking is on by default and depends on Lock footage. It protects every camera’s footage around a detected hard stop (10 seconds before and after), using available speed/brake signals. Automatic locks have a 10% recording-space budget; oldest automatic locks can be released, while manual and flash-to-pass locks are preserved. Signal behavior on the 009 needs verification.

## Custom clip export

In playback, tap Save clip to save 15 seconds before/after the current point, or choose a range starting here, move to the end and tap Save to here. Custom clips are limited to 10 minutes and copied without re-encoding; the start may include preceding keyframe footage. Clips are saved under ZeekrCompanion/Videos/Clips and can be sent to the phone. A codec change stops concatenation. Saved clips and concurrent exports are never overwritten. Swipe left/right to switch cameras when a surround cell is enlarged.

## Recording follows the driver

Settings → Recording & photos has **Start recording when you get in** (off by default) and **Stop recording when you leave the car** (on by default). Explicit saved choices are retained. An occupied driver seat in P (or a shift out of P) requests recording through the current upstream coordinator only with driver-start enabled. Leaving in P stops after 30 seconds with an empty seat, or sooner when the screen is physically off or Sentry is armed. A manual stop holds for the drive; leaving clears that hold and the saved resume intent. Unknown seat/gear readings reset the empty-seat countdown. Sentry-based stopping requires a known empty driver seat; known movement contradicting P blocks this automatic stop. Disconnecting the vehicle interface clears the driver-away gate. Other screen-off rules still apply. The 009’s seat/gear signals need on-car verification; disable either switch if its behavior does not suit your firmware.

Autolaunch opens the app; starting a new recording still requires Record on launch, driver-start, or an explicit manual command. Automatic recording requests retain their origin across process restarts and recheck the originating switch on wake-up and after storage/camera waits. Turning that switch off cancels its pending starts and future automatic recovery; an active clip continues until stopped. Manual recording can still recover from an interruption, and pressing Stop cancels recovery. Saved requests from older builds have no known origin; with both automatic-start switches off, those older requests are discarded. Diagnostics show the effective driver settings and recording-request origin.

## Turn arrows on side cameras

The side camera displays an upright left/right arrow between the top corner controls, including during Super mirror replacement. It follows the indicator display in green/grey; if blink telemetry is unavailable it stays green. It does not indicate an obstacle detection. Camera labels remain independently configurable.

## Night theme and floating record button

Settings → Floating button → Actions offers **Tap**, **Double tap** and **Long press**.
Each offers, in order: Open the app, Start / stop recording, Take a photo, Lock current recording, Super mirror on / off, 360° on / off and Do nothing. The 360° action opens/closes the existing manual popup session, including a temporarily suspended session, without bringing the app forward or adding an automatic-start preference. It uses the same overlay permission and factory-priority rules as Windows → 360° window. Existing gesture choices are preserved by their saved keys. Double tap defaults to **Do nothing**, preserving immediate single taps
and existing tap/hold assignments. Assigning a double-tap action makes a single
tap wait briefly for Android's double-tap timeout; a recognised double tap runs
only its own action. Long press stays at 600 ms. Dragging, cancelled/multi-finger
touches and hiding the button cancel pending gestures. Actions apply without an
app restart; position lock still allows gestures but prevents moving the button.
See [gesture behavior and validation](docs/floating-button-gestures.md).

Recording icons, progress arcs and status text use one shared foreground red per
theme across the main screen, header stop controls, floating button, gallery and
Quit actions. Soft backgrounds and progress tracks remain quieter tints. The
floating button and its duration label follow the app's Follow system/Light/Dark
choice live, including when it differs from the head unit's theme. Saved overlay
opacity still applies to the whole floating button. Solid red destructive buttons
use a contrasting foreground in each theme; recording state and gestures are unchanged.

Night mode uses a black page/main panel with darker settings rows and a distinct selected-row shade. Day layout and primary colors are retained; setting descriptions have stronger contrast. The floating record button has a smaller, thinner icon and no shadow; its background stays neutral while live recording uses a red ring/stop icon. The current upstream waiting-for-cameras state remains grey.

## Whole-project review and data handling

The [whole-project review](docs/project-safety-review.md) covers recording/storage,
vehicle signals, camera scheduling and displays, recovery/sleep, playback/repair,
sharing, permissions, updates, signing and build configuration. This is a source
review with local tests; 009 factory takeover, native latency, USB faults and battery
behavior still need on-car checks. The DIY view remains uncalibrated; preview and calibration have no P-only or speed gate.

Developer Relay write now buffers under the app's private `files/temp_video`, with
recovery scans also checking the previous `cache/temp_video` location. Nonempty
clips are not discarded merely for being under 500 KB, and pending footage is not
expired after an hour. Copies use a synced temporary file, verify SHA-256 and publish
without knowingly replacing a destination. Both files survive a different-content
filename collision. This adds USB reads and needs sustained recording/transfer tests.
Buffers can fill internal storage and recording can then fail; clearing app data or
uninstalling removes them. Transfer destinations and pending lock retries are not yet
persisted across process death. Keep the original USB attached for recovery.

Developer MP4 repair keeps the original filename and edits its index after saving a
verified backup under `ZeekrCompanion/Videos/repair-originals/<filename>.original`. It refuses to
edit if that backup cannot be made or conflicts with an existing different backup.
Repair bounds its sample tables (100,000 samples), reference index (8 MiB), decoder description (1 MiB) and validation buffer (8 MiB); oversized files are refused. Backups are retained, consume additional USB space, and require manual recovery or
cleanup; they are not included in automatic recording rotation.

Footage, diagnostic files and transient recording intent are excluded from Android
backup/device transfer; ordinary preferences remain eligible. Diagnostics may include
location, VIN/device identifiers, storage paths, vehicle state and logs. Save/share is
explicit; review an exported report before giving it to another person. No vehicle
actuator writes or automatic diagnostic upload were found in the reviewed app paths.

Screen-off recording is a Developer option, off by default. Its wake lease uses the
selected duration plus the existing bounded camera-close allowance, with an additional
off-UI timer. This reduces dependence on UI timer delivery; whole-process or vendor
power-service stalls can still exceed software timing expectations. Automatic UI
recovery waits while a known reverse/factory-camera/parking-assist view is active;
unknown vendor flags cannot prove that a factory view is absent.

The [menu review](docs/menu-review-2026-10-15.md) records implemented layout fixes
and the implemented navigation cleanup; the current hierarchy is in the [settings map](docs/settings-menu.md).

Camera status is available from the main preview chip and System. It keeps the recorder’s live/planned source count visible and lists source assignments, producer state, recording participation and explicitly labelled saved quality. Waiting previews now distinguish known contention, recovery, pause and stopped-recovery states; prolonged waits show elapsed time and point to Camera status / diagnostics. Covers remain opaque and recording truth remains independent of preview freshness. Status is read-only and never forces camera retries or interrupts healthy recording. The Settings footer names the source whose quality it summarizes; the main footer now concentrates on storage and camera problems.

Settings now distinguish immediate actions from navigation, confirm resets with their exact scope, and offer separate HUD picture and placement resets. Recording setup reads Vehicle model → Camera sources → Recording sources & quality → Preview layout, with the technical stream profile still available. A persistent settings banner identifies unapplied camera/preview choices and explains that quitting stops recording. Updates is a destination; Check for updates remains its action. USB logging has explicit Start/Stop actions with live status, and the 360 switch describes a session window. No existing tuning or source preference is reset by opening these pages.

HUD settings group picture source, custom framing, position/size, tone and advanced options. An Edit screen picture shortcut shows inherited framing without changing saved custom HUD values. System → Troubleshooting links Camera status, diagnostics, an explicit USB session page and developer-only camera/display research; viewing these pages starts no probes or logging. Diagnostic exports retain their last saved path/error and separate Copy location / Send saved report actions across recreation. About links have 48dp targets and a persistent Copy link fallback when no browser exists. The recording floating button temporarily hides on main/settings and supported About/report screens with reserved stop controls, then returns at its saved external placement; playback and other unsupported screens retain their overlay.

Long selected values now move below their setting title when a side-by-side row would clip either text. Short values retain the existing trailing placement and native font size. The camera-status chip uses a neutral colour when no sources are writing. New status, reset and troubleshooting copy is available in English, Chinese and Malay. These presentation changes preserve saved picture angles, mappings, recording choices and preview layouts.
