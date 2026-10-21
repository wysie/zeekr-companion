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
import android.view.View;
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
        boolean gpuRoute;
        java.util.Map<String,?> gpuBefore;
        void enableGpu(){
            if (gpuBefore == null) gpuBefore=InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("app_config",0).getAll();
            config.setGpuRenderingMode("custom");config.setGpuFeature("side",true);gpuRoute=true;
        }
        EGLDisplay eglDisplay;
        EGLContext eglContext;
        EGLSurface eglSurface;
        final int sourceWidth, sourceHeight;
        int retainedPictureWidth, retainedOffsetX;
        android.hardware.display.VirtualDisplay hudDisplay;
        android.media.ImageReader hudImages;
        android.os.HandlerThread hudConsumer;
        final AtomicReference<android.media.Image> hudImage = new AtomicReference<>();

        Fixture() throws Exception {
            this(256, 1024);
        }

        Fixture(int sourceWidth, int sourceHeight) throws Exception { this(sourceWidth, sourceHeight, false); }

        Fixture(int sourceWidth, int sourceHeight, boolean hud) throws Exception {
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
            if (hud) {
                gpuBefore = target.getSharedPreferences("app_config", 0).getAll();
                config.selectHudLayout(true); config.setHudSideSizePercent(50); config.setHudSideShapePercent(100);
                config.setHudSideLeftPercent(94); config.setHudSideVerticalPercent(50);
                config.setHudSideInnerCropPercent(25); config.setHudSideFollowSignal(false);
                config.setHudSideContrastPercent(100); config.setHudSideFadePercent(0);
                config.setHudSideOpacityPercent(100); config.setHudFollowingPopupPicture(true);
                config.setRearViewCameraLabelEnabled(true);
                hudImages = android.media.ImageReader.newInstance(1280, 640, android.graphics.PixelFormat.RGBA_8888, 3);
                hudConsumer = new android.os.HandlerThread("hud-gpu-badges"); hudConsumer.start();
                hudImages.setOnImageAvailableListener(reader -> {
                    android.media.Image next = reader.acquireLatestImage();
                    if (next != null) {
                        android.media.Image previous = hudImage.getAndSet(next); if (previous != null) previous.close();
                    }
                }, new android.os.Handler(hudConsumer.getLooper()));
                android.hardware.display.DisplayManager displays = (android.hardware.display.DisplayManager)
                        target.getSystemService(Context.DISPLAY_SERVICE);
                hudDisplay = displays.createVirtualDisplay("Disposable GPU HUD", 1280, 640, 160,
                        hudImages.getSurface(), android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                        | android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
                        | android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
                assertNotNull(hudDisplay);
            }
            config.setSidePopupStraighten(false);
            StreamLayoutTable.setCompositeCameraId("synthetic-test");
            AtomicReference<SideViewPopupView> created = new AtomicReference<>();
            main(() -> {
                SideViewPopupView popup = hud ? new SideViewHudPanel(context, config, hudDisplay.getDisplay())
                        : new SideViewPopupView(context, config) {
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
                        if(!PreviewDewarp.ownsOutput(st))st.setDefaultBufferSize(sourceWidth, sourceHeight);
                    }
                    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture st) { return true; }
                    @Override public void onSurfaceTextureUpdated(SurfaceTexture st) {
                        popup.noteFrame();
                        long timestamp = st.getTimestamp();
                        android.util.Log.d("PopupStartupTest", "Updated timestamp=" + timestamp);
                        if (timestamp > 0 && timestamp != lastTimestamp) {
                            lastTimestamp = timestamp;
                            frames++;
                            float alpha = hud ? 1f : ((WindowManager.LayoutParams) popup.getLayoutParams()).alpha;
                            if (hud) ((SideViewHudPanel) popup).setCover(SideViewHudPanel.COVER_NONE);
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
                producer = gpuRoute ? PreviewDewarp.optionalViewSurfaceFor(view.getContext(),texture.get(),new Size(sourceWidth,sourceHeight),true) : new Surface(texture.get());
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

        Bitmap hudSnapshot() {
            await("HUD display composed an image", () -> hudImage.get() != null);
            try (android.media.Image image = hudImage.getAndSet(null)) {
                assertNotNull(image);
                android.media.Image.Plane plane = image.getPlanes()[0];
                java.nio.ByteBuffer buffer = plane.getBuffer();
                Bitmap result = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
                int[] row = new int[image.getWidth()];
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < row.length; x++) {
                        int at = y * plane.getRowStride() + x * plane.getPixelStride();
                        row[x] = Color.argb(buffer.get(at + 3) & 255, buffer.get(at) & 255,
                                buffer.get(at + 1) & 255, buffer.get(at + 2) & 255);
                    }
                    result.setPixels(row, 0, row.length, 0, y, row.length, 1);
                }
                return result;
            }
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
            FisheyeGlPipe pipe=PreviewDewarp.rawPipeFor(view.getTextureView());if(pipe!=null)pipe.release();
            main(view::detach);
            if (hudDisplay != null) hudDisplay.release();
            if (hudImages != null) hudImages.setOnImageAvailableListener(null, null);
            if (hudConsumer != null) {
                hudConsumer.quitSafely();
                try { hudConsumer.join(1000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            if (hudImages != null) {
                android.media.Image last = hudImage.getAndSet(null); if (last != null) last.close(); hudImages.close();
            }
            config.setSidePopupStraighten(oldStraighten);
            if(gpuBefore!=null){
                android.content.SharedPreferences.Editor edit=InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("app_config",0).edit().clear();
                for(java.util.Map.Entry<String,?> e:gpuBefore.entrySet()){
                    Object v=e.getValue();String k=e.getKey();if(v instanceof Boolean)edit.putBoolean(k,(Boolean)v);
                    else if(v instanceof String)edit.putString(k,(String)v);else if(v instanceof Integer)edit.putInt(k,(Integer)v);
                    else if(v instanceof Long)edit.putLong(k,(Long)v);else if(v instanceof Float)edit.putFloat(k,(Float)v);
                    else if(v instanceof java.util.Set)edit.putStringSet(k,new java.util.HashSet<>((java.util.Set<String>)v));
                }edit.commit();
            }
            StreamLayoutTable.setCompositeCameraId(oldComposite);
        }
    }

    @Test public void calibratedHudFitsBothSidesAndCompleteBadgesWithoutReplacingTheProducer() throws Exception {
        for(boolean gpu:new boolean[]{false,true}) try(Fixture f=new Fixture(1280,5140,true)) {
            if(gpu){f.enableGpu();f.config.setGpuRenderingMode("all");}else f.config.setGpuRenderingMode("off");
            f.config.setHudSideFollowSignal(true);f.config.setHudSideLeftPercent(0);
            f.config.setSidePopupStraighten(true);f.config.setSidePopupYaw(40);f.config.setSidePopupMirrored(true);
            HudSafeArea.reset(f.view.getContext());
            main(()->{f.view.applyConfig();f.view.show(LaneCycle.LEFT);f.view.setIndicatorDisplay(null);});
            for(int n=0;n<12;n++){f.frame(0xff203050);SystemClock.sleep(40);}
            SurfaceTexture original=f.texture.get();
            java.util.concurrent.atomic.AtomicReference<org.json.JSONObject> record=new java.util.concurrent.atomic.AtomicReference<>();
            main(()->{try{
                record.set(HudSafeArea.record(HudSafeArea.frame(f.hudDisplay.getDisplay(),(android.view.View)f.view.getParent()),500,780,2));
            }catch(Exception e){throw new AssertionError(e);}});
            assertTrue(HudSafeArea.save(f.view.getContext(),record.get()));
            for(int lane:new int[]{LaneCycle.LEFT,LaneCycle.RIGHT}) {
                main(()->{f.view.applyConfig();f.view.show(lane);});
                for(int n=0;n<12;n++){f.frame(0xff203050);SystemClock.sleep(40);}
                int[] area=new int[4];main(()->{
                    area[0]=f.view.getLeft();area[1]=f.view.getTop();area[2]=f.view.getWidth();area[3]=f.view.getHeight();
                    assertTrue(area[0]>=526&&area[0]+area[2]<=754);assertEquals(228,area[2]);
                    assertEquals(gpu?"GPU":"compatibility",f.view.gpuState());
                    assertSame(original,f.view.getTextureView().getSurfaceTexture());
                    assertEquals("Keep body-aware crop proportion",.75f,area[2]/(float)f.view.cameraPictureWidth(),.01f);
                });
                Bitmap actual=f.hudSnapshot(),expected=Bitmap.createBitmap(area[2],area[3],Bitmap.Config.ARGB_8888);
                try{
                    android.graphics.Canvas canvas=new android.graphics.Canvas(expected);
                    new CameraLabel(f.view.getContext(),false).drawBottom(canvas,lane,area[2],area[3],lane==LaneCycle.LEFT);
                    new SideTurnArrow(f.view.getContext(),false).draw(canvas,lane,null,area[2],area[3],false);
                    int white=0,whiteMatch=0,green=0,greenMatch=0;
                    for(int y=0;y<area[3];y++)for(int x=0;x<area[2];x++){
                        int e=expected.getPixel(x,y),a=actual.getPixel(area[0]+x,area[1]+y);
                        if(Color.alpha(e)>240&&Color.red(e)>210&&Color.green(e)>210&&Color.blue(e)>210){
                            white++;if(Color.red(a)>190&&Color.green(a)>190&&Color.blue(a)>190)whiteMatch++;
                        }
                        if(Color.green(e)>Color.red(e)+60&&Color.green(e)>Color.blue(e)+60){
                            green++;if(Color.green(a)>Color.red(a)+40&&Color.green(a)>Color.blue(a)+40)greenMatch++;
                        }
                    }
                    assertTrue("Calibrated label complete",white>20&&whiteMatch>white*.95);
                    assertTrue("Calibrated arrow complete",green>20&&greenMatch>green*.95);
                }finally{actual.recycle();expected.recycle();}
            }
            assertTrue(HudSafeArea.reset(f.view.getContext()));main(f.view::applyConfig);SystemClock.sleep(180);
            main(()->{assertEquals(240,f.view.getWidth());assertSame(original,f.view.getTextureView().getSurfaceTexture());});
            assertTrue(HudSafeArea.save(f.view.getContext(),record.get()));
            main(()->((android.view.View)f.view.getParent()).setLayoutParams(new android.widget.FrameLayout.LayoutParams(640,360)));
            SystemClock.sleep(180);
            main(()->{assertTrue(SideViewHudPanel.lastState(),SideViewHudPanel.lastState().contains("not applied"));
                assertTrue(f.view.getLeft()>=0&&f.view.getRight()<=640);assertSame(original,f.view.getTextureView().getSurfaceTexture());});
        }
    }

    @Test public void hudBodyEdgeAndBadgesSurviveCropOnBothRenderers() throws Exception {
        for(boolean gpu:new boolean[]{false,true}) try(Fixture f=new Fixture(1280,5140,true)) {
            if(gpu){f.enableGpu();f.config.setGpuRenderingMode("all");}else f.config.setGpuRenderingMode("off");
            f.config.setSidePopupStraighten(true);f.config.setSidePopupFov(90);f.config.setSidePopupYaw(40);f.config.setSidePopupPitch(15);
            f.config.setSidePopupRotation(LaneCycle.LEFT,30);f.config.setSidePopupRotation(LaneCycle.RIGHT,-30);
            f.config.setSideUpscaling("bicubic_sharp");f.config.setSideSharpening(25);
            for(boolean mirrored:new boolean[]{false,true}) for(int lane:new int[]{LaneCycle.LEFT,LaneCycle.RIGHT}) {
                f.config.setSidePopupMirrored(mirrored);f.config.setHudSideInnerCropPercent(0);
                main(()->{f.view.applyConfig();f.view.show(lane);f.view.setIndicatorDisplay(null);});
                for(int n=0;n<16;n++){f.verticalBars();SystemClock.sleep(40);}
                SurfaceTexture input=f.view.getTextureView().getSurfaceTexture();
                int[] full=new int[4];main(()->{full[0]=f.view.getLeft();full[1]=f.view.getTop();full[2]=f.view.getWidth();full[3]=f.view.getHeight();});
                Bitmap before=f.hudSnapshot();
                try {
                    for(int crop:new int[]{25,60}) {
                        f.config.setHudSideInnerCropPercent(crop);main(f.view::applyConfig);
                        for(int n=0;n<12;n++){f.verticalBars();SystemClock.sleep(40);}
                        int[] area=new int[4];
                        main(()->{area[0]=f.view.getLeft();area[1]=f.view.getTop();area[2]=f.view.getWidth();area[3]=f.view.getHeight();
                            assertEquals(full[2],f.view.getTextureView().getWidth());assertSame(input,f.view.getTextureView().getSurfaceTexture());
                            assertEquals(gpu?"GPU":"compatibility",f.view.gpuState());
                            assertEquals(HudPanelPlacement.keepsRightEdge(lane,mirrored)?full[2]-area[2]:0,f.view.cameraPictureOffsetX());});
                        int offset=(lane==LaneCycle.LEFT)==mirrored?full[2]-area[2]:0;
                        Bitmap cropped=f.hudSnapshot();
                        try {
                            int checked=0,matched=0;
                            for(int y=90;y<area[3]-70;y+=3)for(int x=8;x<area[2]-8;x+=3) {
                                int expected=Color.red(before.getPixel(full[0]+offset+x,full[1]+y));
                                int actual=Color.red(cropped.getPixel(area[0]+x,area[1]+y));
                                checked++;if(Math.abs(expected-actual)<20)matched++;
                            }
                            assertTrue("Retain body-side pixels at original scale: gpu="+gpu+" lane="+lane+" mirror="+mirrored
                                    +" crop="+crop+" matched="+matched+"/"+checked,checked>100&&matched>checked*.9);
                        }finally{cropped.recycle();}
                        for(int n=0;n<12;n++){f.frame(0xff203050);SystemClock.sleep(40);}
                        Bitmap actual=f.hudSnapshot(),expected=Bitmap.createBitmap(area[2],area[3],Bitmap.Config.ARGB_8888);
                        try {
                            android.graphics.Canvas canvas=new android.graphics.Canvas(expected);
                            new CameraLabel(f.view.getContext(),false).drawBottom(canvas,lane,area[2],area[3],(lane==LaneCycle.LEFT)==mirrored);
                            new SideTurnArrow(f.view.getContext(),false).draw(canvas,lane,null,area[2],area[3],false);
                            int white=0,whiteMatched=0,green=0,greenMatched=0;
                            for(int y=0;y<area[3];y++)for(int x=0;x<area[2];x++) {
                                int e=expected.getPixel(x,y),a=actual.getPixel(area[0]+x,area[1]+y);
                                if(Color.alpha(e)>240&&Color.red(e)>210&&Color.green(e)>210&&Color.blue(e)>210) {
                                    white++;if(Color.red(a)>190&&Color.green(a)>190&&Color.blue(a)>190)whiteMatched++;
                                }
                                if(Color.green(e)>Color.red(e)+60&&Color.green(e)>Color.blue(e)+60) {
                                    green++;if(Color.green(a)>Color.red(a)+40&&Color.green(a)>Color.blue(a)+40)greenMatched++;
                                }
                            }
                            assertTrue("Complete direction text, lane="+lane+" mirror="+mirrored+" crop="+crop,white>20&&whiteMatched>white*.95);
                            assertTrue("Complete turn arrow",green>20&&greenMatched>green*.95);
                            if(mirrored&&crop==25)try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(
                                    f.view.getContext().getExternalFilesDir(null),"hud-body-trim-"+gpu+"-"+lane+".png"))){actual.compress(Bitmap.CompressFormat.PNG,100,out);}
                        }finally{actual.recycle();expected.recycle();}
                    }
                }finally{before.recycle();}
            }
        }
    }

    @Test public void actualSideWindowUsesGpuAndReturnsToCompatibilityWithoutReplacingItsInput() throws Exception {
        try(Fixture f=new Fixture()){
            f.enableGpu();f.config.setSidePopupStraighten(true);
            main(()->{f.view.applyConfig();f.view.show(LaneCycle.LEFT);});
            for(int i=0;i<10;i++){f.frame(Color.BLUE);SystemClock.sleep(40);}
            assertEquals("GPU",f.view.gpuState());assertTrue(f.view.isShowing());assertEquals(1f,((WindowManager.LayoutParams)f.view.getLayoutParams()).alpha,0f);
            Surface input=f.producer;
            main(()->control(f.view,f.view.getContext().getString(R.string.side_control_expand)).performClick());
            for(int i=0;i<8;i++){f.frame(Color.BLUE);SystemClock.sleep(40);}
            assertEquals("GPU",f.view.gpuState());assertSame(input,f.producer);assertTrue(input.isValid());
            f.config.setGpuRenderingMode("off");
            for(int i=0;i<8;i++){f.frame(Color.BLUE);SystemClock.sleep(40);}
            assertEquals("compatibility",f.view.gpuState());assertSame(input,f.producer);assertTrue(input.isValid());
        }
    }

    private View control(View view, String label) {
        if (label.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof android.view.ViewGroup) for (int i = 0; i < ((android.view.ViewGroup) view).getChildCount(); i++) {
            View found = control(((android.view.ViewGroup) view).getChildAt(i), label);
            if (found != null) return found;
        }
        return null;
    }

    @Test public void ultraWidePopupExpandsToPortraitHalfAndRestoresWithoutReplacingItsTexture() throws Exception {
        try (Fixture f = new Fixture()) {
            String oldAspect = f.config.getSidePopupAspect();
            try {
                f.config.setSidePopupAspect("11:5");
                main(() -> { f.view.applyConfig(); f.view.show(LaneCycle.LEFT); });
                for (int i = 0; i < 8; i++) { f.frame(Color.GREEN); SystemClock.sleep(30); }
                SideViewExpansion.Geometry[] normal = {null};
                SurfaceTexture texture = f.texture.get();
                main(() -> {
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) f.view.getLayoutParams();
                    assertEquals(2.2, params.width / (double) params.height, .01);
                    normal[0] = new SideViewExpansion.Geometry(params.x, params.y, params.width, params.height);
                    control(f.view, f.view.getContext().getString(R.string.side_control_expand)).performClick();
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                main(() -> {
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) f.view.getLayoutParams();
                    Size area = OverlayAvailableSize.read(f.view, (WindowManager) f.view.getContext().getSystemService(Context.WINDOW_SERVICE));
                    int margin = f.view.getResources().getDimensionPixelSize(R.dimen.side_control_margin);
                    assertEquals((area.getWidth() - 2 * margin) / 2, params.width);
                    assertEquals(area.getHeight() - 2 * margin, params.height);
                    assertTrue(params.height > params.width);
                    assertEquals(params.width, f.view.getWidth()); assertEquals(params.height, f.view.getHeight());
                    assertSame(texture, f.view.getTextureView().getSurfaceTexture());
                    assertEquals("11:5", f.config.getSidePopupAspect());
                    control(f.view, f.view.getContext().getString(R.string.side_control_restore)).performClick();
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                main(() -> {
                    WindowManager.LayoutParams params = (WindowManager.LayoutParams) f.view.getLayoutParams();
                    assertEquals(normal[0].x, params.x); assertEquals(normal[0].y, params.y);
                    assertEquals(normal[0].width, params.width); assertEquals(normal[0].height, params.height);
                    assertSame(texture, f.view.getTextureView().getSurfaceTexture());
                    assertEquals("11:5", f.config.getSidePopupAspect());
                });
            } finally { f.config.setSidePopupAspect(oldAspect); }
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
