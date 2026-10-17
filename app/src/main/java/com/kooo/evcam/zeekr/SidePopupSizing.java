package com.kooo.evcam.zeekr;

import com.kooo.evcam.AppConfig;

/** Shared size limits for the settings slider and an on-screen pinch. */
public final class SidePopupSizing {
    private SidePopupSizing() { }

    public static String normalizeAspect(String aspect) {
        return "3:4".equals(aspect) || "4:3".equals(aspect) || "16:9".equals(aspect) || "11:5".equals(aspect)
                ? aspect : "1:1";
    }

    public static double aspect(String value) {
        switch (normalizeAspect(value)) {
            case "3:4": return 3.0 / 4;
            case "4:3": return 4.0 / 3;
            case "16:9": return 16.0 / 9;
            case "11:5": return 2.2;
            default: return 1;
        }
    }

    public static final class Size {
        public final int width, height;
        Size(int width, int height) { this.width = width; this.height = height; }
    }

    /** Height follows the existing slider/pinch policy. Fit both axes together and
     * leave room for the corner controls whenever the screen can accommodate them.
     */
    public static Size dimensions(int percent, String shape, int screenWidth, int screenHeight, int minimum) {
        int w = Math.max(1, screenWidth), h = Math.max(1, screenHeight);
        double ratio = aspect(shape);
        double requested = Math.max(size(percent,w,h), Math.max(minimum, minimum / ratio));
        double height = Math.min(requested, Math.min(h, w / ratio));
        int resultHeight = Math.max(1, Math.min(h, (int) Math.round(height)));
        int resultWidth = Math.max(1, Math.min(w, (int) Math.round(height * ratio)));
        return new Size(resultWidth, resultHeight);
    }

    public static int size(int percent, int screenWidth, int screenHeight) {
        int maxSize = Math.max(1, screenWidth / 2);
        int bounded = Math.max(AppConfig.SIDE_POPUP_MIN_SIZE_PERCENT,
                Math.min(AppConfig.SIDE_POPUP_MAX_SIZE_PERCENT, percent));
        return Math.min(maxSize, Math.max(AppConfig.REARVIEW_MIN_SIZE,
                Math.round(Math.max(0, screenHeight) * bounded / 100f)));
    }

    public static int resizedPercent(int startSize, float scale, int screenHeight) {
        if (screenHeight <= 0) {
            return AppConfig.SIDE_POPUP_DEFAULT_SIZE_PERCENT;
        }
        float safeScale = Float.isFinite(scale) && scale > 0f ? scale : 1f;
        int percent = Math.round(Math.max(0, startSize) * safeScale * 100f / screenHeight);
        return Math.max(AppConfig.SIDE_POPUP_MIN_SIZE_PERCENT,
                Math.min(AppConfig.SIDE_POPUP_MAX_SIZE_PERCENT, percent));
    }

    public static int clampPosition(int coordinate, int screenExtent, int windowExtent) {
        return Math.max(0, Math.min(Math.max(0, screenExtent - windowExtent), coordinate));
    }
}
