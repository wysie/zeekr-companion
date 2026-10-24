# Settings map

The settings sidebar uses native two-pane navigation. Categories replace the
right-hand page; nested pages use Back. About & credits opens directly and Quit
app is a separate red action. Developer options is visible only when enabled in
System. Ordinary diagnostics does not require Developer mode.

## Where things live

| Category | Contents |
| --- | --- |
| Recording & photos | Vehicle-model shortcut, camera sources, recording sources & quality, independent preview layout, stream profile, automatic recording, ordinary screen-off continuation, JPEG photos, stamps and driving information. |
| Storage | Destination, USB power guidance, Developer-only internal/relay choices, video/photo storage limits, usage and footage protection. |
| Super mirror | Enable, guide, camera selection, controls, picture correction/labels, window size and scoped reset. |
| Blind spot camera | Behavior, Picture, Window and HUD tabs; automatic activation, output, timing, framing, upscaling, placement, calibration and resets. |
| 360° camera | Experimental-view explanation, session window switch, vehicle illustration, resolution, shared GPU shortcut, popup layout and projection/calibration. |
| Floating button | Enable, tap/double-tap/hold actions, appearance, position lock and reset. |
| Interface | Language, theme, driver side, status/motion/vehicle-panel options; **Picture & correction** contains camera image adjustment and preview/playback correction. |
| System | Vehicle model, confirmed feeds, boot startup, settings backup, shared GPU processing, Troubleshooting, system information and Developer-mode enable. |
| Developer options | Troubleshooting shortcuts; recording internals; camera/vehicle research; repair and archive tools. See below. |
| Updates | Check for updates and Stable/Beta/Alpha channel selection. |

## Troubleshooting in two places

**System → Troubleshooting** is the ordinary entry point. **Developer options →
Troubleshooting** provides shortcuts to the same Camera status, Diagnostics and
USB troubleshooting session. Permissions sits alongside them in Developer options.
The shortcuts share handlers and do not create separate settings or reports.

Camera status and Diagnostics open read-only tools. Diagnostics collects a report;
it does not start continuous USB logging. Save & send to phone exports the current
report before opening QR download. Send last saved report reuses the saved file.

USB troubleshooting always opens the same session page, from either menu. Opening
it does not start logging or camera research. Its Start/Stop action and live status
control the service; starting requires Developer mode and a writable USB destination.
Closing the page does not stop a running session. Disabling Developer mode stops
Developer services, preserving the existing policy.

## Developer groups

- **Troubleshooting:** Camera status, Diagnostics, USB troubleshooting session,
  Permissions. These are destinations; entering them starts no recording or probes.
- **Recording internals:** internal fallback, read-only MediaCodec pipeline status, Force H.264,
  screen-off keep-awake and its duration.
- **Camera & vehicle research:** shared Camera sources, camera/display tools, raw
  frame export, explicit vehicle-signal scanning and camera-app logging.
- **Maintenance:** repair unindexed recordings and archive to another drive.

Camera sources is the same configuration page used by Recording & photos and the
profile editor. Camera/display research is also reachable from System troubleshooting
when Developer mode is on. These are intentional shortcuts to shared tools.

## Choices that look similar but have different scope

- **Keep recording when the screen goes off** is ordinary recording policy;
  Developer **Screen-off recording (keep awake)** additionally requests a bounded
  wake lease. Neither guarantees overnight recording.
- **Straighten/correction** changes displayed views. **Image adjustment** controls
  supported camera parameters and can affect recordings too; its summary says so.
- **GPU processing** lives in System with Off/All/Custom. Camera routes use Apply
  changes; individual picture settings remain independent.
- **Vehicle model** selects source defaults. **Stream profile** chooses the camera
  configuration. Recording & photos links to the canonical System model setting.
- **Direction label** in Super mirror and blind-side Picture uses one shared setting;
  custom HUD labels are independent.

Source/mapping and renderer changes use Apply changes. Layout-only changes apply
directly. Back navigation, scoped reset confirmations, saved preferences and
Developer gating are retained. See the [user guide](user-guide.md) for behavior,
defaults and limits.

This map replaces the older menu description. Historical typography, HUD and
layout evidence remains in [the previous revision](https://github.com/wysie/zeekr-companion/blob/4e9232f7b3e19b08b1585ceaf5cbacb36fcc367c/docs/settings-menu.md).
The [24 October menu review](menu-review-20261024.md) records findings and validation.
