package com.kooo.evcam.zeekr;

import com.kooo.evcam.profile.CameraProfile;
import com.kooo.evcam.profile.RecordingSources;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.kooo.evcam.zeekr.VehicleIdentity.Model.*;

public class VehicleProfileTest {
    @Test public void legacyFunctionalOptInSurvivesButExplicitSelectionWins() {
        assertEquals("009", VehicleProfile.selection(null, true));
        assertEquals("auto", VehicleProfile.selection(null, false));
        assertEquals(ZEEKR_009, VehicleProfile.resolve(null, true, "Pixel 3a"));
        assertEquals(ZEEKR_7X, VehicleProfile.resolve("7x", true, "Zeekr 009"));
        assertEquals(UNKNOWN, VehicleProfile.resolve("auto", true, "Pixel 3a"));
    }

    @Test public void autoRequiresAnExplicitUnambiguousVehicleName() {
        assertEquals(ZEEKR_009, VehicleProfile.resolve("auto", false, "Zeekr 009"));
        assertEquals(ZEEKR_7X, VehicleProfile.resolve("auto", false, "极氪7X"));
        assertEquals(UNKNOWN, VehicleProfile.resolve("auto", false, "Pixel 3a", "sargo"));
        assertEquals(UNKNOWN, VehicleProfile.resolve("auto", false, "Zeekr 009", "Zeekr 7X"));
        assertEquals(UNKNOWN, VehicleProfile.resolve("bad", false, "1280x5140", "display 3"));
    }

    @Test public void only009SuppliesTheOwnerVerifiedRoadCameraDefaults() {
        for (String selection : new String[]{"009", "7x", "auto"}) {
            boolean known009 = VehicleProfile.resolve(selection, false, "sargo") == ZEEKR_009;
            assertEquals(known009 ? "0" : null,
                    RecordingSources.defaultId(CameraProfile.ROLE_ROAD_FRONT, known009));
            assertEquals(known009 ? "1" : null,
                    RecordingSources.defaultId(CameraProfile.ROLE_ROAD_REAR, known009));
            assertNull(RecordingSources.defaultId(CameraProfile.ROLE_CABIN_2, known009));
            assertNull(RecordingSources.defaultId(CameraProfile.ROLE_MANUAL, known009));
        }
    }
}
