package com.kooo.evcam.zeekr;

import org.junit.Test;
import static org.junit.Assert.*;

public class SidePopupStartupTest {
    @Test public void coldStartStaysHiddenUntilFramesProgress() {
        SidePopupStartup p = new SidePopupStartup();
        CameraFrameFreshness frames = new CameraFrameFreshness();
        Object source = new Object();
        p.request(true, false, 100);
        frames.observe(source, 10, 150);
        p.advance(frames.fresh(150), 150);
        assertFalse(p.revealed());
        frames.observe(source, 20, 200);
        assertTrue(p.advance(frames.fresh(200), 200));
        assertTrue(p.revealed());
    }

    @Test public void warmFeedRevealsImmediatelyWithoutAStartupDelay() {
        SidePopupStartup p = new SidePopupStartup();
        p.request(true, true, 100);
        assertTrue(p.revealed());
    }

    @Test public void waitingNoticeAppearsAtDeadlineEvenIfShowIsRepeated() {
        SidePopupStartup p = new SidePopupStartup();
        p.request(true, false, 100);
        p.request(true, false, 500);
        assertFalse(p.advance(false, 849));
        assertFalse(p.revealed());
        assertTrue(p.advance(false, 850));
        assertTrue(p.revealed());
    }

    @Test public void stalledLiveFeedRemainsVisibleForExistingStaleCover() {
        SidePopupStartup p = new SidePopupStartup();
        p.request(true, true, 100);
        assertFalse(p.advance(false, 700));
        assertTrue(p.revealed());
        // Startup presentation never extends or resets the camera freshness deadline.
        CameraFrameFreshness frames = new CameraFrameFreshness();
        Object source = new Object();
        frames.observe(source, 1, 100);
        frames.observe(source, 2, 110);
        assertFalse(frames.fresh(610));
    }

    @Test public void cancelDuringStartupCannotRevealLater() {
        SidePopupStartup p = new SidePopupStartup();
        p.request(true, false, 100);
        p.request(false, false, 200);
        assertFalse(p.advance(true, 300));
        assertFalse(p.advance(false, 1000));
        assertFalse(p.revealed());
    }

    @Test public void nextActivationGetsItsOwnDeadlineAndCanUseWarmFrames() {
        SidePopupStartup p = new SidePopupStartup();
        p.request(true, false, 100);
        p.advance(false, 850);
        p.request(false, false, 900);
        p.request(true, false, 1000);
        assertFalse(p.revealed());
        assertFalse(p.advance(false, 1749));
        p.request(false, false, 1750);
        p.request(true, true, 1800);
        assertTrue(p.revealed());
    }

    @Test public void hiddenPrewarmingDoesNotRevealWithoutAnIndicatorRequest() {
        SidePopupStartup p = new SidePopupStartup();
        p.request(false, true, 100);
        assertFalse(p.advance(true, 1000));
        assertFalse(p.revealed());
    }
}
