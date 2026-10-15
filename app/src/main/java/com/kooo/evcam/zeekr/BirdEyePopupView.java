package com.kooo.evcam.zeekr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.util.Size;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.kooo.evcam.AutoFitTextureView;
import com.kooo.evcam.R;

/** A service-owned texture and the existing CPU stitcher, with independent saved placement. */
final class BirdEyePopupView extends ViewGroup {
    final AutoFitTextureView texture;
    private final BirdEyePreview preview = new BirdEyePreview(this);
    private final BirdEyeWindowConfig config;
    private final WindowManager windows;
    private final SideViewControls controls;
    private final SideViewExpansion expansion = new SideViewExpansion();
    private final ScaleGestureDetector pinch;
    private WindowManager.LayoutParams params;
    private CompositeStreamGeometry.Plan plan;
    private float downX, downY;
    private int downWindowX, downWindowY;
    private final int slop;
    private boolean attached, multiTouch;

    BirdEyePopupView(Context context, Runnable close) {
        super(context);
        config = new BirdEyeWindowConfig(context);
        windows = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        texture = new AutoFitTextureView(context);
        addView(texture);
        controls = new SideViewControls(context, close, this::expand);
        addView(controls);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        pinch = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private float startSpan;
            private int startPercent;
            @Override public boolean onScaleBegin(ScaleGestureDetector detector) {
                startSpan = detector.getCurrentSpan(); startPercent = config.size();
                if (params != null && !expansion.expanded()) config.position(params.x, params.y);
                return !expansion.expanded() && startSpan > 0;
            }
            @Override public boolean onScale(ScaleGestureDetector detector) {
                config.setSize(Math.round(startPercent * detector.getCurrentSpan() / startSpan));
                applyLayout(); return true;
            }
        });
        setClipToOutline(true);
        setOutlineProvider(new android.view.ViewOutlineProvider() {
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                        getResources().getDimension(R.dimen.side_control_radius));
            }
        });
    }

    void show() {
        if (attached) return;
        params = new WindowManager.LayoutParams(1, 1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        place();
        windows.addView(this, params);
        attached = true;
    }

    void detach() {
        if (!attached) return;
        attached = false;
        preview.release();
        windows.removeView(this);
    }

    void source(Size size) {
        if (size == null || !CompositeStreamGeometry.looksLikeComposite(StreamLayoutTable.compositeCameraId(),
                size.getWidth(), size.getHeight())) return;
        if (plan == null || plan.frameWidth != size.getWidth() || plan.frameHeight != size.getHeight()) {
            plan = CompositeStreamGeometry.analyse(StreamLayoutTable.compositeCameraId(), size.getWidth(), size.getHeight());
            preview.start(texture, plan);
        }
    }

    void recalibrate() { preview.invalidateCalibration(); }
    android.graphics.Bitmap photo() { return preview.photo(); }
    String describe() { return preview.describeState(); }

    void applyLayout() {
        expansion.restore(); controls.setExpanded(false);
        if (attached) { place(); windows.updateViewLayout(this, params); }
        invalidate();
    }

    private void place() {
        int sw = getResources().getDisplayMetrics().widthPixels;
        int sh = getResources().getDisplayMetrics().heightPixels;
        int margin = getResources().getDimensionPixelSize(R.dimen.side_control_margin);
        int minWidth = 2 * getResources().getDimensionPixelSize(R.dimen.side_control_target) + 4 * margin;
        int width = Math.min(Math.max(1, sw - 2 * margin), Math.max(minWidth, Math.round(sh * config.size() / 150f)));
        int height = Math.min(Math.max(1, sh - 2 * margin), Math.round(width * 1.5f));
        params.width = Math.min(width, Math.round(height / 1.5f)); params.height = height;
        params.x = SidePopupSizing.clampPosition(config.x() < 0 ? sw - params.width - margin : config.x(), sw, params.width);
        params.y = SidePopupSizing.clampPosition(config.y() < 0 ? (sh - height) / 2 : config.y(), sh, height);
    }

    private void expand() {
        SideViewExpansion.Geometry next = expansion.toggle(new SideViewExpansion.Geometry(params.x, params.y,
                params.width, params.height), getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels,
                getResources().getDimensionPixelSize(R.dimen.side_control_margin));
        params.x = next.x; params.y = next.y; params.width = next.width; params.height = next.height;
        controls.setExpanded(expansion.expanded()); windows.updateViewLayout(this, params);
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) multiTouch = false;
        if (event.getPointerCount() > 1) multiTouch = true;
        pinch.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getRawX(); downY = event.getRawY(); downWindowX = params.x; downWindowY = params.y;
        }
        return event.getPointerCount() > 1 || (event.getActionMasked() == MotionEvent.ACTION_MOVE
                && Math.hypot(event.getRawX() - downX, event.getRawY() - downY) > slop);
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && event.getPointerCount() == 1
                && !multiTouch && !pinch.isInProgress() && !expansion.expanded()) {
            params.x = SidePopupSizing.clampPosition(downWindowX + Math.round(event.getRawX() - downX),
                    getResources().getDisplayMetrics().widthPixels, params.width);
            params.y = SidePopupSizing.clampPosition(downWindowY + Math.round(event.getRawY() - downY),
                    getResources().getDisplayMetrics().heightPixels, params.height);
            config.position(params.x, params.y); windows.updateViewLayout(this, params);
        }
        return true;
    }

    @Override protected void onMeasure(int width, int height) { super.onMeasure(width, height); measureChildren(width, height); }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        texture.layout(0, 0, getWidth(), getHeight()); controls.layoutIn(getWidth());
    }
    @Override protected void dispatchDraw(Canvas canvas) {
        drawChild(canvas, texture, getDrawingTime()); // Always drain the producer, including underneath a cover.
        preview.draw(canvas, getWidth(), getHeight());
        drawChild(canvas, controls, getDrawingTime());
    }
    @Override protected void onConfigurationChanged(android.content.res.Configuration c) {
        super.onConfigurationChanged(c); applyLayout();
    }
    @Override protected void onDetachedFromWindow() { preview.release(); super.onDetachedFromWindow(); }
}
