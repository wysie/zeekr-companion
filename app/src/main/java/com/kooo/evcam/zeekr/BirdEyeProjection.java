package com.kooo.evcam.zeekr;

/** Inverse ground-plane projection. X is right, Y is forward, Z is up; metres.
 * The equidistant lens and mounting defaults are estimates, not a 009 calibration.
 * No Android dependencies: geometry, coverage and compositing are testable on the JVM.
 */
public final class BirdEyeProjection {
    private BirdEyeProjection() {}

    /** Full feather width perpendicular to the diagonal joining two body edges, in metres.
     * Keep overlap local: averaging uncalibrated views over whole corners doubles markings.
     */
    static final double SEAM_WIDTH = .20;

    /** Physical camera roles are front/rear/left/right; source lane assignment is independent.
     * A diagonal extends out from each body corner. Only a narrow corridor uses both views.
     * This partitions observed pixels; it does not correct inaccurate lens or mount geometry.
     */
    static double regionWeight(int camera, double x, double y, Calibration calibration) {
        double sideDistance = Math.abs(x) - calibration.width / 2;
        double endDistance = Math.abs(y) - calibration.length / 2;
        double t = .5 + (endDistance - sideDistance) / (calibration.seamWidth() * Math.sqrt(2));
        t = Math.max(0, Math.min(1, t));
        double endWeight = t * t * (3 - 2 * t);
        if (camera == 0) return y > 0 ? endWeight : 0;
        if (camera == 1) return y < 0 ? endWeight : 0;
        if (camera == 2) return x < 0 ? 1 - endWeight : 0;
        if (camera == 3) return x > 0 ? 1 - endWeight : 0;
        throw new IllegalArgumentException("Invalid camera role");
    }

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
        /** Physical role order: front, rear, left, right. Each role can select any source lane. */
        public final Camera[] cameras;
        private final BirdEyeImageFit[] imageFits;
        public Calibration(double length, double width, double margin, Camera[] cameras) {
            this(length, width, margin, cameras, null);
        }
        Calibration(double length, double width, double margin, Camera[] cameras, BirdEyeImageFit[] imageFits) {
            if (!finite(length, width, margin) || length <= 0 || width <= 0 || margin <= 0
                    || cameras == null || cameras.length != 4) throw new IllegalArgumentException("Invalid vehicle geometry");
            this.length = length; this.width = width; this.margin = margin;
            this.cameras = cameras.clone();
            if (imageFits != null && imageFits.length != 4) throw new IllegalArgumentException("Four image fits required");
            this.imageFits = imageFits == null ? null : imageFits.clone();
            boolean[] lanes = new boolean[4];
            for (int i = 0; i < cameras.length; i++) {
                Camera camera = cameras[i];
                if (camera == null || lanes[camera.lane]) throw new IllegalArgumentException("Four distinct cameras required");
                if (this.imageFits != null && (this.imageFits[i] == null || this.imageFits[i].role != i))
                    throw new IllegalArgumentException("Image fit role mismatch");
                lanes[camera.lane] = true;
            }
        }
        public boolean imageFitted() { return imageFits != null; }
        double seamWidth() { return imageFitted() ? .12 : SEAM_WIDTH; }
        double project(int role, double x, double y, double[] uv) {
            return imageFits == null ? cameras[role].project(x, y, uv) : imageFits[role].project(x, y, uv);
        }
        public boolean insideCar(double x, double y) {
            return Math.abs(x) <= width / 2 && Math.abs(y) <= length / 2;
        }
        public String describe() {
            StringBuilder result = new StringBuilder("projection=equidistant seam=body-corner-diagonal feather=")
                    .append(seamWidth()).append(imageFits == null ? "m (estimated calibration)\nlength=" : " reference units (image fit)\nlength=")
                    .append(length).append(" width=").append(width).append(" margin=").append(margin)
                    .append(imageFits == null ? " (metres)\n" : " (reference units)\n");
            if (imageFits != null) result.append("profile=").append(BirdEyeImageFit.PROFILE)
                    .append("; empirical image alignment, reference units are not measured distances\n");
            for (int i = 0; i < cameras.length; i++) {
                Camera c = cameras[i];
                if (imageFits != null) {
                    result.append("camera ").append(i).append(": lane=").append(c.lane)
                            .append(" rayToReferenceGround=").append(imageFits[i].describe()).append('\n');
                    continue;
                }
                result.append("camera ").append(i).append(": lane=").append(c.lane)
                        .append(" mount=").append(c.x).append(',').append(c.y).append(',').append(c.height)
                        .append(" yaw/pitch/roll=").append(c.yaw).append('/').append(c.pitch).append('/').append(c.roll)
                        .append(" fov=").append(c.fov).append(" centre=").append(c.cx).append(',').append(c.cy)
                        .append(" mirror=").append(c.mirror).append('\n');
            }
            return result.toString();
        }
    }

    /** Precomputed source pixel addresses and narrow seam blend for each output pixel.
     * All four lanes come from one delivered buffer. This does not prove that their
     * sensor exposures are synchronized or that each individual camera is advancing.
     */
    public static final class Map {
        public final int width, height, sourceWidth, sourceHeight;
        // Each main entry packs address, sub-pixel fraction and seam weight. The
        // common non-seam path reads one lookup array instead of three.
        private final long[] first, second;
        public Map(Calibration calibration, CompositeStreamGeometry.Plan plan, int sourceWidth,
                   int sourceHeight, int width, int height) {
            if (plan == null || !plan.isComposite() || sourceWidth <= 0 || sourceHeight <= 0
                    || width <= 0 || height <= 0 || (long) width * height > 2_000_000) {
                throw new IllegalArgumentException("Invalid surround frame");
            }
            this.width = width; this.height = height;
            this.sourceWidth = sourceWidth; this.sourceHeight = sourceHeight;
            first = new long[width * height]; second = new long[first.length];
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
                    int firstAddress = -1, secondAddress = -1, firstFraction = 0, secondFraction = 0;
                    for (int role = 0; role < calibration.cameras.length; role++) {
                        Camera camera = calibration.cameras[role];
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
                        double weight = regionWeight(role, x, y, calibration);
                        if (weight <= 0) continue;
                        if (weight > a) {
                            b = a; secondAddress = firstAddress; secondFraction = firstFraction;
                            a = weight; firstAddress = address; firstFraction = fraction;
                        } else if (weight > b) {
                            b = weight; secondAddress = address; secondFraction = fraction;
                        }
                    }
                    if (a <= 0) { firstAddress = fallbackAddress; firstFraction = fallbackFraction; }
                    int mix = a > 0 ? (int) Math.round(255 * b / (a + b)) : 0;
                    first[at] = firstAddress < 0 ? -1 : ((long) firstAddress << 24) | ((long) firstFraction << 8) | mix;
                    second[at] = secondAddress < 0 ? -1 : ((long) secondAddress << 16) | secondFraction;
                }
            }
        }
        public void render(int[] source, int[] output) {
            if (source.length != sourceWidth * sourceHeight || output.length != first.length) {
                throw new IllegalArgumentException("Frame size changed");
            }
            for (int i = 0; i < first.length; i++) {
                long entry = first[i];
                if (entry < 0) { output[i] = 0xff000000; continue; }
                int a = sample(source, (int) (entry >>> 24), (int) (entry >>> 8) & 65535, sourceWidth);
                int wb = (int) entry & 255;
                // Most pixels belong to one camera, outside the narrow seam. Blending
                // that pixel with itself adds work but cannot change its value.
                if (wb == 0 || second[i] < 0) { output[i] = a; continue; }
                int wa = 255 - wb;
                long other = second[i];
                int b = sample(source, (int) (other >>> 16), (int) other & 65535, sourceWidth);
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
        if (fraction == 0) return a;
        int left = 256 - fx, top = 256 - fy;
        int aa = left * top, bb = fx * top, cc = left * fy, dd = fx * fy;
        // Two independent 32-bit lanes hold red and blue. Their weighted sums
        // fit in 24 bits, so neither arithmetic nor rounding can cross lanes.
        long rb = redBlue(a) * aa + redBlue(b) * bb + redBlue(c) * cc + redBlue(d) * dd
                + 0x0000800000008000L;
        // Green's unsigned 32-bit sum is bounded by 255*256*65536 + rounding.
        int green = (a & 0xff00) * aa + (b & 0xff00) * bb
                + (c & 0xff00) * cc + (d & 0xff00) * dd + 0x800000;
        return 0xff000000 | (int) ((rb >>> 32) & 0xff0000)
                | ((green >>> 16) & 0xff00) | (int) ((rb >>> 16) & 255);
    }
    private static long redBlue(int pixel) {
        return ((long) (pixel & 0xff0000) << 16) | (pixel & 255);
    }
    private static boolean finite(double... values) {
        for (double v : values) if (Double.isNaN(v) || Double.isInfinite(v)) return false;
        return true;
    }
}
