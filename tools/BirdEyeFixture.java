package com.kooo.evcam.zeekr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Desktop verification scene, not vehicle footage. Uses the app's actual inverse renderer.
 * After :app:compileDebugJavaWithJavac:
 * javac -cp app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes -d /tmp/bird-eye tools/BirdEyeFixture.java
 * java -Djava.awt.headless=true -cp /tmp/bird-eye:app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes com.kooo.evcam.zeekr.BirdEyeFixture /tmp/bird-eye.png
 */
public final class BirdEyeFixture {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Supply the output PNG path");
        int sourceW = 256, sourceH = 1024, width = 320, height = 480;
        CompositeStreamGeometry.Lane[] lanes = new CompositeStreamGeometry.Lane[4];
        for (int i = 0; i < 4; i++) lanes[i] = new CompositeStreamGeometry.Lane(i, 0, i * 256, 256, 256, sourceW, sourceH);
        CompositeStreamGeometry.Plan plan = new CompositeStreamGeometry.Plan(CompositeStreamGeometry.Stacking.VERTICAL,
                sourceW, sourceH, 256, 0, false, lanes, "synthetic fixture");
        BirdEyeProjection.Camera[] cameras = {
                camera(0, 0, 2.6, 0), camera(1, 0, -2.6, 180), camera(2, -1, 0, 270), camera(3, 1, 0, 90)};
        BirdEyeProjection.Calibration calibration = new BirdEyeProjection.Calibration(5.2, 2, 2, cameras);
        int[] source = new int[sourceW * sourceH], pixels = new int[width * height];
        for (BirdEyeProjection.Camera camera : cameras) {
            double yaw = Math.toRadians(camera.yaw), pitch = Math.toRadians(camera.pitch);
            for (int row = 0; row < 256; row++) for (int col = 0; col < 256; col++) {
                double u = (col + .5) / 128 - 1, v = (row + .5) / 128 - 1;
                double radius = Math.hypot(u, v), theta = radius * Math.toRadians(95);
                double right = radius == 0 ? 0 : Math.sin(theta) * u / radius;
                double down = radius == 0 ? 0 : Math.sin(theta) * v / radius;
                double forward = Math.cos(theta);
                double rayX = right * Math.cos(yaw) - down * Math.sin(yaw) * Math.sin(pitch) + forward * Math.sin(yaw) * Math.cos(pitch);
                double rayY = -right * Math.sin(yaw) - down * Math.cos(yaw) * Math.sin(pitch) + forward * Math.cos(yaw) * Math.cos(pitch);
                double rayZ = -down * Math.cos(pitch) - forward * Math.sin(pitch);
                int colour = 0xff202b38;
                if (radius <= 1 && rayZ < -.001) {
                    double t = -camera.height / rayZ;
                    colour = ground(camera.x + t * rayX, camera.y + t * rayY);
                }
                source[(camera.lane * 256 + row) * sourceW + col] = colour;
            }
        }
        long before = System.nanoTime();
        BirdEyeProjection.Map map = new BirdEyeProjection.Map(calibration, plan, sourceW, sourceH, width, height);
        long built = System.nanoTime();
        for (int i = 0; i < 100; i++) map.render(source, pixels);
        long rendered = System.nanoTime();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, pixels, 0, width);
        Graphics2D g = image.createGraphics();
        double mpp = Math.max(6.0 / width, 9.2 / height);
        int carW = (int) (2 / mpp), carH = (int) (5.2 / mpp);
        g.setColor(new Color(0x292b30)); g.fillRoundRect((width - carW) / 2, (height - carH) / 2, carW, carH, 16, 16);
        g.setStroke(new BasicStroke(2)); g.setColor(new Color(0xdedee2));
        g.drawRoundRect((width - carW) / 2, (height - carH) / 2, carW, carH, 16, 16);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14)); g.drawString("FRONT", width / 2 - 24, (height - carH) / 2 + 35);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.setColor(Color.BLACK); g.fillRect(0, height - 36, width, 36);
        g.setColor(Color.WHITE); g.drawString("SIMULATED CALIBRATION SCENE", 54, height - 20);
        g.drawString("No 009 footage or on-car validation", 57, height - 6);
        g.dispose();
        ImageIO.write(image, "PNG", new File(args[0]));
        System.out.printf("Lookup %.1f ms; render %.2f ms/frame (desktop only)%n", (built - before) / 1e6, (rendered - built) / 1e8);
    }
    private static BirdEyeProjection.Camera camera(int lane, double x, double y, double yaw) {
        return new BirdEyeProjection.Camera(lane, x, y, 1, yaw, 35, 0, 190, .5, .5, false);
    }
    private static int ground(double x, double y) {
        if (Math.abs(x - Math.rint(x)) < .035 || Math.abs(y - Math.rint(y)) < .035) return 0xffe5e7eb;
        if (Math.hypot(x - 2, y - 3.5) < .25) return 0xffef4444;
        if (Math.hypot(x + 2, y + 3.5) < .25) return 0xff3b82f6;
        return (((int) Math.floor(x) + (int) Math.floor(y)) & 1) == 0 ? 0xff59616c : 0xff383e48;
    }
}
