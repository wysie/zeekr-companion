package com.kooo.evcam.camera;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Surface;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Exercises acquisition/submission through the real production EGL encoder bridge. */
@RunWith(AndroidJUnit4.class)
public class EncoderFrameAndroidTest {
    @Test public void submittedPixelsUseNewlyAcquiredTimestampAndDuplicateCallbacksDoNotSubmit() throws Exception {
        HandlerThread events = new HandlerThread("encoder-test-events");
        events.start();
        Handler handler = new Handler(events.getLooper());
        ImageReader output = ImageReader.newInstance(128, 128, PixelFormat.RGBA_8888, 3);
        EglSurfaceEncoder renderer = new EglSurfaceEncoder("test", 128, 128);
        SurfaceTexture input = null;
        Surface producer = null;
        AtomicReference<CountDownLatch> frameReady = new AtomicReference<>();
        AtomicReference<CountDownLatch> imageReady = new AtomicReference<>();
        AtomicLong presented = new AtomicLong(-1);
        AtomicLong origin = new AtomicLong(-1);
        AtomicInteger mappings = new AtomicInteger();
        try {
            output.setOnImageAvailableListener(reader -> {
                try (Image image = reader.acquireLatestImage()) {
                    if (image != null) {
                        presented.set(image.getTimestamp());
                        CountDownLatch latch = imageReady.get();
                        if (latch != null) latch.countDown();
                    }
                }
            }, handler);
            int texture = renderer.initialize(output.getSurface());
            renderer.setFrameRate(0);
            input = new SurfaceTexture(texture);
            input.setDefaultBufferSize(128, 128);
            input.setOnFrameAvailableListener(st -> {
                CountDownLatch latch = frameReady.get();
                if (latch != null) latch.countDown();
            }, handler);
            renderer.setInputSurfaceTexture(input);
            producer = new Surface(input);
            long first = -1;
            for (int colour : new int[]{Color.RED, Color.GREEN}) {
                CountDownLatch source = new CountDownLatch(1), shown = new CountDownLatch(1);
                frameReady.set(source);
                imageReady.set(shown);
                long beforeAcquire = input.getTimestamp();
                Canvas canvas = producer.lockCanvas(null);
                canvas.drawColor(colour);
                producer.unlockCanvasAndPost(canvas);
                assertTrue("Producer frame arrived", source.await(5, TimeUnit.SECONDS));
                AtomicLong mapped = new AtomicLong(-1);
                assertTrue(renderer.drawFrame(acquired -> {
                    mappings.incrementAndGet();
                    mapped.set(acquired);
                    origin.compareAndSet(-1, acquired);
                    return 1_000_000_000L + acquired - origin.get();
                }));
                assertTrue("Output frame arrived", shown.await(5, TimeUnit.SECONDS));
                long acquired = input.getTimestamp();
                assertTrue("Fresh pixels must have a new source timestamp", acquired > beforeAcquire);
                assertEquals(acquired, mapped.get());
                assertEquals(1_000_000_000L + acquired - origin.get(), presented.get());
                if (first < 0) first = acquired;
                else assertTrue(acquired > first);
                assertFalse("A repeated callback cannot resubmit old pixels", renderer.drawFrame(ts -> {
                    fail("Duplicate pixels must not advance the recording timebase"); return ts;
                }));
            }
            assertEquals(2, mappings.get());
            input.release();
            input = null;
            assertThrows("Acquisition failure must reach recorder recovery, unlike a healthy skip",
                    IllegalStateException.class, () -> renderer.drawFrame(ts -> ts));
        } finally {
            if (producer != null) producer.release();
            if (input != null) input.release();
            renderer.release();
            output.close();
            events.quitSafely();
            events.join(2000);
        }
    }
}
