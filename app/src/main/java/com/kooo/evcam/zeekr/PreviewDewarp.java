package com.kooo.evcam.zeekr;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.kooo.evcam.AppConfig;
import com.kooo.evcam.AppLog;

import java.lang.ref.WeakReference;

/**
 * Optional main preview GPU routing: per-pixel fisheye correction and raw-texture DIY 360.
 * The normal CPU compatibility route stays direct. DIY 360 owns only a temporary
 * shader request; its source is never the already corrected/displayed picture.
 *
 * <p>平时相机直接写进屏幕上的 TextureView。开发者选项开着时，相机改写进一条
 * {@link FisheyeGlPipe}，由它逐像素校正后再画到那个 TextureView 上。
 * {@code SingleCamera} 建预览 Surface 的地方只改了一行：交给 {@link #surfaceFor}。</p>
 *
 * <p>只接管合成流那一路（环视）；座舱、以及开关没开、几何认不出来、管线起不来的时候，
 * 都照旧 {@code new Surface(surfaceTexture)} —— 和这个类存在之前一模一样。</p>
 *
 * <p>校正本身开不开、用什么投影，跟着屏幕上那个鱼眼开关和设置走（和分格近似同一套）；
 * 这里只决定「用哪种算法」。管线接上之后，{@link FourLaneContainer} 就不再分格。</p>
 *
 * <p>GPU 路由改动需点击「应用更改」：先关闭旧相机会话，再更换 Surface。</p>
 */
public final class PreviewDewarp {

    private static final String TAG = "PreviewDewarp";

    /** 只用来让「建管线」一次一个。读状态不拿它：主界面每画一帧都要问 {@link #isActive}。 */
    private static final Object LOCK = new Object();
    // Drawing must never wait for the camera thread's bounded GL startup/teardown lock.
    private static final java.util.concurrent.CopyOnWriteArrayList<WeakReference<FisheyeGlPipe>> RAW_PIPES
            = new java.util.concurrent.CopyOnWriteArrayList<>();
    static FisheyeGlPipe rawPipeFor(TextureView view) {
        return view == null ? null : rawPipeFor(view.getSurfaceTexture());
    }
    private static FisheyeGlPipe rawPipeFor(SurfaceTexture texture) {
        if (texture == null) return null;
        for (WeakReference<FisheyeGlPipe> reference : RAW_PIPES) {
            FisheyeGlPipe value = reference.get();
            if (value != null && value.isAlive() && value.output() == texture) return value;
        }
        return null;
    }
    private static void register(FisheyeGlPipe value) {
        RAW_PIPES.removeIf(reference -> {
            FisheyeGlPipe previous = reference.get(); return previous == null || !previous.isAlive();
        });
        RAW_PIPES.add(new WeakReference<>(value));
    }

    /** Existing mirror/side output only. Off leaves its original direct surface untouched. */
    public static Surface optionalViewSurfaceFor(Context context, SurfaceTexture output, Size buffer, boolean enabled) {
        if(!enabled || buffer==null)return new Surface(output);
        synchronized(LOCK) {
            FisheyeGlPipe existing=rawPipeFor(output);
            if(existing!=null){Surface surface=existing.newInputSurface();if(surface!=null)return surface;}
            FisheyeGlPipe next=FisheyeGlPipe.start("optional-camera",output,buffer.getWidth(),buffer.getHeight(),null);
            if(next!=null){register(next);Surface surface=next.newInputSurface();if(surface!=null)return surface;next.release();}
        }
        return new Surface(output);
    }
    /** Projected GL outputs have window-sized buffers; their input retains the native camera size. */
    public static boolean ownsOutput(SurfaceTexture output) { return rawPipeFor(output)!=null; }
    public static String describeOutput(TextureView view) {
        FisheyeGlPipe current = rawPipeFor(view);
        return current == null ? "direct" : current.describeOutput();
    }
    /** Same optional route for the existing lowest-priority popup output; no extra camera output. */
    public static Surface birdEyeSurfaceFor(Context context, SurfaceTexture texture, Size buffer) {
        return birdEyeSurfaceFor(context, texture, buffer, com.kooo.evcam.camera.CameraSessionOptions.read(context));
    }
    public static Surface birdEyeSurfaceFor(Context context, SurfaceTexture texture, Size buffer,
                                            com.kooo.evcam.camera.CameraSessionOptions options) {
        if (!options.gpu360 || buffer == null) return new Surface(texture);
        synchronized (LOCK) {
            FisheyeGlPipe existing = rawPipeFor(texture);
            if (existing != null && existing.isAlive()) {
                Surface input = existing.newInputSurface();
                if (input != null) return input;
            }
            FisheyeGlPipe next = FisheyeGlPipe.start("360-popup", texture, buffer.getWidth(), buffer.getHeight(), null);
            if (next != null) {
                register(next);
                Surface input = next.newInputSurface();
                if (input != null) return input;
                next.release();
            }
        }
        return new Surface(texture);
    }

    private static volatile FisheyeGlPipe pipe;
    private static volatile WeakReference<TextureView> pipeView = new WeakReference<>(null);
    private static volatile AppConfig config;
    private static volatile boolean fisheyeRoute;
    /** 拿住它：SharedPreferences 只弱引用监听器。 */
    private static SharedPreferences.OnSharedPreferenceChangeListener listener;

    private PreviewDewarp() {
    }

    /**
     * 相机给预览用的 Surface。
     *
     * @param texture    屏幕上那个 TextureView 的 SurfaceTexture，缓冲区尺寸已经设好
     * @param bufferSize 预览出流的尺寸；不知道时传 null（照旧直连）
     */
    public static Surface surfaceFor(String cameraId, TextureView view, SurfaceTexture texture,
                                     Size bufferSize) {
        return surfaceFor(cameraId, view, texture, bufferSize,
                com.kooo.evcam.camera.CameraSessionOptions.read(view.getContext()));
    }
    public static Surface surfaceFor(String cameraId, TextureView view, SurfaceTexture texture, Size bufferSize,
                                     com.kooo.evcam.camera.CameraSessionOptions options) {
        Surface routed = routed(cameraId, view, texture, bufferSize, options);
        return routed != null ? routed : new Surface(texture);
    }

    /**
     * 这个 TextureView 上的画面此刻是不是已经由 GPU 校正过（或至少经过了管线）。
     *
     * <p>也按画布认（管线画进的就是这个视图的 SurfaceTexture）：主界面重建时画布由管线留着、新界面接回同一块
     * （大纲 §7.6，2.11 阶段 4a），管线照旧画进它、不重建，而建管线时记下的是旧视图 —— 只按视图认的话，
     * 新界面会把校正过的画面当成没校正的再分格一遍。</p>
     */
    public static boolean isActive(TextureView view) {
        FisheyeGlPipe current = pipe;
        return view != null && current != null && current.isAlive() && config != null
                && fisheyeRoute && !current.hasBirdEyeRequest()
                && (pipeView.get() == view || current.output() == view.getSurfaceTexture());
    }

    private static Surface routed(String cameraId, TextureView view, SurfaceTexture texture,
                                  Size bufferSize, com.kooo.evcam.camera.CameraSessionOptions options) {
        if (view == null || texture == null || cameraId == null
                || !cameraId.equals(StreamLayoutTable.compositeCameraId())) {
            return null;
        }
        Context context = view.getContext().getApplicationContext();
        AppConfig appConfig = new AppConfig(context);
        if (!options.gpuPreview && !options.gpu360 && !options.gpuSide) {
            return null;
        }
        if (bufferSize == null) {
            AppLog.w(TAG, "预览尺寸还不知道，这一次照旧直连");
            return null;
        }
        CompositeStreamGeometry.Plan plan = CompositeStreamGeometry.analyse(
                cameraId, bufferSize.getWidth(), bufferSize.getHeight());
        if (plan == null || !plan.isComposite()) {
            AppLog.w(TAG, "预览缓冲 " + bufferSize + " 认不出是合成流，照旧直连");
            return null;
        }

        synchronized (LOCK) {
            // 同一块输出上已经有一条活着的管线：相机那边只是重建了 Surface，给它一个新的
            // 入口就行（直连时也是在同一个 SurfaceTexture 上再 new 一个 Surface）
            if (pipe != null && pipe.isAlive() && pipe.output() == texture) {
                Surface again = pipe.newInputSurface();
                if (again != null) {
                    AppLog.i(TAG, "相机 " + cameraId + " 重建预览 Surface，接回同一条管线");
                    return again;
                }
            }
            // 换了输出（TextureView 重建过）或者旧的坏了：先收掉旧的，同一块输出不能接两个生产者
            releaseLocked();

            float[] lanes = new float[plan.laneCount() * 4];
            for (int i = 0; i < plan.laneCount(); i++) {
                CompositeStreamGeometry.Lane lane = plan.lane(i);
                lanes[i * 4] = lane.u0;
                lanes[i * 4 + 1] = lane.v0;
                lanes[i * 4 + 2] = lane.u1 - lane.u0;
                lanes[i * 4 + 3] = lane.v1 - lane.v0;
            }
            FisheyeGlPipe started = FisheyeGlPipe.start("preview-" + cameraId, texture,
                    bufferSize.getWidth(), bufferSize.getHeight(), lanes);
            if (started == null) {
                return null;
            }
            Surface surface = started.newInputSurface();
            if (surface == null) {
                started.release();
                return null;
            }
            pipe = started;
            register(started);
            pipeView = new WeakReference<>(view);
            config = appConfig;
            fisheyeRoute = options.gpuPreview;
            push();
            if (listener == null) {
                listener = appConfig.onFisheyeChanged(PreviewDewarp::push);
            }
            WeakReference<TextureView> viewRef = new WeakReference<>(view);
            // 这个回调在管线自己的线程上：不拿锁，挪到主线程去做
            started.setOnReleased(() -> {
                TextureView gone = viewRef.get();
                if (gone == null) {
                    return;
                }
                gone.post(() -> {
                    if (pipe == started) {
                        pipe = null;
                    }
                    redraw(gone);
                });
            });
            AppLog.i(TAG, "Camera " + cameraId + " GPU preview route: fisheye=" + appConfig.isGpuFisheyePreview()
                    + " diy360=" + appConfig.isGpuBirdEye() + " lanes=" + plan.laneCount() + " buffer=" + bufferSize);
            redraw(view);
            return surface;
        }
    }

    /** 把屏幕上那个鱼眼开关和设置推给管线。 */
    private static void push() {
        FisheyeGlPipe current = pipe;
        AppConfig appConfig = config;
        if (current == null || appConfig == null) {
            return;
        }
        current.setCorrection(fisheyeRoute && appConfig.isFisheyeCorrection(), appConfig.getFisheyeFov(),
                appConfig.getFisheyeProjection());
    }

    /** Only after camera shutdown: an EGL producer must never be disconnected while in a live session. */
    public static void releaseAll() {
        synchronized (LOCK) {
            for (WeakReference<FisheyeGlPipe> reference : RAW_PIPES) {
                FisheyeGlPipe value = reference.get();
                if (value != null) value.release();
            }
            RAW_PIPES.clear();
            releaseLocked();
            pipeView = new WeakReference<>(null);
        }
    }

    private static void releaseLocked() {
        if (pipe != null) {
            pipe.release();
            pipe = null;
        }
    }

    /** 管线接上或断开，四宫格那边的画法跟着变（分格 / 不分格），要重画一次。 */
    private static void redraw(TextureView view) {
        view.post(() -> {
            View parent = view.getParent() instanceof View ? (View) view.getParent() : null;
            if (parent != null) {
                parent.invalidate();
            }
        });
    }
}
