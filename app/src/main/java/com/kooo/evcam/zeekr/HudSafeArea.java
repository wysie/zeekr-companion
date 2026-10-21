package com.kooo.evcam.zeekr;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.View;
import com.kooo.evcam.AppConfig;
import org.json.JSONObject;

/** One explicitly saved, viewport-specific calibration, included in the normal app settings backup. */
final class HudSafeArea {
    static final String KEY = "hud_safe_area_calibration";
    static final int DEFAULT_MARGIN = 2;
    private static final String[] FRAME_KEYS = {"display_name", "display_flags", "display_width", "display_height",
            "viewport_width", "viewport_height", "viewport_origin_x", "viewport_origin_y"};
    private HudSafeArea() { }

    static JSONObject frame(Display display, View viewport) throws Exception {
        if (viewport == null || viewport.getWidth() <= 0 || viewport.getHeight() <= 0 || !viewport.isAttachedToWindow())
            throw new IllegalStateException("HUD viewport not measured");
        Point real = new Point(); display.getRealSize(real);
        int[] origin = new int[2]; viewport.getLocationOnScreen(origin);
        return new JSONObject().put("display_name", display.getName()).put("display_flags", display.getFlags())
                .put("display_width", real.x).put("display_height", real.y)
                .put("viewport_width", viewport.getWidth()).put("viewport_height", viewport.getHeight())
                .put("viewport_origin_x", origin[0]).put("viewport_origin_y", origin[1]);
    }
    static boolean matches(JSONObject saved, JSONObject frame) {
        if (saved == null || frame == null) return false;
        for (String key : FRAME_KEYS) if (!saved.has(key) || !frame.has(key)
                || !String.valueOf(saved.opt(key)).equals(String.valueOf(frame.opt(key)))) return false;
        return true;
    }
    static JSONObject record(JSONObject frame, int left, int right, int margin) throws Exception {
        JSONObject record = new JSONObject(frame.toString()).put("schema_version", 1)
                .put("left", left).put("right", right).put("margin_percent", margin)
                .put("saved_at_ms", System.currentTimeMillis());
        validate(record); return record;
    }
    private static void validate(JSONObject record) throws Exception {
        if (record.getInt("schema_version") != 1 || record.getString("display_name").isEmpty())
            throw new IllegalArgumentException("Unknown HUD calibration");
        for (String key : FRAME_KEYS) record.get(key);
        int width = record.getInt("viewport_width"), height = record.getInt("viewport_height");
        if (width > 16384 || height <= 0 || height > 16384 || record.getInt("display_width") <= 0
                || record.getInt("display_height") <= 0) throw new IllegalArgumentException("Invalid HUD viewport");
        HudPanelPlacement.safeInterval(width, record.getInt("left"), record.getInt("right"), record.getInt("margin_percent"));
    }
    static JSONObject read(Context context) {
        try {
            String value = context.getSharedPreferences(AppConfig.PREF_NAME, 0).getString(KEY, null);
            if (value == null) return null;
            JSONObject record = new JSONObject(value); validate(record); return record;
        } catch (Exception invalid) { return null; }
    }
    static boolean hasSaved(Context context) {
        return context.getSharedPreferences(AppConfig.PREF_NAME, 0).contains(KEY);
    }
    static boolean save(Context context, JSONObject record) {
        try { validate(record); return context.getSharedPreferences(AppConfig.PREF_NAME, 0).edit()
                .putString(KEY, record.toString()).commit(); }
        catch (Exception invalid) { return false; }
    }
    static boolean reset(Context context) {
        return context.getSharedPreferences(AppConfig.PREF_NAME, 0).edit().remove(KEY).commit();
    }
    static int[] interval(Context context, Display display, View viewport) {
        JSONObject saved = read(context);
        try {
            if (!matches(saved, frame(display, viewport))) return null;
            return HudPanelPlacement.safeInterval(saved.getInt("viewport_width"), saved.getInt("left"),
                    saved.getInt("right"), saved.getInt("margin_percent"));
        } catch (Exception invalid) { return null; }
    }
}
