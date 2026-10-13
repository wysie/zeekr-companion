# Reusing the factory stitched 360-degree view

Research recorded on 2026-10-13 for the owner's Zeekr 009. Prefer obtaining the
factory's already calibrated bird's-eye output before implementing a new stitcher.
No stitched-view feature or live vehicle probe is implemented in this branch.

## Current evidence

- This repo receives a Camera2 surround composite containing four separate views
  packed vertically. `StreamLayoutTable` describes that layout; the application
  reconstructs front/rear/left/right previews. The packed composite is not the
  factory's bird's-eye image.
- The upstream [platform notes](https://github.com/dts88/zeekr-shortcut-car/blob/main/docs/zeekr-platform-notes.md),
  sections 3.1.1 and 3.6, attribute the factory 360-degree view to a separate vendor
  AVM path outside Camera2. Part of the mechanism is explicitly presented there as
  an inference from head-unit behavior, not a documented public SDK contract.
  These observations do not prove the same interface or permissions on the 009.
- `telemetry.Signal.STOCK_360` (`0x2031FE00`) tells the application whether the
  factory 360-degree screen is shown. It does not deliver image pixels or a Surface.
- The current code exposes no API for obtaining the factory's stitched frames.
  This establishes what the repo implements, not that every other access route is
  unavailable on the owner's firmware.

## Questions to resolve on the 009

1. Which factory app, native service and SDK actually render the 360-degree view?
2. Is there an exported, callable interface that can deliver the stitched image
   into an application-provided Surface or provide a read-only frame stream?
3. Does a Camera2 ID, stream mode or vendor tag expose this output even though the
   current application selects the four-view composite?
4. What permissions and caller checks apply? Can an ordinary App Lab application
   use the interface on this firmware?
5. Does obtaining the image require opening the factory screen or changing camera
   ownership? Is a second consumer supported without interrupting factory views?

Launching the factory camera UI and consuming its video output are separate
capabilities. A launchable Activity alone does not allow embedding or recording
the stitched image inside our application.

## Read-only investigation sequence

1. Export the existing application diagnostics for the 009's firmware, camera IDs,
   characteristics and supported stream sizes. These describe accessible Camera2
   devices; they are not a dump of all private vendor interfaces.
2. When an authorized shell connection is available, identify the running factory
   camera package and relevant services while its normal 360-degree screen is
   open. Record exact package/service names rather than guessing names from another
   model. Inspect exported components, required permissions and SDK libraries.
3. Inspect accessible factory APKs and vendor SDK/JAR files for read-only image or
   Surface APIs, Binder interface descriptors, callbacks and their caller checks.
   Public ECARX examples on other cars are leads only; validate on the 009.
4. Inventory accessible camera characteristics and vendor tags, then test only
   supported candidate output modes. A special mode's name alone is not evidence
   that it returns the bird's-eye image.
5. If a candidate image interface exists, make a narrowly scoped opt-in prototype
   and verify actual stitched frames, freshness, aspect ratio, lifecycle and camera
   handover. Keep the factory's own view in control when it is active.

Avoid broad signal writes, patching factory applications or treating capture of
the entire screen as an equivalent camera feed. A screen-capture fallback would
have distinct consent, UI and possibly protected-content limitations that need
separate verification.

## Possible outcomes

| Finding | Result |
| --- | --- |
| Accessible stitched stream / Surface API | Reuse the factory calibration and renderer |
| Factory UI can be launched, image output unavailable | Offer a shortcut to the factory view if desired |
| Output requires privileges unavailable to the app | Record the limitation for the tested firmware |
| Only the existing four-view feed is accessible | Consider a separate calibrated stitching prototype |

An application stitcher remains a fallback: fisheye calibration, ground-plane
mapping, overlap blending and live performance would need independent work. The
existing view correction is an approximate lens model, not a complete 009 surround
calibration.

## Branch

`research/factory-360-view` starts from unmodified upstream `main` and contains only
this note. Keep it separate from the installable `side-view` work and from
`research/blind-spot-alerts`. Create a feature branch once an actual usable output
interface has been established.
