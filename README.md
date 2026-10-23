# Zeekr Companion

<img src="docs/branding/companion-icon.svg" alt="Zeekr Companion logo" width="72" height="72">

Camera recording, live camera windows and vehicle tools for ZEEKR head units.
Developed primarily on a ZEEKR 009.

[**Download**](https://github.com/wysie/zeekr-companion/releases) · [Website](https://wysie.github.io/zeekr-companion/) · [User guide](docs/user-guide.md) · [简体中文](README.zh-CN.md)

## Screenshots

| Light | Dark |
| --- | --- |
| [![Main screen, light theme](docs/screenshots/main-light.webp)](docs/screenshots/main-light.webp) | [![Main screen, dark theme](docs/screenshots/main-dark.webp)](docs/screenshots/main-dark.webp) |

Actual app UI with **simulated camera scenes**. [All screens and settings](docs/screenshots/gallery.md).

## What it does

- **Record and photograph** selected surround, road and cabin cameras, with per-camera quality and USB storage limits.
- **Choose your preview** independently of recording: camera sources, pane layout and fisheye correction.
- **Open camera windows:** Super mirror, turn-signal side views and an experimental stitched 360° view. HUD output depends on the vehicle.
- **Browse and protect footage:** photo/video galleries, clip locks, exports and local phone sharing.
- **Make it yours:** light/dark themes, English/Malay/Chinese, floating-button actions and settings backup.

[Settings, defaults and limitations →](docs/user-guide.md)

## Install

1. Download `ZeekrCompanion-<version>.apk` from [Releases](https://github.com/wysie/zeekr-companion/releases) and sideload through App Lab. **After installation, press Back, then launch the app separately.** Using the installer's Open button can leave it stuck.
2. While parked, check **Settings → Recording & photos**, choose your cameras and select a USB drive under **Storage**. Tap **Apply changes** if shown.
3. Record a short clip manually, then open **Videos** and check playback. Recording on launch and driver arrival are **off by default**.
4. For alpha updates, select **Settings → Updates → Update channel → Alpha**. Fresh installs default to Stable, including when the installed APK is an alpha.

Existing Companion installations retain their package, signing identity and settings.
The signing key is public for compatibility: install only trusted releases.
[Updates and signing details](docs/user-guide.md#updates-and-backups).

## Limitations

This is an experimental personal fork with limited vehicle testing. Compatibility
depends on model and firmware; factory cameras take priority. Side views follow
the turn signal, **not obstacle detection**. DIY 360° alignment is approximate.
Configure while parked; do not rely on these views for manoeuvres or as a
replacement for factory safety equipment.

## Documentation

[User guide](docs/user-guide.md) · [Troubleshooting](docs/user-guide.md#troubleshooting) · [Changelog](CHANGELOG.md) · [Technical docs](docs/README.md) · [Build and fork workflow](FORK_WORKFLOW.md)

Created by [wysie](https://github.com/wysie). A fork of
[Zeekr Shortcut by dts88](https://github.com/dts88/zeekr-shortcut-car), built on
[EVCam](https://github.com/suyunkai/EVCam), with contributions from
[FerixzTanz](https://github.com/FerixzTanz/CAMCORDER) and others.
[Full credits](NOTICE.md) · [GPL-3.0](LICENSE).
