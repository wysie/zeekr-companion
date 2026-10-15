package com.kooo.evcam.zeekr;

import org.junit.Test;
import static org.junit.Assert.*;

public class BirdEyeCadenceTest {
    @Test public void balancedWaitsOnlyUntilNextDeadlineAfterReadbackAndRendering() {
        // Owner diagnostics: 4 ms readback + 18 ms render. No extra 100 ms poll.
        assertEquals(78, BirdEyeCadence.delay(1022, 1100, false, true));
    }
    @Test public void lowCanScheduleFasterThanTheSafetyWatchdog() {
        assertEquals(45, BirdEyeCadence.delay(1022, 1067, false, true));
    }
    @Test public void fullRetainsWatchdogChecksWhileWaitingForItsDeadline() {
        assertEquals(100, BirdEyeCadence.delay(1022, 1200, false, true));
        assertEquals(78, BirdEyeCadence.delay(1122, 1200, false, true));
    }
    @Test public void busyOrBlockedWorkNeverCreatesAFastPollingLoop() {
        assertEquals(100, BirdEyeCadence.delay(1200, 1100, true, true));
        assertEquals(100, BirdEyeCadence.delay(1200, 1100, false, false));
    }
    @Test public void missedDeadlineHasBoundedBackpressureInsteadOfImmediateSpin() {
        assertEquals(16, BirdEyeCadence.delay(1200, 1100, false, true));
        assertEquals(16, BirdEyeCadence.delay(1100, 1100, false, true));
    }
}
