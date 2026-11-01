package com.kooo.evcam.zeekr;

import org.junit.Test;
import static org.junit.Assert.*;

public class SideGlRequestTest {
    @Test public void framingEditsRetainAcquiredFrameWithoutRenewingItsSourceAge() {
        BirdEyeGlRequest output = new BirdEyeGlRequest();
        SideGlRequest first = new SideGlRequest(null, 900, 340, output);
        first.publish(new BirdEyeGlRequest.Frame(10, 100, 1));
        for (int i = 0; i < 100; i++) {
            first.cancel();
            first = new SideGlRequest(null, 900 + i, 340, output);
            assertTrue(output.fresh(10, 150));
            assertFalse("No invented frame in the replacement generation", first.frames.fresh(10, 150));
        }
        assertFalse("Dragging must never keep a stopped camera fresh", output.fresh(10, 600));
        first.publish(new BirdEyeGlRequest.Frame(20, 610, 1));
        assertTrue(output.fresh(20, 620));
        assertFalse(output.fresh(19, 620));
    }
    @Test public void sourceAndOutputChangesUseIndependentHistories() {
        SideGlRequest old = new SideGlRequest(null, 900, 340);
        old.publish(new BirdEyeGlRequest.Frame(10, 100, 1));
        SideGlRequest rebound = new SideGlRequest(null, 900, 340);
        assertFalse(rebound.outputFrames.fresh(10, 150));
        assertTrue(old.outputFrames.fresh(10, 150));
    }
}
