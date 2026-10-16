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
        overlay.prepare(context.getResources(), true);
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
                for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                    // Fractional Canvas bounds can antialias the intersected edge pixel.
                    if (x < Math.floor(body.left - extension) || x > Math.ceil(body.right + extension)
                            || y < Math.floor(body.top) || y > Math.ceil(body.bottom))
                        assertEquals("Camera ground outside vehicle must not change", original.getPixel(x,y), image.getPixel(x,y));
                }
                int hood = image.getPixel(width / 2, (int)(body.top + body.height() * .07f));
                int roof = image.getPixel(width / 2, (int)(body.top + body.height() * .5f));
                assertTrue("White hood faces upward", Color.red(hood) > 180);
                assertTrue("Dark roof glass", Color.red(roof) < 120);
                String theme = background == 0xffdedee2 ? "day" : "night";
                File directory = new File(context.getExternalFilesDir(null), "overlay-review");
                assertTrue(directory.isDirectory() || directory.mkdirs());
                try (FileOutputStream stream = new FileOutputStream(new File(directory, theme + "-" + width + ".png"))) {
                    assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, stream));
                }
                original.recycle(); image.recycle();
            }
        }
    }

    @Test public void unknownVehiclesRetainOutlineAndUnderbodyStaysOpaque() {
        assertFalse(BirdEyeVehicleOverlay.use009(false, false));
        assertTrue(BirdEyeVehicleOverlay.use009(true, false));
        assertTrue(BirdEyeVehicleOverlay.use009(false, true));
        BirdEyeVehicleOverlay overlay = new BirdEyeVehicleOverlay();
        Bitmap image = Bitmap.createBitmap(240, 600, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image); RectF body = new RectF(30, 40, 210, 560);
        overlay.prepare(context.getResources(), false);
        assertFalse(overlay.draw(canvas, body)); assertEquals(0, image.getPixel(120,300));
        overlay.prepare(context.getResources(), true);
        assertTrue(overlay.draw(canvas, body));
        for (int y=45;y<555;y++) for (int x=35;x<205;x++)
            assertEquals("Unknown ground must remain opaque", 255, Color.alpha(image.getPixel(x,y)));
        overlay.prepare(context.getResources(), false);
        assertFalse(overlay.draw(canvas, body));
        image.recycle();
    }
}
