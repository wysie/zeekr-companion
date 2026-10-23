package com.kooo.evcam.settings;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.opengl.*;
import android.os.*;
import android.util.Size;
import android.view.*;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.tabs.TabLayout;
import com.kooo.evcam.*;
import com.kooo.evcam.camera.*;
import com.kooo.evcam.profile.*;
import com.kooo.evcam.zeekr.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Opt-in documentation capture on a disposable emulator. No production demo mode or vehicle data. */
@RunWith(AndroidJUnit4.class)
public class ReadmeScreenshotsAndroidTest {
    private Context target;
    private int oldNight;
    private final Map<String, Map<String, ?>> savedPreferences = new HashMap<>();

    @Before public void setup() {
        // Opt out before any permission or preference mutation in ordinary test runs.
        assumeTrue("Explicit captureReadme=true is required", "true".equals(InstrumentationRegistry.getArguments().getString("captureReadme")));
        assumeTrue("Disposable emulator only", Build.PRODUCT.contains("sdk") || Build.HARDWARE.contains("ranchu"));
        target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        oldNight = AppCompatDelegate.getDefaultNightMode();
        for (String name : new String[]{"app_config", "profiles", "bird_eye_window"})
            savedPreferences.put(name, target.getSharedPreferences(name, 0).getAll());
        for (String permission : new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(target.getPackageName(), permission);
        assertTrue(target.getSharedPreferences("app_config", 0).edit().clear().putBoolean("first_launch", false)
                .putBoolean("language_chosen", true).putBoolean("rail_side_chosen", true).commit());
        AppConfig config = new AppConfig(target);
        config.setAutoStartRecording(false); config.setRearViewEnabled(false);
        config.setSidePopupYaw(50); config.setSidePopupPitch(15); config.setPreviewSourceMode("selected");
        Profile profile = ProfileMigration.migrate(new ProfileMigration.Snapshot()); RecordingSources.ensure(profile);
        ProfileStore store = new ProfileStore(target); store.select(profile.id); store.save(profile);
        main(() -> CameraManagerHolder.getInstance().release());
    }

    @After public void cleanup() {
        if (target == null) return;
        main(() -> { BirdEyePopupService.stop(target); RearViewMirrorService.stop(target); CameraManagerHolder.getInstance().release(); });
        for (Map.Entry<String, Map<String, ?>> file : savedPreferences.entrySet()) {
            SharedPreferences.Editor edit = target.getSharedPreferences(file.getKey(), 0).edit().clear();
            for (Map.Entry<String, ?> item : file.getValue().entrySet()) {
                String key = item.getKey(); Object value = item.getValue();
                if (value instanceof String) edit.putString(key, (String) value);
                else if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
                else if (value instanceof Integer) edit.putInt(key, (Integer) value);
                else if (value instanceof Long) edit.putLong(key, (Long) value);
                else if (value instanceof Float) edit.putFloat(key, (Float) value);
                else if (value instanceof Set) {
                    @SuppressWarnings("unchecked") Set<String> strings = (Set<String>) value;
                    edit.putStringSet(key, new HashSet<>(strings));
                }
            }
            assertTrue(edit.commit());
        }
        main(() -> AppCompatDelegate.setDefaultNightMode(oldNight));
    }

    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private static Field field(Class<?> type,String name) throws Exception { Field f=type.getDeclaredField(name);f.setAccessible(true);return f; }
    private void await(String message,BooleanSupplier ready) {
        long end=SystemClock.uptimeMillis()+10000;
        while(!ready.getAsBoolean() && SystemClock.uptimeMillis()<end)SystemClock.sleep(40);
        assertTrue(message,ready.getAsBoolean());
    }
    private static final class DemoCamera extends SingleCamera {
        final Size size;
        DemoCamera(Context context,String id,Size size) {super(context,id,null);this.size=size;}
        @Override public Size getPreviewSize(){return size;}
        @Override public Size getPreviewBufferSize(){return size;}
        @Override public boolean isCameraOpened(){return true;}
        @Override public boolean hasFramesWithin(long ms){return true;}
    }
    private static final class DemoManager extends MultiCameraManager {
        DemoManager(Context context) throws Exception {
            super(context);setSessionOptions(CameraSessionOptions.read(context));
            @SuppressWarnings("unchecked") Map<String,SingleCamera> cameras=(Map<String,SingleCamera>)field(MultiCameraManager.class,"cameras").get(this);
            cameras.put("front",new DemoCamera(context,"readme-surround",new Size(512,2048)));
            cameras.put(CameraProfile.ROLE_ROAD_FRONT,new DemoCamera(context,"readme-front",new Size(960,540)));
            cameras.put(CameraProfile.ROLE_ROAD_REAR,new DemoCamera(context,"readme-rear",new Size(960,540)));
        }
        @Override public boolean isBuilt(){return true;}
        @Override public CameraUiState sourceUiState(String key){return CameraUiState.STREAMING;}
    }
    private final class Frames implements AutoCloseable {
        final HandlerThread thread=new HandlerThread("ReadmeSyntheticFrames");
        final java.util.List<Producer> producers=new ArrayList<>();
        final java.util.concurrent.CountDownLatch stopped=new java.util.concurrent.CountDownLatch(1);
        Handler handler; volatile boolean running=true;volatile Throwable failure;
        Frames(MainActivity activity,Bitmap atlas) throws Exception {
            AtomicReference<SurfaceTexture[]> targets=new AtomicReference<>();
            main(()->{
                try {
                    Method canvas=MainActivity.class.getDeclaredMethod("canvasFor",String.class);canvas.setAccessible(true);
                    targets.set(new SurfaceTexture[]{((TextureView)canvas.invoke(activity,"front")).getSurfaceTexture(),
                        ((TextureView)canvas.invoke(activity,CameraProfile.ROLE_ROAD_FRONT)).getSurfaceTexture(),
                        ((TextureView)canvas.invoke(activity,CameraProfile.ROLE_ROAD_REAR)).getSurfaceTexture()});
                }catch(Exception e){throw new AssertionError(e);}
            });
            thread.start();handler=new Handler(thread.getLooper());
            handler.post(()->{
                try {
                    Bitmap strip=Bitmap.createBitmap(512,2048,Bitmap.Config.ARGB_8888);
                    Canvas canvas=new Canvas(strip);Paint filter=new Paint(Paint.FILTER_BITMAP_FLAG);
                    int[] tiles={4,5,2,3};
                    for(int lane=0;lane<4;lane++)canvas.drawBitmap(atlas,tile(atlas,tiles[lane]),new Rect(0,lane*512,512,(lane+1)*512),filter);
                    producers.add(new Producer(targets.get()[0],strip));strip.recycle();
                    for(int i=0;i<2;i++){
                        Bitmap road=Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888);
                        new Canvas(road).drawBitmap(atlas,tile(atlas,i),new Rect(0,0,960,540),filter);
                        producers.add(new Producer(targets.get()[i+1],road));road.recycle();
                    }
                    draw.run();
                }catch(Throwable e){failure=e;}
            });
        }
        final Runnable draw=new Runnable(){public void run(){
            if(!running)return;
            try{for(Producer producer:producers)producer.frame();}catch(Throwable e){failure=e;return;}
            handler.postDelayed(this,70);
        }};
        public void close() throws Exception {
            running=false;handler.removeCallbacks(draw);
            handler.post(()->{try{for(Producer producer:producers)producer.close();}finally{stopped.countDown();thread.quitSafely();}});
            assertTrue("Synthetic render thread stopped",stopped.await(5,java.util.concurrent.TimeUnit.SECONDS));thread.join(1000);
            if(failure!=null)throw new AssertionError(failure);
        }
    }
    private static Rect tile(Bitmap atlas,int index){
        int w=atlas.getWidth()/3,h=atlas.getHeight()/2;
        return new Rect(index%3*w+2,index/3*h+2,(index%3+1)*w-2,(index/3+1)*h-2);
    }
    private static final class Producer implements AutoCloseable {
        final Surface target;final EGLDisplay display;final EGLContext context;final EGLSurface surface;
        final int program,texture;final FloatBuffer vertices=ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        Producer(SurfaceTexture output,Bitmap image){
            output.setDefaultBufferSize(image.getWidth(),image.getHeight());target=new Surface(output);
            display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);assertTrue(EGL14.eglInitialize(display,new int[2],0,new int[2],0));
            EGLConfig[] configs=new EGLConfig[1];assertTrue(EGL14.eglChooseConfig(display,new int[]{EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_WINDOW_BIT,EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_NONE},0,configs,0,1,new int[1],0));
            context=EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE},0);
            surface=EGL14.eglCreateWindowSurface(display,configs[0],target,new int[]{EGL14.EGL_NONE},0);assertTrue(EGL14.eglMakeCurrent(display,surface,surface,context));
            program=GLES20.glCreateProgram();GLES20.glAttachShader(program,shader(GLES20.GL_VERTEX_SHADER,"attribute vec2 pos;attribute vec2 uv;varying vec2 tex;void main(){tex=uv;gl_Position=vec4(pos,0.,1.);}"));
            GLES20.glAttachShader(program,shader(GLES20.GL_FRAGMENT_SHADER,"precision mediump float;uniform sampler2D image;varying vec2 tex;void main(){gl_FragColor=texture2D(image,tex);}"));GLES20.glLinkProgram(program);
            int[] ids=new int[1];GLES20.glGenTextures(1,ids,0);texture=ids[0];GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,image,0);vertices.put(new float[]{-1,-1,0,1,1,-1,1,1,-1,1,0,0,1,1,1,0}).position(0);
        }
        static int shader(int kind,String source){int shader=GLES20.glCreateShader(kind);GLES20.glShaderSource(shader,source);GLES20.glCompileShader(shader);return shader;}
        void frame(){
            assertTrue(EGL14.eglMakeCurrent(display,surface,surface,context));int[] size=new int[2];EGL14.eglQuerySurface(display,surface,EGL14.EGL_WIDTH,size,0);EGL14.eglQuerySurface(display,surface,EGL14.EGL_HEIGHT,size,1);
            GLES20.glViewport(0,0,size[0],size[1]);GLES20.glUseProgram(program);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture);
            int pos=GLES20.glGetAttribLocation(program,"pos"),uv=GLES20.glGetAttribLocation(program,"uv");
            vertices.position(0);GLES20.glEnableVertexAttribArray(pos);GLES20.glVertexAttribPointer(pos,2,GLES20.GL_FLOAT,false,16,vertices);
            vertices.position(2);GLES20.glEnableVertexAttribArray(uv);GLES20.glVertexAttribPointer(uv,2,GLES20.GL_FLOAT,false,16,vertices);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);assertTrue(EGLExt.eglPresentationTimeANDROID(display,surface,System.nanoTime()));assertTrue(EGL14.eglSwapBuffers(display,surface));
        }
        public void close(){EGL14.eglMakeCurrent(display,surface,surface,context);GLES20.glDeleteProgram(program);GLES20.glDeleteTextures(1,new int[]{texture},0);EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);EGL14.eglDestroySurface(display,surface);EGL14.eglDestroyContext(display,context);target.release();}
    }
    private void capture(MainActivity activity,String name)throws Exception{
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(500);
        AtomicReference<Rect> area=new AtomicReference<>();
        main(()->{View content=activity.findViewById(android.R.id.content);int[] at=new int[2];content.getLocationOnScreen(at);area.set(new Rect(at[0],at[1],at[0]+content.getWidth(),at[1]+content.getHeight()));});
        Bitmap full=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(full);
        Rect r=area.get();assertTrue(r.width()>0&&r.height()>0);r.intersect(0,0,full.getWidth(),full.getHeight());
        Bitmap image=Bitmap.createBitmap(full,r.left,r.top,r.width(),r.height());
        File directory=new File(activity.getExternalFilesDir(null),"readme-screenshots");assertTrue(directory.isDirectory()||directory.mkdirs());
        try(FileOutputStream file=new FileOutputStream(new File(directory,name+".png"))){assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,file));}
        if(image!=full)image.recycle();full.recycle();
    }
    private SettingsShellFragment settings(MainActivity activity,String section){
        activity.findViewById(R.id.btn_settings).performClick();activity.getSupportFragmentManager().executePendingTransactions();
        SettingsShellFragment shell=(SettingsShellFragment)activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        shell.getChildFragmentManager().executePendingTransactions();shell.showSection(section);shell.getChildFragmentManager().executePendingTransactions();return shell;
    }
    @Test public void captureReadmeScreenshots()throws Exception{
        assumeTrue("Explicit captureReadme=true is required","true".equals(InstrumentationRegistry.getArguments().getString("captureReadme")));
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();AppConfig config=new AppConfig(context);
        Bitmap atlas=BitmapFactory.decodeFile(new File(context.getExternalFilesDir(null),"readme-camera-atlas.png").getAbsolutePath());assertNotNull("Push fictional atlas before capture",atlas);
        config.setLanguageMode("en");config.setVehicleProfileMode("009");config.setStorageLocation(AppConfig.STORAGE_INTERNAL);config.setGpuRenderingMode("custom");config.setFisheyeCorrection(false);
        for(String feature:GpuRendering.FEATURES)config.setGpuFeature(feature,"side".equals(feature)||"mirror".equals(feature)||"playback".equals(feature));
        config.setSourceCameraId(CameraProfile.ROLE_COMPOSITE,"readme-surround");config.setSourceCameraId(CameraProfile.ROLE_ROAD_FRONT,"readme-front");config.setSourceCameraId(CameraProfile.ROLE_ROAD_REAR,"readme-rear");
        config.setSidePopupEnabled(true);config.setFloatingTapAction("toggle_recording");config.setFloatingDoubleTapAction("take_photo");
        ProfileStore store=new ProfileStore(context);Profile profile=store.current();RecordingSources.ensure(profile);
        for(CameraProfile camera:profile.cameras){
            camera.enabled=CameraProfile.ROLE_COMPOSITE.equals(camera.role)||CameraProfile.ROLE_ROAD_FRONT.equals(camera.role)||CameraProfile.ROLE_ROAD_REAR.equals(camera.role);
            if(CameraProfile.ROLE_COMPOSITE.equals(camera.role))for(LaneLayout lane:camera.lanes)lane.fit=LaneLayout.FILL;
        }
        store.save(profile);
        try{
            for(String theme:new String[]{"light","dark"}){
                main(()->{CameraManagerHolder.getInstance().release();config.setThemeMode(theme);AppCompatDelegate.setDefaultNightMode("light".equals(theme)?AppCompatDelegate.MODE_NIGHT_NO:AppCompatDelegate.MODE_NIGHT_YES);});
                try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)){
                    AtomicReference<MainActivity> activity=new AtomicReference<>();scenario.onActivity(activity::set);
                    await("Camera configuration built",()->CameraManagerHolder.getInstance().getCameraManager()!=null&&CameraManagerHolder.getInstance().getCameraManager().isBuilt());
                    SystemClock.sleep(400);
                    main(()->{
                        try{
                            MainActivity a=activity.get();MultiCameraManager original=CameraManagerHolder.getInstance().getCameraManager();original.release();
                            DemoManager demo=new DemoManager(a);field(CameraManagerHolder.class,"cameraManager").set(CameraManagerHolder.getInstance(),demo);field(MainActivity.class,"cameraManager").set(a,demo);
                            StreamLayoutTable.setCompositeCameraId("readme-surround");((FourLaneContainer)a.findViewById(R.id.composite_container)).setSourceSize(512,2048);
                        }catch(Exception e){throw new AssertionError(e);}
                    });
                    await("All preview textures available",()->{
                        AtomicReference<Boolean> ready=new AtomicReference<>(false);main(()->{try{Method canvas=MainActivity.class.getDeclaredMethod("canvasFor",String.class);canvas.setAccessible(true);boolean all=true;for(String key:new String[]{"front",CameraProfile.ROLE_ROAD_FRONT,CameraProfile.ROLE_ROAD_REAR}){TextureView view=(TextureView)canvas.invoke(activity.get(),key);all&=view!=null&&view.isAvailable();}ready.set(all);}catch(Exception e){throw new AssertionError(e);}});return ready.get();
                    });
                    try(Frames frames=new Frames(activity.get(),atlas)){
                        SystemClock.sleep(1200);if(frames.failure!=null)throw new AssertionError(frames.failure);
                        capture(activity.get(),"main-"+theme);
                        main(()->((DrawerLayout)activity.get().findViewById(R.id.drawer_layout)).openDrawer(Gravity.LEFT));SystemClock.sleep(350);capture(activity.get(),"menu-"+theme);
                        main(()->((DrawerLayout)activity.get().findViewById(R.id.drawer_layout)).closeDrawers());SystemClock.sleep(350);
                        main(()->activity.get().findViewById(R.id.btn_preview_layout).performClick());capture(activity.get(),"preview-layout-"+theme);
                        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                    }
                    String[][] sections={{"recording","screen_recording"},{"storage","screen_storage"},{"super-mirror","screen_rearview"},{"blind-camera","screen_blind_spot_camera"},{"360-camera","screen_bird_eye"},{"floating-button","screen_floating"},{"interface","screen_interface"},{"system","screen_system"}};
                    AtomicReference<SettingsShellFragment> shell=new AtomicReference<>();main(()->shell.set(settings(activity.get(),"screen_recording")));
                    for(String[] section:sections){
                        main(()->{shell.get().showSection(section[1]);shell.get().getChildFragmentManager().executePendingTransactions();});capture(activity.get(),section[0]+"-"+theme);
                        if("blind-camera".equals(section[0])){
                            main(()->((TabLayout)activity.get().findViewById(R.id.side_settings_tabs)).getTabAt(3).select());capture(activity.get(),"hud-"+theme);
                        }
                    }
                    main(()->{shell.get().showSection("screen_system");shell.get().getChildFragmentManager().executePendingTransactions();SettingsPreferenceFragment system=(SettingsPreferenceFragment)shell.get().getChildFragmentManager().findFragmentById(R.id.settings_detail);shell.get().onPreferenceStartScreen(system,(androidx.preference.PreferenceScreen)system.findPreference("sub_gpu_processing"));shell.get().getChildFragmentManager().executePendingTransactions();});
                    capture(activity.get(),"gpu-processing-"+theme);
                }
            }
        }finally{atlas.recycle();main(()->CameraManagerHolder.getInstance().release());}
    }
}
