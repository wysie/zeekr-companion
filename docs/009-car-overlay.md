# 009 and 7X vehicle illustrations

Under **Settings → 360° camera → Picture → Vehicle illustration**, choose:

- **Auto** (default): 009 for explicit 009 mapping, the 009 image-fit profile or a
  positively recognised 009 Android identity; 7X for a positively recognised 7X
  Android identity; otherwise the generic outline. Unknown Android head-unit
  names such as Pixel 3a do not identify a vehicle. The legacy 7X camera profile
  name is not evidence because the 009 can use the same stream profile.
- **009** or **7X**: explicitly select only the illustration.
- **Generic outline**: use the existing footprint without a car sprite.

**Car colour** offers Green, White or Black for both cars. White remains the
colour default for new and existing installations. Colours are illustrative,
not calibrated paint samples. The cosmetic Interface vehicle-name preference
is independent of this artwork selector. All artwork choices leave camera
channels, HUD routing and projection calibration unchanged.

7X artwork is available, but the DIY 360 projection has not been validated on a
real 7X. Use Manual projection there; the 009 image-fit profile remains specific
to the supplied 009 references. See [7X evidence collection](7x-360-validation.md).

The saved colour is shared by main/popup/photo output and applies live without
invalidating camera frames, rebuilding projection maps or changing calibration.
It is stored separately in `bird_eye_appearance`, so resetting calibration and
quality keeps both model and colour. Unknown saved colour names fall back to White.

Front faces up. `BirdEyePreview.draw()` supplies the same artwork to the main
view, service popup and stitched PNG export. The body maps to the existing
calibration footprint; mirrors extend about 9.5% of its width on each side for
the 009 and about 4.3% for the 7X. The selected illustration is fitted to that
existing footprint; it does not set real vehicle dimensions.
A small rounded surround replaces the visible black rectangular backing. Its
colour follows the artwork (dark neutral, muted green or charcoal). It extends
about 1.2% of body width, plus the antialiased border, conservatively beyond the
unknown rectangle; its rounded corners still cover the whole original footprint.
It does not reveal or synthesise underbody pixels.
The underlying footprint remains opaque, and notices/watermarks render above
the car. Camera transforms, live-frame freshness, factory priority, recordings
and photo refusal for stale/blocked views are unchanged. This is an illustrative
vehicle, not exact body geometry or a transparent-chassis implementation.

The nodpi PNG retains generated alpha. The current colour bitmap is shared
between renderers and decoded only when needed, outside the frame draw loop.
The cache retains one model/colour combination, rather than all six; old bitmaps are left to
Android/GC while display lists still own them. Rendering uses filtered Canvas
sampling and existing bounds; it does not add camera readbacks, pixel transforms or a
worker. If artwork decoding fails, the existing outline is retained.

## Asset provenance

`app/src/main/res/drawable-nodpi/bird_eye_car_009.png` was generated on 2026-10-16
with the built-in image-generation tool, with transparent background enabled.
No factory screenshot, owner camera pixels or downloaded vehicle artwork is
included. The illustration is not an official Zeekr asset. Its alpha is preserved;
the renderer selects a source rectangle excluding unused transparent padding.

Final generation prompt:

```
Use case: stylized-concept
Asset type: transparent PNG sprite for the central vehicle footprint in an Android surround-camera display.
Primary request: Make one polished, original top-down illustration of a pearl-white Zeekr 009 luxury MPV, with its FRONT pointing straight UP. True orthographic overhead view, perfectly aligned vertical, symmetrical, no perspective tilt. Long gently rounded rectangular MPV body, short hood at top, dark front windscreen, separate dark panoramic roof glass panels, white roof rails and body edges, subtle panel seams, small side mirrors, discreet wheels tucked beside body. Contemporary factory parking-camera UI rendering, realistic but clean and readable at small sizes. Body length approximately 2.6 times body width, excluding mirrors.
Scene/backdrop: genuinely transparent alpha background, isolated vehicle only.
Composition/framing: one complete car centred, tall portrait asset, minimal transparent padding around silhouette. Entire car and mirrors visible, no crop. Front unmistakable at top, rear at bottom.
Lighting: neutral soft studio shading entirely within vehicle silhouette, legible on both dark and light camera imagery.
Constraints: no ground, no cast shadow outside silhouette, no road, no camera image, no glow, no text, no labels, no watermark, no border, no extra objects. Render opaque body and glass; transparent background only.
```

## Validation

Native Android tests exercise day/night backgrounds at popup, preview and photo
sizes (320×480, 640×960, 960×1440), verify unchanged ground outside the vehicle
and mirror bounds, front-up placement, opaque underbody, explicit 009 selection
and outline fallback. Generated native snapshots support visual review. Actual
on-car appearance still needs an owner check.

Verified on the disposable Android emulator: the original native overlay tests passed;
all six day/night snapshots were exported and popup/preview renders inspected.
The combined build passed 1,421 JVM tests, lint and signed release assembly.
The APK retains the existing application ID and signing certificate.

## Colour variants

The built-in image-generation tool edited the original white sprite into sibling
assets `bird_eye_car_009_green.png` and `bird_eye_car_009_black.png`, preserving
transparent alpha. The owner’s green car photo was a colour reference only; the
photo itself is not shipped. The variants retain the same body/mirror placement
and use the original source rectangle and footprint mapping.

Green edit prompt:

```
Use case: precise-object-edit. Edit target: Image 1, the existing top-down white 009 MPV sprite. Change ONLY the painted body colour. Preserve precisely the entire overhead orthographic shape, pixel alignment, canvas dimensions 971x1619, body bounds, front pointing up, mirrors, windscreen, dark panoramic roof glass, lamps, trim, panel lines, highlights and alpha silhouette. Transparent background with real alpha, no ground or cast shadow, no additional text or objects. Do not redraw, rotate, reframe, resize, crop, extend or move the vehicle. Keep the dark roof glass dark; recolour painted hood, roof perimeter, doors, mirrors and rear body while preserving metallic lighting. Image 2 is a COLOUR REFERENCE ONLY: match its muted deep metallic jade/forest green body, with subtle grey-green highlights. Do not copy its environment, viewpoint or body shape.
```

Black edit prompt:

```
Use case: precise-object-edit. Edit target: Image 1, the existing top-down white 009 MPV sprite. Change ONLY the painted body colour. Preserve precisely the entire overhead orthographic shape, pixel alignment, canvas dimensions 971x1619, body bounds, front pointing up, mirrors, windscreen, dark panoramic roof glass, lamps, trim, panel lines, highlights and alpha silhouette. Transparent background with real alpha, no ground or cast shadow, no additional text or objects. Do not redraw, rotate, reframe, resize, crop, extend or move the vehicle. Keep the dark roof glass dark; recolour painted hood, roof perimeter, doors, mirrors and rear body while preserving metallic lighting. Make the painted body glossy metallic black with subtle charcoal/silver highlights that keep the silhouette and panel edges readable against dark camera imagery. Keep the car elegant, with the same glass and trim.
```

Colour validation (2026-10-17): 1,421 JVM tests, lint and signed release assembly
passed. Three native renderer tests passed, covering all three colours on both
backgrounds at three sizes, unchanged surrounding ground, opaque underbody and
synchronised repaint without resetting revision/freshness. The native colour
picker test passed in day and night modes, including confirmation, persistence
after Activity recreation and retention across calibration reset. Eighteen
rendered variant snapshots were exported; Green/Black and both picker themes
were visually reviewed. Vehicle appearance remains to be checked on the 009.

## 7X assets

See [7X provenance and prompts](7x-car-overlay.md) for the three original
generated assets, source-rectangle bounds and testing limitations. The previous
009 assets remain unchanged.

## Lighter 009 Green — 2026-10-20

The owner requested a lighter green/grey finish using their factory overhead-view
photo as a colour reference. The built-in image-generation tool edited the existing
009 Green sprite into a muted silver-sage finish. The reference photo is not shipped.
The file remains `app/src/main/res/drawable-nodpi/bird_eye_car_009_green.png`; both
its verified 971×1620 canvas and transparent background are retained. Existing source bounds,
footprint mapping, body/mirror placement and colour preference are retained. The
009 Green opaque surround uses `#414942`; 7X Green retains `#29352F`. White, Black,
7X artwork, geometry, camera frames and recordings are unchanged. Already-selected
009 Green uses the new artwork after updating. This is an illustrative screen-colour
match, not a calibrated physical paint sample.

Final built-in edit prompt:

```text
Use case: precise-object-edit. Image 1 is the EDIT TARGET: the existing original transparent top-down Zeekr 009 MPV sprite. Image 2 is a PAINT COLOUR REFERENCE ONLY: the silvery muted light green-grey factory overhead car at the centre of the owner's screen photograph. Change ONLY Image 1's painted body to a substantially lighter, less saturated silvery sage grey-green, with grey dominant and a subtle warm green undertone. Aim for painted midtones around #89968B, soft silver-green highlights around #B9C2B5 and shaded paint around #59675E. Avoid deep forest green, blue/teal, turquoise, mint or bright green. Keep the metallic shading and clean detailed finish. Preserve the entire existing 971 x 1619 pixel canvas, exact vehicle position/scale, orthographic top-down geometry, front pointing up, closed doors, both mirrors, body silhouette, panel seams, trim, tyres, lamps, dark windscreen and panoramic roof glass. Preserve the alpha silhouette and genuinely transparent background; no ground, external shadows or extra objects. Do not redraw, move, rotate, crop, reframe or redesign the vehicle. Do not copy the open door, camera scenery, reflections, screen moire or any other details from Image 2. Deliver only the recoloured original isolated sprite at its original dimensions with transparent alpha.
```

Validation for this recolour: both old and new PNGs are 971×1620 RGBA with
transparent backgrounds; thresholded silhouette edges differ by at most four
source pixels. The three existing native overlay tests pass, covering both models
and all three colours at 320/640/960 widths on light/dark backgrounds, opaque
underbody, unchanged surrounding ground and live repaint without invalidating
camera freshness. The 009 colour checks now expect the lighter low-chroma finish;
7X checks retain their previous green requirement. Popup/preview snapshots were
visually inspected; debug/test builds and lint pass. This does not establish a
physical paint match or new vehicle calibration.
