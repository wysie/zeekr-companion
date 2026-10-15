package com.kooo.evcam.developer;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProbeSizesTest {
    @Test public void preserves009StripAndOtherNativeGeometries() {
        int[][] sizes = {{1920,1080}, {2560,2560}, {1280,5140}, {1280,800}};
        assertEquals(2, ProbeSizes.best(sizes, 1280, 5140));
        assertEquals(3, ProbeSizes.best(sizes, 1280, 800));
        assertEquals(2, ProbeSizes.best(sizes, 0, 0)); // largest bounded native area
    }
    @Test public void allocationLimitsRejectHugeOrInvalidSizes() {
        assertEquals(-1, ProbeSizes.best(new int[][]{{8000,8000}, {0,0}, {7000,1}, {-1,2}}, 0,0));
        assertTrue(ProbeSizes.bounded(1280,5140));
        assertFalse(ProbeSizes.bounded(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
    @Test public void aspectMatchWinsOverLargerMismatchedOutput() {
        assertEquals(1, ProbeSizes.best(new int[][]{{2560,2560}, {1280,800}, {640,400}},1280,800));
    }
}
