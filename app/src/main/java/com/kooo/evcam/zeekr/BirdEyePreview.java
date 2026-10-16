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
import com.kooo.evcam.AppConfig;
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
    private static volatile String lastDescription = "never activated";
    static String lastDescription() { return lastDescription; }
    private int requestedQuality = BirdEyeQuality.BALANCED, activeQuality = BirdEyeQuality.BALANCED;
    private BirdEyeQuality.Size size = BirdEyeQuality.resolve(BirdEyeQuality.BALANCED, 1280, 5140);
    private int qualityFallbacks;
    private final BirdEyePerformance performance = new BirdEyePerformance();
    private String qualityReason = "none";
    private final View owner;
    private final BirdEyeConfig config;
    private final BirdEyeAppearance appearance;
    private final BirdEyeVehicleOverlay vehicleOverlay = new BirdEyeVehicleOverlay();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final android.text.TextPaint noticePaint = new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final android.text.TextPaint footerPaint = new android.text.TextPaint(Paint.ANTI_ALIAS_FLAG);
    private android.text.StaticLayout noticeLayout, footerLayout;
    private String noticeText, footerText;
    private int noticeWidth, footerWidth;
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
    private long lastRenderMs, lastLookupMs;
    private long lastPresentedAt = -1, lastPresentationGapMs = -1;
    private long copiedTimestamp;
    private android.graphics.SurfaceTexture copiedIdentity;
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
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener calibrationListener = (prefs, key) ->
            main.post(() -> { if (running) invalidateCalibration(); });
    private final android.content.SharedPreferences.OnSharedPreferenceChangeListener appearanceListener = (prefs, key) ->
            main.post(this::refreshAppearance);

    BirdEyePreview(View owner) {
        this.owner = owner; config = new BirdEyeConfig(owner.getContext());
        appearance = new BirdEyeAppearance(owner.getContext());
    }

    void start(TextureView texture, CompositeStreamGeometry.Plan plan) {
        if (running && this.texture == texture && this.plan == plan) return;
        stop();
        this.texture = texture; this.plan = plan;
        resetQuality(true);
        loadCalibration(); map = null;
        freshness.reset(); slow = false; nextCaptureAt = 0;
        Telemetry.get().addListener(vehicleListener);
        ScreenState.addListener(screenListener);
        running = true;
        config.preferences.registerOnSharedPreferenceChangeListener(calibrationListener);
        appearance.preferences.registerOnSharedPreferenceChangeListener(appearanceListener);
        noteState();
        if (worker == null || worker.isShutdown()) {
            worker = Executors.newSingleThreadExecutor(r -> new Thread(() -> {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                r.run();
            }, "diy-360"));
        }
        main.post(tick);
    }
    void invalidateCalibration() {
        resetQuality(false);
        revision++; map = null; displayedAt = -1; loadCalibration();
        owner.invalidate();
        noteState();
    }
    void stop() {
        running = false;
        config.preferences.unregisterOnSharedPreferenceChangeListener(calibrationListener);
        appearance.preferences.unregisterOnSharedPreferenceChangeListener(appearanceListener);
        invalidatePicture();
        main.removeCallbacks(tick);
        Telemetry.get().removeListener(vehicleListener);
        ScreenState.removeListener(screenListener);
        noteState();
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
        prepareVehicleOverlay();
    }
    private void prepareVehicleOverlay() {
        String model = calibration == null ? BirdEyeAppearance.OUTLINE : BirdEyeAppearance.resolveModel(
                appearance.model(), calibration.imageFitted(), new AppConfig(owner.getContext()).is009CameraMappingEnabled(),
                VehicleIdentity.resolve("auto", android.os.Build.MODEL, android.os.Build.DEVICE, android.os.Build.PRODUCT));
        vehicleOverlay.prepare(owner.getResources(), model, appearance.colour());
    }
    private void refreshAppearance() {
        if (running) { prepareVehicleOverlay(); owner.invalidate(); }
    }
    private void resetQuality(boolean force) {
        int selected = config.quality();
        if (!force && selected == requestedQuality) return;
        requestedQuality = activeQuality = selected;
        qualityFallbacks = 0; qualityReason = "none"; slow = false;
        lastReadbackMs = lastRenderMs = lastLookupMs = 0;
        lastPresentedAt = lastPresentationGapMs = -1;
        if (plan != null) size = BirdEyeQuality.resolve(selected, plan.frameWidth, plan.frameHeight);
        performance.reset(size.intervalMs);
    }
    private void downgrade(String reason) {
        qualityReason = reason;
        invalidatePicture(); map = null;
        if (activeQuality == BirdEyeQuality.LOW) {
            slow = true;
            AppLog.w("BirdEyePreview", "360 paused at Low: " + reason);
        } else {
            activeQuality = BirdEyeQuality.lower(activeQuality); qualityFallbacks++;
            lastPresentationGapMs = -1;
            size = BirdEyeQuality.resolve(activeQuality, plan.frameWidth, plan.frameHeight);
            performance.reset(size.intervalMs);
            AppLog.w("BirdEyePreview", "360 resolution fallback: requested=" + requestedQuality
                    + " active=" + activeQuality + " source=" + size.sourceWidth + "x" + size.sourceHeight
                    + " render=" + size.outputWidth + "x" + size.outputHeight + " reason=" + reason);
            android.widget.Toast.makeText(owner.getContext(), R.string.bird_eye_quality_reduced,
                    android.widget.Toast.LENGTH_LONG).show();
        }
        nextCaptureAt = SystemClock.elapsedRealtime() + performance.interval();
        refreshSafety();
        noteState();
    }
    static boolean canConfigure() {
        return Telemetry.get().carLinkConnected() && !ScreenState.dark() && !ScreenState.androidOff();
    }
    private BirdEyeSafetyPolicy.Block runtimeBlock() {
        Readings readings = Telemetry.get().readings();
        return BirdEyeSafetyPolicy.block(Telemetry.get().carLinkConnected(), readings.text(Signal.GEAR),
                readings.bool(Signal.STOCK_360), readings.bool(Signal.STOCK_POPUP), readings.bool(Signal.PARK_ASSIST),
                owner.isShown() && owner.getWindowVisibility() == View.VISIBLE && !ScreenState.dark(),
                PreviewDewarp.isActive(texture), calibration != null, slow);
    }
    private void invalidatePicture() {
        revision++; displayedAt = -1; displayedIdentity = null; freshness.reset();
        copiedIdentity = null; copiedTimestamp = 0;
        lastPresentedAt = -1; // Retain the last measured gap for Diagnostics after leaving preview.
        owner.invalidate();
    }
    private void refreshSafety() {
        if (!running) return;
        BirdEyeSafetyPolicy.Block block = runtimeBlock();
        if (block != lastBlock) { lastBlock = block; invalidatePicture(); }
    }
    private static int blockText(BirdEyeSafetyPolicy.Block block) {
        switch (block) {
            case VEHICLE_REQUIRED: return R.string.bird_eye_vehicle_required;
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
                + ", renderMs=" + lastRenderMs + ", lookupMs=" + lastLookupMs
                + ", captureIntervalMs=" + performance.interval() + ", cadenceAdjustments=" + performance.adjustments() + ", presentationGapMs=" + lastPresentationGapMs
                + ", qualityRequested=" + requestedQuality + ", qualityActive=" + activeQuality
                + ", fallbacks=" + qualityFallbacks + ", qualityReason=" + qualityReason
                + ", capture=" + size.sourceWidth + "x" + size.sourceHeight
                + ", render=" + size.outputWidth + "x" + size.outputHeight
                + ", failures=" + failureCount;
    }
    private void noteState() {
        if (plan != null) lastDescription = "atElapsedMs=" + SystemClock.elapsedRealtime() + " " + describeState();
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
            scheduleTick();
        }
    };

    private void scheduleTick() {
        main.removeCallbacks(tick);
        if (running) main.postDelayed(tick, BirdEyeCadence.delay(SystemClock.elapsedRealtime(),
                nextCaptureAt, busy, lastBlock == BirdEyeSafetyPolicy.Block.NONE));
    }

    private void capture() {
        try {
            final BirdEyeQuality.Size frameSize = size;
            long began = SystemClock.elapsedRealtime();
            nextCaptureAt = began + performance.interval();
            final long frameBegan = began;
            android.graphics.SurfaceTexture current = texture.getSurfaceTexture();
            long timestamp = current == null ? 0 : current.getTimestamp();
            // Do not pay for readback/blending twice when the producer has not advanced.
            if (timestamp <= 0) { invalidatePicture(); return; }
            if (current == copiedIdentity && timestamp == copiedTimestamp) return;
            if (source == null || source.getWidth() != frameSize.sourceWidth || source.getHeight() != frameSize.sourceHeight
                    || output.getWidth() != frameSize.outputWidth || output.getHeight() != frameSize.outputHeight) {
                Bitmap nextSource = Bitmap.createBitmap(frameSize.sourceWidth, frameSize.sourceHeight, Bitmap.Config.ARGB_8888);
                Bitmap nextOutput = Bitmap.createBitmap(frameSize.outputWidth, frameSize.outputHeight, Bitmap.Config.ARGB_8888);
                int[] nextInput = new int[frameSize.sourceWidth * frameSize.sourceHeight];
                int[] nextPixels = new int[frameSize.outputWidth * frameSize.outputHeight];
                if (source != null) source.recycle(); // No worker is active, and this bitmap is never drawn.
                source = nextSource; output = nextOutput; inputPixels = nextInput; outputPixels = nextPixels;
            }
            began = SystemClock.elapsedRealtime();
            // getBitmap(Bitmap) can fail silently and leave the bitmap UNCHANGED.
            // Transparent sentinels plus render's opacity checks prevent old pixels becoming live.
            source.eraseColor(Color.TRANSPARENT);
            Bitmap bitmap = texture.getBitmap(source);
            lastReadbackMs = SystemClock.elapsedRealtime() - began;
            if (bitmap == null || !texture.isAvailable() || texture.getSurfaceTexture() == null) {
                invalidatePicture(); return;
            }
            android.graphics.SurfaceTexture identity = texture.getSurfaceTexture();
            copiedIdentity = identity;
            copiedTimestamp = identity.getTimestamp();
            long now = SystemClock.elapsedRealtime();
            freshness.observe(identity, copiedTimestamp, now);
            if (!freshness.fresh(now)) { revision++; displayedAt = -1; return; }
            final int request = revision;
            final long capturedAt = freshness.changedAt();
            final BirdEyeProjection.Calibration settings = calibration;
            final CompositeStreamGeometry.Plan geometry = plan;
            final BirdEyeProjection.Map cached = map;
            final Bitmap captured = source;
            final Bitmap target = output;
            final int[] capturedPixels = inputPixels, renderedPixels = outputPixels;
            final long[] timings = new long[2];
            final android.graphics.SurfaceTexture capturedIdentity = identity;
            busy = true;
            worker.execute(() -> {
                BirdEyeProjection.Map prepared = cached;
                Exception failure = null;
                boolean memoryFailure = false;
                try {
                    long lookupBegan = SystemClock.elapsedRealtime();
                    if (prepared == null) prepared = new BirdEyeProjection.Map(settings, geometry,
                            frameSize.sourceWidth, frameSize.sourceHeight, frameSize.outputWidth, frameSize.outputHeight);
                    timings[0] = SystemClock.elapsedRealtime() - lookupBegan;
                    long renderBegan = SystemClock.elapsedRealtime();
                    captured.getPixels(capturedPixels, 0, frameSize.sourceWidth, 0, 0, frameSize.sourceWidth, frameSize.sourceHeight);
                    prepared.render(capturedPixels, renderedPixels);
                    timings[1] = SystemClock.elapsedRealtime() - renderBegan;
                } catch (Exception e) { failure = e; }
                catch (OutOfMemoryError e) { memoryFailure = true; prepared = null; }
                final BirdEyeProjection.Map ready = prepared;
                final Exception error = failure;
                final boolean memoryError = memoryFailure;
                main.post(() -> {
                    busy = false;
                    scheduleTick(); // Worker completion re-arms the exact capture deadline.
                    if (!running || revision != request || texture == null
                            || texture.getSurfaceTexture() != capturedIdentity) return;
                    lastLookupMs = timings[0]; lastRenderMs = timings[1];
                    noteState();
                    if (memoryError) { downgrade("worker memory limit"); return; }
                    if (error != null) { reportFailure(error); return; }
                    BirdEyePerformance.Action pressure = performance.observe(lastReadbackMs, lastRenderMs);
                    if (pressure == BirdEyePerformance.Action.LOWER) {
                        downgrade("sustained readback/render " + lastReadbackMs + "/" + lastRenderMs + " ms at 250ms cadence"); return;
                    }
                    nextCaptureAt = Math.max(nextCaptureAt, frameBegan + performance.interval());
                    failureCount = 0;
                    // Keep a valid LUT even if its first construction took longer than the
                    // freshness budget. The next current frame can reuse it immediately.
                    map = ready;
                    if (runtimeBlock() != BirdEyeSafetyPolicy.Block.NONE) { invalidatePicture(); return; }
                    if (!CameraFrameFreshness.freshAt(capturedAt, SystemClock.elapsedRealtime())) return;
                    target.setPixels(renderedPixels, 0, frameSize.outputWidth, 0, 0, frameSize.outputWidth, frameSize.outputHeight);
                    displayedAt = capturedAt; displayedIdentity = capturedIdentity;
                    long presentedAt = SystemClock.elapsedRealtime();
                    lastPresentationGapMs = lastPresentedAt < 0 ? -1 : presentedAt - lastPresentedAt;
                    lastPresentedAt = presentedAt;
                    owner.invalidate();
                    noteState();
                });
            });
        } catch (Exception e) {
            busy = false; reportFailure(e);
        }
        catch (OutOfMemoryError e) { busy = false; downgrade("capture memory limit"); }
    }
    private void reportFailure(Exception e) {
        invalidatePicture();
        if (failureCount++ == 0) AppLog.e("BirdEyePreview", "DIY 360 preview failed", e);
        noteState();
    }
    /** UI-thread snapshot of a currently fresh, permitted preview; never an old hidden frame. */
    Bitmap photo() {
        if (!running || runtimeBlock() != BirdEyeSafetyPolicy.Block.NONE || output == null
                || texture == null || !texture.isAvailable()
                || texture.getSurfaceTexture() != displayedIdentity
                || !CameraFrameFreshness.freshAt(displayedAt, SystemClock.elapsedRealtime())) return null;
        Bitmap result;
        try { result = Bitmap.createBitmap(size.outputWidth, size.outputHeight, Bitmap.Config.ARGB_8888); }
        catch (OutOfMemoryError error) { downgrade("photo memory limit"); return null; }
        draw(new Canvas(result), size.outputWidth, size.outputHeight);
        // Export a readable warning at native pixel size, independent of head-unit DPI.
        Canvas canvas = new Canvas(result);
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.BLACK);
        float exportScale = size.outputWidth / 320f;
        canvas.drawRect(0, size.outputHeight - 24 * exportScale, size.outputWidth, size.outputHeight, paint);
        paint.setColor(Color.WHITE); paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(12 * exportScale);
        String label = owner.getContext().getString(R.string.bird_eye_watermark);
        float measured = paint.measureText(label);
        if (measured > size.outputWidth - 12) paint.setTextSize(12 * exportScale * (size.outputWidth - 12) / measured);
        canvas.drawText(label, size.outputWidth / 2f, size.outputHeight - 8 * exportScale, paint);
        return result;
    }
    void draw(Canvas canvas, int width, int height) {
        canvas.drawColor(Color.BLACK);
        float scale = Math.min(width / (float) size.outputWidth, height / (float) size.outputHeight);
        float w = size.outputWidth * scale, h = size.outputHeight * scale;
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
            drawNotice(canvas, owner.getContext().getString(blockText(block)), width, height / 2f);
            return;
        }
        double mpp = Math.max((c.width + 2 * c.margin) / size.outputWidth, (c.length + 2 * c.margin) / size.outputHeight);
        float carW = (float) (c.width / mpp * scale), carH = (float) (c.length / mpp * scale);
        car.set(width / 2f - carW / 2, height / 2f - carH / 2, width / 2f + carW / 2, height / 2f + carH / 2);
        if (!vehicleOverlay.draw(canvas, car)) {
            paint.setColor(0xff292b30); paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(car, 10 * scale, 10 * scale, paint);
            paint.setColor(0xffdedee2); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2 * scale);
            canvas.drawRoundRect(car, 10 * scale, 10 * scale, paint);
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(Math.min(18 * owner.getResources().getDisplayMetrics().scaledDensity, Math.max(1, carW / 3)));
            canvas.drawText(owner.getContext().getString(R.string.bird_eye_front), width / 2f, car.top + carH * .2f, paint);
        }
        if (!fresh) {
            drawNotice(canvas, owner.getContext().getString(blockText(block)), width, height / 2f);
        }
        String footer = owner.getContext().getString(R.string.bird_eye_watermark);
        float textSize = 14 * owner.getResources().getDisplayMetrics().scaledDensity;
        int inset = Math.max(1, Math.round(8 * owner.getResources().getDisplayMetrics().density));
        int available = Math.max(1, width - 2 * inset);
        if (footerLayout == null || !footer.equals(footerText) || available != footerWidth || footerPaint.getTextSize() != textSize) {
            footerText = footer; footerWidth = available; footerPaint.setColor(Color.WHITE); footerPaint.setTextSize(textSize);
            footerLayout = android.text.StaticLayout.Builder.obtain(footer, 0, footer.length(), footerPaint, available)
                    .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setMaxLines(2)
                    .setEllipsize(android.text.TextUtils.TruncateAt.END).build();
        }
        int save = canvas.save();
        canvas.translate(inset, Math.max(0, height - footerLayout.getHeight() - 12 * scale));
        footerLayout.draw(canvas); canvas.restoreToCount(save);
    }

    private void drawNotice(Canvas canvas, String text, int width, float centreY) {
        int inset = Math.max(1, Math.round(12 * owner.getResources().getDisplayMetrics().density));
        int available = Math.max(1, width - 2 * inset);
        float textSize = 18 * owner.getResources().getDisplayMetrics().scaledDensity;
        if (noticeLayout == null || !text.equals(noticeText) || available != noticeWidth || noticePaint.getTextSize() != textSize) {
            noticeText = text; noticeWidth = available; noticePaint.setColor(Color.WHITE); noticePaint.setTextSize(textSize);
            noticeLayout = android.text.StaticLayout.Builder.obtain(text, 0, text.length(), noticePaint, available)
                    .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER).setIncludePad(false).setMaxLines(4)
                    .setEllipsize(android.text.TextUtils.TruncateAt.END).build();
        }
        int save = canvas.save(); canvas.translate(inset, Math.max(0, centreY - noticeLayout.getHeight() / 2f));
        noticeLayout.draw(canvas); canvas.restoreToCount(save);
    }
}
