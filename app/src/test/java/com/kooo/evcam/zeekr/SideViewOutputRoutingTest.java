package com.kooo.evcam.zeekr;
import org.junit.Test;
import static org.junit.Assert.*;
public class SideViewOutputRoutingTest {
    @Test public void screenAndBothAlwaysKeepTheScreen() {
        for (int mode : new int[]{0,2}) {
            assertTrue(SideViewOutputRouting.screenWanted(mode,true));
            assertTrue(SideViewOutputRouting.screenWanted(mode,false));
        }
    }
    @Test public void hudOnlyFallsBackWhenUnavailable() {
        assertFalse(SideViewOutputRouting.screenWanted(1,true));
        assertTrue(SideViewOutputRouting.screenWanted(1,false));
    }
}
