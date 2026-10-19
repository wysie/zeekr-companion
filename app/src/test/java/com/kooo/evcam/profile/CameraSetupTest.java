package com.kooo.evcam.profile;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class CameraSetupTest {
    private Profile profile() {
        Profile p = ProfileMigration.migrate(new ProfileMigration.Snapshot()); RecordingSources.ensure(p);
        for (CameraProfile c : p.cameras) c.enabled = CameraProfile.ROLE_COMPOSITE.equals(c.role);
        return p;
    }
    private String key(Profile p, String preview, boolean gpu, boolean jpeg) {
        return CameraSetup.key("7x", false, p, preview, false, Collections.emptyMap(), gpu, false, jpeg);
    }
    @Test public void liveGeometryAndNextRecordingQualityDoNotNeedApply() {
        Profile p = profile(); String before = key(p,"selected",false,true);
        CameraProfile camera = p.camera(CameraProfile.ROLE_COMPOSITE);
        camera.record.fps = "15"; camera.record.bitrate = "4000000"; camera.record.resolution = "1280x720";
        camera.photo.jpegQuality = 80; camera.lanes.clear();
        assertEquals(before, key(p,"selected",false,true));
    }
    @Test public void nativeSizesAndSurfaceTopologyNeedApplyAndRevertingClearsIt() {
        Profile p = profile(); String before = key(p,"selected",false,true);
        String size = p.camera(CameraProfile.ROLE_COMPOSITE).preview.resolution;
        p.camera(CameraProfile.ROLE_COMPOSITE).preview.resolution = "640x480";
        assertNotEquals(before, key(p,"selected",false,true));
        p.camera(CameraProfile.ROLE_COMPOSITE).preview.resolution = size;
        assertEquals(before, key(p,"selected",false,true));
        assertNotEquals(before, key(p,"selected",true,true));
        assertNotEquals(before, key(p,"selected",false,false));
        p.camera(CameraProfile.ROLE_COMPOSITE).photo.resolution = "640x480";
        assertNotEquals(before, key(p,"selected",false,true));
    }
    @Test public void recordingGroupChangeNeedsApplyEvenWhenAllSourcesAlreadyPreviewed() {
        Profile p = profile(); String before = key(p,"surround_roads",false,true);
        p.camera(CameraProfile.ROLE_ROAD_FRONT).enabled = true;
        assertNotEquals(before, key(p,"surround_roads",false,true));
    }
    @Test public void equivalentPreviewChoicesDoNotNeedApplyButDifferentFeedSetsDo() {
        Profile p = profile();
        assertEquals(key(p,"selected",false,true), key(p,"surround",false,true));
        assertNotEquals(key(p,"selected",false,true), key(p,"roads",false,true));
    }
    @Test public void modelAndManualMappingChangesArePartOfTheSnapshot() {
        Profile p = profile(); Map<String,String> sources = new HashMap<>();
        String automatic = CameraSetup.key("7x",false,p,"selected",false,sources,false,false,true);
        sources.put(CameraProfile.ROLE_COMPOSITE, "3");
        String explicit = CameraSetup.key("7x",false,p,"selected",false,sources,false,false,true);
        assertNotEquals(automatic,explicit);
        sources.clear();
        assertEquals(automatic,CameraSetup.key("7x",false,p,"selected",false,sources,false,false,true));
        assertNotEquals(automatic,CameraSetup.key("7x",true,p,"selected",false,sources,false,false,true));
    }
}
