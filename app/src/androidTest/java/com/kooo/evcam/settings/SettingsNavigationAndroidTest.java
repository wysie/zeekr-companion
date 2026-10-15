package com.kooo.evcam.settings;

import android.Manifest;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.view.View;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.zeekr.BirdEyePopupService;
import com.kooo.evcam.zeekr.BirdEyeWindowConfig;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

/** Native preference attachment, real nested navigation and live overlay-session synchronisation. */
@RunWith(AndroidJUnit4.class)
public class SettingsNavigationAndroidTest {
    private Context target;
    private AppConfig config;
    private boolean oldSide, oldLabels, oldDeveloper, oldDestination;
    @Before public void setup() throws Exception {
        target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (String permission : new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}) InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .grantRuntimePermission(target.getPackageName(), permission);
        shell("appops set " + target.getPackageName() + " SYSTEM_ALERT_WINDOW allow");
        target.getSharedPreferences("app_config", Context.MODE_PRIVATE).edit().putBoolean("first_launch", false)
                .putBoolean("language_chosen", true).putBoolean("rail_side_chosen", true).commit();
        config = new AppConfig(target);
        oldSide = config.isSidePopupEnabled(); oldLabels = config.isRearViewCameraLabelEnabled();
        oldDeveloper = DeveloperMode.isUnlocked(); oldDestination = new BirdEyeWindowConfig(target).popup();
        DeveloperMode.setEnabled(target, true);
        config.setSidePopupEnabled(true); config.setRearViewCameraLabelEnabled(false);
    }
    @After public void restore() {
        BirdEyePopupService.stop(target);
        config.setSidePopupEnabled(oldSide); config.setRearViewCameraLabelEnabled(oldLabels);
        new BirdEyeWindowConfig(target).setPopup(oldDestination);
        DeveloperMode.setEnabled(target, oldDeveloper);
    }
    private void shell(String command) throws Exception {
        try (ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
             java.io.InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd)) { while (in.read() != -1) {} }
    }
    private SettingsShellFragment install(MainActivity activity) {
        SettingsShellFragment shell = new SettingsShellFragment();
        activity.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
        activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, shell).commitNow();
        shell.getChildFragmentManager().executePendingTransactions();
        return shell;
    }
    private SettingsPreferenceFragment page(SettingsShellFragment shell) {
        return (SettingsPreferenceFragment) shell.getChildFragmentManager().findFragmentById(R.id.settings_detail);
    }
    private void select(SettingsShellFragment shell, String key) {
        shell.showSection(key); shell.getChildFragmentManager().executePendingTransactions();
    }
    private void open(SettingsShellFragment shell, String key) {
        Preference destination = page(shell).findPreference(key);
        assertNotNull(destination); assertTrue(destination.isEnabled());
        assertEquals(R.layout.pref_row_nav, destination.getLayoutResource());
        destination.performClick(); shell.getChildFragmentManager().executePendingTransactions();
        assertEquals(key, page(shell).getArguments().getString("section"));
    }
    private void back(SettingsShellFragment shell) {
        shell.requireView().findViewById(R.id.settings_nav).performClick();
        shell.getChildFragmentManager().executePendingTransactions();
    }
    @Test public void subpagesAttachNavigateAndShareOneCameraLabelSetting() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                SettingsShellFragment shell = install(activity);
                assertNotNull(page(shell).findPreference("pref_screen_off_keep_recording"));
                open(shell, "sub_recording_stamps");
                assertNotNull(page(shell).findPreference("pref_license_plate"));
                Preference info = page(shell).findPreference("pref_info_bar_items");
                info.performClick(); shell.getChildFragmentManager().executePendingTransactions();
                assertTrue(shell.getChildFragmentManager().findFragmentById(R.id.settings_detail) instanceof VehicleInfoFragment);
                back(shell); back(shell);
                assertEquals(0, shell.getChildFragmentManager().getBackStackEntryCount());
                String[][] children = {{"sub_side_image", "pref_side_popup_fov"},
                        {"sub_side_window", "pref_side_popup_size"}, {"sub_side_hud", "pref_hud_side_mirror"}};
                select(shell, "screen_blind_spot_camera");
                for (String[] child : children) {
                    open(shell, child[0]);
                    assertNotNull(page(shell).findPreference(child[1]));
                    back(shell);
                    assertEquals("screen_blind_spot_camera", shell.currentSection());
                    assertEquals(0, shell.getChildFragmentManager().getBackStackEntryCount());
                }
                open(shell, "sub_side_image");
                SwitchPreferenceCompat label = page(shell).findPreference("pref_side_camera_label");
                assertFalse(label.isChecked()); label.performClick();
                assertTrue(config.isRearViewCameraLabelEnabled());
                select(shell, "screen_rearview");
                assertEquals(0, shell.getChildFragmentManager().getBackStackEntryCount());
                SwitchPreferenceCompat mirrorLabel = page(shell).findPreference("pref_rearview_camera_label");
                assertTrue(mirrorLabel.isChecked()); mirrorLabel.performClick();
                assertFalse(config.isRearViewCameraLabelEnabled());
                select(shell, "screen_blind_spot_camera"); open(shell, "sub_side_image");
                assertFalse(((SwitchPreferenceCompat) page(shell).findPreference("pref_side_camera_label")).isChecked());
                config.setSidePopupEnabled(false);
                select(shell, "screen_blind_spot_camera");
                assertFalse(page(shell).findPreference("sub_side_window").isEnabled());
            });
        }
    }
    @Test public void developerToggleHidesCategoryAndRecordingControlsStayInRecording() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                SettingsShellFragment shell = install(activity); select(shell, "screen_system");
                assertNull(page(shell).findPreference("pref_screen_off_keep_recording"));
                page(shell).findPreference("pref_developer_mode").performClick();
                SettingsHeadersFragment headers = (SettingsHeadersFragment) shell.getChildFragmentManager().findFragmentById(R.id.settings_headers);
                assertNull(headers.findPreference("screen_developer"));
                assertEquals("screen_system", shell.currentSection());
                page(shell).findPreference("pref_developer_mode").performClick();
                assertNotNull(headers.findPreference("screen_developer"));
                select(shell, "screen_developer");
                assertNotNull(page(shell).findPreference("pref_recording_mode"));
                assertNotNull(page(shell).findPreference("pref_camera_display_tools"));
                select(shell, "screen_recording");
                assertNotNull(page(shell).findPreference("pref_screen_off_keep_recording"));
            });
        }
    }
    @Test public void popupToggleTracksServiceCloseAndNeverPersistsVisibility() throws Exception {
        new BirdEyeWindowConfig(target).setPopup(true);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                SettingsShellFragment shell = install(activity); select(shell, "screen_bird_eye");
                SwitchPreferenceCompat visible = page(shell).findPreference("pref_bird_visible");
                assertFalse(visible.isPersistent()); assertFalse(visible.isChecked());
                visible.performClick();
            });
            await(BirdEyePopupService::isRunning);
            scenario.recreate();
            scenario.onActivity(activity -> {
                SettingsShellFragment shell = (SettingsShellFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                assertTrue(((SwitchPreferenceCompat) page(shell).findPreference("pref_bird_visible")).isChecked());
                // The actual X callback is Service.stopSelf(); suspension keeps the session enabled.
                try {
                    java.lang.reflect.Field instance = BirdEyePopupService.class.getDeclaredField("instance");
                    instance.setAccessible(true); ((BirdEyePopupService) instance.get(null)).stopSelf();
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
            await(() -> !BirdEyePopupService.isRunning());
            scenario.onActivity(activity -> {
                SettingsShellFragment shell = (SettingsShellFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                assertFalse(((SwitchPreferenceCompat) page(shell).findPreference("pref_bird_visible")).isChecked());
                assertTrue(new BirdEyeWindowConfig(target).popup());
                ((SwitchPreferenceCompat) page(shell).findPreference("pref_bird_visible")).performClick();
            });
            await(BirdEyePopupService::isRunning);
            scenario.onActivity(activity -> {
                SettingsShellFragment shell = (SettingsShellFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                Preference destination = page(shell).findPreference("pref_bird_display");
                destination.getOnPreferenceChangeListener().onPreferenceChange(destination, "main");
            });
            await(() -> !BirdEyePopupService.isRunning());
            assertFalse(new BirdEyeWindowConfig(target).popup());
            scenario.recreate();
            assertFalse(BirdEyePopupService.isRunning());
            assertFalse(target.getSharedPreferences("app_config", Context.MODE_PRIVATE).contains("pref_bird_visible"));
            scenario.onActivity(activity -> {
                SettingsShellFragment shell = install(activity); select(shell, "screen_bird_eye");
                assertFalse(((SwitchPreferenceCompat) page(shell).findPreference("pref_bird_visible")).isChecked());
            });
        }
    }
    private void await(BooleanSupplier condition) {
        long until = android.os.SystemClock.uptimeMillis() + 4000;
        while (!condition.getAsBoolean() && android.os.SystemClock.uptimeMillis() < until) android.os.SystemClock.sleep(20);
        assertTrue("Service lifecycle must settle", condition.getAsBoolean());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
