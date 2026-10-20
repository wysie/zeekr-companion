package com.kooo.evcam.settings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.os.SystemClock;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.camera.CameraManagerHolder;
import com.kooo.evcam.camera.MultiCameraManager;
import com.kooo.evcam.camera.SingleCamera;
import com.kooo.evcam.telemetry.Telemetry;
import com.kooo.evcam.ui.MainPageControlsAndroidTest;
import org.junit.Test;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Native source fixture is synthetic; no car imagery is manufactured in production. */
public class SideQualityUiAndroidTest extends MainPageControlsAndroidTest {
    private void openPicture(MainActivity activity) {
        SettingsShellFragment shell=new SettingsShellFragment();activity.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
        activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,shell).commitNow();
        shell.showSection("sub_side_image");shell.getChildFragmentManager().executePendingTransactions();
    }
    @Test public void qualityButtonExportsOneNativeFrameAndRetainsPictureSettingsAndPipeline() throws Exception {
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
        android.content.SharedPreferences exports=target.getSharedPreferences("side_quality_exports",0);
        String previous=exports.getString("last_file",null);exports.edit().clear().commit();
        String previousComposite=com.kooo.evcam.zeekr.StreamLayoutTable.compositeCameraId();
        Telemetry telemetry=Telemetry.get();java.lang.reflect.Field car=Telemetry.class.getDeclaredField("car");car.setAccessible(true);
        Object oldCar=car.get(telemetry);Class<?> type=Class.forName("com.kooo.evcam.telemetry.EcarxSource");
        java.lang.reflect.Constructor<?> constructor=type.getDeclaredConstructor(Telemetry.class,boolean.class);constructor.setAccessible(true);
        Object fake=constructor.newInstance(telemetry,false);java.lang.reflect.Field link=type.getDeclaredField("link");link.setAccessible(true);
        link.set(fake,Telemetry.CarLink.CONNECTED);
        AtomicReference<TextureView> source=new AtomicReference<>();AtomicReference<File> exported=new AtomicReference<>();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            AppConfig config=new AppConfig(target);config.setSidePopupYaw(40);config.setSidePopupFov(90);config.setSidePopupPitch(15);
            config.setSidePopupRotation(2,30);config.setSidePopupRotation(3,-30);config.setSidePopupAspect("11:5");config.setSidePopupMirrored(true);
            AtomicReference<Object> oldTexture=new AtomicReference<>();
            java.lang.reflect.Field textureField=MainActivity.class.getDeclaredField("textureFront");textureField.setAccessible(true);
            scenario.onActivity(a -> {
                carSet(car,telemetry,fake);
                com.kooo.evcam.AutoFitTextureView fixture=new com.kooo.evcam.AutoFitTextureView(a);
                fixture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                    public void onSurfaceTextureAvailable(SurfaceTexture t,int w,int h){t.setDefaultBufferSize(1280,5140);}
                    public void onSurfaceTextureSizeChanged(SurfaceTexture t,int w,int h){t.setDefaultBufferSize(1280,5140);}
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture t){return true;}
                    public void onSurfaceTextureUpdated(SurfaceTexture t){ }
                });
                android.widget.FrameLayout.LayoutParams at=new android.widget.FrameLayout.LayoutParams(32,128,android.view.Gravity.END);
                ((android.widget.FrameLayout)a.findViewById(android.R.id.content)).addView(fixture,at);
                try {oldTexture.set(textureField.get(a));textureField.set(a,fixture);}catch(Exception e){throw new AssertionError(e);}
                source.set(fixture);
            });
            long until=SystemClock.elapsedRealtime()+5000;while(!source.get().isAvailable()&&SystemClock.elapsedRealtime()<until)SystemClock.sleep(20);
            assertTrue(source.get().isAvailable());
            MultiCameraManager pipeline=CameraManagerHolder.getInstance().getCameraManager();assertNotNull(pipeline);
            SingleCamera camera=pipeline.getCamera("front");assertNotNull(camera);
            java.lang.reflect.Field size=SingleCamera.class.getDeclaredField("previewSize");size.setAccessible(true);Object oldSize=size.get(camera);size.set(camera,new android.util.Size(1280,5140));
            SurfaceTexture texture=source.get().getSurfaceTexture();texture.setDefaultBufferSize(1280,5140);Surface producer=new Surface(texture);
            try {
                com.kooo.evcam.zeekr.StreamLayoutTable.setCompositeCameraId(camera.getCameraId());
                scenario.onActivity(this::openPicture);
                for(int i=0;i<8;i++) {
                    size.set(camera,new android.util.Size(1280,5140));
                    Canvas c=producer.lockCanvas(null);c.drawColor(Color.BLUE);Paint p=new Paint();p.setColor(Color.WHITE);
                    for(int y=0;y<5140;y+=8)c.drawRect(0,y,1280,y+4,p);producer.unlockCanvasAndPost(c);SystemClock.sleep(70);}
                java.util.Map<String,?> before=target.getSharedPreferences("app_config",0).getAll();
                scenario.onActivity(a -> {
                    carSet(car,telemetry,fake);
                    try {size.set(camera,new android.util.Size(1280,5140));}catch(Exception e){throw new AssertionError(e);}
                    com.kooo.evcam.zeekr.StreamLayoutTable.setCompositeCameraId(camera.getCameraId());
                    source.get().setVisibility(View.VISIBLE);
                    a.findViewById(R.id.side_quality_capture).performClick();
                });
                until=SystemClock.elapsedRealtime()+10000;
                while(SystemClock.elapsedRealtime()<until) {
                    String file=exports.getString("last_file",null);if(file!=null){exported.set(new File(file));break;}
                    SystemClock.sleep(25);
                }
                AtomicReference<String> status=new AtomicReference<>();
                scenario.onActivity(a -> status.set(((android.widget.TextView)a.findViewById(R.id.side_quality_result)).getText().toString()));
                assertNotNull("Native Picture button must create the comparison ZIP: "+status.get(),exported.get());assertTrue(exported.get().isFile());
                java.nio.file.Files.copy(exported.get().toPath(),new File(target.getExternalFilesDir(null),"side-quality-sample.zip").toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a -> {
                    assertTrue(a.findViewById(R.id.side_quality_share).isShown());assertTrue(a.findViewById(R.id.side_quality_copy).isShown());
                    assertEquals(before,target.getSharedPreferences("app_config",0).getAll());assertSame(pipeline,CameraManagerHolder.getInstance().getCameraManager());
                    assertSame(texture,source.get().getSurfaceTexture());
                    android.widget.TextView result=a.findViewById(R.id.side_quality_result);assertTrue(result.getText().toString().contains(exported.get().getName()));
                });
                scenario.onActivity(a -> {try{textureField.set(a,oldTexture.get());size.set(camera,oldSize);}catch(Exception e){throw new AssertionError(e);}});
                main(() -> AppTheme.apply(AppTheme.LIGHT));SystemClock.sleep(250);scenario.onActivity(this::openPicture);capture("side-quality-light");
                main(() -> AppTheme.apply(AppTheme.DARK));SystemClock.sleep(250);scenario.onActivity(this::openPicture);capture("side-quality-dark");
                scenario.onActivity(a -> {assertTrue(a.findViewById(R.id.side_quality_copy).isShown());
                    View picture=a.findViewById(R.id.side_preview_picture);assertTrue("Capture footer preserves picture height",picture.getHeight()>80);});
            } finally {producer.release();}
        } finally {
            com.kooo.evcam.zeekr.StreamLayoutTable.setCompositeCameraId(previousComposite);
            car.set(telemetry,oldCar);if(exported.get()!=null)exported.get().delete();
            exports.edit().clear().putString("last_file",previous).commit();
        }
    }
    private void main(Runnable work) {InstrumentationRegistry.getInstrumentation().runOnMainSync(work);}
    private void capture(String name) throws Exception {
        android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null),name+".png"))) {
            image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);
        } finally {image.recycle();}
    }
    private void carSet(java.lang.reflect.Field field,Object owner,Object value) {
        try {field.set(owner,value);}catch(Exception e){throw new AssertionError(e);}
    }
}
