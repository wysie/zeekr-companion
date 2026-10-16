package com.kooo.evcam.zeekr;

/** Bounded resolution policy; a downgrade lasts until the next activation or quality selection. */
public final class BirdEyeQuality {
    public static final int LOW = 0, BALANCED = 1, FULL = 2;
    public static final long MAX_READBACK_MS = 40, MAX_RENDER_MS = 150;
    private static final long MAX_SOURCE_PIXELS = 8_000_000;
    private BirdEyeQuality() {}
    public static int normalize(int quality) { return quality < LOW || quality > FULL ? BALANCED : quality; }
    public static int lower(int quality) { return Math.max(LOW, normalize(quality) - 1); }
    public static boolean slow(long readbackMs, long renderMs) {
        return readbackMs > MAX_READBACK_MS || renderMs > MAX_RENDER_MS;
    }
    public static final class Size {
        public final int sourceWidth, sourceHeight, outputWidth, outputHeight, intervalMs;
        private Size(int sw, int sh, int ow, int oh, int interval) {
            sourceWidth = sw; sourceHeight = sh; outputWidth = ow; outputHeight = oh; intervalMs = interval;
        }
    }
    public static Size resolve(int quality, int nativeWidth, int nativeHeight) {
        if (nativeWidth <= 0 || nativeHeight <= 0) throw new IllegalArgumentException("Missing source dimensions");
        quality = normalize(quality);
        double factor = quality == FULL ? 1 : Math.min(1,
                (quality == BALANCED ? 768.0 : 256.0) / Math.min(nativeWidth, nativeHeight));
        factor = Math.min(factor, Math.sqrt(MAX_SOURCE_PIXELS / ((double) nativeWidth * nativeHeight)));
        factor = Math.min(factor, 8192.0 / Math.max(nativeWidth, nativeHeight));
        int sw = Math.max(1, (int) Math.floor(nativeWidth * factor));
        int sh = Math.max(1, (int) Math.floor(nativeHeight * factor));
        return new Size(sw, sh, quality == FULL ? 960 : quality == BALANCED ? 640 : 320,
                quality == FULL ? 1440 : quality == BALANCED ? 960 : 480,
                quality == FULL ? 200 : quality == BALANCED ? 100 : 67);
    }
}
