package com.kooo.evcam.recording;

/** One accepted foreground recovery per denial episode; a blocked request may be tried later. */
final class CameraAccessRecovery {
    private long acceptedDenial;

    boolean due(long denial, boolean waitingForCameras, boolean wanted, boolean dark, boolean foreground) {
        return denial > 0 && denial != acceptedDenial && waitingForCameras && wanted && !dark && !foreground;
    }

    void accepted(long denial) {
        acceptedDenial = denial;
    }
}
