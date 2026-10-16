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
        if (args.length < 2 || args.length > 3 || (args.length == 3 && !"--009-image-fit".equals(args[2])))
            throw new IllegalArgumentException("Supply raw composite, output PNG and optionally --009-image-fit");
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
        if (args.length == 3) calibration = BirdEyeImageFit.zeekr009(new int[]{0,1,2,3}, 2);
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
        double mpp = Math.max(6.0 / width, 9.2 / height);
        int carW = (int) Math.round(2 / mpp), carH = (int) Math.round(5.2 / mpp);
        int left = (width - carW) / 2, top = (height - carH) / 2;
        g.setColor(new Color(0x292b30)); g.fillRoundRect(left, top, carW, carH, 12, 12);
        g.setColor(new Color(0xdedee2)); g.drawRoundRect(left, top, carW, carH, 12, 12);
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 26));
        g.drawString("Front", width / 2 - 32, top + 85);
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.PLAIN, 12));
        g.setColor(Color.WHITE);
        g.drawString(args.length == 3 ? "OFFLINE / EXPERIMENTAL 009 IMAGE FIT" : "OFFLINE / ESTIMATED GEOMETRY", 14, height - 16);
        g.dispose();
        ImageIO.write(result, "PNG", new File(args[1]));
    }
    private static BirdEyeProjection.Camera camera(int lane, double x, double y, double yaw) {
        return new BirdEyeProjection.Camera(lane, x, y, 1, yaw, 35, 0, 190, .5, .5, false);
    }
}
