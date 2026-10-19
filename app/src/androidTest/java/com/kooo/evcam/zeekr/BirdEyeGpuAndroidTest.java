package com.kooo.evcam.zeekr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.opengl.*;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.*;
import android.widget.FrameLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real EGL external-camera-texture path, using synthetic frames on a disposable emulator. */
@RunWith(AndroidJUnit4.class)
public class BirdEyeGpuAndroidTest {
    private void main(Runnable work) { InstrumentationRegistry.getInstrumentation().runOnMainSync(work); }
    private void await(String message, BooleanSupplier done) {
        long until = SystemClock.uptimeMillis() + 5000;
        while (!done.getAsBoolean() && SystemClock.uptimeMillis() < until) SystemClock.sleep(15);
        assertTrue(message, done.getAsBoolean());
    }
    private final class Fixture implements AutoCloseable {
        final int sw, sh;
        final int ow = 160, oh = 240;
        final WindowManager windows;
        final TextureView view;
        final View root;
        FourLaneContainer home;
        com.kooo.evcam.AppConfig config;
        boolean previousGpu;
        final AtomicReference<SurfaceTexture> output = new AtomicReference<>();
        volatile long outputTimestamp;
        FisheyeGlPipe pipe;
        Surface producer;
        EGLDisplay display;
        EGLContext context;
        EGLSurface surface;
        Fixture() throws Exception { this(false); }
        Fixture(boolean integrated) throws Exception { this(integrated,256,1024); }
        Fixture(boolean integrated, int width, int height) throws Exception { this(integrated,width,height,false); }
        Fixture(boolean integrated, int width, int height, boolean popupRoute) throws Exception {
            sw=width; sh=height;
            Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
            try (ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .executeShellCommand("appops set " + app.getPackageName() + " SYSTEM_ALERT_WINDOW allow");
                 java.io.InputStream stream = new ParcelFileDescriptor.AutoCloseInputStream(fd)) {
                while (stream.read() != -1) {}
            }
            windows = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
            StreamLayoutTable.setCompositeCameraId("gpu-test");
            config = new com.kooo.evcam.AppConfig(app); previousGpu = config.isGpuBirdEye();
            if (integrated || popupRoute) config.setGpuBirdEye(true);
            AtomicReference<View> createdRoot = new AtomicReference<>();
            AtomicReference<TextureView> current = new AtomicReference<>();
            main(() -> {
                Context themed = new ContextThemeWrapper(app,com.kooo.evcam.R.style.Theme_Cam);
                TextureView texture;
                if (integrated) {
                    home = new FourLaneContainer(themed);
                    texture = new com.kooo.evcam.AutoFitTextureView(themed);
                    home.addView(texture); home.onFinishInflate(); home.setSourceSize(sw,sh);
                } else texture = new TextureView(themed);
                texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) { st.setDefaultBufferSize(sw, sh); output.set(st); }
                    public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {}
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture st) { return true; }
                    public void onSurfaceTextureUpdated(SurfaceTexture st) {
                        outputTimestamp = st.getTimestamp(); if(home!=null) home.noteFrame(st);
                    }
                });
                WindowManager.LayoutParams p = new WindowManager.LayoutParams(320, 480,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        PixelFormat.OPAQUE);
                View target = home == null ? texture : home;
                windows.addView(target, p); current.set(texture); createdRoot.set(target);
            });
            view = current.get(); root = createdRoot.get(); await("Output texture available", () -> output.get() != null);
            if(integrated) {
                producer = PreviewDewarp.surfaceFor("gpu-test",view,output.get(),new android.util.Size(sw,sh));
                pipe = PreviewDewarp.rawPipeFor(view);
            } else if(popupRoute) {
                producer=PreviewDewarp.birdEyeSurfaceFor(app,output.get(),new android.util.Size(sw,sh));
                pipe=PreviewDewarp.rawPipeFor(view);
            } else {
                pipe = FisheyeGlPipe.start("gpu-test", output.get(), sw, sh, null);
                producer = pipe == null ? null : pipe.newInputSurface();
            }
            assertNotNull("Real GL pipeline must initialise", pipe); assertNotNull(producer);
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            assertTrue(EGL14.eglInitialize(display, new int[2], 0, new int[2], 0));
            EGLConfig[] config = new EGLConfig[1];
            assertTrue(EGL14.eglChooseConfig(display, new int[]{EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,
                    EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE,EGL14.EGL_WINDOW_BIT,EGL14.EGL_NONE},0,config,0,1,new int[1],0));
            context = EGL14.eglCreateContext(display,config[0],EGL14.EGL_NO_CONTEXT,
                    new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE},0);
            surface = EGL14.eglCreateWindowSurface(display,config[0],producer,new int[]{EGL14.EGL_NONE},0);
            assertTrue(EGL14.eglMakeCurrent(display,surface,surface,context));
        }
        long frame() {
            int[] colours = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};
            GLES20.glViewport(0,0,sw,sh); GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            GLES20.glClearColor(1,0,1,1); GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
            CompositeStreamGeometry.Plan plan=CompositeStreamGeometry.analyse("gpu-test",sw,sh);
            for (int lane=0;lane<4;lane++) {
                CompositeStreamGeometry.Lane bounds=plan.lane(lane);
                int colour=colours[lane]; GLES20.glScissor(bounds.x,sh-bounds.y-bounds.height,bounds.width,bounds.height);
                GLES20.glClearColor(Color.red(colour)/255f,Color.green(colour)/255f,Color.blue(colour)/255f,1);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            }
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            long stamp=System.nanoTime();
            assertTrue(EGLExt.eglPresentationTimeANDROID(display,surface,stamp));
            assertTrue(EGL14.eglSwapBuffers(display,surface)); return stamp;
        }
        Bitmap bitmap() {
            AtomicReference<Bitmap> image=new AtomicReference<>(); main(() -> image.set(view.getBitmap(ow,oh)));
            assertNotNull(image.get()); return image.get();
        }
        BirdEyeGlRequest install(boolean fitted, int[] lanes) {
            StreamLayoutTable.setCompositeCameraId("gpu-test");
            CompositeStreamGeometry.Plan plan=CompositeStreamGeometry.analyse("gpu-test",sw,sh);
            BirdEyeProjection.Calibration c=BirdEyeImageFit.zeekr009(lanes,3);
            if (!fitted) c=new BirdEyeProjection.Calibration(c.length,c.width,c.margin,c.cameras);
            BirdEyeGlRequest request=new BirdEyeGlRequest();
            request.map=new BirdEyeProjection.Map(c,plan,sw,sh,ow,oh).gpuMap(plan);
            pipe.setBirdEye(request); return request;
        }
        void fresh(BirdEyeGlRequest request) {
            for(int i=0;i<4;i++){frame();SystemClock.sleep(65);}
            await("GPU frame must be published and acquired; failure="+request.failure,
                    () -> request.fresh(outputTimestamp,SystemClock.elapsedRealtime()));
        }
        @Override public void close() {
            if(display!=null){
                EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);
                EGL14.eglDestroySurface(display,surface); EGL14.eglDestroyContext(display,context); EGL14.eglReleaseThread();
            }
            if(producer!=null)producer.release();
            if(pipe!=null)pipe.release(); main(() -> windows.removeViewImmediate(root));
            config.setGpuBirdEye(previousGpu); StreamLayoutTable.reset();
        }
    }
    @Test public void manualAnd009FitPreserveCompleteCameraCoverageSeamsAndMapping() throws Exception {
        try(Fixture f=new Fixture()) {
            for(boolean fit:new boolean[]{false,true}) for(int[] lanes:new int[][]{{0,1,2,3},{3,2,0,1}}) {
                BirdEyeGlRequest request=f.install(fit,lanes); f.fresh(request);
                Bitmap actual=f.bitmap();
                CompositeStreamGeometry.Plan plan=CompositeStreamGeometry.analyse("gpu-test",f.sw,f.sh);
                BirdEyeProjection.Calibration c=BirdEyeImageFit.zeekr009(lanes,3);
                if(!fit)c=new BirdEyeProjection.Calibration(c.length,c.width,c.margin,c.cameras);
                int[] source=new int[f.sw*f.sh]; int[] colours={Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW};
                for(int y=0;y<f.sh;y++) java.util.Arrays.fill(source,y*f.sw,(y+1)*f.sw,colours[y/256]);
                int[] expected=new int[f.ow*f.oh];
                new BirdEyeProjection.Map(c,plan,f.sw,f.sh,f.ow,f.oh).render(source,expected);
                int bad=0;
                for(int y=0;y<f.oh;y++)for(int x=0;x<f.ow;x++){
                    int a=actual.getPixel(x,y),b=expected[y*f.ow+x];
                    if(Math.abs(Color.red(a)-Color.red(b))>3 || Math.abs(Color.green(a)-Color.green(b))>3
                            || Math.abs(Color.blue(a)-Color.blue(b))>3)bad++;
                }
                assertEquals("Full GPU picture must match CPU geometry, lane colours and seam weights; fit="+fit,0,bad);
                request.cancelled=true; f.pipe.clearBirdEye(request);
            }
        }
    }
    @Test public void popupCameraRouteReusesItsExistingConsumer() throws Exception {
        try(Fixture f=new Fixture(false,256,1024,true)) {
            BirdEyeGlRequest request=f.install(true,new int[]{0,1,2,3});f.fresh(request);
            Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
            Surface again=PreviewDewarp.birdEyeSurfaceFor(target,f.output.get(),new android.util.Size(f.sw,f.sh));
            try {
                assertSame(f.pipe,PreviewDewarp.rawPipeFor(f.view)); assertTrue(again.isValid());
                f.fresh(request);
            } finally { again.release(); }
        }
    }
    @Test public void native009BufferExcludesSeparatorPixelsAndPreservesProjection() throws Exception {
        try(Fixture f=new Fixture(false,1280,5140)) {
            BirdEyeGlRequest request=f.install(true,new int[]{0,1,2,3});f.fresh(request);
            Bitmap actual=f.bitmap();
            CompositeStreamGeometry.Plan plan=CompositeStreamGeometry.analyse("gpu-test",1280,5140);
            int[] source=new int[1280*5140];java.util.Arrays.fill(source,Color.MAGENTA);
            int[] colours={Color.RED,Color.GREEN,Color.BLUE,Color.YELLOW};
            for(int i=0;i<4;i++){
                CompositeStreamGeometry.Lane lane=plan.lane(i);
                for(int y=lane.y;y<lane.y+lane.height;y++)java.util.Arrays.fill(source,y*1280,(y+1)*1280,colours[i]);
            }
            int[] expected=new int[f.ow*f.oh];
            new BirdEyeProjection.Map(BirdEyeImageFit.zeekr009(new int[]{0,1,2,3},3),plan,1280,5140,f.ow,f.oh).render(source,expected);
            for(int y=0;y<f.oh;y++)for(int x=0;x<f.ow;x++){
                int a=actual.getPixel(x,y),b=expected[y*f.ow+x];
                assertTrue("Native buffer/lane-centre clamp changed colour at "+x+","+y,
                        Math.abs(Color.red(a)-Color.red(b))<=3 && Math.abs(Color.green(a)-Color.green(b))<=3
                        && Math.abs(Color.blue(a)-Color.blue(b))<=3);
            }
        }
    }
    @Test public void failureAndCancellationRestoreRawOutputWithoutReplacingCameraSurface() throws Exception {
        try(Fixture f=new Fixture()) {
            Surface producer=f.producer; SurfaceTexture output=f.output.get();
            BirdEyeGlRequest first=f.install(true,new int[]{0,1,2,3}); f.fresh(first);
            first.failure="Injected GPU failure";
            for(int i=0;i<3;i++){f.frame();SystemClock.sleep(65);}
            await("Failure returns to an acquired raw camera frame", () -> f.pipe.rawOutputReady(f.outputTimestamp));
            Bitmap raw=f.bitmap();
            assertEquals(Color.RED,raw.getPixel(80,20)); assertEquals(Color.YELLOW,raw.getPixel(80,220));
            assertTrue(f.pipe.rawOutputReady(f.outputTimestamp)); assertFalse(first.fresh(f.outputTimestamp,SystemClock.elapsedRealtime()));
            BirdEyeGlRequest next=f.install(false,new int[]{0,1,2,3}); f.fresh(next);
            next.cancelled=true; f.pipe.clearBirdEye(next);
            for(int i=0;i<3;i++){f.frame();SystemClock.sleep(65);}
            assertTrue(f.pipe.rawOutputReady(f.outputTimestamp));
            assertSame(producer,f.producer); assertSame(output,f.output.get()); assertTrue(producer.isValid());
        }
    }
    @Test public void mainPreviewUsesGpuPreservesPhotoAndYieldsToFactoryAndFrozenSource() throws Exception {
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
        android.content.SharedPreferences preferences=target.getSharedPreferences("app_config",Context.MODE_PRIVATE);
        java.util.Map<String,?> savedPreferences=preferences.getAll();
        boolean developer=com.kooo.evcam.settings.DeveloperMode.isUnlocked();
        com.kooo.evcam.settings.DeveloperMode.setEnabled(target,true);
        new com.kooo.evcam.AppConfig(target).setGpuFisheyePreview(true);
        com.kooo.evcam.telemetry.Telemetry telemetry=com.kooo.evcam.telemetry.Telemetry.get();
        java.lang.reflect.Field car=telemetry.getClass().getDeclaredField("car"); car.setAccessible(true);
        java.lang.reflect.Field readings=telemetry.getClass().getDeclaredField("readings"); readings.setAccessible(true);
        Object oldCar=car.get(telemetry),oldReadings=readings.get(telemetry);
        Class<?> source=Class.forName("com.kooo.evcam.telemetry.EcarxSource");
        java.lang.reflect.Constructor<?> constructor=source.getDeclaredConstructor(telemetry.getClass(),boolean.class);
        constructor.setAccessible(true); Object fake=constructor.newInstance(telemetry,false);
        java.lang.reflect.Field link=source.getDeclaredField("link"); link.setAccessible(true);
        link.set(fake,com.kooo.evcam.telemetry.Telemetry.CarLink.CONNECTED);
        car.set(telemetry,fake);
        try(Fixture f=new Fixture(true)) {
            SurfaceTexture original=f.output.get(); Surface producer=f.producer;
            assertTrue("Existing GPU fisheye route is selected",PreviewDewarp.isActive(f.view));
            main(() -> assertTrue(f.home.showBirdEye()));
            AtomicReference<Bitmap> photo=new AtomicReference<>();
            for(int i=0;i<14 && photo.get()==null;i++){
                f.frame(); SystemClock.sleep(75); main(() -> photo.set(f.home.birdEyePhoto()));
            }
            assertNotNull("GPU home must produce a fresh stitched photo with artwork",photo.get());
            assertTrue(photo.get().getWidth()>0 && photo.get().getHeight()>0);
            AtomicReference<String> state=new AtomicReference<>(); main(() -> state.set(f.home.describeState()));
            assertTrue(state.get(),state.get().contains("renderer=gpu-texture-lookup"));
            assertTrue(state.get(),state.get().contains("readbackMs=0"));
            assertFalse("360 owns raw input, not a second fisheye pass",PreviewDewarp.isActive(f.view));
            int[] location=new int[2]; main(() -> f.root.getLocationOnScreen(location));
            boolean visible=false;
            // Photo acquisition can precede compositor presentation by a frame. Keep the
            // synthetic producer live while waiting for the actual displayed picture.
            for(int attempt=0;attempt<12 && !visible;attempt++) {
                f.frame();SystemClock.sleep(60);
                Bitmap screen=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                visible=true;
                for(float[] point:new float[][]{{.5f,.1f},{.15f,.5f},{.85f,.5f}}){
                    int expected=photo.get().getPixel((int)(photo.get().getWidth()*point[0]),(int)(photo.get().getHeight()*point[1]));
                    int actual=screen.getPixel(location[0]+(int)(f.root.getWidth()*point[0]),location[1]+(int)(f.root.getHeight()*point[1]));
                    if(expected!=actual)visible=false;
                }
            }
            assertTrue("Main screen must actually show the projected GPU picture",visible);
            SystemClock.sleep(550); main(() -> assertNull("Frozen source cannot be photographed",f.home.birdEyePhoto()));
            java.util.EnumMap<com.kooo.evcam.telemetry.Signal,Object> values=new java.util.EnumMap<>(com.kooo.evcam.telemetry.Signal.class);
            values.put(com.kooo.evcam.telemetry.Signal.STOCK_360,true);
            java.lang.reflect.Constructor<?> snapshot=com.kooo.evcam.telemetry.Readings.class.getDeclaredConstructor(java.util.Map.class,long.class);
            snapshot.setAccessible(true); readings.set(telemetry,snapshot.newInstance(values,99L));
            f.frame(); SystemClock.sleep(100);
            main(() -> assertNull("Factory takeover always covers the DIY view",f.home.birdEyePhoto()));
            main(() -> f.home.showGrid(false));
            for(int i=0;i<3;i++){ f.frame();SystemClock.sleep(60); }
            assertSame(original,f.output.get()); assertSame(producer,f.producer); assertTrue(producer.isValid());
            assertFalse(f.pipe.hasBirdEyeRequest());
            assertTrue("Exit restores the existing GPU fisheye route",PreviewDewarp.isActive(f.view));
        } finally {
            car.set(telemetry,oldCar);readings.set(telemetry,oldReadings);
            com.kooo.evcam.settings.DeveloperMode.setEnabled(target,developer);
            android.content.SharedPreferences.Editor edit=preferences.edit().clear();
            for(java.util.Map.Entry<String,?> entry:savedPreferences.entrySet()){
                Object value=entry.getValue();String key=entry.getKey();
                if(value instanceof Boolean)edit.putBoolean(key,(Boolean)value);
                else if(value instanceof Integer)edit.putInt(key,(Integer)value);
                else if(value instanceof Long)edit.putLong(key,(Long)value);
                else if(value instanceof Float)edit.putFloat(key,(Float)value);
                else if(value instanceof String)edit.putString(key,(String)value);
            }
            assertTrue(edit.commit());
        }
    }
    @Test public void realGpuSetupFailureRetainsRawCameraOutput() throws Exception {
        try(Fixture f=new Fixture()) {
            StreamLayoutTable.setCompositeCameraId("gpu-test");
            BirdEyeGlRequest request=new BirdEyeGlRequest();
            request.map=new BirdEyeGpuMap(65537,1,f.sw,f.sh,CompositeStreamGeometry.analyse("gpu-test",f.sw,f.sh));
            f.pipe.setBirdEye(request);
            for(int i=0;i<3;i++){f.frame();SystemClock.sleep(70);}
            assertNotNull("Unsupported GPU texture size must report an explicit fallback reason",request.failure);
            assertTrue(f.pipe.rawOutputReady(f.outputTimestamp));
            Bitmap raw=f.bitmap(); assertEquals(Color.RED,raw.getPixel(80,20)); assertEquals(Color.YELLOW,raw.getPixel(80,220));
            assertTrue(f.producer.isValid());
        }
    }
    @Test public void redrawingWithoutCameraProgressCannotRenewFreshness() throws Exception {
        try(Fixture f=new Fixture()) {
            BirdEyeGlRequest request=f.install(true,new int[]{0,1,2,3}); f.fresh(request);
            SystemClock.sleep(550);
            f.pipe.setBirdEye(request); SystemClock.sleep(80);
            assertFalse("A GL redraw must not turn a frozen camera into a fresh frame",request.fresh(f.outputTimestamp,SystemClock.elapsedRealtime()));
        }
    }
}
