package com.kooo.evcam.zeekr;

import android.content.Context;
import android.content.SharedPreferences;

/** Dedicated prototype calibration; does not alter side views, recording or preview profiles. */
public final class BirdEyeConfig {
    public final SharedPreferences preferences;
    public BirdEyeConfig(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences("bird_eye_calibration", Context.MODE_PRIVATE);
    }
    public int get(String key, int fallback) { return preferences.getInt(key, fallback); }
    public void set(String key, int value) { preferences.edit().putInt(key, value).apply(); }
    public static String key(int camera, String field) { return "camera_" + camera + "_" + field; }
    public void assignLane(int camera, int lane) {
        int[] previous = new int[4];
        for (int i = 0; i < 4; i++) previous[i] = get(key(i, "lane"), i);
        int[] result = assignDistinctLanes(previous, camera, lane);
        SharedPreferences.Editor edit = preferences.edit();
        for (int i = 0; i < 4; i++) edit.putInt(key(i, "lane"), result[i]);
        edit.apply();
    }
    static int[] assignDistinctLanes(int[] previous, int camera, int lane) {
        if (previous.length != 4 || camera < 0 || camera > 3 || lane < 0 || lane > 3)
            throw new IllegalArgumentException("Invalid camera selection");
        int[] result = previous.clone();
        for (int i = 0; i < 4; i++) if (i != camera && result[i] == lane) result[i] = previous[camera];
        result[camera] = lane;
        boolean[] seen = new boolean[4]; seen[lane] = true;
        for (int i = 0; i < 4; i++) {
            if (i == camera) continue;
            int value = result[i];
            if (value < 0 || value > 3 || seen[value]) {
                value = 0; while (seen[value]) value++;
                result[i] = value;
            }
            seen[value] = true;
        }
        return result;
    }
    private int bounded(String key, int fallback, int min, int max) {
        return Math.max(min, Math.min(max, get(key, fallback)));
    }
    public BirdEyeProjection.Calibration read() {
        double length = bounded("length", 520, 300, 650) / 100.0;
        double width = bounded("width", 200, 140, 260) / 100.0;
        double margin = bounded("margin", 200, 50, 400) / 100.0;
        BirdEyeProjection.Camera[] cameras = new BirdEyeProjection.Camera[4];
        int[] headings = {0, 180, 270, 90};
        for (int i = 0; i < 4; i++) {
            double x = i == 2 ? -width / 2 : i == 3 ? width / 2 : 0;
            double y = i == 0 ? length / 2 : i == 1 ? -length / 2 : 0;
            x += bounded(key(i, "offset_x"), 0, -200, 200) / 100.0;
            y += bounded(key(i, "offset_y"), 0, -250, 250) / 100.0;
            cameras[i] = new BirdEyeProjection.Camera(bounded(key(i, "lane"), i, 0, 3), x, y,
                    bounded(key(i, "height"), 100, 30, 200) / 100.0,
                    bounded(key(i, "yaw"), headings[i], 0, 359),
                    bounded(key(i, "pitch"), 35, 0, 90),
                    bounded(key(i, "roll"), 0, -180, 180),
                    bounded(key(i, "fov"), 190, 120, 220),
                    bounded(key(i, "cx"), 50, 25, 75) / 100.0,
                    bounded(key(i, "cy"), 50, 25, 75) / 100.0,
                    preferences.getBoolean(key(i, "mirror"), false));
        }
        return new BirdEyeProjection.Calibration(length, width, margin, cameras);
    }
    public void reset() { preferences.edit().clear().apply(); }
}
