package com.kooo.evcam.zeekr;

import org.junit.Test;
import static org.junit.Assert.*;

public class HudCalibrationGeometryTest {
    @Test public void squareCanCrossEitherEdgeWithoutChangingItsScale() {
        assertEquals(-200, HudCalibrationGeometry.position(-300, 1280, 200));
        assertEquals(1280, HudCalibrationGeometry.position(1400, 1280, 200));
        assertArrayEquals(new int[]{0,220,100,420}, HudCalibrationGeometry.visible(-100,220,200,1280,640));
        assertArrayEquals(new int[]{1180,220,1280,420}, HudCalibrationGeometry.visible(1180,220,200,1280,640));
    }
    @Test public void verticalAndCompletelyHiddenPositionsRemainUnambiguous() {
        assertArrayEquals(new int[]{500,0,700,100}, HudCalibrationGeometry.visible(500,-100,200,1280,640));
        assertArrayEquals(new int[]{500,540,700,640}, HudCalibrationGeometry.visible(500,540,200,1280,640));
        assertArrayEquals(new int[]{0,0,0,200}, HudCalibrationGeometry.visible(-200,0,200,1280,640));
        assertArrayEquals(new int[]{1280,640,1280,640}, HudCalibrationGeometry.visible(1280,640,200,1280,640));
    }
    @Test public void squareFitsSmallerMeasuredViewport() {
        assertEquals(120, HudCalibrationGeometry.size(200,320,120));
        assertEquals(1, HudCalibrationGeometry.size(200,0,0));
        assertEquals(1, HudCalibrationGeometry.size(-8,1280,640));
    }
}
