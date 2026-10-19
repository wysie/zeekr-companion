package com.kooo.evcam.zeekr;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.ContextThemeWrapper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.R;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Real preferences and production renderers; never opens a camera or vehicle display. */
@RunWith(AndroidJUnit4.class)
public class HudPictureAndroidTest {
    private Context target;
    private SharedPreferences prefs;
    private Map<String, ?> before;
    private AppConfig config;

    @Before public void setup() {
        target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = target.getSharedPreferences("app_config", Context.MODE_PRIVATE);
        before = prefs.getAll();
        prefs.edit().clear().commit();
        config = new AppConfig(target);
    }

    @After public void restore() {
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

    private void popupPicture() {
        config.setSidePopupFov(90); config.setSidePopupYaw(50); config.setSidePopupPitch(15);
        config.setSidePopupRotation(LaneCycle.LEFT, 30); config.setSidePopupRotation(LaneCycle.RIGHT, -30);
        config.setSidePopupMirrored(true); config.setRearViewCameraLabelEnabled(true);
        config.setSidePopupStraighten(true); config.setSidePopupAutoFit(false);
    }

    private void assertPopupPicture() {
        assertEquals(90, config.getHudSideFov()); assertEquals(50, config.getHudSideYaw());
        assertEquals(15, config.getHudSidePitch());
        assertEquals(30, config.getHudSideRotation(LaneCycle.LEFT));
        assertEquals(-30, config.getHudSideRotation(LaneCycle.RIGHT));
        assertTrue(config.isHudSideMirrored()); assertTrue(config.isHudSideCameraLabelEnabled());
        assertTrue(config.isHudSideStraighten()); assertFalse(config.isHudSideAutoFit());
    }

    @Test public void newSettingsFollowPopupIncludingLaterEdits() {
        assertTrue(config.isHudFollowingPopupPicture());
        popupPicture(); assertPopupPicture();
        config.setSidePopupYaw(24); config.setSidePopupMirrored(false);
        config.setRearViewCameraLabelEnabled(false); config.setSidePopupStraighten(false);
        config.setSidePopupAutoFit(true); config.setSidePopupRotation(LaneCycle.RIGHT, -10);
        config = new AppConfig(target);
        assertTrue(config.isHudFollowingPopupPicture()); assertEquals(24, config.getHudSideYaw());
        assertFalse(config.isHudSideMirrored()); assertFalse(config.isHudSideCameraLabelEnabled());
        assertFalse(config.isHudSideStraighten()); assertTrue(config.isHudSideAutoFit());
        assertEquals(-10, config.getHudSideRotation(LaneCycle.RIGHT));
        assertFalse("Following must not freeze a hidden snapshot", prefs.contains("hud_custom_picture_initialized"));
    }

    @Test public void customizationStartsFromPopupAndSurvivesFollowingAgain() {
        popupPicture(); config.setHudFollowingPopupPicture(false); assertPopupPicture();
        config.setSidePopupYaw(20); config.setSidePopupFov(80);
        assertPopupPicture();
        config.setHudSideFov(100); config.setHudSideYaw(40); config.setHudSidePitch(10);
        config.setHudSideRotation(LaneCycle.LEFT, 12); config.setHudSideRotation(LaneCycle.RIGHT, -14);
        config.setHudSideMirrored(false); config.setHudSideCameraLabelEnabled(false);
        config.setHudSideStraighten(false); config.setHudSideAutoFit(true);
        assertEquals(20, config.getSidePopupYaw()); assertEquals(80, config.getSidePopupFov());
        assertTrue(config.isSidePopupMirrored()); assertTrue(config.isRearViewCameraLabelEnabled());
        assertTrue(config.isSidePopupStraighten()); assertFalse(config.isSidePopupAutoFit());
        config.setHudFollowingPopupPicture(true);
        assertEquals(20, config.getHudSideYaw()); assertTrue(config.isHudSideMirrored());
        config.setHudFollowingPopupPicture(false); config = new AppConfig(target);
        assertEquals(100, config.getHudSideFov()); assertEquals(40, config.getHudSideYaw());
        assertEquals(10, config.getHudSidePitch());
        assertEquals(12, config.getHudSideRotation(LaneCycle.LEFT));
        assertEquals(-14, config.getHudSideRotation(LaneCycle.RIGHT));
        assertFalse(config.isHudSideMirrored()); assertFalse(config.isHudSideCameraLabelEnabled());
        assertFalse(config.isHudSideStraighten()); assertTrue(config.isHudSideAutoFit());
    }

    @Test public void explicitLegacyMirrorIsPreservedBeforeAndAfterFollowing() {
        popupPicture();
        prefs.edit().putBoolean("hud_side_mirror", false).commit();
        assertFalse(config.isHudFollowingPopupPicture()); assertFalse(config.isHudSideMirrored());
        assertEquals(50, config.getHudSideYaw());
        config.setHudFollowingPopupPicture(true); assertTrue(config.isHudSideMirrored());
        config.setSidePopupYaw(25);
        config.setHudFollowingPopupPicture(false);
        assertFalse(config.isHudSideMirrored()); assertEquals(50, config.getHudSideYaw());
    }

    @Test public void pictureModeNeverChangesLayoutOrToneAndResetReturnsToFollow() {
        config.selectHudLayout(true);
        config.setHudSideSizePercent(55); config.setHudSideShapePercent(200);
        config.setHudSideLeftPercent(88); config.setHudSideVerticalPercent(60);
        config.setHudSideFollowSignal(false); config.setHudSideOpacityPercent(80);
        config.setHudSideNightPercent(70); config.setHudSideContrastPercent(160);
        config.setHudSideContrastNightPercent(120); config.setHudSideFadePercent(20);
        popupPicture(); config.setHudFollowingPopupPicture(false); config.setHudSideYaw(35);
        config.setHudFollowingPopupPicture(true);
        assertEquals(55, config.getHudSideSizePercent()); assertEquals(200, config.getHudSideShapePercent());
        assertEquals(88, config.getHudSideLeftPercent()); assertEquals(60, config.getHudSideVerticalPercent());
        assertFalse(config.isHudSideFollowSignal()); assertEquals(80, config.getHudSideOpacityPercent());
        assertEquals(70, config.getHudSideNightPercent()); assertEquals(160, config.getHudSideContrastPercent());
        assertEquals(120, config.getHudSideContrastNightPercent()); assertEquals(20, config.getHudSideFadePercent());
        config.setSidePopupEnabled(true); config.resetSidePopupSettings();
        assertTrue(config.isSidePopupEnabled()); assertTrue(config.isHudFollowingPopupPicture());
        assertEquals(config.getSidePopupYaw(), config.getHudSideYaw());
        assertFalse(prefs.contains("hud_custom_picture_initialized"));
        for (String key : prefs.getAll().keySet()) assertFalse(key.startsWith("hud_picture_"));
    }

    @Test public void productionHudReadsCustomPictureWhilePopupKeepsItsOwn() {
        popupPicture(); config.setHudFollowingPopupPicture(false);
        config.setHudSideFov(100); config.setHudSideYaw(40); config.setHudSidePitch(10);
        config.setHudSideRotation(LaneCycle.LEFT, 12); config.setHudSideMirrored(false);
        config.setHudSideCameraLabelEnabled(false); config.setHudSideStraighten(false); config.setHudSideAutoFit(true);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context context = new ContextThemeWrapper(target, R.style.Theme_Cam);
            SideViewHudPanel hud = new SideViewHudPanel(context, config, null);
            SideViewPopupView popup = new SideViewPopupView(context, config);
            assertEquals(100, hud.readFov()); assertEquals(40, hud.readYaw()); assertEquals(10, hud.readPitch());
            assertEquals(12, hud.readRotation(LaneCycle.LEFT)); assertFalse(hud.readMirrored());
            assertFalse(hud.readCameraLabel()); assertFalse(hud.readStraighten()); assertTrue(hud.readAutoFit());
            assertEquals(90, popup.readFov()); assertEquals(50, popup.readYaw()); assertEquals(15, popup.readPitch());
            assertEquals(30, popup.readRotation(LaneCycle.LEFT)); assertTrue(popup.readMirrored());
            assertTrue(popup.readCameraLabel()); assertTrue(popup.readStraighten()); assertFalse(popup.readAutoFit());
            config.setHudFollowingPopupPicture(true); hud.applyConfig();
            assertEquals(popup.readFov(), hud.readFov()); assertEquals(popup.readRotation(LaneCycle.LEFT), hud.readRotation(LaneCycle.LEFT));
            assertEquals(popup.readMirrored(), hud.readMirrored());
        });
    }

    @Test public void hudBadgesStayInsideEvenTheSmallestCroppedWindow() {
        Context context=new ContextThemeWrapper(target,R.style.Theme_Cam);
        for(int width:new int[]{48,120,240}) for(int lane:new int[]{LaneCycle.LEFT,LaneCycle.RIGHT}) {
            int height=320,padding=16;
            android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(width+padding*2,height+padding*2,
                    android.graphics.Bitmap.Config.ARGB_8888);
            try {
                android.graphics.Canvas canvas=new android.graphics.Canvas(image);canvas.translate(padding,padding);
                new SideTurnArrow(context,false).draw(canvas,lane,null,width,height,false);
                new CameraLabel(context,false).drawBottom(canvas,lane,width,height);
                int green=0,white=0;
                for(int y=0;y<image.getHeight();y++) for(int x=0;x<image.getWidth();x++) {
                    int color=image.getPixel(x,y);
                    if(x<padding||x>=padding+width||y<padding||y>=padding+height)
                        assertEquals("Badge must not be clipped at the window boundary",0,android.graphics.Color.alpha(color));
                    if(android.graphics.Color.green(color)>android.graphics.Color.red(color)+40)green++;
                    if(android.graphics.Color.red(color)>200&&android.graphics.Color.green(color)>200)white++;
                }
                assertTrue("HUD arrow must not reserve room for absent controls",green>10);
                assertTrue("Direction label must remain visible",white>5);
            } finally {image.recycle();}
        }
    }
}
