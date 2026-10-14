package com.kooo.evcam.zeekr;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.assertEquals;

public class HudDisplayLocatorTest {
    private HudDisplayLocator.Candidate c(int id, int w, int h, String name, int flags) {
        return new HudDisplayLocator.Candidate(id, w, h, name, flags);
    }
    @Test public void findsSevenXSizeIndependentlyOfId() {
        assertEquals(5, HudDisplayLocator.find(Arrays.asList(c(0,3200,2000,"main",8),
                c(2,2560,536,"cluster",8), c(5,1440,480,"HDMI Screen",8))));
    }
    @Test public void unknownIdThreeIsNotEvidenceOfHud() {
        assertEquals(-1, HudDisplayLocator.find(Arrays.asList(c(3,1920,1080,"rear screen",8))));
    }
    @Test public void namedHudCanHaveDifferentSize() {
        assertEquals(7, HudDisplayLocator.find(Arrays.asList(c(7,1920,720,"HUD Display",8))));
    }
    @Test public void excludesMainPrivateAndNonPresentation() {
        assertEquals(-1, HudDisplayLocator.find(Arrays.asList(c(0,1440,480,"HUD",8),
                c(3,1440,480,"HUD",12), c(5,1440,480,"HUD",0))));
    }
    @Test public void neverUsesClusterEvenWithHudLabel() {
        assertEquals(-1, HudDisplayLocator.find(Arrays.asList(c(3,2560,536,"HUD",8),
                c(4,1440,480,"Instrument Cluster",8))));
    }
    @Test public void ambiguousCandidatesFallBack() {
        assertEquals(-1, HudDisplayLocator.find(Arrays.asList(c(3,1440,480,"HDMI",8),
                c(5,1440,480,"HDMI",8))));
    }
    @Test public void invalidDimensionsAndSimilarNamesAreIgnored() {
        assertEquals(-1, HudDisplayLocator.find(Arrays.asList(c(3,0,0,"HUD",8),
                c(4,1920,1080,"hudson",8))));
    }
    @Test public void owner009InventoryRequiresExplicitSelection() {
        java.util.List<HudDisplayLocator.Candidate> inventory = Arrays.asList(
                c(0,2560,1600,"Built-in Screen",0x83),
                c(2,1920,720,"HDMI Screen",0x8b),
                c(3,1280,640,"HDMI Screen",0x8b));
        assertEquals(-1, HudDisplayLocator.find(inventory));
        assertEquals(3, HudDisplayLocator.find(inventory, true));
    }
    @Test public void explicit009MappingRequiresBothIdAndDimensions() {
        assertEquals(-1, HudDisplayLocator.find(Arrays.asList(
                c(2,1280,640,"HDMI Screen",8), c(3,1920,720,"HDMI Screen",8)), true));
    }
    @Test public void explicit009MappingRejectsRestrictedDisplays() {
        assertEquals(-1, HudDisplayLocator.find(Arrays.asList(c(3,1280,640,"HDMI",12)), true));
        assertEquals(-1, HudDisplayLocator.find(Arrays.asList(c(3,1280,640,"HDMI",0)), true));
    }
    @Test public void explicit009MappingRejectsKnownOtherScreenRoles() {
        for (String name : Arrays.asList("Instrument Cluster", "Rear screen", "Passenger screen", "后排屏幕")) {
            assertEquals(-1, HudDisplayLocator.find(Arrays.asList(c(3,1280,640,name,8)), true));
        }
    }
    @Test public void explicit009MappingDoesNotSelectADifferentHud() {
        assertEquals(-1, HudDisplayLocator.find(Arrays.asList(c(5,1440,480,"HUD",8)), true));
        assertEquals(3, HudDisplayLocator.find(Arrays.asList(
                c(3,1280,640,"HDMI",8), c(5,1440,480,"HUD",8)), true));
    }
}
