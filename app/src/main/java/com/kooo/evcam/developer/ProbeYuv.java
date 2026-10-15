package com.kooo.evcam.developer;

import java.nio.ByteBuffer;

/** Camera2 plane packing, retaining row padding, pixel stride and buffer offsets correctly. */
final class ProbeYuv {
    private ProbeYuv() { }
    static byte[] nv21(int w, int h, ByteBuffer[] buffers, int[] rowStrides, int[] pixelStrides) {
        if ((w & 1) != 0 || (h & 1) != 0 || !ProbeSizes.bounded(w, h)) throw new IllegalArgumentException("Unsupported YUV geometry");
        byte[] bytes = new byte[w * h * 3 / 2];
        for (int p = 0; p < 3; p++) {
            ByteBuffer buffer = buffers[p]; int base = buffer.position();
            int rows = p == 0 ? h : h / 2, cols = p == 0 ? w : w / 2;
            for (int y = 0; y < rows; y++) for (int x = 0; x < cols; x++) {
                int dest = p == 0 ? y * w + x : w * h + y * w + 2 * x + (p == 1 ? 1 : 0);
                bytes[dest] = buffer.get(base + y * rowStrides[p] + x * pixelStrides[p]);
            }
        }
        return bytes;
    }
}
