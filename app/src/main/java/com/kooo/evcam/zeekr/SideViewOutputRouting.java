package com.kooo.evcam.zeekr;

/** Center-screen fallback for the Screen/HUD/Both setting (0/1/2). */
public final class SideViewOutputRouting {
    private SideViewOutputRouting() { }
    public static boolean screenWanted(int output, boolean hudAvailable) {
        return output != 1 || !hudAvailable;
    }
}
