package com.kooo.evcam.zeekr;

/** One output generation; frame history tracks acquired buffers rather than settings redraw times. */
final class SideGlRequest {
    final SideGpuPicture picture;
    final int width, height;
    final BirdEyeGlRequest frames = new BirdEyeGlRequest();
    SideGlRequest(SideGpuPicture picture, int width, int height) {
        this.picture = picture; this.width = Math.max(1, Math.min(4096, width));
        this.height = Math.max(1, Math.min(4096, height));
    }
    void cancel() { frames.cancelled = true; frames.frame = null; }
}
