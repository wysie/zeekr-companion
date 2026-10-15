# Owner feedback: Zeekr 009

2026-10-13, personal APK 2.11.6-personal-alpha:

- Camera labels reported working well.
- Side camera replacing Super mirror reported working well.
- Separate side popups observed in different left/right screen positions.
- Owner clarified that the factory display below 30 km/h uses one location for both
  indicators and shows a top-down 360-degree view. Our desired content is the side
  camera; the desired placement behavior is one shared location.

2.11.7-personal-alpha changes the independent popup to one saved position for both
cameras and adds one-finger dragging. Pinch resizing remains available. Default
placement is horizontally centred at the configured height; the owner can drag it
to match the factory window's location. The exact factory coordinates were not
measured. Shared placement and dragging await testing on the 009.

This feedback does not establish exhaustive factory-camera takeover, long-drive
recording continuity or all simultaneous-output cases.

Owner supplied two photos showing opposite left/right scene tilt inside upright
side windows with Straighten picture enabled. The APK version and exact aiming
values for the captures were not supplied. The image alone does not isolate lens
mounting orientation from the virtual camera projection.

2.11.8-personal-alpha adds independent left/right image rotation (default 0°),
applied in either window and correction mode, plus reset actions for side-view
and Super mirror tuning/layout. Resets retain the current feature on/off state.
An uncorrected-camera comparison remains pending to identify the tilt's cause.

Owner supplied a factory popup reference showing an expand control at top-left,
a close control at top-right and rounded video corners. They specified that
closing is temporary, with a new indicator activation showing the view again,
without disabling Super mirror or the side-view feature.

2.11.9-personal-alpha adds these controls to the independent side popup and to
the mirror while it temporarily shows a side camera. Close dismisses the current
activation; off-then-on or a change to the other indicator rearms it. Expand/restore
is temporary and returns the normal geometry on dismissal. Native buttons use
48 dp targets. Side-camera labels sit at the bottom to keep the corner controls
clear. No Android device was connected during development, so the final native
rendering and touch behavior await the owner's on-car check.

The owner reported stepped black patches in the separate left-camera popup at
Aim back 80°, appearing above roughly 50°, and confirmed they disappear with
Straighten picture off. At 90° FOV / 80° rear / 15° up, the original projection
collapses 19 of its 100 tiles onto an image edge. Why it previously appeared
clear at 80° is unresolved; the exact other settings and window geometry then
were not supplied.

2.11.10-personal-alpha fits the corrected viewport inside the camera image before
sampling, retaining the selected rear/up aim and window aspect ratio. It narrows
FOV when needed rather than clamping individual corners, skipping tiles or
stretching edge pixels. The example above fits at about 20.5° horizontal FOV,
a substantial reduction in coverage, explained in the View width setting.
The saved FOV is retained and used again when the aim allows it. Both popup
and mirror replacement use the fix; ordinary Super mirror correction is unchanged.
Regression tests cover the reported collapse, both camera directions, extreme
aiming, different mirror shapes, and recalculation after configuration changes.
The APK still needs an on-car check at the reported angle.

2.11.11-personal-alpha adds a read-only “High angles zoom in” note below the
side-view aiming sliders, with the 80° rear / 15° up / 90° square-window example,
reduced-coverage explanation and guidance to lower the aiming angles. Aim back
also points to the note. This is a settings-text change; rendering is unchanged.

The owner confirmed that the side view remains hidden below 30 km/h with the
minimum speed set to 0 when an indicator triggers the factory 360-degree popup.
This matches the existing factory-view priority check, which runs before the
minimum-speed rule. 0 removes only our speed restriction; it does not bypass
factory-view priority.

2.11.12-personal-alpha renames the setting to “Minimum opening speed” and
explicitly explains that reverse, parking assist and factory views still hide
our side view, including the factory low-speed indicator-triggered 360 popup.
Behavior is unchanged.

The owner reported that the zoom and speed notes were not visible after
installation. The downloaded public 2.11.12 APK was independently inspected:
versionCode 295, matching release checksum, both note strings present, and both
settings entries wired in the compiled preference XML. The version actually
installed on the head unit remains unconfirmed, so the display report is not
yet explained.

2.11.13-personal-alpha puts the black-patch / reduced-coverage explanation first
in the Aim back summary and moves the full example note directly below that
slider, before Aim up. The speed explanation remains on Minimum opening speed.
These are help-text and placement changes, not rendering changes.

2.11.14-personal-alpha moves all side-view controls to a top-level Blind spot
camera settings section. The main switch is Automatic blind spot camera; the
display option is Use Super mirror when active. Existing preference keys and
saved values are retained. The speed-priority explanation and high-angle zoom
note stay with their controls. The feature still responds to turn signals.
This build also integrates upstream 2.11.3-alpha (571a2a9), including the 30-second
cabin camera dark hold and recording startup when the disk becomes usable.
The independent contribution passes 1,084 tests and release build checks against
that upstream base. Native layout visibility remains unverified on the head unit.

## Ferix feature ports — 2026-10-14

All seven feature groups are integrated into main on focused branches with source
credit. Current upstream recording/channel logic is retained. No new vehicle test
has been reported for these ports. Hard-braking locks and both driver recording
switches default on; signal scanning remains opt-in. Android phone transfer still
needs verification, including whether a request reaches the head unit.

Final local validation: 1,146 JVM tests passed (0 failures/errors/skips) and assembleRelease passed, including release vital lint. The APK retains application ID io.github.dts88.zeekrshortcut and the existing signing identity. This does not establish on-car behavior for the new ports.

## Side-camera mirroring and HUD — 2026-10-14

Screen mirroring defaults off and covers the popup and Super mirror replacement. HUD is opt-in, with independent mirroring on by default. Local validation: 1,166 JVM tests passed, zero failures/errors/skips; assembleRelease and release vital lint passed. Settings XML and resource keys were checked in Chinese, English and Malay.

Regression tests cover known-size/named HUD selection, refusal of unknown display ID 3, main/private/non-presentation/cluster/ambiguous displays, placement, independent speed thresholds, factory-view priority, screen fallback, and HUD-only surround ownership/release without losing screen or recording requests. Existing generic holder tests include the HUD.

No head unit or emulator is connected. Native UI, frame timing, 009 HUD availability, screen fallback and picture orientation remain unverified on the car. Test while parked: screen mirror on/off with each indicator, both screen display modes, Screen/HUD/Both output, factory-camera priority, X remaining closed until cancelling/re-signalling, and reset restoring Screen/off-screen-mirroring/on-HUD-mirroring defaults. If HUD is blank, export Diagnostics with HUD enabled in the car and HUD output selected.

## DIY 360 prototype — 2026-10-14

A manual ground-plane 360 display now uses snapshots of the existing composite
preview. The camera session and recording paths are unchanged. All mounting/lens
defaults are estimates; this is not the factory 360 feed or a measured 009
calibration. The feature starts off, with saved independent calibration and reset.

Local validation: 1,180 JVM tests passed, zero failures/errors/skips; signed
assembleRelease and release vital lint passed. Fourteen new tests cover projection,
source orientation, camera coverage, normalized blending, vehicle masking and
composite boundary handling. A synthetic fisheye ground-grid fixture was rendered
through the actual Java renderer and visually inspected: grid lines and coloured
landmarks align across views, with expected low-resolution sampling aliasing.
This fixture is not car footage. Resource XML parses in Chinese, English and Malay.

No connected vehicle or emulator was available. Pending on-car checks: actual
TextureView readback and timestamp progression; main-screen button/dialog layout;
camera order/orientation; manual tuning and calibration reset; factory takeover
then recovery; source loss/stale cover; switching views and backgrounding; memory,
UI responsiveness and recording continuity while the five-fps view is active.
Tomorrow's Diagnostics and original surround composite can refine these estimates.

## Camera safety follow-up — 2026-10-14

The review found silent copy reuse, no independent side-popup stale cover, slow
Super mirror stale detection, duplicate-source calibration and missing parked-only
prototype guards. Fixes and remaining limits are in [the safety review](camera-safety-review.md).
The DIY view now requires a connected vehicle interface reporting P. Screen popup,
HUD and Super mirror use advancing texture timestamps with a 500 ms threshold
and 100 ms cover checks. This is not an end-to-end latency guarantee.

Twelve new regression tests cover copy validation, unique feeds, policy and
timestamp/expiry behavior. Full suite: 1,192 tests, no failures/errors/skips.
No attached vehicle/emulator: native texture copies, new covers/recovery behavior,
P gating, factory takeover, HUD and recording continuity still need testing.

## Whole-project review — 2026-10-14

Baseline: main f785483 (2.11.18-personal-alpha). The audit and fixes are on
review/project-safety for combined main integration, preserving PR #17/#19 heads.
Full report: [project-safety-review.md](project-safety-review.md).

Local tests: 1,217 JVM tests passed, zero failures/errors/skips; signed release build
and release vital lint passed. New cases cover equal-size/different-content footage
collisions, byte-exact copies, short-file retention, failed protection-list replacement,
repair backup survival, unknown seat/gear, contradictory movement, Sentry with unknown
seat, updater digest/size/HTTPS rejection and phone-sharing worker saturation/header
sanitization. JVM Android stubs do not prove native UI or vehicle behavior.

Full Android lint has 15 remaining MissingTranslation findings for the intentional
English About-page fallback in Malay. The 19 non-translation errors were cleared;
correct default-locale metadata removed 933 requests for Chinese entries already
provided by the default Chinese UI strings. No baseline or suppression was added. OSV returned no vulnerability matches for 91 resolved
runtime Maven coordinates at the review date, excluding proprietary SDK/firmware.

On-car checks remain: simultaneous recording and all camera views; stationary factory
camera takeover and recovery; frame-loss covers and retained textures; USB disconnect,
full disk, filename collision and process-death relay recovery; repair backups; driver
seat/gear/Sentry behavior; Android phone sharing; installer integrity errors; screen-off
lease expiry and parked 12-V behavior. Current signing identity remains the public test
key. No new 009 or emulator validation was performed.

## Upstream 2.11.4 and About attribution — 2026-10-14

Merged dts88’s upstream e4d0e0d0ddfef81a7fdb729ad57810fecc95d52c into the
combined fork. upstream-main is fast-forwarded to that exact, unmodified source.
The merge preserves popup/HUD output declarations and rejection priority, camera
freshness guards, relay/repair fixes and all other personal features. Existing
PR #17/#19 branch heads remain unchanged.

Upstream adds no-frame camera reopening, global camera-close serialization,
scheduler-managed photos/exit, camera step/session statistics and signal-change
age diagnostics. Signal-change age is diagnostic metadata, not an expiry policy
for cached readings or a measure of camera exposure latency.

About identifies wysie’s personal custom fork, recommends dts88’s original and
more thoroughly tested version, and links to both repositories. EVCam/GPL credit,
original acknowledgements and Ferix/CAMCORDER credit are retained. PayNow
9271 6786 is explicitly a voluntary tip to the original author, dts88. English and
Chinese About strings were checked in the packaged APK, along with resource XML
and translation parity; the page was not rendered on a head unit or emulator.

Local validation: 1,245 JVM tests passed, zero failures/errors/skips; signed
assembleRelease and vital lint passed. Full lint has 18 MissingTranslation
findings for the intentional English About fallback in Malay and no other error
category. Version 2.11.20-personal-alpha, code 303, retains the established
certificate SHA-256 a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc.
New camera scheduler behavior and the About page still need an on-car check;
no additional vehicle compatibility or safety validation is claimed.

## Owner Diagnostics and 009 HUD mapping — 2026-10-15

The owner reports that HUD output falls back to the screen popup and that the DIY
360 view opens but is misaligned. Diagnostics from 2.11.20 show public,
presentation-capable, ON displays 0 (2560×1600), 2 (1920×720) and 3 (1280×640).
The two secondary displays both have the anonymous name HDMI Screen. HUD output
was selected, but the automatic detector found no candidate; window state was
never. This is a selection failure, not evidence of a rejected HUD window.
The owner authorized trying display 3 directly, without an identification marker.

An opt-in Zeekr 009 HUD switch under Blind spot camera → HUD picture now selects
only display 3 with the exact 1280×640 dimensions. It defaults off; automatic
selection remains unchanged. Existing public/presentation, powered-on and named
cluster exclusions apply; the explicit mapping additionally rejects named
rear/passenger displays. Missing/rejected output uses the existing screen
fallback. Changing the mapping detaches the old window, and reset restores auto.
Diagnostics identify the selected mapping. Physical destination, simultaneous
factory HUD content and native camera rendering remain unverified.

The owner also supplied two original 1280×5140 surround frames and geometry
notes. Four 1280×1280 lanes and five 4-pixel separator bands match the splitter.
Side-camera scenes show tilt; the DIY calibration is still using estimated
defaults, independent of the owner's +30°/-30° side-popup image rotations.
An offline rendering with the existing projection reproduces stretched ground
and mismatched seams. No calibrated replacement defaults are established by
these captures. The ordinary photo action still saves the four-camera composite;
saving a stitched 360 preview remains a separate pending feature. The signal
scanner had never run in the supplied Diagnostics.

Local validation of the 009 mapping: 1,250 JVM tests passed with zero failures,
errors or skips, and signed assembleRelease with vital lint passed. Five new
tests cover the supplied display inventory, explicit selection, exact ID/size,
restricted/known-other-role exclusion and refusal to substitute a different HUD.
The release is 2.11.21-personal-alpha, versionCode 304. On-car output remains
pending; the mapping does not establish factory HUD coexistence or permission.

## Recording starts after car wake-up — 2026-10-15

The owner reports recording starting on car power-on/autolaunch with Record on
launch, driver-start and driver-stop switched off. They clarified that the
supplied Diagnostics predate switching those options off. The earlier report's
DRIVE start at 23:48:59 therefore does not prove the later off-state failure:
driver keys were unset then, and that version defaulted driver-start on.

Code inspection found persisted recordingWanted had no source, wake-up recovery
did not recheck the originating automatic switch, pending starts did not recheck
settings after async storage waits, and the driver's activity-launch extra was
shared with manual floating starts (which clear a manual-stop hold). These gaps
are corrected with a persisted manual/launch/driver origin, checks before
requests and async completions, immediate pending cancellation when switches
change, and a separate driver extra that never marks a start as manual. Existing
active clips are not stopped by start-switch changes. Known manual requests keep
their interruption recovery. Legacy unknown-source requests are dropped with
both auto-start settings off. Unset driver-start now defaults off; explicit
saved settings remain intact. Diagnostics report effective driver switches and
recording intent/source. No new on-car trace establishes the exact later trigger.

Eight new tests exercise origin persistence across process restarts, independent
automatic permissions, switch revocation during waits, legacy requests with all
automatic starts off, manual recovery/manual stop, durable source changes and
end-of-drive cancellation. Full suite: 1,258 tests, zero failures/errors/skips;
signed release build and vital lint pass. Native car wake-up still needs a check.
Packaged as 2.11.22-personal-alpha, versionCode 305, with the existing signing identity.

## DIY 360 refinement — 2026-10-15

The owner's factory and prototype photos show broadly corresponding front/rear
scenes, but the prototype has distorted ground, seams and pixelated edges. The
projection reproduces this offline using the supplied original frames. A limited
experiment with side rotation, forward mounting offsets and lower heights/tilts
did not establish a reliable calibrated preset. No new geometry defaults are
claimed or applied. Measurements or identifiable ground correspondences remain
needed; raised walls/cars are not flat-ground calibration targets.

Bilinear sampling now interpolates within each source lane, validating all used
pixel neighbours and preserving separator exclusion. Readback size, cadence,
500 ms freshness and 40 ms slow-readback pause are unchanged. While 360 is active,
the main photo action saves a fresh stitched PNG and matching calibration/state
note to ZeekrShortcutApp/Photos/360. Blocked/stale views are refused; saving is off-thread,
limited to one snapshot at a time, and does not modify the camera or recordings.
Three new interpolation/sentinel tests and existing lane-boundary tests pass:
1,261 JVM tests, zero failures/errors/skips. Native UI/export and interpolation
cost still need on-car checks. The geometry mismatch remains unresolved.
Packaged as 2.11.23-personal-alpha, versionCode 306, retaining the existing signer.

## DIY 360 resolution — 2026-10-15

The owner requested higher detail because the original 256×1024 readback and
320×480 output look blocky. The calibration dialog now has Low, Balanced and
Full resolution choices. Balanced is the unset default (512×2056 → 640×960 on
the supplied 009 stream, at most 5 fps). Full requests the available live preview
at native dimensions (1280×5140 on this stream) and renders 960×1440 at about
3 fps. Full source copies are bounded to 8 MP/8192 pixels per dimension; output
lookups are bounded to 2 MP. Small sources are not enlarged during readback.
These are preview snapshots, not repeated camera JPEG captures; actual detail
depends on the producer/preview and remains unverified on the car.

Readback over 40 ms, frame extraction/render over 150 ms, or memory-allocation
failure steps Full → Balanced → Low for the activation; Low pauses if still too
slow. The first slow native call cannot be interrupted. Lookup construction is
timed separately and can be reused after its initial frame expires. Calibration
edits do not reset an already-required quality fallback. A new activation or
explicit quality change retries the requested level. Frame-owned buffers and
revision/texture checks prevent a previous-size result publishing after changes.
The existing parked/factory/stale guards remain. Saved photos use active output
dimensions, with the warning sized for the export. Last quality/size/timing
diagnostics remain available after leaving the preview.

Ten new tests cover source-aspect/native bounds, unset/invalid choices, monotonic
fallback, timing limits, full-size rendering of four lanes with separator
exclusion and output-allocation bounds. Full suite: 1,271 tests, zero failures,
errors or skips. Signed build/vital lint pass. Native copy speed, heap use,
full-size export and recording continuity await owner testing. Geometric
misalignment remains unresolved.
Packaged as 2.11.24-personal-alpha, versionCode 307, retaining the existing signer.


## App folder layout (2.11.25-personal-alpha; vehicle check pending)

New files are under ZeekrShortcutApp/Videos, Photos and Logs on the selected volume.
Clips are under Videos/Clips, stitched PNG/TXT pairs under Photos/360, raw JPEG/TXT
pairs under Photos/Raw. Export fresh Diagnostics to Logs. Existing files in old
DCIM/EVCam_Video and DCIM/EVCam_Photo are listed without moving them. Confirm a
new ordinary photo and short manually started recording appear under the new root,
then confirm old media plays and old locked items remain protected. Phone sharing
should work for both old and new media. Use disposable test files if checking manual
deletion; do not lower storage limits merely to provoke deletion of real footage.
Private session logs/relay buffers remain in app-private storage; diagnostics exports
include session logs as before. Factory Sentry paths are unaffected.

## 15 October daytime export: camera selection mismatch

See [diagnostic findings](009-diagnostics-2026-10-15.md). The recording failure was
an empty intersection of live Surround and selected Front cabin, before files or
encoders were opened. Restore Surround ON / cabins OFF and All Auto mapping, then
fully exit and reopen. After the channel-selection fix, cabin-only probing should
open the selected cabin channel rather than silently substituting Surround. Only selected
texture listeners count toward readiness, so hidden unused panes do not block
initialization or resuming from the background.
Validate Front-only, Rear-only, Surround + Rear and normal Surround playback/photo/
manual recording while parked. Changing channel switches in a running session must
show the restart instruction instead of repeatedly reporting generic start failure.
The supplied PNG was Balanced; select Full explicitly for the next quality comparison.


## 2.11.27: developer camera and display tools (vehicle test pending)

The owner requested a batch alternative to switching camera IDs individually,
and HUD identification as well. The new Developer page captures one representative
image per available Camera2 ID and exports numbered cards for public presentation
displays. Neither discovers private factory buffers or screenshots the HUD.

Park in P, stop recording and close the factory camera. In Settings → Developer →
Camera and display tools, run **Export all camera feeds** once. Wait for the saved
path; export the whole `ZeekrShortcutApp/Developer/CameraScan_…` directory, including
`report.json` and any skipped/error results. Profile switches and camera mappings
should remain unchanged; confirm normal preview and manual recording recover
afterward. Press Stop once during a second scan and confirm it closes cleanly.

Run **Identify displays and export cards** separately. Note the numbered card seen
on each physical screen/HUD, including a card unexpectedly appearing on the main
screen or no card visible. Check automatic dismissal after 10 seconds and Stop.
Export `Developer/DisplayTest_…` alongside these observations; its PNGs show the
cards offered, not proof of actual physical routing. Repeat only while parked;
leaving the page, factory-camera activation, screen-off or loss of P cancels the
test based on the available vehicle readings.

JVM checks cover the exclusive lease, delayed closure, channel close ordering,
YUV stride/offset packing and allocation bounds. Native stream combinations,
Presentation placement and HUD routing await the owner's vehicle test. Existing
PR #17/#19 heads remain unchanged; no owner diagnostics or images are published.

Release validation: 1,294 JVM tests, signed release assembly and vital release lint pass. Native Camera2 and Presentation behavior remain untested on the car.


## 2.11.28: gear/speed gate removal (2026-10-15, vehicle test pending)

At the owner's explicit request, remove the P-only/speed restriction from the
Developer camera scan, display identification, DIY 360 preview and all calibration
controls (including resolution, sliders, source/mirror selection and reset).
Changing P to D/N or losing a gear/speed reading alone no longer closes these tools
or blanks the DIY preview. Connected vehicle interface is still required. Reverse
continues to suppress the DIY picture, and known factory-camera activity,
screen-off/hidden state and stale-frame/resource checks retain priority. Camera
scanning still refuses recording/photos and waits for confirmed native closure.

The earlier dated parked-only instructions above describe previous releases.
For the next vehicle check, confirm D/N and unknown gear do not independently
block the tools or calibration, then verify factory-camera takeover, closing the
page and screen-off still cancel/hide as appropriate. No change was made to
firmware behavior or the app's general foreground/background lifecycle.


## 2.11.29: system-theme camera controls (vehicle check pending)

The owner's factory screenshots show light popup controls in the light system UI
and dark controls in the dark UI. The fork used fixed dark control, label and
arrow backgrounds. The new theme follows Android's reported uiMode, with day/night
colour resources and live view configuration handling. This covers the separate
side popup, side view replacing Super mirror and regular Super mirror labels;
projected HUD labels/arrows retain their existing dark backgrounds. Video pixels,
recording, window position/size and temporary close/expand state do not change.

Check both Android day/night modes, then switch while a side popup is open and
while expanded. Confirm expand/restore and X still work, left/right labels and
active/grey turn arrows remain readable, and Super mirror replacement updates too.
Verify there is no camera reopen or recording interruption during the repaint.
The 009's factory theme must be published through Android uiMode for automatic
matching; the screenshot alone does not establish that firmware behavior.

Existing JVM regression/resource-parity checks and release lint pass. Measured
minimum contrast over black/white video exceeds 11:1 for button icons, 8:1 for
label text and 3.8:1 for arrows in either theme. Native rendering and live theme
switching await the owner's 009 check; no native screenshot verification was
available during this change. Existing upstream PR heads remain unchanged.


## Fresh-install permission correction (2026-10-15)

Native API 32 emulator testing found startup repeatedly requesting RECORD_AUDIO
although the manifest does not declare it and the app has no audio recorder.
The ungrantable permission led MainActivity to finish after permission setup.
Remove that stale request and the Developer microphone-permission row, without
adding microphone access. Existing car installations may receive different OEM
permission treatment; this finding is reproduced on a fresh emulator install.


## 2.11.30: settings layout review (2026-10-15, vehicle follow-up pending)

The owner supplied a photo of the Surround switch clipped by its long card name.
An API 32 tablet emulator at 2560×1600 reproduced that exact clipping. Camera
names/custom badges now share a weighted column independently of the full-size
switch. Quality badges move below their titles. Cards stack when the content
pane is narrow, with margins reset when changing between row and column layouts.
Shared segmented options wrap instead of truncating one line and use equal-height
48 dp targets; Diagnostics actions can grow. The main action rail scrolls when
shorter than its content. Switching from an editor to another category clears its
nested history so the section title and Back/menu button match the visible page.

Three real Android instrumentation checks pass at font scale 1.3: long Surround
name + custom badge leaves the complete switch accessible; card rows reflow wide
→ narrow → wide without shifted margins; long option labels wrap, stay aligned
and deliver selection, with disabled controls remaining disabled. Native UI
screens were inspected at 2560×1600 and 1920×1200. MainActivity retains its existing
1.2 font override; the instrumentation tests use independent 1.3-scale contexts
rather than claiming a system font setting overrides that existing behavior.

These checks establish Android layout/interaction, not vehicle camera, USB or HUD
compatibility. Please confirm on the 009 that every card switch is fully visible,
long option labels and diagnostics buttons align, the category title is correct
after leaving the editor, and Settings remains reachable in a shortened app window.
The unidentified button in the owner's recollection cannot be identified with
certainty without seeing that page. No owner photos/diagnostics are published.


## Side-popup startup presentation (2026-10-15, 009 check pending)

The owner reported a roughly half-second “Waiting for live camera” flash before
the side-camera image. The freshness cover was introduced in 2.11.18 and shows
while startup geometry/frame progression is unavailable. Its 500 ms expiry is a
stall threshold, not a mandatory startup delay. No timing log was supplied for
this occurrence, so the actual camera/session startup duration remains unknown.

The separate overlay now remains attached, transparent and touch-through while
its requested activation starts. Geometry plus advancing texture timestamps reveal
it immediately. After 750 ms without readiness (checked every 100 ms), reveal the
existing waiting/close UI. The service still regards the startup request as showing,
so repeated telemetry does not restart startup or defeat temporary dismissal.
Cancellation hides it, and each new activation gets its own deadline. An already
revealed feed that stalls remains visible with the existing black freshness cover;
this presentation change never extends camera freshness. HUD and Super mirror
presentation are unchanged. No image caching or producer timestamp substitution
is used in app code.

On the 009, check first activation, repeated left/right signals, instant-ready on
and off, cancellation before a picture arrives, X then cancel/re-signal, and
recording continuity. A warm fresh popup should appear immediately; startup that
finishes within the grace period should have no waiting-message flash. If startup
actually lasts longer, the waiting message remains intentional.

Seven JVM startup regressions cover retained first buffers, immediate warm reveal,
repeated requests and the notice deadline, stalls after reveal, cancellation and
next activations. Two API 32 native tests use the actual overlay/TextureView and
continuous timestamped synthetic EGL buffers. They verify alpha 0 and touch-through
at startup, alpha 0 on the first positive timestamp, alpha 1 on progression, a
visible waiting fallback without frames, and no reappearance after cancellation.
The initial single-buffer test did not advance: the UI callback can precede
RenderThread latching, and Canvas buffers in this emulator reported timestamp 0.
The final test uses continuous EGL presentation timestamps, like a video producer;
app freshness rules are unchanged. This is emulator evidence, not 009 validation.


## Vehicle display name (2026-10-15, 009 check pending)

The owner's 009 Diagnostics report Android Build.MODEL=Pixel 3a, device=sargo and
a generic Zeekr DHU build. The existing 7X display text names the shared camera
profile and does not establish the physical car. Settings → Interface → Vehicle
name now offers Auto (default), Zeekr 009 and Zeekr 7X. Auto only accepts explicit
known model names in Android model/device/product; missing, generic, spoofed or
contradictory values fall back to Zeekr. Select Zeekr 009 manually on this firmware.

Builtin recording-profile choices, profile editor heading and the main/settings
status line use the resolved localized name. Custom profile names are unchanged.
Legacy zeekr_7x/zeekr_7x_multi IDs remain stable for stored profile and migration
compatibility. This cosmetic setting never chooses cameras, calibration or HUD
displays; the existing explicit 009 HUD option remains separate. Diagnostics
record the selection and displayed name alongside raw Android metadata.


## 360 settings and manual popup (2026-10-15, vehicle checks pending)

A new 360° camera section contains display choice (Main screen default / Popup),
manual Open/Close popup, shared quality/calibration and two separate resets for
window placement versus calibration/quality. The service owns a distinct
lowest-priority surround surface; it does not open a separate CameraDevice.
Adding/removing a shared surface can reconfigure the existing capture session.
Hardware maximum-output behavior and recording continuity remain unverified.

Native API 32 overlay testing checks minimum control targets, expand/restore,
dragging and saved position, pinch resizing, fully on-screen final geometry, X
and preservation of display mode/calibration. The initial short synthetic pinch
did not cross Android’s scaling-span threshold; the final gesture does. Native
screenshots exposed a long waiting notice clipped in narrow windows, now wrapped.
Rapid synthetic resizing also required waiting for the window compositor before
capturing the final screenshot; the settled window and buttons are within bounds.
The settings page was inspected in the existing tablet layout. These native tests
do not establish live camera output, vehicle projection accuracy or coexistence
with the mirror, side-camera/HUD windows and recording on the 009.

On the car, choose Popup window, press 360°, drag/pinch/expand/restore, minimise
the app and check that the popup continues. X closes only this runtime request;
press 360° or Open popup to reopen. While open, Take photo must save a fresh
stitched image. Check factory takeover/return, screen off/on, developer probe
release/recovery and simultaneous recording. A rejected extra output must close
only the optional 360 view with an explanatory message. Existing factory/reverse
and freshness guards remain; there is no P-only restriction or boot/indicator
autostart. Exit stops the service. README and workflow describe the new controls.


## Upstream 2.11.6 integration (2026-10-15, 009 check pending)

Merged dts88 `c448fec0a300deafc00a38a4e93f266e65a6f16f` (2.11.6-alpha),
including 2.11.5 diagnostics and planned surround-before-cabin closure. Fork
recording-origin checks, enabled-channel selection, exclusive developer lease,
side/HUD/360 holders and optional-output priority remain present. Existing
upstream contribution branches are unchanged.

When cabin outputs are released after their hold expires, upstream now closes
surround first, closes the cabins, and reopens surround if still requested.
Upstream reports approximately 0.5–1 second of surround interruption and a new
recording segment; this is upstream feedback, not a timing measurement on the
009. Added regressions exercise that full cycle for each fork window holder
(360, indicator popup, HUD) alongside surround recording, checking that both
outputs return, declarations stay intact, and no rescue attempt is counted.

Validation: 1,354 JVM tests passed with no skips/failures, Android debug lint
passed, and the signed 2.11.34-personal-alpha APK (versionCode 317) built.
The signing certificate remains SHA-256
`a40da80a59d170caa950cf15c18c454d47a39b26989d8b640ecd745ba71bf5dc`.
Full debug lint initially flagged the intentionally English/Chinese-only About
file for missing Malay translations; a file-scoped MissingTranslation annotation
now records that established fallback policy without suppressing other resources.
No Android UI or head-unit tests were run for this integration.

On the 009, check recording start/stop with the selected channels, minimise with
Super mirror or an optional camera window active after using cabin previews,
and check that a short planned surround interruption returns automatically.
Check factory-camera takeover/return and recording files around the transition.
Export Diagnostics if a feed or recording fails to return: upstream's new
camera history, cabin-close coupling and error/buffer counters are included.

The initial Ferix source request failed; a repeat succeeded. The correction and
new CAMCORDER port details are documented in ferix-feature-ports.md.


## Ferix 2.12.5 follow-ups (2026-10-15, 009 check pending)

CAMCORDER became accessible on retry; previous notes describing it as unavailable
were corrected. Ports through `d6193888` add customize-value signal scanning,
shared-pipeline service retention during rapid theme changes, opt-in HUD picture
controls/indicator-side placement/experimental overlay and popup/HUD fades.
Existing saved settings and fork defaults are retained; Ferix's personal defaults
and factory-camera bypass are excluded. See ferix-feature-ports.md for source
commits, adaptation details and exclusions.

1,377 JVM tests, Android debug lint and signed build passed. Six native API 32
tests passed using a disposable emulator with synthetic buffers and owned
secondary displays. Both ordinary Presentation and overlay windows remained on
the explicitly selected test display; mirrored HUD placement, night opacity,
fade/cancellation, waiting grace, new progression on side switches and the
existing 360 popup regression passed. This does not identify or validate the
physical 009 HUD. Factory takeover and stale-frame guards remain in the code.

On the 009, check repeated left/right activation, close then cancel/re-signal,
instant-ready on/off, factory takeover, startup/stall covers and recording
continuity. Check rapid day/night changes while a window and recording are
active. For HUD experiments keep the 009 mapping independent, first try the
normal Presentation path, then explicitly try the overlay if needed; it can
cover factory HUD information and should be disabled if unhelpful. Contrast,
night dimming, edge fade and indicator-side positioning are individually opt-in.
Export Diagnostics after failures. The scanner's new C: callbacks improve
coverage but do not establish a blind-spot warning or factory stitched video feed.


## Configurable HUD coexistence and developer toggle (2026-10-15)

Settings → Blind spot camera → HUD picture adds Keep HUD side camera during
factory views, experimental and off by default. Tests cover every combination
of factory 360/popup/park-assist flags, enabling/disabling with a shown HUD,
reverse and hazards, independent speed thresholds and unchanged centre-screen
input. Service routing withdraws HUD immediately on reverse/default factory
priority and never falls back to a screen popup over a factory view. Hardware
arbitration and stale-frame checks are retained; no head-unit coexistence test
has been performed.

Settings → System → Enable developer mode replaces the About safety-heading
password. The same developer_mode/unlocked preference and backup exclusions
retain existing state across updates. New installs remain off. Off hides the
category and gates saved settings without clearing them; the scanner control
is still reapplied immediately. Some already-configured pipelines need restart.
A native API 32 regression clicks the real System switch, checks immediate
category visibility without leaving System, recreates the Activity and checks
persistence, then disables the mode and verifies a saved raw-photo setting is
retained but ineffective. The six existing native camera-window tests also pass.
The password API, numeric constant, hidden About listener and obsolete strings
were removed. README and the native UI spec describe the visible entry point.

On the 009, confirm the System switch and Developer category, existing enabled
state after upgrade, no password prompt, and optional HUD coexistence only after
explicit enablement. Test reverse and factory takeover with the setting both off
and on, and export Diagnostics if the HUD lacks fresh frames. Native/JVM tests
are software evidence, not validation of camera availability on the car.


Final 2.11.36 validation: 1,378 JVM tests, Android debug lint and signed release
build passed. Seven native Android regressions passed; the final settings capture
also caught/restored category highlighting after Activity recreation. The native
test now checks the actual activated System row. Final System/HUD screenshots
show the full warnings, visible switches and correct category selection in the
existing tablet layout. Initial captures ran before the compositor presented the
new screen; capture now waits for the visible target and presentation settling.
Screenshots are emulator-only and are not committed. Hardware coexistence still
requires the 009 test described above.

## Separate side popup temporarily hiding Super mirror (vehicle checks pending)

Settings → Blind spot camera → When Super mirror is active offers Use its window
(default), Show both windows, and Hide during popup. Old boolean selections map
to the first two choices. Reset removes the new hide key and restores replacement.

Only the actual separate window presentation hides the mirror: initial transparent
prewarming/startup and HUD-only output leave it visible. Fade-out completes before
it returns. X, immediate detach/factory takeover, service stop and mode changes
restore the mirror. An attached but transparent, untouchable mirror keeps its
texture, camera claim, saved lane/layout and enabled setting. This does not free a
hardware output slot. A newly started mirror checks the currently presented popup.

Two new JVM policy tests and four native popup tests passed on the disposable
API32 emulator. The new native test checks delayed reveal, transparent prewarming,
fade completion, immediate detach, unchanged mirror enablement, retained identical
SurfaceTexture and reset semantics, without opening any vehicle camera. On-car
checks still need all three modes with screen-only/HUD-only/both, X then re-signal,
factory takeover, mirror manually stopped/started and app backgrounding.
