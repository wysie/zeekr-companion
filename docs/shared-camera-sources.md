# Shared camera sources

Recording & photos → **Camera sources**, the Developer shortcut and the stream
editor now use the same ordinary source mapping. Developer mode does not gate
these choices. The editor offers both a full-page shortcut and the same per-source
picker from its camera detail section.

Each source has **Automatic**, **Unassigned** or a specific camera ID. The current
selection is checked; OK commits and Cancel leaves it unchanged. The page shows
the resolved ID alongside Automatic/Manual, or reports that no automatic source
has been identified. The inventory includes advertised IDs, successfully captured
Developer discovery IDs, configured choices and the owner-exported 009 IDs when
that model is selected. Listing an ID does not prove current availability or role.
Active duplicate assignments are rejected.

## Automatic and reset

Surround priority is:

1. An explicit manual choice (or explicit Unassigned).
2. A detected surround-composite feed, independent of model name.
3. The selected model fallback: the supplied 009 uses ID 3. Generic/7X retains
   the existing discovery/cached slot-plan behavior when a fresh scan is unavailable.

Actual detector observations are separate from the active selected stream. A manual
assignment can never relabel itself as a detected feed. The last successful detector
ID is retained for display/resolution while the service is temporarily unavailable;
new pipeline discovery can replace it. An explicit ordinary source that is temporarily
absent is retained for the existing camera availability scheduler, without borrowing
the fallback camera's composite geometry or dimensions.

Selecting **Automatic** removes that role's explicit value and retired Developer
key. It does not change recording switches. For surround this returns to detection,
then the model fallback. Front/rear/cabin use their existing model/default discovery
rules; extra Manual mapping has no automatic source.

**Reset all to automatic** clears all source overrides, retired Developer overrides
and cached slot plans that could contain old manual assignments. It retains the
vehicle choice, preview layout, recording switches and all profile/picture quality
settings. It does not clear detector observations. Confirm before applying; Cancel
keeps everything. Additional sources without an automatic default must be mapped
before use. Exit and reopen after source changes to rebuild the pipeline.

**Unassigned** is deliberately different from Automatic. It prevents a source from
being opened and retains the old editor behavior of disabling that source's recorder;
for a road source it disables the Front + Rear recording pair. It does not normalise
or rewrite unrelated profile fields. Merely choosing a different mapped camera does
not enable its recorder. An explicit recording-group enable request can enable that
group once all its sources have mappings.

## Upgrade preservation

Canonical keys remain `source_id_<role>`. The old `zeekr_camera_override_<slot>`
settings no longer form a second active mapping. On the first upgrade:

- Existing canonical choices win, including explicit Unassigned.
- If Developer mode was enabled, old effective slot overrides fill only canonical
  roles that had no choice. Their values then work with Developer mode off.
- Previously inactive Developer overrides are not silently activated. Raw legacy
  values remain archived until an explicit reset clears them; subsequent enabling
  of Developer mode cannot restore them.
- Migration is marked once, and source writes/resets prevent legacy resurrection.

The old internal override getters/setters now alias the canonical role values.
Recording/profile identifiers and historical footage filenames remain unchanged.

## Diagnostics and testing

Diagnostics JSON metadata includes `camera_sources` with each selection mode,
manual ID and resolved ID, plus `detected_surround_id`. Successful camera export
IDs remain separate discovery evidence, not automatic mappings or role guesses.

Host tests cover migration precedence/inactive legacy keys, detected/model fallback
priority and absent/manual/Unassigned composite geometry. Native tests exercise
real preference migration, Developer shutdown, source and whole-map resets, Cancel,
the shared page entries, editor source selection and preservation of preview,
recorders and profile quality. Existing preview/pipeline, vehicle, HUD and camera
export tests run alongside them. Vehicle concurrency and newly discovered feed
roles remain to be checked on the car.
