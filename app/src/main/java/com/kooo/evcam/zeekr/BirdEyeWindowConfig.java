package com.kooo.evcam.zeekr;

import android.content.Context;
import android.content.SharedPreferences;

/** Window placement is independent of calibration, side cameras and mirror preferences. */
public final class BirdEyeWindowConfig {
    private final SharedPreferences prefs;
    public BirdEyeWindowConfig(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("bird_eye_window", Context.MODE_PRIVATE);
    }
    public boolean popup() { return prefs.getBoolean("popup", false); }
    public void setPopup(boolean value) { prefs.edit().putBoolean("popup", value).apply(); }
    public int size() { return Math.max(20, Math.min(75, prefs.getInt("size", 45))); }
    public void setSize(int value) { prefs.edit().putInt("size", Math.max(20, Math.min(75, value))).apply(); }
    int x() { return prefs.getInt("x", -1); }
    int y() { return prefs.getInt("y", -1); }
    void position(int x, int y) { prefs.edit().putInt("x", x).putInt("y", y).apply(); }
    public void resetLayout() { prefs.edit().remove("x").remove("y").remove("size").apply(); }
}
