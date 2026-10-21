package com.kooo.evcam.zeekr;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.R;
import com.kooo.evcam.settings.AppTheme;
import com.kooo.evcam.settings.SettingsBackup;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HudSafeAreaAndroidTest {
    private Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    private void main(Runnable action){InstrumentationRegistry.getInstrumentation().runOnMainSync(action);}
    private static HudCalibrationSession session(HudCalibrationActivity a) {
        try{Field f=HudCalibrationActivity.class.getDeclaredField("session");f.setAccessible(true);return (HudCalibrationSession)f.get(a);}
        catch(Exception e){throw new AssertionError(e);}
    }
    private void move(ActivityScenario<HudCalibrationActivity> scenario,int x) {
        scenario.onActivity(a -> {try{
            session(a).move(x,250,140,Color.RED);
            Method update=HudCalibrationActivity.class.getDeclaredMethod("update");update.setAccessible(true);update.invoke(a);
        }catch(Exception e){throw new AssertionError(e);}});SystemClock.sleep(160);
    }
    private void capture(String name)throws Exception{
        Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        try(FileOutputStream out=new FileOutputStream(new File(context().getExternalFilesDir(null),name))){b.compress(Bitmap.CompressFormat.PNG,100,out);}
        finally{b.recycle();}
    }
    private static void restore(SharedPreferences prefs,Map<String,?> before){
        SharedPreferences.Editor edit=prefs.edit().clear();
        for(Map.Entry<String,?> e:before.entrySet()){
            Object v=e.getValue();String k=e.getKey();
            if(v instanceof Boolean)edit.putBoolean(k,(Boolean)v);else if(v instanceof Integer)edit.putInt(k,(Integer)v);
            else if(v instanceof String)edit.putString(k,(String)v);else if(v instanceof Long)edit.putLong(k,(Long)v);
            else if(v instanceof Float)edit.putFloat(k,(Float)v);
            else if(v instanceof java.util.Set)edit.putStringSet(k,new java.util.HashSet<>((java.util.Set<String>)v));
        }assertTrue(edit.commit());
    }
    @Test public void explicitBoundariesPersistInBackupCancelWithoutSavingAndResetOnlyCalibration()throws Exception{
        Context c=context();SharedPreferences prefs=c.getSharedPreferences(AppConfig.PREF_NAME,0);
        SharedPreferences observations=c.getSharedPreferences("hud_visibility_observations",0);
        Map<String,?> before=prefs.getAll(),logBefore=observations.getAll();
        int oldNight=androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode();
        ImageReader reader=ImageReader.newInstance(1280,640,PixelFormat.RGBA_8888,3);
        HandlerThread consumer=new HandlerThread("safe-area-ui-display");consumer.start();
        reader.setOnImageAvailableListener(r->{try(android.media.Image image=r.acquireLatestImage()){}},new Handler(consumer.getLooper()));
        VirtualDisplay display=((DisplayManager)c.getSystemService(Context.DISPLAY_SERVICE)).createVirtualDisplay(
                "Owned HUD safe area",1280,640,160,reader.getSurface(),DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                |DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION|DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
        assertNotNull(display);
        try{
            HudSafeArea.reset(c);AppConfig config=new AppConfig(c);config.selectHudLayout(true);config.setSidePopupYaw(40);
            main(()->AppTheme.apply(AppTheme.LIGHT));
            try(ActivityScenario<HudCalibrationActivity> scenario=ActivityScenario.launch(HudCalibrationActivity.class)){
                onView(withText(R.string.hud_test_start)).perform(click());SystemClock.sleep(250);
                Map<String,?> initial=prefs.getAll();
                move(scenario,100);onView(withText(R.string.hud_safe_set_left)).perform(scrollTo(),click());
                onView(withText(R.string.hud_safe_save)).check(matches(not(isEnabled())));
                assertNull(HudSafeArea.read(c));
                move(scenario,1040);onView(withText(R.string.hud_safe_set_right)).perform(scrollTo(),click());
                onView(withText(R.string.hud_safe_save)).check(matches(isEnabled()));
                onView(withText(R.string.hud_safe_title)).perform(scrollTo());capture("hud-safe-area-light.png");
                onView(withText(R.string.hud_safe_save)).perform(scrollTo(),click());
                scenario.onActivity(a->assertNull(session(a)));assertFalse(HudCalibrationSession.active());
                JSONObject saved=HudSafeArea.read(c);assertNotNull(saved);
                assertEquals(100,saved.getInt("left"));assertEquals(1180,saved.getInt("right"));assertEquals(2,saved.getInt("margin_percent"));
                Map<String,Object> after=new HashMap<>(prefs.getAll());after.remove(HudSafeArea.KEY);
                assertEquals(initial,after);assertEquals(40,new AppConfig(c).getSidePopupYaw());
                assertEquals(saved.toString(),SettingsBackup.snapshot(c).getJSONObject("stores").getJSONObject(AppConfig.PREF_NAME)
                        .getJSONObject(HudSafeArea.KEY).getString("value"));
                // Unsaved selections are abandoned on leaving; a new session loads saved limits.
                onView(withText(R.string.hud_test_start)).perform(click());SystemClock.sleep(200);
                move(scenario,200);onView(withText(R.string.hud_safe_set_left)).perform(scrollTo(),click());
                scenario.recreate();assertEquals(100,HudSafeArea.read(c).getInt("left"));
                scenario.onActivity(a->assertNull(session(a)));
                main(()->AppTheme.apply(AppTheme.DARK));SystemClock.sleep(250);
                onView(withText(R.string.hud_test_start)).perform(click());SystemClock.sleep(200);
                onView(withText(R.string.hud_safe_title)).perform(scrollTo());capture("hud-safe-area-dark.png");
                onView(withText(R.string.hud_safe_reset)).perform(scrollTo(),click());assertNull(HudSafeArea.read(c));
                assertEquals(initial,prefs.getAll());assertEquals(40,config.getSidePopupYaw());
            }
        }finally{
            main(()->AppTheme.apply(oldNight==androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES?AppTheme.DARK:
                    oldNight==androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO?AppTheme.LIGHT:AppTheme.SYSTEM));
            display.release();reader.setOnImageAvailableListener(null,null);consumer.quitSafely();consumer.join(1000);reader.close();
            restore(prefs,before);restore(observations,logBefore);
        }
    }
    @Test public void corruptOrDifferentViewportCalibrationIsNeverApplied()throws Exception{
        Context c=context();SharedPreferences prefs=c.getSharedPreferences(AppConfig.PREF_NAME,0);Map<String,?> before=prefs.getAll();
        try{
            JSONObject frame=new JSONObject().put("display_name","HUD").put("display_flags",8).put("display_width",1280)
                    .put("display_height",640).put("viewport_width",1280).put("viewport_height",640)
                    .put("viewport_origin_x",0).put("viewport_origin_y",0);
            JSONObject saved=HudSafeArea.record(frame,100,1180,2);assertTrue(HudSafeArea.save(c,saved));
            assertTrue(HudSafeArea.matches(HudSafeArea.read(c),frame));
            for(String key:new String[]{"display_name","display_flags","display_width","display_height","viewport_width",
                    "viewport_height","viewport_origin_x","viewport_origin_y"}){
                JSONObject changed=new JSONObject(frame.toString());changed.put(key,key.equals("display_name")?"Other HUD":999);
                assertFalse(key,HudSafeArea.matches(saved,changed));
            }
            prefs.edit().putString(HudSafeArea.KEY,"bad JSON").commit();assertNull(HudSafeArea.read(c));
            prefs.edit().putString(HudSafeArea.KEY,new JSONObject(saved.toString()).put("right",50).toString()).commit();assertNull(HudSafeArea.read(c));
            assertFalse(HudSafeArea.save(c,new JSONObject(saved.toString()).put("margin_percent",99)));
        }finally{restore(prefs,before);}
    }
}
