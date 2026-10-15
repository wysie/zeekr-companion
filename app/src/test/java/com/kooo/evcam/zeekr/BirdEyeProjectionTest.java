package com.kooo.evcam.zeekr;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class BirdEyeProjectionTest {
    @Before public void setUp() { StreamLayoutTable.setCompositeCameraId("surround-test"); }
    @After public void tearDown() { StreamLayoutTable.reset(); }
    private BirdEyeProjection.Camera camera(int lane, double x, double y, double yaw, boolean mirror) {
        return new BirdEyeProjection.Camera(lane, x, y, 1, yaw, 45, 0, 190, .5, .5, mirror);
    }
    private BirdEyeProjection.Calibration defaults() {
        return new BirdEyeProjection.Calibration(5.2, 2, 2, new BirdEyeProjection.Camera[]{
                camera(0, 0, 2.6, 0, false), camera(1, 0, -2.6, 180, false),
                camera(2, -1, 0, 270, false), camera(3, 1, 0, 90, false)});
    }
    @Test public void bilinearSamplingInterpolatesAPixelGradient() {
        int[] pixels = {0xff000000, 0xffff0000, 0xff00ff00, 0xffffff00};
        assertEquals(0xff808000, BirdEyeProjection.sample(pixels, 0, 128 | (128 << 8), 2));
        assertEquals(0xff800000, BirdEyeProjection.sample(pixels, 0, 128, 2));
    }
    @Test public void fullResolutionRendersFourLanesWithoutSeparatorLeakage() {
        BirdEyeQuality.Size size = BirdEyeQuality.resolve(BirdEyeQuality.FULL, 1280, 5140);
        CompositeStreamGeometry.Plan plan = CompositeStreamGeometry.analyse("surround-test", 1280, 5140);
        int[] source = new int[size.sourceWidth * size.sourceHeight];
        java.util.Arrays.fill(source, 0xffff00ff);
        int[] colours = {0xffff0000, 0xff00ff00, 0xff0000ff, 0xffffff00};
        for (int i = 0; i < 4; i++) {
            CompositeStreamGeometry.Lane lane = plan.lane(i);
            for (int y = lane.y; y < lane.y + lane.height; y++)
                java.util.Arrays.fill(source, y * 1280, (y + 1) * 1280, colours[i]);
        }
        int[] output = new int[size.outputWidth * size.outputHeight];
        new BirdEyeProjection.Map(defaults(), plan, 1280, 5140, size.outputWidth, size.outputHeight).render(source, output);
        assertEquals(colours[0], output[100 * 960 + 480]);
        assertEquals(colours[1], output[1340 * 960 + 480]);
        assertEquals(colours[2], output[720 * 960 + 60]);
        assertEquals(colours[3], output[720 * 960 + 900]);
        assertEquals(0xff000000, output[720 * 960 + 480]);
        for (int pixel : output) assertNotEquals("Separator colour must not leak", 0xffff00ff, pixel);
    }
    @Test(expected=IllegalArgumentException.class) public void outputBudgetIsCheckedBeforeAllocation() {
        new BirdEyeProjection.Map(defaults(), CompositeStreamGeometry.analyse("surround-test", 1280, 5140),
                1280, 5140, 2001, 1000);
    }
    @Test public void zeroFractionDoesNotReadBeyondALaneOrBuffer() {
        assertEquals(0xffabcdef, BirdEyeProjection.sample(new int[]{0xffabcdef}, 0, 0, 1));
        assertEquals(0xff123456, BirdEyeProjection.sample(new int[]{0xff123456, 0}, 0, 0, 2));
    }
    @Test(expected=IllegalArgumentException.class) public void transparentInterpolationNeighbourRejectsReadback() {
        BirdEyeProjection.sample(new int[]{0xff000000, 0}, 0, 128, 2);
    }
    @Test public void opticalAxisOnGroundHitsLensCentre() {
        double[] uv = new double[2];
        assertTrue(camera(0, 0, 0, 0, false).project(0, 1, uv) > 0);
        assertEquals(.5, uv[0], 1e-8); assertEquals(.5, uv[1], 1e-8);
    }
    @Test public void worldRightIsImageRightForFrontAndLeftForRear() {
        double[] uv = new double[2];
        camera(0, 0, 0, 0, false).project(1, 2, uv); assertTrue(uv[0] > .5);
        camera(1, 0, 0, 180, false).project(1, -2, uv); assertTrue(uv[0] < .5);
    }
    @Test public void headingMapsLeftAndRightGroundToCorrespondingOpticalAxes() {
        double[] uv = new double[2];
        assertTrue(camera(2, 0, 0, 270, false).project(-1, 0, uv) > 0);
        assertEquals(.5, uv[0], 1e-8); assertEquals(.5, uv[1], 1e-8);
        assertTrue(camera(3, 0, 0, 90, false).project(1, 0, uv) > 0);
        assertEquals(.5, uv[0], 1e-8); assertEquals(.5, uv[1], 1e-8);
    }
    @Test public void sourceMirroringOnlyReversesU() {
        double[] a = new double[2], b = new double[2];
        camera(0, 0, 0, 0, false).project(1, 2, a);
        camera(0, 0, 0, 0, true).project(1, 2, b);
        assertEquals(1 - a[0], b[0], 1e-8); assertEquals(a[1], b[1], 1e-8);
    }
    @Test public void rotationCanUndoAQuarterTurnOfTheSource() {
        double[] a = new double[2], b = new double[2];
        camera(0, 0, 0, 0, false).project(1, 2, a);
        new BirdEyeProjection.Camera(0, 0, 0, 1, 0, 45, 90, 190, .5, .5, false).project(1, 2, b);
        assertEquals(1 - a[1], b[0], 1e-8); assertEquals(a[0], b[1], 1e-8);
    }
    @Test public void groundBehindCameraIsRejectedInsteadOfSeeingThroughCar() {
        assertEquals(0, camera(0, 0, 0, 0, false).project(0, -1, new double[2]), 0);
    }
    @Test public void outOfLensSamplesAreRejectedRatherThanCollapsedToEdges() {
        BirdEyeProjection.Camera narrow = new BirdEyeProjection.Camera(0, 0, 0, 1, 0, 0, 0, 90, .5, .5, false);
        assertEquals(0, narrow.project(5, .1, new double[2]), 0);
    }
    @Test public void shiftedLensCentreCannotSampleNeighbouringCamera() {
        BirdEyeProjection.Camera shifted = new BirdEyeProjection.Camera(0, 0, 0, 1, 0, 0, 0, 190, .95, .5, false);
        assertEquals(0, shifted.project(5, 1, new double[2]), 0);
    }
    @Test public void fourColoursLandOnFourCorrectSidesAndMaskTheCar() {
        int[] output = renderColours(1280, 5140);
        assertEquals(0xffff0000, output[5 * 60 + 30]);
        assertEquals(0xff00ff00, output[86 * 60 + 30]);
        assertEquals(0xff0000ff, output[46 * 60 + 3]);
        assertEquals(0xffffff00, output[46 * 60 + 56]);
        assertEquals(0xff000000, output[46 * 60 + 30]);
    }
    @Test public void seamBlendsCameraColoursWithoutBlackDarkening() {
        // x=1.95,y=3.55: exactly on the front/right diagonal from the body corner.
        int colour = renderColours(1280, 5140)[10 * 60 + 49];
        assertEquals(255, (colour >>> 16) & 255);
        assertTrue(((colour >>> 8) & 255) > 0);
        assertTrue(((colour >>> 8) & 255) < 255);
        assertEquals(0, colour & 255);
    }
    @Test public void outsideNarrowJoinOnlyOneCameraContributes() {
        int[] image = renderColours(1280, 5140);
        // Previously all corner overlap was mixed, doubling displaced markings well away from a join.
        assertEquals(0xffff0000, image[5 * 60 + 49]); // Front side of front/right join.
        assertEquals(0xffffff00, image[15 * 60 + 49]); // Right side of the same join.
        assertEquals(0xff00ff00, image[86 * 60 + 10]);
        assertEquals(0xff0000ff, image[76 * 60 + 10]);
    }
    @Test public void joinsAreSymmetricNormalizedAndAnchoredAtBodyCorners() {
        BirdEyeProjection.Calibration c = defaults();
        for (int sx : new int[]{-1, 1}) for (int sy : new int[]{-1, 1}) {
            double x = sx * 2, y = sy * 3.6;
            int end = sy > 0 ? 0 : 1, side = sx < 0 ? 2 : 3;
            assertEquals(.5, BirdEyeProjection.regionWeight(end, x, y, c), 1e-8);
            assertEquals(.5, BirdEyeProjection.regionWeight(side, x, y, c), 1e-8);
            for (double offset : new double[]{-.3, -.05, 0, .05, .3}) {
                double sum = 0;
                for (int role = 0; role < 4; role++) {
                    double weight = BirdEyeProjection.regionWeight(role, x, y + sy * offset, c);
                    assertTrue(weight >= 0 && weight <= 1);
                    sum += weight;
                }
                assertEquals(1, sum, 1e-8);
            }
            assertEquals(1, BirdEyeProjection.regionWeight(end, x, y + sy * .3, c), 0);
            assertEquals(1, BirdEyeProjection.regionWeight(side, x, y - sy * .3, c), 0);
        }
        BirdEyeProjection.Calibration longer = new BirdEyeProjection.Calibration(6, 2, 2, c.cameras);
        assertEquals(.5, BirdEyeProjection.regionWeight(0, 2, 4, longer), 1e-8);
    }
    @Test public void seamUsesPhysicalRoleAfterLaneAssignmentsAreSwapped() {
        BirdEyeProjection.Camera[] cameras = defaults().cameras.clone();
        BirdEyeProjection.Camera front = cameras[0], right = cameras[3];
        cameras[0] = new BirdEyeProjection.Camera(3, front.x, front.y, front.height,
                front.yaw, front.pitch, front.roll, front.fov, front.cx, front.cy, false);
        cameras[3] = new BirdEyeProjection.Camera(0, right.x, right.y, right.height,
                right.yaw, right.pitch, right.roll, right.fov, right.cx, right.cy, false);
        int[] output = renderSolidLanes(new BirdEyeProjection.Calibration(5.2, 2, 2, cameras),
                new int[]{0xffff0000, 0xff00ff00, 0xff0000ff, 0xffffff00});
        assertEquals(0xffffff00, output[5 * 60 + 49]);
        assertEquals(0xffff0000, output[15 * 60 + 49]);
    }
    @Test public void missingPreferredLensCoverageFallsBackToAnObservedCamera() {
        BirdEyeProjection.Camera[] cameras = defaults().cameras.clone();
        // Front camera turned away. The still-visible right camera must fill this point alone.
        cameras[0] = camera(0, 0, 2.6, 180, false);
        int[] output = renderSolidLanes(new BirdEyeProjection.Calibration(5.2, 2, 2, cameras),
                new int[]{0xffff0000, 0xff00ff00, 0xff0000ff, 0xffffff00});
        assertEquals(0xffffff00, output[5 * 60 + 49]);
    }
    @Test public void squashedHalFramesUseSameNormalizedLaneMapping() {
        assertArrayEquals(renderColours(1280, 5140), renderColours(1600, 900));
    }
    private int[] renderColours(int realW, int realH) {
        CompositeStreamGeometry.Plan plan = CompositeStreamGeometry.analyse("surround-test", realW, realH);
        int w = 64, h = 256;
        int[] pixels = new int[w * h], output = new int[60 * 92];
        java.util.Arrays.fill(pixels, 0xffff00ff); // Sentinel: no separators may leak into output.
        int[] colours = {0xffff0000, 0xff00ff00, 0xff0000ff, 0xffffff00};
        for (int lane = 0; lane < 4; lane++) {
            CompositeStreamGeometry.Lane l = plan.lane(lane);
            for (int y = (int) Math.ceil(l.v0 * h); y < (int) Math.ceil(l.v1 * h); y++) {
                for (int x = (int) Math.ceil(l.u0 * w); x < (int) Math.ceil(l.u1 * w); x++) pixels[y * w + x] = colours[lane];
            }
        }
        new BirdEyeProjection.Map(defaults(), plan, w, h, 60, 92).render(pixels, output);
        for (int colour : output) assertNotEquals(0xffff00ff, colour);
        return output;
    }
    private int[] renderSolidLanes(BirdEyeProjection.Calibration c, int[] colours) {
        CompositeStreamGeometry.Plan plan = CompositeStreamGeometry.analyse("surround-test", 1280, 5140);
        int[] pixels = new int[1280 * 5140], output = new int[60 * 92];
        java.util.Arrays.fill(pixels, 0xffff00ff);
        for (int i = 0; i < 4; i++) {
            CompositeStreamGeometry.Lane lane = plan.lane(i);
            for (int y = lane.y; y < lane.y + lane.height; y++)
                java.util.Arrays.fill(pixels, y * 1280, (y + 1) * 1280, colours[i]);
        }
        new BirdEyeProjection.Map(c, plan, 1280, 5140, 60, 92).render(pixels, output);
        return output;
    }
    @Test(expected = IllegalArgumentException.class) public void nanCalibrationCannotGenerateAnImage() {
        new BirdEyeProjection.Camera(0, 0, 0, Double.NaN, 0, 0, 0, 190, .5, .5, false);
    }
    @Test(expected = IllegalArgumentException.class) public void duplicateCameraCoverageIsRejected() {
        new BirdEyeProjection.Calibration(5.2, 2, 2, new BirdEyeProjection.Camera[]{
                camera(0, 0, 2.6, 0, false), camera(0, 0, -2.6, 180, false),
                camera(2, -1, 0, 270, false), camera(3, 1, 0, 90, false)});
    }
    @Test(expected = IllegalArgumentException.class) public void silentlyFailedCopyCannotBecomeOpaqueLiveVideo() {
        new BirdEyeProjection.Map(defaults(), CompositeStreamGeometry.analyse("surround-test", 1280, 5140),
                64, 256, 60, 92).render(new int[64 * 256], new int[60 * 92]);
    }
    @Test(expected = IllegalArgumentException.class) public void nonCompositeFrameIsRejected() {
        new BirdEyeProjection.Map(defaults(), CompositeStreamGeometry.analyse("other", 640, 480), 64, 64, 60, 92);
    }
    @Test(expected = IllegalArgumentException.class) public void changedFrameSizeCannotReadOutsideBuffer() {
        new BirdEyeProjection.Map(defaults(), CompositeStreamGeometry.analyse("surround-test", 1280, 5140),
                64, 256, 60, 92).render(new int[5], new int[60 * 92]);
    }
}
