package com.kooo.evcam.settings;

import android.Manifest;
import android.content.Context;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicReference;

/** One bounded review batch: current native settings and playback control fixtures, no vehicle camera. */
@RunWith(AndroidJUnit4.class)
public class MenuReviewAndroidTest {
    @Test public void captureSettingsAndPlaybackForMenuReview() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (String permission : new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}) InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .grantRuntimePermission(target.getPackageName(), permission);
        target.getSharedPreferences("app_config", Context.MODE_PRIVATE).edit().putBoolean("first_launch", false)
                .putBoolean("language_chosen", true).putBoolean("rail_side_chosen", true).commit();
        com.kooo.evcam.AppConfig config = new com.kooo.evcam.AppConfig(target);
        boolean oldSideEnabled = config.isSidePopupEnabled();
        int oldMirrorMode = config.getSidePopupMirrorMode();
        boolean confirmation = InstrumentationRegistry.getArguments().getBoolean("menuReviewConfirmation", false)
                || "true".equals(InstrumentationRegistry.getArguments().getString("menuReviewConfirmation"));
        boolean developer = DeveloperMode.isUnlocked();
        DeveloperMode.setEnabled(target, true);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<SettingsShellFragment> ref = new AtomicReference<>();
            scenario.onActivity(activity -> {
                SettingsShellFragment shell = new SettingsShellFragment(); ref.set(shell);
                activity.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, shell).commitNow();
                shell.getChildFragmentManager().executePendingTransactions();
            });
            String[] sections = confirmation ? new String[]{"blind_spot_camera"} : new String[]{"recording", "storage", "rearview", "blind_spot_camera", "bird_eye",
                    "floating", "interface", "system", "developer", "update"};
            for (String section : sections) {
                scenario.onActivity(activity -> {
                    if ("blind_spot_camera".equals(section)) {
                        config.setSidePopupEnabled(true);
                        config.setSidePopupMirrorMode(com.kooo.evcam.zeekr.SidePopupMirrorMode.HIDE);
                    }
                    ref.get().showSection("screen_"+section);
                    ref.get().getChildFragmentManager().executePendingTransactions();
                });
                capture("menu-"+section+".png");
                if (!confirmation && ("recording".equals(section) || "blind_spot_camera".equals(section))) {
                    scenario.onActivity(activity -> {
                        SettingsPreferenceFragment detail = (SettingsPreferenceFragment) ref.get().getChildFragmentManager().findFragmentById(R.id.settings_detail);
                        detail.scrollToPreference("recording".equals(section) ? "sub_recording_stamps" : "sub_side_image");
                    });
                    capture("menu-"+section+"-more.png");
                    String[] children = "recording".equals(section) ? new String[]{"sub_recording_stamps"}
                            : new String[]{"sub_side_image", "sub_side_window"};
                    for (String child : children) {
                        scenario.onActivity(activity -> {
                            SettingsPreferenceFragment detail = (SettingsPreferenceFragment) ref.get().getChildFragmentManager().findFragmentById(R.id.settings_detail);
                            detail.findPreference(child).performClick();
                            ref.get().getChildFragmentManager().executePendingTransactions();
                        });
                        capture("menu-"+child+".png");
                        scenario.onActivity(activity -> {
                            ref.get().getChildFragmentManager().popBackStackImmediate();
                        });
                    }
                }
                if (confirmation && "blind_spot_camera".equals(section)) {
                    scenario.onActivity(activity -> {
                        SettingsPreferenceFragment detail = (SettingsPreferenceFragment) ref.get().getChildFragmentManager().findFragmentById(R.id.settings_detail);
                        androidx.preference.ListPreference mode = detail.findPreference("pref_side_popup_mirror_mode");
                        org.junit.Assert.assertEquals("2", mode.getValue());
                        org.junit.Assert.assertEquals(3, mode.getEntries().length);
                        detail.onDisplayPreferenceDialog(mode);
                    });
                    capture("menu-mirror-options.png");
                    androidx.test.espresso.Espresso.pressBack();
                }
            }
            scenario.onActivity(activity -> {
                ref.get().openDetail(SettingsPreferenceFragment.forSection("sub_side_hud"), target.getString(R.string.set_sub_hud_title));
                ref.get().getChildFragmentManager().executePendingTransactions();
            });
            capture("menu-hud.png");
            // Playback layout fixture with all eight actions visible, including the normally conditional Lock.
            scenario.onActivity(activity -> {
                activity.setContentView(R.layout.activity_timeline_player);
                activity.findViewById(R.id.timeline_lock).setVisibility(View.VISIBLE);
            });
            capture("menu-playback.png");
            if (confirmation) {
                scenario.onActivity(activity -> activity.getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES));
                scenario.onActivity(activity -> {
                    activity.setContentView(R.layout.activity_timeline_player);
                    activity.findViewById(R.id.timeline_lock).setVisibility(View.VISIBLE);
                });
                capture("menu-playback-dark.png");
            }
        } finally {
            DeveloperMode.setEnabled(target, developer);
            config.setSidePopupEnabled(oldSideEnabled);
            config.setSidePopupMirrorMode(oldMirrorMode);
        }
    }
    private void capture(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(150, 2000);
        android.os.SystemClock.sleep(350);
        try (android.os.ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("screencap -p /sdcard/Download/"+name);
             java.io.InputStream in = new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)) {
            while (in.read() != -1) {}
        }
    }
}
