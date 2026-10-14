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
