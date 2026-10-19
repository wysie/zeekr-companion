package com.kooo.evcam.zeekr;

/** A generation token shared by UI/worker/GL. Cancelling cannot publish an old projection. */
final class BirdEyeGlRequest {
    volatile boolean cancelled;
    volatile BirdEyeGpuMap map;
    volatile String failure;
    volatile Frame frame;
    // TextureView may acquire a frame behind the latest swap. Retain bounded metadata,
    // never pixel buffers, and accept only the timestamp actually acquired by the UI.
    private final java.util.concurrent.atomic.AtomicReferenceArray<Frame> recent = new java.util.concurrent.atomic.AtomicReferenceArray<>(8);
    private int nextFrame;
    void publish(Frame next) { recent.set(nextFrame++ & 7, next); frame = next; }
    Frame acquired(long timestamp) {
        Frame latest = frame;
        if (latest != null && latest.outputTimestamp == timestamp) return latest;
        for (int i = 0; i < recent.length(); i++) {
            Frame candidate = recent.get(i);
            if (candidate != null && candidate.outputTimestamp == timestamp) return candidate;
        }
        return null;
    }
    static final class Frame {
        final long outputTimestamp, sourceAt, renderMs;
        Frame(long outputTimestamp, long sourceAt, long renderMs) {
            this.outputTimestamp = outputTimestamp; this.sourceAt = sourceAt; this.renderMs = renderMs;
        }
    }
    boolean fresh(long outputTimestamp, long now) {
        Frame f = acquired(outputTimestamp);
        return !cancelled && failure == null && f != null && f.outputTimestamp == outputTimestamp
                && CameraFrameFreshness.freshAt(f.sourceAt, now);
    }
}
