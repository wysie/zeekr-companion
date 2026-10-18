package com.kooo.evcam.zeekr;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.TextureView;
import com.kooo.evcam.AppLog;
import com.kooo.evcam.CameraForegroundService;
import com.kooo.evcam.R;
import com.kooo.evcam.UserExit;
import com.kooo.evcam.WakeUpHelper;
import com.kooo.evcam.camera.CameraManagerHolder;
import com.kooo.evcam.camera.CameraNeeds;
import com.kooo.evcam.camera.CameraSlots;
import com.kooo.evcam.camera.MultiCameraManager;
import com.kooo.evcam.camera.SingleCamera;
import com.kooo.evcam.screen.ScreenState;
import com.kooo.evcam.settings.Languages;
import com.kooo.evcam.telemetry.Readings;
import com.kooo.evcam.telemetry.Signal;
import com.kooo.evcam.telemetry.Telemetry;

/** Explicit, non-sticky 360 overlay; never starts from boot or an indicator. */
public final class BirdEyePopupService extends Service {
    private static volatile BirdEyePopupService instance;
    private static final java.util.Set<Runnable> stateListeners =
            new java.util.concurrent.CopyOnWriteArraySet<>();

    /** Main-thread lifecycle notifications; the requested session includes temporary suspension. */
    public static void addStateListener(Runnable listener) { stateListeners.add(listener); }
    public static void removeStateListener(Runnable listener) { stateListeners.remove(listener); }
    private static void notifyState() { for (Runnable listener : stateListeners) listener.run(); }
    private final Handler main = new Handler(Looper.getMainLooper());
    private BirdEyePopupView view;
    private SingleCamera camera;
    private MultiCameraManager manager;
    private SurfaceTexture bound;
    private boolean destroyed, listening;
    private long openedAt;
    private final Telemetry.Listener telemetry = readings -> evaluate();
    private final ScreenState.Listener screen = new ScreenState.Listener() {
        public void onScreenOff() { listen(false); suspend(); }
        public void onScreenOn() { listen(true); evaluate(); }
    };
    private final Runnable tick = new Runnable() {
        public void run() { if (!destroyed) { evaluate(); main.postDelayed(this, 200); } }
    };

    public static boolean start(Context context) {
        if (!WakeUpHelper.hasOverlayPermission(context)) return false;
        if (!CameraForegroundService.isRunning()) CameraForegroundService.start(context);
        context.startService(new Intent(context, BirdEyePopupService.class));
        return true;
    }
    public static void stop(Context context) { context.stopService(new Intent(context, BirdEyePopupService.class)); }
    public static boolean isRunning() { return instance != null; }
    public static boolean isWindowVisible() { return instance != null && instance.view != null; }
    public static android.graphics.Bitmap photo() { return instance == null || instance.view == null ? null : instance.view.photo(); }
    public static String describe() { return instance == null ? "closed" : instance.view == null ? "suspended" : instance.view.describe(); }
    public static void applyLayout() { if (instance != null && instance.view != null) instance.view.applyLayout(); }

    @Override public void onCreate() {
        super.onCreate(); instance = this; ScreenState.addListener(screen); notifyState();
    }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (intent == null || UserExit.blocks(this, "BirdEyePopupService") || !WakeUpHelper.hasOverlayPermission(this)) {
            stopSelf(); return START_NOT_STICKY;
        }
        openedAt = android.os.SystemClock.uptimeMillis();
        ScreenState.whenKnown(() -> {
            if (destroyed) return;
            listen(!ScreenState.dark() && !ScreenState.androidOff());
            main.removeCallbacks(tick); main.post(tick);
        });
        return START_NOT_STICKY;
    }
    private void listen(boolean on) {
        if (on == listening) return;
        listening = on;
        if (on) { Telemetry.get().acquire(this, "bird-eye-popup"); Telemetry.get().addListener(telemetry); }
        else { Telemetry.get().removeListener(telemetry); Telemetry.get().release("bird-eye-popup"); }
    }
    private void evaluate() {
        if (destroyed) return;
        Readings r = Telemetry.get().readings();
        if (!Telemetry.get().carLinkConnected() || ScreenState.dark() || ScreenState.androidOff()
                || CameraNeeds.current().developerProbe() || "R".equals(r.text(Signal.GEAR))
                || Boolean.TRUE.equals(r.bool(Signal.STOCK_360)) || Boolean.TRUE.equals(r.bool(Signal.STOCK_POPUP))
                || Boolean.TRUE.equals(r.bool(Signal.PARK_ASSIST))) { suspend(); return; }
        MultiCameraManager current = CameraManagerHolder.getInstance().getCameraManager();
        if (manager != null && manager != current) suspend();
        if (view != null) { if (bound == null && view.texture.isAvailable()) bind(view.texture.getSurfaceTexture()); return; }
        try {
            Context context = new ContextThemeWrapper(Languages.localized(this), R.style.Theme_Cam);
            BirdEyePopupView next = new BirdEyePopupView(context, this::stopSelf);
            view = next;
            next.texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) { if (view == next) bind(st); }
                public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) { restoreBuffer(st); }
                public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
                    MultiCameraManager owner = manager != null ? manager : CameraManagerHolder.getInstance().getCameraManager();
                    boolean kept = MultiCameraManager.keepAuxiliaryCanvas(owner,
                            com.kooo.evcam.camera.ChannelRules.Kind.BIRD_EYE, st);
                    if (bound == st) unbind(); return !kept;
                }
                public void onSurfaceTextureUpdated(SurfaceTexture st) {
                    if (view != next) return;
                    if (camera != null) next.source(camera.getPreviewSize());
                    next.invalidate();
                }
            });
            next.show();
        } catch (RuntimeException error) {
            AppLog.e("BirdEyePopup", "Unable to attach 360 window", error); suspend(); stopSelf();
        }
    }
    private void bind(SurfaceTexture st) {
        if (view == null || st == null || st.isReleased() || destroyed) return;
        MultiCameraManager available = CameraManagerHolder.getInstance().getCameraManager();
        if (available == null) available = CameraManagerHolder.getInstance().getOrInit(this);
        SingleCamera candidate = available == null ? null : available.getCamera(CameraSlots.KEY_SURROUND);
        if (candidate == null) {
            if (android.os.SystemClock.uptimeMillis() - openedAt > 5000) {
                android.widget.Toast.makeText(Languages.localized(this), R.string.bird_eye_unavailable,
                        android.widget.Toast.LENGTH_LONG).show();
                suspend(); stopSelf();
            }
            return;
        } // Tick retries discovery without opening another camera directly.
        if (bound == st && camera == candidate) return;
        unbind(); manager = available; camera = candidate; bound = st;
        restoreBuffer(st); view.source(candidate.getPreviewSize());
        candidate.declareBirdEye(st, why -> {
            if (why == SingleCamera.Drop.CONFIG_FAILED) main.post(() -> {
                if (destroyed || bound != st || camera != candidate) return;
                android.widget.Toast.makeText(Languages.localized(this), R.string.bird_eye_popup_rejected,
                        android.widget.Toast.LENGTH_LONG).show();
                suspend(); stopSelf();
            });
        });
        CameraNeeds.current().claim(CameraNeeds.Holder.BIRD_EYE);
        available.channelScheduler().wake();
    }
    private void restoreBuffer(SurfaceTexture st) {
        if (camera != null && camera.getPreviewBufferSize() != null && st != null && !st.isReleased()) {
            android.util.Size size = camera.getPreviewBufferSize(); st.setDefaultBufferSize(size.getWidth(), size.getHeight());
        }
    }
    private void unbind() {
        CameraNeeds.current().release(CameraNeeds.Holder.BIRD_EYE);
        if (camera != null && camera.withdrawBirdEye(bound) && manager != null) manager.channelScheduler().wake();
        camera = null; manager = null; bound = null;
    }
    private void suspend() {
        unbind(); BirdEyePopupView old = view; view = null;
        if (old != null) old.detach();
    }
    @Override public void onDestroy() {
        destroyed = true; main.removeCallbacksAndMessages(null); listen(false); ScreenState.removeListener(screen);
        suspend(); if (instance == this) { instance = null; notifyState(); } super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
