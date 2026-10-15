# Image-based 009 transform fitting

This is offline research, not a new app calibration or a recovered factory feed.
Existing phone photos and original raw composites can supply an approximate
image alignment. A checkerboard is not a prerequisite for trying that approach.
The earlier statement that new measured targets were needed before any transform
could be calculated was too categorical.

## Method

The supplied night raw composite and factory phone photo show the same carport.
This assumes the vehicle position remained unchanged; matching scenery alone
does not prove that, and any movement can contaminate the fitted mapping.
First rectify the phone perspective using the factory car surround rectangle.
Its assigned 5.2 × 2.0 m dimensions establish an **estimated reference scale**,
not a measured scene. Crop each original 1280 × 1280 lane using its exported
4 px separator layout; never use corrected photos as if they were raw.

Affine-simulated SIFT matching finds candidate image correspondences between
projected raw lanes and the rectified reference. Restrict matches to regions
expected to show the same physical camera, reject controls, deduplicate source
AND destination features, and inspect matches. Repeated floor joints, featureless
flooring, factory blending and different camera viewpoints can create false
matches. A four-point RANSAC fit alone can collapse many source features onto a
few target locations and must not be accepted as proof of a good transform.

`tools/fit_bird_eye_reference.py` consumes inspected normalized raw-lane UV and
reference-plane XY correspondences. It converts UV to rays using the **estimated**
equidistant 190° lens, then fits a 3 × 3 ray-to-reference-plane transform per
camera. This directly fits display mapping without claiming to have uniquely
recovered lens coefficients, physical camera heights or mounting angles. The
ray conversion precedes the homography; a single global warp of the final
four-camera image would not correct four independently misaligned projections.

Nearby source detections are grouped; eight distinct features are required even
to try a camera. At least two groups are held out from fitting. Degenerate
layouts, unstable matrices and bad held-out residuals are reported. Every result
is marked **not installable**, including candidates with small residuals. Input
and result files and owner images remain private, outside tracked source.

```
python tools/fit_bird_eye_reference.py private-points.json private-result.json
python -m unittest discover -s tools -p test_fit_bird_eye_reference.py -v
```

Desktop dependencies are NumPy and SciPy. OpenCV was used to investigate feature
matches but is not required by this fitter or added to the APK. See OpenCV's
[homography explanation](https://docs.opencv.org/4.x/d9/dab/tutorial_homography.html)
and [fisheye model](https://docs.opencv.org/4.x/db/d58/group__calib3d__fisheye.html).

## Results on the supplied carport pair

After grouping nearby source features, front had 42 distinct matches and rear
34. Their transforms gave median errors of approximately **1.45 px** over eight
held-out front matches and **2.95 px** over six held-out rear matches, measured
back in the original 1280 px camera lane. These are same-photo correspondence
errors, not distance errors or an independent accuracy test. The offline
front/rear render follows the factory reference more closely at those features.

Only four left and seven right distinct matches remained. Neither side was
accepted for fitting by the reusable tool. An earlier exploratory left pose fit
missed its held-out point by roughly 70 source pixels. Copying the right-camera
pose to the left is an assumption, not a recovered left calibration. Trials that
fit lens and pose together also traded one against the other; their inferred
physical dimensions should not be presented as measurements.

The source pair is dark, reference perspective/scale are estimated, and feature
coverage is limited. Tall objects matched to the factory rendering may help
replicate its display mapping but do not validate their physical ground position.
An approximate mapping still needs checks on another scene and across the whole
visible region. The separate daytime raw frame allows an extrapolation render;
there is no matching factory daytime reference for that frame to measure accuracy.

No Android preferences, calibration defaults, recording code or release APK were
changed. The partial transforms, correspondence files and comparison images are
kept locally for continued fitting. Obtaining more useful side correspondences
from existing captures comes before requiring the owner to collect new targets.
