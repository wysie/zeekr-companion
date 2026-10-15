package com.kooo.evcam.zeekr;

/** Screen-window relationship only. HUD visibility and saved mirror enablement are independent. */
public final class SidePopupMirrorMode {
    public static final int REPLACE = 0, ALONGSIDE = 1, HIDE = 2;
    private SidePopupMirrorMode() {}
    public static int fromLegacy(boolean replace, boolean hide) {
        return replace ? REPLACE : hide ? HIDE : ALONGSIDE;
    }
    public static int normalize(int mode) {
        return mode >= REPLACE && mode <= HIDE ? mode : REPLACE;
    }
    public static boolean hideMirror(int mode, boolean popupPresented) {
        return normalize(mode) == HIDE && popupPresented;
    }
}
