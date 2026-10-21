# 009 live HUD video and layout

Latest owner photos (received 2026-10-19, 376479/376478) show left/right square
camera windows beside the parked factory gear/car graphics, with a dark inward
notch on the right picture. The owner requests matching narrower windows to reduce
overlap. The photos alone cannot distinguish dark source content, factory masking
and app clipping; the installed build and physical safe bounds remain unconfirmed. The [new 009 popup reference](009-popup-reference.md) applies only
to the centre-screen popup; it must not be presented as a HUD preset. Earlier
software-bound checks below do not establish that physical clipping is resolved.

The owner has confirmed live side-camera video on their 009 windscreen HUD and
supplied phone photos with/without the camera. This supersedes the earlier
identification-card-only finding. It establishes that head unit, not every 009
firmware or every frame-rate/visibility condition. No installed version is
inferred from the phone pictures.

## Automatic selection

Choose **Blind spot camera → Show side view on → HUD** or **Screen and HUD**.
There is no longer a separate **Zeekr 009 HUD** enable switch. Its obsolete
stored value is ignored, including if it was false. Matching requires an on,
public presentation display with ID 3 and exactly 1280×640. Main/private displays
and named cluster/rear/passenger screens are excluded. The known 7X 1440×480
signature and explicit HUD names remain supported; ambiguous candidates fail
closed to the existing screen fallback. The cosmetic vehicle-name choice does
not select the display.

## Placement and clipping

**Follow popup picture settings** uses popup view width, aim, rotations, mirroring,
labels, Straighten and Auto-fit. It defaults on unless an explicit legacy HUD
mirror choice needs preserving. Turn it off for a custom HUD picture, initially
seeded from the popup; later toggles retain the last custom tuning. Layout and
tone remain independent in both modes. Equal requested picture values can still
produce different fitted coverage at different aspect ratios. This does not
establish a recommended HUD preset or fix the reported optical clipping/overlap.

The confirmed 009 display selects its own saved placement profile: height 50%,
square shape, horizontal position 94%, vertical position 50%, Follow indicator
side off. Thus left and right cameras both appear on the right. The existing
height/shape/position sliders can still move and resize the picture. Enabling
indicator-side following translates the 009 position to distance from that edge
(default 6%), and turning it off restores the corresponding right-hand position. Its first
use starts these new defaults independently of saved generic/7X layout values;
switching back to another recognised HUD restores that layout's stored values.
Switching placement profiles does not change tone or the selected picture mode.

The panel now uses the measured drawable Presentation content. Physical display
metrics can exceed an app's content space, so they are used only before layout.
A layout listener recomputes placement after measurement and viewport changes.
Wide shapes are fitted proportionally if they would exceed the available width,
rather than squeezing the width or leaving the panel outside its parent. These
changes address clipping risks in the app; physical projector masking and the
new right-hand picture still need a vehicle check. Corrected-camera ray fitting
and the configured field of view remain in use.

### Road-edge trim

**Blind spot camera → HUD → Position & size → HUD road-edge trim** trims 0–60%
of the original fitted width. The recognised 009 layout starts at 25%, including
existing installations without an explicit crop preference. Generic/7X starts at
0% and stores its choice separately. Existing size, shape and position values
are retained; zero restores their full picture. Placement reset restores the
current layout’s crop default, while picture reset leaves it alone.

The displayed camera picture retains the car-body edge first. With mirroring on,
Left trims its left/road edge and Right trims its right/road edge; mirroring off
reverses those picture edges. Custom HUD mirroring is respected when picture
following is off. The visible panel still shrinks inward from its outer HUD
placement, but picture offset is independent of that window position. A fixed-right
panel therefore keeps the same bounds for both cameras while retaining different
parts of their pictures. No extra zoom/squeeze or saved-aim change is applied.

Direction labels are anchored at the bottom car-body edge inside the final visible
rectangle: mirrored Left at bottom-right, mirrored Right at bottom-left. Popup,
Super mirror and settings-preview label placement is unchanged. Labels and the
centred turn arrow remain separate final-resolution drawing after image cropping,
GPU scaling, tone and fade. The existing 0–60% preference and independent 009/generic
defaults are retained, including settings backup/reset compatibility. Diagnostics
retain the crop percentage/picture offset and now add actual trim edge, lane and
mirroring. A label missing on the physical HUD is not established to have been
software-cropped; projector masking/factory-layer overlap still need a car check.

25% is an initial overlap-reduction value, not a measured projector calibration
or a guarantee against all navigation/speed graphics. The owner still needs to
verify the result in the car and can adjust crop/position/height independently.

Diagnostics now report automatic selection, the active placement profile,
measured viewport, visible panel rectangle, crop percentage, full picture width
and retained picture offset. These allow a subsequent export to
distinguish app bounds from optical clipping on the actual windscreen.

## Factory graphics

The owner observes factory speed/navigation graphics over the camera. The app
uses an ordinary transparent Presentation and cannot assume control over a
separate factory HUD layer. Black on a projected HUD adds no light; it does not
erase factory speed or warnings. Moving the camera to the right and reducing
its default size reduces central overlap, but dynamic factory graphics can
still overlap. No higher-priority overlay or suppression of factory information
has been reintroduced.

Reverse, factory-view coexistence settings, output arbitration, stale-frame
covers, cancellation and screen fallback retain their existing policies.

## Verification

Inner-edge crop checks (2026-10-19): 1,505 JVM tests, debug lint and debug/release
assembly pass, with the existing release signing certificate verified. Fifteen
native Android checks pass on the disposable emulator. These cover actual
Presentation bounds and full-width texture layout after cropping, independent
009/generic persistence and both reset scopes, nested settings and recreation,
and badge pixels within 48/120/240px windows. A native EGL pattern comparison
checks both trimmed edges with raw and corrected/rotated/mirrored pictures: the
remaining pixels retain their original scale and the same SurfaceTexture.
Existing native-detail and corrected-coverage regressions also pass. These checks
establish software clipping and retention, not physical factory-graphic clearance.


Follow/custom picture checks pass on Android with real SharedPreferences,
production popup/HUD renderers and nested settings navigation. They cover live
following, first-use copying, retained custom values, legacy mirror preservation,
independent layout/tone, reset and recreation. The combined check ran 16 native
tests (including existing Presentation fitting and corrected-image rendering),
1,479 JVM tests, lint and debug/release assembly. Settings were visually checked
at 2560×1600 with enlarged text. This validates configuration/software bounds,
not physical HUD clipping, factory-layer overlap or an in-car picture preset.

JVM checks cover the owner's display inventory, exact signature matching,
restricted/ambiguous screens, smaller viewport bounds and proportional shape.
Native checks exercise the real Presentation on a disposable secondary display,
then constrain its content to 640×360 while physical metrics remain 1280×640;
the complete wide panel must stay inside the content. Other native checks cover
independent 009/generic placement persistence/reset and corrected image coverage
for rotated square/wide/tall views. Emulator checks do not validate windscreen
optics, actual factory-layer positions or in-car smoothness.

Validation passed: 1,426 JVM tests, debug lint, signed release assembly and eight
Android checks (three real Presentation/configuration checks, two corrected-image
coverage checks, vehicle-label independence, settings navigation and settings
screenshot review). The existing signing certificate is retained. The new
right-hand layout and physical clipping still require an owner check on the car;
the supplied photos confirm live video before these changes.

### Body-edge trim verification — 2026-10-20

The camera/mirroring-aware policy supersedes the earlier position-only crop rule.
The missing physical Left label was not reproduced as software clipping; its HUD
anchor now moves to the retained car-body edge to keep it inside the remaining
picture, with the final optical/factory-layer outcome still awaiting a car check.

Validation: 1,528 JVM tests pass, zero lint errors, and debug/instrumentation/signed
release builds pass. Nineteen native checks pass, including a real 1280×640 public
Presentation captured through ImageReader with synthetic 1280×5140 EGL input.
For both side cameras, mirroring on/off and GPU All/Canvas compatibility, 25%/60%
trims retain the expected pixels at the original scale and keep the same camera
TextureView/producer. GPU coverage includes Bicubic sharp with 25% sharpening.
Complete white-label and green-arrow pixels match final-visible-window references;
48/120/240px badge bounds, smaller Presentation viewports, follow/custom preferences,
independent 009/generic reset and existing popup freshness/lifecycle also pass.
Composed mirrored left/right captures were inspected on both renderers. These are
software bounds checks, not an optical-resolution or physical HUD visibility claim.
Existing crop values/defaults are retained; no new release is published here.

### Manual HUD visibility observations — 2026-10-20

Blind spot camera → HUD → Position & size → HUD visible-area test opens main-display
controls and a temporary square on the existing detected public HUD. The marker
starts at 200px, supports Red/Cyan/White, pixel-position/size sliders and 1/10px
nudges. It can deliberately cross logical display edges. Save a Fully visible and
a cut-off observation at each physical boundary, repeating at different heights.
These are user-entered observations, never inferred projector limits. Factory
objects may hide a marker inside an otherwise visible region; a single pair of
observations does not prove one permanent rectangular safe area.

The latest 128 explicit observations are committed to a separate local log, with
session/timestamp/version, display identity/dimensions/flags, measured content
viewport and origin, requested square coordinates/size/colour, logical visible
intersection and current HUD placement/trim/mirroring. Diagnostics schema 4 includes
`hud_visibility_observations` and the text report includes the same bounded data.
The controls link to existing Diagnostics export. No derived bounds are applied;
normal video is temporarily concealed, saved framing and recording intent retained.
Leaving, screen-off, Presentation dismissal or three minutes without input stop
the marker; returning never restarts it automatically.

Validation: 1,531 JVM tests pass; lint has zero errors and no new calibration-file
warnings; debug, instrumentation and signed release APK builds pass. Twelve native
checks pass across the calibration tool, existing HUD window/picture/settings and
Diagnostics settings-export regression. A real public 1280×640 Presentation captured
through ImageReader shows the expected clipped red square at both logical edges.
Manual observations remain explicit, correctly exported and bounded to 128; leaving
and display removal close the session. Normal HUD concealment retains its attached
panel and TextureView. Light/dark native control captures were visually inspected.
These checks establish Android behavior, not windscreen visibility, automatic
optical calibration, timed physical projector response or recording on a car.
No release was published for this change.

### Owner viewing-position observation — 2026-10-21

The owner tested the red square and reports that apparent cut-off changes with
viewing angle: moving their head can reveal the previously missing portion. This
supports an optical viewing-position contribution rather than establishing a fixed
Android clipping boundary. It does not prove every label/arrow issue resolved, nor
provide coordinates from which to derive a universal safe rectangle. No saved HUD
placement, trim, scale or defaults are changed in response. Subsequent visibility
observations should use the owner's normal seat/head position and allow ordinary
head movement, rather than treating a maximum revealed area as always visible.

This interpretation is consistent with the HUD eyebox described in
[Texas Instruments' HUD optical guidance](https://www.ti.com/lit/an/dlpa073a/dlpa073a.pdf),
sections 4.4 and Figure 8. That general optical reference does not establish the
009's projector design or its exact viewing limits.
