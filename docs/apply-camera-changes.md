# Applying camera configuration inside Zeekr Companion

Available in **2.11.54-alpha.1**. Camera configuration is saved independently of the running pipeline. A shared
**Camera changes pending · Apply changes** banner appears above the main preview
and below the Settings title. No full app quit/relaunch is needed for these edits.

## What needs Apply

- Vehicle/profile mapping, enabled recording roles and shared manual camera IDs.
- Main preview source sets; equivalent source sets do not require Apply.
- Native preview resolution and JPEG resolution/output enablement.
- Developer GPU preview straightening and the CPU/GPU 360° backend.

The immutable session identity also includes recording-role selection when every
source is already open for preview. Bitrate, recording FPS/codec/size and JPEG
quality are excluded; encoding changes take effect at the next recording. Live
picture geometry, calibration, theme and source arrangement retain their existing
behavior. The pipeline-choice developer preference already reports that recording
always uses MediaCodec; Apply does not introduce another recording backend.

## Transaction and recovery

1. Capture the saved setup at confirmation. Later edits remain pending.
2. If recording or waiting to record, ask to finish the clip, restart cameras and
   resume. Cancel changes neither cameras nor the recording.
3. Hold recording starts and invalidate outstanding storage checks. Stop and drain
   the recorder through its existing lifecycle callback and teardown barrier.
4. Release the old manager. Wait for the scheduler's native-camera closure
   notification before removing old GPU producers. A cleanup/setup timeout fails
   the action rather than treating the timeout as successful camera closure.
5. Build from the captured profile, source mapping, native sizes and surface
   options. Photo size/output and GPU routing stay fixed for that pipeline even if
   preferences are edited before it opens another session.
6. On successful enumeration/setup, recreate the Activity's source views. Android
   restores the Settings fragment/category/detail stack and saved expanded/360°
   view state; the process, preferences and media galleries remain in place.
7. Resume only an existing recording/request, retaining manual/automatic origin.
   Recheck its automatic-start permission. Applying is not an encoder failure and
   does not consume the automatic-recovery budget. Stop during Apply wins. Idle
   Apply stays idle. Failed Apply stops recording and leaves a Retry action.

Background consumers and UI recreation cannot silently consume saved camera edits.
The applied selector and source panes remain separate from the saved next choice.
An actual process restart naturally starts with the latest saved configuration.
A later structural recording edit can still require another Apply before recording
can start with that selection. Camera absence/factory ownership remains a waiting
condition under the existing scheduler, not proof that Apply failed or that the
mapped feed is correct.

The stop lifecycle keeps its existing 20-second bound; the additional teardown
barrier has a 20-second limit. Native shutdown, GPU teardown and setup have a shared
60-second limit. Late callbacks are ignored after failure/retry. Slow or broken
vehicle HALs remain subject to the existing scheduler's recovery rules; this change
does not bypass factory priority or force-close another app's cameras.

## Validation

- JVM session-identity regressions distinguish live/next-recording settings from
  native camera edits, cover equivalent preview choices, reverted mappings and
  recording-role changes with identical open-source sets.
- Disposable API 32 Android emulator: idle Apply, Settings restoration, staged edits
  across theme/UI recreation, edits during Apply, shared banner clearing, light/dark
  and enlarged-text geometry.
- Real emulator Camera2/MediaCodec recording: cancel confirmation keeps the old
  recording running; successful Apply finalizes playable MP4s and resumes on the
  new manager; Stop during Apply cancels resumption. Automatic origin, disabled
  automatic-start settings and failed-Apply recording state are checked separately.
- A retained popup texture accepts a direct producer after its old GPU producer is
  released, exercising the GPU-to-CPU Apply teardown boundary.
- Existing main-page, Settings, pending-preview recording and GPU rendering native
  regressions are included in the final verification run.

These checks establish application behavior on the emulator. They do not establish
009/7X HAL restart timing, actual HUD fit, availability of additional vehicle cameras
or GPU 360° frame rate. No physical-vehicle test is claimed.

Final validation: **1,514 JVM tests**, **34 distinct native cases**, Android lint
(no errors) and signed release APK build pass. The two older native cases that
expected UI recreation to apply saved sources were updated to explicit Apply and
confirmed separately; the new retained-GPU-texture case also passes.

Build/test evidence is kept outside the repository: `/tmp/companion-camera-apply-validation.log`
and `/tmp/companion-camera-apply-native-regressions.log`, with the corrected-test/GPU
confirmation in `/tmp/companion-camera-apply-confirmation.log`. UI screenshots use real
native views, with the emulator's waiting state where no composite camera exists;
no factory camera scene was fabricated.
