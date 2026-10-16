package com.kooo.evcam.settings;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.view.View;
import androidx.preference.ListPreference;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.zeekr.BirdEyeConfig;
import com.kooo.evcam.zeekr.BirdEyeProjection;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class BirdEyeImageFitAndroidTest {
    private Context target() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private void restore(SharedPreferences prefs, Map<String,?> saved) {
        SharedPreferences.Editor edit=prefs.edit().clear();
        for(Map.Entry<String,?> e:saved.entrySet()) {
            Object v=e.getValue();
            if(v instanceof Integer) edit.putInt(e.getKey(),(Integer)v);
            else if(v instanceof Boolean) edit.putBoolean(e.getKey(),(Boolean)v);
            else if(v instanceof String) edit.putString(e.getKey(),(String)v);
            else if(v instanceof Long) edit.putLong(e.getKey(),(Long)v);
            else if(v instanceof Float) edit.putFloat(e.getKey(),(Float)v);
        }
        assertTrue(edit.commit());
    }
    @Test public void profileKeepsManualValuesQualityAndLaneAssignmentsAndResetRestoresManual() {
        BirdEyeConfig config=new BirdEyeConfig(target());Map<String,?> saved=config.preferences.getAll();
        try {
            config.reset(); assertFalse(config.imageFitted());
            config.set("length",550);config.set("width",210);config.set("margin",300);
            config.set("quality",2);config.set(BirdEyeConfig.key(0,"pitch"),47);
            config.assignLane(0,3);config.setImageFitted(true);
            BirdEyeProjection.Calibration fit=config.read();
            assertTrue(fit.imageFitted());assertEquals(5.2,fit.length,0);assertEquals(2,fit.width,0);
            assertEquals(2,fit.margin,0);assertEquals(3,fit.cameras[0].lane);assertEquals(0,fit.cameras[3].lane);
            assertEquals(2,config.quality());
            config.setImageFitted(false);
            BirdEyeProjection.Calibration manual=config.read();
            assertFalse(manual.imageFitted());assertEquals(5.5,manual.length,0);
            assertEquals(2.1,manual.width,0);assertEquals(3,manual.margin,0);
            assertEquals(47,manual.cameras[0].pitch,0);
            config.set("projection_profile",99);assertFalse(config.read().imageFitted());
            config.setImageFitted(true);config.reset();assertFalse(config.imageFitted());
        } finally { restore(config.preferences,saved); }
    }
    @Test public void nativeProfilePickerRequiresConfirmationAndPersistsAcrossRecreation() throws Exception {
        BirdEyeConfig config=new BirdEyeConfig(target());Map<String,?> saved=config.preferences.getAll();
        SharedPreferences app=target().getSharedPreferences("app_config",Context.MODE_PRIVATE);
        Map<String,?> oldApp=app.getAll();boolean developer=DeveloperMode.isUnlocked();
        for(String permission:new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}) InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .grantRuntimePermission(target().getPackageName(),permission);
        app.edit().putBoolean("first_launch",false).putBoolean("language_chosen",true)
                .putBoolean("rail_side_chosen",true).commit();
        DeveloperMode.setEnabled(target(),true);config.setImageFitted(false);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity->{
                SettingsShellFragment shell=new SettingsShellFragment();
                activity.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,shell).commitNow();
                shell.getChildFragmentManager().executePendingTransactions();shell.showSection("screen_bird_eye");
                shell.getChildFragmentManager().executePendingTransactions();
                SettingsPreferenceFragment page=page(shell);
                assertEquals("0",((ListPreference)page.findPreference("pref_bird_profile")).getValue());
                page.scrollToPreference("pref_bird_profile");
            });
            capture("009-profile-row.png");
            onView(withText(R.string.bird_eye_profile)).perform(click());capture("009-profile-options.png");
            onView(withText(R.string.bird_eye_profile_009)).perform(click());
            assertFalse("Selection is not committed before OK",config.imageFitted());
            onView(withText(R.string.action_ok)).perform(click());assertTrue(config.imageFitted());
            scenario.recreate();
            scenario.onActivity(activity->{
                SettingsShellFragment shell=(SettingsShellFragment)activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                shell.getChildFragmentManager().executePendingTransactions();
                ListPreference pref=page(shell).findPreference("pref_bird_profile");
                assertEquals("1",pref.getValue());assertEquals(activity.getString(R.string.bird_eye_profile_009),pref.getSummary());
                page(shell).scrollToPreference("pref_bird_profile");
            });capture("009-profile-selected.png");
            scenario.onActivity(activity->{
                SettingsShellFragment shell=(SettingsShellFragment)activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                page(shell).findPreference("pref_bird_reset_calibration").performClick();
                assertFalse(config.imageFitted());assertEquals("0",((ListPreference)page(shell).findPreference("pref_bird_profile")).getValue());
            });
        } finally { restore(config.preferences,saved);restore(app,oldApp);DeveloperMode.setEnabled(target(),developer); }
    }
    private SettingsPreferenceFragment page(SettingsShellFragment shell) {
        return (SettingsPreferenceFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);
    }
    private void capture(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.os.SystemClock.sleep(180);
        Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(image);
        try(FileOutputStream out=new FileOutputStream(new File(target().getExternalFilesDir(null),name))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,out));
        } finally { image.recycle(); }
    }
}
