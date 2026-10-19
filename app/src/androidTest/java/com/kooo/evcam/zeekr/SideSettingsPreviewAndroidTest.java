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
    @Test public void nativeReadbackPreservesFineDetailThroughSettingsCorrection() throws Exception {
        java.util.concurrent.atomic.AtomicReference<android.view.TextureView> sourceView = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicInteger frames = new java.util.concurrent.atomic.AtomicInteger();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                android.view.TextureView texture = new android.view.TextureView(activity);
                texture.setSurfaceTextureListener(new android.view.TextureView.SurfaceTextureListener() {
                    public void onSurfaceTextureAvailable(android.graphics.SurfaceTexture st, int w, int h) { st.setDefaultBufferSize(1280, 5140); }
                    public void onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture st, int w, int h) { st.setDefaultBufferSize(1280, 5140); }
                    public boolean onSurfaceTextureDestroyed(android.graphics.SurfaceTexture st) { return true; }
                    public void onSurfaceTextureUpdated(android.graphics.SurfaceTexture st) { frames.incrementAndGet(); }
                });
                ((android.widget.FrameLayout) activity.findViewById(android.R.id.content)).addView(texture,
                        new android.widget.FrameLayout.LayoutParams(128, 514));
                sourceView.set(texture);
            });
            long deadline = SystemClock.elapsedRealtime() + 5000;
            while (!sourceView.get().isAvailable() && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25);
            assertTrue(sourceView.get().isAvailable());
            android.view.Surface producer = new android.view.Surface(sourceView.get().getSurfaceTexture());
            try {
                Canvas canvas = producer.lockCanvas(null);
                assertEquals(1280, canvas.getWidth()); assertEquals(5140, canvas.getHeight());
                canvas.drawColor(Color.BLACK);
                android.graphics.Paint paint = new android.graphics.Paint(); paint.setColor(Color.WHITE);
                for (int y = 0; y < 5140; y += 4) canvas.drawRect(0, y, 1280, y + 2, paint);
                producer.unlockCanvasAndPost(canvas);
                deadline = SystemClock.elapsedRealtime() + 5000;
                while (frames.get() == 0 && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(25);
                assertTrue("Native source was consumed", frames.get() > 0);
                scenario.onActivity(activity -> {
                    String previous = StreamLayoutTable.compositeCameraId();
                    SideSettingsPreview preview = new SideSettingsPreview(activity, value -> {});
                    Bitmap result = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888);
                    try {
                        StreamLayoutTable.setCompositeCameraId("settings-native-detail-fixture");
                        CompositeStreamGeometry.Plan plan = CompositeStreamGeometry.analyse("settings-native-detail-fixture", 1280, 5140);
                        java.lang.reflect.Method copy = SideSettingsPreview.class.getDeclaredMethod("copySource",
                                android.view.TextureView.class, android.graphics.SurfaceTexture.class, int.class, int.class);
                        copy.setAccessible(true);
                        assertTrue((Boolean) copy.invoke(preview, sourceView.get(), sourceView.get().getSurfaceTexture(), 1280, 5140));
                        Bitmap sample = (Bitmap) field("sample").get(preview);
                        assertEquals(1280, sample.getWidth()); assertEquals(5140, sample.getHeight());
                        assertTrue((Boolean) copy.invoke(preview, sourceView.get(), sourceView.get().getSurfaceTexture(), 1280, 5140));
                        assertSame("Reuse the native sample allocation", sample, field("sample").get(preview));
                        field("plan").set(preview, plan);
                        AppConfig config = new AppConfig(activity);
                        config.setSidePopupYaw(0); config.setSidePopupPitch(0); config.setSidePopupMirrored(false);
                        config.setSidePopupRotation(LaneCycle.LEFT, 0); config.setSidePopupRotation(LaneCycle.RIGHT, 0);
                        config.setRearViewCameraLabelEnabled(false); config.setSidePopupFov(90); config.setSidePopupStraighten(true);
                        config.setSidePopupAspect("1:1");
                        View picture = preview.findViewById(R.id.side_preview_picture);
                        picture.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY));
                        picture.layout(0, 0, 600, 600);
                        for (int lane : new int[]{LaneCycle.LEFT, LaneCycle.RIGHT}) {
                            ((TabLayout) preview.findViewById(R.id.side_preview_left_right)).getTabAt(lane == LaneCycle.LEFT ? 0 : 1).select();
                            CameraFrameFreshness freshness = (CameraFrameFreshness) field("freshness").get(preview);
                            long now = SystemClock.elapsedRealtime(); Object identity = new Object();
                            freshness.observe(identity, 1, now); freshness.observe(identity, 2, now);
                            picture.draw(new Canvas(result));
                            SideViewFraming map = new SideViewFraming(); map.configure(90, 0, 0, lane, 1f);
                            float[] uv = new float[2]; int matched = 0, count = 0, low = 255, high = 0;
                            for (int y = 150; y < 450; y++) {
                                map.sourcePoint(300.5f / 600, (y + .5f) / 600, uv);
                                float row = plan.lane(lane).y + uv[1] * 1280;
                                float phase = row % 4;
                                if (phase < .75f || phase > 3.25f || Math.abs(phase - 2) < .75f) continue;
                                int red = Color.red(result.getPixel(300, y));
                                low = Math.min(low, red); high = Math.max(high, red);
                                if ((phase < 2 && red > 170) || (phase >= 2 && red < 85)) matched++;
                                count++;
                            }
                            android.util.Log.i("SettingsNativeDetail", "lane=" + lane + " matched=" + matched + "/" + count + " range=" + low + ".." + high);
                            assertTrue("Enough fine-detail samples", count > 40);
                            assertTrue("Native bars retain contrast", high - low > 160);
                            assertTrue("Corrected detail follows native source", matched >= count * .9);
                        }
                    } catch (Exception failure) { throw new AssertionError(failure); }
                    finally { preview.stop(); result.recycle(); StreamLayoutTable.setCompositeCameraId(previous); }
                });
            } finally { producer.release(); }
        }
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
