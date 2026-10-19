package com.kooo.evcam.zeekr;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.SystemClock;
import android.view.View;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.android.material.tabs.TabLayout;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.ui.MainPageControlsAndroidTest;
import org.junit.Test;
import static org.junit.Assert.*;

/** Deterministic colored source fixture, never used as production camera imagery. */
public class SideSettingsPreviewAndroidTest extends MainPageControlsAndroidTest {
    private java.lang.reflect.Field field(String name) throws Exception {
        java.lang.reflect.Field field = SideSettingsPreview.class.getDeclaredField(name); field.setAccessible(true); return field;
    }
    @Test public void settingsPictureSelectsTheActualLaneAndCoversStaleFrames() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                String previous = StreamLayoutTable.compositeCameraId();
                try {
                    StreamLayoutTable.setCompositeCameraId("settings-render-fixture");
                    AppConfig config = new AppConfig(activity);
                    config.setSidePopupYaw(0); config.setSidePopupPitch(0); config.setSidePopupMirrored(false);
                    config.setSidePopupRotation(LaneCycle.LEFT, 0); config.setSidePopupRotation(LaneCycle.RIGHT, 0);
                    config.setRearViewCameraLabelEnabled(false); config.setSidePopupFov(90);
                    SideSettingsPreview preview = new SideSettingsPreview(activity, value -> {});
                    // Real geometry includes separator bands; the four source lanes are distinct colours.
                    CompositeStreamGeometry.Plan plan = CompositeStreamGeometry.analyse("settings-render-fixture", 1280, 5140);
                    assertTrue(plan.isComposite());
                    Bitmap source = Bitmap.createBitmap(1280, 5140, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(source); c.drawColor(Color.BLACK);
                    int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};
                    android.graphics.Paint paint = new android.graphics.Paint();
                    for (int i = 0; i < 4; i++) {
                        CompositeStreamGeometry.Lane lane = plan.lane(i); paint.setColor(colors[i]);
                        c.drawRect(lane.x, lane.y, lane.x + lane.width, lane.y + lane.height, paint);
                    }
                    field("sample").set(preview, source); field("plan").set(preview, plan);
                    CameraFrameFreshness freshness = (CameraFrameFreshness) field("freshness").get(preview);
                    View picture = preview.findViewById(R.id.side_preview_picture);
                    picture.measure(View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY));
                    picture.layout(0, 0, 300, 300);
                    Bitmap result = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
                    for (boolean correction : new boolean[]{false, true}) {
                        config.setSidePopupStraighten(correction);
                        for (int lane : new int[]{LaneCycle.LEFT, LaneCycle.RIGHT}) {
                            ((TabLayout) preview.findViewById(R.id.side_preview_left_right)).getTabAt(lane == LaneCycle.LEFT ? 0 : 1).select();
                            long now = SystemClock.elapsedRealtime(); Object identity = new Object();
                            freshness.observe(identity, 1, now); freshness.observe(identity, 2, now);
                            picture.draw(new Canvas(result));
                            assertEquals("Correct source lane, straighten=" + correction, colors[lane], result.getPixel(150, 150));
                        }
                    }
                    freshness.reset(); picture.draw(new Canvas(result));
                    assertEquals(Color.BLACK, result.getPixel(150, 150));
                    preview.stop();
                    assertNull(field("sample").get(preview));
                    assertFalse(freshness.fresh(SystemClock.elapsedRealtime()));
                } catch (Exception failure) { throw new AssertionError(failure); }
                finally { StreamLayoutTable.setCompositeCameraId(previous); }
            });
        }
    }
}
