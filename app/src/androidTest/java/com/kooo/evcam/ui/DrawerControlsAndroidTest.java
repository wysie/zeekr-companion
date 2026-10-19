package com.kooo.evcam.ui;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.navigation.NavigationView;
import com.kooo.evcam.*;
import com.kooo.evcam.camera.CameraManagerHolder;
import com.kooo.evcam.zeekr.*;
import java.io.File;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import static org.junit.Assert.*;

/** Disposable emulator, actual NavigationView rows and the existing 360 service authority. */
public class DrawerControlsAndroidTest extends MainPageControlsAndroidTest {
    private Context target() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private void await(String message, BooleanSupplier condition) {
        long end=SystemClock.elapsedRealtime()+10000;
        while(!condition.getAsBoolean()&&SystemClock.elapsedRealtime()<end)SystemClock.sleep(50);
        assertTrue(message,condition.getAsBoolean());
    }
    private void shell(String cmd) throws Exception {
        try(ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(cmd);
            java.io.InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)) { while(in.read()!=-1){} }
    }
    private void open(ActivityScenario<MainActivity> scenario) {
        scenario.onActivity(a->((DrawerLayout)a.findViewById(R.id.drawer_layout)).openDrawer(GravityCompat.START,false));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    private CompoundButton toggle(MainActivity a) {
        NavigationView nav=a.findViewById(R.id.nav_view);
        return nav.getMenu().findItem(R.id.nav_bird_eye_window).getActionView().findViewById(R.id.nav_switch);
    }
    private void screenshot(String name) throws Exception {
        // Let the emulator-only "no composite camera" toast expire before capturing the drawer.
        SystemClock.sleep(3500);
        android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new File(target().getExternalFilesDir(null),name+".png"))) {
            assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));
        } finally {image.recycle();}
    }
    @Test public void drawer360SwitchUsesSessionAuthorityAndKeepsDrawerAndPipeline() throws Exception {
        Context context=target(); shell("appops set "+context.getPackageName()+" SYSTEM_ALERT_WINDOW allow");
        Map<String,?> before=context.getSharedPreferences("bird_eye_window",0).getAll();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            await("Initial camera manager built",()->CameraManagerHolder.getInstance().getCameraManager()!=null
                    &&CameraManagerHolder.getInstance().getCameraManager().isBuilt());
            open(scenario);
            final com.kooo.evcam.camera.MultiCameraManager[] pipeline={null};
            scenario.onActivity(a->{
                pipeline[0]=CameraManagerHolder.getInstance().getCameraManager();
                NavigationView nav=a.findViewById(R.id.nav_view);
                android.view.SubMenu switches=nav.getMenu().getItem(3).getSubMenu();
                assertEquals(R.id.nav_rearview,switches.getItem(0).getItemId());
                assertEquals(R.id.nav_bird_eye_window,switches.getItem(1).getItemId());
                assertFalse(toggle(a).isChecked());
                assertTrue(nav.getMenu().performIdentifierAction(R.id.nav_bird_eye_window,0)==false);
            });
            await("The 360 session starts",BirdEyePopupService::isRunning);
            final boolean[] checked={false};
            await("Drawer reflects asynchronous start",()->{scenario.onActivity(a->checked[0]=toggle(a).isChecked());return checked[0];});
            scenario.onActivity(a->{
                assertTrue(((DrawerLayout)a.findViewById(R.id.drawer_layout)).isDrawerOpen(GravityCompat.START));
                assertSame(pipeline[0],CameraManagerHolder.getInstance().getCameraManager());
                View row=a.findViewById(R.id.nav_bird_eye_window);assertNotNull(row);
                android.view.accessibility.AccessibilityNodeInfo node=row.createAccessibilityNodeInfo();
                assertTrue(node.isCheckable());assertTrue(node.isChecked());node.recycle();
            });
            // External stop covers the popup's X/service-close route while the drawer remains open.
            main(()->BirdEyePopupService.stop(context));
            await("The popup stops",()->!BirdEyePopupService.isRunning());
            await("Drawer follows external closure",()->{scenario.onActivity(a->checked[0]=toggle(a).isChecked());return !checked[0];});
            main(()->BirdEyePopupService.start(context));await("External start",BirdEyePopupService::isRunning);
            await("Drawer follows other entry points",()->{scenario.onActivity(a->checked[0]=toggle(a).isChecked());return checked[0];});
            scenario.onActivity(a->((NavigationView)a.findViewById(R.id.nav_view)).getMenu().performIdentifierAction(R.id.nav_bird_eye_window,0));
            await("Drawer can stop the same session",()->!BirdEyePopupService.isRunning());
            assertEquals("No boot/session preference is created by the drawer",before,context.getSharedPreferences("bird_eye_window",0).getAll());
            scenario.onActivity(a->((NavigationView)a.findViewById(R.id.nav_view)).getMenu().performIdentifierAction(R.id.nav_settings,0));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.recreate();open(scenario);
            scenario.onActivity(a->assertEquals("Restored Settings retains the correct drawer destination",R.id.nav_settings,
                    ((NavigationView)a.findViewById(R.id.nav_view)).getCheckedItem().getItemId()));
        } finally {main(()->BirdEyePopupService.stop(context));}
    }
    private void checkRows(MainActivity a) {
        NavigationView nav=a.findViewById(R.id.nav_view);
        assertEquals(0f,((DrawerLayout)a.findViewById(R.id.drawer_layout)).getDrawerElevation(),0f);
        List<TextView> labels=new ArrayList<>(); collect(nav,labels);
        int matched=0;
        for(TextView text:labels) {
            String value=text.getText().toString();
            if(value.equals(a.getString(R.string.nav_recording))||value.equals(a.getString(R.string.about_title))
                    ||value.equals(a.getString(R.string.nav_exit))||value.equals(a.getString(R.string.main_360_window))) {
                assertFalse("Drawer labels use regular weight",text.getTypeface().isBold());
                float sp=text.getTextSize()/a.getResources().getDisplayMetrics().scaledDensity;
                assertEquals("Every destination keeps the same size",18f,sp,0.2f);
                if(text.getLayout()!=null)for(int i=0;i<text.getLayout().getLineCount();i++)assertEquals(0,text.getLayout().getEllipsisCount(i));
                matched++;
            }
        }
        assertTrue("Visible native labels were checked",matched>=2);
    }
    private void collect(View view,List<TextView> out) {
        if(view instanceof TextView)out.add((TextView)view);
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)collect(((ViewGroup)view).getChildAt(i),out);
    }
    @Test public void nativeDrawerFitsLightDarkAndLargerTextWithoutChangingCameraOrRecordingChoices() throws Exception {
        Map<String,?> choices=target().getSharedPreferences("profiles",0).getAll();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            for(int mode:new int[]{AppCompatDelegate.MODE_NIGHT_NO,AppCompatDelegate.MODE_NIGHT_YES}) {
                main(()->AppCompatDelegate.setDefaultNightMode(mode));SystemClock.sleep(800);open(scenario);
                scenario.onActivity(this::checkRows);screenshot(mode==AppCompatDelegate.MODE_NIGHT_NO?"drawer-light":"drawer-dark");
            }
            shell("settings put system font_scale 1.3");SystemClock.sleep(900);open(scenario);
            scenario.onActivity(this::checkRows);screenshot("drawer-dark-large");
            assertEquals(choices,target().getSharedPreferences("profiles",0).getAll());
        } finally {shell("settings put system font_scale 1.0");}
    }
}
