package com.kooo.evcam.settings;

import android.graphics.SurfaceTexture;
import android.os.SystemClock;
import android.view.TextureView;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.tabs.TabLayout;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.camera.CameraManagerHolder;
import com.kooo.evcam.camera.CameraNeeds;
import com.kooo.evcam.camera.MultiCameraManager;
import com.kooo.evcam.ui.MainPageControlsAndroidTest;
import org.junit.Test;
import java.util.Map;
import java.util.HashMap;
import static org.junit.Assert.*;

/** Real settings navigation and retained consumers; inherits the isolated emulator fixture. */
public class FactoryStyleSettingsAndroidTest extends MainPageControlsAndroidTest {
    private SettingsShellFragment shell(MainActivity activity) {
        return (SettingsShellFragment) activity.getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }
    private SettingsPreferenceFragment page(MainActivity activity) {
        return (SettingsPreferenceFragment) shell(activity).getChildFragmentManager().findFragmentById(R.id.settings_detail);
    }
    private void select(ActivityScenario<MainActivity> scenario, int tab) {
        scenario.onActivity(a -> {
            ((TabLayout) a.findViewById(R.id.side_settings_tabs)).getTabAt(tab).select();
            shell(a).getChildFragmentManager().executePendingTransactions();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    private void open(ActivityScenario<MainActivity> scenario) {
        scenario.onActivity(a -> {
            a.findViewById(R.id.btn_settings).performClick();
            a.getSupportFragmentManager().executePendingTransactions();
            shell(a).getChildFragmentManager().executePendingTransactions();
            shell(a).showSection("screen_blind_spot_camera");
            shell(a).getChildFragmentManager().executePendingTransactions();
        });
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    @Test public void tabsRetainValuesConsumersAndRestoreTheirSelectedPage() {
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Map<String, ?> profiles = context.getSharedPreferences("profiles", 0).getAll();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync(); SystemClock.sleep(300);
            final TextureView[] texture = {null}; final SurfaceTexture[] surface = {null};
            final MultiCameraManager[] manager = {null}; final String[] holders = {null};
            scenario.onActivity(a -> {
                texture[0] = a.findViewById(R.id.texture_front); surface[0] = texture[0].getSurfaceTexture();
                assertNotNull(surface[0]); manager[0] = CameraManagerHolder.getInstance().getCameraManager();
                holders[0] = CameraNeeds.current().describe();
            });
            open(scenario);
            scenario.onActivity(a -> {
                assertFalse(page(a).findPreference("sub_side_image").isVisible());
                assertNotNull(page(a).findPreference("pref_side_popup"));
            });
            Map<String, ?> before = new HashMap<>(context.getSharedPreferences("app_config", 0).getAll());
            for (int tab : new int[]{1, 2, 3, 0, 1}) select(scenario, tab);
            scenario.onActivity(a -> {
                assertEquals("screen_blind_spot_camera", shell(a).currentSection());
                assertEquals(0, shell(a).getChildFragmentManager().getBackStackEntryCount());
                assertEquals("sub_side_image", page(a).getArguments().getString("section"));
                assertSame(texture[0], a.settingsPreviewTexture());
                assertSame(surface[0], texture[0].getSurfaceTexture());
                assertSame(manager[0], CameraManagerHolder.getInstance().getCameraManager());
                assertEquals(holders[0], CameraNeeds.current().describe());
                assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
                        a.findViewById(R.id.main).getImportantForAccessibility());
                a.findViewById(R.id.side_preview_expand).performClick();
                assertEquals(View.GONE, a.findViewById(R.id.side_settings_preferences).getVisibility());
                a.findViewById(R.id.side_preview_close).performClick();
                assertEquals(View.VISIBLE, a.findViewById(R.id.side_settings_preferences).getVisibility());
            });
            assertEquals(before, context.getSharedPreferences("app_config", 0).getAll());
            assertEquals(profiles, context.getSharedPreferences("profiles", 0).getAll());
            scenario.recreate();
            scenario.onActivity(a -> {
                assertEquals(1, ((TabLayout) a.findViewById(R.id.side_settings_tabs)).getSelectedTabPosition());
                assertNotNull(page(a).findPreference("pref_side_popup_yaw"));
                assertTrue("Tuning remains available with the automatic popup off", page(a).findPreference("pref_side_popup_yaw").isEnabled());
                assertEquals(View.VISIBLE, a.findViewById(R.id.main).getVisibility());
                a.goToRecordingInterface();
                assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO, a.findViewById(R.id.main).getImportantForAccessibility());
            });
        }
    }
    @Test public void captureFactorySettingsBothThemesAndResponsiveLayout() throws Exception {
        for (int mode : new int[]{AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES}) {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> AppCompatDelegate.setDefaultNightMode(mode));
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                open(scenario); select(scenario, 1);
                capture("factory-picture-" + mode);
                scenario.onActivity(a -> {
                    LinearLayout body = a.findViewById(R.id.side_settings_body);
                    assertEquals(LinearLayout.HORIZONTAL, body.getOrientation());
                    View controls = a.findViewById(R.id.side_settings_preferences), preview = a.findViewById(R.id.side_settings_preview);
                    assertTrue(controls.getWidth() > 0); assertTrue(preview.getWidth() > controls.getWidth());
                });
                scenario.onActivity(a -> {
                    View pane = page(a).requireView();
                    android.view.ViewGroup.LayoutParams params = pane.getLayoutParams();
                    params.width = Math.round(640 * a.getResources().getDisplayMetrics().density);
                    pane.setLayoutParams(params);
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a -> {
                    assertEquals(LinearLayout.VERTICAL, ((LinearLayout) a.findViewById(R.id.side_settings_body)).getOrientation());
                    android.widget.TextView status = a.findViewById(R.id.side_preview_status);
                    assertEquals(status.length(), status.getLayout().getLineEnd(status.getLineCount() - 1));
                    assertTrue(status.getLayout().getHeight() <= status.getHeight() - status.getCompoundPaddingTop() - status.getCompoundPaddingBottom());
                });
                capture("factory-picture-narrow-" + mode);
                select(scenario, 0); capture("factory-behavior-" + mode);
                select(scenario, 3); capture("factory-hud-" + mode);
                scenario.onActivity(a -> a.goToRecordingInterface()); capture("factory-main-" + mode);
            }
        }
    }
    private void capture(String name) throws Exception {
        if (!"true".equals(InstrumentationRegistry.getArguments().getString("captureFactoryStyle"))) return;
        InstrumentationRegistry.getInstrumentation().waitForIdleSync(); SystemClock.sleep(6500);
        android.graphics.Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        android.content.Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(context.getExternalFilesDir(null), name + ".png"))) {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out));
        } finally { bitmap.recycle(); }
    }
}
