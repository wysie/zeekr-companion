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
        RearViewMirrorView mirror;
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
        Fixture(boolean integrated, int width, int height, boolean popupRoute) throws Exception { this(integrated,width,height,popupRoute,false); }
        Fixture(boolean integrated, int width, int height, boolean popupRoute, boolean mirrorWindow) throws Exception {
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
                if(mirrorWindow){mirror=new RearViewMirrorView(themed,config);mirror.setSourceSize(new android.util.Size(sw,sh));texture=mirror.getTextureView();}
                else if (integrated) {
                    home = new FourLaneContainer(themed);
                    texture = new com.kooo.evcam.AutoFitTextureView(themed);
                    home.addView(texture); home.onFinishInflate(); home.setSourceSize(sw,sh);
                } else texture = new TextureView(themed);
                texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) { st.setDefaultBufferSize(sw, sh); output.set(st); }
                    public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {}
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture st) { return true; }
                    public void onSurfaceTextureUpdated(SurfaceTexture st) {
                        outputTimestamp = st.getTimestamp(); if(home!=null) home.noteFrame(st);if(mirror!=null){mirror.noteFrame();mirror.invalidate();}
                    }
                });
                WindowManager.LayoutParams p = new WindowManager.LayoutParams(320, 480,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        PixelFormat.OPAQUE);
                View target = mirrorWindow ? mirror : home == null ? texture : home;
                if(mirrorWindow)mirror.show();else windows.addView(target, p); current.set(texture); createdRoot.set(target);
            });
            view = current.get(); root = createdRoot.get(); await("Output texture available", () -> output.get() != null);
            if(mirrorWindow){producer=PreviewDewarp.optionalViewSurfaceFor(app,output.get(),new android.util.Size(sw,sh),true);pipe=PreviewDewarp.rawPipeFor(view);}
            else if(integrated) {
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
        long patternFrame() {
            GLES20.glViewport(0,0,sw,sh);GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
            for(int y=0;y<sh;y+=16)for(int x=0;x<sw;x+=16){
                GLES20.glScissor(x,sh-y-16,16,16);
                GLES20.glClearColor(x/255f,(y%256)/255f,(y/256)/3f,1);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            }
            GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
            long stamp=System.nanoTime();assertTrue(EGLExt.eglPresentationTimeANDROID(display,surface,stamp));
            assertTrue(EGL14.eglSwapBuffers(display,surface));return stamp;
        }
        void freshSide(SideGlRequest request) {
            for(int i=0;i<4;i++){patternFrame();SystemClock.sleep(55);}
            await("Side shader frame acquired: "+request.frames.failure,()->request.frames.fresh(outputTimestamp,SystemClock.elapsedRealtime()));
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
    private void assertSidePattern(Fixture f,int lane,FisheyeMesh.SourceMap reference) {
        assertSidePattern(f.bitmap(),lane,reference);
    }
    private void assertSidePattern(Bitmap image,int lane,FisheyeMesh.SourceMap reference) {
        float[] uv=new float[2];int checked=0,matched=0;
        int width=image.getWidth(),height=image.getHeight();
        try {
            for(int y=12;y<height-12;y+=3)for(int x=12;x<width-12;x+=3){
                reference.map((x+.5f)/width,(y+.5f)/height,uv);
                int actual=image.getPixel(x,y);
                if(uv[0]<0 || uv[0]>1 || uv[1]<0 || uv[1]>1){
                    checked++;if(Color.red(actual)<10 && Color.green(actual)<10 && Color.blue(actual)<10)matched++;continue;
                }
                float sx=uv[0]*256,sy=uv[1]*256;
                if(sx%16<3 || sx%16>13 || sy%16<3 || sy%16>13)continue;
                int red=Math.min(240,(int)(sx/16)*16),green=Math.min(240,(int)(sy/16)*16),blue=lane*85;
                checked++;if(Math.abs(Color.red(actual)-red)<12 && Math.abs(Color.green(actual)-green)<12 && Math.abs(Color.blue(actual)-blue)<12)matched++;
            }
            assertTrue("Enough detailed source samples",checked>100);
            assertTrue("Per-pixel shader matches CPU projection: "+matched+"/"+checked,matched>checked*.98);
        } finally {image.recycle();}
    }
    /** Known native texels, enlarged through the actual OES shader and acquired TextureView. */
    @Test public void sideSamplingMatchesCubicReferenceAndLiveChangesRetainInput() throws Exception {
        try(Fixture f=new Fixture(false,64,256)) {
            CompositeStreamGeometry.Plan plan=CompositeStreamGeometry.analyse("gpu-test",f.sw,f.sh);
            SurfaceTexture input=f.pipe.output();
            for(int lane:new int[]{LaneCycle.LEFT,LaneCycle.RIGHT}) {
                int[][] rows=new int[3][];
                String[] methods={"bilinear","bicubic_soft","bicubic_sharp"};
                for(int method=0;method<3;method++)for(int sharp:new int[]{0,100}) {
                    SideGpuPicture picture=new SideGpuPicture(plan,lane,RearViewGeometry.Viewport.full(),
                            0,90,0,0,0,false,f.ow,f.oh).withSampling(methods[method],sharp);
                    SideGlRequest request=new SideGlRequest(picture,f.ow,f.oh);f.pipe.setSide(request);
                    for(int repeat=0;repeat<4;repeat++) {
                        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);GLES20.glClearColor(1,0,1,1);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);
                        CompositeStreamGeometry.Lane bounds=plan.lane(lane);
                        for(int x=0;x<bounds.width;x++) {
                            float grey=sourceGrey(x)/255f;
                            GLES20.glScissor(bounds.x+x,f.sh-bounds.y-bounds.height,1,bounds.height);
                            GLES20.glClearColor(grey,grey,grey,1);GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                        }
                        GLES20.glDisable(GLES20.GL_SCISSOR_TEST);
                        EGLExt.eglPresentationTimeANDROID(f.display,f.surface,System.nanoTime());
                        assertTrue(EGL14.eglSwapBuffers(f.display,f.surface));SystemClock.sleep(55);
                    }
                    await("Sampling request acquired: "+request.frames.failure,()->request.frames.fresh(f.outputTimestamp,SystemClock.elapsedRealtime()));
                    Bitmap image=f.bitmap();
                    try {
                        int[] row=new int[f.ow];
                        for(int x=0;x<f.ow;x++) {
                            double pos=(x+.5)*64/f.ow-.5;
                            double raw=linearGrey(pos),value=raw;
                            if(method>0) {
                                value=0;int base=(int)Math.floor(pos);
                                for(int i=-1;i<=2;i++)value+=sourceGrey(base+i)*cubicWeight(base+i-pos,method==1?1./3:0,method==1?1./3:.5);
                                value=Math.max(64,Math.min(192,value));
                            }
                            if(sharp>0) {
                                double a=linearGrey(pos-1),b=linearGrey(pos+1);
                                double low=Math.min(value,Math.min(raw,Math.min(a,b))),high=Math.max(value,Math.max(raw,Math.max(a,b)));
                                value=Math.max(low,Math.min(high,value+(2*raw-a-b)*.25));
                            }
                            int colour=image.getPixel(x,f.oh/2);row[x]=Color.red(colour);
                            assertEquals(methods[method]+" sharpen="+sharp+" x="+x,(int)Math.round(value),Color.red(colour),4);
                            assertEquals("No neighbouring magenta lane",Color.red(colour),Color.green(colour),2);
                            assertEquals(Color.red(colour),Color.blue(colour),2);
                        }
                        if(sharp==0)rows[method]=row;
                    } finally {image.recycle();}
                    assertSame("Sampling edits keep the camera producer",input,f.pipe.output());assertTrue(f.producer.isValid());
                    request.cancel();f.pipe.clearSide(request);
                }
                int softDifference=0,sharpDifference=0;
                for(int x=0;x<f.ow;x++){softDifference+=Math.abs(rows[0][x]-rows[1][x]);sharpDifference+=Math.abs(rows[0][x]-rows[2][x]);}
                assertTrue("Soft cubic has distinct reconstructed pixels",softDifference>40);
                assertTrue("Sharp cubic has distinct reconstructed pixels",sharpDifference>100);
            }
        }
    }
    private static int sourceGrey(int x) { return ((Math.max(0,Math.min(63,x))/4)%2)==0?64:192; }
    private static double linearGrey(double x) {
        int base=(int)Math.floor(x);double f=x-base;return sourceGrey(base)*(1-f)+sourceGrey(base+1)*f;
    }
    private static double cubicWeight(double x,double b,double c) {
        x=Math.abs(x);
        if(x<1)return ((12-9*b-6*c)*x*x*x+(-18+12*b+6*c)*x*x+6-2*b)/6;
        if(x<2)return ((-b-6*c)*x*x*x+(6*b+30*c)*x*x+(-12*b-48*c)*x+8*b+24*c)/6;
        return 0;
    }
    @Test public void sideShaderMatchesAimRollMirrorAutoFitAndRawAcrossWindowShapes() throws Exception {
        try(Fixture f=new Fixture()) {
            CompositeStreamGeometry.Plan plan=CompositeStreamGeometry.analyse("gpu-test",f.sw,f.sh);
            for(int lane:new int[]{LaneCycle.LEFT,LaneCycle.RIGHT})for(int scenario=0;scenario<4;scenario++)
                    for(String sampling:new String[]{"bilinear","bicubic_soft","bicubic_sharp"}){
                int w=scenario==0?240:scenario==1?400:160,h=240,roll=lane==LaneCycle.LEFT?30:-30;
                boolean corrected=scenario!=3,autoFit=scenario!=2;
                SideViewFraming framing=new SideViewFraming();framing.configure(90,50,15,lane,w/(float)h,roll,autoFit);
                SideGpuPicture picture=SideGpuPicture.side(plan,lane,w,h,corrected,90,50,15,roll,true,framing).withSampling(sampling,35);
                SideGlRequest request=new SideGlRequest(picture,w,h);f.pipe.setSide(request);f.freshSide(request);
                assertSidePattern(f,lane,(u,v,out)->{
                    u=1-u;
                    if(corrected)framing.sourcePoint(u,v,out);
                    else {
                        double angle=Math.toRadians(roll),scale=SideViewRotation.coverScale(roll,w,h);
                        float x=u-.5f,y=v-.5f,aspect=w/(float)h;
                        float a=(float)((Math.cos(angle)*x+Math.sin(angle)*y/aspect)/scale+.5);
                        float b=(float)((-Math.sin(angle)*x*aspect+Math.cos(angle)*y)/scale+.5);
                        out[0]=picture.view[0]+a*picture.view[2];out[1]=picture.view[1]+b*picture.view[3];
                    }
                });
                request.cancel();f.pipe.clearSide(request);
            }
        }
    }
    @Test public void normalMirrorShaderUsesCorrectLaneViewportAndRearMirroring() throws Exception {
        try(Fixture f=new Fixture()) {
            CompositeStreamGeometry.Plan plan=CompositeStreamGeometry.analyse("gpu-test",f.sw,f.sh);
            for(int lane=0;lane<4;lane++)for(boolean corrected:new boolean[]{false,true}){
                RearViewGeometry.Viewport viewport=RearViewGeometry.Viewport.forWindow(400,180,.7f,.6f);
                boolean mirrored=LaneCycle.isMirrored(lane);
                SideGpuPicture picture=new SideGpuPicture(plan,lane,viewport,corrected?1:0,110,0,0,0,mirrored,400,180);
                SideGlRequest request=new SideGlRequest(picture,400,180);f.pipe.setSide(request);f.freshSide(request);
                assertSidePattern(f,lane,(u,v,out)->{
                    if(mirrored)u=1-u;
                    u=viewport.x+u*viewport.width;v=viewport.y+v*viewport.height;
                    if(corrected)FisheyeProjection.sourcePoint(u,v,110,FisheyeProjection.PROJECTION_RECTILINEAR,out,0);
                    else {out[0]=u;out[1]=v;}
                });
                request.cancel();f.pipe.clearSide(request);
            }
        }
    }
    @Test public void sideRedrawCannotRenewFrozenSourceAndFailureRetainsProducerForRawFallback() throws Exception {
        try(Fixture f=new Fixture()) {
            CompositeStreamGeometry.Plan plan=CompositeStreamGeometry.analyse("gpu-test",f.sw,f.sh);
            SideViewFraming framing=new SideViewFraming();framing.configure(90,0,0,LaneCycle.LEFT,1f);
            SideGpuPicture picture=SideGpuPicture.side(plan,LaneCycle.LEFT,240,240,true,90,0,0,0,false,framing);
            SideGlRequest request=new SideGlRequest(picture,240,240);f.pipe.setSide(request);f.freshSide(request);
            SurfaceTexture input=f.pipe.output();SystemClock.sleep(550);
            SideGlRequest changed=new SideGlRequest(picture,260,260);f.pipe.setSide(changed);SystemClock.sleep(80);
            assertFalse("A settings redraw cannot make old camera content live",changed.frames.fresh(f.outputTimestamp,SystemClock.elapsedRealtime()));
            changed.frames.failure="Injected side renderer failure";
            for(int i=0;i<4;i++){f.patternFrame();SystemClock.sleep(55);}
            await("Failed shader returns an acquired raw camera buffer",()->f.pipe.rawOutputReady(f.outputTimestamp));
            assertSame(input,f.pipe.output());assertTrue(f.producer.isValid());assertTrue(f.pipe.rawOutputReady(f.outputTimestamp));
            f.pipe.clearSide(changed);
        }
    }
    @Test public void actualSuperMirrorSwitchesGpuBetweenNormalAndBlindSideOnTheSameStream() throws Exception {
        Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();
        android.content.SharedPreferences prefs=app.getSharedPreferences("app_config",0);java.util.Map<String,?> saved=prefs.getAll();
        com.kooo.evcam.AppConfig config=new com.kooo.evcam.AppConfig(app);
        config.setGpuRenderingMode("custom");config.setGpuFeature("mirror",true);config.setGpuFeature("side",true);
        config.setRearViewLane(LaneCycle.REAR);config.setSidePopupStraighten(true);
        try(Fixture f=new Fixture(false,256,1024,false,true)){
            for(int i=0;i<8;i++){f.frame();SystemClock.sleep(40);}
            assertEquals("GPU",f.mirror.gpuState());Surface producer=f.producer;
            Bitmap image=f.bitmap();assertTrue(Color.green(image.getPixel(f.ow/2,f.oh/2))>240);image.recycle();
            main(()->f.mirror.showSideView(LaneCycle.LEFT));
            for(int i=0;i<8;i++){f.frame();SystemClock.sleep(40);}
            assertEquals("GPU",f.mirror.gpuState());image=f.bitmap();assertTrue(Color.blue(image.getPixel(f.ow/2,f.oh/2))>240);image.recycle();
            main(f.mirror::clearSideView);
            for(int i=0;i<8;i++){f.frame();SystemClock.sleep(40);}
            assertEquals("GPU",f.mirror.gpuState());assertSame(producer,f.producer);assertTrue(producer.isValid());
        } finally {
            android.content.SharedPreferences.Editor edit=prefs.edit().clear();
            for(java.util.Map.Entry<String,?> e:saved.entrySet()){
                Object v=e.getValue();String k=e.getKey();if(v instanceof Boolean)edit.putBoolean(k,(Boolean)v);else if(v instanceof String)edit.putString(k,(String)v);
                else if(v instanceof Integer)edit.putInt(k,(Integer)v);else if(v instanceof Long)edit.putLong(k,(Long)v);else if(v instanceof Float)edit.putFloat(k,(Float)v);
                else if(v instanceof java.util.Set)edit.putStringSet(k,new java.util.HashSet<>((java.util.Set<String>)v));
            }edit.commit();
        }
    }

    @Test public void settingsTapSharesRawInputEvenWhenMainCorrectionIsEnabled() throws Exception {
        try(Fixture f=new Fixture()) {
            Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();
            AtomicReference<TextureView> target=new AtomicReference<>();AtomicReference<SurfaceTexture> output=new AtomicReference<>();
            java.util.concurrent.atomic.AtomicLong stamp=new java.util.concurrent.atomic.AtomicLong();
            main(()->{TextureView view=new TextureView(app);view.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){
                public void onSurfaceTextureAvailable(SurfaceTexture st,int w,int h){output.set(st);}
                public void onSurfaceTextureSizeChanged(SurfaceTexture st,int w,int h){}
                public boolean onSurfaceTextureDestroyed(SurfaceTexture st){return true;}
                public void onSurfaceTextureUpdated(SurfaceTexture st){stamp.set(st.getTimestamp());}
            });WindowManager.LayoutParams p=new WindowManager.LayoutParams(240,240,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,PixelFormat.OPAQUE);
                p.gravity=Gravity.TOP|Gravity.START;p.x=500;f.windows.addView(view,p);target.set(view);});
            await("Settings output ready",()->output.get()!=null);
            try {
                CompositeStreamGeometry.Plan plan=CompositeStreamGeometry.analyse("gpu-test",f.sw,f.sh);
                SideViewFraming framing=new SideViewFraming();framing.configure(90,50,15,LaneCycle.RIGHT,1f,30,true);
                SideGpuPicture picture=SideGpuPicture.side(plan,LaneCycle.RIGHT,240,240,true,90,50,15,30,false,framing);
                SideGlRequest request=new SideGlRequest(picture,240,240);
                float[] lanes=new float[16];for(int lane=0;lane<4;lane++){
                    CompositeStreamGeometry.Lane l=plan.lane(lane);lanes[lane*4]=l.u0;lanes[lane*4+1]=l.v0;lanes[lane*4+2]=l.u1-l.u0;lanes[lane*4+3]=l.v1-l.v0;
                }
                f.pipe.setLanes(lanes);f.pipe.setCorrection(true,140,FisheyeProjection.PROJECTION_RECTILINEAR);
                f.pipe.setSideTap(output.get(),request);
                for(int i=0;i<4;i++){f.patternFrame();SystemClock.sleep(55);}
                await("Shared settings shader acquires raw source",()->request.frames.fresh(stamp.get(),SystemClock.elapsedRealtime()));
                AtomicReference<Bitmap> result=new AtomicReference<>();main(()->result.set(target.get().getBitmap()));
                assertNotNull(result.get());assertSidePattern(result.get(),LaneCycle.RIGHT,framing::sourcePoint);
                f.pipe.clearSideTap(request);assertTrue(f.producer.isValid());assertTrue(request.frames.cancelled);
            } finally {main(()->f.windows.removeViewImmediate(target.get()));}
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
    @Test public void cameraApplyReleasesGpuProducerBeforeReusingRetainedPopupTexture() throws Exception {
        try(Fixture f=new Fixture(false,256,1024,true)) {
            BirdEyeGlRequest request=f.install(true,new int[]{0,1,2,3}); f.fresh(request);
            // Emulate scheduler-confirmed native camera closure before removing its GL route.
            EGL14.eglMakeCurrent(f.display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);
            assertTrue(EGL14.eglDestroySurface(f.display,f.surface));
            f.surface=EGL14.EGL_NO_SURFACE; f.producer.release(); f.producer=null;
            PreviewDewarp.releaseAll();
            assertNull("Old route is no longer discoverable",PreviewDewarp.rawPipeFor(f.view));
            Surface raw=new Surface(f.output.get());
            try {
                android.graphics.Canvas canvas=raw.lockCanvas(null); canvas.drawColor(Color.MAGENTA); raw.unlockCanvasAndPost(canvas);
                await("A direct producer can draw into the retained output after GPU Apply",()->{
                    Bitmap image=f.view.getBitmap(16,16); if(image==null)return false;
                    int colour=image.getPixel(8,8);image.recycle();return Color.red(colour)>240&&Color.blue(colour)>240&&Color.green(colour)<15;
                });
            } finally { raw.release(); }
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
