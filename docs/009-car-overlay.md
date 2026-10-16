# 009 vehicle illustration

DIY 360 uses an original 009-style vehicle illustration when either
the explicit 009 camera mapping or the 009 image-fit projection is enabled.
It does not infer the vehicle from unreliable Android device properties, or
change camera mappings based on the cosmetic vehicle-name setting. Other
vehicles retain the generic outline. Under **Settings → 360° camera → Picture →
009 car colour**, select Green, White or Black. White is the default for new and
existing installations. Green approximates the owner’s supplied metallic green
reference; these are illustrative colours, not calibrated paint samples.

The saved colour is shared by main/popup/photo output and applies live without
invalidating camera frames, rebuilding projection maps or changing calibration.
It is stored separately in `bird_eye_appearance`, so resetting calibration and
quality keeps the colour. Unknown saved colour names fall back to White.

Front faces up. `BirdEyePreview.draw()` supplies the same artwork to the main
view, service popup and stitched PNG export. The body maps to the existing
calibration footprint; mirrors extend about 9.5% of its width on each side.
The underlying footprint remains opaque, and notices/watermarks render above
the car. Camera transforms, live-frame freshness, factory priority, recordings
and photo refusal for stale/blocked views are unchanged. This is an illustrative
vehicle, not exact body geometry or a transparent-chassis implementation.

The nodpi PNG retains generated alpha. The current colour bitmap is shared
between renderers and decoded only when needed, outside the frame draw loop.
The cache retains one colour, rather than all three; old bitmaps are left to
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
