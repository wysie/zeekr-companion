package com.kooo.evcam.zeekr;

/** One output generation; frame history tracks acquired buffers rather than settings redraw times. */
final class SideGlRequest {
    final SideGpuPicture picture;
    final int width, height;
    final BirdEyeGlRequest frames = new BirdEyeGlRequest();
    final BirdEyeGlRequest outputFrames;
    SideGlRequest(SideGpuPicture picture, int width, int height) {
        this(picture, width, height, new BirdEyeGlRequest());
    }
    SideGlRequest(SideGpuPicture picture, int width, int height, BirdEyeGlRequest outputFrames) {
        this.picture = picture; this.width = Math.max(1, Math.min(4096, width));
        this.height = Math.max(1, Math.min(4096, height));
        this.outputFrames = outputFrames;
    }
    void publish(BirdEyeGlRequest.Frame frame) { frames.publish(frame); outputFrames.publish(frame); }
    void cancel() { frames.cancelled = true; frames.frame = null; }
}
