# 7X DIY 360: evidence still needed

The shared four-camera renderer and a 7X illustration are implemented. This does
not establish accurate stitching, compatible camera output combinations or good
performance on a real 7X. Manual defaults are estimates. The 009 image-fit profile
was fitted to one owner's 009 references and is not a 7X calibration.

The camera-stream profile retains a historical 7X name but is also used on the
009; that name cannot establish vehicle identity or calibration compatibility.
Artwork selection changes no camera channels, lens parameters, body dimensions
or projection profile. System → Vehicle model is the functional model selector;
selecting/detecting 7X prevents the 009 image fit from being applied while retaining
its saved values for a later switch back. No 7X-specific camera calibration has
been added. See [model defaults and evidence](vehicle-model-profiles.md).

## Useful first contribution from a 7X owner

Choose **Manual** under 360° camera → Calibration → Projection profile. For an
initial comparison, keep the vehicle stationary on a flat, clearly marked area;
use painted lines, paving joints or other identifiable ground features. This is
a collection procedure, not a new software parking/speed restriction.

1. Export Diagnostics, including the installed app version, camera sizes,
   mapping, calibration values and DIY 360 quality/timing. Include the vehicle's
   model year, market and head-unit firmware when available.
2. Export the **original raw surround photo** and its companion notes. Return to
   Four cameras for raw capture; a phone photograph of the screen is not a
   substitute for the original image pixels. The source lane order, orientation,
   padding and dimensions must be checked before fitting transforms.
3. Without moving the car, capture the factory top-down display, our DIY 360 view
   and the four-camera view. Save the fresh stitched 360 PNG and notes as well as
   phone pictures where possible. Repeat at a few different marked locations;
   one garage image is insufficient to establish general alignment.
4. Check popup/main/photo consistency and whether opening/closing the 360 popup
   interrupts ordinary recording. A short exported recording and logs help check
   frame cadence and camera contention. Keep factory/reverse priority enabled.

After lane/orientation validation, fit a separate 7X profile to ground features
and check it against captures excluded from the fit. Measured geometry or a
calibration target is needed for distance accuracy; visual agreement with a
factory screenshot alone does not establish metric accuracy. Camera height,
mounting, lens centres/distortion and firmware differences can affect results
even when the composite format matches the 009.

Keep original exports private unless the owner deliberately shares them; they
can contain surroundings, plates, locations and device details. Generated car
artwork does not improve the underlying camera resolution or stitching.
