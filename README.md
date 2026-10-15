# Zeekr Shortcut (Car Version) — personal fork

This is [wysie's personal fork](https://github.com/wysie/zeekr-shortcut-car) of
[Zeekr Shortcut by dts88](https://github.com/dts88/zeekr-shortcut-car), maintained
primarily for use on my ZEEKR 009. Credit for the original Zeekr application goes
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

The in-app About page identifies wysie’s fork and links separately to this fork,
dts88’s original project and Ferix’s CAMCORDER contributions. Its optional PayNow
tip, **9271 6786**, goes to **dts88, the original author**.

The combined fork includes upstream **2.11.4-alpha** (`e4d0e0d`), including cabin
camera recovery, serialized camera actions, scheduler-managed photos/exit and
camera step diagnostics. `upstream-main` tracks that unmodified source; personal
release numbers remain independent of upstream.

## Additions in this fork

- **Settings layout fixes:** camera cards reserve space for their switches; long names/custom badges wrap instead of hiding controls. Camera/quality cards stack in narrower panes, option buttons wrap at equal heights, and Diagnostics actions can grow with text. The main action column scrolls when too short to fit. Changing settings categories clears stale nested titles/Back history. See [native layout review](docs/settings-layout-review.md).

- **Fresh-install permission handling:** startup requests camera/storage access used by the app and no longer blocks on the undeclared, unused microphone permission. The Developer permissions page omits that unavailable permission.

- **Camera and display research tools:** Settings → Developer → Camera and display tools provides an on-demand batch snapshot of every available Camera2 ID, plus a numbered 10-second test for public presentation displays. Images, generated display cards and JSON reports go under **ZeekrShortcutApp/Developer**. Both require a connected vehicle interface with recording stopped and factory camera closed; neither checks gear or speed. The scan temporarily closes this app’s normal feeds and retains exclusive ownership until native closure is confirmed; saved camera/HUD settings stay unchanged. These tools cannot screenshot factory displays or expose private factory video buffers. See [instructions and limits](docs/developer-camera-display-tools.md).

- **Independent camera selection:** Surround, Front cabin and Rear cabin are opened according to their individual profile switches, including cabin-only profiles. Exit and reopen after changing switches; recording shows an explicit restart message while saved channel choices differ from the live pipeline.

- **One app folder:** new recordings, photos and exported diagnostics go under **ZeekrShortcutApp/Videos**, **Photos**, **Logs** and on-demand **Developer** exports on selected storage. Playback, protection, storage accounting and cleanup also support older EVCam folders without moving existing files. See [storage layout](docs/app-storage-folders.md).

- **Recording and transfer integrity:** verified USB/archive copies preserve both files on collisions; relay buffers use private files instead of disposable Android cache. Failed protection-list replacement keeps the old list; developer repair first retains a verified original backup. See the [whole-project review](docs/project-safety-review.md) for fixes and remaining limits.
- **Camera freshness guards:** Main surround/cabin previews, Super mirror, side popup and HUD cover stalled pictures after 500 ms without advancing texture timestamps, checked at 100 ms intervals. Reopened textures require new frame progression; missing geometry stays covered. These software guards still need on-car timing checks. See the [camera safety review](docs/camera-safety-review.md).
- **DIY 360 prototype (experimental):** manually switch the live surround preview to a stitched top-down ground view with a central vehicle outline, overlap blending and bilinear sampling. Tap **360°** on the main camera screen; hold it to calibrate, choose **360 resolution** or reset. Balanced is the sharper default; Full requests the live frame at its native dimensions (up to 8 MP), with automatic resolution reduction if processing is slow or memory is limited. On the supplied 009 stream, Balanced uses 512×2056 → 640×960; Full uses 1280×5140 → 960×1440. While 360 is selected, the main photo action saves the fresh stitched preview at the active resolution and calibration notes to **ZeekrShortcutApp/Photos/360** on configured storage. Switch back to Four cameras to capture ordinary/raw composite photos. Saved 360 PNGs are separate from the regular photo-playback list. It requires a connected vehicle interface, with no P-only or speed restriction on preview or calibration. Reverse and factory-camera takeover still suppress the DIY picture. Geometry is still estimated: owner feedback confirms visible misalignment, and resolution/export improvements do not establish calibrated distances. See [setup and limits](docs/diy-360-view.md).
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
- **System day/night camera controls:** side-popup buttons, turn-arrow badges and camera labels use light surfaces/dark icons by day and dark surfaces/light icons at night. Super mirror uses the same themed labels and side controls. Open windows repaint when Android reports a theme change; no separate switch is needed. This follows Android’s reported `uiMode`, so firmware that does not publish its factory theme may differ. The projected HUD keeps its dark-backed badges. Camera pixels and recordings are unaffected.
- **Optional side-camera mirroring:** flip the image in the popup or Super mirror replacement; off by default, with recordings unchanged.
- **Experimental HUD side camera:** adapted from Ferix PR #14; choose Screen, HUD or both, with separate HUD mirroring and layout controls. An opt-in Zeekr 009 mapping targets display 3 at 1280×640; physical output and camera availability on the 009 remain unverified.
- **Optional camera labels:** identify the displayed camera in Super mirror and
  the side window. Super mirror also has a defaults reset.

The following additions are adapted from [FerixzTanz/CAMCORDER](https://github.com/FerixzTanz/CAMCORDER), with source credit in [NOTICE.md](NOTICE.md):

- **Phone sharing fixes and diagnostics:** reachable network addresses, transfer status and HEAD/compression fixes. Android timeout reports still need device verification.
- **Read-only vehicle signal scanner:** an opt-in Developer option for finding unmapped signals, including possible blind-spot warnings; automatically stops after three hours or when Developer options are locked.
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
See [factory 360 research and the collection tool](https://github.com/wysie/zeekr-shortcut-car/tree/research/factory-360-view)
for the current investigation and the device information needed next.

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

**Mirror side-camera image** flips left and right in both the separate popup and Super mirror replacement. It is off by default; labels, arrows and controls remain readable and recordings are unchanged. Reset side-camera settings restores mirroring to off.

**Show side view on** selects Screen (default), HUD, or Screen and HUD. Screen output still follows **Use Super mirror when active**. Automatic HUD selection needs an accessible, powered-on, public presentation display with the known 1440×480 HUD dimensions or an explicit HUD name. An unavailable or rejected HUD falls back to the screen behavior, preserving its opening-speed threshold.

Under **HUD picture**, enable **Zeekr 009 HUD** to explicitly select display 3 only when its resolution is 1280×640. This option defaults off and is independent of the camera profile. The owner's 009 Diagnostics expose this anonymous display, but its physical destination and ability to show camera video remain unverified. With HUD or both outputs selected, check the result while parked and turn this option off if it appears on another screen. Private, non-presentation and explicitly named cluster/rear/passenger displays remain excluded; a missing matching display falls back to the screen. Changing the mapping releases the existing HUD window before selecting again. Reset side-camera settings restores automatic selection. Diagnostics include the selected mapping.

Under **HUD picture**, adjust height (30–100%, default 74%), shape (width/height, 100–300%, default square), horizontal position (default 6%), vertical position (default 50%) and opacity (20–100%, default 100%). HUD mirroring defaults on and is independent of screen mirroring. Aim, correction, per-side rotation, camera labels and turn arrows are shared. HUD windows have no touch controls; use the settings to configure them. Reset side-camera settings also restores Screen output and all HUD defaults, retaining the main feature switch.

The HUD has no app speed threshold, but reverse, factory views, parking assist and screen-off still suppress it. Closing the screen side view also dismisses the HUD for that signal activation; it returns after cancelling and signalling again. HUD video is covered when frames stop arriving. Its optional camera output yields before screen output, mirror and recording when the session cannot support it. Diagnostics include display flags, the detected HUD, output mode, mirroring choices and recent HUD window state. This does not establish compatibility or picture quality on the 009.

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
point to this fork. The signing **private key** is public, so someone else can create
Android-accepted updates with the same identity. It does not establish that an APK
came from this maintainer. A private-key migration needs a separate installation/update
plan; the existing identity is retained for compatibility. The updater now requires
GitHub asset size and SHA-256 to match and uses HTTPS through redirects. Old assets
without a digest are refused by the in-app downloader; manual alpha installs remain unchanged.

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

Send to phone offers Wi-Fi, hotspot or tethering addresses instead of internal vehicle links. The dialog shows whether the phone connected and whether sending completed; recent attempts appear in Diagnostics. Both devices must be on the same local network. Sharing uses an unencrypted HTTP bearer link with a new 128-bit token for each selection and at most four simultaneous clients. The temporary server stops when the dialog is dismissed. Use a trusted local network; anyone with the active link can download the selected file. Includes Ferix’s HEAD/compression fixes; Android timeout reports still need device verification.

## Vehicle signal scanner

Enable Settings → Developer options → Record all vehicle signals for a read-only scan of function changes and unfamiliar sensor events. Blind-spot-like functions are polled when available. It stops after three hours or when Developer options are locked, skips sensors already used by the app, and exports results through Diagnostics. Pending callbacks are capped at 256 and excess events are dropped under overload; the export is not a complete event history. This is a research tool: no obstacle-alert trigger or factory 360 video feed has been identified.

## Hard-braking footage lock

Settings → Storage → Lock footage on hard braking is on by default and depends on Lock footage. It protects every camera’s footage around a detected hard stop (10 seconds before and after), using available speed/brake signals. Automatic locks have a 10% recording-space budget; oldest automatic locks can be released, while manual and flash-to-pass locks are preserved. Signal behavior on the 009 needs verification.

## Custom clip export

In playback, tap Save clip to save 15 seconds before/after the current point, or choose a range starting here, move to the end and tap Save to here. Custom clips are limited to 10 minutes and copied without re-encoding; the start may include preceding keyframe footage. Clips are saved under ZeekrShortcutApp/Videos/Clips and can be sent to the phone. A codec change stops concatenation. Saved clips and concurrent exports are never overwritten. Swipe left/right to switch cameras when a surround cell is enlarged.

## Recording follows the driver

Settings → Recording has **Start recording when you get in** (off by default) and **Stop recording when you leave the car** (on by default). Explicit saved choices are retained. An occupied driver seat in P (or a shift out of P) requests recording through the current upstream coordinator only with driver-start enabled. Leaving in P stops after 30 seconds with an empty seat, or sooner when the screen is physically off or Sentry is armed. A manual stop holds for the drive; leaving clears that hold and the saved resume intent. Unknown seat/gear readings reset the empty-seat countdown. Sentry-based stopping requires a known empty driver seat; known movement contradicting P blocks this automatic stop. Disconnecting the vehicle interface clears the driver-away gate. Other screen-off rules still apply. The 009’s seat/gear signals need on-car verification; disable either switch if its behavior does not suit your firmware.

Autolaunch opens the app; starting a new recording still requires Record on launch, driver-start, or an explicit manual command. Automatic recording requests retain their origin across process restarts and recheck the originating switch on wake-up and after storage/camera waits. Turning that switch off cancels its pending starts and future automatic recovery; an active clip continues until stopped. Manual recording can still recover from an interruption, and pressing Stop cancels recovery. Saved requests from older builds have no known origin; with both automatic-start switches off, those older requests are discarded. Diagnostics show the effective driver settings and recording-request origin.

## Turn arrows on side cameras

The side camera displays an upright left/right arrow between the top corner controls, including during Super mirror replacement. It follows the indicator display in green/grey; if blink telemetry is unavailable it stays green. It does not indicate an obstacle detection. Camera labels remain independently configurable.

## Night theme and floating record button

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
verified backup under `ZeekrShortcutApp/Videos/repair-originals/<filename>.original`. It refuses to
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
