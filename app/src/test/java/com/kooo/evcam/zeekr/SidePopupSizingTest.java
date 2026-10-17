package com.kooo.evcam.zeekr;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SidePopupSizingTest {
    @Test public void squareKeepsTheExistingWindowDimensions() {
        for (int[] display : new int[][]{{2560,1600},{1200,1000},{480,900}}) {
            for (int percent : new int[]{25,55,90}) {
                SidePopupSizing.Size result=SidePopupSizing.dimensions(percent,"1:1",display[0],display[1],120);
                int previous=Math.min(Math.min(display[0],display[1]),
                        Math.max(120,SidePopupSizing.size(percent,display[0],display[1])));
                assertEquals(previous,result.width);assertEquals(previous,result.height);
            }
        }
    }

    @Test public void rectanglesPreserveRatioAndFitEvenOnSmallOrPortraitScreens() {
        for(String aspect:new String[]{"3:4","1:1","4:3","16:9","11:5"})
            for(int[] display:new int[][]{{2560,1600},{1280,720},{480,900},{200,100},{1,1}})
                for(int percent:new int[]{0,25,55,90,500}) {
                    SidePopupSizing.Size s=SidePopupSizing.dimensions(percent,aspect,display[0],display[1],120);
                    assertTrue(s.width>0&&s.width<=display[0]);assertTrue(s.height>0&&s.height<=display[1]);
                    assertTrue("Only pixel rounding may change aspect",Math.abs(s.width-s.height*SidePopupSizing.aspect(aspect))<=2);
                    if(display[0]>=320&&display[1]>=320) assertTrue(s.width>=120&&s.height>=120);
                }
    }

    @Test public void unknownAspectFallsBackToSquareAndRatiosAreExact() {
        assertEquals("1:1",SidePopupSizing.normalizeAspect(null));
        assertEquals("1:1",SidePopupSizing.normalizeAspect("bogus"));
        assertEquals(16.0/9,SidePopupSizing.aspect("16:9"),0);
        assertEquals(2.2,SidePopupSizing.aspect("11:5"),0);
    }
    @Test
    public void sizeRespectsHalfScreenWidthAndTheSliderLimits() {
        assertEquals(550, SidePopupSizing.size(55, 2000, 1000));
        assertEquals(600, SidePopupSizing.size(90, 1200, 1000));
        assertEquals(250, SidePopupSizing.size(0, 2000, 1000));
        assertEquals(900, SidePopupSizing.size(500, 2000, 1000));
        assertEquals(100, SidePopupSizing.size(55, 200, 100));
    }

    @Test
    public void pinchStartsFromTheActualClampedWindowSize() {
        assertEquals(60, SidePopupSizing.resizedPercent(600, 1f, 1000));
        assertEquals(30, SidePopupSizing.resizedPercent(600, 0.5f, 1000));
        assertEquals(90, SidePopupSizing.resizedPercent(600, 2f, 1000));
        assertEquals(25, SidePopupSizing.resizedPercent(600, 0.01f, 1000));
    }

    @Test
    public void invalidGestureGeometryDoesNotProduceAnInvalidPreference() {
        assertEquals(55, SidePopupSizing.resizedPercent(550, Float.NaN, 1000));
        assertEquals(55, SidePopupSizing.resizedPercent(550, Float.POSITIVE_INFINITY, 1000));
        assertEquals(55, SidePopupSizing.resizedPercent(550, 2f, 0));
    }

    @Test
    public void growingWindowKeepsSavedPositionInsideTheScreen() {
        assertEquals(1600, SidePopupSizing.clampPosition(1600, 2000, 300));
        assertEquals(1500, SidePopupSizing.clampPosition(1600, 2000, 500));
        assertEquals(500, SidePopupSizing.clampPosition(900, 1000, 500));
    }

    @Test
    public void draggingCannotLoseTheWindowBeyondScreenEdges() {
        assertEquals(0, SidePopupSizing.clampPosition(-100, 2000, 550));
        assertEquals(1450, SidePopupSizing.clampPosition(2500, 2000, 550));
        assertEquals(0, SidePopupSizing.clampPosition(100, 300, 550));
    }
}
