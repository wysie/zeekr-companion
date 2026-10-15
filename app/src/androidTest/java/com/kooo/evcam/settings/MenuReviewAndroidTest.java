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
            for (String section : new String[]{"recording", "storage", "rearview", "blind_spot_camera", "bird_eye",
                    "floating", "interface", "system", "developer", "update"}) {
                scenario.onActivity(activity -> {
                    ref.get().showSection("screen_"+section);
                    ref.get().getChildFragmentManager().executePendingTransactions();
                });
                capture("menu-"+section+".png");
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
        } finally { DeveloperMode.setEnabled(target, developer); }
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
