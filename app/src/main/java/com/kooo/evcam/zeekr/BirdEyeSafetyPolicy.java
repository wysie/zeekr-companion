package com.kooo.evcam.zeekr;

/** Availability of the experimental, uncalibrated view; no gear/speed eligibility gate. */
public final class BirdEyeSafetyPolicy {
    public enum Block { NONE, VEHICLE_REQUIRED, FACTORY, HIDDEN, GPU, CALIBRATION, PERFORMANCE }
    private BirdEyeSafetyPolicy() {}
    public static Block block(boolean connected, String gear, Boolean surround, Boolean popup,
                              Boolean assist, boolean visible, boolean gpu, boolean valid, boolean slow) {
        if (!visible) return Block.HIDDEN;
        if ("R".equals(gear) || Boolean.TRUE.equals(surround) || Boolean.TRUE.equals(popup)
                || Boolean.TRUE.equals(assist)) return Block.FACTORY;
        if (!connected) return Block.VEHICLE_REQUIRED;
        if (gpu) return Block.GPU;
        if (!valid) return Block.CALIBRATION;
        if (slow) return Block.PERFORMANCE;
        return Block.NONE;
    }
}
