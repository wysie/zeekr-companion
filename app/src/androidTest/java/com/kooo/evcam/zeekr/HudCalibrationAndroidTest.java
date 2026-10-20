package com.kooo.evcam.zeekr;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.view.WindowManager;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.R;
import com.kooo.evcam.settings.AppTheme;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HudCalibrationAndroidTest {
    private Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private static HudCalibrationSession session(HudCalibrationActivity activity) {
        try { Field f=HudCalibrationActivity.class.getDeclaredField("session"); f.setAccessible(true);
            return (HudCalibrationSession)f.get(activity); }
        catch (Exception e) { throw new AssertionError(e); }
    }
    private void capture(String name) throws Exception {
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        try (FileOutputStream out=new FileOutputStream(new File(context().getExternalFilesDir(null), name))) {
            bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
        } finally { bitmap.recycle(); }
    }
    @Test public void squareRendersOnPublicHudAndLogsOnlyExplicitObservationsThenStopsOnLeave() throws Exception {
        Context context=context(); SharedPreferences log=context.getSharedPreferences("hud_visibility_observations",0);
        String oldLog=log.getString("observations",null);
        AppConfig config=new AppConfig(context); boolean oldLayout=config.is009HudLayout();
        int oldNight=androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode();
        AtomicReference<Bitmap> frame=new AtomicReference<>();
        ImageReader reader=ImageReader.newInstance(1280,640,PixelFormat.RGBA_8888,3);
        HandlerThread consumer=new HandlerThread("hud-calibration-test"); consumer.start();
        reader.setOnImageAvailableListener(r -> {
            try (Image image=r.acquireLatestImage()) {
                if(image==null)return;
                Image.Plane p=image.getPlanes()[0]; ByteBuffer pixels=p.getBuffer();
                Bitmap padded=Bitmap.createBitmap(p.getRowStride()/p.getPixelStride(),image.getHeight(),Bitmap.Config.ARGB_8888);
                padded.copyPixelsFromBuffer(pixels);
                Bitmap copy=Bitmap.createBitmap(padded,0,0,image.getWidth(),image.getHeight());
                if(copy!=padded)padded.recycle();
                synchronized(frame){ Bitmap old=frame.getAndSet(copy);if(old!=null)old.recycle(); }
            }
        },new Handler(consumer.getLooper()));
        VirtualDisplay display=((DisplayManager)context.getSystemService(Context.DISPLAY_SERVICE)).createVirtualDisplay(
                "Owned HUD visibility test",1280,640,160,reader.getSurface(),
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC|DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
                        |DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
        assertNotNull(display);
        try {
            log.edit().clear().commit(); main(() -> AppTheme.apply(AppTheme.LIGHT));
            try(ActivityScenario<HudCalibrationActivity> scenario=ActivityScenario.launch(HudCalibrationActivity.class)) {
                AtomicReference<HudCalibrationSession> ref=new AtomicReference<>();
                onView(withText(R.string.hud_test_start)).perform(click());
                SystemClock.sleep(350);
                scenario.onActivity(a -> {
                    HudCalibrationSession s=session(a);ref.set(s);assertNotNull(s);assertTrue(HudCalibrationSession.active());
                    assertEquals(1280,s.marker.getWidth());assertEquals(640,s.marker.getHeight());assertEquals(200,s.size);
                    WindowManager.LayoutParams params=(WindowManager.LayoutParams)s.marker.getRootView().getLayoutParams();
                    assertNotEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,params.type);
                    assertTrue((params.flags&WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)!=0);
                    assertEquals(0,HudCalibrationLog.read(a).length());
                    s.move(-100,220,200,Color.RED);
                    try { java.lang.reflect.Method update=HudCalibrationActivity.class.getDeclaredMethod("update");
                        update.setAccessible(true);update.invoke(a); } catch(Exception e){throw new AssertionError(e);}
                });
                SystemClock.sleep(250);
                synchronized(frame) {
                    Bitmap image=frame.get();assertNotNull(image);
                    int red=0;
                    for(int y=220;y<420;y++)for(int x=0;x<100;x++) {
                        int c=image.getPixel(x,y);if(Color.red(c)>200&&Color.green(c)<40&&Color.blue(c)<40)red++;
                    }
                    assertTrue("Actual composed HUD square not rendered: "+red,red>16000);
                    try(FileOutputStream out=new FileOutputStream(new File(context.getExternalFilesDir(null),"hud-calibration-projected.png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}
                }
                onView(withText(R.string.hud_test_clip_left)).perform(scrollTo(),click());
                JSONObject record=HudCalibrationLog.read(context).getJSONObject(0);
                assertEquals("left_cut_off",record.getString("observation"));assertEquals(-100,record.getInt("x"));
                assertEquals(200,record.getInt("size"));assertEquals(100,record.getJSONArray("logical_visible_rect").getInt(2));
                scenario.onActivity(a -> {
                    session(a).move(1180,220,200,Color.RED);
                    try { java.lang.reflect.Method update=HudCalibrationActivity.class.getDeclaredMethod("update");
                        update.setAccessible(true);update.invoke(a); } catch(Exception e){throw new AssertionError(e);}
                });
                SystemClock.sleep(200);
                synchronized(frame) {
                    Bitmap image=frame.get();int red=0;
                    for(int y=220;y<420;y++)for(int x=1180;x<1280;x++) {
                        int c=image.getPixel(x,y);if(Color.red(c)>200&&Color.green(c)<40&&Color.blue(c)<40)red++;
                    }
                    assertTrue("Right-edge square not rendered: "+red,red>16000);
                }
                onView(withText(R.string.hud_test_clip_right)).perform(scrollTo(),click());
                assertEquals("right_cut_off",HudCalibrationLog.read(context).getJSONObject(1).getString("observation"));
                // Calibration suppresses normal HUD presentation without detaching its existing texture/window.
                scenario.onActivity(a -> {
                    SideViewHudPanel panel=new SideViewHudPanel(a,config,display.getDisplay());
                    SideViewHudOutput output=new SideViewHudOutput(a,config,() -> { });
                    try {
                        panel.show(LaneCycle.LEFT);
                        Field field=SideViewHudOutput.class.getDeclaredField("panel");field.setAccessible(true);field.set(output,panel);
                        android.view.TextureView texture=panel.getTextureView();
                        output.update(LaneCycle.RIGHT,false,true,0);
                        assertTrue(panel.isAttached());assertFalse(panel.isShowing());
                        assertSame(texture,panel.getTextureView());
                    } catch(Exception e){throw new AssertionError(e);}
                    finally {output.dispose();}
                });
                JSONObject report=new JSONObject(DiagnosticsJson.build(context,"HUD test"));
                assertEquals(2,report.getJSONArray("hud_visibility_observations").length());
                assertFalse(report.getJSONObject("settings").getJSONObject("stores").has("hud_visibility_observations"));
                // Return to the top for a useful complete native UI capture, in each real app theme.
                onView(withText(R.string.hud_test_x)).perform(scrollTo());
                capture("hud-calibration-controls-light.png");
                scenario.moveToState(Lifecycle.State.CREATED);
                main(() -> assertFalse(HudCalibrationSession.active()));
                assertEquals(2,HudCalibrationLog.read(context).length());
                scenario.moveToState(Lifecycle.State.RESUMED);
                scenario.onActivity(a -> assertNull(session(a))); // Never restart a marker automatically.
                main(() -> AppTheme.apply(AppTheme.DARK));
                SystemClock.sleep(300);
                onView(withText(R.string.hud_test_start)).perform(click());SystemClock.sleep(250);
                capture("hud-calibration-controls-dark.png");
                display.release();SystemClock.sleep(250);
                scenario.onActivity(a -> assertNull(session(a)));
                main(() -> assertFalse(HudCalibrationSession.active()));
            }
            for(int i=0;i<132;i++)assertTrue(HudCalibrationLog.save(context,new JSONObject().put("sequence",i)));
            assertEquals(128,HudCalibrationLog.read(context).length());
            assertEquals(4,HudCalibrationLog.read(context).getJSONObject(0).getInt("sequence"));
            assertEquals(131,HudCalibrationLog.read(context).getJSONObject(127).getInt("sequence"));
        } finally {
            main(() -> { AppTheme.apply(oldNight==androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES?AppTheme.DARK:
                    oldNight==androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO?AppTheme.LIGHT:AppTheme.SYSTEM); });
            display.release();reader.setOnImageAvailableListener(null,null);consumer.quitSafely();consumer.join(1000);
            reader.close();synchronized(frame){Bitmap image=frame.getAndSet(null);if(image!=null)image.recycle();}
            log.edit().clear().putString("observations",oldLog).commit();config.selectHudLayout(oldLayout);
        }
    }
}
