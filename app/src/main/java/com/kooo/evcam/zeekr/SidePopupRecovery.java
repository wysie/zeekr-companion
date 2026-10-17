package com.kooo.evcam.zeekr;

/** Refusal episodes adapted from dts88 PR #41 SideViewTrigger/SideViewService.
 * A rejected output must not rejoin on every telemetry update. Visibility/geometry remain separate.
 */
final class SidePopupRecovery {
    private boolean refused, driving;
    private long graceUntil;

    void observe(long now, boolean drivingNow, boolean requested, boolean blocked) {
        if (blocked) { reset(); return; }
        if (drivingNow) graceUntil = 0;
        else if (driving) graceUntil = now + SideViewPopupService.READY_GRACE_MS;
        else if (graceUntil != 0 && now >= graceUntil) graceUntil = 0;
        driving = drivingNow;
        if (!driving && !requested && graceUntil == 0) refused = false;
    }
    void refuse() { refused = true; }
    boolean refused() { return refused; }
    long wakeAt() { return graceUntil; }
    void reset() { refused = false; driving = false; graceUntil = 0; }
}
