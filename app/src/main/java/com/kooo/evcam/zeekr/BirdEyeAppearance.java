package com.kooo.evcam.zeekr;

import android.content.Context;
import android.content.SharedPreferences;

/** Cosmetic settings are independent of calibration and its reset action. */
public final class BirdEyeAppearance {
    public static final String WHITE = "white", GREEN = "green", BLACK = "black";
    public final SharedPreferences preferences;

    public BirdEyeAppearance(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("bird_eye_appearance", Context.MODE_PRIVATE);
    }

    public String colour() { return normalize(preferences.getString("vehicle_colour", WHITE)); }
    public void setColour(String colour) { preferences.edit().putString("vehicle_colour", normalize(colour)).apply(); }
    static String normalize(String colour) { return GREEN.equals(colour) || BLACK.equals(colour) ? colour : WHITE; }
}
