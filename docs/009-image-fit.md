# Experimental 009 image-fitted 360 projection

Choose Settings → 360° camera → Calibration → Projection profile → **009 image
fit (experimental)** to force that choice, or use Automatic to follow the vehicle
model. Untouched setups use Automatic (009 fit for 009, Manual for 7X/unknown);
existing explicit choices and manual lens edits are preserved. See
[selection and migration](automatic-360-projection.md). The original profile shipped in 2.11.44-alpha.1. The framing, sampling and
backing refinements below are subsequent source changes, not changes to that
published APK.

The profile changes the mapping of the current live raw camera pixels into the
DIY ground view. It embeds numeric transforms, no owner images or cached scene
content. Main preview, popup and stitched-photo export share this mapping. Camera
acquisition, recording, factory/reverse priority, freshness covers and output
arbitration are unchanged. No gear/speed gate was added.

The four transforms were fitted from owner-supplied original 1280×5140 composite
and factory top-down photo references. Factory overlay corners provided a
perspective/reference footprint of 5.2×2 units; that scale is assumed, not measured.
The lens normalisation is estimated equidistant 190° with centre .5/.5. These
matrices combine remaining lens/mount/reference error empirically. They are not
recovered factory intrinsics, measured camera poses or verified parking distances.
Do not apply this 009 profile to a 7X.

## Settings and rendering

The fitted reference footprint is fixed. View margin now defaults to 3 and is limited to 0.5–3 reference units.
It is saved independently from the Manual margin. Existing fitted installations
initially use the new wider default; Manual values are retained. Use a lower
View margin to zoom in again. The expanded extent was checked for finite, valid
lens coverage; it does not change any of the four fitted matrices or establish
accuracy outside the reference features. Camera source
assignments remain independent of physical camera roles, as in the manual model.
Quality controls and performance fallback remain available. Balanced uses a reduced
working copy; Full requests the live native buffer within the existing budget.

Physical-lens sliders do not apply to an empirical transform, so its calibration
dialog shows source selection, quality and margin instead. Switching back to
Manual restores the saved dimensions and lens parameters. Reset calibration and
quality clears tuning and returns to Automatic and model-dependent defaults. Diagnostics/photo sidecars name
`009-image-fit-20261021` and record its transforms/reference units; older captures
name `009-image-fit-20261016`.

Invalid/non-finite or singular transforms are rejected. Rays outside lens/UV
coverage or on the wrong side of the body are not clamped into fabricated edge
imagery. Lane-centre bilinear sampling remains in use. The image-fit blend corridor is
0.12 reference units (previously 0.20); Manual retains its existing 0.20 corridor.
This reduces averaging of slightly displaced detail, but cannot align raised
objects or resolve inaccurate geometry. Balanced now copies 768×3084 instead of
512×2056 from the supplied 009 stream at unchanged 640×960 output; additional
readback cost may reduce cadence before the existing resolution fallback.

## Evidence

The new correspondence set has 21/62/15/27 distinct source features for
front/rear/left/right after merging detections within 10 source pixels. All four
pass same-capture holdouts. Full-data matrices, not holdout training subsets, are
embedded after the checks. Same-photo fits alone do not establish generalisation.

The earlier capture's 91 reference correspondences were **not used to fit** these
matrices. Median reprojection errors in its original 1280-pixel source lanes:

| Camera | Earlier reference points | Generic estimate | New image fit |
| --- | ---: | ---: | ---: |
| Front | 43 | 65.50 px | 7.89 px |
| Rear | 36 | 41.29 px | 7.59 px |
| Left | 4 | 60.82 px | 10.38 px |
| Right | 8 | 151.50 px | 3.61 px |

These are image-reference alignment errors, **not real-world distance errors**.
The earlier capture is also from the same garage, with few side correspondences.
The largest earlier left residual is about 77 px; outer-view errors and seams can
remain. The held-out capture therefore supports an experimental test profile,
not a claim of factory-equivalent calibration across scenes.

The exact Java renderer was run on the owner's native raw frame for both profiles.
The fitted result narrows the broad front band and better matches ground scale
and the car behind. Both comparison renders use the same source/resolution;
neither is a phone screenshot of the new build. Private sources, factory
normalisation, matches and renders remain untracked under
`artifacts/009-360-fit-20261016/` and are excluded from Git/APKs.

## Reproduction and tests

`tools/fit_bird_eye_reference.py` accepts JSON points containing physical camera
role, original-lane UV and approximate reference-plane XY. It deduplicates,
checks spread, fits ray/plane homographies with robust refinement and reports
held-out source errors plus reviewed full-data matrices. It never writes app
settings or automatically promotes a fit. Requires NumPy/SciPy. Owner images and
the fitting set are private; the earlier anonymous coordinate-only regression
fixture is in `app/src/test/resources/009-reference-rays.csv`.

`tools/BirdEyeRawFixture.java INPUT OUTPUT.png --009-image-fit` reproduces the
actual Java mapping offline; omit the flag for generic defaults. See its header
for compilation. Native-buffer geometry must be the original 1280×5140 strip.

JVM tests cover earlier-capture improvement per camera, finite/outward coverage,
source reassignment, separator boundaries, live-buffer updates, invalid matrices
and bounded profile extents. Native tests cover persisted opt-in, manual-value
preservation, reset, picker confirmation/recreation and day/night layouts.

On-car checks remain: compare main and popup at the same stationary scene, save
stitched photos/sidecars, check all four corners and nearby ground markings, then
check frame cadence while recording. Factory/reverse takeover and stale-frame
covers retain their existing priority. This profile does not access the factory
stitched video feed.

## Framing/detail refinement validation (17 October)

The new factory/app phone-photo pair was compared with the retained original
raw capture and stitched export. Background feature matching associated the app
phone photo with the saved export (164 inliers); this supports reproducing that
view, not proving installed version or measured calibration. Approximate screen
perspective correction showed that the old fitted framing was tighter than
factory. The new margin changes the extent only, retaining all four matrices.

The full 1,424 JVM tests, debug lint and signed release assembly passed. Six
Android instrumentation checks passed, including profile persistence/reset,
independent Manual/fitted margins, all six model/colour combinations at three
sizes against day/night backgrounds, unchanged ground outside the conservative
illustration bounds, opaque footprint corners and live appearance changes
without invalidating the camera frame.

The opt-in raw-review check rendered the same private 1280×5140 capture through
the Java map and Android Canvas at 512/768/1280-pixel source widths and margins
2/3. Six images were exported privately and the final wider view inspected.
In a fixed ground-region comparison, the 768-pixel source was closer to native
source rendering than 512; native sampling is only a same-projection detail
reference, not the factory image or optical ground truth.

Run `BirdEyeRawReviewAndroidTest` with instrumentation argument `bird_eye_raw`
pointing to an accessible private original capture; without that argument it is
skipped. Owner images/outputs are not bundled or committed. The previous
`BirdEyeRawFixture` still reproduces the close margin-2 view at native source
resolution; the Android review covers the new framing and source sizes.

The final frame was generated offline. Actual copy cost, sustained frame rate,
wider-view alignment and factory takeover still need an on-car check. Higher
Balanced source resolution uses 2.25 times as many input pixels; cadence and
resolution fallback are retained. Some corner mismatch and stretching of walls
and gates remain; narrowing blends does not recover missing geometric calibration.


## Road/ground alignment refinement — 21 October

The owner clarified that the concern is alignment against the factory view, not
merely the visible front bodywork strip. The matched raw captures at 10:57:38 and
10:57:46 are native 1280×5140 camera-3 composites with the same bus/kerb/road scene
as the supplied factory/app phone photos. Four diagnostics confirm alpha 58,
explicit 009 fit and GPU All; the runtime snapshots are outside an active DIY
view, so they do not establish the actual renderer of the photographed frame.
The exact old Java CPU map reproduces the app photo's ground geometry, making a
calibration correction relevant to both rendering backends. The existence of a
similar strip in the factory view does not establish that alignment is correct.

The revised `009-image-fit-20261021` coefficients jointly constrain each camera to
both the previous garage points and reviewed flat-road correspondences. New road
reference features exclude the van, own bodywork, tall walls and factory controls.
The factory phone view is approximately rectified using its vehicle-footprint
corner markers, retaining the assumed 5.2×2 reference footprint. There are 125 prior
training points and 137 new road training points. Each capture contributes equal
total residual weight per camera, preventing the more numerous road points from
replacing the previous scene. DLT initialisation is followed by weighted robust
least-squares refinement (`soft_l1`, 0.025 reference-unit scale). The estimated
190° equidistant lens normalisation and .5/.5 centre are unchanged.

The road set has 32 points held out with deterministic seed `360 + role`, after
10px native-feature deduplication; they are excluded from optimisation. Numeric
held-out UV/reference coordinates are the anonymous regression fixture
`009-road-heldout-20261021.csv`. Input media, matches, training data, fit scripts,
hashes and comparison renders remain private under
`artifacts/009-front-review-20261021/`; no owner images are embedded in the app or
committed. The second raw capture is a repeat at the same location, not an
independent environment. The earlier 91-point capture remains excluded from fit.

| Camera | Held-out road points | Previous median source error | Revised median source error |
| --- | ---: | ---: | ---: |
| Front | 5 | 5.62 px | 1.38 px |
| Rear | 10 | 9.34 px | 4.89 px |
| Left | 9 | 16.42 px | 4.51 px |
| Right | 8 | 8.27 px | 1.01 px |

These are native-source image-reference residuals, not real distances or full-frame
error bounds. Tail errors also improve on each road hold-out group. Earlier
independent medians are 6.02/7.61/9.73/6.34px for front/rear/left/right and remain
inside the existing <12px per-camera regression bound. The earlier right median
is worse than the old fit's 3.61px; the prior garage training residuals also rise.
The change trades exact fit to one photo for improved cross-scene alignment; it
does not assert that every old scene or every pixel improves. Phone perspective,
flat-plane assumptions, limited side references and raised objects remain limits.

The profile identifier changes so Diagnostics and photo sidecars distinguish the
new fit. Automatic-on-009 and explicit 009 selection receive it without resetting
preferences. Manual values, model/colour, lane assignment, quality, margin,
recording and camera-session rules are unchanged. No extra mask, fake road pixels,
vehicle stretch or intermediate image warp is introduced. No new missing ground
is found versus the previous fit over the tested 0.5/2/3 margins.

Validation: 1,545 JVM tests pass, lint has zero errors, and debug, signed release
and instrumentation builds pass. Twenty-two native tests pass across detailed
CPU/GPU UV parity (including front and native 009 separators), source reassignment,
coverage/blending, freshness/failure/retained producers, main/photo/factory-priority
behavior, CPU rendering and persisted profile/manual-value selection. Production
Java renders of both new raw captures and the retained garage capture were
inspected; same-source before/after comparisons use identical framing and artwork.
Owner verification of actual vehicle alignment remains pending. This revision is
included in 2.11.60-alpha.1.
