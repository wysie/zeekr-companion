package com.kooo.evcam.zeekr;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.util.Size;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowInsets;
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
    private Size viewport;
    private final Runnable refreshViewport = this::refreshViewport;

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
        if (Build.VERSION.SDK_INT >= 30) {
            // Geometry is relative to this same fitted area. Never subtract bars twice or reserve hidden bars.
            params.setFitInsetsTypes(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            params.setFitInsetsIgnoringVisibility(false);
        }
        viewport = availableSize();
        place();
        windows.addView(this, params);
        attached = true;
        post(refreshViewport);
    }

    void detach() {
        if (!attached) return;
        attached = false;
        removeCallbacks(refreshViewport);
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
    String describe() {
        return preview.describeState() + " popupViewport=" + viewport
                + " popupParams=" + (params == null ? "none" : params.x + "," + params.y + " "
                + params.width + "x" + params.height) + " popupMeasured=" + getWidth() + "x" + getHeight();
    }

    private Size availableSize() {
        if (Build.VERSION.SDK_INT >= 30) {
            android.view.WindowMetrics metrics = windows.getCurrentWindowMetrics();
            Rect bounds = metrics.getBounds();
            android.graphics.Insets bars = metrics.getWindowInsets().getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            return new Size(Math.max(1, bounds.width() - bars.left - bars.right),
                    Math.max(1, bounds.height() - bars.top - bars.bottom));
        }
        // Before API 30, the attached overlay's visible frame is more useful than app resource metrics.
        Rect frame = new Rect();
        if (attached) getWindowVisibleDisplayFrame(frame);
        return frame.width() > 0 && frame.height() > 0 ? new Size(frame.width(), frame.height())
                : new Size(Math.max(1, getResources().getDisplayMetrics().widthPixels),
                        Math.max(1, getResources().getDisplayMetrics().heightPixels));
    }

    private void refreshViewport() {
        if (!attached) return;
        Size next = availableSize();
        if (next.equals(viewport)) return;
        viewport = next;
        if (expansion.expanded()) setGeometry(expansion.refit(viewport.getWidth(), viewport.getHeight(), margin()));
        else place();
        windows.updateViewLayout(this, params);
        invalidate();
    }

    private int margin() { return getResources().getDimensionPixelSize(R.dimen.side_control_margin); }

    void applyLayout() {
        expansion.restore(); controls.setExpanded(false);
        if (attached) { viewport = availableSize(); place(); windows.updateViewLayout(this, params); }
        invalidate();
    }

    private void place() {
        int sw = viewport.getWidth(), sh = viewport.getHeight();
        int margin = margin();
        int minWidth = 2 * getResources().getDimensionPixelSize(R.dimen.side_control_target) + 4 * margin;
        int width = Math.min(Math.max(1, sw - 2 * margin), Math.max(minWidth, Math.round(sh * config.size() / 150f)));
        int height = Math.min(Math.max(1, sh - 2 * margin), Math.round(width * 1.5f));
        params.width = Math.min(width, Math.round(height / 1.5f)); params.height = height;
        params.x = SidePopupSizing.clampPosition(config.x() < 0 ? sw - params.width - margin : config.x(), sw, params.width);
        params.y = SidePopupSizing.clampPosition(config.y() < 0 ? (sh - height) / 2 : config.y(), sh, height);
    }

    private void expand() {
        refreshViewport();
        if (expansion.expanded()) {
            expansion.restore(); place();
        } else {
            setGeometry(expansion.toggle(new SideViewExpansion.Geometry(params.x, params.y,
                    params.width, params.height), viewport.getWidth(), viewport.getHeight(), margin()));
        }
        controls.setExpanded(expansion.expanded()); windows.updateViewLayout(this, params);
    }

    private void setGeometry(SideViewExpansion.Geometry next) {
        params.x = next.x; params.y = next.y; params.width = next.width; params.height = next.height;
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
                    viewport.getWidth(), params.width);
            params.y = SidePopupSizing.clampPosition(downWindowY + Math.round(event.getRawY() - downY),
                    viewport.getHeight(), params.height);
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
        preview.draw(canvas, getWidth(), getHeight(), () -> drawChild(canvas, texture, getDrawingTime()));
        drawChild(canvas, controls, getDrawingTime());
    }
    @Override protected void onConfigurationChanged(android.content.res.Configuration c) {
        super.onConfigurationChanged(c); post(refreshViewport);
    }
    @Override public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        post(refreshViewport);
        return super.onApplyWindowInsets(insets);
    }
    @Override protected void onDetachedFromWindow() {
        removeCallbacks(refreshViewport); preview.release(); super.onDetachedFromWindow();
    }
}
