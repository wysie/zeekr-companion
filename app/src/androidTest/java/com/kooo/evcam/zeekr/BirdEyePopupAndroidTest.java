package com.kooo.evcam.zeekr;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Disposable emulator: native overlay geometry and controls, without opening any camera. */
@RunWith(AndroidJUnit4.class)
public class BirdEyePopupAndroidTest {
    private void main(Runnable work) { InstrumentationRegistry.getInstrumentation().runOnMainSync(work); }
    private void shell(String command) throws Exception {
        try (ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
             java.io.InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd)) { while (in.read() != -1) {} }
    }
    private View control(View view, String label) {
        if (label.contentEquals(view.getContentDescription() == null ? "" : view.getContentDescription())) return view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup)view).getChildCount(); i++) {
            View found = control(((ViewGroup)view).getChildAt(i), label); if (found != null) return found;
        }
        return null;
    }
    private WindowManager.LayoutParams params(View view) { return (WindowManager.LayoutParams)view.getLayoutParams(); }

    @Test public void expandedViewUsesNativeAreaEvenWhenAppResourceHeightIsShorter() throws Exception {
        exerciseViewport(false);
    }

    @Test public void insetChangesRefitExpansionWithoutSavingItOrLeavingExpandedMode() throws Exception {
        exerciseViewport(true);
    }

    /** A shortened resource height models the stale app metrics observed over immersive projection apps. */
    private void exerciseViewport(boolean changeInsets) throws Exception {
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT >= 30);
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        shell("appops set " + target.getPackageName() + " SYSTEM_ALERT_WINDOW allow");
        WindowManager real = (WindowManager) target.getSystemService(Context.WINDOW_SERVICE);
        android.view.WindowMetrics nativeMetrics = real.getCurrentWindowMetrics();
        AtomicReference<android.view.WindowMetrics> metrics = new AtomicReference<>(nativeMetrics);
        if (changeInsets) {
            android.graphics.Insets nav = nativeMetrics.getWindowInsets().getInsets(android.view.WindowInsets.Type.navigationBars());
            metrics.set(new android.view.WindowMetrics(nativeMetrics.getBounds(),
                    new android.view.WindowInsets.Builder(nativeMetrics.getWindowInsets())
                            .setInsets(android.view.WindowInsets.Type.navigationBars(), android.graphics.Insets.of(
                                    nav.left, nav.top, nav.right, nav.bottom + 180))
                            .setVisible(android.view.WindowInsets.Type.navigationBars(), true).build()));
        }
        WindowManager manager = (WindowManager) java.lang.reflect.Proxy.newProxyInstance(
                WindowManager.class.getClassLoader(), new Class<?>[]{WindowManager.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getCurrentWindowMetrics")) return metrics.get();
                    try { return method.invoke(real, args); }
                    catch (java.lang.reflect.InvocationTargetException e) { throw e.getCause(); }
                });
        android.util.DisplayMetrics shortMetrics = new android.util.DisplayMetrics();
        shortMetrics.setTo(target.getResources().getDisplayMetrics()); shortMetrics.heightPixels -= 240;
        android.content.res.Resources resources = new android.content.res.Resources(target.getAssets(), shortMetrics,
                new android.content.res.Configuration(target.getResources().getConfiguration()));
        Context staleAppContext = new android.content.ContextWrapper(target) {
            @Override public android.content.res.Resources getResources() { return resources; }
            @Override public Object getSystemService(String name) {
                return Context.WINDOW_SERVICE.equals(name) ? manager : super.getSystemService(name);
            }
        };
        Context context = new ContextThemeWrapper(staleAppContext, R.style.Theme_Cam);
        android.content.SharedPreferences preferences = target.getSharedPreferences("bird_eye_window", Context.MODE_PRIVATE);
        java.util.Map<String, ?> original = preferences.getAll();
        BirdEyeWindowConfig config = new BirdEyeWindowConfig(context);
        config.resetLayout(); config.setSize(40); config.position(400, 150);
        java.util.Map<String, ?> saved = preferences.getAll();
        AtomicReference<BirdEyePopupView> current = new AtomicReference<>();
        main(() -> { BirdEyePopupView view = new BirdEyePopupView(context, () -> {});
            current.set(view); view.show(); });
        try {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            main(() -> control(current.get(), context.getString(R.string.side_control_expand)).performClick());
            assertExpandedViewport(current.get(), metrics.get());
            if (changeInsets) {
                int oldHeight = params(current.get()).height;
                main(() -> {
                    metrics.set(nativeMetrics);
                    current.get().dispatchApplyWindowInsets(nativeMetrics.getWindowInsets());
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                assertExpandedViewport(current.get(), nativeMetrics);
                assertTrue("Expansion grows when reserved system-bar space disappears", params(current.get()).height > oldHeight);
            }
            main(() -> {
                assertNotNull(control(current.get(), context.getString(R.string.side_control_restore)));
                assertEquals("Viewport refitting never overwrites normal placement", saved, preferences.getAll());
            });
            if ("true".equals(InstrumentationRegistry.getArguments().getString("captureViewport"))) {
                android.os.SystemClock.sleep(400);
                shell("screencap -p /sdcard/Download/zeekr-360-viewport-" + (changeInsets ? "changed" : "native") + ".png");
            }
            main(() -> {
                control(current.get(), context.getString(R.string.side_control_restore)).performClick();
                assertEquals(400, params(current.get()).x); assertEquals(150, params(current.get()).y);
                assertEquals(saved, preferences.getAll());
                assertNotNull(control(current.get(), context.getString(R.string.side_control_expand)));
            });
        } finally {
            main(() -> current.get().detach());
            android.content.SharedPreferences.Editor edit = preferences.edit().clear();
            for (java.util.Map.Entry<String, ?> entry : original.entrySet()) {
                if (entry.getValue() instanceof Integer) edit.putInt(entry.getKey(), (Integer) entry.getValue());
                else if (entry.getValue() instanceof Boolean) edit.putBoolean(entry.getKey(), (Boolean) entry.getValue());
            }
            assertTrue(edit.commit());
        }
    }

    private void assertExpandedViewport(BirdEyePopupView view, android.view.WindowMetrics metrics) {
        android.graphics.Insets bars = metrics.getWindowInsets().getInsets(
                android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
        int availableHeight = metrics.getBounds().height() - bars.top - bars.bottom;
        int margin = view.getResources().getDimensionPixelSize(R.dimen.side_control_margin);
        main(() -> {
            assertEquals(availableHeight - 2 * margin, params(view).height);
            assertEquals(availableHeight - margin, params(view).y + params(view).height);
            assertEquals(2f / 3f, params(view).width / (float) params(view).height, .002f);
            assertEquals(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout(),
                    params(view).getFitInsetsTypes());
            assertFalse(params(view).isFitInsetsIgnoringVisibility());
        });
        long deadline = android.os.SystemClock.uptimeMillis() + 4000;
        AtomicBoolean settled = new AtomicBoolean();
        while (!settled.get() && android.os.SystemClock.uptimeMillis() < deadline) {
            main(() -> {
                int[] location = new int[2]; view.getLocationOnScreen(location);
                settled.set(view.getHeight() == params(view).height
                        && location[1] + view.getHeight() == metrics.getBounds().bottom - bars.bottom - margin);
            });
            if (!settled.get()) android.os.SystemClock.sleep(20);
        }
        assertTrue("Actual overlay bottom must reach the available bottom margin", settled.get());
    }
    private void twoPointers(View view, long time, int action, float left, float right) {
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[2];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[2];
        for (int i = 0; i < 2; i++) {
            properties[i] = new MotionEvent.PointerProperties(); properties[i].id = i;
            properties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;
            coords[i] = new MotionEvent.PointerCoords(); coords[i].x = i == 0 ? left : right;
            coords[i].y = 300; coords[i].pressure = 1; coords[i].size = 1;
        }
        MotionEvent event = MotionEvent.obtain(time, time + 20, action, 2, properties, coords,
                0, 0, 1, 1, 0, 0, android.view.InputDevice.SOURCE_TOUCHSCREEN, 0);
        view.dispatchTouchEvent(event); event.recycle();
    }

    @Test public void expandRestoreDragAndCloseUseIndependentSavedGeometry() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        shell("appops set " + target.getPackageName() + " SYSTEM_ALERT_WINDOW allow");
        Context context = new ContextThemeWrapper(target, R.style.Theme_Cam);
        BirdEyeWindowConfig config = new BirdEyeWindowConfig(context);
        config.resetLayout(); config.setPopup(true);
        BirdEyeConfig calibration = new BirdEyeConfig(context);
        calibration.set("length", 530); calibration.set("quality", BirdEyeQuality.FULL);
        AtomicReference<BirdEyePopupView> current = new AtomicReference<>();
        AtomicBoolean closed = new AtomicBoolean();
        main(() -> {
            BirdEyePopupView view = new BirdEyePopupView(context, () -> { closed.set(true); current.get().detach(); });
            current.set(view); view.show();
        });
        try {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            main(() -> {
                BirdEyePopupView view = current.get();
                int sw = view.getResources().getDisplayMetrics().widthPixels;
                int sh = view.getResources().getDisplayMetrics().heightPixels;
                WindowManager.LayoutParams p = params(view);
                assertTrue(p.x >= 0 && p.y >= 0 && p.x + p.width <= sw && p.y + p.height <= sh);
                int x = p.x, y = p.y, width = p.width, height = p.height;
                View expand = control(view, context.getString(R.string.side_control_expand));
                assertNotNull(expand);
                assertTrue(expand.getWidth() >= 48 * context.getResources().getDisplayMetrics().density);
                expand.performClick();
                assertTrue(params(view).height >= height);
                assertEquals(-1, config.x()); // Expansion must never save enlarged geometry.
                View restore = control(view, context.getString(R.string.side_control_restore));
                assertNotNull(restore); restore.performClick();
                assertEquals(x, params(view).x); assertEquals(y, params(view).y);
                assertEquals(width, params(view).width); assertEquals(height, params(view).height);
                long time = android.os.SystemClock.uptimeMillis();
                MotionEvent down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, width / 2f, height / 2f, 0);
                view.dispatchTouchEvent(down); down.recycle();
                MotionEvent move = MotionEvent.obtain(time, time + 20, MotionEvent.ACTION_MOVE, width / 2f - 80, height / 2f + 20, 0);
                view.dispatchTouchEvent(move); move.recycle();
                MotionEvent up = MotionEvent.obtain(time, time + 40, MotionEvent.ACTION_UP, width / 2f - 80, height / 2f + 20, 0);
                view.dispatchTouchEvent(up); up.recycle();
                assertEquals(params(view).x, config.x()); assertEquals(params(view).y, config.y());
                assertTrue(config.x() < x);
                MotionEvent pinchDown = MotionEvent.obtain(time + 100, time + 100, MotionEvent.ACTION_DOWN, 60, 300, 0);
                view.dispatchTouchEvent(pinchDown); pinchDown.recycle();
                twoPointers(view, time + 100, MotionEvent.ACTION_POINTER_DOWN | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), 60, 360);
                twoPointers(view, time + 140, MotionEvent.ACTION_MOVE, 20, 460);
                twoPointers(view, time + 180, MotionEvent.ACTION_MOVE, -60, 540);
                twoPointers(view, time + 210, MotionEvent.ACTION_MOVE, -160, 640);
                twoPointers(view, time + 240, MotionEvent.ACTION_POINTER_UP | (1 << MotionEvent.ACTION_POINTER_INDEX_SHIFT), -160, 640);
                MotionEvent pinchUp = MotionEvent.obtain(time + 100, time + 280, MotionEvent.ACTION_UP, 30, 300, 0);
                view.dispatchTouchEvent(pinchUp); pinchUp.recycle();
                assertTrue("Pinching must change the saved popup size", config.size() > 45);
                assertEquals(530, calibration.get("length", 0));
                assertEquals(BirdEyeQuality.FULL, calibration.quality());
            });
            // WindowManager applies resize/move asynchronously after the UI callback.
            AtomicBoolean settled = new AtomicBoolean();
            long deadline = android.os.SystemClock.uptimeMillis() + 4000;
            while (!settled.get() && android.os.SystemClock.uptimeMillis() < deadline) {
                main(() -> {
                    BirdEyePopupView view = current.get(); int[] location = new int[2]; view.getLocationOnScreen(location);
                    WindowManager.LayoutParams p = params(view);
                    settled.set(view.getWidth() == p.width && view.getHeight() == p.height
                            && location[0] >= 0 && location[0] + view.getWidth() <= view.getResources().getDisplayMetrics().widthPixels);
                });
                if (!settled.get()) android.os.SystemClock.sleep(20);
            }
            assertTrue("Resized popup must settle fully inside the display", settled.get());
            InstrumentationRegistry.getInstrumentation().getUiAutomation().waitForIdle(300, 4000);
            main(() -> {
                BirdEyePopupView view = current.get(); int[] location = new int[2]; view.getLocationOnScreen(location);
                android.util.DisplayMetrics real = new android.util.DisplayMetrics(); view.getDisplay().getRealMetrics(real);
                WindowManager.LayoutParams p = params(view);
                android.util.Log.d("BirdPopupGeometry", "resources=" + view.getResources().getDisplayMetrics().widthPixels
                        + "x" + view.getResources().getDisplayMetrics().heightPixels + " real=" + real.widthPixels + "x" + real.heightPixels
                        + " params=" + p.x + "," + p.y + " " + p.width + "x" + p.height
                        + " location=" + location[0] + "," + location[1] + " view=" + view.getWidth() + "x" + view.getHeight());
            });
            shell("screencap -p /sdcard/Download/zeekr-360-popup-native.png");
            main(() -> {
                View close = control(current.get(), context.getString(R.string.side_control_close));
                assertNotNull(close); close.performClick();
                assertTrue(closed.get());
                assertTrue(config.popup()); // X never changes the preferred display mode.
                config.resetLayout();
                assertEquals(45, config.size()); assertEquals(-1, config.x());
                assertEquals(530, calibration.get("length", 0));
            });
        } finally { main(() -> current.get().detach()); }
    }
}
