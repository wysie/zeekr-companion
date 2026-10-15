package com.kooo.evcam.profile;

/** Selected channel identities, independent of how many channels happen to be enabled. */
public final class ProfileChannels {
    public final boolean surround;
    public final boolean cabinFront;
    public final boolean cabinRear;
    public final int count;

    private ProfileChannels(boolean surround, boolean front, boolean rear) {
        this.surround = surround;
        this.cabinFront = front;
        this.cabinRear = rear;
        count = (surround ? 1 : 0) + (front ? 1 : 0) + (rear ? 1 : 0);
    }

    public static ProfileChannels of(Profile profile) {
        return new ProfileChannels(enabled(profile, CameraProfile.ROLE_COMPOSITE),
                enabled(profile, CameraProfile.ROLE_CABIN_1), enabled(profile, CameraProfile.ROLE_CABIN_2));
    }

    private static boolean enabled(Profile profile, String role) {
        CameraProfile camera = profile.camera(role);
        return camera != null && camera.enabled;
    }

    public boolean cabinLayout() { return cabinFront || cabinRear; }

    /** The cabin layout contains three texture slots even when only one is selected. */
    public int layoutSlots() { return cabinLayout() ? 3 : 1; }

    public boolean needsTexture(String cameraKey) {
        return "front".equals(cameraKey) ? surround
                : "back".equals(cameraKey) ? cabinFront
                : "left".equals(cameraKey) && cabinRear;
    }

    public String key() {
        return (surround ? "1" : "0") + (cabinFront ? "1" : "0") + (cabinRear ? "1" : "0");
    }
}
