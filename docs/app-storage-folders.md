# App files on selected storage

New public files use one folder on the selected USB volume (or internal storage
when that existing developer-only option is permitted):

```text
ZeekrShortcutApp/
├── Videos/
│   ├── Clips/            exported ranges; excluded from automatic cleanup
│   └── repair-originals/ verified backups; excluded from automatic cleanup
├── Photos/
│   ├── 360/              experimental stitched PNG + calibration TXT
│   └── Raw/              developer raw JPEG + TXT
└── Logs/                 exported diagnostics JSON
```

No automatic migration or bulk copy runs during upgrade. Old recordings/photos in
`DCIM/EVCam_Video` and `DCIM/EVCam_Photo` on the selected volume remain available in
playback. Locks remain beside their own files, including legacy `locked.txt` and
`locked-auto.txt`; matching filenames in different folders have independent locks.
Pending relay clips inherit any legacy protection entry before publication into
the new folder; those recovered entries are kept as manual protection. The existing video/photo limits count recognized footage across both layouts, and
automatic cleanup applies the existing rules across both. No limit still means no
automatic deletion. Locks are respected while the protection option is enabled.
Nested raw/360 captures, exported clips and repair backups retain their existing
exclusion from ordinary playback and automatic cleanup.

Older diagnostics remain in `Download/EVCam_Log`; old exported clips remain in
`DCIM/EVCam_Clips`. These can still be accessed directly on the drive. Send to phone
uses the actual selected file path, so legacy playback files remain shareable.
Developer repair checks both recording layouts. Developer archive scans both layouts
and preserves their relative paths and lock lists on the target volume, avoiding
merging equal filenames or losing their protection metadata.

Live session logs and relay buffers stay in app-private storage for reliable
operation when USB is absent. Diagnostics exports include the logs as before;
archiving copies private session logs into `ZeekrShortcutApp/Logs/app-logs`.
Factory Sentry recordings remain in the factory folder and are not scanned or moved.

The native storage/USB permissions and on-car playback/sharing still need vehicle
validation; JVM checks cover path discovery and cleanup/protection decisions.
