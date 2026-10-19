package com.kooo.evcam.zeekr;

import com.kooo.evcam.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.view.View;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.os.Build;
import android.util.Size;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.kooo.evcam.AppConfig;
import com.kooo.evcam.AppLog;
import com.kooo.evcam.AutoFitTextureView;

/**
 * 打转向灯弹出的侧视窗：只显示左或右那一路，双指缩放，单指拖动。
 *
 * <p>画法和超级后视镜一样（见 {@link RearViewMirrorView} 的类说明）：子视图是普通 TextureView，
 * 本容器在 {@code dispatchDraw} 里按 {@link RearViewGeometry#combinedSourceRect} 把那一路放大重画，
 * 不新建 GL 管线；「拉直」开着时走同一套分片反投影（{@link FisheyeMesh}），
 * 只是虚拟相机先转了个角度（{@link SideViewProjection}）。侧视不镜像。</p>
 *
 * <h3>三种状态</h3>
 *
 * <ul>
 *   <li>没挂：没有窗口，不占相机。</li>
 *   <li><b>备着</b>（{@link #attachHidden}）：窗口挂着但完全透明（窗口 alpha 0），相机照常往里推帧 ——
 *       打灯时只要把 alpha 拨回 1，不用重建会话。<b>不用 INVISIBLE</b>：TextureView 不画就不取帧，
 *       共享流上一个不取帧的消费者可能把整条流（包括录像）拖住。</li>
 *   <li>显示：alpha 1，在对应那一边。</li>
 * </ul>
 *
 * <p>两侧共用同一个保存的位置。可以单指拖动、双指缩放；大小和上下位置也能在设置里调。
 * 显示时接触摸，隐藏备着时点击穿过去。</p>
 */
public class SideViewPopupView extends ViewGroup {

    private static final String TAG = "SideViewPopup";

    private final WindowManager windowManager;
    protected final AppConfig appConfig;
    private final SideTurnArrow sideArrow;
    private Integer indicatorDisplay;
    private final AutoFitTextureView textureView;
    private final ScaleGestureDetector resizeGesture;
    private final CameraLabel cameraLabel;
    private final SideViewControls controls;
    private final SideViewExpansion expansion = new SideViewExpansion();
    private final Matrix drawMatrix = new Matrix();
    private final CameraFrameFreshness frameFreshness = new CameraFrameFreshness();
    private final SidePopupStartup startup = new SidePopupStartup();
    private final android.os.Handler freshnessHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final android.graphics.Paint stalePaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
    private final Runnable freshnessTick = new Runnable() {
        @Override public void run() {
            if (!isAttachedToWindow()) return;
            refreshStartupReveal();
            if (!frameFreshness.fresh(android.os.SystemClock.uptimeMillis())) invalidate();
            freshnessHandler.postDelayed(this, 100);
        }
    };
    private final RectF sourceRect = new RectF();
    private final RectF destRect = new RectF();
    private static final PorterDuffXfermode DST_OUT = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
    private final Paint fadePaint = new Paint();
    private final FisheyeMesh mesh = new FisheyeMesh();
    private final FisheyeMesh.Painter paintTexture = this::drawTextureOnce;

    private WindowManager.LayoutParams params;
    protected boolean attached;
    protected boolean visible;
    private float dragStartX;
    private float dragStartY;
    private int windowStartX;
    private int windowStartY;
    private boolean multiTouchGesture;
    private final int dragSlop;
    protected static final float IDLE_ALPHA = 0.02f;
    private static final long FADE_IN_MS = 130L;
    private static final long FADE_OUT_MS = 160L;
    private ObjectAnimator fade;
    /** 正在淡出：{@link #visible} 已经是 false（逻辑上收了），窗口要露着直到淡完。 */
    private boolean fadingOut;
    private CompositeStreamGeometry.Plan plan;
    protected int lane = LaneCycle.LEFT;
    private boolean straighten;
    private int viewFov;
    private int yaw;
    private int pitch;
    private final SideViewFraming framing = new SideViewFraming();
    private final FisheyeMesh.SourceMap turnedMap = framing::sourcePoint;

    public SideViewPopupView(Context context, AppConfig appConfig) { this(context, appConfig, true); }

    protected SideViewPopupView(Context context, AppConfig appConfig, boolean followSystemTheme) {
        super(context);
        this.appConfig = appConfig;
        this.cameraLabel = new CameraLabel(context, followSystemTheme);
        this.sideArrow = new SideTurnArrow(context, followSystemTheme);
        dragSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        resizeGesture = new ScaleGestureDetector(context,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    private float startSpan;
                    private int startSize;

                    @Override
                    public boolean onScaleBegin(ScaleGestureDetector detector) {
                        if (!attached || !visible || params == null || expansion.expanded()) {
                            return false;
                        }
                        startSpan = detector.getCurrentSpan();
                        startSize = params.height;
                        savePosition();
                        return startSpan > 0f;
                    }

                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        int percent = SidePopupSizing.resizedPercent(startSize,
                                detector.getCurrentSpan() / startSpan,
                                getResources().getDisplayMetrics().heightPixels);
                        if (percent != appConfig.getSidePopupSizePercent()) {
                            appConfig.setSidePopupSizePercent(percent);
                            place();
                            savePosition();
                            update();
                            invalidate();
                        }
                        return true;
                    }
                });
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        setBackgroundColor(0xFF000000);
        textureView = new AutoFitTextureView(context);
        addView(textureView);
        controls = new SideViewControls(context, SideViewPopupService::dismissCurrentSideView,
                this::toggleExpanded);
        addView(controls);
        if (!showControls()) controls.setVisibility(GONE);
        setClipToOutline(true);
        setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override
            public void getOutline(android.view.View view, android.graphics.Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                        getResources().getDimension(com.kooo.evcam.R.dimen.side_control_radius));
            }
        });
        readConfig();
    }

    @Override protected void onConfigurationChanged(android.content.res.Configuration configuration) {
        super.onConfigurationChanged(configuration);
        invalidate(); // Repaint Canvas badges even when the current frame is unchanged.
    }

    public AutoFitTextureView getTextureView() {
        return textureView;
    }

    /** 合成流的真实尺寸；不像合成流的忽略（同 {@link RearViewMirrorView#setSourceSize}）。 */
    public void setSourceSize(Size size) {
        if (size == null || size.getWidth() <= 0 || size.getHeight() <= 0) {
            return;
        }
        String id = StreamLayoutTable.compositeCameraId();
        if (!CompositeStreamGeometry.looksLikeComposite(id, size.getWidth(), size.getHeight())) {
            AppLog.d(TAG, "忽略非合成流尺寸 " + size);
            return;
        }
        plan = CompositeStreamGeometry.analyse(id, size.getWidth(), size.getHeight());
        AppLog.i(TAG, "侧视取景: " + plan + " 第 " + lane + " 路");
        refreshStartupReveal();
        invalidate();
    }

    /** 知不知道合成流的几何；不知道时画面是黑的。 */
    public boolean hasGeometry() {
        return plan != null && plan.isComposite();
    }

    /** 窗口挂着（备着或显示中）。 */
    public boolean isAttached() {
        return attached;
    }

    /** 正显示在屏幕上。 */
    public boolean isShowing() {
        return isAttached() && visible;
    }

    private boolean presented;
    private Runnable windowVisibilityListener;
    public boolean isPresented() { return presented; }
    public void setWindowVisibilityListener(Runnable listener) { windowVisibilityListener = listener; }
    private void noteWindowVisibility(boolean shown) {
        if (presented == shown) return;
        presented = shown;
        if (windowVisibilityListener != null) windowVisibilityListener.run();
    }

    public int lane() {
        return lane;
    }

    // ------------------------------------------------------------------ 窗口

    /** 挂上但不显示：给相机一个推帧的地方，打灯时马上能出画面。 */
    public void attachHidden(int side) {
        attachAt(side, false);
    }

    /** 显示 {@code side} 那一路；没挂就挂上，备着就拨亮，显示着就换边。 */
    public void show(int side) {
        attachAt(side, true);
    }

    /** 收起来但窗口留着、相机照推（备着）。 */
    public void conceal() {
        if (!attached || !visible) {
            return;
        }
        visible = false;
        restoreExpanded();
        hideWithFade();
    }

    /** 整个拿掉。 */
    public void detach() { detachWindow(false); }

    /** Service-only removal outside this window's draw/touch dispatch; permits immediate texture reclamation. */
    public void detachImmediately() { detachWindow(true); }

    private void detachWindow(boolean immediate) {
        if (!attached) return;
        abortFade();
        attached = false;
        visible = false;
        try {
            if (immediate) windowManager.removeViewImmediate(this);
            else windowManager.removeView(this);
        } catch (Exception e) {
            AppLog.w(TAG, "侧视窗移除失败: " + e);
        }
        attached = false;
        visible = false;
        noteWindowVisibility(false);
        startup.request(false, false, android.os.SystemClock.uptimeMillis());
    }

    /** 设置页改了大小 / 位置 / 拉直：套用到挂着的窗口上。 */
    public void applyConfig() {
        restoreExpanded();
        readConfig();
        if (attached) {
            place();
            update();
        }
        invalidate();
    }

    protected boolean readMirrored() { return appConfig.isSidePopupMirrored(); }
    protected boolean readStraighten() { return appConfig.isSidePopupStraighten(); }
    protected boolean readAutoFit() { return appConfig.isSidePopupAutoFit(); }
    protected boolean readCameraLabel() { return appConfig.isRearViewCameraLabelEnabled(); }
    protected int readFov() { return appConfig.getSidePopupFov(); }
    protected int readYaw() { return appConfig.getSidePopupYaw(); }
    protected int readPitch() { return appConfig.getSidePopupPitch(); }
    protected int readRotation(int side) { return appConfig.getSidePopupRotation(side); }

    protected boolean showControls() { return true; }

    private void readConfig() {
        straighten = readStraighten();
        viewFov = readFov();
        yaw = readYaw();
        pitch = readPitch();
    }

    protected void attachAt(int side, boolean show) {
        boolean sideChanged = side != lane;
        boolean wasShowing = visible;
        lane = side;
        if (sideChanged) {
            // Require new source progression before revealing the newly selected crop.
            frameFreshness.reset();
            startup.request(false, false, android.os.SystemClock.uptimeMillis());
        }
        if (attached) {
            visible = show;
            if (sideChanged) place();
            if (show && (!wasShowing || sideChanged)) reveal(wasShowing && !sideChanged);
            else if (!show && wasShowing) hideWithFade();
            else { applyVisibility(show); update(); }
            invalidate();
            return;
        }
        params = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        setAlpha(IDLE_ALPHA);
        visible = show;
        applyVisibility(show);
        place();
        try {
            windowManager.addView(this, params);
            attached = true;
            noteWindowVisibility(params.alpha > 0f);
            if (show && startup.revealed()) startFade(IDLE_ALPHA, 1f, FADE_IN_MS, null);
        } catch (Exception e) {
            visible = false;
            AppLog.e(TAG, "侧视窗添加失败", e);
        }
    }

    // ------------------------------------------------------------------ 淡入淡出

    /** 窗口露在屏幕上：显示中，或还在淡出。备着（窗口全透明）时是 false。 */
    protected final boolean windowShown() {
        return visible || fadingOut;
    }

    /** 窗口露出来（不透明、接点击）。HUD 那块窗口换成自己的做法。 */
    protected void windowUp() {
        applyVisibility(true);
        update();
    }

    /** 窗口完全收起（全透明、点击穿过去）。HUD 那块窗口换成自己的做法。 */
    protected void windowDown() {
        applyVisibility(false);
        update();
    }

    /**
     * 拨亮：窗口露出来，画面从近乎透明淡入。
     *
     * @param continueFromCurrent 淡出到一半又要显示、而且还是同一边：从现在的透明度接着往上淡
     */
    protected void reveal(boolean continueFromCurrent) {
        fadingOut = false;
        float from = continueFromCurrent ? Math.max(getAlpha(), IDLE_ALPHA) : IDLE_ALPHA;
        cancelFade();
        setAlpha(from);   // 窗口露出来之前先定好，第一帧就是淡入的起点
        windowUp();
        if (params == null || startup.revealed()) startFade(from, 1f, FADE_IN_MS, null);
    }

    /** 收起：画面淡出，淡完才让窗口全透明（调用前 {@link #visible} 已经是 false）。 */
    protected void hideWithFade() {
        startup.request(false, false, android.os.SystemClock.uptimeMillis());
        if (params != null && params.alpha == 0f) {
            abortFade();
            windowDown();
            return;
        }
        fadingOut = true;
        if (params != null) params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        startFade(getAlpha(), IDLE_ALPHA, FADE_OUT_MS, () -> {
            fadingOut = false;
            if (!visible && attached) {
                windowDown();
            }
        });
    }

    /** 立刻停掉淡入淡出，画面回到「没显示」的透明度（窗口拿掉、重建之前）。 */
    protected void abortFade() {
        cancelFade();
        fadingOut = false;
        setAlpha(IDLE_ALPHA);
    }

    private void startFade(float from, float to, long durationMs, Runnable whenDone) {
        cancelFade();
        setAlpha(from);
        final ObjectAnimator animator = ObjectAnimator.ofFloat(this, View.ALPHA, from, to);
        animator.setDuration(durationMs);
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean canceled;

            @Override
            public void onAnimationCancel(Animator a) {
                canceled = true;
            }

            @Override
            public void onAnimationEnd(Animator a) {
                if (fade == animator) {
                    fade = null;
                }
                if (!canceled && whenDone != null) {
                    whenDone.run();
                }
            }
        });
        fade = animator;
        animator.start();
    }

    private void cancelFade() {
        ObjectAnimator running = fade;
        fade = null;
        if (running != null) {
            running.cancel();
        }
    }

    /**
     * 显示时不透明、接点击；备着时全透明、点击穿过去。
     *
     * <p><b>显示时不能带 FLAG_NOT_TOUCHABLE</b>：安卓 12 起，带这个标志的悬浮窗会被系统把不透明度
     * 压到 0.8（为了让点击能穿过去，b/218777508）—— PR 作者实车上看就是「有点透明」（2026-10-01）。
     * 代价是弹着的那一两秒里，窗口下面那块点不到。备着时 alpha 0，不受那条限制，照旧让点击穿过去，
     * 不然 D 档时屏幕上会有一块看不见又点不动的区域。</p>
     */
    private void applyVisibility(boolean show) {
        startup.request(show, hasLivePicture(), android.os.SystemClock.uptimeMillis());
        boolean reveal = startup.revealed();
        params.alpha = reveal || fadingOut ? 1f : 0f;
        if (reveal && visible) {
            params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        } else {
            params.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
    }

    private boolean hasLivePicture() {
        return hasGeometry() && textureView.isAvailable()
                && frameFreshness.fresh(android.os.SystemClock.uptimeMillis());
    }

    private void refreshStartupReveal() {
        // HUD owns a Presentation rather than these overlay params; its routing is unchanged.
        if (params == null || !attached) return;
        if (startup.advance(hasLivePicture(), android.os.SystemClock.uptimeMillis())) {
            applyVisibility(visible);
            update();
            startFade(IDLE_ALPHA, 1f, FADE_IN_MS, null);
            invalidate();
        }
    }

    /** Drag to position or pinch to resize; both sides share the saved window geometry. */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (!attached || !visible || params == null) {
            return true;
        }
        if (expansion.expanded()) {
            return true;
        }
        resizeGesture.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                multiTouchGesture = false;
                dragStartX = event.getRawX();
                dragStartY = event.getRawY();
                windowStartX = params.x;
                windowStartY = params.y;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                multiTouchGesture = true;
                break;
            case MotionEvent.ACTION_MOVE:
                if (!multiTouchGesture && event.getPointerCount() == 1) {
                    float dx = event.getRawX() - dragStartX;
                    float dy = event.getRawY() - dragStartY;
                    if (Math.hypot(dx, dy) >= dragSlop) {
                        params.x = SidePopupSizing.clampPosition(windowStartX + Math.round(dx),
                                getResources().getDisplayMetrics().widthPixels, params.width);
                        params.y = SidePopupSizing.clampPosition(windowStartY + Math.round(dy),
                                getResources().getDisplayMetrics().heightPixels, params.height);
                        savePosition();
                        update();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                multiTouchGesture = false;
                break;
            default:
                break;
        }
        return true;
    }

    private void savePosition() {
        if (params != null) {
            appConfig.setSidePopupPosition(params.x, params.y,
                    getResources().getDisplayMetrics().heightPixels, params.height);
        }
    }

    protected void update() {
        try {
            windowManager.updateViewLayout(this, params);
            noteWindowVisibility(attached && params.alpha > 0f);
        } catch (Exception e) {
            AppLog.w(TAG, "侧视窗更新失败: " + e);
        }
    }

    /**
     * One shared position for both lanes. Default is horizontally centred; dragging saves
     * its location, and resizing keeps that location within the screen.
     */
    protected void place() {
        if (expansion.expanded()) {
            return;
        }
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;
        SidePopupSizing.Size size = SidePopupSizing.dimensions(appConfig.getSidePopupSizePercent(),
                appConfig.getSidePopupAspect(), w, h, controls.minimumWindowSize());
        params.width = size.width;
        params.height = size.height;
        int savedX = appConfig.getSidePopupX();
        int savedY = appConfig.getSidePopupY();
        params.x = SidePopupSizing.clampPosition(savedX < 0 ? (w - size.width) / 2 : savedX, w, size.width);
        int y = savedY < 0
                ? Math.round(Math.max(0, h - size.height) * appConfig.getSidePopupVerticalPercent() / 100f)
                : savedY;
        params.y = SidePopupSizing.clampPosition(y, h, size.height);
    }

    private void toggleExpanded() {
        if (!attached || !visible || params == null) {
            return;
        }
        if (!expansion.expanded()) {
            savePosition();
        }
        SideViewExpansion.Geometry next = expansion.toggle(
                new SideViewExpansion.Geometry(params.x, params.y, params.width, params.height),
                getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels,
                getResources().getDimensionPixelSize(com.kooo.evcam.R.dimen.side_control_margin));
        applyGeometry(next);
        controls.setExpanded(expansion.expanded());
        update();
        invalidate();
    }

    private void restoreExpanded() {
        SideViewExpansion.Geometry normal = expansion.restore();
        if (normal != null && params != null) {
            applyGeometry(normal);
            controls.setExpanded(false);
        }
    }

    private void applyGeometry(SideViewExpansion.Geometry geometry) {
        params.x = geometry.x;
        params.y = geometry.y;
        params.width = geometry.width;
        params.height = geometry.height;
    }

    // ------------------------------------------------------------------ 布局与绘制

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        textureView.layout(0, 0, cameraPictureWidth(), getHeight());
        controls.layoutIn(getWidth());
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);
        measureChildren(widthSpec, heightSpec);
    }

    public void setIndicatorDisplay(Integer display) {
        if (java.util.Objects.equals(indicatorDisplay, display)) return;
        indicatorDisplay = display;
        invalidate();
    }

    public void clearFrames() { frameFreshness.reset(); invalidate(); }

    /** A replacement camera must supply its own geometry before a live picture can be revealed. */
    public void clearSourceGeometry() { plan = null; clearFrames(); }

    public void noteFrame() {
        android.graphics.SurfaceTexture source = textureView.getSurfaceTexture();
        try {
            frameFreshness.observe(source, source == null ? 0 : source.getTimestamp(), android.os.SystemClock.uptimeMillis());
        } catch (IllegalStateException released) { frameFreshness.reset(); }
        refreshStartupReveal();
        invalidate();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        clearFrames();
        freshnessHandler.post(freshnessTick);
    }

    @Override protected void onDetachedFromWindow() {
        abortFade();
        freshnessHandler.removeCallbacks(freshnessTick);
        clearFrames();
        super.onDetachedFromWindow();
    }

    private void drawStaleCover(Canvas canvas, int width, int height) {
        canvas.drawColor(android.graphics.Color.BLACK);
        stalePaint.setColor(android.graphics.Color.WHITE);
        stalePaint.setTextAlign(android.graphics.Paint.Align.CENTER);
        String message = getContext().getString(R.string.side_view_waiting_live);
        stalePaint.setTextSize(Math.max(1f, Math.min(height * .12f, width / (message.length() * .6f))));
        canvas.drawText(message, width / 2f, height / 2f, stalePaint);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        if (!hasGeometry()) {
            // 还不知道几何：子视图照画（TextureView 不画就不取帧，onSurfaceTextureUpdated 也不来，
            // 服务就没机会补上尺寸 —— 窗口会一直黑），再盖成黑的，不露出四路挤在一起的整条合成流
            drawTextureOnce(canvas);
            canvas.drawColor(0xFF000000);
            drawStaleCover(canvas, width, height);
            if (showControls()) drawChild(canvas, controls, getDrawingTime());
            return;
        }
        int save = canvas.save();
        Paint tone = pictureTone();
        float fade = HudEdgeFade.widthPx(edgeFadePercent(), width, height);
        if (tone != null || fade > 0f) canvas.saveLayer(0f, 0f, width, height, tone);
        int picture = canvas.save();
        canvas.clipRect(0, 0, width, height);
        canvas.translate(-cameraPictureOffsetX(), 0);
        int visibleWidth = width;
        width = cameraPictureWidth();
        int rotation = readRotation(lane);
        int inner = SideViewRotation.begin(canvas, straighten ? 0 : rotation, width, height, readMirrored());
        if (straighten) {
            boolean autoFit = readAutoFit();
            framing.configure(viewFov, yaw, pitch, lane, (float) width / height, rotation,
                    autoFit);
            // 虚拟相机转过去（往后、往上）再拉直，见 SideViewProjection
            RearViewGeometry.ShaderRects r = RearViewGeometry.toShaderRects(
                    plan, lane, RearViewGeometry.Viewport.full());
            mesh.prepare(SideViewFraming.meshDivisions(width, height),
                    r.laneOffsetX * width, r.laneOffsetY * height,
                    r.laneScaleX * width, r.laneScaleY * height, turnedMap);
            if (autoFit) mesh.draw(canvas, 0f, 0f, width, height, paintTexture);
            else mesh.drawClippedToLane(canvas, 0f, 0f, width, height, paintTexture);
        } else {
            // 不拉直：只能在原图里裁一块挪一挪（见 SideViewAim），角度折算成那边的百分比
            int zoomPercent = Math.round(180f / viewFov * 100f);
            int backPercent = Math.round(yaw / SideViewProjection.MAX_YAW_DEGREES * 100f);
            int upPercent = Math.round(pitch / SideViewProjection.MAX_PITCH_DEGREES * 100f);
            RearViewGeometry.Viewport viewport = SideViewAim.viewport(lane, zoomPercent, backPercent, upPercent);
            RearViewGeometry.Viewport crop = RearViewGeometry.Viewport.forWindow(width, height, 0.5f);
            viewport = new RearViewGeometry.Viewport(viewport.x + crop.x * viewport.width,
                    viewport.y + crop.y * viewport.height, crop.width * viewport.width, crop.height * viewport.height);
            float[] rect = RearViewGeometry.combinedSourceRect(plan, lane, viewport);
            sourceRect.set(rect[0] * width, rect[1] * height,
                    (rect[0] + rect[2]) * width, (rect[1] + rect[3]) * height);
            destRect.set(0, 0, width, height);
            drawMatrix.setRectToRect(sourceRect, destRect, Matrix.ScaleToFit.FILL);
            canvas.clipRect(destRect);
            canvas.concat(drawMatrix);
            drawChild(canvas, textureView, getDrawingTime());
        }
        canvas.restoreToCount(inner);
        canvas.restoreToCount(picture);
        width = visibleWidth;
        if (fade > 0f) drawEdgeFade(canvas, width, height, fade);
        canvas.restoreToCount(save);
        if (!textureView.isAvailable() || !frameFreshness.fresh(android.os.SystemClock.uptimeMillis())) {
            drawStaleCover(canvas, width, height);
            if (showControls()) drawChild(canvas, controls, getDrawingTime());
            return;
        }
        if (readCameraLabel()) {
            cameraLabel.drawBottom(canvas, lane, width, height);
        }
        sideArrow.draw(canvas, lane, indicatorDisplay, width, height, showControls());
        if (showControls()) drawChild(canvas, controls, getDrawingTime());
    }

    private void drawEdgeFade(Canvas canvas, int width, int height, float fade) {
        final int opaque = 0xFF000000;
        final int clear = 0x00000000;
        fadePaint.setXfermode(DST_OUT);
        fadePaint.setShader(new LinearGradient(0f, 0f, fade, 0f, opaque, clear, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, fade, height, fadePaint);
        fadePaint.setShader(new LinearGradient(width, 0f, width - fade, 0f, opaque, clear, Shader.TileMode.CLAMP));
        canvas.drawRect(width - fade, 0f, width, height, fadePaint);
        fadePaint.setShader(new LinearGradient(0f, 0f, 0f, fade, opaque, clear, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, 0f, width, fade, fadePaint);
        fadePaint.setShader(new LinearGradient(0f, height, 0f, height - fade, opaque, clear, Shader.TileMode.CLAMP));
        canvas.drawRect(0f, height - fade, width, height, fadePaint);
        fadePaint.setShader(null);
        fadePaint.setXfermode(null);
    }

    private void drawTextureOnce(Canvas canvas) {
        drawChild(canvas, textureView, getDrawingTime());
    }
    /** Optional HUD-only image treatment; labels and stale covers stay outside it. */
    protected Paint pictureTone() { return null; }
    protected int edgeFadePercent() { return 0; }
    /** HUD may retain only one side of the full picture; badges use the visible view size. */
    protected int cameraPictureWidth() { return getWidth(); }
    protected int cameraPictureOffsetX() { return 0; }
}
