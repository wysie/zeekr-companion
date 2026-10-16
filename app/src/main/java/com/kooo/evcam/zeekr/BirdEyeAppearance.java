package com.kooo.evcam.zeekr;

import android.content.Context;
import android.content.SharedPreferences;

/** Cosmetic settings are independent of calibration and its reset action. */
public final class BirdEyeAppearance {
    public static final String WHITE = "white", GREEN = "green", BLACK = "black";
    public static final String AUTO = "auto", MODEL_009 = "009", MODEL_7X = "7x", OUTLINE = "outline";
    public final SharedPreferences preferences;

    public BirdEyeAppearance(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("bird_eye_appearance", Context.MODE_PRIVATE);
    }

    public String colour() { return normalize(preferences.getString("vehicle_colour", WHITE)); }
    public void setColour(String colour) { preferences.edit().putString("vehicle_colour", normalize(colour)).apply(); }
    static String normalize(String colour) { return GREEN.equals(colour) || BLACK.equals(colour) ? colour : WHITE; }
    public String model() { return normalizeModel(preferences.getString("vehicle_model", AUTO)); }
    public void setModel(String model) { preferences.edit().putString("vehicle_model", normalizeModel(model)).apply(); }
    static String normalizeModel(String model) {
        return MODEL_009.equals(model) || MODEL_7X.equals(model) || OUTLINE.equals(model) ? model : AUTO;
    }
    static String resolveModel(String selection, boolean imageFitted009, boolean mapping009, VehicleIdentity.Model reported) {
        String model = normalizeModel(selection);
        if (!AUTO.equals(model)) return model;
        if (imageFitted009 || mapping009 || reported == VehicleIdentity.Model.ZEEKR_009) return MODEL_009;
        return reported == VehicleIdentity.Model.ZEEKR_7X ? MODEL_7X : OUTLINE;
    }
}
