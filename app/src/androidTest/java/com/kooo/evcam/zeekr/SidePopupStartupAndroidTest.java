package com.kooo.evcam.zeekr;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.EGLExt;
import android.opengl.GLES20;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Size;
import android.view.ContextThemeWrapper;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.kooo.evcam.AppConfig;
import com.kooo.evcam.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

/** Disposable emulator only: draws synthetic buffers, never opens a vehicle camera. */
@RunWith(AndroidJUnit4.class)
public class SidePopupStartupAndroidTest {
    private void main(Runnable work) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(work);
    }

    private void await(String message, BooleanSupplier done) {
        long deadline = SystemClock.uptimeMillis() + 4000;
        while (!done.getAsBoolean() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(20);
        assertTrue(message, done.getAsBoolean());
    }

    private final class Fixture implements AutoCloseable {
        final SideViewPopupView view;
        final AppConfig config;
        final boolean oldStraighten;
        final String oldComposite = StreamLayoutTable.compositeCameraId();
        final AtomicReference<SurfaceTexture> texture = new AtomicReference<>();
        final AtomicReference<Float> firstFrameAlpha = new AtomicReference<>();
        final AtomicReference<Float> secondFrameAlpha = new AtomicReference<>();
        long lastTimestamp;
        int frames;
        Surface producer;
        EGLDisplay eglDisplay;
        EGLContext eglContext;
        EGLSurface eglSurface;
        final int sourceWidth, sourceHeight;
        int retainedPictureWidth, retainedOffsetX;

        Fixture() throws Exception {
            this(256, 1024);
        }

        Fixture(int sourceWidth, int sourceHeight) throws Exception {
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;
            Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
            try (ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .executeShellCommand("appops set " + target.getPackageName() + " SYSTEM_ALERT_WINDOW allow");
                 java.io.InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd)) {
                while (in.read() != -1) { /* Wait for the emulator permission command. */ }
            }
            Context context = new ContextThemeWrapper(target, R.style.Theme_Cam);
            config = new AppConfig(context);
            oldStraighten = config.isSidePopupStraighten();
            config.setSidePopupStraighten(false);
            StreamLayoutTable.setCompositeCameraId("synthetic-test");
            AtomicReference<SideViewPopupView> created = new AtomicReference<>();
            main(() -> {
                SideViewPopupView popup = new SideViewPopupView(context, config) {
                    @Override protected int cameraPictureWidth() {
                        return retainedPictureWidth > 0 ? retainedPictureWidth : getWidth();
                    }
                    @Override protected int cameraPictureOffsetX() { return retainedOffsetX; }
                };
                popup.setSourceSize(new Size(sourceWidth, sourceHeight));
                popup.getTextureView().setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    @Override public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
                        st.setDefaultBufferSize(sourceWidth, sourceHeight);
                        texture.set(st);
                    }
                    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {
                        // Match the production service's restoreBufferSize on resize.
                        st.setDefaultBufferSize(sourceWidth, sourceHeight);
                    }
                    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture st) { return true; }
                    @Override public void onSurfaceTextureUpdated(SurfaceTexture st) {
                        popup.noteFrame();
                        long timestamp = st.getTimestamp();
                        android.util.Log.d("PopupStartupTest", "Updated timestamp=" + timestamp);
                        if (timestamp > 0 && timestamp != lastTimestamp) {
                            lastTimestamp = timestamp;
                            frames++;
                            float alpha = ((WindowManager.LayoutParams) popup.getLayoutParams()).alpha;
                            if (frames == 1) firstFrameAlpha.set(alpha);
                            if (frames == 2) secondFrameAlpha.set(alpha);
                        }
                    }
                });
                created.set(popup);
                popup.attachHidden(LaneCycle.LEFT);
            });
            view = created.get();
            await("Transparent TextureView must become available", () -> texture.get() != null);
        }

        float alpha() {
            AtomicReference<Float> result = new AtomicReference<>();
            main(() -> result.set(((WindowManager.LayoutParams) view.getLayoutParams()).alpha));
            return result.get();
        }

        void frame(int color) {
            // Canvas-posted emulator buffers have timestamp 0. Use EGL presentation
            // timestamps to exercise the same progression check as a camera producer.
            if (producer == null) {
                producer = new Surface(texture.get());
                eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
                assertTrue(EGL14.eglInitialize(eglDisplay, new int[2], 0, new int[2], 0));
                EGLConfig[] configs = new EGLConfig[1];
                assertTrue(EGL14.eglChooseConfig(eglDisplay, new int[] {
                        EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                        EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL14.EGL_NONE
                }, 0, configs, 0, 1, new int[1], 0));
                eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT,
                        new int[] {EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE}, 0);
                eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], producer,
                        new int[] {EGL14.EGL_NONE}, 0);
                assertTrue(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext));
            }
            GLES20.glClearColor(Color.red(color) / 255f, Color.green(color) / 255f,
                    Color.blue(color) / 255f, 1f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            assertTrue(EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, System.nanoTime()));
            assertTrue(EGL14.eglSwapBuffers(eglDisplay, eglSurface));
        }

        void fineHorizontalBars() {
            if (producer == null) frame(Color.BLACK);
            int[] dimensions = new int[1];
            assertTrue(EGL14.eglQuerySurface(eglDisplay, eglSurface, EGL14.EGL_WIDTH, dimensions, 0));
            assertEquals(sourceWidth, dimensions[0]);
            assertTrue(EGL14.eglQuerySurface(eglDisplay, eglSurface, EGL14.EGL_HEIGHT, dimensions, 0));
            assertEquals(sourceHeight, dimensions[0]);
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            GLES20.glClearColor(0, 0, 0, 1);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
            GLES20.glClearColor(1, 1, 1, 1);
            // Four native rows white, four black. Reducing the whole strip to
            // popup size before taking a lane cannot preserve this detail.
            for (int y = 0; y < sourceHeight; y += 8) {
                GLES20.glScissor(0, sourceHeight - y - 4, sourceWidth, 4);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            }
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            assertTrue(EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, System.nanoTime()));
            assertTrue(EGL14.eglSwapBuffers(eglDisplay, eglSurface));
        }

        void verticalBars() {
            if (producer == null) frame(Color.BLACK);
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            GLES20.glClearColor(0, 0, 0, 1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
            GLES20.glClearColor(1, 1, 1, 1);
            for (int x=0;x<sourceWidth;x+=16) {
                GLES20.glScissor(x,0,8,sourceHeight); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            }
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            assertTrue(EGLExt.eglPresentationTimeANDROID(eglDisplay,eglSurface,System.nanoTime()));
            assertTrue(EGL14.eglSwapBuffers(eglDisplay,eglSurface));
        }

        @Override public void close() {
            if (producer != null) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
                EGL14.eglDestroySurface(eglDisplay, eglSurface);
                EGL14.eglDestroyContext(eglDisplay, eglContext);
                EGL14.eglTerminate(eglDisplay);
                EGL14.eglReleaseThread();
                producer.release();
            }
            main(view::detach);
            config.setSidePopupStraighten(oldStraighten);
            StreamLayoutTable.setCompositeCameraId(oldComposite);
        }
    }

    @Test public void innerCropRetainsNativePixelsForBothEdgesRawAndCorrected() throws Exception {
        for(boolean corrected:new boolean[]{false,true}) for(boolean right:new boolean[]{false,true}) {
            try(Fixture f=new Fixture(1280,5140)) {
                int oldFov=f.config.getSidePopupFov(),oldYaw=f.config.getSidePopupYaw();
                int oldPitch=f.config.getSidePopupPitch(),oldRoll=f.config.getSidePopupRotation(LaneCycle.LEFT);
                boolean oldMirror=f.config.isSidePopupMirrored();
                Bitmap before=null,after=null;
                try {
                    f.config.setSidePopupStraighten(corrected); f.config.setSidePopupFov(90);
                    f.config.setSidePopupYaw(0); f.config.setSidePopupPitch(0);
                    f.config.setSidePopupRotation(LaneCycle.LEFT,30); f.config.setSidePopupMirrored(true);
                    main(()->{f.view.applyConfig();f.view.show(LaneCycle.LEFT);});
                    for(int n=0;n<12;n++){f.verticalBars();SystemClock.sleep(30);}
                    int[] rect=new int[4];
                    main(()->{f.view.getLocationOnScreen(rect);rect[2]=f.view.getWidth();rect[3]=f.view.getHeight();});
                    before=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                    assertNotNull(before);
                    SurfaceTexture original=f.view.getTextureView().getSurfaceTexture();
                    int trim=rect[2]/4,offset=right?trim:0;
                    main(()->{
                        f.retainedPictureWidth=rect[2];f.retainedOffsetX=offset;
                        WindowManager.LayoutParams params=(WindowManager.LayoutParams)f.view.getLayoutParams();
                        params.width=rect[2]-trim;
                        ((WindowManager)f.view.getContext().getSystemService(Context.WINDOW_SERVICE))
                                .updateViewLayout(f.view,params);
                    });
                    for(int n=0;n<10;n++){f.verticalBars();SystemClock.sleep(30);}
                    int[] cropped=new int[4];
                    main(()->{
                        f.view.getLocationOnScreen(cropped);cropped[2]=f.view.getWidth();cropped[3]=f.view.getHeight();
                        assertEquals(rect[2]-trim,cropped[2]);assertEquals(rect[3],cropped[3]);
                        assertEquals(rect[2],f.view.getTextureView().getWidth());
                        assertSame("Cropping must keep the camera consumer",original,f.view.getTextureView().getSurfaceTexture());
                    });
                    after=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                    assertNotNull(after);
                    int samples=0,matched=0,light=0,dark=0;
                    for(int y=cropped[3]/3;y<cropped[3]*2/3;y+=5) for(int x=12;x<cropped[2]-12;x+=3) {
                        int expected=Color.red(before.getPixel(rect[0]+x+offset,rect[1]+y));
                        int actual=Color.red(after.getPixel(cropped[0]+x,cropped[1]+y));
                        if(Math.abs(expected-actual)<20) matched++;
                        if(actual>180) light++; if(actual<70) dark++; samples++;
                    }
                    assertTrue("Cropped picture must retain detail",light>samples/10&&dark>samples/10);
                    assertTrue("Crop must preserve original pixel scale: corrected="+corrected+" right="+right
                            +" matched="+matched+"/"+samples,matched>samples*.9);
                } finally {
                    if(before!=null)before.recycle();if(after!=null)after.recycle();
                    f.config.setSidePopupFov(oldFov);f.config.setSidePopupYaw(oldYaw);f.config.setSidePopupPitch(oldPitch);
                    f.config.setSidePopupRotation(LaneCycle.LEFT,oldRoll);f.config.setSidePopupMirrored(oldMirror);
                }
            }
        }
    }

    @Test public void nativeStripDetailSurvivesCorrectedPopupCrop() throws Exception {
        try (Fixture f = new Fixture(1280, 5140)) {
            int oldFov = f.config.getSidePopupFov(), oldYaw = f.config.getSidePopupYaw();
            int oldPitch = f.config.getSidePopupPitch(), oldRoll = f.config.getSidePopupRotation(LaneCycle.LEFT);
            int oldSize = f.config.getSidePopupSizePercent();
            boolean oldMirrored = f.config.isSidePopupMirrored();
            try {
                f.config.setSidePopupStraighten(true);
                f.config.setSidePopupFov(90);
                f.config.setSidePopupYaw(0);
                f.config.setSidePopupPitch(0);
                f.config.setSidePopupRotation(LaneCycle.LEFT, 0);
                f.config.setSidePopupSizePercent(30);
                f.config.setSidePopupMirrored(false);
                main(() -> { f.view.applyConfig(); f.view.show(LaneCycle.LEFT); });
                for (int i = 0; i < 15; i++) { f.fineHorizontalBars(); SystemClock.sleep(30); }
                int[] rect = new int[4];
                main(() -> {
                    f.view.getLocationOnScreen(rect);
                    rect[2] = f.view.getWidth(); rect[3] = f.view.getHeight();
                    assertEquals(1f, f.view.getAlpha(), .001f);
                });
                Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                assertNotNull(screenshot);
                try {
                    SideViewFraming map = new SideViewFraming();
                    map.configure(90, 0, 0, LaneCycle.LEFT, (float) rect[2] / rect[3]);
                    float[] uv = new float[2];
                    int samples = 0, matched = 0, low = 255, high = 0;
                    int x = rect[2] / 2;
                    for (int y = rect[3] / 4; y < rect[3] * 3 / 4; y++) {
                        map.sourcePoint((x + .5f) / rect[2], (y + .5f) / rect[3], uv);
                        double row = 2572 + uv[1] * 1280;
                        double phase = row % 8;
                        // Stay away from transitions and bilinear edge samples.
                        if (phase < 1 || phase > 7 || Math.abs(phase - 4) < 1) continue;
                        int red = Color.red(screenshot.getPixel(rect[0] + x, rect[1] + y));
                        low = Math.min(low, red); high = Math.max(high, red);
                        if ((phase < 4 && red > 170) || (phase >= 4 && red < 85)) matched++;
                        samples++;
                    }
                    android.util.Log.i("NativeSideSampling", "popup=" + rect[2] + "x" + rect[3]
                            + " native=1280x5140 matched=" + matched + "/" + samples
                            + " range=" + low + ".." + high);
                    assertTrue("Need enough native-detail samples", samples > 40);
                    assertTrue("Fine native bars must keep contrast", high - low > 160);
                    assertTrue("Output must match native source rows, not a small intermediate copy",
                            matched >= samples * .9);
                } finally { screenshot.recycle(); }
            } finally {
                f.config.setSidePopupFov(oldFov); f.config.setSidePopupYaw(oldYaw);
                f.config.setSidePopupPitch(oldPitch); f.config.setSidePopupRotation(LaneCycle.LEFT, oldRoll);
                f.config.setSidePopupSizePercent(oldSize); f.config.setSidePopupMirrored(oldMirrored);
            }
        }
    }

    @Test public void transparentPopupDrainsFramesAndRevealsOnProgression() throws Exception {
        try (Fixture f = new Fixture()) {
            main(() -> {
                f.view.show(LaneCycle.LEFT);
                assertTrue("Requested startup counts as showing for dismissal/routing", f.view.isShowing());
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) f.view.getLayoutParams();
                assertEquals(0f, params.alpha, 0f);
                assertTrue((params.flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0);
            });
            // RenderThread can latch a buffer after the UI callback; emulate a
            // continuous camera rather than waiting for one isolated buffer.
            for (int i = 0; i < 12 && f.secondFrameAlpha.get() == null; i++) {
                f.frame(i % 2 == 0 ? Color.RED : Color.BLUE);
                SystemClock.sleep(40);
            }
            await("Advancing buffers must drain while transparent", () -> f.firstFrameAlpha.get() != null);
            assertEquals(0f, f.firstFrameAlpha.get(), 0f);
            await("Second advancing buffer must arrive", () -> f.secondFrameAlpha.get() != null);
            assertEquals(1f, f.secondFrameAlpha.get(), 0f);
            SystemClock.sleep(CameraFrameFreshness.LIMIT_MS + 120);
            assertEquals("Stall stays visible for the stale cover", 1f, f.alpha(), 0f);
        }
    }

    @Test public void delayedCameraGetsWaitingWindowButCancellationStaysHidden() throws Exception {
        try (Fixture f = new Fixture()) {
            main(() -> f.view.show(LaneCycle.LEFT));
            assertEquals(0f, f.alpha(), 0f);
            await("Waiting UI must appear after startup deadline", () -> f.alpha() == 1f);
            main(() -> f.view.conceal());
            await("Fade-out finishes hidden", () -> f.alpha() == 0f);
            main(() -> {
                f.view.show(LaneCycle.RIGHT);
                f.view.conceal();
            });
            SystemClock.sleep(SidePopupStartup.WAIT_NOTICE_MS + 150);
            assertEquals("Cancelled request must not reappear", 0f, f.alpha(), 0f);
        }
    }
    @Test public void warmReactivationFadesAndSideSwitchRequiresNewSourceFrames() throws Exception {
        try (Fixture f = new Fixture()) {
            main(() -> f.view.show(LaneCycle.LEFT));
            for (int i = 0; i < 12; i++) { f.frame(Color.RED); SystemClock.sleep(30); }
            assertEquals(1f, f.alpha(), 0f);
            main(() -> { assertEquals(1f, f.view.getAlpha(), .02f); f.view.conceal(); });
            await("Window hides after fade", () -> f.alpha() == 0f);
            for (int i = 0; i < 3; i++) { f.frame(Color.BLUE); SystemClock.sleep(30); }
            main(() -> {
                assertEquals(SideViewPopupView.IDLE_ALPHA, f.view.getAlpha(), .001f);
                f.view.show(LaneCycle.LEFT);
                assertEquals(1f, ((WindowManager.LayoutParams) f.view.getLayoutParams()).alpha, 0f);
                assertTrue(f.view.getAlpha() < 1f);
            });
            for (int i = 0; i < 7; i++) { f.frame(Color.BLUE); SystemClock.sleep(30); }
            main(() -> {
                assertEquals(1f, f.view.getAlpha(), .02f);
                f.view.show(LaneCycle.RIGHT);
                assertEquals("Old side remains hidden", 0f,
                        ((WindowManager.LayoutParams) f.view.getLayoutParams()).alpha, 0f);
            });
            for (int i = 0; i < 9; i++) { f.frame(Color.GREEN); SystemClock.sleep(30); }
            assertEquals(1f, f.alpha(), 0f);
            main(() -> {
                f.view.conceal();
                f.view.detach();
                assertFalse(f.view.isAttached());
            });
            SystemClock.sleep(250);
            main(() -> assertFalse("Detached animation must not reopen", f.view.isAttached()));
        }
    }

    @Test public void popupHidesMirrorOnlyAfterRevealAndRestoresWithoutLosingItsSurface() throws Exception {
        try (Fixture f = new Fixture()) {
            Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
            int oldMode = f.config.getSidePopupMirrorMode();
            boolean oldEnabled = f.config.isRearViewEnabled();
            AtomicReference<RearViewMirrorView> mirror = new AtomicReference<>();
            try {
                f.config.setSidePopupMirrorMode(SidePopupMirrorMode.HIDE);
                main(() -> {
                    RearViewMirrorView view = new RearViewMirrorView(new ContextThemeWrapper(target, R.style.Theme_Cam), f.config);
                    mirror.set(view);
                    view.show();
                    f.view.setWindowVisibilityListener(() -> view.setHiddenForSidePopup(
                            SidePopupMirrorMode.hideMirror(f.config.getSidePopupMirrorMode(), f.view.isPresented())));
                    f.view.show(LaneCycle.LEFT);
                    assertEquals(1f, ((WindowManager.LayoutParams) view.getLayoutParams()).alpha, 0f);
                });
                await("Mirror texture available", () -> mirror.get().getTextureView().isAvailable());
                SurfaceTexture original = mirror.get().getTextureView().getSurfaceTexture();
                for (int i = 0; i < 12 && f.alpha() == 0f; i++) { f.frame(Color.BLUE); SystemClock.sleep(40); }
                await("Popup presented", () -> f.alpha() == 1f);
                main(() -> {
                    assertTrue(f.view.isPresented());
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) mirror.get().getLayoutParams();
                    assertEquals(0f, params.alpha, 0f);
                    assertTrue((params.flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0);
                    assertTrue(mirror.get().isShowing());
                    assertSame(original, mirror.get().getTextureView().getSurfaceTexture());
                    assertEquals(oldEnabled, f.config.isRearViewEnabled());
                    f.view.conceal();
                });
                await("Popup fade finished", () -> f.alpha() == 0f);
                main(() -> {
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) mirror.get().getLayoutParams();
                    assertEquals(1f, params.alpha, 0f);
                    assertEquals(0, params.flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    assertSame(original, mirror.get().getTextureView().getSurfaceTexture());
                    f.view.show(LaneCycle.LEFT);
                });
                await("Popup waiting notice presented", () -> f.alpha() == 1f);
                main(() -> {
                    f.view.detach();
                    assertEquals(1f, ((WindowManager.LayoutParams) mirror.get().getLayoutParams()).alpha, 0f);
                    f.config.resetSidePopupSettings();
                    assertEquals(SidePopupMirrorMode.REPLACE, f.config.getSidePopupMirrorMode());
                    f.config.setSidePopupMirrorMode(SidePopupMirrorMode.ALONGSIDE);
                    assertFalse(f.config.isSidePopupReplaceMirror());
                });
            } finally {
                main(() -> { f.view.detach(); if (mirror.get() != null) mirror.get().hide(); });
                f.config.setSidePopupMirrorMode(oldMode);
            }
        }
    }

}
