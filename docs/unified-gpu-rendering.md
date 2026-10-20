# Shared GPU image processing — 2.11.55-alpha.1, 2026-10-20

Settings → System → GPU image processing (experimental) offers Off, All and Custom.
The five choices are surround preview correction, 360° stitching, blind-side camera,
normal Super mirror and video playback correction. Blind-side includes the popup,
HUD, Super mirror replacement and its settings picture. The normal mirror continues
to crop front/rear/left/right from the mapped surround composite; dedicated road
sources are unchanged and are not used by Super mirror.

Off selects the existing direct/Canvas mesh/CPU stitcher paths. All selects every
GPU path. Custom enables independent switches. The rows show effective state in
All/Off; their saved Custom choices survive both modes. New installs default Off.
Without the new mode key, getters retain the old effective GPU selections, including
the old developer gate. The first explicit mode selection snapshots those effective
legacy choices as Custom values; editing a legacy Custom switch also freezes that
policy before saving. It never automatically opts an upgrade into new
side/mirror routes. Explicit System selections are independent of Developer mode.

The old Developer preview/playback GPU controls are removed; the 360° rendering
entry links to this same page. Straighten controls still decide whether correction
is active; the rendering choice decides the backend. Camera changes participate in
the existing Apply flow and retain applied session options until the camera rebuild.
Preview-only rendering changes do not alter recording compatibility or encoder
surfaces. Playback applies the saved choice when next opened.

## Side and mirror renderer

Each existing optional camera output can use `FisheyeGlPipe` instead of its direct
surface. Camera2 still receives the same native input dimensions, output declarations,
scheduler ownership and priorities. A dedicated per-pixel side shader produces a
window-sized image. It implements raw crop/rotation, rectilinear mirror correction,
and aimed/rolled side correction. CPU `SideViewFraming` supplies its cached fitted
field of view; GPU ray projection uses the same yaw/pitch/roll ordering. Mirroring
is applied in output coordinates. Missing coverage is black; sampling is clamped
to pixel centres inside the selected lane, excluding separators/adjacent cameras.
HUD inner-edge cropping, tone, fades, badges and factory covers remain outside the
shader and retain their existing geometry/authority. Normal mirror rear-only
mirroring, view selection, pan, gestures and replacement restore behavior remain.

The renderer is a temporary request on the retained GL input. Picture/size changes
replace its generation, not the camera producer. Acquired output timestamps are
matched to bounded source-observation metadata. Settings redraws cannot renew frozen
source freshness. Cancelled generations cannot publish as live. Failure returns raw
output for the mesh renderer when possible; the UI waits for an acquired raw frame
before applying mesh correction, avoiding correction of an already projected image.
GL startup refusal retains direct output. A stopped EGL/driver route may need Off
and Apply changes; automatic recovery from every hardware failure is not claimed.

## Settings picture

With Blind-side GPU enabled, the settings TextureView is a second EGL destination
of the existing main raw-input pipe. It adds no Camera2 declaration, opens no camera
and performs no live bitmap readback. It uses the same side shader as the actual
window, independently of main surround correction. It still requires an attached
Surround preview source. Its GL request is cancelled on hide, pause, factory
takeover, source loss and removal. Last queued output is drained beneath the cover;
hiding/removing the preview detaches the settings consumer so it cannot block the
shared pipe. Compatibility retains the bounded native-detail bitmap sampler.

If GPU surround correction is enabled while Blind-side uses compatibility, the
CPU sampler refuses the already-corrected source. The visible explanation directs
the owner to enable Blind-side GPU or disable Surround preview correction in Custom
and apply. A failed GPU settings destination can use the raw compatibility sampler
when available; it does not repeatedly recreate a failed tap until restart/source
replacement. Factory, screen, connection and stale-frame guards remain in place.

## Validation limits

Native EGL/TextureView tests use synthetic source patterns on an API 32 ARM64
emulator. They verify projection pixels, acquired-frame freshness, raw fallback,
retained input surfaces and actual popup/mirror integration. They do not prove
009/7X camera HAL acceptance, windscreen brightness, live frame rate, thermals or
recording performance with all outputs enabled. Physical vehicle testing is still
required; no hardware capability is inferred from successful emulator GL setup.

## Verification — 2026-10-20

- `:app:testDebugUnitTest`: 1,516 tests, zero failures/errors.
- `:app:lintDebug`: zero errors; debug, instrumentation and signed release APKs build.
- Focused native rendering suite: 29 passed. Includes 13 EGL/360/side/mirror cases,
  two policy/Settings cases, three popup cases, two settings-preview cases, three
  compatibility side-rendering cases and six HUD picture cases. Patterned input
  checks analytic projection, raw/straightened modes, left/right roll, mirroring,
  Auto-fit, multiple aspects, all four normal mirror lanes and raw settings taps
  while the main output is corrected. Actual mirror and popup tests retain their
  producer when changing projection/returning to compatibility. Frozen source
  and shader-failure cases cannot publish an old projected frame as fresh raw.
- Focused camera Apply/recording suite: seven passed, including finalized playable
  clips, manual/automatic requests, cancel/stop behavior and pending preview choices.
  The first broad run lacked emulator `MANAGE_EXTERNAL_STORAGE` access: output
  folder creation failed and the writer reported ENOENT. After granting access to
  this disposable emulator app, all seven focused cases passed. That broad run also
  unintentionally included inherited screenshot tests; it is not claimed as a pass.
- Actual System GPU page inspected in light and dark themes on the 2560×1600 API32
  emulator. Effective All/Off state, editable Custom state, retained profile values
  and pending Apply behavior pass. Scoped Impeccable detector returns no findings.

Local evidence: `/tmp/companion-unified-gpu-verified-build.log`,
`/tmp/companion-unified-gpu-renderer-recheck.log`,
`/tmp/companion-unified-gpu-recording-recheck.log`,
`/tmp/companion-unified-gpu-ui-detect.json` and
`/tmp/companion-gpu-system-{light,dark}.png`. These temporary files are not release
assets. Recordings are checked with the emulator camera; synthetic GL tests and
emulator recording checks do not establish simultaneous-output performance on a car.

## Experimental blind-side upscaling

Blind spot camera → Picture → GPU picture quality offers Bilinear (default),
Bicubic soft (Mitchell-Netravali, B=C=1/3) and Bicubic sharp (Catmull-Rom,
B=0, C=1/2), plus independent 0–100% sharpening (default zero). They are shared
by popup, HUD, Super mirror side replacement and the settings raw-input tap,
including when Straighten is off. Normal mirror, surround/360°, playback and
recording do not use these sampling choices. No camera topology flag changes;
with an active blind-side GPU route, editing them replaces shader parameters live.
GPU Off/compatibility retains the saved choices without applying them.

The cubic kernels reconstruct from a 4×4 native-pixel neighbourhood after mapping
each destination pixel into its side-camera lane. All taps are clamped to native
pixel centres within that lane before the OES texture matrix. This avoids camera
separator/adjacent-lane bleed. Cubic overshoot is limited to the neighbourhood's
channel bounds. Optional sharpening adds a bounded native-neighbour high-pass term,
not a second dewarping pass; unsupported destination coverage still returns black.
Labels and arrows remain separate final-resolution Canvas overlays. This is
spatial reconstruction, not AI enhancement or recovered optical detail. Cubic
uses 16 texture samples per output pixel, plus five when sharpening is enabled;
car GPU performance and noise need owner comparison. Defaults retain one bilinear
sample and no sharpening.

Validation on a disposable API 32 emulator, 2026-10-20:

- 1,528 JVM tests pass; Android lint has zero errors; signed release/debug and
  instrumentation builds pass.
- 23 relevant native tests pass: actual OES pixel results match an independent
  CPU cubic/sharpening reference across both side lanes and method/strength changes;
  methods yield distinct reconstructed pixels while retaining the input producer.
  Corrected/raw, rotated, mirrored and unsupported-coverage projections pass for
  all three methods. Existing GPU fallback, acquired-frame freshness, native 009
  separators, Super mirror routing and popup lifecycle checks also pass.
- Native Picture controls preserve 40° aim back, 11:5 aspect and recording choices;
  preferences persist, are included in settings export, survive GPU Off/All, and
  reset to Bilinear/zero. Camera topology flags remain unchanged. Actual light/dark
  Picture screens were inspected, with a truthful unavailable-camera preview on the
  emulator. No camera image, frame-rate or physical HUD validation is claimed.

This does not resolve the owner's separately reported HUD label/arrow clipping;
that report still needs evidence from the physical HUD.
