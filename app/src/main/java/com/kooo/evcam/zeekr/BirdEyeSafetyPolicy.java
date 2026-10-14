package com.kooo.evcam.zeekr;

/** The uncalibrated prototype is a parked calibration tool, never a driving aid. */
public final class BirdEyeSafetyPolicy {
    public enum Block { NONE, PARK_REQUIRED, FACTORY, HIDDEN, GPU, CALIBRATION, PERFORMANCE }
    private BirdEyeSafetyPolicy() {}
    public static boolean parked(String gear, Float speed) {
        return "P".equals(gear) && (speed == null
                || (!Float.isNaN(speed) && !Float.isInfinite(speed) && Math.abs(speed) <= .5f));
    }
    public static Block block(String gear, Float speed, Boolean surround, Boolean popup,
                              Boolean assist, boolean visible, boolean gpu, boolean valid, boolean slow) {
        if (!visible) return Block.HIDDEN;
        if ("R".equals(gear) || Boolean.TRUE.equals(surround) || Boolean.TRUE.equals(popup)
                || Boolean.TRUE.equals(assist)) return Block.FACTORY;
        if (!parked(gear, speed)) return Block.PARK_REQUIRED;
        if (gpu) return Block.GPU;
        if (!valid) return Block.CALIBRATION;
        if (slow) return Block.PERFORMANCE;
        return Block.NONE;
    }
}
