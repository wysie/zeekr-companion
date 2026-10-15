package com.kooo.evcam.developer;

import org.junit.Test;
import java.nio.ByteBuffer;
import static org.junit.Assert.*;

public class ProbeYuvTest {
    @Test public void paddedPlanesWithOffsetAndInterleavedChromaRetainCorrectColors() {
        ByteBuffer y = ByteBuffer.wrap(new byte[]{99, 1,2,3,4,0,0, 5,6,7,8}); y.position(1);
        ByteBuffer u = ByteBuffer.wrap(new byte[]{99, 10,0,11}); u.position(1);
        ByteBuffer v = ByteBuffer.wrap(new byte[]{99, 20,0,21}); v.position(1);
        assertArrayEquals(new byte[]{1,2,3,4,5,6,7,8,20,10,21,11}, ProbeYuv.nv21(4,2,
                new ByteBuffer[]{y,u,v}, new int[]{6,4,4}, new int[]{1,2,2}));
        assertEquals(1,y.position()); assertEquals(1,u.position()); assertEquals(1,v.position());
    }
    @Test(expected=IllegalArgumentException.class) public void refusesOddGeometry() {
        ProbeYuv.nv21(3,2, new ByteBuffer[3], new int[3], new int[3]);
    }
}
