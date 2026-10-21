# GPU 360° renderer — alpha 54, 2026-10-20

Current unreleased settings use **System → GPU image processing → Custom →
360° stitching**, or All. See [shared GPU processing](unified-gpu-rendering.md).
The release-specific location below describes alpha 54.

The new backend is opt-in under **Settings → 360° camera → Picture → 360° rendering
→ GPU (experimental)**. Tap Apply changes after changing it. **CPU compatibility
remains the default**, retaining the installed camera route until the owner chooses
the new backend. This is independent of the Developer GPU fisheye switches and is
included in 2.11.54-alpha.1; it is absent from release 2.11.53-alpha.1.

## Rendering

The existing preview output (or existing lowest-priority popup output) is routed
through the app's EGL/SurfaceTexture pipe. There is no new Camera2 declaration,
recording surface, resolution selection or camera mapping. Camera buffers go into
an external OES texture. A fragment shader projects/blends that texture directly
onto the existing TextureView. The main view and popup draw the same vehicle
artwork and footer over it. Camera/input/output texture identities survive mode
changes; no camera/session rebuild is requested just to enter or leave 360.

A background worker builds the existing `BirdEyeProjection.Map` once per
calibration/quality/source change, then encodes two 16-bit UV lookup textures plus
lane/coverage/seam metadata. This shares Manual and 009-fit geometry, role-to-lane
assignments, missing coverage, vehicle footprint and narrow joins with the CPU
renderer. Hardware bilinear sampling is clamped to pixel centres inside the
assigned source lane, including separator edges. UV quantisation and GPU sampling
are not a claim of bit-exact colour equivalence on arbitrary detailed imagery.
Lookup buffers are discarded after upload, and GL textures are released when the
request ends. There is one lookup build in flight, with cancelled generations
unable to publish. No live camera frame is copied into CPU pixels or uploaded as
a bitmap. Explicit stitched-photo export makes a fresh, checked snapshot of the
GPU output; it retains the artwork/watermark and active output resolution.

Low/Balanced/Full retain their output dimensions (320×480 / 640×960 / 960×1440).
GPU mode samples the actual delivered camera buffer rather than CPU readback
sizes. Rendering follows incoming frames, capped at 30 fps for all qualities;
this ceiling does not promise 30 fps from a ≤20 fps source. The unchanged CPU
backend retains roughly 15/10/5 fps caps and its adaptive performance policy.

## Freshness and lifecycle

GL input timestamps must progress; a redraw caused by settings never renews the
source observation age. Each projected swap publishes a bounded record of its
output timestamp and source observation time. The UI accepts only the metadata
for the buffer it actually acquired, with the existing 500 ms freshness limit.
The eight retained records contain no frame pixels. This accommodates normal
TextureView acquisition lag without treating an old camera as live.

Factory/reverse/screen/hidden-view conditions cancel the GPU request and cover its
picture. Changing calibration or leaving 360 invalidates the old generation.
Leaving 360 restores raw/fisheye preview rendering and covers the brief transition
until an acquired normal-preview frame is identified. The input listener keeps
draining the producer while the view is covered. Recording remains on its own
existing surfaces and is neither straightened nor stitched by this feature.

When GPU fisheye preview is also enabled, 360 temporarily takes precedence over
that correction. Its raw source is used for stitching and CPU fallback; exiting
restores the saved fisheye behavior. CPU compatibility alone retains the earlier
refusal to stitch a pre-corrected preview.

## Fallback and limitations

GL-route startup failure returns the original direct camera surface. Unsupported
stitcher texture sizes, shader/upload/draw failures or lookup allocation failure
record a reason and return to the CPU stitcher over the raw preview. The camera
producer surface is retained. CPU fallback waits for an identified raw output,
so it cannot accidentally stitch an already projected picture.

This cannot recover every EGL/driver/camera failure. If the underlying GL route
itself stops delivering output, select CPU compatibility and tap Apply changes. An Android
GPU pipe that initialises does not prove the vehicle HAL accepts its input, which
is why GPU routing is opt-in. Existing camera scheduler priorities and recovery
remain in charge. Car startup, resource/thermal load, performance while recording,
factory takeover and 7X compatibility require physical testing. No factory-like
frame-rate, parking-distance accuracy or added underbody imagery is claimed.

Diagnostics identify the selected/active backend, fallback reason, source/input
and output sizes, lookup time and GL draw/swap timing. CPU readback fields remain
zero on an exclusively GPU-rendered session; the timing is not a GPU timer query
or a physical windscreen/screen presentation measurement.

## Verification

### Front-alignment report — 2026-10-21

The owner supplied factory/app screen photos reporting a front-region mismatch.
The photos do not establish the active projection mode, backend, raw-frame pixels
or matched acquisition time. The generic uncalibrated footer is shown for all
profiles, so it cannot identify Manual versus the experimental 009 fit.

The existing constant-lane tests checked coverage, joins and role assignment but
could not rule out a within-lane UV warp. A new native regression uploads a known
two-axis gradient and spatial checker pattern into a real EGL producer, then
compares the complete CPU output against the acquired GPU output. It covers both
256×1024 and native 1280×5140 strips, Manual/009 fit and normal/remapped roles:
eight combinations, 38,400 output pixels each, including 10,240 front-region
pixels. All eight pass on the disposable API32/SwiftShader emulator. None exceeds
six intensity levels of maximum channel error; whole-image mean maximum-channel
error ranges from 0.028 to 0.039 levels. This supports renderer geometry parity on
that backend; it is not an on-car driver test or a calibration-accuracy result.

No production mapping, owner calibration or GPU default is changed. Active
projection selection still needs confirmation; any front-fit revision needs
matched raw/native and factory ground features. Objects above the reference
ground plane cannot be used as if they were flat road correspondences. Existing
private reference fits are not automatically retuned from these phone photos.

Native tests exercise real EGL external-texture input and TextureView output,
without opening a camera. They compare complete synthetic Manual/009-fit outputs
and swapped lane assignments against CPU geometry, exercise failure/cancellation
and retained input/output surfaces, reject frozen-source redraws and check the
production main-preview route, visible picture, stitched photo and factory cover.
JVM tests cover UV encoding, complete coverage/seam metadata, bounded acquired-frame
history, expiry and cancelled/failed generations. Emulator results do not measure
009 speed.

Final validation on Android API 32 / ARM64 `sdk_gphone64_arm64`, SwiftShader:

- **1,509 JVM tests** pass, including four new GPU mapping/freshness regressions.
- **Seven GPU native tests** pass: full synthetic Manual/009 outputs and remapped
  sources; native 1280×5140 input with excluded separator pixels; popup route reuse;
  injected draw failure and cancellation; actual unsupported lookup-size failure;
  frozen-source redraw rejection; production main view, visible screenshot, fresh
  stitched photo, factory cover and restoration of existing GPU fisheye mode.
- **Eight retained native tests** pass: three popup geometry/lifecycle, three
  vehicle artwork/rendering and two calibration/persistence tests. Fifteen distinct
  Android cases passed across the validation runs; the final GPU-only rerun after
  nonblocking route lookup/cancellation refinements passed all seven cases.
- `:app:testDebugUnitTest :app:lintDebug :app:assembleDebug
  :app:assembleDebugAndroidTest :app:assembleRelease` pass. Signed release assembly
  retains the existing application ID and signing certificate. This evidence describes the initial development build; the feature is included
  in alpha 54, rather than replacing the historical alpha 53 asset.

The first UI screenshot assertion exposed normal acquisition-versus-compositor
lag and now waits for a live presented image rather than taking the first possible
screenshot. It still requires the actual visible projected colours to match the
stitched photo. The frame matcher separately retains bounded metadata for acquired
buffers, since the UI can be behind the latest EGL swap; it never renews source age.

Primary platform references: [SurfaceTexture](https://developer.android.com/reference/android/graphics/SurfaceTexture),
[TextureView](https://developer.android.com/reference/android/view/TextureView),
[EGLExt](https://developer.android.com/reference/android/opengl/EGLExt).
