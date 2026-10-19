package com.kooo.evcam.zeekr;

import java.nio.ByteBuffer;

/** GLES2-compatible 16-bit UV lookup textures and explicit lane/coverage metadata.
 * Built on the worker using the CPU projection, so Manual/009 geometry stays shared.
 */
final class BirdEyeGpuMap {
    final int width, height, sourceWidth, sourceHeight;
    final ByteBuffer first, second, metadata;
    final float[] lanes = new float[16];
    BirdEyeGpuMap(int width, int height, int sourceWidth, int sourceHeight, CompositeStreamGeometry.Plan plan) {
        this.width = width; this.height = height; this.sourceWidth = sourceWidth; this.sourceHeight = sourceHeight;
        first = ByteBuffer.allocateDirect(width * height * 4);
        second = ByteBuffer.allocateDirect(width * height * 4);
        metadata = ByteBuffer.allocateDirect(width * height * 4);
        for (int i = 0; i < 4; i++) {
            CompositeStreamGeometry.Lane lane = plan.lane(i);
            lanes[i * 4] = lane.u0; lanes[i * 4 + 1] = lane.v0;
            lanes[i * 4 + 2] = lane.u1; lanes[i * 4 + 3] = lane.v1;
        }
    }
    void put(int index, int a, int af, int b, int bf, int mix) {
        uv(first, index, a, af); uv(second, index, b, bf);
        int at = index * 4;
        metadata.put(at, (byte) (a < 0 || b < 0 ? 0 : mix));
        metadata.put(at + 1, (byte) lane(a)); metadata.put(at + 2, (byte) lane(b));
        metadata.put(at + 3, (byte) 255);
    }
    private int lane(int address) {
        if (address < 0) return 0;
        int x = address % sourceWidth, y = address / sourceWidth;
        for (int i = 0; i < 4; i++) if (x >= Math.ceil(lanes[i * 4] * sourceWidth)
                && x < Math.ceil(lanes[i * 4 + 2] * sourceWidth)
                && y >= Math.ceil(lanes[i * 4 + 1] * sourceHeight)
                && y < Math.ceil(lanes[i * 4 + 3] * sourceHeight)) return i + 1;
        throw new IllegalArgumentException("GPU lookup outside camera lane");
    }
    private void uv(ByteBuffer bytes, int index, int address, int fraction) {
        double u = address < 0 ? 0 : (address % sourceWidth + (fraction & 255) / 256.0 + .5) / sourceWidth;
        double v = address < 0 ? 0 : (address / sourceWidth + (fraction >>> 8) / 256.0 + .5) / sourceHeight;
        int x = (int) Math.round(u * 65535), y = (int) Math.round(v * 65535), at = index * 4;
        bytes.put(at, (byte) (x >>> 8)); bytes.put(at + 1, (byte) x);
        bytes.put(at + 2, (byte) (y >>> 8)); bytes.put(at + 3, (byte) y);
    }
}
