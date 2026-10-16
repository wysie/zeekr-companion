# Experimental 009 image-fitted 360 projection

Choose Settings → 360° camera → Calibration → Projection profile → **009 image
fit (experimental)**. Manual remains the default, including on upgrades. The new
profile requires a subsequent build; published 2.11.43-beta.1 is unchanged.

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

The fitted reference footprint is fixed. Margin is limited to 0.5–2 reference
units to avoid extending the view beyond the supplied framing. Existing larger
manual margins are retained and restored on returning to Manual. Camera source
assignments remain independent of physical camera roles, as in the manual model.
Quality and performance fallback are unchanged. Balanced still uses a reduced
working copy; Full requests the live native buffer within the existing budget.

Physical-lens sliders do not apply to an empirical transform, so its calibration
dialog shows source selection, quality and margin instead. Switching back to
Manual restores the saved dimensions and lens parameters. Reset calibration and
quality returns to Manual and existing defaults. Diagnostics/photo sidecars name
`009-image-fit-20261016` and record its transforms/reference units.

Invalid/non-finite or singular transforms are rejected. Rays outside lens/UV
coverage or on the wrong side of the body are not clamped into fabricated edge
imagery. Existing lane-centre bilinear sampling and narrow joins remain in use.

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
