package com.kooo.evcam.zeekr;

import android.os.Build;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Synthetic ART compute comparison, not a camera/readback or head-unit benchmark. */
@RunWith(AndroidJUnit4.class)
public class BirdEyeRenderAndroidTest {
    @Test public void preservesCompleteFramesAndMeasuresLegacyVersusOptimizedCompute() throws Exception {
        StringBuilder report = new StringBuilder("Synthetic renderer only; no camera, readback or UI upload.\n")
                .append("device=").append(Build.MODEL).append(" hardware=").append(Build.HARDWARE)
                .append(" sdk=").append(Build.VERSION.SDK_INT).append('\n');
        StreamLayoutTable.setCompositeCameraId("render-benchmark");
        try {
            CompositeStreamGeometry.Plan plan = CompositeStreamGeometry.analyse("render-benchmark", 1280, 5140);
            for (boolean fitted : new boolean[]{false, true}) for (int quality = 0; quality <= 2; quality++) {
                BirdEyeQuality.Size size = BirdEyeQuality.resolve(quality, 1280, 5140);
                BirdEyeProjection.Calibration calibration = BirdEyeImageFit.zeekr009(new int[]{0,1,2,3}, 3);
                if (!fitted) calibration = new BirdEyeProjection.Calibration(calibration.length,
                        calibration.width, calibration.margin, calibration.cameras);
                BirdEyeProjection.Map map = new BirdEyeProjection.Map(calibration, plan,
                        size.sourceWidth, size.sourceHeight, size.outputWidth, size.outputHeight);
                Reference old = new Reference(calibration, plan, size.sourceWidth, size.sourceHeight,
                        size.outputWidth, size.outputHeight);
                int[] input = new int[size.sourceWidth * size.sourceHeight];
                Random random = new Random(360);
                for (int i = 0; i < input.length; i++) input[i] = random.nextInt() | 0xff000000;
                int[] expected = new int[size.outputWidth * size.outputHeight], actual = new int[expected.length];
                old.render(input, expected); map.render(input, actual);
                assertArrayEquals("Every output pixel must match the previous renderer", expected, actual);
                for (int warm = 0; warm < 8; warm++) { old.render(input, expected); map.render(input, actual); }
                long[] baseline = new long[11], optimized = new long[11];
                for (int run = 0; run < baseline.length; run++) {
                    // Alternate order to avoid consistently favouring the second pass's warm cache.
                    if ((run & 1) == 0) {
                        baseline[run] = measure(() -> old.render(input, expected));
                        optimized[run] = measure(() -> map.render(input, actual));
                    } else {
                        optimized[run] = measure(() -> map.render(input, actual));
                        baseline[run] = measure(() -> old.render(input, expected));
                    }
                }
                assertArrayEquals(expected, actual);
                Arrays.sort(baseline); Arrays.sort(optimized);
                report.append(fitted ? "009-fit" : "manual").append(" quality=").append(quality)
                        .append(" source=").append(size.sourceWidth).append('x').append(size.sourceHeight)
                        .append(" output=").append(size.outputWidth).append('x').append(size.outputHeight)
                        .append(" legacyMedianMs=").append(baseline[5] / 1e6)
                        .append(" optimizedMedianMs=").append(optimized[5] / 1e6).append('\n');
            }
            File output = new File(InstrumentationRegistry.getInstrumentation().getTargetContext()
                    .getExternalFilesDir(null), "bird-eye-render-benchmark.txt");
            Files.write(output.toPath(), report.toString().getBytes(StandardCharsets.UTF_8));
        } finally { StreamLayoutTable.reset(); }
    }

    private static long measure(Runnable work) { long start = System.nanoTime(); work.run(); return System.nanoTime() - start; }

    /** Frozen pre-optimization lookup builder, sampler and renderer. */
    private static final class Reference {
        public final int width, height, sourceWidth, sourceHeight;
        private final int[] first, second, mix;
        private final int[] firstFraction, secondFraction;
        Reference(BirdEyeProjection.Calibration calibration, CompositeStreamGeometry.Plan plan, int sourceWidth,
                   int sourceHeight, int width, int height) {
            if (plan == null || !plan.isComposite() || sourceWidth <= 0 || sourceHeight <= 0
                    || width <= 0 || height <= 0 || (long) width * height > 2_000_000) {
                throw new IllegalArgumentException("Invalid surround frame");
            }
            this.width = width; this.height = height;
            this.sourceWidth = sourceWidth; this.sourceHeight = sourceHeight;
            first = new int[width * height]; second = new int[first.length]; mix = new int[first.length];
            firstFraction = new int[first.length]; secondFraction = new int[first.length];
            java.util.Arrays.fill(first, -1); java.util.Arrays.fill(second, -1);
            double[] uv = new double[2];
            double metresPerPixel = Math.max((calibration.width + 2 * calibration.margin) / width,
                    (calibration.length + 2 * calibration.margin) / height);
            for (int row = 0; row < height; row++) {
                double y = (height / 2.0 - row - .5) * metresPerPixel;
                for (int col = 0; col < width; col++) {
                    double x = (col + .5 - width / 2.0) * metresPerPixel;
                    if (calibration.insideCar(x, y)) continue;
                    int at = row * width + col;
                    double a = 0, b = 0, fallbackWeight = 0;
                    int fallbackAddress = -1, fallbackFraction = 0;
                    for (int role = 0; role < calibration.cameras.length; role++) {
                        BirdEyeProjection.Camera camera = calibration.cameras[role];
                        double confidence = calibration.project(role, x, y, uv);
                        if (confidence <= 0) continue;
                        CompositeStreamGeometry.Lane lane = plan.lane(camera.lane);
                        // Clamp to pixel centres INSIDE this lane, never a separator or neighbour.
                        int minX = Math.max(0, (int) Math.ceil(lane.u0 * sourceWidth));
                        int maxX = Math.min(sourceWidth - 1, (int) Math.ceil(lane.u1 * sourceWidth) - 1);
                        int minY = Math.max(0, (int) Math.ceil(lane.v0 * sourceHeight));
                        int maxY = Math.min(sourceHeight - 1, (int) Math.ceil(lane.v1 * sourceHeight) - 1);
                        double px = Math.max(minX, Math.min(maxX,
                                (lane.u0 + uv[0] * (lane.u1 - lane.u0)) * sourceWidth - .5));
                        double py = Math.max(minY, Math.min(maxY,
                                (lane.v0 + uv[1] * (lane.v1 - lane.v0)) * sourceHeight - .5));
                        int sx = (int) Math.floor(px), sy = (int) Math.floor(py);
                        int fraction = (int) ((px - sx) * 256) | (int) ((py - sy) * 256) << 8;
                        int address = sy * sourceWidth + sx;
                        // If the intended camera has no lens coverage, use an actually observed
                        // alternative, without clamping an invalid ray or blending in black.
                        if (confidence > fallbackWeight) {
                            fallbackWeight = confidence; fallbackAddress = address; fallbackFraction = fraction;
                        }
                        double weight = BirdEyeProjection.regionWeight(role, x, y, calibration);
                        if (weight <= 0) continue;
                        if (weight > a) {
                            b = a; second[at] = first[at]; secondFraction[at] = firstFraction[at];
                            a = weight; first[at] = address; firstFraction[at] = fraction;
                        } else if (weight > b) {
                            b = weight; second[at] = address; secondFraction[at] = fraction;
                        }
                    }
                    if (a <= 0) { first[at] = fallbackAddress; firstFraction[at] = fallbackFraction; }
                    mix[at] = a > 0 ? (int) Math.round(255 * b / (a + b)) : 0;
                }
            }
        }
        public void render(int[] source, int[] output) {
            if (source.length != sourceWidth * sourceHeight || output.length != first.length) {
                throw new IllegalArgumentException("Frame size changed");
            }
            for (int i = 0; i < first.length; i++) {
                if (first[i] < 0) { output[i] = 0xff000000; continue; }
                int a = sample(source, first[i], firstFraction[i], sourceWidth);
                int wb = mix[i], wa = 255 - wb;
                int b = second[i] < 0 || wb == 0 ? a : sample(source, second[i], secondFraction[i], sourceWidth);
                int r = (((a >> 16) & 255) * wa + ((b >> 16) & 255) * wb + 127) / 255;
                int g = (((a >> 8) & 255) * wa + ((b >> 8) & 255) * wb + 127) / 255;
                int blue = ((a & 255) * wa + (b & 255) * wb + 127) / 255;
                output[i] = 0xff000000 | r << 16 | g << 8 | blue;
            }
        }
        static int sample(int[] source, int at, int fraction, int stride) {
            int fx = fraction & 255, fy = fraction >>> 8;
            int a = source[at], b = fx == 0 ? a : source[at + 1];
            int c = fy == 0 ? a : source[at + stride];
            int d = fy == 0 ? b : fx == 0 ? c : source[at + stride + 1];
            if ((a >>> 24) != 255 || (b >>> 24) != 255 || (c >>> 24) != 255 || (d >>> 24) != 255)
                throw new IllegalArgumentException("Camera readback incomplete");
            int result = 0xff000000;
            for (int shift = 0; shift <= 16; shift += 8) {
                int upper = ((a >>> shift) & 255) * (256 - fx) + ((b >>> shift) & 255) * fx;
                int lower = ((c >>> shift) & 255) * (256 - fx) + ((d >>> shift) & 255) * fx;
                result |= ((upper * (256 - fy) + lower * fy + 32768) >>> 16) << shift;
            }
            return result;
        }
    }

}
