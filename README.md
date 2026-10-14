# Zeekr Shortcut (Car Version) — personal fork

This is [wysie's personal fork](https://github.com/wysie/zeekr-shortcut-car) of
[Zeekr Shortcut by dts88](https://github.com/dts88/zeekr-shortcut-car), maintained
primarily for use on my ZEEKR 009. Credit for the original Zeekr application goes
to dts88 and its contributors; the underlying EVCam project is by
[suyunkai and contributors](https://github.com/suyunkai/EVCam). Full attribution
and license details are in [NOTICE.md](NOTICE.md).

For the original project and its maintainer's builds, use
[dts88's repository and releases](https://github.com/dts88/zeekr-shortcut-car/releases).
This fork is an experimental alternative for trying the additions below. Neither
project is an official ZEEKR product. I do not claim that this fork has the same
testing coverage or device compatibility as upstream.

## Additions in this fork

- **Automatic blind spot camera:** adapted from
  [FerixzTanz's original turn-signal side view, PR #4](https://github.com/dts88/zeekr-shortcut-car/pull/4),
  with credit and original commit authorship preserved. It works with the current
  camera scheduler and has its own settings section.
- **Super mirror integration:** temporarily show the signalling side's camera in
  the existing mirror, then restore the previous view; alternatively, show a
  separate side-camera window alongside it.
- **Side-window controls and tuning:** shared left/right placement, drag and pinch
  resizing, temporary expand/close controls, per-side image rotation, defaults
  reset, and automatic framing adjustment to avoid black patches at extreme aim
  angles. High angles can substantially narrow the visible area.
- **Optional camera labels:** identify the displayed camera in Super mirror and
  the side window. Super mirror also has a defaults reset.

The blind spot camera currently responds to **turn signals**, not obstacle alerts.
Setting its minimum opening speed to 0 removes the app's speed threshold; factory
camera views, including the low-speed 360 window, still take priority. Factory
stitched 360 access and obstacle-warning signals remain research topics, not
implemented features. Ferix's newer phone-sharing fixes and scanner are not yet
included in this build.

This fork is mainly for personal use. Automated tests and release builds are
checked, but vehicle testing is limited. I have confirmed camera labels and Super
mirror replacement on my 009; other changes still need on-car verification. See
[test notes](docs/009-test-notes.md) for the scope of that feedback. Compatibility
with other models and firmware versions is not established.

**Clone `main` and build APKs from `main`.** It contains the combined installable
fork; `upstream-main` tracks dts88's unmodified code. Individual contribution
branches remain separate so useful changes can be proposed upstream. See
[Fork workflow](FORK_WORKFLOW.md) for the branch layout. Whenever a feature is
added or changed, update this README with its behavior, settings and limitations.

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
on and **Use Super mirror when active** is enabled (default), it temporarily shows the side camera in
the same window and restores the previous camera,
framing and position after the signal ends and the configured close delay. A docked mirror is
brought into view temporarily and docks again afterwards. With Super mirror off, a separate
pop-up is used. Turn **Use Super mirror when active** off to keep the mirror and show a separate
side window alongside it. Both windows share the existing surround camera stream; the side
window has its own output and does not replace the mirror output. Reverse, factory 360, factory
side view and park assist take precedence.

Side-camera correction and aim apply to either window. The pop-up size and placement settings
apply only to the separate pop-up. Left and right cameras use one shared window position.
Drag the window to place it and pinch to resize; both position and size are saved. It stays
square and is limited to half the screen width.
Super mirror keeps its own size and position. If the head unit rejects the additional output,
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
modes. Both reset actions are available while the feature is off and preserve its on/off state.

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
- **This personal fork:** download the desired APK from
  [this fork's releases](https://github.com/wysie/zeekr-shortcut-car/releases).
  New combined APKs are built from `main`; each release tag identifies its exact source commit.
  Personal alpha builds are installed manually; they are excluded from the in-app updater.

The fork retains the original application ID and public test signing key so its
APK can update an existing installation signed with that key. In-app update checks
point to this fork.

Download the `ZeekrShortcut-*.apk` asset from the release page and sideload it through App Lab.
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

1. **Lower the frame rate to 10 fps first.** In **Settings → Recording → Edit stream profile**, tap
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

Send to phone offers Wi-Fi, hotspot or tethering addresses instead of internal vehicle links. The dialog shows whether the phone connected and whether sending completed; recent attempts appear in Diagnostics. Both devices must be on the same local network. Includes Ferix’s HEAD/compression fixes; Android timeout reports still need device verification.

## Vehicle signal scanner

Enable Settings → Developer options → Record all vehicle signals for a read-only scan of function changes and unfamiliar sensor events. Blind-spot-like functions are polled when available. It stops after three hours, skips sensors already used by the app, and exports results through Diagnostics. This is a research tool: no obstacle-alert trigger or factory 360 video feed has been identified.

## Hard-braking footage lock

Settings → Storage → Lock footage on hard braking is on by default and depends on Lock footage. It protects every camera’s footage around a detected hard stop (10 seconds before and after), using available speed/brake signals. Automatic locks have a 10% recording-space budget; oldest automatic locks can be released, while manual and flash-to-pass locks are preserved. Signal behavior on the 009 needs verification.

## Custom clip export

In playback, tap Save clip to save 15 seconds before/after the current point, or choose a range starting here, move to the end and tap Save to here. Custom clips are limited to 10 minutes and copied without re-encoding; the start may include preceding keyframe footage. Clips are saved beside the recording directory in EVCam_Clips and can be sent to the phone. A codec change stops concatenation. Swipe left/right to switch cameras when a surround cell is enlarged.
