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

Hard-braking locks and the two driver-recording switches default on, matching the
source feature defaults. Vehicle signal scanning is off until enabled in
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
