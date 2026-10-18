# Zeekr Companion identity

The owner selected concept v3 (linked companions) on 2026-10-19. Two offset,
open C shapes express the Companion name. The approved generated concept was
redrawn as simple vector paths for crisp launcher and notification rendering.

- Launcher: warm white `#F7F7F5` and orange `#F0622D` on charcoal `#202124`.
- Main-page title: a compact 32dp mark (`ic_companion_mark.xml`), using the same
  paths without launcher padding. The first loop follows the theme’s primary text
  colour; the second retains orange. The adjacent app name supplies accessibility text.
- Android themed icon and notification icon: the same two separated silhouettes,
  coloured by Android. The notification uses the central 72dp crop at 24dp.
- Both loops fit inside the adaptive icon’s central 66dp safe circle, including
  round and squircle launcher masks. The background is provided separately by Android.
- `companion-icon.svg` is the standalone square vector reference. Keep its paths
  aligned with `ic_launcher_foreground_zeekr.xml`, `ic_launcher_monochrome.xml`
  and `ic_stat_app.xml` under `app/src/main/res/drawable`.

This is the independent app’s identity, not the official ZEEKR logo. The Android
application ID, signing identity and `ZeekrShortcutApp` storage folders stay unchanged.
