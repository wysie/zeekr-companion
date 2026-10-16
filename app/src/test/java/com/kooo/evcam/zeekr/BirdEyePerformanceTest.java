package com.kooo.evcam.zeekr;
import org.junit.Test;
import static org.junit.Assert.*;
public class BirdEyePerformanceTest {
    @Test public void isolatedReadbackSpikesKeepBalancedDetail() {
        BirdEyePerformance p=new BirdEyePerformance();p.reset(100);
        for(int i=0;i<20;i++){assertEquals(BirdEyePerformance.Action.KEEP,p.observe(45,18));p.observe(5,18);}
        assertEquals(100,p.interval());
    }
    @Test public void sustainedCostSlowsBeforeAnyResolutionFallback() {
        BirdEyePerformance p=new BirdEyePerformance();p.reset(100);
        for(int i=0;i<9;i++)assertNotEquals(BirdEyePerformance.Action.LOWER,p.observe(45,18));
        assertEquals(250,p.interval());
        p.observe(45,18);p.observe(45,18);assertEquals(BirdEyePerformance.Action.LOWER,p.observe(45,18));
    }
    @Test public void longStableRunRestoresCadenceAndResetClearsPressure() {
        BirdEyePerformance p=new BirdEyePerformance();p.reset(100);for(int i=0;i<3;i++)p.observe(60,20);
        assertEquals(150,p.interval());for(int i=0;i<30;i++)p.observe(5,18);assertEquals(100,p.interval());
        p.reset(200);assertEquals(0,p.adjustments());assertEquals(200,p.interval());
    }
}
