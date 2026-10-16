package com.kooo.evcam.zeekr;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Local-only reproduction from an original, uncorrected four-lane composite.
 * Uses estimated defaults, never fits or claims to recover factory calibration.
 * Input images and generated results must not be committed without owner consent.
 * After :app:compileDebugJavaWithJavac:
 * javac -cp app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes -d /tmp/bird-eye tools/BirdEyeRawFixture.java
 * java -Djava.awt.headless=true -cp /tmp/bird-eye:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes com.kooo.evcam.zeekr.BirdEyeRawFixture INPUT OUTPUT.png
 */
public final class BirdEyeRawFixture {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Supply raw composite and output PNG paths");
        BufferedImage raw = ImageIO.read(new File(args[0]));
        if (raw == null) throw new IllegalArgumentException("Input is not a readable image");
        // This tool explicitly expects a vertical original, not an ordinary photo or phone screenshot.
        if (raw.getWidth() != 1280 || raw.getHeight() != 5140)
            throw new IllegalArgumentException("Supply the original 1280x5140 four-camera composite");
        // Exact lane boundaries from the owner's exported raw geometry note.
        CompositeStreamGeometry.Lane[] lanes = new CompositeStreamGeometry.Lane[4];
        for (int i = 0; i < 4; i++) lanes[i] = new CompositeStreamGeometry.Lane(
                i, 0, 4 + i * 1284, 1280, 1280, 1280, 5140);
        CompositeStreamGeometry.Plan plan = new CompositeStreamGeometry.Plan(
                CompositeStreamGeometry.Stacking.VERTICAL, 1280, 5140, 1280, 4, true, lanes, "raw export geometry");
        BirdEyeProjection.Camera[] cameras = {
                camera(0, 0, 2.6, 0), camera(1, 0, -2.6, 180),
                camera(2, -1, 0, 270), camera(3, 1, 0, 90)};
        BirdEyeProjection.Calibration calibration = new BirdEyeProjection.Calibration(5.2, 2, 2, cameras);
        int width = 640, height = 960;
        int[] source = raw.getRGB(0, 0, raw.getWidth(), raw.getHeight(), null, 0, raw.getWidth());
        int[] output = new int[width * height];
        BirdEyeProjection.Map map = new BirdEyeProjection.Map(calibration, plan, raw.getWidth(), raw.getHeight(), width, height);
        long start = System.nanoTime();
        map.render(source, output);
        System.out.printf("Render %.1f ms (desktop only)%n", (System.nanoTime() - start) / 1e6);
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, width, height, output, 0, width);
        Graphics2D g = result.createGraphics();
        g.setColor(Color.WHITE);
        g.drawString("OFFLINE / ESTIMATED GEOMETRY", 14, height - 16);
        g.dispose();
        ImageIO.write(result, "PNG", new File(args[1]));
    }
    private static BirdEyeProjection.Camera camera(int lane, double x, double y, double yaw) {
        return new BirdEyeProjection.Camera(lane, x, y, 1, yaw, 35, 0, 190, .5, .5, false);
    }
}
