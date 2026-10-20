package com.kooo.evcam.zeekr;

/** Logical pixels, deliberately allowing the marker to cross a reported display edge. */
public final class HudCalibrationGeometry {
    private HudCalibrationGeometry() { }
    public static int size(int requested, int width, int height) {
        return Math.max(1, Math.min(requested, Math.max(1, Math.min(width, height))));
    }
    public static int position(int requested, int extent, int size) {
        return Math.max(-size, Math.min(extent, requested));
    }
    public static int[] visible(int x, int y, int size, int width, int height) {
        int left = Math.max(0, Math.min(width, x));
        int top = Math.max(0, Math.min(height, y));
        return new int[]{left, top, Math.max(left, Math.min(width, x + size)),
                Math.max(top, Math.min(height, y + size))};
    }
}
