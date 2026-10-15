package com.kooo.evcam.zeekr;

/** Deadline pacing without a queue of old frames or a second fixed polling delay. */
final class BirdEyeCadence {
    static final long WATCHDOG_MS = 100;
    static long delay(long now, long nextCaptureAt, boolean busy, boolean allowed) {
        if (busy || !allowed) return WATCHDOG_MS;
        return Math.max(16, Math.min(WATCHDOG_MS, nextCaptureAt - now));
    }
}
