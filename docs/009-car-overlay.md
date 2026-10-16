# 009 vehicle illustration

DIY 360 uses an original pearl-white 009-style vehicle illustration when either
the explicit 009 camera mapping or the 009 image-fit projection is enabled.
It does not infer the vehicle from unreliable Android device properties, or
change camera mappings based on the cosmetic vehicle-name setting. Other
vehicles retain the generic outline. There is no new setting to enable.

Front faces up. `BirdEyePreview.draw()` supplies the same artwork to the main
view, service popup and stitched PNG export. The body maps to the existing
calibration footprint; mirrors extend about 9.5% of its width on each side.
The underlying footprint remains opaque, and notices/watermarks render above
the car. Camera transforms, live-frame freshness, factory priority, recordings
and photo refusal for stale/blocked views are unchanged. This is an illustrative
vehicle, not exact body geometry or a transparent-chassis implementation.

The nodpi PNG retains generated alpha. One shared bitmap is decoded once per
process, outside the frame draw loop. Rendering uses filtered Canvas sampling
and existing bounds; it does not add camera readbacks, pixel transforms or a
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

Verified on the disposable Android emulator: both native overlay tests passed;
all six day/night snapshots were exported and popup/preview renders inspected.
The combined build passed 1,421 JVM tests, lint and signed release assembly.
The APK retains the existing application ID and signing certificate.
