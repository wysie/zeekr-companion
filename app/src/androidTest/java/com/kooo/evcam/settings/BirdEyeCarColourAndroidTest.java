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
import com.kooo.evcam.zeekr.BirdEyeAppearance;
import com.kooo.evcam.zeekr.BirdEyeConfig;
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
public class BirdEyeCarColourAndroidTest {
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
    @Test public void pickerConfirmsAndPersistsColourIndependentlyOfCalibrationReset() throws Exception {
        BirdEyeAppearance appearance=new BirdEyeAppearance(target());Map<String,?> saved=appearance.preferences.getAll();
        BirdEyeConfig config=new BirdEyeConfig(target());Map<String,?> oldCalibration=config.preferences.getAll();
        SharedPreferences app=target().getSharedPreferences("app_config",Context.MODE_PRIVATE);
        Map<String,?> oldApp=app.getAll();
        for(String permission:new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}) InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .grantRuntimePermission(target().getPackageName(),permission);
        app.edit().putBoolean("first_launch",false).putBoolean("language_chosen",true)
                .putBoolean("rail_side_chosen",true).commit();
        appearance.preferences.edit().clear().commit();
        assertEquals(BirdEyeAppearance.WHITE,appearance.colour());
        appearance.setColour("future-colour"); assertEquals(BirdEyeAppearance.WHITE,appearance.colour());
        config.setImageFitted(true);config.set("length",550);
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity->{
                SettingsShellFragment shell=new SettingsShellFragment();
                activity.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,shell).commitNow();
                shell.getChildFragmentManager().executePendingTransactions();shell.showSection("screen_bird_eye");
                shell.getChildFragmentManager().executePendingTransactions();
                page(shell).scrollToPreference("pref_bird_car_colour");
                assertEquals("white",((ListPreference)page(shell).findPreference("pref_bird_car_colour")).getValue());
            });
            onView(withText(R.string.bird_eye_car_colour)).perform(click());capture("009-colour-options.png");
            onView(withText(R.string.bird_eye_car_green)).perform(click());
            assertEquals("Not committed before OK",BirdEyeAppearance.WHITE,appearance.colour());
            onView(withText(R.string.action_ok)).perform(click());assertEquals(BirdEyeAppearance.GREEN,appearance.colour());
            assertTrue(config.imageFitted());assertEquals(550,config.get("length",0));
            scenario.recreate();
            scenario.onActivity(activity->{
                SettingsShellFragment shell=(SettingsShellFragment)activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                shell.getChildFragmentManager().executePendingTransactions();
                ListPreference pref=page(shell).findPreference("pref_bird_car_colour");
                assertEquals("green",pref.getValue());assertEquals(activity.getString(R.string.bird_eye_car_green),pref.getSummary());
                page(shell).scrollToPreference("pref_bird_car_colour");
            });capture("009-colour-green.png");
            onView(withText(R.string.bird_eye_car_colour)).perform(click());
            onView(withText(R.string.bird_eye_car_black)).perform(click());
            onView(withText(R.string.action_ok)).perform(click());assertEquals(BirdEyeAppearance.BLACK,appearance.colour());
            scenario.onActivity(activity->{
                SettingsShellFragment shell=(SettingsShellFragment)activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                page(shell).findPreference("pref_bird_reset_calibration").performClick();
                assertFalse(config.imageFitted());assertEquals(BirdEyeAppearance.BLACK,appearance.colour());
                assertEquals("black",((ListPreference)page(shell).findPreference("pref_bird_car_colour")).getValue());
            });
        } finally { restore(appearance.preferences,saved);restore(config.preferences,oldCalibration);restore(app,oldApp); }
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
