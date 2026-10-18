# Settings map

The [Astra UI and UX review from 2026-10-18](ui-ux-review-2026-10-18.md)
is saved as deferred work, including prioritized recommendations, the proposed
implementation batches and unresolved owner reports. It does not change the
implemented settings described below.

The native two-pane sidebar remains: Recording & photos, Storage, Super mirror,
Blind spot camera, 360° camera, Floating button, Interface, System, Developer
options (when enabled), and Updates. About & credits remains a direct destination.
Headings group rows without another tap. Only the four named child pages below
add navigation depth; their native Back button returns to their parent.

All sidebar entries, including About & credits and Quit app, bind the same Body
font size with autosizing disabled. Long labels wrap to two lines, and row height
grows for larger system fonts. Rebinding a recycled row restores its size; About
and Quit are direct actions, not smaller secondary labels.

Interface → **Theme** offers Follow system (default), Light and Dark. Choose an
option and confirm with OK; Cancel retains the current choice. It applies to this
app, persists across restarts and may recreate its Activity to load the palette.
The settings sidebar uses the same 18sp Body style in both themes; there are no
night-specific font sizes. MainActivity retains its existing 1.2× font scale,
now through an Activity-local configuration established before AppCompat/view
inflation, instead of mutating an already-used resources configuration during
onCreate. Other screens and application resources retain their own scaling.
The About/Quit recycled-row font fix was included in release 47.

Native API 32 checks cover font-resource isolation, English app locale, returning
from the actual About screen, Activity recreation, real Light/Dark/System picker
changes, and sidebar scrolling/rebinding with equal category/action text sizes.
The owner's intermittent smaller-label state has not been reproduced on the
emulator; this resource correction still needs the car check.

Camera-popup corner buttons, labels and indicator badges resolve the selected
palette too. Attached corner controls repaint without removing the window or
replacing its camera texture. The projected HUD keeps its dark-backed badges and
separate brightness/contrast settings; camera pixels and recording files are not
recoloured by this appearance choice. Vehicle/system theme settings are unchanged.
This uses [AppCompat's local DayNight modes](https://developer.android.com/develop/ui/views/theming/darktheme#change_themes_in-app).

The theme change passed 1,482 JVM tests, eight relevant Android checks, lint and
debug/release builds. Native checks use the real picker/OK/Cancel and recreation,
measure equal menu sizes across Light/Dark/System, exercise normal/1.3× fonts and
About/Quit recycling, and retain the same mirror texture/window while changing
its control palette. Light/dark settings and the picker were visually inspected.
This does not identify which installed build caused the owner's perceived size
difference or establish the behavior of every vehicle firmware.

Typography validation: a native regression deliberately gives a reused title a
smaller/autosized font, then rebinds section and action labels; the old binder
retained the smaller size, while the updated binder restores the shared size.
Measurements cover normal/1.3× system fonts and day/night contexts. Real sidebar
scroll-to-bottom/top/bottom checks and screenshots verify About/Quit in both
themes without invoking either action. Both new checks and three existing native
layout tests pass, alongside 1,472 JVM tests, lint and debug/release builds. This
does not establish the exact cause of the owner's older-build screenshot.

| Main category | Groups and options |
| --- | --- |
| Recording & photos | Cameras: stream profile, profile editor with independent recording groups; shared Camera sources and Automatic reset; Preview layout with saved selection and separate help; model defaults live under System. Automatic recording: launch, driver arrival, driver departure. Screen off: keep recording. Photos: JPEG output. **Stamps & driving information →** app stamp, plate switch/text, timestamp, specifications, information bar and signal-selection shortcut. |
| Storage | Destination, USB power guidance, relay write; video/photo limits and usage; protected footage, flash-to-pass/hard-braking triggers and protected usage. |
| Super mirror | Enable and guide; camera selection and button/gesture controls; correction, field of view and shared camera labels; window width/height; reset. |
| Blind spot camera | Enable; Screen/HUD/Both and mirror relationship; minimum screen-opening speed and factory priority; prewarming and close delay; **Image & framing →**, **Popup window →**, **HUD picture →**; reset. |
| 360° camera | Experimental explanation; main-button destination; Show 360° popup; quality; Automatic / Manual / 009 image-fit projection; popup size/layout reset; calibration dialog and calibration/quality reset. Automatic follows the model unless an explicit choice or legacy manual tuning is preserved. |
| Floating button | Enable; tap/double-tap/hold actions (including Do nothing); size, opacity and timer appearance; lock position; reset. Double tap starts off; assigning it briefly delays single taps. |
| Interface | Language; Theme (Follow system / Light / Dark); action side, recording statistics, reduced motion, vehicle panel; in-app picture adjustments, main preview/photo/video correction method and field of view. |
| System | Vehicle model (Auto / 009 / 7X), confirmed feed/default information, app startup, Diagnostics, system/vehicle information, developer-mode enable with warning. |
| Developer options | Recording internals: allow internal storage fallback (off by default), pipeline, H.264, wake lock and duration. Image/rendering: preview/playback GPU methods. Camera research: mapping, camera/display tools and batch exports, raw frames, USB drive logging with live status, signal scanner, camera-app logging, permissions. Maintenance: repair recordings, archive. |
| Updates | Check for updates, include beta releases. |

## Blind spot child pages

- **Image & framing:** straighten, view width, aim back/up and high-angle crop note,
  separate left/right rotation, centre-screen mirroring, shared camera labels.
- **Popup window:** aspect ratio (Square 1:1, Portrait 3:4, Landscape 4:3, Wide 16:9, Extra wide 2.2:1), height/position and drag/pinch/expand/close guidance. Square remains the default; resizing and expand/restore retain the chosen shape.
- **HUD picture:** Follow popup picture settings; Custom HUD picture (visible when
  following is off: Straighten, Auto-fit, view width, aim, rotations, mirror, label);
  Picture (contrast, night brightness, opacity, edge fade);
  Placement (height, shape, horizontal/vertical position, follow indicator side);
  Advanced experimental routing (factory-view coexistence).

Timing/prewarming remain on the parent because their effects are not limited to
popup geometry. HUD picture follows popup tuning by default unless an explicitly
saved legacy HUD mirror preference needs preserving. Turning following off seeds
the current popup picture on first use; later switches restore previous custom
values. HUD layout and tone stay independent in both modes. The camera-label rows
in Super mirror and Image & framing read and write the same AppConfig setting;
custom HUD labels have their own preference.

## Window controls and reset scopes

Super mirror and Floating button keep their persistent enable switches. Blind spot
camera keeps its automatic-feature switch; X dismisses the current signal activation,
which re-arms after cancellation and signalling again.

Normal Super mirror now has the same corner controls as the side popup. Close
turns the mirror enable switch off (including while Settings is visible); its
existing menu/settings/floating-button action reopens it. Expand/Restore preserves
aspect ratio and the saved normal position/size. Expanded geometry is temporary:
restore before drag/pinch/camera-switch gestures. Entering a blind-side replacement
restores normal mirror geometry first; dismissing that replacement returns to it
without turning Super mirror off. Its camera label sits below the corner controls.
Docked tabs hide the buttons and keep their existing tap/pull-out behavior.
At very narrow widths, Expand hides to leave Close usable; pinch wider to expose
both. Native checks exercise actual button touches, saved geometry, replacement
transitions, size changes, body drag/pinch, docking and real service close/reopen
with the Settings switch visible. Day/night normal and expanded windows were
visually checked on the emulator; in-car camera operation is not established by
these checks.

**Show 360° popup** is a non-persistent session switch. It opens/closes the existing
service; X, startup rejection and service shutdown update it. Factory camera,
screen-off and probe suspension retain the session intent until it can resume.
Selecting Main as the main-button destination closes the popup, as before.
Recreation reads the live session; closing the popup never changes its preferred
destination, calibration or saved geometry. There is no new boot/indicator trigger.

- Super mirror reset retains enable state and resets its own framing/window and
  controls, including camera labels shared with the blind-spot popup.
- Blind spot reset retains enable state and shared labels, and resets screen/HUD
  output, picture, timing and popup placement, clears saved HUD custom tuning and
  returns to following the popup.
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

Camera sources is one shared ordinary page, linked from Recording & photos, the
stream editor and Developer. Explicit mapping wins over detected surround, then
model fallback. Automatic clears one override; Reset all to automatic clears all
overrides while retaining recording/preview/quality choices. Legacy effective
choices migrate conservatively. See [source mapping and reset](shared-camera-sources.md).
