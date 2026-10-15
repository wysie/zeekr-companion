# Ferix feature ports

Recorded on 2026-10-14. Sources are from FerixzTanz/CAMCORDER at
`c6d8b6335799ca863a186f2530a45748827864b9`. The fork retains dts88's upstream
`571a2a9904d73bed382f592a98f6e1ca45478d29` recording/channel framework.
Source credit and commit links are in `NOTICE.md`.

| Port | Main adaptations |
| --- | --- |
| Phone sharing | Include all subsequent address, HEAD and compression fixes; retain random-token redaction and connection diagnostics |
| Signal scanner | Latest sensor/polling logic; share the reconnectable vehicle object, skip known sensors, stop when developer mode locks, preserve the original deadline |
| Hard-braking locks | Preserve current second-pass timing; include later current-event protection and bounded lock-list reading |
| Clip export | Port remux/range UI independently of drive-grouped playback; reserve partial files and publish without replacing existing exports |
| Driver sessions | Use current coordinator and physical ScreenState, including WOKE guards; clear persistent resume intent durably on leaving; do not import the older coordinator or its surround-start delays |
| Turn arrows | Draw between existing controls in both independent popup and mirror replacement; labels remain separate |
| Night UI | Port semantic colors/drawables and icon dimensions; retain current waiting/live recording behavior |

Hard-braking locks and driver-stop default on. Driver-start is now opt-in (off
when no choice is saved); existing explicit choices are retained. Automatic
recording recovery rechecks the switch that originated the request, and delayed
driver commands retain their automatic origin. Vehicle signal scanning is off until enabled in
Developer options. Unknown driver signals retain the existing screen-off rules.
Turn arrows use indicator-display telemetry when available and remain green if
it is unreadable. No obstacle-warning trigger or factory stitched 360 feed has
been established.

## Validation

The combined app compiles and the imported detector, budget, signal-scan,
indicator and driver-session tests run alongside the existing suite. Additional
regressions cover driver resume state across process restart and repeated or
concurrent clip exports. Final counts and build results are in `009-test-notes.md`.

There is no connected head unit for native UI or vehicle testing. Android phone
sharing, 009 driver signals, braking events and media export need device checks;
passing local tests does not establish those behaviors on the car.

## HUD side view and mirroring — 2026-10-14

Ported from Ferix PR #14, implementation becbfe10c6c9c1ebd1840245d68791c051c96c6a and diagnostic-token correction 426c5fa3e97bc588d8bee8674c812fadc29a76b8. Screen mirroring is on feature/side-view-mirroring; HUD adaptation is on feature/side-view-hud, based on the combined fork so existing contribution heads remain unchanged.

The port keeps current ChannelScheduler declarations, an independent SIDE_HUD output/holder, factory-camera priority, screen mirror replacement, labels, rotation, framing protection, close/reset behavior and recording. HUD output shares the current preview stream and yields first on session failure; it does not use the source's obsolete direct camera-open/rebuild calls. Screen is the default output; HUD mirroring defaults on and screen mirroring off.

Display matching requires a public presentation-capable, on display identified by the known 7X size or a HUD name, excludes the primary display and instrument cluster, and rejects ambiguous candidates. The source's id-3 fallback is deliberately omitted because display IDs on the 009 have not been identified. Diagnostics report the inventory and selection. Unavailable/failed HUD output falls back to current screen behavior. No 009 HUD has been tested.


## Source access correction — 2026-10-15

An initial GitHub API/Git request returned not found for CAMCORDER. A repeat
request succeeded: [FerixzTanz/CAMCORDER](https://github.com/FerixzTanz/CAMCORDER)
is public, with main at `d6193888b3fa86465ab82ec4fc232f0b9a15959e` (2.12.5).
The separately public `FerixzTanz/zeekr-shortcut-car` is an older fork and is
not the source of the latest CAMCORDER changes. The earlier unavailability
conclusion was incorrect. Original source links remain valid.


## CAMCORDER 2.12.1–2.12.5 follow-ups — 2026-10-15

Integrated source main `d6193888b3fa86465ab82ec4fc232f0b9a15959e` through
separate fork branches, with original source authorship/credit retained:

| Branch | Imported work and adaptations |
| --- | --- |
| `feature/ferix-scan-callbacks` | `945ee4ed`: customize function values as C: entries and callback shapes; retain the fork's bounded queue and cap shapes at 128 |
| `fix/ferix-shared-pipeline` | `57797edd`: find the shared pipeline during rapid Activity recreation; current upstream already retains camera ownership, so adapt the lookup to foreground/relay service retention |
| `feature/ferix-hud-refinements` | PR #15, `6218413c`, `3ea2c9b1`, `d6193888`: contrast, night dimming, edge fade, optional left/right placement, display-window-context overlay; retain current defaults, 009 mapping and current scheduler |
| `feature/ferix-side-transitions` | `3176bae5`: 130/160 ms popup/HUD fades, low nonzero hidden view alpha; retain 750 ms startup grace, fresh frame progression on side switches, immediate factory detach and cancellation guards |

New HUD picture defaults are neutral: contrast 100%, night brightness 100%,
edge fade 0%. Follow indicator side and the experimental overlay are off until
enabled under Settings → Blind spot camera. The fixed-position path and all
previous saved settings remain valid. The overlay uses TYPE_APPLICATION_OVERLAY
through a display-specific window context on Android 11+, with the normal
Presentation path on older versions. Refused HUD output retains screen fallback.

Not imported: `e992d000` owner-specific defaults (including automatic launch,
recording behaviour, geometry and default output/mirroring); HUD bypass of
factory 360/park-assist signals (`830f2d0d` and part of PR #15); replacement
README/versioning; the separate old camera-lifecycle branch. Our current dts88
2.11.6 scheduler is newer than that lifecycle base and remains authoritative.
HUD already has an independent zero speed threshold here; physical factory
camera arbitration still takes priority for every output. The new controls and
emulator results do not establish working video projection on the 009.

Validation: 1,377 JVM tests, Android debug lint and the signed build passed.
Six native Android tests passed on the disposable API 32 emulator: screen-popup
startup/fade/side switch/cancellation, normal and overlay HUD windows on owned
secondary displays (including placement and night alpha), plus the 360 popup
regression. These are synthetic display/frame tests, not head-unit evidence.
