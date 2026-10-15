# Camera and display identification

Enable Developer mode under **Settings → System → Enable developer mode**.
Read the warning beside the switch; no password or About-page gesture is needed.
The selection persists across restarts/updates. Turning it off hides the Developer
category and disables developer-gated settings while retaining their saved values.


Open **Settings → Developer → Camera and display tools**. Unlock Developer options
first if hidden. Stop recording and close the factory camera.
A connected vehicle interface is required. There is no gear or speed gate.
Factory camera/parking assistance, interface loss and screen-off block or cancel the tools.

## Export all camera feeds

One tap enumerates all exposed Camera2 IDs, then opens available IDs one at a time.
Each successful ID saves one uncorrected JPEG, with ID, lens facing, sensor
orientation, physical IDs, advertised PRIVATE/JPEG/YUV sizes and capture result
in `report.json`. The image is a representative supported output, not a sweep of
every size/format combination. Files retain native orientation, without app
straightening, mirroring, labels or stitching. A JPEG stream can have different
geometry from the preview (for example a four-quadrant surround image).

JPEG capture uses a temporary PRIVATE repeating sink when available and waits for
three advancing preview frames before the still. If JPEG is not advertised, a
supported YUV output is sampled after three advancing frames and packed into a
JPEG, respecting plane row/pixel strides. Unsupported sizes/stream combinations,
busy IDs and permission failures are reported rather than silently mapped to a
different camera. Size selection favors preview aspect ratio, bounded to 8 MP and
6000 pixels on either axis; encoded JPEG data is bounded to 32 MiB.

The app refuses scans during recording, pending/recovering recording, cleanup or
a photo. It claims a runtime lease and asks the existing scheduler to close its
usual feeds in order. It waits up to 30 seconds for strict native closure across
live managers, without relying on the scheduler's elapsed-time closure registry.
Existing claims and saved settings remain intact. Recording/photo requests cannot
start while the lease is held; normal preview/mirror claims can resume afterward.

Each ID has a 12-second capture deadline and 10-second close-confirmation wait.
Stop or leaving the foreground cancels the scan. If native opening/closing remains
unconfirmed, further IDs are skipped and the lease stays held, including for late
callbacks. It releases only after definitive failure/closure; restart the app if
it does not recover. No forced retries or takeover of reported-busy IDs occur.
Cancellation and failure retain the partial JSON report when storage is writable.

Files are in a unique timestamped directory:

```text
ZeekrShortcutApp/Developer/CameraScan_<timestamp>_<id>/
  report.json
  camera_0_<camera-id>.jpg
  camera_1_<camera-id>.jpg
  …
```

## Identify displays and export cards

The test first covers the tools page with a large **DISPLAY 0** card, with its
confirmation and Stop controls kept on screen. Confirm **I can see DISPLAY 0** to
continue. **Card not visible**, Stop, or the 30-second timeout ends the test without
claiming the main screen was visible. No secondary window opens before confirmation.

Choose one public, powered-on, presentation-capable secondary display at a time.
Its normal non-touchable Presentation window uses the central 75% of the display's
width and height, with a large black-backed numbered marker. There is no overlay
above factory HUD layers. The main screen shows observation controls rather than
a duplicate marker. Each secondary test lasts up to 30 seconds. Select its physical
location (HUD, cluster, rear/passenger, main or another screen), or **Not seen**, and
press **Save observation**. The marker closes and the display selector returns;
you can retest an ID or choose another. **Finish and save** ends the session.
The selector closes after two minutes without a selection.

Timeouts and cancellation are recorded as **unconfirmed**, rather than as an
explicit Not seen result. The report distinguishes Android show/layout completion
from owner-entered physical visibility, and retains each attempt, its elapsed time,
errors and observation. Private/off/unsupported displays are recorded as skipped.
Stop, leaving the page, recording starting or losing the vehicle-interface/factory-camera
conditions dismisses the test. There is no gear or speed gate.

A number is an Android display ID, not an inferred physical role. Record what you
actually see: `Presentation.show()` returning and a view completing layout do not
prove the marker appeared on the windscreen. No model inference, permanent HUD
selection or vehicle control setting is changed.

```text
ZeekrShortcutApp/Developer/DisplayTest_<timestamp>_<id>/
  report.json
  display_0_test_card.png
  display_2_test_card.png
  display_3_test_card.png
  …
```

These PNGs are **cards generated by this app**, not screenshots of whatever factory
content was on each display. This app has no privileged factory-display capture
API. The report records offered IDs, names, dimensions, flags, state, show errors,
current HUD configuration, main-screen confirmation, per-attempt observations and why the test ended.

Both tools write to selected storage, using the existing developer-only internal
fallback rule if enabled. Their files are excluded from playback and automatic
media cleanup; Developer archive includes the folder. The tools run only on demand.
Send the entire matching export folders alongside physical-display observations.
No exported footage or private diagnostics are committed to the repository.

## Research limits and validation

This can identify publicly exposed feeds and physical display routing. It does not
promise that the factory stitched 360 feed is available through Camera2; private
vendor surfaces/buffers remain unresolved. It does not calibrate DIY 360 geometry.

The implementation follows Android's [CameraDevice lifecycle](https://developer.android.com/reference/android/hardware/camera2/CameraDevice)
and [Presentation API](https://developer.android.com/reference/android/app/Presentation).
JVM tests cover the lease, delayed callbacks, scheduler close/no-reopen behavior,
size bounds and YUV packing. Vehicle stream compatibility, placement and visibility
need the owner's on-car test; no vehicle was attached during development.
