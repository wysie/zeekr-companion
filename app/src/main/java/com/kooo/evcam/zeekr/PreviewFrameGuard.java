package com.kooo.evcam.zeekr;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.os.SystemClock;
import android.view.View;
import com.kooo.evcam.R;

/** Draw the native texture first, then cover missing/stalled pictures with opaque black. */
final class PreviewFrameGuard {
    private final View owner;
    private final CameraFrameFreshness frames = new CameraFrameFreshness();
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String waiting;
    private boolean attached;
    private String sourceKey, announcement;
    private long waitingSince;
    private android.text.StaticLayout messageLayout;
    private String messageText;
    private int messageWidth;
    void setSourceKey(String key) { sourceKey=key; }
    private com.kooo.evcam.camera.CameraUiState sourceState() {
        com.kooo.evcam.camera.MultiCameraManager manager=com.kooo.evcam.camera.CameraManagerHolder.getInstance().getCameraManager();
        return manager==null||sourceKey==null?com.kooo.evcam.camera.CameraUiState.STARTING:manager.sourceUiState(sourceKey);
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!attached) return;
            if (!owner.isShown()) frames.reset();
            else if (!frames.fresh(SystemClock.elapsedRealtime())) owner.invalidate();
            owner.postDelayed(this, 100);
        }
    };
    PreviewFrameGuard(View owner) {
        this.owner = owner;
        waiting = owner.getContext().getString(R.string.side_view_waiting_live);
        text.setColor(Color.WHITE);
        // StaticLayout centres each line. Paint must retain a left origin or it
        // shifts the already-centred glyphs left and clips their beginning.
        text.setTextAlign(Paint.Align.LEFT);
        text.setTextSize(16 * owner.getResources().getDisplayMetrics().scaledDensity);
    }
    void attach() { attached = true; reset(); owner.removeCallbacks(tick); owner.post(tick); }
    void detach() { attached = false; owner.removeCallbacks(tick); reset(); }
    void reset() { frames.reset(); waitingSince=SystemClock.elapsedRealtime(); owner.invalidate(); }
    void observe(SurfaceTexture source) {
        if (!attached || !owner.isShown()) { reset(); return; }
        try { frames.observe(source, source.getTimestamp(), SystemClock.elapsedRealtime()); }
        catch (RuntimeException unavailable) { frames.reset(); }
        owner.invalidate();
    }
    void cover(Canvas canvas, boolean force) {
        long now=SystemClock.elapsedRealtime();
        if (!force && frames.fresh(now)) {
            waitingSince=0; announce(owner.getContext().getString(R.string.camera_preview_live)); return;
        }
        if(waitingSince==0)waitingSince=now;
        com.kooo.evcam.camera.CameraUiState state=sourceState();
        boolean prolonged=now-waitingSince>=10000;
        int resource=state==com.kooo.evcam.camera.CameraUiState.STREAMING ? R.string.camera_preview_waiting
          : (state==com.kooo.evcam.camera.CameraUiState.STARTING||state==com.kooo.evcam.camera.CameraUiState.WAITING)
             ? (prolonged?R.string.camera_state_waiting:R.string.camera_state_starting):state.text;
        String semantic=owner.getContext().getString(resource);
        if(prolonged)semantic+="\n"+owner.getContext().getString(R.string.camera_waiting_status_link);
        announce(semantic);
        String shown=semantic+(prolonged?"\n"+owner.getContext().getString(R.string.camera_waiting_elapsed,(now-waitingSince)/1000):"");
        canvas.drawColor(Color.BLACK);
        int width=Math.max(1,owner.getWidth()-24);
        if(messageLayout==null||!shown.equals(messageText)||messageWidth!=width){
            android.text.TextPaint paint=new android.text.TextPaint(text);
            messageLayout=android.text.StaticLayout.Builder.obtain(shown,0,shown.length(),paint,width)
              .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER).setIncludePad(false).build();
            messageText=shown;messageWidth=width;
        }
        canvas.save();canvas.translate(12,Math.max(0,(owner.getHeight()-messageLayout.getHeight())/2f));
        messageLayout.draw(canvas);canvas.restore();
    }
    private void announce(String value) {
        if(value.equals(announcement))return;announcement=value;
        // Announce state transitions, never every elapsed second. Preserve child controls in the tree.
        owner.setContentDescription(value);owner.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        androidx.core.view.ViewCompat.setStateDescription(owner,value);
    }
}
