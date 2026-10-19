package com.kooo.evcam.zeekr;

import java.nio.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.*;

public class BirdEyeGpuMapTest {
    @Test public void encodesSubpixelCoordinatesAndDistinctLaneMetadata() {
        StreamLayoutTable.setCompositeCameraId("gpu-map");
        try {
            CompositeStreamGeometry.Plan plan = CompositeStreamGeometry.analyse("gpu-map", 1280, 5140);
            BirdEyeGpuMap map = new BirdEyeGpuMap(2, 1, 1280, 5140, plan);
            map.put(0, 100 * 1280 + 123, 128 | (64 << 8), 4000 * 1280 + 800, 0, 73);
            assertEquals((123.5 + .5) / 1280, decode(map.first, 0), 1.0 / 65535);
            assertEquals((100.25 + .5) / 5140, decode(map.first, 2), 1.0 / 65535);
            assertEquals(73, map.metadata.get(0) & 255);
            assertEquals(1, map.metadata.get(1)); assertEquals(4, map.metadata.get(2));
            map.put(1, -1, 0, -1, 0, 255);
            assertEquals(0, map.metadata.get(4)); assertEquals(0, map.metadata.get(5));
            assertEquals(0, map.metadata.get(6));
        } finally { StreamLayoutTable.reset(); }
    }
    @Test public void entireLookupRetainsObservedCoverageAndSeamsInBothProjectionProfiles() {
        StreamLayoutTable.setCompositeCameraId("gpu-map");
        try {
            CompositeStreamGeometry.Plan plan = CompositeStreamGeometry.analyse("gpu-map", 1280, 5140);
            BirdEyeProjection.Calibration fitted = BirdEyeImageFit.zeekr009(new int[]{3,2,0,1}, 3);
            for (BirdEyeProjection.Calibration calibration : new BirdEyeProjection.Calibration[]{fitted,
                    new BirdEyeProjection.Calibration(fitted.length, fitted.width, fitted.margin, fitted.cameras)}) {
                BirdEyeProjection.Map cpu = new BirdEyeProjection.Map(calibration, plan, 1280, 5140, 160, 240);
                BirdEyeGpuMap gpu = cpu.gpuMap(plan);
                int[] source = new int[1280 * 5140]; java.util.Arrays.fill(source, 0xffffffff);
                int[] output = new int[160 * 240]; cpu.render(source, output);
                int covered = 0, holes = 0, seams = 0;
                for (int i = 0; i < output.length; i++) {
                    boolean observed = gpu.metadata.get(i * 4 + 1) != 0;
                    assertEquals(output[i] == 0xffffffff, observed);
                    if (observed) covered++; else holes++;
                    if ((gpu.metadata.get(i * 4) & 255) > 0) seams++;
                    for (ByteBuffer uv : new ByteBuffer[]{gpu.first, gpu.second}) {
                        assertTrue(decode(uv, i * 4) >= 0 && decode(uv, i * 4) <= 1);
                        assertTrue(decode(uv, i * 4 + 2) >= 0 && decode(uv, i * 4 + 2) <= 1);
                    }
                }
                assertTrue(covered > 1000); assertTrue(holes > 1000); assertTrue(seams > 0);
            }
        } finally { StreamLayoutTable.reset(); }
    }
    @Test public void sourceProgressAndExactPublishedOutputAreBothRequired() {
        BirdEyeGlRequest request = new BirdEyeGlRequest();
        assertFalse(request.fresh(10, 100));
        request.frame = new BirdEyeGlRequest.Frame(10, -1, 2);
        assertFalse(request.fresh(10, 100));
        request.frame = new BirdEyeGlRequest.Frame(10, 100, 2);
        assertTrue(request.fresh(10, 101)); assertFalse(request.fresh(9, 101));
        assertFalse(request.fresh(10, 600)); assertFalse(request.fresh(10, 99));
        request.failure = "shader failed"; assertFalse(request.fresh(10, 101));
        request.failure = null; request.cancelled = true; assertFalse(request.fresh(10, 101));
    }
    @Test public void acquiredFrameHistoryIsBoundedAndDoesNotRenewSourceAge() {
        BirdEyeGlRequest request = new BirdEyeGlRequest();
        for (int i = 1; i <= 9; i++) request.publish(new BirdEyeGlRequest.Frame(i, 100, 2));
        assertFalse(request.fresh(1, 110)); assertTrue(request.fresh(2, 110));
        assertTrue(request.fresh(8, 110)); assertTrue(request.fresh(9, 110));
        assertFalse(request.fresh(8, 600));
    }
    private double decode(ByteBuffer data, int at) { return ((data.get(at) & 255) * 256 + (data.get(at + 1) & 255)) / 65535.0; }
}
