package com.kooo.evcam.zeekr;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.TextureView;
import android.view.View;

import com.kooo.evcam.AppLog;
import com.kooo.evcam.R;
import com.kooo.evcam.telemetry.Readings;
import com.kooo.evcam.telemetry.Signal;
import com.kooo.evcam.telemetry.Telemetry;
import com.kooo.evcam.screen.ScreenState;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounded CPU prototype using snapshots of the existing preview, never another Camera2 output.
 * Texture capture stays on the UI thread; lookup construction and blending run on one worker.
 * One frame can be in flight. A stopped/reconfigured view cannot publish an old result.
 */
final class BirdEyePreview {
    private static final int SOURCE_W = 256, SOURCE_H = 1024, OUT_W = 320, OUT_H = 480;
    private static final long INTERVAL_MS = 200, TICK_MS = 100, MAX_READBACK_MS = 40;
    private final View owner;
    private final BirdEyeConfig config;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF(), car = new RectF();
    private ExecutorService worker;
    private TextureView texture;
    private CompositeStreamGeometry.Plan plan;
    private Bitmap source, output;
    private int[] inputPixels, outputPixels;
    private BirdEyeProjection.Map map;
    private BirdEyeProjection.Calibration calibration;
    private boolean running, busy;
    private int revision;
    private long displayedAt = -1, nextCaptureAt, lastReadbackMs;
    private final CameraFrameFreshness freshness = new CameraFrameFreshness();
    private android.graphics.SurfaceTexture displayedIdentity;
    private boolean slow;
    private BirdEyeSafetyPolicy.Block lastBlock = BirdEyeSafetyPolicy.Block.HIDDEN;
    private final Telemetry.Listener vehicleListener = readings -> refreshSafety();
    private final ScreenState.Listener screenListener = new ScreenState.Listener() {
        public void onScreenOff() { refreshSafety(); }
        public void onScreenOn() { refreshSafety(); }
    };
    private int failureCount;

    BirdEyePreview(View owner) { this.owner = owner; config = new BirdEyeConfig(owner.getContext()); }

    void start(TextureView texture, CompositeStreamGeometry.Plan plan) {
        if (running && this.texture == texture && this.plan == plan) return;
        stop();
        this.texture = texture; this.plan = plan;
        loadCalibration(); map = null;
        freshness.reset(); slow = false; nextCaptureAt = 0;
        Telemetry.get().addListener(vehicleListener);
        ScreenState.addListener(screenListener);
        running = true;
        if (worker == null || worker.isShutdown()) {
            worker = Executors.newSingleThreadExecutor(r -> new Thread(() -> {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                r.run();
            }, "diy-360"));
        }
        main.post(tick);
    }
    void invalidateCalibration() {
        revision++; map = null; displayedAt = -1; loadCalibration();
        owner.invalidate();
    }
    void stop() {
        running = false;
        invalidatePicture();
        main.removeCallbacks(tick);
        Telemetry.get().removeListener(vehicleListener);
        ScreenState.removeListener(screenListener);
    }
    void release() {
        stop();
        if (worker != null) { worker.shutdown(); worker = null; }
        // Do not recycle a bitmap still referenced by a worker or a hardware display list.
    }
    private void loadCalibration() {
        try { calibration = config.read(); }
        catch (IllegalArgumentException error) {
            calibration = null;
            AppLog.w("BirdEyePreview", "Invalid calibration: " + error.getMessage());
        }
    }
    static boolean parked() {
        Readings readings = Telemetry.get().readings();
        return Telemetry.get().carLinkConnected()
                && BirdEyeSafetyPolicy.parked(readings.text(Signal.GEAR), readings.number(Signal.SPEED));
    }
    private BirdEyeSafetyPolicy.Block runtimeBlock() {
        Readings readings = Telemetry.get().readings();
        return BirdEyeSafetyPolicy.block(Telemetry.get().carLinkConnected() ? readings.text(Signal.GEAR) : null, readings.number(Signal.SPEED),
                readings.bool(Signal.STOCK_360), readings.bool(Signal.STOCK_POPUP), readings.bool(Signal.PARK_ASSIST),
                owner.isShown() && owner.getWindowVisibility() == View.VISIBLE && !ScreenState.dark(),
                PreviewDewarp.isActive(texture), calibration != null, slow);
    }
    private void invalidatePicture() {
        revision++; displayedAt = -1; displayedIdentity = null; freshness.reset(); owner.invalidate();
    }
    private void refreshSafety() {
        if (!running) return;
        BirdEyeSafetyPolicy.Block block = runtimeBlock();
        if (block != lastBlock) { lastBlock = block; invalidatePicture(); }
    }
    private static int blockText(BirdEyeSafetyPolicy.Block block) {
        switch (block) {
            case PARK_REQUIRED: return R.string.bird_eye_park_required;
            case FACTORY: return R.string.bird_eye_factory_active;
            case GPU: return R.string.bird_eye_gpu_blocked;
            case CALIBRATION: return R.string.bird_eye_invalid_calibration;
            case PERFORMANCE: return R.string.bird_eye_slow;
            default: return R.string.bird_eye_waiting;
        }
    }
    String describeState() {
        long now = SystemClock.elapsedRealtime();
        return "running=" + running + ", busy=" + busy + ", lookup=" + (map != null)
                + ", block=" + runtimeBlock() + ", timestamp=" + freshness.timestamp()
                + ", sourceAgeMs=" + (freshness.changedAt() < 0 ? -1 : now - freshness.changedAt())
                + ", pictureAgeMs=" + (displayedAt < 0 ? -1 : now - displayedAt)
                + ", readbackMs=" + lastReadbackMs
                + ", capture=" + SOURCE_W + "x" + SOURCE_H + ", render=" + OUT_W + "x" + OUT_H
                + ", failures=" + failureCount;
    }
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            refreshSafety();
            if (lastBlock == BirdEyeSafetyPolicy.Block.NONE) {
                if (texture == null || !texture.isAvailable() || texture.getSurfaceTexture() == null) invalidatePicture();
                else if (!busy && SystemClock.elapsedRealtime() >= nextCaptureAt) capture();
            }
            owner.invalidate();
            if (running) main.postDelayed(this, TICK_MS);
        }
    };

    private void capture() {
        try {
            if (source == null) {
                source = Bitmap.createBitmap(SOURCE_W, SOURCE_H, Bitmap.Config.ARGB_8888);
                inputPixels = new int[SOURCE_W * SOURCE_H];
                outputPixels = new int[OUT_W * OUT_H];
                output = Bitmap.createBitmap(OUT_W, OUT_H, Bitmap.Config.ARGB_8888);
            }
            long began = SystemClock.elapsedRealtime();
            nextCaptureAt = began + INTERVAL_MS;
            // getBitmap(Bitmap) can fail silently and leave the bitmap UNCHANGED.
            // Transparent sentinels plus render's opacity checks prevent old pixels becoming live.
            source.eraseColor(Color.TRANSPARENT);
            Bitmap bitmap = texture.getBitmap(source);
            lastReadbackMs = SystemClock.elapsedRealtime() - began;
            if (lastReadbackMs > MAX_READBACK_MS) {
                slow = true; refreshSafety(); return;
            }
            if (bitmap == null || !texture.isAvailable() || texture.getSurfaceTexture() == null) {
                invalidatePicture(); return;
            }
            android.graphics.SurfaceTexture identity = texture.getSurfaceTexture();
            long now = SystemClock.elapsedRealtime();
            freshness.observe(identity, identity.getTimestamp(), now);
            if (!freshness.fresh(now)) { revision++; displayedAt = -1; return; }
            final int request = revision;
            final long capturedAt = freshness.changedAt();
            final BirdEyeProjection.Calibration settings = calibration;
            final CompositeStreamGeometry.Plan geometry = plan;
            final BirdEyeProjection.Map cached = map;
            final Bitmap captured = source;
            final android.graphics.SurfaceTexture capturedIdentity = identity;
            busy = true;
            worker.execute(() -> {
                BirdEyeProjection.Map prepared = cached;
                Exception failure = null;
                try {
                    if (prepared == null) prepared = new BirdEyeProjection.Map(settings, geometry,
                            SOURCE_W, SOURCE_H, OUT_W, OUT_H);
                    captured.getPixels(inputPixels, 0, SOURCE_W, 0, 0, SOURCE_W, SOURCE_H);
                    prepared.render(inputPixels, outputPixels);
                } catch (Exception e) { failure = e; }
                final BirdEyeProjection.Map ready = prepared;
                final Exception error = failure;
                main.post(() -> {
                    busy = false;
                    if (!running || revision != request || texture == null
                            || texture.getSurfaceTexture() != capturedIdentity) return;
                    if (error != null) { reportFailure(error); return; }
                    failureCount = 0;
                    // Keep a valid LUT even if its first construction took longer than the
                    // freshness budget. The next current frame can reuse it immediately.
                    map = ready;
                    if (runtimeBlock() != BirdEyeSafetyPolicy.Block.NONE) { invalidatePicture(); return; }
                    if (!CameraFrameFreshness.freshAt(capturedAt, SystemClock.elapsedRealtime())) return;
                    output.setPixels(outputPixels, 0, OUT_W, 0, 0, OUT_W, OUT_H);
                    displayedAt = capturedAt; displayedIdentity = capturedIdentity;
                    owner.invalidate();
                });
            });
        } catch (Exception e) {
            busy = false; reportFailure(e);
        }
    }
    private void reportFailure(Exception e) {
        invalidatePicture();
        if (failureCount++ == 0) AppLog.e("BirdEyePreview", "DIY 360 preview failed", e);
    }
    /** UI-thread snapshot of a currently fresh, permitted preview; never an old hidden frame. */
    Bitmap photo() {
        if (!running || runtimeBlock() != BirdEyeSafetyPolicy.Block.NONE || output == null
                || texture == null || !texture.isAvailable()
                || texture.getSurfaceTexture() != displayedIdentity
                || !CameraFrameFreshness.freshAt(displayedAt, SystemClock.elapsedRealtime())) return null;
        Bitmap result = Bitmap.createBitmap(OUT_W, OUT_H, Bitmap.Config.ARGB_8888);
        draw(new Canvas(result), OUT_W, OUT_H);
        // Export a readable warning at native pixel size, independent of head-unit DPI.
        Canvas canvas = new Canvas(result);
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.BLACK);
        canvas.drawRect(0, OUT_H - 24, OUT_W, OUT_H, paint);
        paint.setColor(Color.WHITE); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(12);
        String label = owner.getContext().getString(R.string.bird_eye_watermark);
        float measured = paint.measureText(label);
        if (measured > OUT_W - 12) paint.setTextSize(12 * (OUT_W - 12) / measured);
        canvas.drawText(label, OUT_W / 2f, OUT_H - 8, paint);
        return result;
    }
    void draw(Canvas canvas, int width, int height) {
        canvas.drawColor(Color.BLACK);
        float scale = Math.min(width / (float) OUT_W, height / (float) OUT_H);
        float w = OUT_W * scale, h = OUT_H * scale;
        destination.set((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2);
        BirdEyeSafetyPolicy.Block block = runtimeBlock();
        boolean fresh = block == BirdEyeSafetyPolicy.Block.NONE && texture != null && texture.isAvailable()
                && texture.getSurfaceTexture() == displayedIdentity
                && CameraFrameFreshness.freshAt(displayedAt, SystemClock.elapsedRealtime());
        if (fresh && output != null) {
            paint.setColor(Color.WHITE); paint.setStyle(Paint.Style.FILL);
            canvas.drawBitmap(output, null, destination, paint);
        }
        BirdEyeProjection.Calibration c = calibration;
        if (c == null) {
            paint.setColor(Color.WHITE); paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(18 * owner.getResources().getDisplayMetrics().scaledDensity);
            canvas.drawText(owner.getContext().getString(blockText(block)), width / 2f, height / 2f, paint);
            return;
        }
        double mpp = Math.max((c.width + 2 * c.margin) / OUT_W, (c.length + 2 * c.margin) / OUT_H);
        float carW = (float) (c.width / mpp * scale), carH = (float) (c.length / mpp * scale);
        car.set(width / 2f - carW / 2, height / 2f - carH / 2, width / 2f + carW / 2, height / 2f + carH / 2);
        paint.setColor(0xff292b30); paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(car, 10 * scale, 10 * scale, paint);
        paint.setColor(0xffdedee2); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2 * scale);
        canvas.drawRoundRect(car, 10 * scale, 10 * scale, paint);
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(18 * owner.getResources().getDisplayMetrics().scaledDensity);
        canvas.drawText(owner.getContext().getString(R.string.bird_eye_front), width / 2f, car.top + carH * .2f, paint);
        if (!fresh) {
            canvas.drawText(owner.getContext().getString(blockText(block)), width / 2f, height / 2f, paint);
        }
        paint.setTextSize(14 * owner.getResources().getDisplayMetrics().scaledDensity);
        canvas.drawText(owner.getContext().getString(R.string.bird_eye_watermark), width / 2f, height - 12 * scale, paint);
    }
}
