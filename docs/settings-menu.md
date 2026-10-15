# Settings map

The native two-pane sidebar remains: Recording & photos, Storage, Super mirror,
Blind spot camera, 360° camera, Floating button, Interface, System, Developer
options (when enabled), and Updates. About & credits remains a direct destination.
Headings group rows without another tap. Only the four named child pages below
add navigation depth; their native Back button returns to their parent.

| Main category | Groups and options |
| --- | --- |
| Recording & photos | Cameras: stream profile and profile editor. Automatic recording: launch, driver arrival, driver departure. Screen off: keep recording. Photos: JPEG output. **Stamps & driving information →** app stamp, plate switch/text, timestamp, specifications, information bar and signal-selection shortcut. |
| Storage | Destination, USB power guidance, relay write; video/photo limits and usage; protected footage, flash-to-pass/hard-braking triggers and protected usage. |
| Super mirror | Enable and guide; camera selection and button/gesture controls; correction, field of view and shared camera labels; window width/height; reset. |
| Blind spot camera | Enable; Screen/HUD/Both and mirror relationship; minimum screen-opening speed and factory priority; prewarming and close delay; **Image & framing →**, **Popup window →**, **HUD picture →**; reset. |
| 360° camera | Experimental explanation; main-button destination; Show 360° popup; quality; popup size/layout reset; calibration dialog and calibration/quality reset. |
| Floating button | Enable; tap/hold actions; size, opacity and timer appearance; lock position; reset. |
| Interface | Language, vehicle display name, action side, recording statistics, reduced motion, vehicle panel; main preview/photo/video correction method and field of view. |
| System | App startup, Diagnostics, system/vehicle information, developer-mode enable with warning. |
| Developer options | Recording internals: allow internal storage fallback (off by default), pipeline, H.264, wake lock and duration. Image/rendering: adjustments/reset and preview/playback GPU methods. Camera research: mapping, camera/display tools and batch exports, raw frames, signal scanner, camera-app logging, permissions. Maintenance: repair recordings, archive. |
| Updates | Check for updates, include beta releases. |

## Blind spot child pages

- **Image & framing:** straighten, view width, aim back/up and high-angle crop note,
  separate left/right rotation, centre-screen mirroring, shared camera labels.
- **Popup window:** size, height/position and drag/pinch/expand/close guidance.
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

The former “HUD above factory information” experiment has been removed. Stored
`hud_side_overlay` values are ignored. Normal HUD output, optional 009 display
mapping and factory-view coexistence remain available for investigation; working
HUD video on the owner’s 009 has not been established.

## USB fallback permission

Developer mode alone no longer enables internal fallback. The Developer recording
internals switch defaults off even on upgrades. USB recording requests wait for
a writable drive; the main record button shows Waiting for USB and allows
cancellation. If fallback is explicitly on, startup waits ten seconds before
allowing internal storage. In-recording internal failover is gated too. Direct
Internal storage and Relay write remain independent explicit choices. Changes do
not relocate an active file; stop/start recording to re-evaluate its destination.
