package com.kooo.evcam.zeekr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.kooo.evcam.AppConfig;

import org.junit.Test;

/** Fixed fork defaults and optional indicator-side placement. */
public class HudPanelPlacementTest {

    private static HudPanelPlacement.Rect defaults(boolean rightSide) {
        return HudPanelPlacement.compute(1440, 480,
                AppConfig.HUD_SIDE_DEFAULT_SIZE_PERCENT, AppConfig.HUD_SIDE_DEFAULT_SHAPE_PERCENT,
                AppConfig.HUD_SIDE_DEFAULT_LEFT_PERCENT, AppConfig.HUD_SIDE_DEFAULT_VERTICAL_PERCENT, rightSide);
    }

    @Test
    public void defaultsRetainTheForksOriginalSquarePlacement() {
        HudPanelPlacement.Rect r = defaults(false);
        assertEquals(355, r.height);
        assertEquals(355, r.width);
        assertEquals(65, r.left);
        assertEquals(63, r.top);
    }

    @Test
    public void rightSignalSitsTheSameDistanceFromTheRightEdge() {
        HudPanelPlacement.Rect left = defaults(false);
        HudPanelPlacement.Rect right = defaults(true);
        assertEquals(left.width, right.width);
        assertEquals(left.top, right.top);
        assertEquals(1440 - 355 - 65, right.left);
        assertEquals(left.left, 1440 - (right.left + right.width));
    }

    @Test
    public void fiftyIsTheMiddleFromEitherSide() {
        HudPanelPlacement.Rect left = HudPanelPlacement.compute(1440, 480, 50, 100, 50, 50, false);
        HudPanelPlacement.Rect right = HudPanelPlacement.compute(1440, 480, 50, 100, 50, 50, true);
        assertEquals(600, left.left);
        assertEquals(600, right.left);
    }

    @Test
    public void shapeWidensTheWindow() {
        HudPanelPlacement.Rect r = HudPanelPlacement.compute(1440, 480, 50, 200, 0, 0, false);
        assertEquals(240, r.height);
        assertEquals(480, r.width);
        assertEquals(0, r.left);
        assertEquals(0, r.top);
    }

    @Test
    public void positionPercentsRunFromEdgeToEdge() {
        HudPanelPlacement.Rect left = HudPanelPlacement.compute(1440, 480, 50, 100, 0, 0, false);
        HudPanelPlacement.Rect right = HudPanelPlacement.compute(1440, 480, 50, 100, 100, 100, false);
        assertEquals(0, left.left);
        assertEquals(0, left.top);
        assertEquals(1440 - 240, right.left);
        assertEquals(480 - 240, right.top);
    }

    @Test
    public void neverLargerThanTheHud() {
        HudPanelPlacement.Rect r = HudPanelPlacement.compute(1440, 480, 100, 300, 100, 100, true);
        assertEquals(480, r.height);
        assertEquals(1440, r.width);
        assertEquals(0, r.left);
        assertEquals(0, r.top);
    }

    @Test
    public void neverTinyAndPositionIsClamped() {
        HudPanelPlacement.Rect r = HudPanelPlacement.compute(1440, 480, 1, 100, 500, -20, false);
        assertEquals(HudPanelPlacement.MIN_SIZE_PX, r.height);
        assertTrue(r.left >= 0 && r.left + r.width <= 1440);
        assertEquals(0, r.top);
        HudPanelPlacement.Rect rr = HudPanelPlacement.compute(1440, 480, 1, 100, 500, -20, true);
        assertTrue(rr.left >= 0 && rr.left + rr.width <= 1440);
        assertEquals(0, rr.left);
    }

    @Test public void confirmed009DefaultsAreSmallerAndFixedOnTheRight() {
        for (int[] viewport : new int[][]{{1280,640},{960,480},{640,360}}) {
            HudPanelPlacement.Rect r = HudPanelPlacement.compute(viewport[0],viewport[1],
                    AppConfig.HUD_SIDE_009_DEFAULT_SIZE_PERCENT,100,
                    AppConfig.HUD_SIDE_009_DEFAULT_LEFT_PERCENT,50);
            assertTrue(r.left > viewport[0]/2);
            assertEquals(viewport[1]/2,r.height);
            assertEquals(r.height,r.width);
            assertTrue(r.left+r.width<=viewport[0]);assertTrue(r.top+r.height<=viewport[1]);
        }
    }

    @Test public void widePictureFitsSmallContentWithoutSqueezingItsAspectRatio() {
        HudPanelPlacement.Rect r=HudPanelPlacement.compute(640,480,100,300,100,100);
        assertEquals(640,r.width);assertEquals(213,r.height);
        assertEquals(0,r.left);assertEquals(267,r.top);
    }

    @Test public void innerCropKeepsEqualSignalWindowsAndTheirOuterEdges() {
        HudPanelPlacement.Rect left = defaults(false), right = defaults(true);
        HudPanelPlacement.Crop l = HudPanelPlacement.cropRoadEdge(left, 1440, 25, LaneCycle.LEFT, false);
        HudPanelPlacement.Crop r = HudPanelPlacement.cropRoadEdge(right, 1440, 25, LaneCycle.RIGHT, false);
        assertEquals(l.bounds.width, r.bounds.width);
        assertEquals(left.height, l.bounds.height);
        assertEquals(right.height, r.bounds.height);
        assertEquals(left.left, l.bounds.left);
        assertEquals(right.left + right.width, r.bounds.left + r.bounds.width);
        assertEquals(0, l.pictureOffsetX);
        assertEquals(right.width - r.bounds.width, r.pictureOffsetX);
        assertEquals(left.width, l.pictureWidth);
        assertEquals(right.width, r.pictureWidth);
    }

    @Test public void fixedPlacementPreservesBodyEdgeByCameraAndMirroring() {
        for(int position:new int[]{6,50,94}) {
            HudPanelPlacement.Rect full=HudPanelPlacement.compute(1280,640,50,100,position,50);
            for(int lane:new int[]{LaneCycle.LEFT,LaneCycle.RIGHT})for(boolean mirrored:new boolean[]{false,true}) {
                HudPanelPlacement.Crop c=HudPanelPlacement.cropRoadEdge(full,1280,25,lane,mirrored);
                assertEquals(240,c.bounds.width);assertEquals(320,c.bounds.height);assertEquals(320,c.pictureWidth);
                // Mirrored left loses left road edge; mirrored right loses right road edge.
                // Turning mirroring off reverses picture edges, without changing window bounds.
                int expectedOffset=(lane==LaneCycle.LEFT)==mirrored?80:0;
                assertEquals(expectedOffset,c.pictureOffsetX);
                assertEquals(full.left+(full.left*2+full.width>=1280?80:0),c.bounds.left);
                assertEquals(full.top,c.bounds.top);
            }
        }
    }

    @Test public void disabledCropRestoresFullPictureAndExtremeValuesStayUsable() {
        for (int[] viewport : new int[][]{{1280,640},{640,360},{40,30}}) {
            HudPanelPlacement.Rect full=HudPanelPlacement.compute(viewport[0],viewport[1],100,300,100,100);
            for(int percent:new int[]{-10,0,25,60,1000}) for(int lane:new int[]{LaneCycle.LEFT,LaneCycle.RIGHT}) for(boolean mirrored:new boolean[]{false,true}) {
                HudPanelPlacement.Crop c=HudPanelPlacement.cropRoadEdge(full,viewport[0],percent,lane,mirrored);
                assertTrue(c.bounds.width>=Math.min(HudPanelPlacement.MIN_SIZE_PX,full.width));
                assertTrue(c.bounds.left>=0&&c.bounds.left+c.bounds.width<=viewport[0]);
                assertEquals(full.height,c.bounds.height);
                assertEquals(full.width,c.pictureWidth);
                if(percent<=0){assertEquals(full.width,c.bounds.width);assertEquals(full.left,c.bounds.left);assertEquals(0,c.pictureOffsetX);}
            }
        }
    }
}
