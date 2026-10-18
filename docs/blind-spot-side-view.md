# Blind-spot-triggered side view: investigation

Investigated on 2026-10-13 for the owner's Zeekr 009. The existing application and
turn-signal side view already work on this car. This note concerns the additional
trigger: detecting an obstacle warning even when no indicator is active.

## What is established

- The maintainer explicitly plans both turn-signal and blind-spot-alert triggers in
  [issue #6](https://github.com/dts88/zeekr-shortcut-car/issues/6#issuecomment-6050799572).
- PR #4 and our side-view implementation currently use turn signals, not obstacle
  warnings. `SideViewDecision` already provides factory-camera priority and the
  existing window services provide the camera presentation.
- `Signal.BSD` (`0x28081600`) represents the assist enable setting.
  `VehicleState.blindSpotAssist` is **not** a left/right obstacle warning. See
  `docs/zeekr-platform-notes.md`, the safety-assist-switch row, and
  `telemetry/Signal.java`. Using this value as a trigger would open the camera
  merely because the assist system is switched on.
- The current reader uses `com.ecarx.xui.adaptapi.car.Car`, its function/sensor
  managers, subscriptions and periodic reconciliation. It is read-only.

## October 18 owner correlation candidate

The owner recalls a warning around 16:34, probably on the right. This is an
approximate recollection, not a timed confirmation of a particular signal.
The continuous 16:27:56–16:58:52 drive capture contains actual callbacks for
`IADAS.SETTING_FUNC_ASYAL_INDCR` (`0x28084400`), rather than only an SDK-name hint.
Its zone `0x00000080` changes between values 1 and 2 around that time:

| Value 1 timestamp | Next value 2 timestamp | Concurrent turn-indicator evidence |
| --- | --- | --- |
| 16:34:07.319 | 16:34:07.445 | Right indicator still selected; turns off at 16:34:07.578 |
| 16:34:31.230 | 16:34:33.085 | Right indicator selected throughout |
| 16:34:56.442 | 16:34:57.993 | Indicator already off at 16:34:56.211 |

The selected-indicator comparison uses the nonflashing
`SETTING_FUNC_LIGHT_INDCR_STS` (`0x2A091500`), not the individual lamp's blink
phase. The app's right-side popup/HUD opens at 16:34:02.8, 16:34:13.0,
16:34:28.2 and 16:34:52.3 were triggered by turn indicators; those app messages
are not obstacle-warning detections. There is no callback for this candidate
during the exact 16:30 minute, though other vehicle data is present.

Across the continuous capture, this function produces 41 value-1 and 40 value-2
callbacks for zone `0x00000080`, with transitions observed while the selected
indicator is off, left, right and hazards. One value-1 callback for zone
`0x00000008` occurs at 16:48:30.028. Neither the zones' meaning nor whether 1 or 2
means warning/clear has been established. Do not map `0x80` to right or activate
a camera from these values based on one approximate owner recollection.

This is a candidate to investigate alongside the named blind-spot IDs, not an
identified blind-spot decoder. A repeat observation with a timestamp and observed
side, ideally including a warning without the turn indicator, can distinguish it
from other ADAS state changes. SDK/firmware metadata could also establish the
function's semantics. Unknown/stale values must remain unknown until verified.
The owner dump stays private; no raw log or vehicle image is included here.

## Public leads, not verified 009 signals

The [AutoService analysis](https://github.com/Mikhail2092/geely-third-party-software-analysis/blob/main/apps/AutoService/analysis/report.en.md)
describes a separate property adapter with custom property IDs `10027` / `10028`
for driving warnings and `10029` / `10030` for parking warnings. These are that
adapter's IDs; they are not ECARX function IDs for this application.

Its [FS11_A2 VHAL mapping](https://github.com/Mikhail2092/geely-third-party-software-analysis/blob/main/apps/AutoService/vehicle-profiles/vhal_fs11_a2_signal_mapping.json)
contains these underlying IDs:

| Side/type | VHAL ID | Mapping name |
| --- | --- | --- |
| Left driving | `0x21401A19` | `CLUSTER_CHIME_LCMAINDCNLE` |
| Right driving | `0x21401A1A` | `CLUSTER_CHIME_LCMAINDCNRI` |
| Left parking | `0x21401A1B` | `CLUSTER_CHIME_LETTCLV` |
| Right parking | `0x21401A1C` | `CLUSTER_CHIME_RITTCLV` |

These are another model's VHAL properties. Their names suggest cluster/chime
indications; they do not establish silent blind-spot mirror-lamp coverage, value
semantics, permissions, or availability on the 009. Do not put these numbers into
`Signal` as FUNCTION entries: that is a different API namespace.

The same project's [ECARX KX11_21 mapping](https://github.com/Mikhail2092/geely-third-party-software-analysis/blob/main/apps/AutoService/vehicle-profiles/ecarx_21kx11_signal_mapping.json)
has **no entries for these four alerts**. Its underlying reader uses
`ecarxcar_service` → `car_signal`, rather than this application's adaptapi
function/sensor reader. It therefore does not supply a working ECARX translation.
The public Monjaro widget SDK/source inspected also supplied no blind-spot warning
constant. The diagnostic lab referenced by upstream was inaccessible through the
authenticated GitHub API during this investigation.

## Implementation once the real signal is identified

1. Identify the 009 firmware's read-only left/right warning properties or sensor
   events, including zones, supported status and value meanings. Start with SDK
   constant/interface discovery and targeted subscriptions in a diagnostic build.
   Do not sweep/write vehicle settings or infer warning values from unrelated IDs.
2. Correlate timestamped callbacks with the actual left/right mirror warning
   indicators. Distinguish assist enabled, nearby vehicle present, urgent lane
   change warning and parking/door warnings. Establish which of these the owner
   wants to trigger the view and verify clear/disconnect behavior.
3. Add separate nullable `blindSpotLeftAlert` / `blindSpotRightAlert` fields to
   telemetry. Unknown/unsupported remains unknown; decode documented states
   explicitly rather than treating every nonzero value as an alert. Bound the
   freshness of each reading so a stale warning cannot hold a view open.
4. Add an independent **Blind-spot side view** toggle, off by default. Route both
   warning and indicator triggers through one presentation decision. Clear the
   temporary view only when neither enabled trigger still requests that side.
   Use a separate warning policy rather than automatically inheriting PR #4's
   30 km/h indicator threshold. Initially scope this to driving warnings.
5. Reuse the existing Super mirror window when active; otherwise use the existing
   side-view pop-up. No second camera output is required. Restore the previous
   mirror view and placement after alerts clear. Reverse, stock camera views,
   park assist, screen-off and feature disable keep priority.
6. Handle simultaneous left/right warnings deliberately: prefer an indicated side
   when it is also warning, then retain an already displayed warning side; if no
   side can be chosen, use an explicit both-sides presentation rather than flicker
   between cameras. Define and test this policy before enabling it.

Verify left-only, right-only, both sides, warning without indicator, warning
persisting after indicator cancellation, disconnect/stale values, assist switch
changes, reverse/factory takeover, screen-off and restoration of a docked mirror.
Decision tests can cover these transitions; signal identification and real warning
timing still need observations from the 009.

## Branch boundary

`research/blind-spot-alerts` holds this investigation and no active vehicle trigger.
The eventual `feature/blind-spot-side-view` can start from
`feature/turn-signal-side-view`, carrying only the signal and trigger work.
`feature/camera-labels` stays independently reviewable. Merge verified feature
branches into the installable fork's `main` branch; keep `upstream-main` aligned with dts88.
