package com.kooo.evcam.zeekr;

/** Inverse ground-plane projection. X is right, Y is forward, Z is up; metres.
 * The equidistant lens and mounting defaults are estimates, not a 009 calibration.
 * No Android dependencies: geometry, coverage and compositing are testable on the JVM.
 */
public final class BirdEyeProjection {
    private BirdEyeProjection() {}

    public static final class Camera {
        public final int lane;
        public final double x, y, height, yaw, pitch, roll, fov, cx, cy;
        public final boolean mirror;
        private final double sy, co, sp, cp, sr, cr;

        public Camera(int lane, double x, double y, double height, double yaw,
                      double pitch, double roll, double fov, double cx, double cy, boolean mirror) {
            if (lane < 0 || lane > 3 || !finite(x, y, height, yaw, pitch, roll, fov, cx, cy)
                    || height <= 0 || fov < 90 || fov > 220 || cx < 0 || cx > 1 || cy < 0 || cy > 1) {
                throw new IllegalArgumentException("Invalid camera calibration");
            }
            this.lane = lane; this.x = x; this.y = y; this.height = height;
            this.yaw = yaw; this.pitch = pitch; this.roll = roll; this.fov = fov;
            this.cx = cx; this.cy = cy; this.mirror = mirror;
            sy = Math.sin(Math.toRadians(yaw)); co = Math.cos(Math.toRadians(yaw));
            sp = Math.sin(Math.toRadians(pitch)); cp = Math.cos(Math.toRadians(pitch));
            sr = Math.sin(Math.toRadians(roll)); cr = Math.cos(Math.toRadians(roll));
        }

        /** Returns a confidence weight and writes UV. Zero means no observed coverage.
         * Reject, never clamp, rays outside this lens: clamping produces false edge imagery.
         */
        public double project(double worldX, double worldY, double[] uv) {
            double dx = worldX - x, dy = worldY - y;
            double outward = dx * sy + dy * co;
            if (outward <= 0) return 0; // Do not see through the vehicle to its other side.
            double right = dx * co - dy * sy;
            double down = -outward * sp + height * cp;
            double forward = outward * cp + height * sp;
            double radial = Math.hypot(right, down);
            double theta = Math.atan2(radial, forward);
            double radius = theta / Math.toRadians(fov / 2);
            if (radius >= 1) return 0;
            double u = radial < 1e-10 ? 0 : radius * right / radial * .5;
            double v = radial < 1e-10 ? 0 : radius * down / radial * .5;
            double rx = u * cr - v * sr, ry = u * sr + v * cr;
            uv[0] = cx + (mirror ? -rx : rx); uv[1] = cy + ry;
            if (uv[0] < 0 || uv[0] > 1 || uv[1] < 0 || uv[1] > 1) return 0;
            double facing = outward / Math.hypot(dx, dy);
            return facing * facing * Math.min(1, (1 - radius) * 8);
        }
    }

    public static final class Calibration {
        public final double length, width, margin;
        public final Camera[] cameras;
        public Calibration(double length, double width, double margin, Camera[] cameras) {
            if (!finite(length, width, margin) || length <= 0 || width <= 0 || margin <= 0
                    || cameras == null || cameras.length != 4) throw new IllegalArgumentException("Invalid vehicle geometry");
            this.length = length; this.width = width; this.margin = margin;
            this.cameras = cameras.clone();
            boolean[] lanes = new boolean[4];
            for (Camera camera : cameras) {
                if (camera == null || lanes[camera.lane]) throw new IllegalArgumentException("Four distinct cameras required");
                lanes[camera.lane] = true;
            }
        }
        public boolean insideCar(double x, double y) {
            return Math.abs(x) <= width / 2 && Math.abs(y) <= length / 2;
        }
    }

    /** Precomputed source pixel addresses and normalized blend for each output pixel.
     * All four lanes come from one delivered buffer. This does not prove that their
     * sensor exposures are synchronized or that each individual camera is advancing.
     */
    public static final class Map {
        public final int width, height, sourceWidth, sourceHeight;
        private final int[] first, second, mix;
        private final int[] firstFraction, secondFraction;
        public Map(Calibration calibration, CompositeStreamGeometry.Plan plan, int sourceWidth,
                   int sourceHeight, int width, int height) {
            if (plan == null || !plan.isComposite() || sourceWidth <= 0 || sourceHeight <= 0
                    || width <= 0 || height <= 0 || (long) width * height > 1_000_000) {
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
                    double a = 0, b = 0;
                    for (Camera camera : calibration.cameras) {
                        double weight = camera.project(x, y, uv);
                        if (weight <= 0) continue;
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
                        if (weight > a) {
                            b = a; second[at] = first[at]; secondFraction[at] = firstFraction[at];
                            a = weight; first[at] = address; firstFraction[at] = fraction;
                        } else if (weight > b) {
                            b = weight; second[at] = address; secondFraction[at] = fraction;
                        }
                    }
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
                int b = second[i] < 0 ? a : sample(source, second[i], secondFraction[i], sourceWidth);
                int wb = mix[i], wa = 255 - wb;
                int r = (((a >> 16) & 255) * wa + ((b >> 16) & 255) * wb + 127) / 255;
                int g = (((a >> 8) & 255) * wa + ((b >> 8) & 255) * wb + 127) / 255;
                int blue = ((a & 255) * wa + (b & 255) * wb + 127) / 255;
                output[i] = 0xff000000 | r << 16 | g << 8 | blue;
            }
        }
    }

    /** Bilinear pixel-centre sampling. Zero fractions never read an unused neighbour. */
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
    private static boolean finite(double... values) {
        for (double v : values) if (Double.isNaN(v) || Double.isInfinite(v)) return false;
        return true;
    }
}
