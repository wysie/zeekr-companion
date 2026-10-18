package com.kooo.evcam.settings;

import android.Manifest;
import android.content.*;
import android.os.Build;
import android.view.View;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.*;
import com.kooo.evcam.camera.CameraManagerHolder;
import com.kooo.evcam.profile.*;
import com.kooo.evcam.zeekr.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class VehicleProfileAndroidTest {
    private Context target;
    private Map<String, ?> appBefore, birdBefore;
    private AppConfig config;
    @Before public void setup() {
        assumeTrue(Build.PRODUCT.contains("sdk") || Build.HARDWARE.contains("ranchu"));
        target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        appBefore = target.getSharedPreferences("app_config", 0).getAll();
        birdBefore = target.getSharedPreferences("bird_eye_calibration", 0).getAll();
        target.getSharedPreferences("app_config", 0).edit().clear().putBoolean("first_launch", false)
                .putBoolean("language_chosen", true).putBoolean("rail_side_chosen", true).commit();
        target.getSharedPreferences("bird_eye_calibration", 0).edit().clear().commit();
        config = new AppConfig(target);
        config.setAutoStartRecording(false); config.setDriveAutoStart(false);
        config.setSidePopupEnabled(false); config.setRearViewEnabled(false);
        for (String p : new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE})
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(target.getPackageName(), p);
    }
    @SuppressWarnings("unchecked") private void restore(String name, Map<String, ?> values) {
        SharedPreferences.Editor e = target.getSharedPreferences(name, 0).edit().clear();
        for (Map.Entry<String, ?> item : values.entrySet()) {
            Object v = item.getValue(); String k = item.getKey();
            if (v instanceof String) e.putString(k, (String)v);
            else if (v instanceof Boolean) e.putBoolean(k, (Boolean)v);
            else if (v instanceof Integer) e.putInt(k, (Integer)v);
            else if (v instanceof Long) e.putLong(k, (Long)v);
            else if (v instanceof Float) e.putFloat(k, (Float)v);
            else if (v instanceof Set) e.putStringSet(k, new HashSet<>((Set<String>)v));
        }
        e.commit();
    }
    @After public void cleanup() {
        if (target == null) return;
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> CameraManagerHolder.getInstance().release());
        restore("app_config", appBefore); restore("bird_eye_calibration", birdBefore);
    }
    @Test public void legacyOptInMigratesButCosmeticNameNeverChangesCameraSources() {
        config.setVehicleNameMode("009");
        assertEquals("auto", config.getVehicleProfileMode());
        assertFalse(config.is009CameraMappingEnabled());
        config.set009CameraMappingEnabled(true);
        assertEquals("009", config.getVehicleProfileMode());
        assertTrue(config.is009CameraMappingEnabled());
        assertEquals("3", ProfileSizes.cameraIdFor(target, CameraProfile.ROLE_COMPOSITE));
        assertEquals("2", ProfileSizes.cameraIdFor(target, CameraProfile.ROLE_CABIN_1));
        assertEquals("0", ProfileSizes.cameraIdFor(target, CameraProfile.ROLE_ROAD_FRONT));
        assertEquals("1", ProfileSizes.cameraIdFor(target, CameraProfile.ROLE_ROAD_REAR));
        assertNull(ProfileSizes.cameraIdFor(target, CameraProfile.ROLE_CABIN_2));
        config.setVehicleProfileMode("7x");
        assertFalse(config.is009CameraMappingEnabled());
        assertNull(ProfileSizes.cameraIdFor(target, CameraProfile.ROLE_ROAD_FRONT));
        assertNull(ProfileSizes.cameraIdFor(target, CameraProfile.ROLE_ROAD_REAR));
        config.setVehicleProfileMode("auto");
        assertFalse(config.is009CameraMappingEnabled()); // Anonymous emulator is not a model detector.
    }
    @Test public void switchingModelsPreservesOverridesRecordingChoicesAndFittedCalibration() {
        String role = CameraProfile.ROLE_ROAD_FRONT;
        config.setSourceCameraId(role, "12");
        config.setPreviewSourceMode("roads");
        config.setSidePopupFov(105);
        String profile = new ProfileStore(target).current().toMap().toString();
        BirdEyeConfig bird = new BirdEyeConfig(target);
        bird.setImageFitted(true); bird.set(BirdEyeConfig.FIT_MARGIN, 275);
        config.selectHudLayout(true); config.setVehicleProfileMode("009");
        assertTrue(bird.imageFitted());
        config.setVehicleProfileMode("7x");
        assertFalse(bird.imageFitted());
        assertEquals(1, bird.preferences.getInt("projection_profile", 0));
        assertEquals("12", ProfileSizes.cameraIdFor(target, role));
        assertEquals("roads", config.getPreviewSourceMode());
        assertEquals(105, config.getSidePopupFov());
        assertEquals(profile, new ProfileStore(target).current().toMap().toString());
        assertTrue(config.is009HudLayout()); // Model selection cannot redirect or reset an actual display match.
        config.setVehicleProfileMode("009");
        assertTrue(bird.imageFitted()); assertEquals(275, bird.get(BirdEyeConfig.FIT_MARGIN, 0));
        assertFalse(config.isAutoStartRecording()); assertFalse(config.isDriveAutoStart());
    }
    @Test public void automatic360FollowsModelWithoutWritingAnOverrideAndRetainsManualTuning() {
        BirdEyeConfig bird = new BirdEyeConfig(target);
        config.setVehicleProfileMode("009");
        assertEquals("-1", bird.projectionMode()); assertTrue(bird.read().imageFitted());
        assertFalse(bird.preferences.contains("projection_profile"));
        config.setVehicleProfileMode("7x"); assertFalse(bird.read().imageFitted());
        assertEquals("-1", bird.projectionMode());
        config.setVehicleProfileMode("auto"); assertFalse(bird.imageFitted());
        // Real manual edits while Auto resolves to Manual must pin that intent before a later model change.
        bird.setManualParameter("length", 550); bird.setManualParameter(BirdEyeConfig.key(0, "pitch"), 47);
        config.setVehicleProfileMode("009"); assertEquals("0", bird.projectionMode()); assertFalse(bird.imageFitted());
        bird.setProjectionMode("-1"); assertTrue(bird.imageFitted()); assertEquals(550, bird.get("length", 0));
        bird.setProjectionMode("0"); assertEquals(5.5, bird.read().length, 0);
        assertEquals(47, bird.read().cameras[0].pitch, 0);
    }
    @Test public void legacySliderEditsAreManualButQualitySourcesAndFittedMarginAreNot() {
        BirdEyeConfig bird = new BirdEyeConfig(target); config.setVehicleProfileMode("009");
        bird.set("quality", 2); bird.assignLane(0, 3); bird.set(BirdEyeConfig.FIT_MARGIN, 275);
        assertEquals("-1", bird.projectionMode()); assertTrue(bird.imageFitted());
        assertEquals(2, bird.quality()); assertEquals(3, bird.read().cameras[0].lane);
        bird.set("length", 520); // Old versions saved slider values without a projection flag.
        assertEquals("0", bird.projectionMode()); assertFalse(bird.imageFitted());
        bird.setProjectionMode("-1"); assertTrue(bird.imageFitted());
        bird.setManualMirror(0, true); assertEquals("0", bird.projectionMode());
        bird.reset(); assertEquals("-1", bird.projectionMode()); assertTrue(bird.imageFitted());
        assertEquals(BirdEyeQuality.BALANCED, bird.quality());
    }
    @Test public void saved009FitOn7xRemainsVisibleAsInactiveInsteadOfBeingRelabelledAutomatic() {
        BirdEyeConfig bird = new BirdEyeConfig(target); bird.setImageFitted(true);
        config.setVehicleProfileMode("7x");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                SettingsShellFragment shell = new SettingsShellFragment();
                activity.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, shell).commitNow();
                shell.getChildFragmentManager().executePendingTransactions(); shell.showSection("screen_bird_eye");
                shell.getChildFragmentManager().executePendingTransactions();
                SettingsPreferenceFragment page = (SettingsPreferenceFragment) shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);
                ListPreference projection = page.findPreference("pref_bird_profile");
                assertEquals("1", projection.getValue());
                assertEquals(activity.getString(R.string.bird_eye_profile_009_inactive), projection.getSummary());
                assertFalse(bird.imageFitted()); assertEquals(1, bird.preferences.getInt("projection_profile", 0));
            });
        }
        config.setVehicleProfileMode("009"); assertTrue(bird.imageFitted());
    }
    @Test public void systemHasOneFunctionalSelectorWithEvidenceAndNoOldMappingToggle() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                SettingsShellFragment shell = new SettingsShellFragment();
                activity.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, shell).commitNow();
                shell.getChildFragmentManager().executePendingTransactions();
                shell.showSection("screen_system"); shell.getChildFragmentManager().executePendingTransactions();
                SettingsPreferenceFragment page = (SettingsPreferenceFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);
                ListPreference model = page.findPreference("pref_vehicle_profile");
                Preference info = page.findPreference("pref_vehicle_profile_info");
                assertNotNull(model); assertNotNull(info);
                assertEquals(3, model.getEntryValues().length);
                assertFalse(model.isPersistent());
                model.callChangeListener("009");
                assertEquals("009", model.getValue()); assertTrue(config.is009CameraMappingEnabled());
                assertTrue(info.getSummary().toString().contains("3"));
                model.callChangeListener("7x");
                assertEquals("7x", config.getVehicleProfileMode()); assertFalse(config.is009CameraMappingEnabled());
                shell.showSection("screen_recording"); shell.getChildFragmentManager().executePendingTransactions();
                page = (SettingsPreferenceFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);
                assertNull(page.findPreference("pref_009_camera_mapping"));
                shell.showSection("screen_interface"); shell.getChildFragmentManager().executePendingTransactions();
                page = (SettingsPreferenceFragment)shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);
                assertNull(page.findPreference("pref_vehicle_name"));
            });
        }
        assertEquals("7x", new AppConfig(target).getVehicleProfileMode());
    }
}
