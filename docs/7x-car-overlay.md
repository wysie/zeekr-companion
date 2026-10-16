# 7X illustration assets

Original illustrative top-down 7X artwork was generated with the built-in image
tool on 2026-10-17, with transparent background enabled. The white asset was
generated first; green and black are colour-only edits of it. The generated PNGs
and their alpha are preserved. These are not official Zeekr assets, exact vehicle
geometry, factory images or calibrated paint samples.

Files under `app/src/main/res/drawable-nodpi/`:

- `bird_eye_car_7x.png` (white)
- `bird_eye_car_7x_green.png`
- `bird_eye_car_7x_black.png`

[Zeekr's official 7X page](https://www.zeekrlife.com/en-sg/models/7x/)
was consulted for model/design context. No official photograph is bundled.

Each image is 1024×1536. The renderer selects source pixels (184,42)–(840,1495),
with the body aligned approximately at x=210–814. Mirrors extend about 4.3% of
the existing calibrated footprint width on each side. Runtime alignment is
asset placement only: selecting 7X changes no projection, dimensions or camera
mapping. Both 009 and 7X use a single current model/colour cache, with no bitmap
decoding in the frame draw loop. The full underbody footprint stays opaque.

## White prompt

```
Use case: stylized-concept
Asset type: transparent PNG vehicle sprite for an Android 360 surround-camera display.
Primary request: One polished, original illustration of a pearl-white ZEEKR 7X midsize luxury SUV viewed from perfectly straight above. True orthographic overhead view, symmetric, FRONT pointing UP. This is the 7X SUV, not the long 009 MPV: shorter sporty broad SUV body, rounded hood at top, slim full-width black front light band, dark windshield, continuous large dark panoramic glass roof, small side mirrors, gently tapering rear roof and distinct rear glass, white rear hatch and subtle red rear light bar at bottom. Dark roof glass and discreet tucked wheels. Clean realistic factory parking-camera UI aesthetic, detailed metallic body shading yet readable when small.
Composition: portrait canvas 1024x1536, whole car centered vertically and horizontally. Target BODY excluding mirrors approximately x220..804, y60..1476, width584, length1416. Mirrors extend to about x150 and874. Vehicle body length about2.43 times width. Do not add a ground shadow.
Background: actual transparent alpha, no road, no camera pixels, no backdrop, no drop shadow, no glow. One complete vehicle only. No text, no labels, no watermark, no border. Soft neutral highlights preserve legibility on both day and night camera feeds. Opaque painted body and dark roof glass. No perspective tilt, no rotation, no cut off parts.
```

## Green edit prompt

```
Use case: precise-object-edit. Edit target: the provided top-down white Zeekr 7X SUV sprite. Change ONLY its painted body colour, preserving exactly the 1024x1536 canvas, overhead orthographic shape, front-up direction, location, size, proportions, silhouette, mirrors, dark roof glass and windshield, rear glass, lamps, trim, panel seams and shading. Keep the same pixels aligned; do not reframe, rotate, resize, crop or redraw the car. Actual transparent background and alpha cutout, no backdrop, ground, cast shadow, glow, text, labels or extra objects. Painted body and glass remain opaque. Recolour painted surfaces to muted deep metallic jade/forest green with grey-green highlights (approximately #405b50 midtone), while retaining dark roof glass and all trim. This is an illustrative colour option.
```

## Black edit prompt

```
Use case: precise-object-edit. Edit target: the provided top-down white Zeekr 7X SUV sprite. Change ONLY its painted body colour, preserving exactly the 1024x1536 canvas, overhead orthographic shape, front-up direction, location, size, proportions, silhouette, mirrors, dark roof glass and windshield, rear glass, lamps, trim, panel seams and shading. Keep the same pixels aligned; do not reframe, rotate, resize, crop or redraw the car. Actual transparent background and alpha cutout, no backdrop, ground, cast shadow, glow, text, labels or extra objects. Painted body and glass remain opaque. Recolour painted surfaces to glossy metallic black with subtle charcoal/silver highlights that preserve the body edges on dark imagery. Retain dark roof glass and all trim.
```

## Limits

The artwork is decorative and is shared by main preview, popup and stitched
photo export. It does not validate DIY 360 on the 7X, retrieve a factory stream
or supply imagery under the body. See [the 7X evidence checklist](7x-360-validation.md)
for the separate camera-mapping, geometric and performance work still needed.

## Validation (2026-10-17)

The combined app passed 1,423 JVM tests, lint and signed release assembly. Three
native renderer tests passed; they rendered both vehicle models in all three
colours on day/night backgrounds at 320×480, 640×960 and 960×1440, checked the
surrounding-ground bounds and opaque footprint, and retained live-frame state
when repainting. The model/colour picker test passed in day and night modes,
including confirmation, Activity recreation, persistence across calibration
reset and preservation of the selected projection. Thirty-six current model/
colour render snapshots were exported, and the 7X variants and both picker
themes were visually inspected. These are synthetic emulator checks; no actual
7X camera or on-car validation has been performed.
