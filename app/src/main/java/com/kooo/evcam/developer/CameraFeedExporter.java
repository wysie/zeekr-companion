package com.kooo.evcam.developer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/** One Camera2 ID at a time. Normal app cameras must already be confirmed closed. */
final class CameraFeedExporter {
    private static final int MAX_JPEG_BYTES = 32 * 1024 * 1024;
    private final CameraManager manager;
    private final File folder;
    private final android.content.SharedPreferences discoveries;
    private final HandlerThread thread = new HandlerThread("DeveloperCameraProbe");
    private final Handler callbacks;
    private final ProbeCloseGuard guard = new ProbeCloseGuard();
    private final Consumer<String> progress;
    private final Runnable released;
    private final Map<String, Boolean> availability = new HashMap<>();
    private final CountDownLatch heard = new CountDownLatch(1);
    private volatile boolean cancelled;
    private volatile Shot active;
    private final CameraManager.AvailabilityCallback availabilityCallback = new CameraManager.AvailabilityCallback() {
        @Override public void onCameraAvailable(String id) { synchronized (availability) { availability.put(id, true); } heard.countDown(); }
        @Override public void onCameraUnavailable(String id) { synchronized (availability) { availability.put(id, false); } heard.countDown(); }
    };

    CameraFeedExporter(Context context, File folder, Consumer<String> progress, Runnable released) {
        manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        this.folder = folder; this.progress = progress; this.released = released;
        discoveries = context.getSharedPreferences("camera_discovery", Context.MODE_PRIVATE);
        thread.start(); callbacks = new Handler(thread.getLooper());
    }

    boolean waitingForNativeClosure() { return guard.waiting(); }

    void cancel() {
        cancelled = true;
        Shot shot = active;
        if (shot != null) callbacks.post(shot::stop);
    }

    void run(JSONObject report) {
        JSONArray results = new JSONArray();
        try {
            report.put("cameras", results);
            manager.registerAvailabilityCallback(availabilityCallback, callbacks);
            heard.await(2, TimeUnit.SECONDS);
            // Deliver the initial availability inventory before opening any device.
            CountDownLatch barrier = new CountDownLatch(1);
            callbacks.post(barrier::countDown); barrier.await(2, TimeUnit.SECONDS);
            String[] listed = manager.getCameraIdList();
            report.put("camera_ids", new JSONArray(Arrays.asList(listed)));
            java.util.Set<String> advertised = new java.util.LinkedHashSet<>(Arrays.asList(listed));
            java.util.Set<String> reported = new java.util.LinkedHashSet<>();
            synchronized (availability) { reported.addAll(availability.keySet()); }
            for (String id : listed) try { reported.addAll(manager.getCameraCharacteristics(id).getPhysicalCameraIds()); }
                catch (Exception ignored) { }
            java.util.List<String> ids = ProbeCameraIds.candidates(advertised, reported);
            report.put("candidate_ids", new JSONArray(ids));
            report.put("candidate_policy", "Advertised, physical/availability IDs and numeric 0–15; max 48 candidates, 3 minutes; inaccessible IDs are reported, never permission-bypassed.");
            long deadline = android.os.SystemClock.elapsedRealtime() + 180_000;
            for (int n = 0; n < ids.size() && !cancelled; n++) {
                if (android.os.SystemClock.elapsedRealtime() >= deadline) { report.put("limit_reached", "3-minute scan limit"); break; }
                String id = ids.get(n);
                JSONObject item = new JSONObject().put("id", id).put("advertised", advertised.contains(id))
                        .put("candidate_origin", advertised.contains(id) ? "advertised" : reported.contains(id) ? "physical_or_availability" : "numeric_probe");
                results.put(item);
                progress.accept("Camera " + id + " (" + (n + 1) + "/" + ids.size() + ")");
                try {
                    CameraCharacteristics c = manager.getCameraCharacteristics(id);
                    item.put("lens_facing", c.get(CameraCharacteristics.LENS_FACING));
                    item.put("sensor_orientation", c.get(CameraCharacteristics.SENSOR_ORIENTATION));
                    item.put("physical_ids", new JSONArray(c.getPhysicalCameraIds()));
                    StreamConfigurationMap map = c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map == null) throw new IllegalStateException("No stream configuration map");
                    Size[] previewSizes = map.getOutputSizes(ImageFormat.PRIVATE);
                    Size[] jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
                    Size[] yuvSizes = map.getOutputSizes(ImageFormat.YUV_420_888);
                    item.put("private_sizes", sizesJson(previewSizes));
                    item.put("jpeg_sizes", sizesJson(jpegSizes));
                    item.put("yuv_sizes", sizesJson(yuvSizes));
                    synchronized (availability) {
                        if (Boolean.FALSE.equals(availability.get(id)) || advertised.contains(id) && !Boolean.TRUE.equals(availability.get(id))) {
                            item.put("result", "Skipped: unavailable or availability unknown; no takeover attempted");
                            continue;
                        }
                    }
                    Size nativeSize = choose(previewSizes, null);
                    int format = jpegSizes != null && jpegSizes.length > 0 ? ImageFormat.JPEG : ImageFormat.YUV_420_888;
                    Size capture = choose(format == ImageFormat.JPEG ? jpegSizes : yuvSizes, nativeSize);
                    if (capture == null) throw new IllegalStateException("No supported capture size within 8 MP / 6000 px bound");
                    // A preview sink warms the camera before a JPEG still; YUV can repeat directly.
                    Size preview = format == ImageFormat.JPEG ? choose(previewSizes, capture) : null;
                    item.put("capture_size", capture.toString()).put("format", format == ImageFormat.JPEG ? "JPEG" : "YUV_420_888 to JPEG");
                    item.put("preview_size", preview == null ? JSONObject.NULL : preview.toString());
                    Shot shot = new Shot(id, capture, preview, format);
                    active = shot;
                    if (cancelled) break;
                    guard.opening();
                    callbacks.post(shot::open);
                    if (!shot.done.await(12, TimeUnit.SECONDS)) {
                        shot.error = "Timed out waiting for open/session/fresh image";
                    }
                    callbacks.post(shot::stop);
                    boolean closed = shot.closed.await(10, TimeUnit.SECONDS);
                    item.put("closure_confirmed", closed);
                    if (shot.bytes != null && !cancelled) {
                        String name = "camera_" + n + "_" + id.replaceAll("[^a-zA-Z0-9_-]", "_") + ".jpg";
                        File file = new File(folder, name);
                        try (FileOutputStream out = new FileOutputStream(file)) { out.write(shot.bytes); }
                        item.put("image", name).put("image_timestamp_ns", shot.timestamp);
                        item.put("result", "Saved");
                        java.util.Set<String> captured = new java.util.LinkedHashSet<>(discoveries.getStringSet("captured_ids", java.util.Collections.emptySet()));
                        if (captured.size() < ProbeCameraIds.MAX_CANDIDATES) captured.add(id);
                        discoveries.edit().putStringSet("captured_ids", captured).apply();
                    } else item.put("result", cancelled ? "Cancelled" : shot.error == null ? "No image" : shot.error);
                    if (!closed) {
                        report.put("closure_warning", "Native close not confirmed. Lease retained; restart app if it does not recover.");
                        break; // Never open a second device alongside a late open/close.
                    }
                    active = null;
                } catch (Exception e) { item.put("result", e.toString()); }
                if (guard.waiting()) break;
            }
            report.put("cancelled", cancelled);
        } catch (Exception e) {
            try { report.put("error", e.toString()); } catch (Exception ignored) { }
        } finally {
            // Late callbacks still own their HandlerThread and resources until confirmed closed.
            if (guard.finish()) release();
        }
    }

    private void release() {
        try { manager.unregisterAvailabilityCallback(availabilityCallback); }
        finally { released.run(); thread.quitSafely(); }
    }

    private final class Shot {
        final String id;
        final Size size, previewSize;
        final int format;
        final CountDownLatch done = new CountDownLatch(1), closed = new CountDownLatch(1);
        CameraDevice device;
        CameraCaptureSession session;
        ImageReader image, sink;
        boolean stopped, terminal, shutter;
        int frames;
        long lastTimestamp;
        volatile long timestamp;
        volatile byte[] bytes;
        volatile String error;
        Shot(String id, Size size, Size preview, int format) { this.id = id; this.size = size; previewSize = preview; this.format = format; }

        @SuppressLint("MissingPermission") void open() {
            if (cancelled || stopped) { noDevice(); return; }
            try { manager.openCamera(id, state, callbacks); }
            catch (Exception e) { error = e.toString(); noDevice(); }
        }
        final CameraDevice.StateCallback state = new CameraDevice.StateCallback() {
            @Override public void onOpened(CameraDevice d) {
                device = d;
                if (cancelled || stopped) { stop(); return; }
                try {
                    image = ImageReader.newInstance(size.getWidth(), size.getHeight(), format, 2);
                    image.setOnImageAvailableListener(reader -> imageReady(reader), callbacks);
                    if (previewSize != null) {
                        sink = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.PRIVATE, 2);
                        sink.setOnImageAvailableListener(reader -> {
                            try (Image frame = reader.acquireLatestImage()) {
                                if (frame != null && !stopped && frame.getTimestamp() > lastTimestamp) {
                                    lastTimestamp = frame.getTimestamp();
                                    if (++frames == 3) capture();
                                }
                            } catch (Exception | OutOfMemoryError e) { fail(e.toString()); }
                        }, callbacks);
                    }
                    d.createCaptureSession(sink == null ? Arrays.asList(image.getSurface())
                            : Arrays.asList(sink.getSurface(), image.getSurface()), new CameraCaptureSession.StateCallback() {
                        @Override public void onConfigured(CameraCaptureSession s) {
                            session = s;
                            if (cancelled || stopped) { s.close(); stop(); return; }
                            try {
                                if (format == ImageFormat.JPEG && sink == null) { capture(); return; }
                                CaptureRequest.Builder req = d.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                                req.addTarget(sink == null ? image.getSurface() : sink.getSurface());
                                session.setRepeatingRequest(req.build(), null, callbacks);
                            } catch (Exception | OutOfMemoryError e) { fail(e.toString()); }
                        }
                        @Override public void onConfigureFailed(CameraCaptureSession s) { session = s; fail("Session configuration failed"); }
                    }, callbacks);
                } catch (Exception | OutOfMemoryError e) { fail(e.toString()); }
            }
            @Override public void onDisconnected(CameraDevice d) { device = d; fail("Camera disconnected"); }
            @Override public void onError(CameraDevice d, int code) { device = d; fail("Camera error " + code); }
            @Override public void onClosed(CameraDevice d) { device = null; noDevice(); }
        };
        void capture() {
            if (shutter || stopped || cancelled || session == null) return;
            shutter = true;
            try {
                CaptureRequest.Builder req = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                req.addTarget(image.getSurface());
                req.set(CaptureRequest.JPEG_QUALITY, (byte) 95);
                session.capture(req.build(), new CameraCaptureSession.CaptureCallback() {
                    @Override public void onCaptureFailed(CameraCaptureSession s, CaptureRequest r, CaptureFailure f) { fail("Still capture failed: " + f.getReason()); }
                }, callbacks);
            } catch (Exception | OutOfMemoryError e) { fail(e.toString()); }
        }
        void imageReady(ImageReader reader) {
            try (Image frame = reader.acquireLatestImage()) {
                if (frame == null || stopped || cancelled || bytes != null) return;
                if (format != ImageFormat.JPEG) {
                    if (frame.getTimestamp() <= lastTimestamp) return;
                    lastTimestamp = frame.getTimestamp();
                    if (++frames < 3) return;
                }
                if (format == ImageFormat.JPEG) {
                    ByteBuffer buffer = frame.getPlanes()[0].getBuffer();
                    if (buffer.remaining() <= 0 || buffer.remaining() > MAX_JPEG_BYTES) throw new IllegalStateException("JPEG outside byte bound");
                    bytes = new byte[buffer.remaining()]; buffer.get(bytes);
                } else bytes = yuvJpeg(frame);
                timestamp = frame.getTimestamp(); done.countDown();
            } catch (Exception | OutOfMemoryError e) { fail(e.toString()); }
        }
        void fail(String message) { error = message; done.countDown(); stop(); }
        void stop() {
            stopped = true;
            done.countDown();
            if (session != null) {
                try { session.close(); } catch (RuntimeException e) { error = e.toString(); }
                session = null;
            }
            if (device != null) {
                try { device.close(); } catch (RuntimeException e) { error = e.toString(); }
            }
            // If open is pending, onOpened/onError will close it. Keep the lease and readers until then.
        }
        void noDevice() {
            if (terminal) return;
            terminal = true;
            try { if (image != null) image.close(); } catch (RuntimeException e) { error = e.toString(); }
            try { if (sink != null) sink.close(); } catch (RuntimeException e) { error = e.toString(); }
            image = null; sink = null;
            done.countDown();
            boolean releaseNow = guard.closed();
            closed.countDown(); // Publish closure only after updating the exclusive-lease ledger.
            if (releaseNow) release();
        }
    }

    private static Size choose(Size[] sizes, Size nativeSize) {
        if (sizes == null) return null;
        int[][] pairs = new int[sizes.length][2];
        for (int i = 0; i < sizes.length; i++) { pairs[i][0] = sizes[i].getWidth(); pairs[i][1] = sizes[i].getHeight(); }
        int index = ProbeSizes.best(pairs, nativeSize == null ? 0 : nativeSize.getWidth(), nativeSize == null ? 0 : nativeSize.getHeight());
        return index < 0 ? null : sizes[index];
    }
    private static JSONArray sizesJson(Size[] sizes) {
        JSONArray list = new JSONArray();
        if (sizes != null) for (Size size : sizes) list.put(size.toString());
        return list;
    }
    private static byte[] yuvJpeg(Image image) throws Exception {
        int w = image.getWidth(), h = image.getHeight();
        if (!image.getCropRect().equals(new Rect(0, 0, w, h))) throw new IllegalStateException("Cropped YUV output unsupported");
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer[] buffers = new ByteBuffer[3]; int[] rows = new int[3], pixels = new int[3];
        for (int i = 0; i < 3; i++) {
            buffers[i] = planes[i].getBuffer(); rows[i] = planes[i].getRowStride(); pixels[i] = planes[i].getPixelStride();
        }
        byte[] nv21 = ProbeYuv.nv21(w, h, buffers, rows, pixels);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!new YuvImage(nv21, ImageFormat.NV21, w, h, null).compressToJpeg(new Rect(0, 0, w, h), 95, out))
            throw new IllegalStateException("YUV JPEG encoding failed");
        return out.toByteArray();
    }
}
