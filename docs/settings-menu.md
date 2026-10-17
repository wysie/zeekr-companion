# Settings map

The native two-pane sidebar remains: Recording & photos, Storage, Super mirror,
Blind spot camera, 360° camera, Floating button, Interface, System, Developer
options (when enabled), and Updates. About & credits remains a direct destination.
Headings group rows without another tap. Only the four named child pages below
add navigation depth; their native Back button returns to their parent.

| Main category | Groups and options |
| --- | --- |
| Recording & photos | Cameras: stream profile, profile editor with independent recording groups; Preview layout with saved selection and separate help; model defaults live under System. Automatic recording: launch, driver arrival, driver departure. Screen off: keep recording. Photos: JPEG output. **Stamps & driving information →** app stamp, plate switch/text, timestamp, specifications, information bar and signal-selection shortcut. |
| Storage | Destination, USB power guidance, relay write; video/photo limits and usage; protected footage, flash-to-pass/hard-braking triggers and protected usage. |
| Super mirror | Enable and guide; camera selection and button/gesture controls; correction, field of view and shared camera labels; window width/height; reset. |
| Blind spot camera | Enable; Screen/HUD/Both and mirror relationship; minimum screen-opening speed and factory priority; prewarming and close delay; **Image & framing →**, **Popup window →**, **HUD picture →**; reset. |
| 360° camera | Experimental explanation; main-button destination; Show 360° popup; quality; popup size/layout reset; calibration dialog and calibration/quality reset. |
| Floating button | Enable; tap/hold actions; size, opacity and timer appearance; lock position; reset. |
| Interface | Language, action side, recording statistics, reduced motion, vehicle panel; in-app picture adjustments, main preview/photo/video correction method and field of view. |
| System | Vehicle model (Auto / 009 / 7X), confirmed feed/default information, app startup, Diagnostics, system/vehicle information, developer-mode enable with warning. |
| Developer options | Recording internals: allow internal storage fallback (off by default), pipeline, H.264, wake lock and duration. Image/rendering: preview/playback GPU methods. Camera research: mapping, camera/display tools and batch exports, raw frames, USB drive logging with live status, signal scanner, camera-app logging, permissions. Maintenance: repair recordings, archive. |
| Updates | Check for updates, include beta releases. |

## Blind spot child pages

- **Image & framing:** straighten, view width, aim back/up and high-angle crop note,
  separate left/right rotation, centre-screen mirroring, shared camera labels.
- **Popup window:** aspect ratio (Square 1:1, Portrait 3:4, Landscape 4:3, Wide 16:9, Extra wide 2.2:1), height/position and drag/pinch/expand/close guidance. Square remains the default; resizing and expand/restore retain the chosen shape.
- **HUD picture:** Picture (mirror, contrast, night brightness, opacity, edge fade);
  Placement (height, shape, horizontal/vertical position, follow indicator side);
  Advanced experimental routing (009 mapping and factory-view coexistence).

Timing/prewarming remain on the parent because their effects are not limited to
popup geometry. Screen and HUD mirroring remain separate. The camera-label rows
in Super mirror and Image & framing read and write the same AppConfig setting.

## Window controls and reset scopes

Super mirror and Floating button keep their persistent enable switches. Blind spot
camera keeps its automatic-feature switch; X dismisses the current signal activation,
which re-arms after cancellation and signalling again.

**Show 360° popup** is a non-persistent session switch. It opens/closes the existing
service; X, startup rejection and service shutdown update it. Factory camera,
screen-off and probe suspension retain the session intent until it can resume.
Selecting Main as the main-button destination closes the popup, as before.
Recreation reads the live session; closing the popup never changes its preferred
destination, calibration or saved geometry. There is no new boot/indicator trigger.

- Super mirror reset retains enable state and resets its own framing/window and
  controls, including camera labels shared with the blind-spot popup.
- Blind spot reset retains enable state and shared labels, and resets screen/HUD
  output, picture, timing and popup placement.
- 360 popup-layout reset changes placement/size only. Calibration reset also resets
  rendering quality, but not popup placement or preferred destination.
- Floating-button reset retains its existing position/size/timer-text scope.

The reorganisation retains stored values, defaults, permission/developer gating,
factory/reverse priority and existing independent PR branch heads. It does not
change the recording policy or 360 geometry. New labels/help are localised in
English, Chinese and Malay.

The former “HUD above factory information” experiment remains removed. Stored
`hud_side_overlay` values are ignored. The owner has now confirmed live camera
video on the 009 windscreen HUD. Automatic matching recognises its public/on
display 3 at 1280×640; the extra 009 enable switch is removed and its old value
is ignored. HUD advanced keeps factory-view coexistence only.

009 placement has separate saved settings: 50% height, square shape, horizontal
94% (right), vertical 50%, and indicator-side following off. Both cameras thus
use the same right-hand position. Existing generic/7X layout values remain
saved; first use on a matched 009 starts its own defaults. Sliders still adjust
size and position. The panel fits measured Presentation content, including when
it is smaller than physical display metrics; wide shapes shrink proportionally.
Factory speed/warnings may still draw over it. See [009 live HUD evidence](009-hud-live-video.md).

## USB fallback permission

Developer mode alone no longer enables internal fallback. The Developer recording
internals switch defaults off even on upgrades. USB recording requests wait for
a writable drive; the main record button shows Waiting for USB and allows
cancellation. If fallback is explicitly on, startup waits ten seconds before
allowing internal storage. In-recording internal failover is gated too. Direct
Internal storage and Relay write remain independent explicit choices. Changes do
not relocate an active file; stop/start recording to re-evaluate its destination.


Merged follow-ups: Interface → Preview & playback now includes **Image
adjustment**, inside the Activity. It is ordinary configuration, not gated by
Developer mode. The panel includes enable/disable and reset; unsupported camera
controls are omitted. The former Developer picture-adjustment/reset rows are gone.
Blind spot camera → HUD picture has independent **HUD picture contrast at night** (100% default).
System → **Vehicle model** replaces the cosmetic name selector and separate 009
mapping switch. Auto uses explicit Android model names; unidentified head units
show a manual-selection prompt. An enabled legacy 009 mapping is retained as 009,
but a cosmetic name alone never changes camera sources. The model selects automatic
source defaults, builtin names and Auto artwork. Manual sources and saved picture
settings remain. Exit and reopen after changing. The evidence row explains 009
confirmed sources, upstream 7X surround/cabin evidence and unverified road/360
capabilities. HUD still matches actual displays; the 009-only image fit is suspended
without deletion on 7X. See [model defaults](vehicle-model-profiles.md).
