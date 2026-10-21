package com.kooo.evcam.zeekr;

import org.junit.Test;
import static org.junit.Assert.*;

public class HudSafeAreaGeometryTest {
    @Test public void marginInsetsEachObservedEdgeUsingTheViewportWidth() {
        assertArrayEquals(new int[]{126,1154}, HudPanelPlacement.safeInterval(1280,100,1180,2));
        assertArrayEquals(new int[]{100,1180}, HudPanelPlacement.safeInterval(1280,100,1180,0));
    }
    @Test public void horizontalFitMovesBothSidesWithoutChangingBodyTrimOrSizeWhenTheyFit() {
        for (int lane : new int[]{LaneCycle.LEFT,LaneCycle.RIGHT}) for (boolean mirrored : new boolean[]{false,true}) {
            HudPanelPlacement.Rect full = HudPanelPlacement.compute(1280,640,50,100,0,50,lane==LaneCycle.RIGHT);
            HudPanelPlacement.Crop before = HudPanelPlacement.cropRoadEdge(full,1280,25,lane,mirrored);
            HudPanelPlacement.Crop after = HudPanelPlacement.fitHorizontal(before,150,1130,640,50);
            assertEquals(before.bounds.width,after.bounds.width); assertEquals(before.bounds.height,after.bounds.height);
            assertEquals(before.pictureWidth,after.pictureWidth);assertEquals(before.pictureOffsetX,after.pictureOffsetX);
            assertEquals(lane==LaneCycle.LEFT?150:1130-after.bounds.width,after.bounds.left);
        }
    }
    @Test public void narrowIntervalScalesFullPictureAndCropTogetherInsteadOfTrimmingMoreRoadOrBody() {
        for (int lane : new int[]{LaneCycle.LEFT,LaneCycle.RIGHT}) for (boolean mirrored : new boolean[]{false,true}) {
            HudPanelPlacement.Rect full=HudPanelPlacement.compute(1280,640,100,300,94,60,false);
            HudPanelPlacement.Crop before=HudPanelPlacement.cropRoadEdge(full,1280,25,lane,mirrored);
            HudPanelPlacement.Crop after=HudPanelPlacement.fitHorizontal(before,400,880,640,60);
            assertEquals(480,after.bounds.width);assertEquals(400,after.bounds.left);
            assertEquals(before.bounds.width/(float)before.bounds.height,after.bounds.width/(float)after.bounds.height,.02f);
            assertEquals(before.pictureWidth/(float)before.bounds.width,after.pictureWidth/(float)after.bounds.width,.003f);
            assertEquals(before.pictureOffsetX/(float)before.pictureWidth,after.pictureOffsetX/(float)after.pictureWidth,.003f);
            assertTrue(after.bounds.top>=0&&after.bounds.top+after.bounds.height<=640);
        }
    }
    @Test public void alreadyInsidePanelIsUnchanged() {
        HudPanelPlacement.Crop old=HudPanelPlacement.cropRoadEdge(new HudPanelPlacement.Rect(500,100,200,200),1280,0,LaneCycle.LEFT,true);
        HudPanelPlacement.Crop fitted=HudPanelPlacement.fitHorizontal(old,100,1180,640,90);
        assertEquals(500,fitted.bounds.left);assertEquals(100,fitted.bounds.top);assertEquals(200,fitted.bounds.width);
    }
    @Test(expected=IllegalArgumentException.class) public void reversedEdgesAreRefused() { HudPanelPlacement.safeInterval(1280,900,300,2); }
    @Test(expected=IllegalArgumentException.class) public void marginCannotConsumeTheUsableInterval() { HudPanelPlacement.safeInterval(1280,100,200,2); }
    @Test(expected=IllegalArgumentException.class) public void outsideLogicalViewportIsRefused() { HudPanelPlacement.safeInterval(1280,-10,1200,0); }
    @Test(expected=IllegalArgumentException.class) public void invalidImportedMarginIsRefused() { HudPanelPlacement.safeInterval(1280,100,1180,6); }
}
