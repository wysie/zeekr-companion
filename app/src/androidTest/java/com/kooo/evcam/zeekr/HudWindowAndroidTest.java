package com.kooo.evcam.zeekr;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.ContextThemeWrapper;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Own disposable secondary displays only; no vehicle display or camera is opened. */
@RunWith(AndroidJUnit4.class)
public class HudWindowAndroidTest {
    private void main(Runnable r) { InstrumentationRegistry.getInstrumentation().runOnMainSync(r); }

    @Test public void presentationWindowRemainsOnTheSelectedDisplay() throws Exception { exercise(false); }
    @Test public void legacyOverlayFlagStillUsesTheNormalPresentation() throws Exception { exercise(true); }

    private void exercise(boolean legacyOverlay) throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("appops set " + target.getPackageName() + " SYSTEM_ALERT_WINDOW allow");
             java.io.InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd)) {
            while (in.read() != -1) {}
        }
        Context context = new ContextThemeWrapper(target, R.style.Theme_Cam);
        AppConfig config = new AppConfig(context);
        android.content.SharedPreferences preferences = target.getSharedPreferences("app_config", Context.MODE_PRIVATE);
        boolean hadLegacy = preferences.contains("hud_side_overlay");
        boolean oldLegacy = preferences.getBoolean("hud_side_overlay", false);
        boolean oldFollow = config.isHudSideFollowSignal();
        int oldOpacity = config.getHudSideOpacityPercent(), oldNight = config.getHudSideNightPercent();
        int dayContrast = config.getHudSideContrastPercent(), nightContrast = config.getHudSideContrastNightPercent();
        AtomicReference<SideViewHudPanel> ref = new AtomicReference<>();
        ImageReader images = ImageReader.newInstance(1280, 640, PixelFormat.RGBA_8888, 3);
        android.os.HandlerThread consumer = new android.os.HandlerThread("hud-test-display");
        consumer.start();
        images.setOnImageAvailableListener(reader -> {
            try (android.media.Image image = reader.acquireLatestImage()) {}
        }, new android.os.Handler(consumer.getLooper()));
        DisplayManager displays = (DisplayManager) target.getSystemService(Context.DISPLAY_SERVICE);
        VirtualDisplay display = displays.createVirtualDisplay("Disposable HUD test", 1280, 640, 160,
                images.getSurface(), DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC
                        | DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION
                        | DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY);
        assertNotNull(display);
        try {
            // Upgrades can retain this obsolete value; it must never select an overlay window.
            preferences.edit().putBoolean("hud_side_overlay", legacyOverlay).commit();
            config.setHudSideContrastPercent(160);config.setHudSideContrastNightPercent(100);
            config.setHudSideFollowSignal(true);
            config.setHudSideOpacityPercent(80); config.setHudSideNightPercent(50);
            main(() -> {
                SideViewHudPanel panel = new SideViewHudPanel(context, config, display.getDisplay());
                ref.set(panel); panel.show(LaneCycle.LEFT);
            });
            SystemClock.sleep(250);
            main(() -> {
                SideViewHudPanel panel = ref.get();
                assertTrue(SideViewHudPanel.lastState(), panel.isAttached());
                assertEquals(display.getDisplay().getDisplayId(), panel.getRootView().getDisplay().getDisplayId());
                WindowManager.LayoutParams window = (WindowManager.LayoutParams) panel.getRootView().getLayoutParams();
                assertNotEquals(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, window.type);
                assertTrue((window.flags & WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE) != 0);
                assertEquals(.8f, window.alpha, .01f);
                int left = ((FrameLayout.LayoutParams) panel.getLayoutParams()).leftMargin;
                int width = panel.getLayoutParams().width;
                panel.show(LaneCycle.RIGHT);
                assertEquals(1280 - width - left,
                        ((FrameLayout.LayoutParams) panel.getLayoutParams()).leftMargin);
                assertNotNull(panel.pictureTone());
                panel.setNight(true);
                assertNull(panel.pictureTone());
                panel.setNight(false);
                assertNotNull(panel.pictureTone());
                panel.setNight(true);
                assertEquals(.4f, ((WindowManager.LayoutParams) panel.getRootView().getLayoutParams()).alpha, .01f);
                panel.conceal();
            });
            SystemClock.sleep(250);
            main(() -> {
                assertEquals(0f, ((WindowManager.LayoutParams) ref.get().getRootView().getLayoutParams()).alpha, 0f);
                ref.get().detach(); assertFalse(ref.get().isAttached());
            });
        } finally {
            config.setHudSideContrastPercent(dayContrast);config.setHudSideContrastNightPercent(nightContrast);
            main(() -> { if (ref.get() != null) ref.get().detach(); });
            display.release(); images.close(); consumer.quitSafely(); consumer.join(1000);
            if (hadLegacy) preferences.edit().putBoolean("hud_side_overlay", oldLegacy).commit();
            else preferences.edit().remove("hud_side_overlay").commit();
            config.setHudSideFollowSignal(oldFollow);
            config.setHudSideOpacityPercent(oldOpacity); config.setHudSideNightPercent(oldNight);
        }
    }
}
