package com.kooo.evcam.zeekr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class BirdEyeVehicleOverlayAndroidTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

    @Test public void illustrationFitsBodyAndOnlyMirrorsExtendIntoGroundAtAllOutputSizes() throws Exception {
        BirdEyeVehicleOverlay overlay = new BirdEyeVehicleOverlay();
        for (String model : new String[]{BirdEyeAppearance.MODEL_009, BirdEyeAppearance.MODEL_7X}) {
            for (String colour : new String[]{BirdEyeAppearance.WHITE, BirdEyeAppearance.GREEN, BirdEyeAppearance.BLACK}) {
                overlay.prepare(context.getResources(), model, colour);
                for (int width : new int[]{320, 640, 960}) {
                    int height = width * 3 / 2;
                    RectF body = new RectF(width * .3f, height * .15f, width * .7f, height * .85f);
                    for (int background : new int[]{0xffdedee2, 0xff181a20}) {
                        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(image); canvas.drawColor(background);
                        Paint paint = new Paint(); paint.setColor(0xff779981);
                        for (int x = 0; x < width; x += 40) canvas.drawLine(x, 0, x, height, paint);
                        for (int y = 0; y < height; y += 40) canvas.drawLine(0, y, width, y, paint);
                        Bitmap original = image.copy(Bitmap.Config.ARGB_8888, false);
                        assertTrue(overlay.draw(canvas, body));
                        float extension = body.width() * .1f;
                        // Rounded backing + half the outline stroke + its antialiased edge pixel.
                        float surround = BirdEyeVehicleOverlay.surroundOutset(body.width()) * 1.25f + 1;
                        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                            // Fractional Canvas bounds can antialias the intersected edge pixel.
                            if (x < Math.floor(body.left - extension) || x > Math.ceil(body.right + extension)
                                    || y < Math.floor(body.top - surround) || y > Math.ceil(body.bottom + surround))
                                assertEquals("Camera ground outside vehicle must not change", original.getPixel(x,y), image.getPixel(x,y));
                        }
                        int hood = image.getPixel(width / 2, (int)(body.top + body.height() * .07f));
                        int roof = image.getPixel(width / 2, (int)(body.top + body.height() * .5f));
                        if (BirdEyeAppearance.WHITE.equals(colour)) assertTrue("White hood faces upward", Color.red(hood) > 180);
                        else if (BirdEyeAppearance.GREEN.equals(colour)) assertTrue("Green body", Color.green(hood) > Color.red(hood) + 5);
                        else assertTrue("Black body", Color.red(hood) < 90);
                        assertTrue("Dark roof glass", Color.red(roof) < 120);
                        String theme = background == 0xffdedee2 ? "day" : "night";
                        File directory = new File(context.getExternalFilesDir(null), "overlay-review");
                        assertTrue(directory.isDirectory() || directory.mkdirs());
                        try (FileOutputStream stream = new FileOutputStream(new File(directory, model + "-" + colour + "-" + theme + "-" + width + ".png"))) {
                            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, stream));
                        }
                        original.recycle(); image.recycle();
                    }
                }
            }
        }
    }

    @Test public void unknownVehiclesRetainOutlineAndUnderbodyStaysOpaque() {
        BirdEyeVehicleOverlay overlay = new BirdEyeVehicleOverlay();
        Bitmap image = Bitmap.createBitmap(240, 600, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image); RectF body = new RectF(30, 40, 210, 560);
        overlay.prepare(context.getResources(), BirdEyeAppearance.OUTLINE, BirdEyeAppearance.WHITE);
        assertFalse(overlay.draw(canvas, body)); assertEquals(0, image.getPixel(120,300));
        overlay.prepare(context.getResources(), BirdEyeAppearance.MODEL_009, BirdEyeAppearance.WHITE);
        assertTrue(overlay.draw(canvas, body));
        for (int y=45;y<555;y++) for (int x=35;x<205;x++)
            assertEquals("Unknown ground must remain opaque", 255, Color.alpha(image.getPixel(x,y)));
        for (int[] corner : new int[][]{{30,40},{209,40},{30,559},{209,559}}) {
            int pixel = image.getPixel(corner[0],corner[1]);
            assertEquals("Rounded surround must still cover the unknown rectangular corners",255,Color.alpha(pixel));
            assertNotEquals("No exposed black rectangle",Color.BLACK,pixel);
        }
        overlay.prepare(context.getResources(), BirdEyeAppearance.OUTLINE, BirdEyeAppearance.WHITE);
        assertFalse(overlay.draw(canvas, body));
        image.recycle();
    }

    @Test public void colourChangeRepaintsBothRenderersWithoutInvalidatingCameraFrames() throws Exception {
        BirdEyeAppearance appearance = new BirdEyeAppearance(context);
        String previous = appearance.preferences.getString("vehicle_colour", null);
        String previousModel = appearance.preferences.getString("vehicle_model", null);
        BirdEyePreview[] previews = new BirdEyePreview[2];
        android.content.SharedPreferences.OnSharedPreferenceChangeListener[] listeners =
                new android.content.SharedPreferences.OnSharedPreferenceChangeListener[2];
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                appearance.setModel(BirdEyeAppearance.AUTO);
                appearance.setColour(BirdEyeAppearance.WHITE);
                for (int i=0; i<previews.length; i++) {
                    BirdEyePreview preview = previews[i] = new BirdEyePreview(new android.view.View(context));
                    set(preview,"calibration",BirdEyeImageFit.zeekr009(new int[]{0,1,2,3},1.6));
                    set(preview,"running",true);set(preview,"revision",37);set(preview,"displayedAt",123L);
                    listeners[i] = (android.content.SharedPreferences.OnSharedPreferenceChangeListener)get(preview,"appearanceListener");
                    appearance.preferences.registerOnSharedPreferenceChangeListener(listeners[i]);
                }
                appearance.setColour(BirdEyeAppearance.GREEN);
            });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (BirdEyePreview preview : previews) {
                    assertEquals(37,get(preview,"revision"));assertEquals(123L,get(preview,"displayedAt"));
                    BirdEyeVehicleOverlay overlay = (BirdEyeVehicleOverlay)get(preview,"vehicleOverlay");
                    Bitmap image=Bitmap.createBitmap(240,600,Bitmap.Config.ARGB_8888);
                    assertTrue(overlay.draw(new Canvas(image),new RectF(30,40,210,560)));
                    int hood=image.getPixel(120,76);
                    assertTrue(Color.green(hood)>Color.red(hood)+5);image.recycle();
                }
            });
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                for (int i=0; i<previews.length; i++) if (previews[i]!=null) {
                    set(previews[i],"running",false);
                    if (listeners[i]!=null) appearance.preferences.unregisterOnSharedPreferenceChangeListener(listeners[i]);
                }
                if (previous==null) appearance.preferences.edit().remove("vehicle_colour").commit();
                else appearance.setColour(previous);
                if (previousModel==null) appearance.preferences.edit().remove("vehicle_model").commit();
                else appearance.setModel(previousModel);
            });
        }
    }
    private static Object get(Object owner,String name) {
        try { java.lang.reflect.Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void set(Object owner,String name,Object value) {
        try { java.lang.reflect.Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.set(owner,value); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
}
