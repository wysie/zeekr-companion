package com.kooo.evcam.recording;

import org.junit.Test;
import static org.junit.Assert.*;

public class CameraAccessRecoveryTest {
    @Test public void realDenialIsRecoveredOnceEvenWhenServiceWasEligible() {
        CameraAccessRecovery recovery = new CameraAccessRecovery();
        assertTrue(recovery.due(100, true, true, false, false));
        recovery.accepted(100);
        assertFalse("Do not repeatedly pull the UI forward", recovery.due(100, true, true, false, false));
        assertTrue("A later denial is a separate episode", recovery.due(500, true, true, false, false));
    }
    @Test public void blockedLaunchIsRetryableButStoppedDarkOrLiveRecordingDoesNotLaunch() {
        CameraAccessRecovery recovery = new CameraAccessRecovery();
        assertTrue(recovery.due(100, true, true, false, false));
        assertTrue("Factory/dedupe rejection must not consume the episode", recovery.due(100, true, true, false, false));
        assertFalse(recovery.due(100, true, false, false, false));
        assertFalse(recovery.due(100, false, true, false, false));
        assertFalse(recovery.due(100, true, true, true, false));
        assertFalse(recovery.due(0, true, true, false, false));
        assertFalse("An already foreground Activity must not silently relaunch itself",
                recovery.due(100, true, true, false, true));
    }
}
