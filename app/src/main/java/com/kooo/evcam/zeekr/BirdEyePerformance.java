package com.kooo.evcam.zeekr;

/** Sustained pressure slows capture first; a one-off readback never permanently lowers resolution. */
final class BirdEyePerformance {
    enum Action { KEEP, CADENCE, LOWER }
    private int base, interval, slowFrames, goodFrames, adjustments;
    void reset(int baseInterval) { base=interval=baseInterval;slowFrames=goodFrames=adjustments=0; }
    int interval() { return interval; }
    int adjustments() { return adjustments; }
    Action observe(long readbackMs, long renderMs) {
        if (BirdEyeQuality.slow(readbackMs,renderMs)) {
            goodFrames=0;
            if (++slowFrames < 3) return Action.KEEP;
            slowFrames=0;
            if (interval >= 250) return Action.LOWER;
            interval=Math.min(250,interval+50); adjustments++;
            return Action.CADENCE;
        }
        slowFrames=0;
        if (++goodFrames >= 30 && interval > base) {
            interval=Math.max(base,interval-50);goodFrames=0;adjustments++;return Action.CADENCE;
        }
        return Action.KEEP;
    }
}
