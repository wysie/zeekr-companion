package com.kooo.evcam.zeekr;

/** Model selection policy. Camera IDs and display shapes never identify the vehicle. */
public final class VehicleProfile {
    public static final String KEY = "vehicle_profile_mode";
    private VehicleProfile() {}

    /** Preserve the old functional 009 opt-in, but never promote the old cosmetic name. */
    public static String selection(String saved, boolean legacy009) {
        if (saved == null) return legacy009 ? "009" : "auto";
        return "009".equals(saved) || "7x".equals(saved) ? saved : "auto";
    }

    public static VehicleIdentity.Model resolve(String saved, boolean legacy009, String... names) {
        return VehicleIdentity.resolve(selection(saved, legacy009), names);
    }
}
