package com.kooo.evcam.profile;

import java.util.Map;
import java.util.TreeMap;

/** Immutable session topology. Encoding quality and on-screen geometry deliberately stay outside it. */
public final class CameraSetup {
    private CameraSetup() {}
    public static String key(String model, boolean mapping009, Profile profile, String preview,
                             boolean overlay, Map<String, String> sources,
                             boolean gpuPreview, boolean gpu360, boolean jpeg) {
        ProfileChannels channels = ProfileChannels.of(profile, preview, overlay);
        Map<String, String> sizes = new TreeMap<>();
        for (CameraProfile camera : profile.cameras)
            if (channels.keys.contains(RecordingSources.key(camera.role))) {
                sizes.put(camera.role + ".preview", camera.preview.resolution);
                if (jpeg) sizes.put(camera.role + ".photo", camera.photo.resolution);
            }
        return RecordingSetup.of(model, mapping009, profile, sources) + "/channels=" + channels.key()
                + "/sources=" + new TreeMap<>(sources) + "/sizes=" + sizes
                + "/gpu=" + gpuPreview + "," + gpu360 + "/jpeg=" + jpeg;
    }
}
