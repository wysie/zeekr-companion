package com.kooo.evcam.profile;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecordingSetupTest {
    private Profile profile() {
        Profile p = ProfileMigration.migrate(new ProfileMigration.Snapshot()); RecordingSources.ensure(p);
        for (CameraProfile c : p.cameras) c.enabled = CameraProfile.ROLE_COMPOSITE.equals(c.role)
                || CameraProfile.ROLE_ROAD_FRONT.equals(c.role) || CameraProfile.ROLE_ROAD_REAR.equals(c.role);
        return p;
    }
    private RecordingSetup setup(Profile p, Map<String,String> choices) { return RecordingSetup.of("zeekr_7x", true, p, choices); }
    @Test public void pendingPreviewDoesNotChangeTheRecordingSetup() {
        Profile p = profile(); Map<String,String> choices = new HashMap<>();
        RecordingSetup built = setup(p, choices);
        String original = ProfileChannels.of(p, "surround_roads", true).key();
        for (String mode : new String[]{"surround", "roads", "roadfront", "roadrear", "cabin"}) {
            assertNotEquals(original, ProfileChannels.of(p, mode, false).key());
            assertEquals("Preview changes must not block the three recording sources", built, setup(p, choices));
        }
        assertEquals(original, ProfileChannels.of(p, "selected", false).key());
        assertEquals(built, setup(p, choices));
    }
    @Test public void recordingSelectionIsCapturedEvenWhenThePreviewStillKeepsThoseCameras() {
        Profile p = profile(); RecordingSetup built = setup(p, Collections.emptyMap());
        Set<String> before = ProfileChannels.of(p, "surround_roads").keys;
        p.camera(CameraProfile.ROLE_ROAD_REAR).enabled = false;
        assertEquals(before, ProfileChannels.of(p, "surround_roads").keys);
        assertNotEquals(built, setup(p, Collections.emptyMap()));
    }
    @Test public void activeMappingEditsAndAutomaticResetRequireReopen() {
        Profile p = profile(); Map<String,String> choices = new HashMap<>();
        choices.put(CameraProfile.ROLE_COMPOSITE, "3"); RecordingSetup built = setup(p, choices);
        choices.put(CameraProfile.ROLE_COMPOSITE, "4"); assertNotEquals(built, setup(p, choices));
        choices.remove(CameraProfile.ROLE_COMPOSITE); assertNotEquals(built, setup(p, choices));
        choices.put(CameraProfile.ROLE_COMPOSITE, "3"); assertEquals(built, setup(p, choices));
    }
    @Test public void inactiveMappingsAndPresentationOrderDoNotBlockRecording() {
        Profile p = profile(); Map<String,String> choices = new HashMap<>(); RecordingSetup built = setup(p, choices);
        choices.put(CameraProfile.ROLE_CABIN_1, "2"); choices.put(CameraProfile.ROLE_MANUAL, "99");
        Collections.reverse(p.cameras);
        assertEquals(built, setup(p, choices)); assertEquals(built.hashCode(), setup(p, choices).hashCode());
    }
    @Test public void modelAndPhysicalMappingRemainPartOfRecordingCompatibility() {
        Profile p = profile(); Map<String,String> choices = Collections.emptyMap(); RecordingSetup built = setup(p, choices);
        assertNotEquals(built, RecordingSetup.of("custom", true, p, choices));
        assertNotEquals(built, RecordingSetup.of("zeekr_7x", false, p, choices));
    }
}
