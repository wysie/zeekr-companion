package com.kooo.evcam.profile;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Structural recording identity only. Preview modes, layouts and overlay enablement must
 * never participate in deciding whether an existing pipeline can start recording. */
public final class RecordingSetup {
    private final String carModel;
    private final boolean mapping009;
    private final Map<String, String> sources;

    private RecordingSetup(String carModel, boolean mapping009, Map<String, String> sources) {
        this.carModel = carModel; this.mapping009 = mapping009;
        this.sources = Collections.unmodifiableMap(new TreeMap<>(sources));
    }

    public static RecordingSetup of(String carModel, boolean mapping009, Profile profile,
                                    Map<String, String> manualSourceIds) {
        Map<String, String> selected = new TreeMap<>();
        for (CameraProfile camera : profile.cameras) {
            String key = RecordingSources.key(camera.role);
            if (camera.enabled && key != null) selected.put(key, manualSourceIds.get(camera.role));
        }
        return new RecordingSetup(carModel, mapping009, selected);
    }

    @Override public boolean equals(Object other) {
        if (!(other instanceof RecordingSetup)) return false;
        RecordingSetup setup = (RecordingSetup) other;
        return mapping009 == setup.mapping009 && Objects.equals(carModel, setup.carModel)
                && sources.equals(setup.sources);
    }
    @Override public int hashCode() { return Objects.hash(carModel, mapping009, sources); }
    @Override public String toString() { return carModel + "/mapping009=" + mapping009 + "/recording=" + sources; }
}
