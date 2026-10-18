package com.kooo.evcam.settings;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import androidx.preference.SwitchPreferenceCompat;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.overlay.OverlayCoordinator;
import com.kooo.evcam.zeekr.LaneCycle;
import com.kooo.evcam.zeekr.RearViewMirrorService;
import com.kooo.evcam.zeekr.RearViewMirrorView;
import java.util.Map;
import java.util.function.BooleanSupplier;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real overlay controls and Settings; the emulator has no vehicle camera feeds. */
@RunWith(AndroidJUnit4.class)
public class SuperMirrorControlsAndroidTest {
    private Context target;
    private SharedPreferences prefs;
    private Map<String, ?> before;
    private AppConfig config;
    private RearViewMirrorView window;
    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }

    @Before public void setup() throws Exception {
        target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = target.getSharedPreferences("app_config", Context.MODE_PRIVATE); before = prefs.getAll();
        for (String permission : new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}) InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .grantRuntimePermission(target.getPackageName(), permission);
        try (ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("appops set " + target.getPackageName() + " SYSTEM_ALERT_WINDOW allow");
             java.io.InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd)) { while (in.read() != -1) {} }
        prefs.edit().clear().putBoolean("first_launch", false).putBoolean("language_chosen", true)
                .putBoolean("rail_side_chosen", true).commit();
        config = new AppConfig(target); config.setRearViewGuideSeen();
        config.setRearViewSize(720, 360, 2560, 1600); config.setRearViewPosition(300, 250);
        config.setRearViewLane(LaneCycle.REAR);
    }

    @After public void restore() {
        main(() -> { RearViewMirrorService.stop(target); if (window != null) window.hide(); });
        await(() -> !RearViewMirrorService.isRunning());
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

    private void show(int nightMode) {
        main(() -> {
            Configuration c = new Configuration(target.getResources().getConfiguration());
            c.uiMode = (c.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | nightMode;
            window = new RearViewMirrorView(new ContextThemeWrapper(target.createConfigurationContext(c),
                    R.style.Theme_Cam), config); window.show();
        });
        settled();
    }
    private WindowManager.LayoutParams geometry() { return (WindowManager.LayoutParams) window.getLayoutParams(); }
    private View control(View view, int string) {
        if (window.getContext().getString(string).contentEquals(view.getContentDescription() == null
                ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            View result = control(((ViewGroup) view).getChildAt(i), string); if (result != null) return result;
        }
        return null;
    }
    private void event(long start, int action, float x, float y) {
        MotionEvent e = MotionEvent.obtain(start, SystemClock.uptimeMillis(), action, x, y, 0);
        window.dispatchTouchEvent(e); e.recycle();
    }
    private void tap(int description) {
        View button = control(window, description); assertNotNull(button); assertTrue(button.isShown());
        Rect rect = new Rect(0, 0, button.getWidth(), button.getHeight());
        window.offsetDescendantRectToMyCoords(button, rect);
        long time = SystemClock.uptimeMillis();
        event(time, MotionEvent.ACTION_DOWN, rect.exactCenterX(), rect.exactCenterY());
        event(time, MotionEvent.ACTION_UP, rect.exactCenterX(), rect.exactCenterY());
    }
    private void await(BooleanSupplier condition) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (!condition.getAsBoolean() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(20);
        assertTrue("Window/service must settle", condition.getAsBoolean());
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
    private void settled() {
        boolean[] ready = {false};
        await(() -> { main(() -> ready[0] = window.getWidth() == geometry().width
                && window.getHeight() == geometry().height); return ready[0]; });
    }

    @Test public void cornerTouchesExpandRestoreWithoutChangingSavedGeometryOrCamera() throws Exception {
        for (int mode : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES}) {
            show(mode);
            Map<String, ?> saved = prefs.getAll();
            main(() -> {
                assertTrue(control(window, R.string.mirror_control_close).isShown());
                assertTrue(control(window, R.string.mirror_control_expand).isShown());
                tap(R.string.mirror_control_expand);
            }); settled(); // Native touch UP posts the button click to the next UI turn.
            main(() -> {
                assertEquals(2.0, geometry().width / (double) geometry().height, .01);
                assertTrue(geometry().width > 720);
                assertTrue(geometry().x >= 0 && geometry().y >= 0);
                assertTrue(geometry().x + geometry().width <= window.getResources().getDisplayMetrics().widthPixels);
                assertTrue(geometry().y + geometry().height <= window.getResources().getDisplayMetrics().heightPixels);
                // Routine "no replacement" cleanup must not collapse a normal expanded mirror.
                window.clearSideView();
                assertTrue(geometry().width > 720);
            }); settled();
            capture(mode == Configuration.UI_MODE_NIGHT_NO ? "mirror-day-expanded" : "mirror-night-expanded");
            main(() -> tap(R.string.side_control_restore)); settled();
            main(() -> {
                assertEquals(300, geometry().x); assertEquals(250, geometry().y);
                assertEquals(720, geometry().width); assertEquals(360, geometry().height);
                assertEquals(saved, prefs.getAll());
            });
            capture(mode == Configuration.UI_MODE_NIGHT_NO ? "mirror-day" : "mirror-night");
            main(() -> window.hide());
        }
    }

    @Test public void replacementAndSettingsResizeRestoreNormalGeometry() {
        show(Configuration.UI_MODE_NIGHT_NO);
        main(() -> tap(R.string.mirror_control_expand)); settled();
        main(() -> {
            window.showSideView(LaneCycle.LEFT);
            assertEquals(720, geometry().width); assertEquals(360, geometry().height);
            assertNotNull(control(window, R.string.side_control_close));
            assertNull(control(window, R.string.mirror_control_close));
        }); settled();
        main(() -> tap(R.string.side_control_expand)); settled();
        main(() -> {
            window.clearSideView();
            assertEquals(720, geometry().width); assertEquals(360, geometry().height);
            assertNotNull(control(window, R.string.mirror_control_close));
        }); settled();
        main(() -> tap(R.string.mirror_control_expand)); settled();
        main(() -> {
            config.setRearViewSize(800, 400, 2560, 1600); window.applySizeFromConfig();
            assertEquals(800, geometry().width); assertEquals(400, geometry().height);
            assertNotNull(control(window, R.string.mirror_control_expand));
        });
    }

    @Test public void bodyDragPinchAndDockTabStillWorkBelowTheControls() {
        show(Configuration.UI_MODE_NIGHT_NO);
        main(() -> {
            long t = SystemClock.uptimeMillis();
            event(t, MotionEvent.ACTION_DOWN, 40, 240);
            event(t, MotionEvent.ACTION_MOVE, 100, 270);
            event(t, MotionEvent.ACTION_UP, 100, 270);
            assertEquals(360, geometry().x); assertEquals(280, geometry().y);
            assertEquals(360, config.getRearViewX()); assertEquals(280, config.getRearViewY());
            geometry().x = -geometry().width + 72; window.applySizeFromConfig();
            assertTrue(window.isDocked());
            assertFalse(control(window, R.string.mirror_control_close).isShown());
            long now = SystemClock.uptimeMillis();
            event(now, MotionEvent.ACTION_DOWN, geometry().width - 36, 240);
            event(now, MotionEvent.ACTION_UP, geometry().width - 36, 240);
        });
        await(() -> !window.isDocked()); settled();
        main(() -> {
            assertTrue(control(window, R.string.mirror_control_close).isShown());
            int width = geometry().width;
            long t = SystemClock.uptimeMillis();
            event(t, MotionEvent.ACTION_DOWN, 80, 240);
            pointers(t, MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), 80, 420);
            pointers(t, MotionEvent.ACTION_MOVE, 20, 480);
            pointers(t, MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), 20, 480);
            event(t, MotionEvent.ACTION_UP, 20, 240);
            assertTrue("Pinch still enlarges the mirror", geometry().width > width);
            assertEquals(2.0, geometry().width / (double) geometry().height, .01);
            assertEquals(geometry().width, config.getRearViewWidth(2560));
            assertEquals(geometry().height, config.getRearViewHeight(1600));
        });
    }

    private void pointers(long t, int action, float left, float right) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[2];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[2];
        for (int i = 0; i < 2; i++) {
            properties[i] = new MotionEvent.PointerProperties(); properties[i].id = i;
            properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coords[i] = new MotionEvent.PointerCoords(); coords[i].x = i == 0 ? left : right;
            coords[i].y = 240; coords[i].pressure = 1; coords[i].size = 1;
        }
        MotionEvent e = MotionEvent.obtain(t, SystemClock.uptimeMillis(), action, 2, properties, coords,
                0, 0, 1, 1, 0, 0, android.view.InputDevice.SOURCE_TOUCHSCREEN, 0);
        window.dispatchTouchEvent(e); e.recycle();
    }

    @Test public void closingRealMirrorStopsServiceAndUpdatesOpenSettingsSwitch() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                SettingsShellFragment shell = new SettingsShellFragment();
                activity.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, shell).commitNow();
                shell.getChildFragmentManager().executePendingTransactions();
                shell.showSection("screen_rearview"); shell.getChildFragmentManager().executePendingTransactions();
                assertTrue(OverlayCoordinator.setRearViewEnabled(target, true));
            });
            await(() -> RearViewMirrorService.isRunning());
            main(() -> {
                try {
                    java.lang.reflect.Field instance = RearViewMirrorService.class.getDeclaredField("instance");
                    instance.setAccessible(true);
                    java.lang.reflect.Field mirror = RearViewMirrorService.class.getDeclaredField("mirrorView");
                    mirror.setAccessible(true); window = (RearViewMirrorView) mirror.get(instance.get(null));
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
                assertNotNull(window);
            }); settled();
            main(() -> tap(R.string.mirror_control_close));
            await(() -> !RearViewMirrorService.isRunning());
            assertFalse(config.isRearViewEnabled()); assertFalse(window.isShowing());
            scenario.onActivity(activity -> {
                SettingsShellFragment shell = (SettingsShellFragment) activity.getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
                SettingsPreferenceFragment page = (SettingsPreferenceFragment) shell.getChildFragmentManager()
                        .findFragmentById(R.id.settings_detail);
                SwitchPreferenceCompat toggle = page.findPreference("pref_rearview");
                assertFalse(toggle.isChecked()); assertFalse(page.findPreference("pref_rearview_button_mode").isEnabled());
                toggle.performClick();
            });
            await(RearViewMirrorService::isRunning); assertTrue(config.isRearViewEnabled());
        }
    }

    private void capture(String name) throws Exception {
        if (!"true".equals(InstrumentationRegistry.getArguments().getString("captureMirrorControls"))) return;
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // WindowManager's resize/move animation continues after the View layout is idle.
        SystemClock.sleep(400);
        Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(image);
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(new java.io.File(
                target.getExternalFilesDir(null), name + ".png"))) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out));
        } finally { image.recycle(); }
    }
}
