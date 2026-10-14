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
        int colour = renderColours(1280, 5140)[5 * 60 + 56];
        assertEquals(255, (colour >>> 16) & 255);
        assertTrue(((colour >>> 8) & 255) > 0);
        assertTrue(((colour >>> 8) & 255) < 255);
        assertEquals(0, colour & 255);
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
