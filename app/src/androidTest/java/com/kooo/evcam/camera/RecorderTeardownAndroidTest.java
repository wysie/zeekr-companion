package com.kooo.evcam.camera;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.telemetry.InfoBarRenderer;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RecorderTeardownAndroidTest {
    @Test public void timedOutReleaseKeepsFrameResourcesUntilEncoderTaskReturns() throws Exception {
        CodecVideoRecorder recorder = new CodecVideoRecorder("teardown-test", 128, 128);
        HandlerThread encoder = new HandlerThread("held-encoder-test");
        encoder.start();
        Handler handler = new Handler(encoder.getLooper());
        SurfaceTexture input = new SurfaceTexture(false);
        Surface surface = new Surface(input);
        InfoBarRenderer bar = new InfoBarRenderer(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), 128);
        set(recorder, "encoderThread", encoder);
        set(recorder, "encoderHandler", handler);
        set(recorder, "inputSurfaceTexture", input);
        set(recorder, "recordSurface", surface);
        recorder.setInfoBar(bar);
        CountDownLatch entered = new CountDownLatch(1), resume = new CountDownLatch(1);
        AtomicBoolean ownedUntilReturn = new AtomicBoolean();
        handler.post(() -> {
            entered.countDown();
            try {
                resume.await();
                ownedUntilReturn.set(!input.isReleased() && !bar.bitmap().isRecycled() && surface.isValid());
            } catch (InterruptedException failure) { Thread.currentThread().interrupt(); }
        });
        Thread release = new Thread(recorder::release, "release-recorder-test");
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            release.start();
            release.join(6000);
            assertFalse("Release remains bounded while encoder is blocked", release.isAlive());
            recorder.release(); // Idempotent even while the first teardown remains queued.
            assertFalse("Input still belongs to in-flight frame", input.isReleased());
            assertFalse("Overlay still belongs to in-flight frame", bar.bitmap().isRecycled());
            assertTrue(surface.isValid());
            resume.countDown();
            encoder.join(5000);
            assertFalse(encoder.isAlive());
            assertTrue(ownedUntilReturn.get());
            assertTrue(input.isReleased());
            assertTrue(bar.bitmap().isRecycled());
            assertFalse(surface.isValid());
        } finally {
            resume.countDown();
            release.join(6000);
            recorder.release();
            encoder.quitSafely();
            encoder.join(5000);
            surface.release();
            input.release();
            bar.recycle();
        }
    }

    @Test public void releaseBeforePrepareStillRecyclesConfiguredOverlay() {
        CodecVideoRecorder recorder = new CodecVideoRecorder("unprepared-test", 128, 128);
        InfoBarRenderer bar = new InfoBarRenderer(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), 128);
        recorder.setInfoBar(bar);
        recorder.release();
        recorder.release();
        assertTrue(bar.bitmap().isRecycled());
    }

    private static void set(Object object, String name, Object value) throws Exception {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }
}
