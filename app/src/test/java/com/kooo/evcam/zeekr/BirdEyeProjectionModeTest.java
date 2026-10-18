package com.kooo.evcam.zeekr;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static com.kooo.evcam.zeekr.BirdEyeProjectionMode.*;
import static com.kooo.evcam.zeekr.VehicleIdentity.Model.*;
import static org.junit.Assert.*;

public class BirdEyeProjectionModeTest {
    @Test public void untouchedProjectionFollowsOnlyConfirmedModel() {
        String mode = selection(new HashMap<>());
        assertEquals(AUTO, mode); assertTrue(imageFitted(mode, ZEEKR_009));
        assertFalse(imageFitted(mode, ZEEKR_7X)); assertFalse(imageFitted(mode, UNKNOWN));
    }
    @Test public void legacyExplicitManualNeverBecomesAutomaticEvenAtDefaultValue() {
        Map<String, Object> saved = new HashMap<>(); saved.put(KEY, 0);
        assertEquals(MANUAL, selection(saved)); assertFalse(imageFitted(selection(saved), ZEEKR_009));
    }
    @Test public void legacy009ChoiceIsRetainedButSuppressedOn7X() {
        Map<String, Object> saved = new HashMap<>(); saved.put(KEY, 1); saved.put("length", 550);
        assertEquals(FIT_009, selection(saved)); assertTrue(imageFitted(selection(saved), ZEEKR_009));
        assertTrue(imageFitted(selection(saved), UNKNOWN)); assertFalse(imageFitted(selection(saved), ZEEKR_7X));
        assertEquals(1, saved.get(KEY));
    }
    @Test public void everyLegacyGeometryFieldPreservesManualEvenWhenValueEqualsDefault() {
        for (String key : new String[]{"length", "width", "margin", "camera_0_height", "camera_1_pitch",
                "camera_2_yaw", "camera_3_roll", "camera_0_fov", "camera_1_cx", "camera_2_cy",
                "camera_3_offset_x", "camera_0_offset_y", "camera_1_mirror"}) {
            Map<String, Object> saved = new HashMap<>(); saved.put(key, 0);
            assertEquals(key, MANUAL, selection(saved));
        }
    }
    @Test public void qualitySourcesAndFittedMarginDoNotPinManualGeometry() {
        Map<String, Object> saved = new HashMap<>(); saved.put("quality", 2);
        saved.put("image_fit_margin", 275); saved.put("camera_0_lane", 3); saved.put("camera_3_lane", 0);
        assertEquals(AUTO, selection(saved));
    }
    @Test public void choosingAutomaticOverridesInferenceWithoutDeletingGeometry() {
        Map<String, Object> saved = new HashMap<>(); saved.put("length", 550); saved.put("camera_0_pitch", 47);
        assertEquals(MANUAL, selection(saved)); saved.put(KEY, -1);
        assertEquals(AUTO, selection(saved)); assertTrue(imageFitted(selection(saved), ZEEKR_009));
        assertEquals(550, saved.get("length")); assertEquals(47, saved.get("camera_0_pitch"));
    }
    @Test public void unknownStoredChoicesStayConservativelyManual() {
        for (Object value : new Object[]{99, "1", true, null}) {
            Map<String, Object> saved = new HashMap<>(); saved.put(KEY, value);
            assertEquals(MANUAL, selection(saved)); assertFalse(imageFitted(selection(saved), ZEEKR_009));
        }
    }
}
