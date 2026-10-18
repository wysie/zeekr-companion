# Recording sources and extended discovery

Settings → Recording & photos → Edit stream profile now has four recording groups:

| Group | Sources |
| --- | --- |
| Surround | The existing four-camera composite, with its existing recording layout |
| Front + Rear | Separate `roadfront` and `roadrear` sources, each with quality controls |
| Cabin | Existing cabin sources; only the confirmed driver/cabin feed on the 009 preset |
| Manual mapping | One user-assigned extra source; an enabled legacy 009 extra is retained here |

Groups can be combined. New road/manual roles start off; stored recording choices,
profile settings, application identity, storage roots and historical filenames remain.
A legacy 009 extra is retained rather than silently reassigned as a road camera.
The existing two-cabin configuration is retained for other models; it does not
establish that every car physically has two cabin feeds.

Recording & photos → **Preview layout** is independent: Selected recording
sources (unchanged default), Surround, Front + Rear, Front road, Rear road,
Driver / front cabin, or Surround + Front + Rear. The last option displays the
composite alongside the two dedicated road sources; it does not synthesize new
camera views. The historical `cabin` choice still shows the first cabin/driver
source; use Selected recording sources to retain an existing two-cabin layout.
The stream editor shows the current layout in its shortcut button. Both selectors
use the same saved value and choices. The editor highlights the current selection
and waits for OK; Cancel/back changes nothing. The Settings row refreshes on
return from the editor. Preview-only changes never save the recording profile,
so existing custom preview/photo/recording tuning is preserved. Existing stored
layout IDs remain valid; an unset preference still follows recording sources.
Model changes do not reset this choice. Exit and reopen after changing.
Opening a preview does not enable its recorder. Normal side-camera/Super mirror/360-popup consumers retain access
to the surround source without forcing its recorder or main preview on. The
existing scheduler, fresh-frame guards and factory/reverse priority still apply.
Existing non-surround screen-off release rules also remain in place.

Tap a group to edit it. Front + Rear and multi-cabin groups have a camera selector
inside their detail section. **Change camera source** uses the shared Camera sources picker: Automatic,
Unassigned or a camera ID without requiring Developer mode. Recording & photos
and Developer shortcuts open the same full page, including Reset all to automatic.
See [shared mapping, migration and resets](shared-camera-sources.md). The list is
refreshed when opened; advertised IDs, configured sources, owner-confirmed 009
IDs while that preset is selected, and previously captured discovery IDs are
included. A listed/configured choice does not prove current availability. Duplicate
active assignments are rejected. Cancel changes nothing. Assigning no source
turns its recording off. Source/group/preview changes require exiting and reopening
the app; ongoing recording uses its existing pipeline/settings snapshot.

With System → Vehicle model set to 009 (or a positively detected 009), defaults
are detected surround (fallback 3), front road 0, rear road 1 and cabin 2. An old enabled 009 mapping
is retained. The old cosmetic name never selects these exterior IDs, and they
are not assumed on a 7X. See [selection and evidence](vehicle-model-profiles.md). Manual mapping has no
default camera. Actual sizes remain limited to what each camera declares. New source quality starts
at Auto resolution / 20 fps / medium bitrate. Select a higher declared resolution
in each road camera’s detail section when testing quality; a 2560×1440 JPEG export
does not prove simultaneous recording at that resolution.

The backend supports named source keys alongside historical cabin keys. Road
files use `_roadfront` / `_roadrear`; manual files use `_manual`. Normal and
historical surround/cabin names stay readable. The gallery and timeline retain
surround priority, then discover uncovered road/cabin/manual recordings. A longer
companion extending beyond an existing primary can appear as its own recording
so its remaining footage is accessible. Same-time recordings in separate folders
stay separate. Primary files retain their own ownership; companion deletion/share
membership is assigned once to a nearby same-folder recording. File lists are
deduplicated. Ordinary sources never receive surround grid/fisheye correction.

## Camera/display tools

The camera exporter checks advertised Camera2 IDs first, then reported physical
and availability IDs, plus numeric candidates 0–15, deduplicated and capped at 48.
It checks characteristics before attempting capture. Known unavailable cameras
are skipped; unadvertised candidates with readable characteristics may be tried
when no unavailable callback has been observed. It uses ordinary Camera2 calls,
without bypassing permissions or private-buffer access. Physical child IDs may
reject direct opening; the report retains that result rather than claiming access.

The normal app pipeline is first leased exclusively and confirmed closed. Each
candidate is opened/captured/closed sequentially. New candidates stop after
three minutes; a current open/image timeout and final closure wait can extend
that time. Failed native closure retains the existing lease and stops subsequent
probes. Cancellation and factory takeover retain their existing behaviour.

`report.json` keeps the advertised `camera_ids`, adds `candidate_ids`, candidate
origin/advertised flags and each result. Successful snapshots retain supported
sizes and closure status. Successful IDs are saved in a separate discovery
inventory for the source picker; mappings/HUD settings are not changed. One
capture configuration per candidate is tested, not every resolution, format,
vendor control or private factory feed. Display identification remains limited
to public displays; no guessed display-ID routing is added.

## Validation and remaining vehicle tests

- 1,450 JVM tests pass, including all 64 source combinations, profile persistence/defaults,
  preview/recording separation and retained overlay access; candidate ordering,
  deduplication/bounds; road-only timelines, surround priority and directory isolation.
- Native API 32 tests exercise combined group toggles, immediate Manual source
  picking, save/cancel, independent preview choice and editor reopening.
- Generated one-second H.264 fixtures verify Front + Rear-only and Cabin-only
  gallery discovery and ordinary road playback. They do not use owner footage.
- The real emulator Camera2 exporter saves accessible feeds, reports absent
  numeric IDs and confirms closure. This does not establish hidden 009 feeds.
- Native 2560×1600 large-text captures verify readable group cards/source choices;
  existing layout/navigation and camera/HUD regression checks also pass (28 distinct
  native tests, including the named pipeline/diagnostic/recording-key check).

On the 009, test Front + Rear alone, then Front + Rear + Surround, verify that each
selected source produces playable clips and that stop/restart works. Check CPU,
frame rate, factory takeover/recovery and side-camera/HUD operation. These tests
are still needed: single-camera JPEG exports do not prove concurrent recording
at the chosen quality. 7X exterior mapping and concurrency need separate evidence.
No downloadable release or version bump is created by this change.

## Preview selector validation

Host coverage exercises the combined preview against all 64 recording-source
combinations without changing any profile fields. Native Android regressions
check existing layout IDs, invalid writes, unset defaults, vehicle-model switches,
current-choice highlighting, Cancel/OK, stored profile tuning, Activity recreation,
the Settings entry and the real combined-preview pipeline/pane routing. Native
layout/navigation and vehicle-profile tests run alongside them. Emulator routing
does not establish real vehicle concurrency or frame rate.
