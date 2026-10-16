package com.kooo.evcam.zeekr;

/** Empirical ray-to-reference-plane fit, not measured lens intrinsics or distance calibration.
 * The 009 profile was fitted from the owner's raw/factory image pairs. It is opt-in.
 * No scene pixels are embedded: every output is sampled from the current live buffer.
 */
public final class BirdEyeImageFit {
    public static final String PROFILE = "009-image-fit-20261016";
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
        if (lanes == null || lanes.length != 4 || !Double.isFinite(margin) || margin < .5 || margin > 2)
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
    // footprint: 5.2×2 visual units. Coefficients do not establish metric accuracy.
    private static final double[][] MATRICES = {
        {1.524746493108509, -0.005208178299783897, -0.006325936440197357, 0.01830193446779125, 5.381663307617981, 3.680939150957493, 0.009184350089327125, 2.358644490690996, 1},
        {-1.966167145981322, 0.04532711640949655, 0.02867818252229461, 0.04614078714071514, -4.21384985859972, -3.896487569061321, -0.04045835000852071, 2.051786238483615, 1},
        {-0.06859749417753332, 0.01213937866995913, -2.328204872037626, 1.999132909003765, 1.149917278552485, 0.8826836717310127, 0.02115174743226117, 1.229318478437866, 1},
        {0.02163463993162737, 0.1062122183403377, 2.34253581098979, -1.985361573959905, 1.219060398330053, 0.9597291093504084, -0.005024891604802594, 1.293869553358916, 1},
    };
}
