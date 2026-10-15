package com.kooo.evcam.developer;

/** Bounds native capture allocations; no resizing, correction or stitching of exported frames. */
public final class ProbeSizes {
    public static final long MAX_PIXELS = 8_000_000;
    private ProbeSizes() { }
    public static boolean bounded(int w, int h) {
        return w > 0 && h > 0 && w <= 6000 && h <= 6000 && (long) w * h <= MAX_PIXELS;
    }
    /** Match the native preview aspect first; largest supported bounded size breaks ties. */
    public static int best(int[][] sizes, int nativeW, int nativeH) {
        int best = -1;
        double bestError = Double.MAX_VALUE;
        long bestPixels = 0;
        for (int i = 0; i < sizes.length; i++) {
            int w = sizes[i][0], h = sizes[i][1];
            if (!bounded(w, h)) continue;
            double error = nativeW > 0 && nativeH > 0
                    ? Math.abs(Math.log(((double) w / h) / ((double) nativeW / nativeH))) : 0;
            long pixels = (long) w * h;
            if (error < bestError - .001 || (Math.abs(error - bestError) <= .001 && pixels > bestPixels)) {
                best = i; bestError = error; bestPixels = pixels;
            }
        }
        return best;
    }
}
