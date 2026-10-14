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

## Additional leads checked on 2026-10-14

Ferix's CAMCORDER remains at `c6d8b6335799ca863a186f2530a45748827864b9`.
Its scanner is a vehicle-signal tool; it does not expose stitched images.
[OpenAVM's platform notes](https://github.com/Dantenothing/openavm-recorder/blob/main/PLATFORM_NOTES.md)
also describe the same vertically packed Camera2 feed, not a demonstrated factory
bird's-eye endpoint. Neither check establishes a usable stitched feed on the 009.

Android's [EVS overview](https://source.android.com/docs/automotive/camera/evs)
and [Vehicle Camera HAL documentation](https://source.android.com/docs/automotive/camera-hal)
provide another investigation route. EVS Manager can share camera buffers with
additional clients, while the underlying hardware HAL has exclusive ownership
behavior. EVS camera buffers alone do not establish access to the factory's final
stitched rendering.

A separate AOSP `android.hardware.automotive.sv@1.0` interface includes
`ISurroundView2dSession`, documented as a top/bird's-eye view. The
[reference client](https://android.googlesource.com/platform/packages/services/Car/+/26f47c7/surround_view/app/sv_app.cpp)
starts a 2d session and receives its stream through a callback. This establishes
that such an Android interface exists; it does **not** establish that ZEEKR ships
it, uses it for the factory view, or allows an App Lab app to call it. Inspect the
009's HAL/service inventory and the factory renderer before implementing a client.

Potential inventory names are `android.hardware.automotive.evs`,
`android.hardware.automotive.sv`, EVS Manager and vendor AVM services. Their presence
would be a lead, not proof of a permitted or calibrated output. Do not open the
hardware HAL or run a reference demo as an inventory test: opening a hardware
camera/display can displace its previous owner according to the AOSP contract.

## Collection tool

`tools/collect_factory_360.py` collects read-only metadata through an already
connected, authorized ADB device. It records selected build properties, camera
service state, package candidates, focus/window names, service/HAL inventories,
framework paths and the vendor manifest. It records command failures separately;
missing permissions are not evidence that an interface does not exist. It requests
no video frames, launches no app and changes no vehicle settings.

The script was checked locally with simulated ADB responses. No actual vehicle
was attached; no factory package name or feed API has been identified on the 009.

1. Export the normal app Diagnostics JSON first, noting the 009's firmware.
2. With the factory 360 window closed, collect a baseline:

   ```sh
   python3 tools/collect_factory_360.py --label idle --out /tmp/009-360-idle
   ```

3. Open the normal factory 360 window manually and collect a second snapshot:

   ```sh
   python3 tools/collect_factory_360.py --label factory-360 --out /tmp/009-360-open
   ```

4. Compare window/package/process hints and the service manifests. Once the actual
   factory package is identified, collect its permissions/components and, optionally,
   readable APKs:

   ```sh
   python3 tools/collect_factory_360.py --label factory-package \
     --package ACTUAL.PACKAGE.NAME --pull-package --out /tmp/009-360-package
   ```

   Replace the placeholder with a package established by the first snapshots.
   `--pull-package` copies files to the Mac; it does not modify the installed app.

Each run writes a `.zip` next to its output directory. Existing reports are never
overwritten. Use `--adb /path/to/adb` if it is not on PATH, and `--serial SERIAL` if
more than one Android device is attached. The tool does not enable ADB or connect
to a guessed address. Reports may contain device/package details; collect them for
private analysis rather than committing them or factory APKs to this repository.

## Branch

`research/factory-360-view` holds research notes and the collection tool; it is not
the combined application or a 360 implementation. Installable features live on the
fork's `main`. `upstream-main` remains an unmodified dts88 mirror. Create a separate
feature branch only after an actual usable image-output interface is established.
