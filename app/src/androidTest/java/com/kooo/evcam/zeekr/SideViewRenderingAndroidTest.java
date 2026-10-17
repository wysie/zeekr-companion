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

    @Test public void unfittedViewsShowBlackCoverageWithoutLeakingOtherCameras() {
        check(880, 400, LaneCycle.LEFT, 45, false, false);
        check(880, 400, LaneCycle.RIGHT, -45, true, false);
        check(600, 600, LaneCycle.LEFT, 0, true, false);
        check(400, 600, LaneCycle.RIGHT, 45, false, false);
    }

    private void check(int width, int height, int lane, int roll, boolean mirrored) {
        check(width, height, lane, roll, mirrored, true);
    }

    private void check(int width, int height, int lane, int roll, boolean mirrored, boolean autoFit) {
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
            framing.configure(94, 80, 15, lane, (float) width / height, roll, autoFit);
            FisheyeMesh mesh = new FisheyeMesh();
            mesh.prepare(SideViewFraming.meshDivisions(width, height),
                    0, (float) TOPS[lane] / STRIP_HEIGHT * height,
                    width, (float) SIZE / STRIP_HEIGHT * height, framing::sourcePoint);
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
            Canvas canvas = new Canvas(target);
            int save = SideViewRotation.begin(canvas, 0, width, height, mirrored);
            FisheyeMesh.Painter painter = c -> c.drawBitmap(source, null, new RectF(0, 0, width, height), paint);
            int drawn = autoFit ? mesh.draw(canvas, 0, 0, width, height, painter)
                    : mesh.drawClippedToLane(canvas, 0, 0, width, height, painter);
            canvas.restoreToCount(save);
            if (autoFit) assertEquals(mesh.divisions() * mesh.divisions(), drawn);

            int[] pixels = new int[width * height];
            target.getPixels(pixels, 0, width, 0, 0, width, height);
            int black = 0;
            for (int color : pixels) {
                assertEquals("No uncovered tile or clipped edge", 255, Color.alpha(color));
                if (!autoFit && color == Color.BLACK) black++;
                else assertEquals("Sampling must stay in the selected camera", 127, Color.blue(color));
            }
            if (!autoFit) {
                assertEquals(94, framing.effectiveFovDegrees(), .001);
                assertTrue("Missing coverage must be black", black > width * height / 10);
                assertTrue("Retain the real visible picture", black < width * height * .9);
            }
            float[] point = new float[2];
            for (int y = 7; y < height; y += 31) {
                for (int x = 7; x < width; x += 31) {
                    float u = (x + .5f) / width;
                    framing.sourcePoint(mirrored ? 1 - u : u, (y + .5f) / height, point);
                    int color = target.getPixel(x, y);
                    if (!autoFit && (point[0] < .03 || point[0] > .97 || point[1] < .03 || point[1] > .97)) continue;
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
