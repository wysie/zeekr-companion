package com.kooo.evcam.zeekr;

import org.junit.Test;
import static org.junit.Assert.*;

public class BirdEyeQualityTest {
    @Test public void fullUsesTheOwnersNativeComposite() {
        BirdEyeQuality.Size s = BirdEyeQuality.resolve(BirdEyeQuality.FULL, 1280, 5140);
        assertEquals(1280, s.sourceWidth); assertEquals(5140, s.sourceHeight);
        assertEquals(960, s.outputWidth); assertEquals(1440, s.outputHeight);
        assertEquals(200, s.intervalMs);
    }
    @Test public void balancedIsSharperAndPreservesCompositeAspect() {
        BirdEyeQuality.Size s = BirdEyeQuality.resolve(BirdEyeQuality.BALANCED, 1280, 5140);
        assertEquals(768, s.sourceWidth); assertEquals(3084, s.sourceHeight);
        assertEquals(640, s.outputWidth); assertEquals(960, s.outputHeight);
        assertEquals(100, s.intervalMs);
        assertEquals(1280.0 / 5140, (double)s.sourceWidth / s.sourceHeight, .001);
    }
    @Test public void lowRetainsTheSmallOutputWithCorrectSourceAspect() {
        BirdEyeQuality.Size s = BirdEyeQuality.resolve(BirdEyeQuality.LOW, 1280, 5140);
        assertEquals(256, s.sourceWidth); assertEquals(1028, s.sourceHeight);
        assertEquals(320, s.outputWidth); assertEquals(480, s.outputHeight);
        assertEquals(67, s.intervalMs);
    }
    @Test public void nativeSmallFeedIsNotUpscaledDuringReadback() {
        BirdEyeQuality.Size s = BirdEyeQuality.resolve(BirdEyeQuality.BALANCED, 128, 514);
        assertEquals(128, s.sourceWidth); assertEquals(514, s.sourceHeight);
    }
    @Test public void dimensionsAreBoundedEvenForLargeOrThinFeeds() {
        for (int[] dimensions : new int[][]{{8000,8000},{1,1000000000},{1000000000,1000000000}}) {
            BirdEyeQuality.Size s = BirdEyeQuality.resolve(BirdEyeQuality.FULL, dimensions[0], dimensions[1]);
            assertTrue((long)s.sourceWidth * s.sourceHeight <= 8_000_000);
            assertTrue(s.sourceWidth <= 8192 && s.sourceHeight <= 8192);
        }
    }
    @Test public void fallbackOnlyMovesDownAndTimingBoundsAreInclusive() {
        assertEquals(BirdEyeQuality.BALANCED, BirdEyeQuality.lower(BirdEyeQuality.FULL));
        assertEquals(BirdEyeQuality.LOW, BirdEyeQuality.lower(BirdEyeQuality.BALANCED));
        assertEquals(BirdEyeQuality.LOW, BirdEyeQuality.lower(BirdEyeQuality.LOW));
        assertFalse(BirdEyeQuality.slow(40,150));
        assertTrue(BirdEyeQuality.slow(41,0)); assertTrue(BirdEyeQuality.slow(0,151));
    }
    @Test public void unknownSelectionUsesBalanced() {
        assertEquals(BirdEyeQuality.BALANCED, BirdEyeQuality.normalize(-1));
        assertEquals(BirdEyeQuality.BALANCED, BirdEyeQuality.normalize(99));
    }
    @Test(expected=IllegalArgumentException.class) public void missingGeometryCannotAllocateFrames() {
        BirdEyeQuality.resolve(BirdEyeQuality.FULL, 0, 5140);
    }
}
