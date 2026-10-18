package com.kooo.evcam.zeekr;
import org.junit.Test;
import static org.junit.Assert.*;
public class Observed009MappingTest {
    @Test public void roadCamerasAreNeverSilentlyCalledCabins() {
        ZeekrMultiPlan p=ZeekrMultiPlan.observed009(new String[]{"0","1","2","3"},"3",null,null,null);
        assertEquals("3",p.compositeId);assertEquals("2",p.cabin1Id);assertNull(p.cabin2Id);assertTrue(p.compositeIsReal);
    }
    @Test public void explicitAdditionalRoadChoiceIsRetainedAndDuplicatesAreExcluded() {
        ZeekrMultiPlan p=ZeekrMultiPlan.observed009(new String[]{"0","1","2","3"},"3",null,null,"0");assertEquals("0",p.cabin2Id);
        assertNull(ZeekrMultiPlan.observed009(new String[]{"2","3"},"3",null,null,"2").cabin2Id);
    }
    @Test public void TemporarilyMissingDriverIsNotReplacedByAnExteriorCamera() {
        ZeekrMultiPlan p=ZeekrMultiPlan.observed009(new String[]{"0","1","3"},"3",null,null,null);
        assertEquals("2",p.cabin1Id);assertTrue(p.missing.contains("2"));assertNull(p.cabin2Id);
    }
    @Test public void detectedSurroundWinsOverModelFallbackAndManualWinsOverDetection() {
        ZeekrMultiPlan automatic=ZeekrMultiPlan.observed009(new String[]{"0","1","2","3","4"},"4",null,null,null);
        assertEquals("4",automatic.compositeId);assertTrue(automatic.compositeIsReal);
        ZeekrMultiPlan manual=ZeekrMultiPlan.observed009(new String[]{"2","3","4"},"4","3",null,null);
        assertEquals("3",manual.compositeId);assertFalse(manual.compositeIsReal);
        assertEquals("3",ZeekrMultiPlan.observed009(new String[]{"2","3"},null,null,null,null).compositeId);
    }
    @Test public void canonicalManualAndUnassignedNeverKeepTheFallbackCompositeGeometry() {
        ZeekrMultiPlan detected=ZeekrMultiPlan.build(new String[]{"0","1","3"},"3",null,null,null);
        ZeekrMultiPlan manual=detected.withSourceChoices(new String[]{"0","1","3"},"3","5",null,null);
        assertEquals("5",manual.compositeId);assertFalse(manual.compositeIsReal);assertTrue(manual.missing.contains("5"));
        ZeekrMultiPlan unassigned=detected.withSourceChoices(new String[]{"0","1","3"},"3","",null,null);
        assertNull(unassigned.compositeId);assertFalse(unassigned.compositeIsReal);
    }
}
