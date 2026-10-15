package com.kooo.evcam.zeekr;
import org.junit.Test;
import static org.junit.Assert.*;
public class SidePopupMirrorModeTest {
    @Test public void oldSettingsKeepTheirBehaviourAndReplacementWinsConflictingKeys() {
        assertEquals(SidePopupMirrorMode.REPLACE, SidePopupMirrorMode.fromLegacy(true, false));
        assertEquals(SidePopupMirrorMode.ALONGSIDE, SidePopupMirrorMode.fromLegacy(false, false));
        assertEquals(SidePopupMirrorMode.HIDE, SidePopupMirrorMode.fromLegacy(false, true));
        assertEquals(SidePopupMirrorMode.REPLACE, SidePopupMirrorMode.fromLegacy(true, true));
    }
    @Test public void onlyAnActuallyPresentedSeparatePopupHidesTheMirror() {
        for (int mode : new int[]{SidePopupMirrorMode.REPLACE, SidePopupMirrorMode.ALONGSIDE, -1, 3}) {
            assertFalse(SidePopupMirrorMode.hideMirror(mode, true));
            assertFalse(SidePopupMirrorMode.hideMirror(mode, false));
        }
        assertFalse(SidePopupMirrorMode.hideMirror(SidePopupMirrorMode.HIDE, false));
        assertTrue(SidePopupMirrorMode.hideMirror(SidePopupMirrorMode.HIDE, true));
    }
}
