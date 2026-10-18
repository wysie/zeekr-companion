package com.kooo.evcam.zeekr;

import java.util.Map;
import java.util.regex.Pattern;

/** Stored intent is separate from its current model-dependent rendering result. */
public final class BirdEyeProjectionMode {
    public static final String KEY = "projection_profile";
    // Preserve the existing explicit Manual/009 values; -1 explicitly restores model defaults.
    public static final String AUTO = "-1", MANUAL = "0", FIT_009 = "1";
    private static final Pattern LENS_KEY = Pattern.compile(
            "camera_[0-3]_(height|pitch|yaw|roll|fov|cx|cy|offset_x|offset_y|mirror)");
    private BirdEyeProjectionMode() { }

    public static boolean manualGeometryKey(String key) {
        return "length".equals(key) || "width".equals(key) || "margin".equals(key)
                || (key != null && LENS_KEY.matcher(key).matches());
    }

    public static String selection(Map<String, ?> saved) {
        if (saved.containsKey(KEY)) {
            Object value = saved.get(KEY);
            if (Integer.valueOf(-1).equals(value)) return AUTO;
            if (Integer.valueOf(1).equals(value)) return FIT_009;
            return MANUAL; // Explicit 0, unknown/corrupt/future choices never turn on a fitted view.
        }
        // Legacy slider edits did not write a projection choice. Presence, not value equality,
        // establishes tuning: even a saved default-valued adjustment must be retained.
        for (String key : saved.keySet()) if (manualGeometryKey(key)) return MANUAL;
        return AUTO; // Quality, lane assignment and fitted-margin changes are not lens geometry.
    }

    public static boolean imageFitted(String selection, VehicleIdentity.Model model) {
        if (model == VehicleIdentity.Model.ZEEKR_7X) return false;
        return FIT_009.equals(selection) || (AUTO.equals(selection) && model == VehicleIdentity.Model.ZEEKR_009);
    }
}
