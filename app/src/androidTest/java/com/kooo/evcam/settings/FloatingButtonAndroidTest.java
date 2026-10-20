package com.kooo.evcam.settings;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.preference.ListPreference;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.overlay.FloatingAction;
import com.kooo.evcam.overlay.FloatingButtonGesture;
import com.kooo.evcam.service.RecordingFloatingService;
import com.kooo.evcam.settings.SettingsPreferenceFragment;
import com.kooo.evcam.settings.SettingsShellFragment;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import static com.kooo.evcam.overlay.FloatingAction.*;
import static org.junit.Assert.*;

/** Real overlay touch adapter and preference persistence; action sink never opens a camera. */
@RunWith(AndroidJUnit4.class)
public class FloatingButtonAndroidTest {
    private Context target() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private void main(Runnable r) { InstrumentationRegistry.getInstrumentation().runOnMainSync(r); }
    private static class WindowFixture extends RecordingFloatingService {
        void attach(Context context) { attachBaseContext(context); }
    }
    private static void set(Object service, String key, Object value) throws Exception {
        Field f = RecordingFloatingService.class.getDeclaredField(key); f.setAccessible(true); f.set(service, value);
    }
    private static Object get(Object service, String key) throws Exception {
        Field f = RecordingFloatingService.class.getDeclaredField(key); f.setAccessible(true); return f.get(service);
    }
    private static void call(Object service, String method) {
        try {
            Method m = RecordingFloatingService.class.getDeclaredMethod(method); m.setAccessible(true); m.invoke(service);
        } catch (Exception e) { throw new AssertionError(e); }
    }
    private final class Fixture implements AutoCloseable {
        WindowFixture service;
        View view;
        final List<FloatingAction> actions = new ArrayList<>();
        final AppConfig config = new AppConfig(target());
        final String oldTap = config.getFloatingTapAction(), oldDouble = config.getFloatingDoubleTapAction(),
                oldHold = config.getFloatingLongPressAction();
        final boolean oldLock = config.isFloatingPositionLocked();
        Fixture() throws Exception {
            try (ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .executeShellCommand("appops set " + target().getPackageName() + " SYSTEM_ALERT_WINDOW allow");
                 java.io.InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd)) { while (in.read() != -1) {} }
            config.setFloatingTapAction(TOGGLE_RECORDING.key); config.setFloatingDoubleTapAction(TOGGLE_MIRROR.key);
            config.setFloatingLongPressAction(TAKE_PHOTO.key);
            main(() -> {
                try {
                    service = new WindowFixture(); service.attach(new ContextThemeWrapper(target(), R.style.Theme_Cam));
                    Handler handler = new Handler(Looper.getMainLooper());
                    set(service, "appConfig", config); set(service, "mainHandler", handler);
                    set(service, "windowManager", target().getSystemService(Context.WINDOW_SERVICE));
                    set(service, "screenWidth", 2560); set(service, "screenHeight", 1600);
                    set(service, "gestures", new FloatingButtonGesture(new FloatingButtonGesture.Scheduler() {
                        public void post(Runnable r, long delay) { handler.postDelayed(r, delay); }
                        public void remove(Runnable r) { handler.removeCallbacks(r); }
                    }, actions::add, ViewConfiguration.getDoubleTapTimeout(), 600,
                            ViewConfiguration.get(target()).getScaledDoubleTapSlop()));
                    call(service, "createFloatingWindow"); view = (View) get(service, "floatingContainer");
                } catch (Exception e) { throw new AssertionError(e); }
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            main(() -> assertTrue(view.isAttachedToWindow()));
        }
        void event(int action, float x, float y) {
            main(() -> {
                long now = SystemClock.uptimeMillis();
                MotionEvent event = MotionEvent.obtain(now, now, action, x, y, 0);
                try { view.dispatchTouchEvent(event); } finally { event.recycle(); }
            });
        }
        void tap() { event(MotionEvent.ACTION_DOWN, 20, 20); event(MotionEvent.ACTION_UP, 20, 20); }
        void expect(FloatingAction... expected) { main(() -> assertArrayEquals(expected, actions.toArray())); }
        public void close() {
            main(() -> call(service, "hideFloatingWindow"));
            config.setFloatingTapAction(oldTap); config.setFloatingDoubleTapAction(oldDouble);
            config.setFloatingLongPressAction(oldHold); config.setFloatingPositionLocked(oldLock);
        }
    }
    @Test public void retainedOverlayMatchesHeaderAndMainRedAcrossAppThemeOverrides() throws Exception {
        int oldNight=androidx.appcompat.app.AppCompatDelegate.getDefaultNightMode();
        try(Fixture f=new Fixture()) {
            Object button=get(f.service,"recordingButton");
            Method state=button.getClass().getDeclaredMethod("setRecording",boolean.class,boolean.class);state.setAccessible(true);
            Method progress=button.getClass().getDeclaredMethod("setProgress",float.class);progress.setAccessible(true);
            main(()->{try {f.view.setAlpha(1);state.invoke(button,true,false);progress.invoke(button,.65f);}catch(Exception error){throw new AssertionError(error);}});
            for(String choice:new String[]{AppTheme.LIGHT,AppTheme.DARK,AppTheme.LIGHT}) {
                int[] expected={0};android.graphics.Rect bounds=new android.graphics.Rect();
                main(()->{
                    AppTheme.apply(choice);
                    Context palette=AppTheme.palette(target());int red=palette.getColor(R.color.recording);expected[0]=red;
                    Context themed=new ContextThemeWrapper(palette,R.style.Theme_Cam);
                    View rail=android.view.LayoutInflater.from(themed).inflate(R.layout.layout_action_rail,null);
                    com.kooo.evcam.ui.RecordButtonUi mainControl=new com.kooo.evcam.ui.RecordButtonUi(rail.findViewById(R.id.btn_start_record));
                    mainControl.setState(com.kooo.evcam.ui.RecordButtonUi.State.RECORDING);
                    assertEquals(red,((android.graphics.drawable.GradientDrawable)rail.findViewById(R.id.record_dot).getBackground()).getColor().getDefaultColor());
                    assertEquals(red,((com.google.android.material.progressindicator.CircularProgressIndicator)rail.findViewById(R.id.record_ring)).getIndicatorColor()[0]);
                    View header=android.view.LayoutInflater.from(themed).inflate(R.layout.fragment_settings_two_pane,null);
                    com.google.android.material.button.MaterialButton stop=header.findViewById(R.id.in_app_record_stop);
                    assertEquals(red,stop.getIconTint().getDefaultColor());assertEquals(red,palette.getColor(R.color.recording_text));
                    assertTrue("Recording text contrast",androidx.core.graphics.ColorUtils.calculateContrast(red,palette.getColor(R.color.settings_row))>=4.5);
                    assertTrue("Solid red button contrast",androidx.core.graphics.ColorUtils.calculateContrast(palette.getColor(R.color.on_recording),red)>=4.5);
                    assertTrue("Main control contrast",androidx.core.graphics.ColorUtils.calculateContrast(red,palette.getColor(R.color.bg))>=4.5);
                    assertSame(button,getUnchecked(f.service,"recordingButton"));
                    int[] xy=new int[2];f.view.getLocationOnScreen(xy);bounds.set(xy[0],xy[1],xy[0]+f.view.getWidth(),xy[1]+f.view.getHeight());
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(250);
                // Actual composed hardware output: parent-only invalidation can otherwise leave
                // the child's old cached red on screen even though a software draw looks correct.
                android.graphics.Bitmap image=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                try {
                    int matched=0;
                    for(int y=Math.max(0,bounds.top);y<Math.min(image.getHeight(),bounds.bottom);y++)
                        for(int x=Math.max(0,bounds.left);x<Math.min(image.getWidth(),bounds.right);x++)if(image.getPixel(x,y)==expected[0])matched++;
                    assertTrue("Retained hardware overlay paints the selected red after "+choice+": "+matched,matched>20);
                    if("true".equals(InstrumentationRegistry.getArguments().getString("captureRecordingColours"))) {
                        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new java.io.File(target().getExternalFilesDir(null),"recording-colours-"+choice+".png"))){assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out));}
                    }
                } finally {image.recycle();}
            }
            main(()->{
                try {state.invoke(button,true,true);}catch(Exception error){throw new AssertionError(error);}
                int red=AppTheme.palette(target()).getColor(R.color.recording);
                android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(f.view.getWidth(),f.view.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
                try {f.view.draw(new android.graphics.Canvas(image));int matched=0;
                    for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if(image.getPixel(x,y)==red)matched++;
                    assertEquals("Waiting remains neutral",0,matched);
                }finally{image.recycle();}
            });
        } finally {main(()->androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(oldNight));}
    }
    private static Object getUnchecked(Object service,String key) {
        try {return get(service,key);}catch(Exception error){throw new AssertionError(error);}
    }
    @Test public void realOverlayDoubleTapAndHoldNeverFireSingleAction() throws Exception {
        try (Fixture f = new Fixture()) {
            f.tap(); f.expect(); SystemClock.sleep(50); f.tap();
            SystemClock.sleep(400); f.expect(TOGGLE_MIRROR);
            f.event(MotionEvent.ACTION_DOWN, 20, 20); SystemClock.sleep(700);
            f.event(MotionEvent.ACTION_UP, 20, 20); SystemClock.sleep(400);
            f.expect(TOGGLE_MIRROR, TAKE_PHOTO);
            f.config.setFloatingDoubleTapAction(NONE.key);
            f.tap(); f.expect(TOGGLE_MIRROR, TAKE_PHOTO, TOGGLE_RECORDING);
        }
    }
    @Test public void realOverlayDragCancelAndHideSuppressPendingActions() throws Exception {
        try (Fixture f = new Fixture()) {
            f.config.setFloatingPositionLocked(true);
            f.event(MotionEvent.ACTION_DOWN, 20, 20); f.event(MotionEvent.ACTION_MOVE, 60, 60);
            f.event(MotionEvent.ACTION_UP, 60, 60); SystemClock.sleep(700); f.expect();
            f.event(MotionEvent.ACTION_DOWN, 20, 20); f.event(MotionEvent.ACTION_CANCEL, 20, 20);
            SystemClock.sleep(700); f.expect();
            f.config.setFloatingPositionLocked(false);
            f.event(MotionEvent.ACTION_DOWN, 20, 20); f.event(MotionEvent.ACTION_MOVE, 60, 60);
            f.event(MotionEvent.ACTION_UP, 60, 60); SystemClock.sleep(700); f.expect();
            f.tap(); main(() -> call(f.service, "hideFloatingWindow")); SystemClock.sleep(400); f.expect();
        }
    }
    @Test public void supportedForegroundPageHidesOverlayAndRestoresSavedPlacement() throws Exception {
        org.junit.Assume.assumeTrue(android.os.Build.PRODUCT.contains("sdk")||android.os.Build.HARDWARE.contains("ranchu"));
        try(Fixture f=new Fixture()) {
            final Runnable[] listener={null};final int[] placement=new int[4];
            main(()->{try{listener[0]=(Runnable)get(f.service,"foregroundVisibility");
                com.kooo.evcam.ui.AppRecordingControls.addListener(listener[0]);
                WindowManager.LayoutParams params=(WindowManager.LayoutParams)get(f.service,"layoutParams");placement[0]=params.x;placement[1]=params.y;
                placement[2]=f.config.getRecordingFloatingX();placement[3]=f.config.getRecordingFloatingY();
            }catch(Exception e){throw new AssertionError(e);}});
            try {
                try(ActivityScenario<com.kooo.evcam.ui.CameraStatusActivity> scenario=ActivityScenario.launch(com.kooo.evcam.ui.CameraStatusActivity.class)) {
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                    main(()->{listener[0].run();assertEquals(View.GONE,f.view.getVisibility());assertTrue(f.view.isAttachedToWindow());});
                }
                SystemClock.sleep(250);main(()->{listener[0].run();assertEquals(View.VISIBLE,f.view.getVisibility());
                    try{WindowManager.LayoutParams params=(WindowManager.LayoutParams)get(f.service,"layoutParams");assertEquals(placement[0],params.x);assertEquals(placement[1],params.y);}catch(Exception e){throw new AssertionError(e);}
                    assertEquals(placement[2],f.config.getRecordingFloatingX());assertEquals(placement[3],f.config.getRecordingFloatingY());
                });
            } finally {main(()->com.kooo.evcam.ui.AppRecordingControls.removeListener(listener[0]));}
        }
    }

    @Test public void settingsExposeThreeActionsAndPersistOptionalDoubleTap() {
        Context context = target(); AppConfig config = new AppConfig(context);
        String oldDouble = config.getFloatingDoubleTapAction(); boolean oldEnabled = config.isRecordingFloatingEnabled();
        try {
            context.getSharedPreferences("app_config", Context.MODE_PRIVATE).edit().remove("floating_double_tap_action")
                    .putBoolean("first_launch", false).putBoolean("language_chosen", true)
                    .putBoolean("rail_side_chosen", true).commit();
            assertEquals(NONE.key, config.getFloatingDoubleTapAction());
            String preservedTap = config.getFloatingTapAction(), preservedHold = config.getFloatingLongPressAction();
            config.setRecordingFloatingEnabled(true);
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(activity -> {
                    SettingsShellFragment shell = new SettingsShellFragment();
                    activity.findViewById(R.id.fragment_container).setVisibility(View.VISIBLE);
                    activity.getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, shell).commitNow();
                    shell.getChildFragmentManager().executePendingTransactions(); shell.showSection("screen_floating");
                    shell.getChildFragmentManager().executePendingTransactions();
                    SettingsPreferenceFragment page = (SettingsPreferenceFragment) shell.getChildFragmentManager()
                            .findFragmentById(R.id.settings_detail);
                    assertNotNull(page.findPreference("pref_floating_tap"));
                    assertNotNull(page.findPreference("pref_floating_long_press"));
                    ListPreference doubleTap = page.findPreference("pref_floating_double_tap");
                    assertTrue(doubleTap.isEnabled()); assertEquals(NONE.key, doubleTap.getValue());
                    String[] expected = {OPEN_APP.key, TOGGLE_RECORDING.key, TAKE_PHOTO.key,
                            LOCK_FOOTAGE.key, TOGGLE_MIRROR.key, TOGGLE_360.key, NONE.key};
                    for (String key : new String[]{"pref_floating_tap", "pref_floating_double_tap", "pref_floating_long_press"}) {
                        ListPreference choice = page.findPreference(key);
                        assertArrayEquals("Requested order applies to every gesture", expected, choice.getEntryValues());
                        assertEquals(activity.getString(R.string.floating_action_toggle_360), choice.getEntries()[5]);
                    }
                    assertTrue(doubleTap.callChangeListener(LOCK_FOOTAGE.key)); doubleTap.setValue(LOCK_FOOTAGE.key);
                    assertEquals(LOCK_FOOTAGE.key, new AppConfig(context).getFloatingDoubleTapAction());
                    assertTrue(doubleTap.callChangeListener(TOGGLE_360.key)); doubleTap.setValue(TOGGLE_360.key);
                    assertEquals(TOGGLE_360.key, new AppConfig(context).getFloatingDoubleTapAction());
                    assertEquals(preservedTap, config.getFloatingTapAction());
                    assertEquals(preservedHold, config.getFloatingLongPressAction());
                });
                scenario.onActivity(activity -> {
                    SettingsShellFragment shell = (SettingsShellFragment) activity.getSupportFragmentManager()
                            .findFragmentById(R.id.fragment_container);
                    SettingsPreferenceFragment page = (SettingsPreferenceFragment) shell.getChildFragmentManager()
                            .findFragmentById(R.id.settings_detail);
                    page.findPreference("pref_floating_tap").performClick();
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                SystemClock.sleep(3500);
                android.graphics.Bitmap screenshot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
                assertNotNull(screenshot);
                try (java.io.FileOutputStream out = new java.io.FileOutputStream(
                        new java.io.File(context.getCacheDir(), "double-tap-settings.png"))) {
                    screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
                } catch (java.io.IOException e) { throw new AssertionError(e); }
                finally { screenshot.recycle(); }
                androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.action_cancel))
                        .perform(androidx.test.espresso.action.ViewActions.click());
                scenario.recreate();
                scenario.onActivity(activity -> {
                    SettingsShellFragment shell = (SettingsShellFragment) activity.getSupportFragmentManager()
                            .findFragmentById(R.id.fragment_container);
                    SettingsPreferenceFragment page = (SettingsPreferenceFragment) shell.getChildFragmentManager()
                            .findFragmentById(R.id.settings_detail);
                    assertEquals(TOGGLE_360.key, ((ListPreference) page.findPreference("pref_floating_double_tap")).getValue());
                });
            }
            context.getSharedPreferences("app_config", Context.MODE_PRIVATE).edit()
                    .putString("floating_double_tap_action", "unknown-future-action").commit();
            assertEquals(NONE.key, new AppConfig(context).getFloatingDoubleTapAction());
        } finally { config.setFloatingDoubleTapAction(oldDouble); config.setRecordingFloatingEnabled(oldEnabled); }
    }
    private void dispatch360(RecordingFloatingService service) {
        main(() -> {
            try {
                Method method = RecordingFloatingService.class.getDeclaredMethod("perform", FloatingAction.class);
                method.setAccessible(true); method.invoke(service, TOGGLE_360);
            } catch (Exception e) { throw new AssertionError(e); }
        });
    }
    private void await360(boolean running) {
        long end = SystemClock.uptimeMillis() + 5000;
        while (com.kooo.evcam.zeekr.BirdEyePopupService.isRunning() != running
                && SystemClock.uptimeMillis() < end) SystemClock.sleep(40);
        assertEquals(running, com.kooo.evcam.zeekr.BirdEyePopupService.isRunning());
    }
    @Test public void floating360UsesActualSessionStateWithAndWithoutMainActivity() throws Exception {
        org.junit.Assume.assumeTrue(android.os.Build.PRODUCT.contains("sdk") || android.os.Build.HARDWARE.contains("ranchu"));
        Context context = target();
        java.util.Map<String, ?> window = context.getSharedPreferences("bird_eye_window", 0).getAll();
        java.util.Map<String, ?> sources = context.getSharedPreferences("profiles", 0).getAll();
        main(() -> com.kooo.evcam.zeekr.BirdEyePopupService.stop(context)); await360(false);
        try (Fixture fixture = new Fixture()) {
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                dispatch360(fixture.service); await360(true);
                // With no car link, the emulator session is suspended but still toggles off.
                assertFalse(com.kooo.evcam.zeekr.BirdEyePopupService.isWindowVisible());
                dispatch360(fixture.service); await360(false);
                assertTrue("Main stays open", scenario.getState().isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED));
            }
            assertNull(MainActivity.getInstance());
            dispatch360(fixture.service); await360(true);
            assertNull("Floating action does not launch the app", MainActivity.getInstance());
            main(() -> com.kooo.evcam.zeekr.BirdEyePopupService.stop(context)); await360(false);
            dispatch360(fixture.service); await360(true);
            dispatch360(fixture.service); await360(false);
            assertEquals("No saved popup layout/start preference changes", window,
                    context.getSharedPreferences("bird_eye_window", 0).getAll());
            assertEquals("Recording sources are retained", sources, context.getSharedPreferences("profiles", 0).getAll());
        } finally { main(() -> com.kooo.evcam.zeekr.BirdEyePopupService.stop(context)); }
    }

}
