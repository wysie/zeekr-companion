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
