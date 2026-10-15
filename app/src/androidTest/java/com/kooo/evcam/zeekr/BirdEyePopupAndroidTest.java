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
