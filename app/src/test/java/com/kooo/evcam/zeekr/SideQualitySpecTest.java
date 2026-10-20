package com.kooo.evcam.zeekr;
import org.junit.Test;

public class SideQualitySpecTest {
    @Test public void native009AndPortraitPopupStayInsideReadbackBudget() {
        SideQualitySpec.requireDimensions(1280,1280,1280,1600);
        SideQualitySpec.requireDimensions(2048,2048,2048,2048);
    }
    @Test(expected=IllegalArgumentException.class) public void cannotSilentlyDownscaleOversizedNativeSide() {
        SideQualitySpec.requireDimensions(4097,1280,640,480);
    }
    @Test(expected=IllegalArgumentException.class) public void combinedMemoryBudgetBoundsBothImages() {
        SideQualitySpec.requireDimensions(2048,2048,2048,2049);
    }
    @Test(expected=IllegalArgumentException.class) public void cannotCaptureUnmeasuredPicture() {
        SideQualitySpec.requireDimensions(1280,1280,0,0);
    }
}
