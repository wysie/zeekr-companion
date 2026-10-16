package com.kooo.evcam.zeekr;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Exercise Android's real four-point Canvas transforms with an encoded camera strip. */
@RunWith(AndroidJUnit4.class)
public class SideViewRenderingAndroidTest {
    private static final int SIZE = 512;
    private static final int STRIP_HEIGHT = 2056;
    private static final int[] TOPS = {2, 516, 1030, 1544};

    @Test public void rolledSquareViewsKeepLaneAndFinalMirroring() {
        check(800, 800, LaneCycle.LEFT, 30, false);
        check(800, 800, LaneCycle.RIGHT, -30, true);
    }

    @Test public void wideAndTallWindowsKeepCoverageAtHighRearAim() {
        check(900, 300, LaneCycle.LEFT, 30, true);
        check(300, 900, LaneCycle.RIGHT, -30, false);
    }

    private void check(int width, int height, int lane, int roll, boolean mirrored) {
        Bitmap source = Bitmap.createBitmap(SIZE, STRIP_HEIGHT, Bitmap.Config.ARGB_8888);
        Bitmap target = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        try {
            // A different colour in every other lane reveals accidental strip leakage.
            source.eraseColor(Color.MAGENTA);
            int[] row = new int[SIZE];
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    row[x] = Color.rgb(Math.round(x * 255f / (SIZE - 1)),
                            Math.round(y * 255f / (SIZE - 1)), 127);
                }
                source.setPixels(row, 0, SIZE, 0, TOPS[lane] + y, SIZE, 1);
            }
            SideViewFraming framing = new SideViewFraming();
            framing.configure(94, 80, 15, lane, (float) width / height, roll);
            FisheyeMesh mesh = new FisheyeMesh();
            mesh.prepare(SideViewFraming.meshDivisions(width, height),
                    0, (float) TOPS[lane] / STRIP_HEIGHT * height,
                    width, (float) SIZE / STRIP_HEIGHT * height, framing::sourcePoint);
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            Canvas canvas = new Canvas(target);
            int save = SideViewRotation.begin(canvas, 0, width, height, mirrored);
            int drawn = mesh.draw(canvas, 0, 0, width, height,
                    c -> c.drawBitmap(source, null, new RectF(0, 0, width, height), paint));
            canvas.restoreToCount(save);
            assertEquals(mesh.divisions() * mesh.divisions(), drawn);

            int[] pixels = new int[width * height];
            target.getPixels(pixels, 0, width, 0, 0, width, height);
            for (int color : pixels) {
                assertEquals("No uncovered tile or clipped edge", 255, Color.alpha(color));
                assertEquals("Sampling must stay in the selected camera", 127, Color.blue(color));
            }
            float[] point = new float[2];
            for (int y = 7; y < height; y += 31) {
                for (int x = 7; x < width; x += 31) {
                    float u = (x + .5f) / width;
                    framing.sourcePoint(mirrored ? 1 - u : u, (y + .5f) / height, point);
                    int color = target.getPixel(x, y);
                    float red = (point[0] * SIZE - .5f) / (SIZE - 1) * 255;
                    float green = (point[1] * SIZE - .5f) / (SIZE - 1) * 255;
                    assertTrue("Roll/flip must match the projected ray (red)", Math.abs(Color.red(color) - red) < 3);
                    assertTrue("Roll/flip must match the projected ray (green)", Math.abs(Color.green(color) - green) < 3);
                }
            }
        } finally {
            source.recycle();
            target.recycle();
        }
    }
}
