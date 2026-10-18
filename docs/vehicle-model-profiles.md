# Vehicle model selection and evidence

Settings → System → **Vehicle model** offers Auto, Zeekr 009 and Zeekr 7X.
This replaces Interface → Vehicle name and the separate 009 camera-mapping switch.
Stop recording before changing the model; an active recording rejects the change.
It is a functional default selector, not a claim that every firmware exposes the
same sources. Exit and reopen after changing, so the camera pipeline can rebuild.

Auto parses explicit model names in Android MODEL/DEVICE/PRODUCT. Generic names,
shared platform names and contradictory model strings are not accepted. The
owner 009 reports Pixel 3a/sargo, so Auto cannot identify that car: choose 009.
The setting shows unresolved detection and asks for a manual choice. It never
infers a vehicle from camera IDs, stream dimensions or a display number.

## Camera defaults

| Source | 009 owner export | Upstream 7X evidence |
| --- | --- | --- |
| Surround | ID 3, four fisheye lanes in 1280×5140 | ID 2 in the documented head unit, vertical surround composite |
| Front road | ID 0, captured 2560×1440 | Not identified; requires manual mapping |
| Rear road | ID 1, captured 2560×1440 | Not identified; requires manual mapping |
| Cabin | ID 2, monochrome; one confirmed feed | Front cabin ID 0 and rear cabin ID 1 in the documented head unit |
| Second 009 cabin | Not identified; unassigned | Two-cabin discovery retained |

Surround detection now takes priority over the 009 ID-3 fallback; an explicit
manual source still wins. Automatic reset clears the explicit choice and returns
to discovery. See [shared mapping and resets](shared-camera-sources.md).

The 7X runtime retains the existing surround-signature/cabin discovery path,
rather than forcing the table IDs on every firmware. An unknown model uses that
generic discovery path and leaves new dedicated road roles unassigned. Manual
mapping has no default. Camera2 FRONT/BACK metadata does not establish a physical
road/cabin role.

Sources: [009 exported image review](009-diagnostics-2026-10-17.md#exposed-camera2-images)
and the upstream [platform evidence, §1.6](zeekr-platform-notes.md#16-朝向与镜像正常视角只有一个2026-09-26).
These establish the pictures in tested exports, not simultaneous output support,
higher-resolution live recording or the complete set of cameras in the vehicle.
The 7X platform notes also document rear-cabin/surround resource conflicts; a
listed source is not a guarantee that every selected combination can run together.

## What changes and what stays saved

- Model selection supplies automatic source IDs, builtin profile/status labels
  and Auto 360 artwork. Custom profile names remain unchanged.
- Ordinary manual source overrides (including Unassigned) take priority. Model
  selection does not enable recording groups, change preview mode or turn on
  automatic recording. Existing quality and picture adjustments stay saved.
- An enabled old functional `camera_mapping_009` preference is read as 009 until
  an explicit new selection is saved. An explicit Auto/7X selection takes priority
  over that legacy value. The old cosmetic name is not promoted into a hardware
  selection; choose the functional model once if only the old label was selected.
- HUD routing still checks actual public display signatures/eligibility. The
  confirmed 009 display-3 1280×640 match selects its existing independent right-hand
  layout defaults. Generic/7X matching and saved layout remain separate. Model
  selection cannot force a HUD destination or suppress factory graphics.
- 360 projection offers Automatic, Manual and 009 image fit. Untouched setups use
  Automatic: selected/detected 009 uses the experimental 009 fit; 7X/unknown uses
  Manual. Existing explicit choices and legacy manual lens edits stay preserved.
  Choosing Automatic retains tuning; Reset calibration clears it and restores
  Automatic. An explicit saved 009 fit on 7X is shown as inactive and retained for
  switching back, never silently relabelled Automatic. There is no verified 7X fit.
  See [projection selection and preservation](automatic-360-projection.md).
- Blind-spot picture width/angles, correction fitting and other common defaults
  remain shared; no measured per-model replacements have been established.
  These controls remain adjustable and are never reset on each launch.

Diagnostics JSON records `vehicle_profile_mode`, `vehicle_profile_resolved` and
its display label. The historical `vehicle_name_mode` field remains for interpreting
older exports and is no longer the functional selection.

## Validation

Host tests cover migration precedence, conservative Auto/unknown/conflicting names
and avoiding guessed 7X road IDs. Native emulator tests exercise the System selector,
localised labels, actual AppConfig/ProfileSizes integration, legacy migration,
manual-source/preview/picture/recording-selection preservation, and retention of
009 fitted settings while switching models. Existing recording-group and HUD
window tests run alongside them. These checks do not replace vehicle validation
of camera availability, concurrency, geometry or HUD physical placement.
