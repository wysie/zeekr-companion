# App files on selected storage

New public files use one folder on the selected USB volume (or internal storage
when that existing developer-only option is permitted):

```text
ZeekrCompanion/
├── Videos/
│   ├── Clips/            exported ranges; excluded from automatic cleanup
│   └── repair-originals/ verified backups; excluded from automatic cleanup
├── Photos/
│   ├── 360/              experimental stitched PNG + calibration TXT
│   └── Raw/              developer raw JPEG + TXT
├── Logs/                 exported diagnostics JSON
└── Developer/            on-demand camera scans and display test cards + JSON
```

This folder-name change is newer than 2.11.52-alpha.1. No automatic migration,
rename or bulk copy runs during upgrade. Existing `ZeekrShortcutApp/Videos` and
`ZeekrShortcutApp/Photos`, plus `DCIM/EVCam_Video` and `DCIM/EVCam_Photo`, on the
selected volume remain available in playback. Locks remain beside their own files,
including legacy `locked.txt` and
`locked-auto.txt`; matching filenames in different folders have independent locks.
Pending relay clips inherit any legacy protection entry before publication into
the new folder; those recovered entries are kept as manual protection. The existing video/photo limits count recognized footage across all three layouts, and
automatic cleanup applies the existing rules across them. No limit still means no
automatic deletion. Locks are respected while the protection option is enabled.
Nested raw/360 captures, exported clips and repair backups retain their existing
exclusion from ordinary playback and automatic cleanup. Developer exports are also excluded.

Older diagnostics remain in `ZeekrShortcutApp/Logs` and `Download/EVCam_Log`; old exported clips remain in
`DCIM/EVCam_Clips`. These can still be accessed directly on the drive. Send to phone
uses the actual selected file path, so legacy playback files remain shareable.
Developer repair checks all three recording layouts. Developer archive scans all three layouts
and both app names' Developer folders, preserving their relative paths and lock lists on the target volume, avoiding
merging equal filenames or losing their protection metadata.

Live session logs and relay buffers stay in app-private storage for reliable
operation when USB is absent. Diagnostics exports include the logs as before;
archiving copies private session logs into `ZeekrCompanion/Logs/app-logs`.
Factory Sentry recordings remain in the factory folder and are not scanned or moved.

FileProvider retains the old `app_videos`, `app_photos` and `app_logs` URI aliases
and adds separate Companion aliases, so old shared paths keep their meaning.
The Android application ID, signing certificate, preferences and app-private
storage are unchanged. This is a public media-folder rename, not an app identity migration.

JVM checks cover all three layouts, same-name files/lock lists, cleanup-volume
isolation, relay protection and archive destinations. Native emulator checks cover
sharing exact bytes from all three layouts and finalized USER/DRIVE MP4 output
under the new folder. Storage/USB permissions and playback/sharing still need
vehicle validation.

Validation on 2026-10-19: all 1,502 JVM tests, Android lint, debug/release builds,
and the two targeted native sharing/recording methods pass. The native recording
method exercises both USER and DRIVE starts and reads a finalized sample from
each MP4; the sharing method reads distinct bytes from nine same-name files
across three file kinds and three layouts.
