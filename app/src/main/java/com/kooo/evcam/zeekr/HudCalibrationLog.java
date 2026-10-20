package com.kooo.evcam.zeekr;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Bounded, durable user observations, separate from backed-up vehicle settings. */
public final class HudCalibrationLog {
    private static final int LIMIT = 128;
    private HudCalibrationLog() { }
    public static synchronized JSONArray read(Context context) {
        try { return new JSONArray(context.getSharedPreferences("hud_visibility_observations", 0)
                .getString("observations", "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }
    public static synchronized boolean save(Context context, JSONObject observation) {
        JSONArray previous = read(context), next = new JSONArray();
        for (int i = Math.max(0, previous.length() - LIMIT + 1); i < previous.length(); i++)
            next.put(previous.opt(i));
        next.put(observation);
        return context.getSharedPreferences("hud_visibility_observations", 0).edit()
                .putString("observations", next.toString()).commit();
    }
}
