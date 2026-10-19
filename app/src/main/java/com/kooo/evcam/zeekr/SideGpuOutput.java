package com.kooo.evcam.zeekr;

import android.view.TextureView;
import android.os.SystemClock;

/** UI-side generation/fallback guard. Never creates cameras, changes mappings or owns the producer. */
final class SideGpuOutput {
    private final boolean tap;
    private FisheyeGlPipe pipe;
    private SideGlRequest request;
    private FisheyeGlPipe waitingRaw;
    SideGpuOutput(boolean tap) { this.tap=tap; }
    boolean prepare(TextureView target,FisheyeGlPipe next,SideGpuPicture picture,int width,int height,boolean enabled) {
        if(!enabled || next==null || !next.isAlive() || !target.isAvailable()) {
            if(pipe!=null)waitingRaw=pipe;
            release();
            boolean waiting=!tap && waitingRaw!=null && waitingRaw==next && waitingRaw.isAlive()
                    && waitingRaw.needsRawFrame(timestamp(target));
            if(!waiting)waitingRaw=null;
            return waiting;
        }
        if(pipe!=next){release();pipe=next;}
        if(request==null || !request.picture.equals(picture) || request.width!=Math.min(4096,width) || request.height!=Math.min(4096,height)) {
            request=new SideGlRequest(picture,width,height);
            if(tap)pipe.setSideTap(target.getSurfaceTexture(),request);else pipe.setSide(request);
        }
        if(request.frames.failure!=null) {
            if(tap)pipe.clearSideTap(request);else pipe.clearSide(request);
            return !tap && pipe.needsRawFrame(timestamp(target));
        }
        return true;
    }
    boolean fresh(TextureView target) {
        return pipe!=null && pipe.isAlive() && request!=null && request.frames.fresh(timestamp(target),SystemClock.elapsedRealtime());
    }
    boolean failed() { return request!=null && request.frames.failure!=null; }
    boolean requested() { return request!=null && !request.frames.cancelled && request.frames.failure==null; }
    String state() { return request==null?"compatibility":request.frames.failure!=null?"fallback: "+request.frames.failure:"GPU"; }
    void release() {
        if(pipe!=null && request!=null){if(tap)pipe.clearSideTap(request);else pipe.clearSide(request);request.cancel();}
        request=null;pipe=null;
    }
    private static long timestamp(TextureView view) {
        try{return view.getSurfaceTexture()==null?0:view.getSurfaceTexture().getTimestamp();}catch(IllegalStateException gone){return 0;}
    }
}
