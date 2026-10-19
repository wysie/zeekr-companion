package com.kooo.evcam.settings;

import android.os.SystemClock;
import android.view.View;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.ListPreference;
import androidx.preference.SwitchPreferenceCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.*;
import com.kooo.evcam.camera.*;
import com.kooo.evcam.ui.MainPageControlsAndroidTest;
import com.kooo.evcam.zeekr.GpuRendering;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** Isolated emulator policy/Settings checks, with original preferences restored by the fixture. */
public class GpuRenderingAndroidTest extends MainPageControlsAndroidTest {
    private android.content.Context target(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
    @Test public void legacyMixedChoicesMigrateWithoutEnablingNewPathsAndCustomSurvivesAllOff() {
        AppConfig config=new AppConfig(target());
        target().getSharedPreferences("app_config",0).edit().remove(GpuRendering.MODE)
                .putBoolean("gpu_fisheye_preview",false).putBoolean("gpu_fisheye_video",false)
                .putBoolean("gpu_bird_eye",true).putBoolean("gpu_side",false).putBoolean("gpu_mirror",false).commit();
        assertEquals("custom",config.getGpuRenderingMode());assertTrue(config.isGpuBirdEye());assertFalse(config.isGpuSide());
        config.setGpuFeature("side",true);
        assertEquals("custom",target().getSharedPreferences("app_config",0).getString(GpuRendering.MODE,null));
        assertTrue(config.isGpuBirdEye());assertTrue(config.isGpuSide());
        config.setGpuFeature("side",false);
        config.setGpuRenderingMode("all");
        for(String feature:GpuRendering.FEATURES)assertTrue(feature,config.isGpuFeatureEnabled(feature));
        config.setGpuRenderingMode("off");
        for(String feature:GpuRendering.FEATURES)assertFalse(feature,config.isGpuFeatureEnabled(feature));
        CameraSessionOptions options=CameraSessionOptions.read(target());
        assertFalse(options.gpuPreview||options.gpu360||options.gpuSide||options.gpuMirror);
        config.setGpuRenderingMode("custom");assertTrue(config.isGpuBirdEye());assertFalse(config.isGpuSide());
        config.setGpuFeature("side",true);config.setGpuRenderingMode("all");config.setGpuRenderingMode("off");config.setGpuRenderingMode("custom");
        assertTrue(new AppConfig(target()).isGpuSide());assertFalse(new AppConfig(target()).isGpuMirror());
    }
    @Test public void systemControlsReflectAllOffCustomAndStageCameraChangesWithoutEditingRecording() throws Exception {
        AppConfig config=new AppConfig(target());config.setGpuRenderingMode("off");
        Map<String,?> recording=target().getSharedPreferences("profiles",0).getAll();
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            long until=SystemClock.elapsedRealtime()+10000;
            while((CameraManagerHolder.getInstance().getCameraManager()==null || !CameraManagerHolder.getInstance().getCameraManager().isBuilt())&&SystemClock.elapsedRealtime()<until)SystemClock.sleep(50);
            Object pipeline=CameraManagerHolder.getInstance().getCameraManager();
            scenario.onActivity(a->{
                SettingsShellFragment shell=new SettingsShellFragment();
                a.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
                a.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,shell).commitNow();
                shell.showSection("screen_system");shell.getChildFragmentManager().executePendingTransactions();
                SettingsPreferenceFragment system=(SettingsPreferenceFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);
                assertNotNull(system.findPreference("sub_gpu_processing"));
                shell.onPreferenceStartScreen(system,system.findPreference("sub_gpu_processing"));shell.getChildFragmentManager().executePendingTransactions();
                SettingsPreferenceFragment fragment=(SettingsPreferenceFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);
                ListPreference mode=fragment.findPreference("pref_gpu_mode");assertNotNull(mode);
                assertTrue(mode.callChangeListener("all")==false);
                for(String feature:GpuRendering.FEATURES){SwitchPreferenceCompat row=fragment.findPreference(GpuRendering.preference(feature));assertTrue(row.isChecked());assertFalse(row.isEnabled());}
                assertFalse(mode.callChangeListener("off"));
                for(String feature:GpuRendering.FEATURES){SwitchPreferenceCompat row=fragment.findPreference(GpuRendering.preference(feature));assertFalse(row.isChecked());assertFalse(row.isEnabled());}
                assertFalse(mode.callChangeListener("custom"));
                SwitchPreferenceCompat side=fragment.findPreference("pref_gpu_side");assertTrue(side.isEnabled());assertTrue(side.callChangeListener(true));
                assertTrue(config.isGpuSide());assertFalse(config.isGpuMirror());
                assertSame("Editing rendering never rebuilds immediately",pipeline,CameraManagerHolder.getInstance().getCameraManager());
                assertTrue("Side-only route edits participate in Apply",CameraManagerHolder.getInstance().hasPendingChannelChange(a));
                assertEquals(recording,target().getSharedPreferences("profiles",0).getAll());
            });
            if("true".equals(InstrumentationRegistry.getArguments().getString("captureGpuSettings"))){
                for(int theme:new int[]{AppCompatDelegate.MODE_NIGHT_NO,AppCompatDelegate.MODE_NIGHT_YES}){
                    InstrumentationRegistry.getInstrumentation().runOnMainSync(()->AppCompatDelegate.setDefaultNightMode(theme));SystemClock.sleep(500);
                    android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                    try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(target().getExternalFilesDir(null),theme==AppCompatDelegate.MODE_NIGHT_NO?"gpu-system-light.png":"gpu-system-dark.png"))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}finally{image.recycle();}
                }
            }
        }
    }
}
