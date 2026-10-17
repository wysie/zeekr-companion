package com.kooo.evcam.zeekr;

import org.junit.Test;
import static org.junit.Assert.*;

public class SidePopupRecoveryTest {
    @Test public void rejectionDoesNotRearmOnEverySignalOrSpeedUpdateInDrive() {
        SidePopupRecovery r=new SidePopupRecovery();r.observe(0,true,true,false);r.refuse();
        for(int t=1;t<100;t++){r.observe(t,true,t%2==0,false);assertTrue(r.refused());}
    }
    @Test public void factoryOrScreenInterruptionStartsANewEpisode() {
        SidePopupRecovery r=new SidePopupRecovery();r.observe(0,true,true,false);r.refuse();
        r.observe(1,true,true,true);assertFalse(r.refused());assertEquals(0,r.wakeAt());
        r.observe(2,true,true,false);assertFalse(r.refused());
    }
    @Test public void leavingDriveMustCompleteGraceAndReturningDoesNotRearm() {
        SidePopupRecovery r=new SidePopupRecovery();r.observe(100,true,false,false);r.refuse();
        r.observe(1000,false,false,false);assertEquals(31000,r.wakeAt());assertTrue(r.refused());
        r.observe(2000,true,false,false);assertEquals(0,r.wakeAt());assertTrue(r.refused());
        r.observe(3000,false,false,false);r.observe(32999,false,false,false);assertTrue(r.refused());
        r.observe(33000,false,false,false);assertFalse(r.refused());assertEquals(0,r.wakeAt());
    }
    @Test public void signalOutsideDriveKeepsRefusalUntilThatRequestEnds() {
        SidePopupRecovery r=new SidePopupRecovery();r.observe(0,false,true,false);r.refuse();
        r.observe(10000,false,true,false);assertTrue(r.refused());
        r.observe(10001,false,false,false);assertFalse(r.refused());
    }
    @Test public void activeRequestKeepsRefusalPastDriveGraceUntilItEnds() {
        SidePopupRecovery r=new SidePopupRecovery();r.observe(0,true,true,false);r.refuse();
        r.observe(1,false,true,false);r.observe(30001,false,true,false);assertTrue(r.refused());
        r.observe(30002,false,false,false);assertFalse(r.refused());
    }
}
