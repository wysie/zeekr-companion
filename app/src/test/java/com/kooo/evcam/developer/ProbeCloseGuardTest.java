package com.kooo.evcam.developer;

import org.junit.Test;
import static org.junit.Assert.*;

public class ProbeCloseGuardTest {
    @Test public void pendingOpenRetainsLeaseAfterTimeoutOrCancellationUntilLateClose() {
        ProbeCloseGuard guard = new ProbeCloseGuard();
        guard.opening();
        assertFalse(guard.finish());
        assertTrue(guard.waiting());
        assertTrue(guard.closed());
        assertFalse(guard.waiting());
        assertFalse(guard.finish());
    }
    @Test public void sequentialCamerasDoNotReleaseLeaseBetweenIds() {
        ProbeCloseGuard guard = new ProbeCloseGuard();
        for (int i = 0; i < 4; i++) {
            guard.opening(); assertFalse(guard.closed());
        }
        assertTrue(guard.finish()); assertFalse(guard.finish());
    }
    @Test public void emptyOrCancelledBeforeOpenReleasesOnce() {
        ProbeCloseGuard guard = new ProbeCloseGuard();
        assertTrue(guard.finish()); assertFalse(guard.finish());
    }
}
