package com.kooo.evcam.zeekr;

import org.junit.Test;
import static com.kooo.evcam.zeekr.VehicleIdentity.Model.*;
import static org.junit.Assert.*;

public class VehicleIdentityTest {
    @Test public void manual009WorksEvenWhenFirmwareClaimsToBeAPixel() {
        assertEquals(ZEEKR_009, VehicleIdentity.resolve("009", "Pixel 3a", "sargo"));
    }
    @Test public void manual7xOverridesAnOtherwiseNamed009() {
        assertEquals(ZEEKR_7X, VehicleIdentity.resolve("7x", "Zeekr 009"));
    }
    @Test public void exactAndNamedModelsCanBeRecognized() {
        assertEquals(ZEEKR_009, VehicleIdentity.resolve("auto", "009"));
        assertEquals(ZEEKR_009, VehicleIdentity.resolve("auto", "ZEEKR_009", "Pixel 3a"));
        assertEquals(ZEEKR_7X, VehicleIdentity.resolve("auto", "Zeekr 7x RHD"));
        assertEquals(ZEEKR_7X, VehicleIdentity.resolve("auto", "7X"));
        assertEquals(ZEEKR_009, VehicleIdentity.resolve("auto", "极氪009"));
    }
    @Test public void spoofedOrGenericDeviceNamesRemainUnknown() {
        assertEquals(UNKNOWN, VehicleIdentity.resolve("auto", "Pixel 3a", "sargo", "zeekr_dhu_sa8295_ovs"));
        assertEquals(UNKNOWN, VehicleIdentity.resolve("auto", "Zeekr", "Google"));
        assertEquals(UNKNOWN, VehicleIdentity.resolve("auto", (String[]) null));
    }
    @Test public void incidentalNumbersAndPartialModelCodesAreNotEvidence() {
        for (String value : new String[]{"build2026009", "009A", "sa8295_009", "7x_mcu", "notzeekr009"})
            assertEquals(value, UNKNOWN, VehicleIdentity.resolve("auto", value));
    }
    @Test public void conflictingExplicitIdentitiesFallBackToGeneric() {
        assertEquals(UNKNOWN, VehicleIdentity.resolve("auto", "Zeekr 009", "Zeekr 7X"));
        assertEquals(UNKNOWN, VehicleIdentity.resolve("auto", "Zeekr009 / Zeekr7x"));
    }
    @Test public void unknownSelectionUsesAutoAndMissingFieldsAreHarmless() {
        assertEquals(ZEEKR_009, VehicleIdentity.resolve("invalid", null, " Zeekr-009 ", ""));
    }
}
