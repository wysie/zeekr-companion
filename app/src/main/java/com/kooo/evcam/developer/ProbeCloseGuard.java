package com.kooo.evcam.developer;

/** Keeps the exclusive lease until even a timed-out open has definitively closed. */
public final class ProbeCloseGuard {
    private int pending;
    private boolean finished, released;
    public synchronized void opening() {
        if (finished) throw new IllegalStateException("Scan finished");
        pending++;
    }
    public synchronized boolean closed() {
        if (pending == 0) throw new IllegalStateException("Unpaired close");
        pending--;
        return releaseIfReady();
    }
    public synchronized boolean finish() { finished = true; return releaseIfReady(); }
    public synchronized boolean waiting() { return pending != 0; }
    private boolean releaseIfReady() {
        if (!finished || pending != 0 || released) return false;
        released = true;
        return true;
    }
}
