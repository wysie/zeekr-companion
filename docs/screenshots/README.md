# App screenshots

Captured on **23 October 2026**, using the production UI at `8069737d`
(Zeekr Companion **3.0.0-alpha.1**). The screenshot fixture is Android-test code;
it adds no demo mode, camera simulation or generated image assets to the app APK.

There are **26 screenshots: 13 screens in light and dark themes**. The
[README gallery](../../README.md#screenshots) shows the main screen, side menu,
preview chooser, recording, storage, Super mirror, blind spot camera, HUD,
360° camera, floating button, interface, system and GPU settings. These are
initial scroll positions, not stitched images of entire scrolling pages.

## What is real and what is simulated

Controls, typography, colours, icons, dialogs, preference values and layouts are
captured from the running Android app. Only the camera source pixels are generated.
They are repeated into the app's real TextureViews with advancing frame timestamps;
the normal surround layout and front/rear panes display them.

The fixture replaces the camera manager with test doubles after startup and uses
nonexistent camera IDs. It does not record footage, emulate vehicle telemetry or
demonstrate camera hardware, HUD optics, image quality, frame rate or 360° alignment.
The 360° screenshot shows its settings, not a fabricated stitched result. No owner
photos, raw camera exports or diagnostic logs are used.

Example preferences include English, the 009 profile, three recording sources,
Selected recording sources / Automatic preview arrangement, raw surround panes
set to Fill, and Straighten off. GPU processing is Custom with side, mirror and
playback selected; preview correction and 360° stitching are off. These choices
illustrate the UI and are not recommended defaults. The emulator has no USB drive,
so its actual No USB drive status is retained. Recording remains idle.

Captures use a 2560 × 1600 emulator at density 320 and font scale 1.0. Each saved
image is **2560 × 1432**, clipped to the activity's actual content rectangle.
Android status and navigation bars are excluded; the app's own footer remains.
No UI is repainted or retouched after capture. Lossless WebP encoding reduces file
size without changing screenshot pixels.

## Regenerate

Use a **disposable Android emulator**, not an installed car app. The opt-in fixture
grants test permissions and temporarily replaces the emulator's app preferences.
It restores the three preference files it edits and the previous theme afterward;
generated files remain in the emulator's app-private external directory.

With the Android SDK, Java 17, `cwebp` and `rg` on PATH, start an emulator, then run from
the repository root:

```sh
bash .github/scripts/capture_readme_screenshots.sh emulator-5574
```

The script builds and installs the debug app and instrumentation APK, pushes the
tracked fictional source image, runs only the documentation capture method, pulls
all 26 screenshots and losslessly encodes them into this directory. It deliberately
requires an explicit emulator serial. It does not start or stop the emulator.

Fixture:
[`ReadmeScreenshotsAndroidTest.java`](../../app/src/androidTest/java/com/kooo/evcam/settings/ReadmeScreenshotsAndroidTest.java).

Ordinary instrumentation runs skip this test unless `captureReadme=true` is
provided. Capture uses Android's native screenshot API and content bounds rather
than hard-coded crop offsets. Full-size intermediate PNGs are temporary; committed
WebP images can be opened individually from the README.

## Generated camera source

Source asset: [`source/camera-atlas.png`](source/camera-atlas.png).
Created with the built-in **image_gen** tool in **generation mode**, with no
reference images and an opaque background. The six-tile source is fictional.
The fixture trims two pixels from each tile boundary, packs the bumper/side
images into a four-lane strip and sends the two road images to their own panes.

Exact generation prompt:

```text
Use case: photorealistic-natural. Asset type: synthetic vehicle camera test imagery for documentation, not a UI mockup. Create ONE clean atlas of SIX equal camera images arranged in an exact 3-column by 2-row grid with no gaps, no frames and absolutely NO TEXT, numbers, watermarks or interface graphics. Overall landscape image. All six views show the same fictional clean Singapore residential street and parking area in soft daylight: pale concrete pavement, lush green trees, modern low-rise white apartments, road/parking markings, no people and no readable signs or license plates. Camera at vehicle mirror/bumper height, realistic modest wide-angle car-camera optics with detail retained and natural muted colours. Tile positions: top-left forward road view down the street; top-middle rearward road view back along the street; top-right left-side blind camera looking slightly backward alongside a pale silver-green MPV with only a sliver of its body at the right edge; bottom-left right-side blind camera looking slightly backward with a sliver of silver-green vehicle body at the left edge; bottom-middle front bumper view with pavement immediately ahead and the quiet street beyond; bottom-right rear bumper view with pavement and a parking bay behind. The atlas is only camera source pixels that will be rendered into the actual Android app; do not draw a dashboard, screen, phone, vehicle interior, app, labels, layout controls or decorative borders.
```

## Validation

The debug app and instrumentation APKs build successfully. The opt-in Android
capture passes, and the method skips cleanly without its opt-in argument.
All 26 images were visually checked for the intended page/theme and absence of
Android system bars. README image paths, image dimensions and lossless conversion
were also checked. These checks concern documentation capture, not car operation.
