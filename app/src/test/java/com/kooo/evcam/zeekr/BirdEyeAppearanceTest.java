package com.kooo.evcam.zeekr;

import org.junit.Test;
import static org.junit.Assert.*;

public class BirdEyeAppearanceTest {
    @Test public void autoOnlyUsesPositiveIdentityOrExisting009Evidence() {
        assertEquals("outline", BirdEyeAppearance.resolveModel("auto",false,false,
                VehicleIdentity.resolve("auto","Pixel 3a","sargo")));
        assertEquals("7x", BirdEyeAppearance.resolveModel("auto",false,false,
                VehicleIdentity.resolve("auto","ZEEKR 7X")));
        assertEquals("009", BirdEyeAppearance.resolveModel("auto",false,false,VehicleIdentity.Model.ZEEKR_009));
        assertEquals("009", BirdEyeAppearance.resolveModel("auto",true,false,VehicleIdentity.Model.UNKNOWN));
        assertEquals("009", BirdEyeAppearance.resolveModel("auto",false,true,VehicleIdentity.Model.UNKNOWN));
        assertEquals("009", BirdEyeAppearance.resolveModel("auto",false,true,VehicleIdentity.Model.ZEEKR_7X));
        assertEquals("outline", BirdEyeAppearance.resolveModel("unknown-selection",false,false,VehicleIdentity.Model.UNKNOWN));
    }
    @Test public void explicitIllustrationAlwaysWinsWithoutSelectingCameraOrProjection() {
        for (String model : new String[]{"009","7x","outline"})
            assertEquals(model, BirdEyeAppearance.resolveModel(model,true,true,VehicleIdentity.Model.ZEEKR_009));
    }
}
