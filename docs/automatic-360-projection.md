# Automatic 360 projection

Settings → 360° camera → Calibration → Projection profile now separates the
stored choice from the active renderer:

| Choice | Selected/detected 009 | 7X | Unidentified model |
| --- | --- | --- | --- |
| Automatic | Experimental 009 image fit | Manual geometry | Manual geometry |
| Manual | Saved manual geometry | Saved manual geometry | Saved manual geometry |
| 009 image fit | Experimental 009 image fit | Inactive; use Manual, retain choice | Explicit experimental 009 fit |

Choose the model under Settings → System → Vehicle model. Auto model detection
requires an explicit Android model name; the owner's generic Pixel/sargo identity
does not identify their 009. Stop recording before changing model, then exit and
reopen as the selector instructs so source mapping and the camera pipeline rebuild.
Projection choice changes use the existing live calibration listener for main,
popup and photo rendering. They do not alter recording sources or raw recordings.

## Preserve intent during upgrades

The existing integer preference `bird_eye_calibration/projection_profile` retains
its historical explicit meanings: `0` = Manual, `1` = 009 fit. New value `-1` means
Automatic. A read or opening the settings never writes an override.

If no projection key exists, the app checks for saved manual geometry: vehicle
length/width/manual margin and each camera's height, pitch/yaw/roll, FOV, optical
centre, offsets and mirroring. Any such key preserves Manual, even if its value
happens to equal the default. Old versions did not write a projection choice when
these sliders were edited. No comparison with current defaults is used to infer
intent. An unknown existing projection value conservatively stays Manual.

Quality, source-lane assignments and fitted-view margin alone do not imply manual
lens tuning: both projection methods use them. An untouched setup, or one with
only those settings, uses Automatic. A new user edit of manual geometry explicitly
pins Manual; merely opening a calibration dialog does not. Quality/source/fitted
margin changes do not pin it. Saved choices still take priority over geometry.

Choosing Automatic explicitly writes `-1`, overriding that legacy inference while
keeping all lens values, source assignments and quality. Choosing Manual later
restores those saved values. This is different from **Reset calibration and
quality**, which clears calibration/quality and returns to Automatic/model defaults.
Vehicle illustration/colour and popup placement remain separate.

An existing explicit 009 fit on a selected 7X is retained and shown as **inactive
on 7X**, with Manual rendering. It is not misleadingly displayed as Automatic or
applied as a 7X calibration. Switching back restores it. Otherwise a 7X picker
offers Automatic and Manual. No fitted 7X calibration has been established.

The option summary shows the current Automatic result, so the user can see which
projection will be used. The 009 transforms remain empirical estimates from owner
images, not measured lens calibration or verified distances. See
[009 fit evidence and limits](009-image-fit.md).

## Validation

Seven host policy regressions cover fresh defaults, legacy explicit Manual/009,
all manual geometry fields, non-geometry preferences, explicit Automatic retaining
tuning, 7X suppression and unknown stored values. Native vehicle/projection tests
exercise real preferences, model changes, manual edits, source/quality retention,
Automatic/Manual/009 confirmation and Cancel, inactive saved 009 presentation,
calibration reset and artwork independence. They use a disposable emulator, not
vehicle cameras; model-specific alignment and frame rate are unchanged claims.

Final checks passed: 1,479 JVM tests, nine native Android vehicle/projection/artwork
tests, lint and debug/test/release APK builds. Picker and resolved Automatic
summary screenshots were inspected at the existing head-unit-sized configuration.

This change is Unreleased; earlier published APKs keep their original defaults.
