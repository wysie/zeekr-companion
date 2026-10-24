# Settings menu review — 24 October 2026

Scope: all categories and nested screens in preferences.xml, dynamically bound
rows, drawer entry points, Developer-mode gating and shared navigation handlers.
This is a menu/navigation review, not a new whole-app performance or accessibility
audit. Current behavior is documented in the [settings map](settings-menu.md).

## Findings addressed

| Priority | Finding | Change |
| --- | --- | --- |
| P2 | Developer “Camera research & diagnostics” omitted Diagnostics and Camera status. | A first Troubleshooting group links to both existing activities and Permissions. System troubleshooting remains accessible without Developer mode. |
| P2 | USB logging had a direct Start/Stop pair in Developer options and a separate session page under System. | Both menus now open the existing session page. One explicit Start/Stop action and live status remain; entering the page never starts logging. |
| P2 | Permissions was filed under camera research. | Move its existing destination into Developer troubleshooting. |
| P2 | Interface “Preview & playback correction” also contained camera adjustment that changes recordings. | Rename the heading to Picture & correction; retain the existing warning and controls. |
| P2 | Legacy pipeline picker offered choices that recording ignored. | Show MediaCodec as read-only status; retain legacy stored preferences. |
| P2 | Help and generated release notes omitted Troubleshooting from the Diagnostics path. | Correct the nested path in all app languages and future release notes. |
| P3 | The settings map still described removed GPU switches, beta-only updates and obsolete restart instructions. | Replace it with the current hierarchy, shared destinations and scope distinctions. |

The research heading now says Camera & vehicle research, matching its actual
camera/display, signal-scan and camera-app logging tools. New aliases use unique
preference keys and share action handlers; there is no new persisted setting.
English, Malay and Chinese headings are updated together.

## Deliberate placements retained

The main drawer's galleries/window switches and Settings entry remain direct
shortcuts. GPU processing stays in System, as requested previously. Camera
sources, vehicle-model setup and camera/display research have intentional shared
entry points. Blind-side tabs, scoped resets, update channels and native Back
navigation remain in place.

Ordinary screen-off continuation and Developer keep-awake have different behavior;
they should not be merged. Camera image adjustment can affect recordings, whereas
preview correction does not. Both distinctions are now explicit in the settings map.

## Verification

- 1,571 JVM tests passed with no failures, errors or skips, including translation
  parity and exhaustive Developer-row accounting. All 218 preference keys are unique.
- All 16 targeted API 32 native navigation/troubleshooting/USB checks passed with
  the explicitly provisioned disposable virtual USB volume; none skipped in the
  final run. Earlier USB checks skipped until that fixture was provisioned.
- Both menu paths launch the same Diagnostics and Camera status activities. USB
  navigation/Back retains the right category, opens the common session page, and
  starts no logging or camera probe. Ordinary troubleshooting remains available
  with Developer mode off; USB Start stays gated.
- Debug/test APK builds, resource processing and lint passed with zero lint errors.
  Five release-note/packaging checks and edited Markdown links passed.

These are emulator/source checks, not head-unit touch or on-car camera validation.
