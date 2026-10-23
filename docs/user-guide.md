# Using Zeekr Companion

[Install](../README.md#install) · [Screenshots](screenshots/gallery.md) · [Technical docs](README.md)

## Cameras and the main screen

Under **Settings → Recording & photos**, check the vehicle model, camera sources,
then recording sources and quality. Surround, Front + Rear, Cabin and Manual
mapping are separate source groups. The dedicated road feeds are different from
the front/rear crops in Surround. Availability depends on the head unit.

**Preview** on the main screen chooses what you see, independently of what you
record. Selected recording sources follows the recording selection; other presets
show a different combination. Automatic, Side by side and Stacked arrange source
panes. Surround retains its four-view grid; tap a view to enlarge it.

Save source or renderer changes, then tap **Apply changes** above the preview or
in Settings. Apply finishes the current recording clip, rebuilds camera routes
and resumes recording if it was requested. Pane arrangement and ordinary picture
adjustments apply directly. A pending label means the saved camera choices are
not yet active. [Camera changes](apply-camera-changes.md) · [Source groups](recording-source-groups.md).

## Recording and photos

Select a USB destination under **Storage**, then use **Start recording** or
**Take photo** on the main screen. **Gallery** and the side menu open Videos and
Photos. Verify a short clip before relying on a new setup.

Per-camera controls set frame rate, bitrate and codec; clips can be 1–10 minutes.
Storage limits rotate the oldest unlocked footage. With no recording storage
limit, footage is not automatically rotated and recording stops when space is low.
Internal recording and relay writing require Developer mode.

Recording on launch, boot startup and driver-arrival recording are off by default;
driver-leave stopping is on by default. Screen-off continuation is a separate,
off-by-default Developer option. Only the planned surround source can continue
while dark; cabin cameras release after 30 seconds. Vehicle sleep can stop capture:
keep-awake settings do not guarantee overnight recording.

New public files use the **ZeekrCompanion** folder. Existing ZeekrShortcutApp and
EVCam folders remain readable; they are not automatically moved. Normal recordings
and photos include the app name/version watermark. The experimental driving-info
bar is separate and off by default. [Storage folders](app-storage-folders.md).

## Playback, locks and sharing

Open Videos or Photos to browse date-grouped footage. Tap a camera during playback
to enlarge it; use the thumbnail strip to switch recordings. Clip export selects a
time range of up to ten minutes. Saved DIY 360° PNGs have separate browsing limits.

With **Storage → Lock footage** enabled, the floating button's **Lock current
recording** action protects source clips spanning ten seconds before and after the
press. Automatic flash/braking protection is separate; hard-braking protection
starts enabled. [Manual locking](manual-recording-lock.md).

**Send to phone** displays a QR code for a local browser download while its dialog
is open. Both devices need the same trusted network, such as a phone hotspot.
The one-off link grants access to the selected file; it is not a cloud upload.

## Camera windows

**Super mirror** is a movable window for a surround-camera view. All four
orientations use the surround composite, including front and rear; they do not
switch to the dedicated road streams. Pinch to resize, swipe to change cameras,
and use Picture settings to tune framing.

**Blind spot camera** opens a side view when the turn signal is on. Its Behavior,
Picture, Window and HUD tabs control output and framing. It can use a separate
popup or temporarily replace Super mirror; the mirror relationship setting controls
how the windows coexist. Screen mirroring starts off, while HUD mirroring has its
own default and setting. Straightening, rotation, aim and Auto-fit change displayed
framing, not recorded pixels. Auto-fit starts on. This feature does not detect
vehicles or reproduce the factory blind-spot warning.

HUD output is vehicle-dependent. Set picture and placement separately, then use
visibility calibration from your normal driving position. The optical viewing
area can change with head position; emulator screenshots cannot validate it.
[Side-view settings](blind-spot-side-view.md) · [009 HUD](009-hud-live-video.md).

**360° view** is an experimental reconstruction from Surround, not the factory
stitched image. Automatic / 009 image fit and Manual calibration are available;
geometry and distances remain approximate. Configure it while parked. The main
view and floating window share calibration; the car illustration is not a live
transparent-underbody image. [360° setup and limits](diy-360-view.md).

Factory reversing, parking and camera views take priority. A window may pause or
close when the factory needs the cameras; a frozen-picture cover is not proof
that recording has stopped. Check recording status separately.

## Picture and GPU processing

**Straighten** controls fisheye correction. **System → GPU image processing**
chooses the rendering method: Off, All or Custom for surround preview, blind-side
views, Super mirror, 360° stitching and playback. New installs start Off; upgrades
retain their existing effective choices. Custom selections survive All/Off changes.
Apply camera-route changes when prompted; playback uses the selection next time
it opens. Off still uses Android drawing paths and does not mean all drawing is
CPU-only. No renderer is guaranteed to be faster on every head unit.

Bilinear/bicubic sampling and sharpening cannot create missing camera detail.
The side-picture comparison export can help distinguish source limitations from
rendering loss. [GPU settings](unified-gpu-rendering.md) · [Quality comparison](blind-quality-comparison.md).

Correction and preview layout leave recorded pixels unchanged. In contrast,
**Interface → Preview & playback → Image adjustment** changes supported camera
exposure, white balance and other camera controls across active channels,
**including recordings**. Unsupported controls are not shown.

## Interface and floating button

Choose Follow system, Light or Dark under **Interface**, along with English,
Bahasa Melayu or Simplified Chinese. Floating-button tap, double-tap and long-press
can open the app, toggle recording, take a photo, lock footage, toggle Super mirror,
toggle 360°, or do nothing.

Double tap defaults to Do nothing so single taps remain immediate. Assigning a
double-tap action adds a short wait to distinguish it from a single tap. Position
lock prevents dragging but keeps actions available. [Gesture details](floating-button-gestures.md).

## Updates and backups

Download APKs from [Companion releases](https://github.com/wysie/zeekr-companion/releases).
After App Lab installation, press **Back** and launch the app separately; the
installer's Open button can leave it stuck.

**Updates → Update channel** filters manual checks: Stable includes stable releases,
Beta includes beta and stable, Alpha includes all three. Fresh installs start on
Stable; upgrades preserve existing choices. Installing an alpha does not opt into
alpha updates. Checks run when you tap **Check for updates** and do not offer a
downgrade. The downloader verifies GitHub asset size and SHA-256; assets without
usable digest metadata require manual installation.

The package remains `io.github.dts88.zeekrshortcut` and the signing identity is
unchanged for compatible updates. The repository ships a public AOSP test signing
key, including its private part. This preserves compatibility but cannot prove an
APK came from this maintainer. Use trusted releases. A personal build can use a
private key, but Android will not install it over an app signed with a different
key. [Build workflow](../FORK_WORKFLOW.md).

**System → Settings backup** exports preferences, profiles, camera choices, GPU and
360° settings. Import accepts matching-format Companion 3 backups and supported
Companion 2.11 backups from alpha 55 onward. It confirms, stops recording and exits;
reopen to restore before services start. Footage, permissions, detected hardware,
developer access and external artwork are not restored. Saved text such as plate
stamps is included. [Backup scope and recovery](settings-backup.md).

## Troubleshooting

- **No picture or a stalled window:** open **Camera status** from the main screen
  or System to distinguish factory contention, recovery and missing sources. It
  reports status without forcing retries. Check for unapplied camera changes.
- **Poor preview detail:** compare another view and export a side-picture comparison
  if relevant. Record the GPU policy, source and whether recordings are affected.
- **USB recording stalls:** try a per-camera 10 fps cap, then a lower bitrate.
  Check free space and the selected USB destination. Relay writing uses internal
  storage and is a Developer option, not a remedy to enable blindly.
- **App Lab disappears after a head-unit reboot:** this has been reported; the
  cause is unresolved. Export diagnostics before rebooting if possible.

Export a report through **Settings → System → Diagnostics** and
[open a Companion issue](https://github.com/wysie/zeekr-companion/issues).
Reports can contain location, VIN/device identifiers, paths, vehicle state and
saved settings. Review them before sharing. USB logging and camera/display probes
are explicit Developer tools; merely opening troubleshooting does not start them.

This personal fork has limited on-car validation. Do not operate it while driving
or use its imagery to judge obstacles, distance or manoeuvres. It does not replace
factory cameras, blind-spot monitoring or required safety equipment. Share footage
with care for other people's privacy. [Source review and data handling](project-safety-review.md).
