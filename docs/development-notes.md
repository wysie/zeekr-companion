# Companion development history

Historical implementation notes, retained from the README. These describe the
work at the time; use the current README and release notes for current behavior.
Relative links in the archived notes are resolved against the repository root.

## Development notes

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
storage durability; see the [codebase audit and validation](codebase-audit-20261021.md).

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
[regression evidence and remaining limits](storage-contention-20261022.md).

In alpha 60, the experimental 009 fit combines the earlier garage reference with matched
flat-road features from the owner’s 21 October raw/factory captures. Held-out
image-reference alignment improves for all four cameras, including the front;
CPU/GPU share the correction. Existing Automatic/009 selection uses the revised
fit without a reset; saved Manual tuning, sources and framing stay intact.
Residual joins/raised-object distortion remain, and on-car confirmation is pending.
See the [alignment evidence and limits](009-image-fit.md#roadground-alignment-refinement--21-october).
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
these states. See [Apply verification and limits](apply-camera-changes.md).
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
[factory-style UI behavior and validation](factory-style-ui.md).


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
See the [main-screen verification notes](main-screen-validation.md) for coverage and limits.

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
