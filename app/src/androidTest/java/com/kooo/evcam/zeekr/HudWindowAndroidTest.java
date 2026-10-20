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
        int oldSize = config.getHudSideSizePercent(), oldShape = config.getHudSideShapePercent();
        int oldCrop = config.getHudSideInnerCropPercent();
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
            config.setHudSideInnerCropPercent(0);
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
                // Simulate a smaller app-compatible Presentation content area. Physical
                // display metrics remain 1280x640, so the previous placement would clip.
                FrameLayout content = (FrameLayout) panel.getParent();
                content.setLayoutParams(new FrameLayout.LayoutParams(640,360));
                config.setHudSideSizePercent(100); config.setHudSideShapePercent(300);
                panel.applyConfig();
                panel.conceal();
            });
            SystemClock.sleep(250);
            main(() -> {
                SideViewHudPanel panel=ref.get(); FrameLayout content=(FrameLayout)panel.getParent();
                assertEquals(640,content.getWidth());assertEquals(360,content.getHeight());
                assertTrue(panel.getLeft()>=0&&panel.getTop()>=0);
                assertTrue("Whole camera panel must fit measured content",panel.getRight()<=640&&panel.getBottom()<=360);
                assertEquals("Keep the requested wide shape",3f,panel.getWidth()/(float)panel.getHeight(),.02f);
                assertTrue(SideViewHudPanel.lastState().contains("viewport=640x360"));
                config.setHudSideInnerCropPercent(25); panel.applyConfig();
            });
            SystemClock.sleep(250);
            main(() -> {
                SideViewHudPanel panel=ref.get();
                assertEquals(480,panel.getWidth()); assertEquals(213,panel.getHeight());
                assertEquals(160,panel.getLeft()); assertEquals(640,panel.getRight());
                assertEquals("Trim pixels, never squeeze the camera",640,panel.getTextureView().getWidth());
                int offset=HudPanelPlacement.keepsRightEdge(LaneCycle.RIGHT,config.isHudSideMirrored())?160:0;
                assertEquals(offset,panel.cameraPictureOffsetX());
                assertTrue(SideViewHudPanel.lastState().contains("innerCrop=25% picture=640 offsetX="+offset));
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
            config.setHudSideSizePercent(oldSize);config.setHudSideShapePercent(oldShape);
            config.setHudSideInnerCropPercent(oldCrop);
            config.setHudSideOpacityPercent(oldOpacity); config.setHudSideNightPercent(oldNight);
        }
    }

    @Test public void confirmed009LayoutHasIndependentRightHandDefaultsAndPersistence() {
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppConfig config=new AppConfig(target);
        android.content.SharedPreferences prefs=target.getSharedPreferences("app_config",Context.MODE_PRIVATE);
        java.util.Map<String,?> before=prefs.getAll();
        try {
            prefs.edit().remove("009_hud_side_size").remove("009_hud_side_shape").remove("009_hud_side_left")
                    .remove("009_hud_side_vertical").remove("009_hud_side_follow").remove("009_hud_side_inner_crop").commit();
            config.selectHudLayout(false);config.setHudSideLeftPercent(12);config.setHudSideSizePercent(80);
            config.setHudSideFollowSignal(true);
            assertEquals(0,config.getHudSideInnerCropPercent()); config.setHudSideInnerCropPercent(10);
            config.selectHudLayout(true);
            assertEquals(25,config.getHudSideInnerCropPercent());
            config.setHudSideInnerCropPercent(35);
            assertEquals(35,new AppConfig(target).getHudSideInnerCropPercent());
            assertEquals(94,config.getHudSideLeftPercent());assertEquals(50,config.getHudSideSizePercent());
            assertFalse(config.isHudSideFollowSignal());
            config.setHudSideFollowSignal(true);assertEquals(6,config.getHudSideLeftPercent());
            config.setHudSideFollowSignal(false);assertEquals(94,config.getHudSideLeftPercent());
            config.setHudSideLeftPercent(88);config.setHudSideSizePercent(55);
            assertEquals(88,new AppConfig(target).getHudSideLeftPercent());
            config.selectHudLayout(false);
            assertEquals(10,config.getHudSideInnerCropPercent());
            assertEquals(12,config.getHudSideLeftPercent());assertEquals(80,config.getHudSideSizePercent());
            assertTrue(config.isHudSideFollowSignal());
            config.selectHudLayout(true);assertEquals(88,config.getHudSideLeftPercent());
            config.resetHudPicture(); assertEquals(35,config.getHudSideInnerCropPercent());
            config.resetHudPlacement(); assertEquals(25,config.getHudSideInnerCropPercent());
            config.setHudSideInnerCropPercent(60);
            config.resetSidePopupSettings();assertEquals(94,config.getHudSideLeftPercent());
            assertEquals(25,config.getHudSideInnerCropPercent());
            assertEquals(50,config.getHudSideSizePercent());assertFalse(config.isHudSideFollowSignal());
            // The obsolete enable preference is irrelevant to matching and layout.
            prefs.edit().putBoolean("hud_zeekr_009",false).commit();
            assertTrue(config.is009HudLayout());
        } finally {
            android.content.SharedPreferences.Editor edit=prefs.edit().clear();
            for(java.util.Map.Entry<String,?> e:before.entrySet()) {
                Object v=e.getValue();String k=e.getKey();
                if(v instanceof Boolean) edit.putBoolean(k,(Boolean)v);
                else if(v instanceof Integer) edit.putInt(k,(Integer)v);
                else if(v instanceof Long) edit.putLong(k,(Long)v);
                else if(v instanceof Float) edit.putFloat(k,(Float)v);
                else if(v instanceof String) edit.putString(k,(String)v);
                else if(v instanceof java.util.Set) edit.putStringSet(k,new java.util.HashSet<>((java.util.Set<String>)v));
            }
            assertTrue(edit.commit());
        }
    }
}
