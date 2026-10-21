package com.kooo.evcam.zeekr;

/** Empirical ray-to-reference-plane fit, not measured lens intrinsics or distance calibration.
 * The 009 profile is fitted from the owner's raw/factory image pairs across garage and road scenes.
 * No scene pixels are embedded: every output is sampled from the current live buffer.
 */
public final class BirdEyeImageFit {
    public static final String PROFILE = "009-image-fit-20261021";
    // Wider view of observed rays, not a new metric calibration. Keep a bounded extent.
    public static final double DEFAULT_MARGIN = 3, MAX_MARGIN = 3;
    final int role;
    private final double[] groundToRay;
    private final double[] rayToGround;

    BirdEyeImageFit(int role, double[] matrix) {
        if (role < 0 || role > 3 || matrix == null || matrix.length != 9)
            throw new IllegalArgumentException("Invalid image fit");
        for (double value : matrix) if (!Double.isFinite(value))
            throw new IllegalArgumentException("Non-finite image fit");
        this.role = role;
        rayToGround = matrix.clone();
        groundToRay = inverse(matrix);
    }

    /** Keep the fitted source on its outward side and inside observed lens coverage. */
    double project(double x, double y, double[] uv) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) return 0;
        if ((role == 0 && y <= 2.6) || (role == 1 && y >= -2.6)
                || (role == 2 && x >= -1) || (role == 3 && x <= 1)) return 0;
        double rx = groundToRay[0] * x + groundToRay[1] * y + groundToRay[2];
        double ry = groundToRay[3] * x + groundToRay[4] * y + groundToRay[5];
        double rz = groundToRay[6] * x + groundToRay[7] * y + groundToRay[8];
        double radial = Math.hypot(rx, ry);
        if (!Double.isFinite(radial) || !Double.isFinite(rz) || Math.hypot(radial, rz) < 1e-10) return 0;
        double radius = Math.atan2(radial, rz) / Math.toRadians(95);
        if (!Double.isFinite(radius) || radius >= 1) return 0;
        uv[0] = .5 + (radial < 1e-10 ? 0 : .5 * radius * rx / radial);
        uv[1] = .5 + (radial < 1e-10 ? 0 : .5 * radius * ry / radial);
        if (uv[0] < 0 || uv[0] > 1 || uv[1] < 0 || uv[1] > 1) return 0;
        return Math.min(1, (1 - radius) * 8);
    }

    String describe() { return java.util.Arrays.toString(rayToGround); }

    private static double[] inverse(double[] m) {
        double a = m[4]*m[8]-m[5]*m[7], b = m[5]*m[6]-m[3]*m[8], c = m[3]*m[7]-m[4]*m[6];
        double det = m[0]*a + m[1]*b + m[2]*c;
        if (!Double.isFinite(det) || Math.abs(det) < 1e-10)
            throw new IllegalArgumentException("Singular image fit");
        double[] out = {a, m[2]*m[7]-m[1]*m[8], m[1]*m[5]-m[2]*m[4],
                b, m[0]*m[8]-m[2]*m[6], m[2]*m[3]-m[0]*m[5],
                c, m[1]*m[6]-m[0]*m[7], m[0]*m[4]-m[1]*m[3]};
        for (int i = 0; i < 9; i++) {
            out[i] /= det;
            if (!Double.isFinite(out[i])) throw new IllegalArgumentException("Unstable image fit");
        }
        return out;
    }

    public static BirdEyeProjection.Calibration zeekr009(int[] lanes, double margin) {
        if (lanes == null || lanes.length != 4 || !Double.isFinite(margin) || margin < .5 || margin > MAX_MARGIN)
            throw new IllegalArgumentException("Invalid 009 reference profile");
        BirdEyeProjection.Camera[] cameras = new BirdEyeProjection.Camera[4];
        BirdEyeImageFit[] fits = new BirdEyeImageFit[4];
        for (int i = 0; i < 4; i++) {
            cameras[i] = new BirdEyeProjection.Camera(lanes[i], i == 2 ? -1 : i == 3 ? 1 : 0,
                    i == 0 ? 2.6 : i == 1 ? -2.6 : 0, 1, new int[]{0,180,270,90}[i],
                    35, 0, 190, .5, .5, false);
            fits[i] = new BirdEyeImageFit(i, MATRICES[i]);
        }
        return new BirdEyeProjection.Calibration(5.2, 2, margin, cameras, fits);
    }

    // Lens normalisation: estimated equidistant 190°, centre .5/.5. Reference
    // footprint: 5.2×2 visual units. Joint fit gives both reference captures equal total weight.
    // Road hold-outs and an earlier capture are excluded; coefficients are not metric calibration.
    private static final double[][] MATRICES = {
        {1.5254024391451004, -0.048682602245399186, -0.007325653854474614, -0.057994796150342809, 5.319867618336013, 3.6648127411732316, -0.020911064763847333, 2.3188986539967753, 1},
        {-1.9662220090775444, 0.028666604533514519, 0.019595646018705573, 0.079598350688436403, -4.3731061716348902, -3.8398318921949595, -0.03055299452120263, 2.0196257523839285, 1},
        {-0.079912569988391136, -0.13698222964854179, -2.3109384025310074, 1.9781421752667734, 1.4684235800625989, 0.91708777414831655, 0.029820622907050457, 1.3511628813203185, 1},
        {0.030100166157953693, -0.016715738376757956, 2.3934385136757408, -1.9900610879632386, 1.2433798513851375, 0.95578984775538245, -0.0042948239060715942, 1.3079046899199696, 1},
    };
}
