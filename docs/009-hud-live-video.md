# 009 live HUD video and layout

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

The confirmed 009 display selects its own saved placement profile: height 50%,
square shape, horizontal position 94%, vertical position 50%, Follow indicator
side off. Thus left and right cameras both appear on the right. The existing
height/shape/position sliders can still move and resize the picture. Enabling
indicator-side following translates the 009 position to distance from that edge
(default 6%), and turning it off restores the corresponding right-hand position. Its first
use starts these new defaults independently of saved generic/7X layout values;
switching back to another recognised HUD restores that layout's stored values.
Contrast, mirroring, brightness, camera aim and correction remain unchanged.

The panel now uses the measured drawable Presentation content. Physical display
metrics can exceed an app's content space, so they are used only before layout.
A layout listener recomputes placement after measurement and viewport changes.
Wide shapes are fitted proportionally if they would exceed the available width,
rather than squeezing the width or leaving the panel outside its parent. These
changes address clipping risks in the app; physical projector masking and the
new right-hand picture still need a vehicle check. Corrected-camera ray fitting
and the configured field of view remain in use.

Diagnostics now report automatic selection, the active placement profile,
measured viewport and panel rectangle. These allow a subsequent export to
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
