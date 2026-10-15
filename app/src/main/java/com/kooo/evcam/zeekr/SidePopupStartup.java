package com.kooo.evcam.zeekr;

/** Keep ordinary startup invisible; do not hide an already presented, stalled feed. */
final class SidePopupStartup {
    static final long WAIT_NOTICE_MS = 750;
    private boolean requested;
    private boolean revealed;
    private long requestedAt;

    void request(boolean show, boolean live, long now) {
        if (!show) {
            requested = false;
            revealed = false;
        } else {
            if (!requested) requestedAt = now;
            requested = true;
            advance(live, now);
        }
    }

    /** True only when the window needs a visibility update. */
    boolean advance(boolean live, long now) {
        if (!requested || revealed) return false;
        if (live || (now >= requestedAt && now - requestedAt >= WAIT_NOTICE_MS)) {
            revealed = true;
            return true;
        }
        return false;
    }

    boolean revealed() { return requested && revealed; }
}
