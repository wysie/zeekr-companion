package com.kooo.evcam.zeekr;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.kooo.evcam.zeekr.BirdEyeSafetyPolicy.Block.*;

public class BirdEyeSafetyTest {
    @Test public void unknownOrNonParkGearCannotEnableThePrototype() {
        for (String gear : new String[]{null, "", "D", "N", "R", "S"}) assertFalse(BirdEyeSafetyPolicy.parked(gear, 0f));
        assertTrue(BirdEyeSafetyPolicy.parked("P", 0f));
    }
    @Test public void parkGearWithContradictoryOrInvalidSpeedIsRejected() {
        for (Float speed : new Float[]{1f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
            assertFalse(BirdEyeSafetyPolicy.parked("P", speed));
        assertTrue(BirdEyeSafetyPolicy.parked("P", null)); // Reported P is required even with no speed source.
    }
    @Test public void allFactoryTriggersSuppressEvenAParkedLiveFeed() {
        assertEquals(FACTORY, BirdEyeSafetyPolicy.block("P", 0f, true, false, false, true, false, true, false));
        assertEquals(FACTORY, BirdEyeSafetyPolicy.block("P", 0f, false, true, false, true, false, true, false));
        assertEquals(FACTORY, BirdEyeSafetyPolicy.block("P", 0f, false, false, true, true, false, true, false));
        assertEquals(FACTORY, BirdEyeSafetyPolicy.block("R", 0f, false, false, false, true, false, true, false));
    }
    @Test public void hiddenGpuInvalidAndSlowPathsCannotDisplayLivePictures() {
        assertEquals(HIDDEN, BirdEyeSafetyPolicy.block("P", 0f, false, false, false, false, false, true, false));
        assertEquals(GPU, BirdEyeSafetyPolicy.block("P", 0f, false, false, false, true, true, true, false));
        assertEquals(CALIBRATION, BirdEyeSafetyPolicy.block("P", 0f, false, false, false, true, false, false, false));
        assertEquals(PERFORMANCE, BirdEyeSafetyPolicy.block("P", 0f, false, false, false, true, false, true, true));
        assertEquals(NONE, BirdEyeSafetyPolicy.block("P", 0f, false, false, false, true, false, true, false));
    }
    @Test public void firstRetainedFrameIsNotLive() {
        CameraFrameFreshness f = new CameraFrameFreshness();
        f.observe(new Object(), 100, 10); assertFalse(f.fresh(10));
    }
    @Test public void duplicatedTimestampNeverExtendsFreshnessAndExpiresAt500ms() {
        CameraFrameFreshness f = new CameraFrameFreshness(); Object source = new Object();
        f.observe(source, 100, 0); f.observe(source, 200, 10);
        assertTrue(f.fresh(509)); f.observe(source, 200, 509);
        assertFalse(f.fresh(510)); assertEquals(10, f.changedAt());
    }
    @Test public void decreasingTimestampsNeedANewBaseline() {
        CameraFrameFreshness f = new CameraFrameFreshness(); Object source = new Object();
        f.observe(source, 100, 0); f.observe(source, 200, 10);
        f.observe(source, 150, 20); assertFalse(f.fresh(20));
        f.observe(source, 160, 30); assertTrue(f.fresh(30));
    }
    @Test public void switchingTexturesOrResettingRequiresFreshProgression() {
        CameraFrameFreshness f = new CameraFrameFreshness(); Object source = new Object(), next = new Object();
        f.observe(source, 100, 0); f.observe(source, 200, 10);
        f.observe(next, 300, 20); assertFalse(f.fresh(20));
        f.observe(next, 400, 30); assertTrue(f.fresh(30));
        f.reset(); f.observe(next, 500, 40); assertFalse(f.fresh(40));
    }
    @Test public void zeroOrMissingTimestampAndBackwardsConsumerClockAreNotFresh() {
        CameraFrameFreshness f = new CameraFrameFreshness(); Object source = new Object();
        f.observe(source, 100, 0); f.observe(source, 200, 10);
        assertFalse(f.fresh(9)); f.observe(source, 0, 20); assertFalse(f.fresh(20));
        f.observe(null, 300, 30); assertFalse(f.fresh(30));
    }
    @Test public void cameraAssignmentSwapsInsteadOfDuplicatingCoverage() {
        assertArrayEquals(new int[]{2, 1, 0, 3}, BirdEyeConfig.assignDistinctLanes(new int[]{0,1,2,3}, 0, 2));
        assertArrayEquals(new int[]{0,1,2,3}, BirdEyeConfig.assignDistinctLanes(new int[]{0,1,2,3}, 2, 2));
        for (int camera = 0; camera < 4; camera++) for (int lane = 0; lane < 4; lane++) {
            int[] result = BirdEyeConfig.assignDistinctLanes(new int[]{0,0,0,0}, camera, lane);
            assertEquals(lane, result[camera]);
            java.util.Arrays.sort(result); assertArrayEquals(new int[]{0,1,2,3}, result);
        }
    }
}
