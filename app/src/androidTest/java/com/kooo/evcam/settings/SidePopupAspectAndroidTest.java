package com.kooo.evcam.settings;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import androidx.preference.ListPreference;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.zeekr.LaneCycle;
import com.kooo.evcam.zeekr.SidePopupSizing;
import com.kooo.evcam.zeekr.SideViewPopupView;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/** Disposable emulator: real window geometry, gestures and confirmed native choice UI. */
@RunWith(AndroidJUnit4.class)
public class SidePopupAspectAndroidTest {
    private Context target;
    private AppConfig config;
    private Map<String,?> original;
    private boolean oldDeveloper;
    private SideViewPopupView window;
    private void main(Runnable r) { InstrumentationRegistry.getInstrumentation().runOnMainSync(r); }
    @Before public void setup() throws Exception {
        target=InstrumentationRegistry.getInstrumentation().getTargetContext();
        original=target.getSharedPreferences("app_config",Context.MODE_PRIVATE).getAll();
        oldDeveloper=DeveloperMode.isUnlocked();
        DeveloperMode.setEnabled(target,true);
        for(String permission:new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}) InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .grantRuntimePermission(target.getPackageName(),permission);
        try(ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("appops set "+target.getPackageName()+" SYSTEM_ALERT_WINDOW allow");
            java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)) { while(in.read()!=-1){} }
        target.getSharedPreferences("app_config",Context.MODE_PRIVATE).edit().putBoolean("first_launch",false)
                .putBoolean("language_chosen",true).putBoolean("rail_side_chosen",true).commit();
        config=new AppConfig(target);config.setSidePopupEnabled(true);
        config.setSidePopupAspect("1:1");
    }
    @After @SuppressWarnings("unchecked") public void restore() {
        main(()->{ if(window!=null) window.detach(); });
        SharedPreferences.Editor edit=target.getSharedPreferences("app_config",Context.MODE_PRIVATE).edit().clear();
        for(Map.Entry<String,?> e:original.entrySet()) {
            Object v=e.getValue();String k=e.getKey();
            if(v instanceof Boolean) edit.putBoolean(k,(Boolean)v);
            else if(v instanceof Integer) edit.putInt(k,(Integer)v);
            else if(v instanceof String) edit.putString(k,(String)v);
            else if(v instanceof Long) edit.putLong(k,(Long)v);
            else if(v instanceof Float) edit.putFloat(k,(Float)v);
            else if(v instanceof java.util.Set) edit.putStringSet(k,new java.util.HashSet<>((java.util.Set<String>)v));
        }
        assertTrue(edit.commit());DeveloperMode.setEnabled(target,oldDeveloper);
    }
    private WindowManager.LayoutParams geometry() { return (WindowManager.LayoutParams)window.getLayoutParams(); }
    private View control(View v,int text) {
        if(target.getString(text).contentEquals(v.getContentDescription()==null?"":v.getContentDescription())) return v;
        if(v instanceof ViewGroup) for(int i=0;i<((ViewGroup)v).getChildCount();i++) {
            View result=control(((ViewGroup)v).getChildAt(i),text);if(result!=null)return result;
        }
        return null;
    }
    private void settled() {
        long deadline=SystemClock.uptimeMillis()+4000;boolean[] ready={false};
        while(!ready[0]&&SystemClock.uptimeMillis()<deadline) {
            main(()->ready[0]=window.getWidth()==geometry().width&&window.getHeight()==geometry().height);
            if(!ready[0])SystemClock.sleep(20);
        }
        assertTrue("Window layout must settle",ready[0]);
    }
    private void checkBoundsAndRatio(String shape) {
        WindowManager.LayoutParams p=geometry();
        assertEquals(SidePopupSizing.aspect(shape),p.width/(double)p.height,.01);
        assertTrue(p.x>=0&&p.y>=0);
        assertTrue(p.x+p.width<=window.getResources().getDisplayMetrics().widthPixels);
        assertTrue(p.y+p.height<=window.getResources().getDisplayMetrics().heightPixels);
        assertEquals(View.VISIBLE,control(window,R.string.side_control_expand).getVisibility());
        assertEquals(View.VISIBLE,control(window,R.string.side_control_close).getVisibility());
    }
    private void pointers(long time,int action,float left,float right) {
        MotionEvent.PointerProperties[] properties=new MotionEvent.PointerProperties[2];
        MotionEvent.PointerCoords[] coords=new MotionEvent.PointerCoords[2];
        for(int i=0;i<2;i++) {
            properties[i]=new MotionEvent.PointerProperties();properties[i].id=i;
            properties[i].toolType=MotionEvent.TOOL_TYPE_FINGER;
            coords[i]=new MotionEvent.PointerCoords();coords[i].x=i==0?left:right;
            coords[i].y=200;coords[i].pressure=1;coords[i].size=1;
        }
        MotionEvent e=MotionEvent.obtain(time,time+20,action,2,properties,coords,0,0,1,1,0,0,
                android.view.InputDevice.SOURCE_TOUCHSCREEN,0);window.dispatchTouchEvent(e);e.recycle();
    }
    @Test public void shapeSurvivesPinchSideChangesExpansionAndScreenBounds() throws Exception {
        config.setSidePopupSizePercent(30);
        config.setSidePopupPosition(2500,1500,1600,480);
        int hudShape=config.getHudSideShapePercent(),output=config.getSideOutput();
        main(()->{ window=new SideViewPopupView(new ContextThemeWrapper(target,R.style.Theme_Cam),config);
            window.show(LaneCycle.LEFT); });
        settled();
        for(String shape:new String[]{"1:1","3:4","4:3","16:9","11:5"}) {
            main(()->{config.setSidePopupAspect(shape);window.applyConfig();});settled();
            main(()->{
                checkBoundsAndRatio(shape);
                WindowManager.LayoutParams p=geometry();int x=p.x,y=p.y,w=p.width,h=p.height;
                window.show(LaneCycle.RIGHT);assertEquals(x,geometry().x);assertEquals(y,geometry().y);
                control(window,R.string.side_control_expand).performClick();
                assertEquals(SidePopupSizing.aspect(shape),geometry().width/(double)geometry().height,.01);
                control(window,R.string.side_control_restore).performClick();
                assertEquals(x,geometry().x);assertEquals(y,geometry().y);
                assertEquals(w,geometry().width);assertEquals(h,geometry().height);
            });settled();
        }
        main(()->{config.setSidePopupAspect("16:9");window.applyConfig();});settled();
        main(()->{
            int x=geometry().x,y=geometry().y;
            long t=SystemClock.uptimeMillis();
            for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP}) {
                MotionEvent e=MotionEvent.obtain(t,t+20,action,action==MotionEvent.ACTION_DOWN?300:220,
                        action==MotionEvent.ACTION_DOWN?250:210,0);
                window.dispatchTouchEvent(e);e.recycle();
            }
            assertTrue(geometry().x<x&&geometry().y<y);
            assertEquals(geometry().x,config.getSidePopupX());assertEquals(geometry().y,config.getSidePopupY());
        });settled();
        int[] before=new int[2];
        main(()->{
            before[0]=geometry().width;before[1]=geometry().height;
            long t=SystemClock.uptimeMillis();
            MotionEvent down=MotionEvent.obtain(t,t,MotionEvent.ACTION_DOWN,80,200,0);
            window.dispatchTouchEvent(down);down.recycle();
            pointers(t,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),80,420);
            pointers(t+40,MotionEvent.ACTION_MOVE,20,480);
            pointers(t+80,MotionEvent.ACTION_MOVE,-20,540);
            pointers(t+120,MotionEvent.ACTION_POINTER_UP|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),-20,540);
            MotionEvent up=MotionEvent.obtain(t,t+140,MotionEvent.ACTION_UP,30,200,0);
            window.dispatchTouchEvent(up);up.recycle();
            assertTrue("Pinch must enlarge width",geometry().width>before[0]);
            assertTrue("Pinch must enlarge height",geometry().height>before[1]);
        });settled();main(()->checkBoundsAndRatio("16:9"));
        assertEquals("16:9",new AppConfig(target).getSidePopupAspect());
        assertEquals(hudShape,config.getHudSideShapePercent());assertEquals(output,config.getSideOutput());
        main(()->{control(window,R.string.side_control_expand).performClick();
            config.setSidePopupAspect("4:3");window.applyConfig();});settled();
        main(()->checkBoundsAndRatio("4:3"));
        config.setSidePopupAutoFit(false);
        config.resetSidePopupSettings();assertEquals("1:1",config.getSidePopupAspect());
        assertTrue("Reset restores default-on auto-fit",config.isSidePopupAutoFit());
        assertTrue("Reset keeps the feature enabled",config.isSidePopupEnabled());
    }
    @Test public void pickerRequiresConfirmationAndPersistsAfterRecreation() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<SettingsShellFragment> current=new AtomicReference<>();
            scenario.onActivity(activity->{
                SettingsShellFragment shell=new SettingsShellFragment();current.set(shell);
                activity.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,shell).commitNow();
                shell.getChildFragmentManager().executePendingTransactions();
                shell.showSection("screen_blind_spot_camera");shell.getChildFragmentManager().executePendingTransactions();
                shell.openDetail(SettingsPreferenceFragment.forSection("sub_side_window"),activity.getString(R.string.menu_popup_window));
                shell.getChildFragmentManager().executePendingTransactions();
                assertEquals("1:1",((ListPreference)page(shell).findPreference("pref_side_popup_aspect")).getValue());
            });
            onView(withText(R.string.set_side_popup_aspect_title)).perform(click());capture("popup-aspect-options.png");
            onView(withText(R.string.side_aspect_wide)).perform(click());assertEquals("1:1",config.getSidePopupAspect());
            onView(withText(R.string.action_cancel)).perform(click());assertEquals("1:1",config.getSidePopupAspect());
            onView(withText(R.string.set_side_popup_aspect_title)).perform(click());
            onView(withText(R.string.side_aspect_wide)).perform(click());
            onView(withText(R.string.action_ok)).perform(click());assertEquals("16:9",config.getSidePopupAspect());
            scenario.recreate();scenario.onActivity(activity->{
                SettingsShellFragment shell=(SettingsShellFragment)activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                shell.getChildFragmentManager().executePendingTransactions();
                ListPreference pref=page(shell).findPreference("pref_side_popup_aspect");
                assertEquals("16:9",pref.getValue());assertEquals(activity.getString(R.string.side_aspect_wide),pref.getSummary());
            });capture("popup-aspect-selected.png");
        }
    }
    private SettingsPreferenceFragment page(SettingsShellFragment shell) {
        return (SettingsPreferenceFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);
    }
    private void capture(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(image);
        try(FileOutputStream out=new FileOutputStream(new File(target.getExternalFilesDir(null),name))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));
        } finally {image.recycle();}
    }
}
