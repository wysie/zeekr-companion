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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounded CPU prototype using snapshots of the existing preview, never another Camera2 output.
 * Texture capture stays on the UI thread; lookup construction and blending run on one worker.
 * One frame can be in flight. A stopped/reconfigured view cannot publish an old result.
 */
final class BirdEyePreview {
    private static final int SOURCE_W = 256, SOURCE_H = 1024, OUT_W = 320, OUT_H = 480;
    private static final long INTERVAL_MS = 200, FRESH_MS = 1000;
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
    private long sourceTimestamp, sourceChangedAt, displayedAt;
    private android.graphics.SurfaceTexture sourceIdentity;
    private int failureCount;

    BirdEyePreview(View owner) { this.owner = owner; config = new BirdEyeConfig(owner.getContext()); }

    void start(TextureView texture, CompositeStreamGeometry.Plan plan) {
        if (running && this.texture == texture && this.plan == plan) return;
        stop();
        this.texture = texture; this.plan = plan;
        calibration = config.read(); map = null;
        sourceTimestamp = 0; sourceChangedAt = 0; sourceIdentity = null;
        running = true;
        if (worker == null || worker.isShutdown()) {
            worker = Executors.newSingleThreadExecutor(r -> new Thread(r, "diy-360"));
        }
        main.post(tick);
    }
    void invalidateCalibration() {
        revision++; map = null; displayedAt = 0; calibration = config.read();
        owner.invalidate();
    }
    void stop() {
        running = false; revision++; displayedAt = 0;
        main.removeCallbacks(tick);
    }
    void release() {
        stop();
        if (worker != null) { worker.shutdown(); worker = null; }
        // Do not recycle a bitmap still referenced by a worker or a hardware display list.
    }
    String describeState() {
        long now = SystemClock.elapsedRealtime();
        return "running=" + running + ", busy=" + busy + ", lookup=" + (map != null)
                + ", timestamp=" + sourceTimestamp
                + ", sourceAgeMs=" + (sourceChangedAt == 0 ? -1 : now - sourceChangedAt)
                + ", pictureAgeMs=" + (displayedAt == 0 ? -1 : now - displayedAt)
                + ", capture=" + SOURCE_W + "x" + SOURCE_H + ", render=" + OUT_W + "x" + OUT_H
                + ", failures=" + failureCount;
    }
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            if (!busy && owner.isShown() && owner.getWindowVisibility() == View.VISIBLE && texture != null
                    && texture.isAvailable() && !PreviewDewarp.isActive(texture)) capture();
            owner.invalidate(); // A stalled camera must visibly lose its last picture.
            if (running) main.postDelayed(this, INTERVAL_MS);
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
            if (texture.getBitmap(source) == null || texture.getSurfaceTexture() == null) return;
            android.graphics.SurfaceTexture identity = texture.getSurfaceTexture();
            long timestamp = identity.getTimestamp();
            long now = SystemClock.elapsedRealtime();
            if (identity != sourceIdentity) {
                sourceIdentity = identity; sourceTimestamp = timestamp;
                sourceChangedAt = 0; // First buffer may be retained from an old, stopped session.
            } else if (timestamp != sourceTimestamp) {
                sourceTimestamp = timestamp;
                sourceChangedAt = timestamp == 0 ? 0 : now;
            }
            if (sourceChangedAt == 0 || now - sourceChangedAt > FRESH_MS) return;
            final int request = revision;
            final long capturedAt = sourceChangedAt;
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
                    if (SystemClock.elapsedRealtime() - capturedAt > FRESH_MS) return;
                    output.setPixels(outputPixels, 0, OUT_W, 0, 0, OUT_W, OUT_H);
                    displayedAt = capturedAt;
                    owner.invalidate();
                });
            });
        } catch (Exception e) {
            busy = false; displayedAt = 0; reportFailure(e);
        }
    }
    private void reportFailure(Exception e) {
        displayedAt = 0;
        if (failureCount++ == 0) AppLog.e("BirdEyePreview", "DIY 360 preview failed", e);
    }
    void draw(Canvas canvas, int width, int height) {
        canvas.drawColor(Color.BLACK);
        float scale = Math.min(width / (float) OUT_W, height / (float) OUT_H);
        float w = OUT_W * scale, h = OUT_H * scale;
        destination.set((width - w) / 2, (height - h) / 2, (width + w) / 2, (height + h) / 2);
        boolean fresh = displayedAt != 0 && SystemClock.elapsedRealtime() - displayedAt <= FRESH_MS;
        if (fresh && output != null) {
            paint.setColor(Color.WHITE); paint.setStyle(Paint.Style.FILL);
            canvas.drawBitmap(output, null, destination, paint);
        }
        BirdEyeProjection.Calibration c = calibration == null ? config.read() : calibration;
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
            canvas.drawText(owner.getContext().getString(PreviewDewarp.isActive(texture)
                    ? R.string.bird_eye_gpu_blocked : R.string.bird_eye_waiting), width / 2f, height / 2f, paint);
        }
        paint.setTextSize(14 * owner.getResources().getDisplayMetrics().scaledDensity);
        canvas.drawText(owner.getContext().getString(R.string.bird_eye_watermark), width / 2f, height - 12 * scale, paint);
    }
}
