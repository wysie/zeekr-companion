package com.kooo.evcam.zeekr;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * HUD 不看车速门槛，主屏弹窗才看：同一份读数，门槛传 0 的（HUD）低速也出，传设置值的（主屏）等车速。
 * 服务按这个分别判两路（见 {@code SideViewPopupService.evaluate}）。
 */
public class SideViewHudDecisionTest {

    private static SideViewDecision.Input leftSignalAt(float speedKmh, int minSpeedKmh) {
        SideViewDecision.Input in = new SideViewDecision.Input();
        in.turnSignal = 1;
        in.hazard = false;
        in.gear = "D";
        in.speedKmh = speedKmh;
        in.minSpeedKmh = minSpeedKmh;
        return in;
    }

    @Test
    public void hudShowsAtLowSpeedWhileTheScreenWaits() {
        assertEquals("主屏等车速", SideViewDecision.NONE, SideViewDecision.decide(leftSignalAt(10f, 30)));
        assertEquals("HUD 低速也出", LaneCycle.LEFT, SideViewDecision.decide(leftSignalAt(10f, 0)));
    }

    @Test
    public void hudShowsStandingStill() {
        assertEquals(LaneCycle.LEFT, SideViewDecision.decide(leftSignalAt(0f, 0)));
    }

    @Test
    public void bothStillYieldToTheCarsOwnViews() {
        SideViewDecision.Input reverse = leftSignalAt(10f, 0);
        reverse.gear = "R";
        assertEquals(SideViewDecision.NONE, SideViewDecision.decide(reverse));
        SideViewDecision.Input stock360 = leftSignalAt(10f, 0);
        stock360.stock360Shown = true;
        assertEquals(SideViewDecision.NONE, SideViewDecision.decide(stock360));
    }

    @Test
    public void bothStayQuietOnHazards() {
        SideViewDecision.Input hazard = leftSignalAt(50f, 0);
        hazard.hazard = true;
        assertEquals(SideViewDecision.NONE, SideViewDecision.decide(hazard));
    }
    @Test public void coexistenceIsHudOnlyAndIncludesAllScreenFactorySignals() {
        for (int bit = 1; bit < 8; bit++) {
            SideViewDecision.Input screen = leftSignalAt(10f, 0);
            screen.stock360Shown = (bit & 1) != 0;
            screen.stockPopupShown = (bit & 2) != 0;
            screen.parkAssistOn = (bit & 4) != 0;
            assertEquals(SideViewDecision.NONE, SideViewDecision.decide(screen));
            assertEquals(SideViewDecision.NONE, SideViewDecision.decide(
                    SideViewDecision.forHud(screen, SideViewDecision.NONE, false)));
            assertEquals(LaneCycle.LEFT, SideViewDecision.decide(
                    SideViewDecision.forHud(screen, SideViewDecision.NONE, true)));
            assertEquals("HUD policy cannot mutate screen policy", SideViewDecision.NONE,
                    SideViewDecision.decide(screen));
        }
    }

    @Test public void reverseAndHazardsStillHideOptedInHudAndNeverKeepItReady() {
        SideViewDecision.Input screen = leftSignalAt(5f, 30);
        screen.stock360Shown = true;
        screen.gear = "R";
        SideViewDecision.Input hud = SideViewDecision.forHud(screen, LaneCycle.LEFT, true);
        assertEquals(SideViewDecision.NONE, SideViewDecision.decide(hud));
        org.junit.Assert.assertFalse(SideViewDecision.shouldStayReady(hud));
        screen.gear = "D"; screen.hazard = true;
        assertEquals(SideViewDecision.NONE, SideViewDecision.decide(
                SideViewDecision.forHud(screen, LaneCycle.LEFT, true)));
    }

    @Test public void disablingCoexistenceImmediatelySuppressesAnAlreadyShownHud() {
        SideViewDecision.Input screen = leftSignalAt(20f, 30);
        screen.stock360Shown = true;
        assertEquals(LaneCycle.LEFT, SideViewDecision.decide(
                SideViewDecision.forHud(screen, LaneCycle.LEFT, true)));
        assertEquals(SideViewDecision.NONE, SideViewDecision.decide(
                SideViewDecision.forHud(screen, LaneCycle.LEFT, false)));
        assertEquals("Screen speed threshold retained", 30, screen.minSpeedKmh);
    }
}
