package com.kooo.evcam.developer;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.os.SystemClock;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Spinner;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.R;
import com.kooo.evcam.settings.DeveloperMode;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicReference;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

/** Real native views/windows, using only this test's disposable virtual secondary display. */
@RunWith(AndroidJUnit4.class)
public class DisplayIdentificationAndroidTest {
    private void main(Runnable action) { InstrumentationRegistry.getInstrumentation().runOnMainSync(action); }
    private Context context() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }

    @Test public void mainConfirmationIsVisibleBeforeSecondaryTestAndObservationIsExplicit() throws Exception {
        boolean oldMode = DeveloperMode.isUnlocked(); DeveloperMode.setEnabled(context(), true);
        ImageReader reader = ImageReader.newInstance(1280, 640, PixelFormat.RGBA_8888, 3);
        android.os.HandlerThread consumer = new android.os.HandlerThread("display-marker-consumer"); consumer.start();
        reader.setOnImageAvailableListener(r -> { try (android.media.Image image = r.acquireLatestImage()) {} },
                new android.os.Handler(consumer.getLooper()));
        VirtualDisplay virtual = ((DisplayManager) context().getSystemService(Context.DISPLAY_SERVICE))
                .createVirtualDisplay("Owned marker test", 1280, 640, 160, reader.getSurface(),
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
                                | DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
        assertNotNull(virtual);
        AtomicReference<DisplayIdentification> ref = new AtomicReference<>(); JSONObject report = new JSONObject();
        AtomicReference<FrameLayout> host = new AtomicReference<>(); int[] finished = {0};
        try (ActivityScenario<CameraDisplayToolsActivity> scenario = ActivityScenario.launch(CameraDisplayToolsActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    FrameLayout parent = activity.findViewById(android.R.id.content); host.set(parent);
                    DisplayIdentification test = new DisplayIdentification(activity, parent, report, listener(finished));
                    ref.set(test); test.start(new Display[]{activity.getDisplay(), virtual.getDisplay()});
                } catch (Exception e) { throw new AssertionError(e); }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            SystemClock.sleep(200);
            main(() -> {
                assertFalse(report.optBoolean("main_screen_confirmed"));
                JSONObject primary = report.optJSONArray("displays").optJSONObject(0).optJSONArray("attempts").optJSONObject(0);
                assertTrue(primary.toString(), primary.optBoolean("android_layout_ready"));
                assertTrue(primary.optInt("view_height") > 200);
                assertEquals(0, report.optJSONArray("displays").optJSONObject(1).optJSONArray("attempts").length());
            });
            capture("zeekr-display-main-check.png");
            onView(visibleText(context().getString(R.string.dev_display_main_confirm))).perform(click());
            main(() -> assertTrue(report.optBoolean("main_screen_confirmed")));
            onView(visibleText(context().getString(R.string.dev_display_test_target,
                    virtual.getDisplay().getDisplayId(), 1280, 640))).perform(click());
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            AtomicReference<android.app.Presentation> window = new AtomicReference<>();
            main(() -> {
                try {
                    java.lang.reflect.Field field = DisplayIdentification.class.getDeclaredField("presentation");
                    field.setAccessible(true); window.set((android.app.Presentation) field.get(ref.get()));
                    assertTrue(window.get().isShowing());
                    assertEquals(virtual.getDisplay().getDisplayId(), window.get().getWindow().getDecorView().getDisplay().getDisplayId());
                    WindowManager.LayoutParams params = window.get().getWindow().getAttributes();
                    assertEquals(960, params.width); assertEquals(480, params.height);
                    assertNotEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, params.type);
                    assertTrue((params.flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0);
                    JSONObject attempt = report.optJSONArray("displays").optJSONObject(1).optJSONArray("attempts").optJSONObject(0);
                    assertTrue(attempt.optBoolean("android_show_returned"));
                    assertEquals("unconfirmed", attempt.optString("observation"));
                    Spinner spinner = findSpinner(host.get()); assertNotNull(spinner); spinner.setSelection(6); // Not seen.
                } catch (Exception e) { throw new AssertionError(e); }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            capture("zeekr-display-secondary-controls.png");
            onView(visibleText(context().getString(R.string.dev_display_save_observation))).perform(click());
            main(() -> {
                assertFalse(window.get().isShowing());
                JSONObject attempt = report.optJSONArray("displays").optJSONObject(1).optJSONArray("attempts").optJSONObject(0);
                assertEquals("not_seen", attempt.optString("observation"));
            });
            // A second attempt must replace, not accumulate, windows; Stop retains no false observation.
            onView(visibleText(context().getString(R.string.dev_display_test_target,
                    virtual.getDisplay().getDisplayId(), 1280, 640))).perform(click());
            AtomicReference<android.app.Presentation> second = new AtomicReference<>();
            main(() -> {
                try {
                    java.lang.reflect.Field field = DisplayIdentification.class.getDeclaredField("presentation");
                    field.setAccessible(true); second.set((android.app.Presentation) field.get(ref.get()));
                    assertTrue(second.get().isShowing()); assertNotSame(window.get(), second.get());
                } catch (Exception e) { throw new AssertionError(e); }
            });
            onView(visibleText(context().getString(R.string.dev_probe_stop))).perform(click());
            main(() -> {
                assertFalse(second.get().isShowing());
                assertEquals("cancelled_unconfirmed", report.optJSONArray("displays").optJSONObject(1)
                        .optJSONArray("attempts").optJSONObject(1).optString("observation"));
                assertEquals(1, finished[0]); assertEquals(1, host.get().getChildCount());
                ref.get().stop("Again"); assertEquals(1, finished[0]);
            });
        } finally {
            main(() -> { if (ref.get() != null) ref.get().stop("Test cleanup"); });
            virtual.release(); reader.close(); consumer.quitSafely(); consumer.join(1000);
            DeveloperMode.setEnabled(context(), oldMode);
        }
    }

    @Test public void decliningMainCardNeverClaimsVisibilityOrStartsSecondaryWindows() throws Exception {
        boolean oldMode = DeveloperMode.isUnlocked(); DeveloperMode.setEnabled(context(), true);
        JSONObject report = new JSONObject(); int[] finished = {0};
        try (ActivityScenario<CameraDisplayToolsActivity> scenario = ActivityScenario.launch(CameraDisplayToolsActivity.class)) {
            scenario.onActivity(activity -> {
                try { new DisplayIdentification(activity, activity.findViewById(android.R.id.content), report, listener(finished))
                        .start(new Display[]{activity.getDisplay()}); }
                catch (Exception e) { throw new AssertionError(e); }
            });
            onView(visibleText(context().getString(R.string.dev_display_main_not_seen))).perform(click());
            main(() -> {
                assertEquals(1, finished[0]); assertFalse(report.optBoolean("main_screen_confirmed"));
                assertEquals("not_seen", report.optJSONArray("displays").optJSONObject(0).optJSONArray("attempts").optJSONObject(0).optString("observation"));
            });
        } finally { DeveloperMode.setEnabled(context(), oldMode); }
    }

    @Test public void timeoutStaysUnconfirmedAndRemovesTheTestPanel() throws Exception {
        boolean oldMode = DeveloperMode.isUnlocked(); DeveloperMode.setEnabled(context(), true);
        JSONObject report = new JSONObject(); int[] finished = {0}; AtomicReference<DisplayIdentification> ref = new AtomicReference<>();
        AtomicReference<FrameLayout> host = new AtomicReference<>();
        try (ActivityScenario<CameraDisplayToolsActivity> scenario = ActivityScenario.launch(CameraDisplayToolsActivity.class)) {
            scenario.onActivity(activity -> {
                try {
                    host.set(activity.findViewById(android.R.id.content));
                    ref.set(new DisplayIdentification(activity, host.get(), report, listener(finished)));
                    ref.get().start(new Display[]{activity.getDisplay()});
                } catch (Exception e) { throw new AssertionError(e); }
            });
            SystemClock.sleep(DisplayIdentification.CARD_MS + 1200);
            main(() -> {
                assertEquals(1, finished[0]); assertEquals(1, host.get().getChildCount());
                assertFalse(report.optBoolean("main_screen_confirmed"));
                assertEquals("timed_out_unconfirmed", report.optJSONArray("displays").optJSONObject(0).optJSONArray("attempts").optJSONObject(0).optString("observation"));
            });
        } finally { main(() -> { if (ref.get() != null) ref.get().stop("Cleanup"); }); DeveloperMode.setEnabled(context(), oldMode); }
    }
    private DisplayIdentification.Listener listener(int[] finished) {
        return new DisplayIdentification.Listener() {
            @Override public void card(String name, android.graphics.Bitmap image) { assertTrue(image.getWidth() > 320); }
            @Override public void changed() { }
            @Override public void finished(String reason) { finished[0]++; }
        };
    }
    private org.hamcrest.Matcher<View> visibleText(String label) {
        return org.hamcrest.Matchers.allOf(withText(label), androidx.test.espresso.matcher.ViewMatchers.isDisplayed());
    }
    private Spinner findSpinner(View view) {
        if (view instanceof Spinner) return (Spinner) view;
        if (view instanceof ViewGroup) for (int i=0; i<((ViewGroup) view).getChildCount(); i++) {
            Spinner result=findSpinner(((ViewGroup) view).getChildAt(i)); if(result!=null)return result;
        }
        return null;
    }
    private void capture(String name) throws Exception {
        File file = new File(context().getExternalFilesDir(null), InstrumentationRegistry.getArguments().getString("capturePrefix", "") + name);
        try (FileOutputStream out = new FileOutputStream(file)) {
            assertTrue(InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot().compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out));
        }
    }
}
