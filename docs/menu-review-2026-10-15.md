# Menu and layout review — 2026-10-15

Scope: the navigation drawer, every declared item in the ten settings sections,
the nested HUD page, and photo/video playback control layouts. Reviewed current
XML, preference binding and twelve native emulator captures. The playback capture
is a layout fixture with all eight controls visible, not a recording from a car.
Layout tests cover EN/ZH/MS, day/night, 1.0/1.3 font scale and 360–1800 dp action
widths. Long settings pages were catalogued from source; their initial viewports
were captured, not every scroll position/dialog. No full TalkBack, contrast-meter,
vehicle performance or 009 display-inset audit was performed.

The existing native two-pane structure and semantic day/night palette should be
kept. The problem is accumulated settings and some fixed control sizes, not a
need to replace the visual design. This review does not authorize or implement
a wholesale menu reorganisation; the narrow fixes requested by the owner are
implemented separately.

## Current inventory

Counts include explanatory/action rows; nested HUD entries are counted separately.

| Section | Direct rows | Assessment |
| --- | ---: | --- |
| Recording | 12 | Core recording controls mixed with six stamp/info options |
| Storage | 10 | Coherent; locking controls already depend on their master switch |
| Super mirror | 10 | Manageable; shared camera-label control has a discoverability issue |
| Blind spot camera | 20 | Most overloaded everyday section; also leads to HUD |
| HUD picture (nested) | 13 | Position, optics, picture tone and experimental routing intermixed |
| 360° camera | 10 | Coherent experimental section with independent layout/calibration resets |
| Floating button | 9 | Coherent; related appearance/action controls belong together |
| Interface | 8 | Mostly coherent; fisheye method/width need consistent scope wording |
| System | 5 | Developer warning dominates the top before common startup/screen controls |
| Developer options | 17 | Encoding, camera research and storage maintenance mixed in one list |
| Check for updates | 2 | Short and clear; keep top-level findability |

The drawer has three destinations, two quick switches and three system actions.
Its direct Super mirror switch remains useful and should not change its meaning
when the mirror is temporarily hidden by a side popup. About is reachable from
both drawer and Settings, which is reasonable and not a defect by itself.

## Findings, in priority order

### [P1, fixed] Playback actions wrap down and clip at the bottom

Location: `activity_timeline_player.xml` and `activity_photo_playback.xml`.
Eight equal-weight 52 dp video buttons were baseline-aligned; a two-line Send to
phone label could move its whole button downward and clip the bottom. Photo
actions had the same fixed-height risk. `WrappingActionRow` now uses content-sized
native buttons, equal row heights and row wrapping. It retains action IDs,
callbacks and native text size. Normal wide layouts still fit one row. Native
measurement and visual captures verify the video row, with photo and RTL cases
also covered by measurement. No car-specific video decoding is implied by this.

### [P2, improved] Binary mirror wording cannot express the third relationship

Location: Blind spot camera / `pref_side_popup_replace_mirror`.
A second boolean would create ambiguous combinations. Replaced the UI with one
three-way When Super mirror is active choice: Use its window, Show both windows,
Hide during popup. Existing booleans migrate without changing behaviour; an
explanation is separate from the current-value row. The mirror enable switch
continues to express the owner's saved intent. The selected value stays visible.

### [P2, proposed] Split blind-spot tuning by task

Location: `screen_blind_spot_camera` in `preferences.xml`.
The common enable/output/mirror relationship/speed controls precede a long mixture
of lens framing, source rotation, timing and window layout. Finding one option
requires remembering where it falls in a scroll. Keep behaviour at the top, with
three named destinations: Image and framing, Popup window, HUD picture. Place
straightening/FOV/aim/rotation in the first, size/position/close timing/prewarming
in the second. Preserve the minimum-speed and high-angle notes beside their
controls and retain existing keys/reset semantics. Do not add more top-level
sidebar categories to solve this.

### [P2, proposed] Separate HUD picture, placement and advanced routing

Location: `sub_side_hud`.
Group mirror/contrast/night brightness/edge fade under Picture; height/shape/
position/follow-indicator under Placement; 009 mapping, overlay and factory-view
coexistence under a clearly labelled Advanced group. The experimental descriptions
and defaults must stay visible with the affected controls. Keep display output
selection at the parent page; avoid duplicating it under HUD.

### [P2, proposed] Make shared camera labels discoverable from blind-spot settings

Location: `pref_rearview_camera_label` under Super mirror.
Its existing summary correctly says it affects Super mirror and the side popup,
but an owner using only the popup must visit an unrelated section. Add a shortcut
or another view of the same stored setting under Image and framing, keeping one
source of truth and live refresh for both outputs. Do not create a second,
conflicting preference merely to improve navigation.

### [P2, proposed] Group developer tools and demote the System entry

Location: `screen_developer` / `screen_system`.
The 17-row developer list mixes capture diagnostics, rendering experiments,
recording internals and data maintenance. Use those four concrete groups. Move
Enable developer mode after the common System controls, retaining its warning.
Keep the Developer category hidden when disabled and retain existing stored-value
and signal-scan shutdown rules. Do not make a new generic Advanced top-level page.

### [P2, proposed] Keep optional recording stamps off the main behaviour list

Location: `screen_recording`.
App/plate/timestamp/specs/driving-info controls lengthen the page after its core
recording actions. A Stamps and recording info subpage would reduce scroll without
altering recording defaults. Preserve the three distinct launch/driver/leave-car
behaviours and their explanatory text; do not merge them into a vague Auto record
switch. This is a navigation proposal, not an approved recording-policy change.

## What is working

- Native preferences, a consistent two-pane layout, and visible selected category.
- Semantic light/dark colours rather than a separate hardcoded night theme.
- Clear separation of Super mirror, blind-spot camera and experimental DIY 360.
- Developer features hidden by default, with an explicit System switch and warning.
- Independent reset scopes and retained warnings around factory-camera priority,
  speed thresholds and high-angle cropping.
- Earlier responsive camera-card and segmented-choice fixes remain covered by tests.

## Recommended follow-up

Make one focused navigation-only branch for Blind spot camera/HUD grouping, then
another for Recording/Developer grouping. Keep preference keys, defaults, reset
scopes, overlay lifecycles and existing upstream PR branches unchanged. Verify
nested Back navigation and current-value visibility, and finish with one bounded
native polish pass. Existing source review and emulator screenshots do not replace
an on-car check at the 009's actual display density and system bar geometry.
