package com.kooo.evcam.settings;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.view.View;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreferenceCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Exercises real nested preferences and their visibility, persistence and write targets. */
@RunWith(AndroidJUnit4.class)
public class SettingsHudPictureAndroidTest {
    @Test public void followingHidesOverridesButKeepsPlacementAvailable() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = target.getSharedPreferences("app_config", Context.MODE_PRIVATE);
        Map<String, ?> before = prefs.getAll();
        try {
            for (String permission : new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}) InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .grantRuntimePermission(target.getPackageName(), permission);
            prefs.edit().clear().putBoolean("first_launch", false).putBoolean("language_chosen", true)
                    .putBoolean("rail_side_chosen", true).commit();
            AppConfig config = new AppConfig(target);
            config.setSidePopupEnabled(true); config.setSidePopupYaw(50);
            config.setSidePopupStraighten(true); config.setHudSideShapePercent(200);
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(activity -> {
                    SettingsShellFragment shell = new SettingsShellFragment();
                    activity.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
                    activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, shell).commitNow();
                    shell.getChildFragmentManager().executePendingTransactions();
                    shell.showSection("screen_blind_spot_camera"); shell.getChildFragmentManager().executePendingTransactions();
                    SettingsPreferenceFragment page = page(shell);
                    page.findPreference("sub_side_hud").performClick(); shell.getChildFragmentManager().executePendingTransactions();
                    page = page(shell);
                    SwitchPreferenceCompat follow = page.findPreference("pref_hud_follow_popup_picture");
                    assertTrue(follow.isChecked()); assertFalse(follow.isPersistent());
                    assertFalse(page.findPreference("group_hud_custom_picture").isVisible());
                    assertTrue(page.findPreference("group_hud_placement").isVisible());
                    assertTrue(page.findPreference("pref_hud_side_shape").isEnabled());
                    SeekBarPreference crop=page.findPreference("pref_hud_side_inner_crop");
                    assertTrue(crop.isEnabled()); assertFalse(crop.isPersistent());
                    assertTrue(crop.callChangeListener(35)); crop.setValue(35);
                    assertEquals(35,config.getHudSideInnerCropPercent());
                    assertEquals(50,config.getSidePopupYaw());
                });
                capture("hud-follow");
                scenario.onActivity(activity -> {
                    SettingsPreferenceFragment page = current(activity);
                    page.findPreference("pref_hud_follow_popup_picture").performClick();
                    assertFalse(config.isHudFollowingPopupPicture());
                    assertTrue(page.findPreference("group_hud_custom_picture").isVisible());
                    SeekBarPreference yaw = page.findPreference("pref_hud_picture_yaw");
                    assertEquals(50, yaw.getValue());
                    assertTrue(yaw.callChangeListener(35)); yaw.setValue(35);
                    assertEquals(35, config.getHudSideYaw()); assertEquals(50, config.getSidePopupYaw());
                    SwitchPreferenceCompat straighten = page.findPreference("pref_hud_picture_straighten");
                    straighten.performClick(); assertFalse(page.findPreference("pref_hud_picture_auto_fit").isEnabled());
                    straighten.performClick(); assertTrue(page.findPreference("pref_hud_picture_auto_fit").isEnabled());
                    page.findPreference("pref_hud_follow_popup_picture").performClick();
                    assertTrue(config.isHudFollowingPopupPicture());
                    assertFalse(page.findPreference("group_hud_custom_picture").isVisible());
                    config.setSidePopupYaw(60);
                    assertEquals(60, config.getHudSideYaw());
                    page.findPreference("pref_hud_follow_popup_picture").performClick();
                    assertEquals(35, ((SeekBarPreference) page.findPreference("pref_hud_picture_yaw")).getValue());
                    assertEquals(35, ((SeekBarPreference) page.findPreference("pref_hud_side_inner_crop")).getValue());
                    assertEquals(200, config.getHudSideShapePercent());
                });
                capture("hud-custom");
                scenario.recreate();
                scenario.onActivity(activity -> {
                    SettingsPreferenceFragment page = current(activity);
                    assertFalse(((SwitchPreferenceCompat) page.findPreference("pref_hud_follow_popup_picture")).isChecked());
                    assertTrue(page.findPreference("group_hud_custom_picture").isVisible());
                    assertEquals(35, ((SeekBarPreference) page.findPreference("pref_hud_picture_yaw")).getValue());
                    page.scrollToPreference("group_hud_placement");
                });
                capture("hud-placement");
            }
        } finally {
            SharedPreferences.Editor edit = prefs.edit().clear();
            for (Map.Entry<String, ?> entry : before.entrySet()) {
                Object value = entry.getValue(); String key = entry.getKey();
                if (value instanceof Boolean) edit.putBoolean(key, (Boolean) value);
                else if (value instanceof Integer) edit.putInt(key, (Integer) value);
                else if (value instanceof Long) edit.putLong(key, (Long) value);
                else if (value instanceof Float) edit.putFloat(key, (Float) value);
                else if (value instanceof String) edit.putString(key, (String) value);
                else if (value instanceof java.util.Set) edit.putStringSet(key,
                        new java.util.HashSet<>((java.util.Set<String>) value));
            }
            assertTrue(edit.commit());
        }
    }

    private SettingsPreferenceFragment page(SettingsShellFragment shell) {
        return (SettingsPreferenceFragment) shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);
    }
    private SettingsPreferenceFragment current(MainActivity activity) {
        return page((SettingsShellFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container));
    }
    private void capture(String name) throws Exception {
        if (!"true".equals(InstrumentationRegistry.getArguments().getString("captureHudSettings"))) return;
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        android.os.SystemClock.sleep(250);
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        java.io.File file = new java.io.File(InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getExternalFilesDir(null), name + ".png");
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
        } finally { bitmap.recycle(); }
    }
}
