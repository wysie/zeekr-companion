package com.kooo.evcam.camera;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

public class CodecOutputDrainTest {
    @Test public void normalCaptureNeverWaitsForFutureOutput() {
        List<Integer> timeouts = new ArrayList<>(), buffers = new ArrayList<>();
        int[] events = {-2, 7, 8, -1};
        AtomicInteger next = new AtomicInteger(), formats = new AtomicInteger();
        assertTrue(CodecOutputDrain.run(false, 8, 1000, () -> 0,
                timeout -> { timeouts.add(timeout); return events[next.getAndIncrement()]; },
                formats::incrementAndGet, index -> { buffers.add(index); return false; }));
        assertEquals(List.of(0, 0, 0, 0), timeouts);
        assertEquals(List.of(7, 8), buffers);
        assertEquals(1, formats.get());
    }

    @Test public void anEmptyEncoderReturnsImmediately() {
        AtomicInteger calls = new AtomicInteger();
        assertFalse(CodecOutputDrain.run(false, 8, 1000, () -> 0,
                timeout -> { assertEquals(0, timeout); calls.incrementAndGet(); return -1; },
                () -> fail("No format"), index -> { fail("No buffer"); return false; }));
        assertEquals(1, calls.get());
    }

    @Test public void aContinuouslyReadyOrRepeatingStatusCodecCannotStarveCameraCallbacks() {
        for (int event : new int[]{3, -2, -3}) {
            AtomicInteger calls = new AtomicInteger();
            CodecOutputDrain.run(false, 8, 1000, () -> 0,
                    timeout -> { assertEquals(0, timeout); calls.incrementAndGet(); return event; },
                    () -> {}, index -> false);
            assertEquals(8, calls.get());
        }
    }

    @Test public void eosWaitsForDelayedOutputAndStopsOnEndMarker() {
        AtomicInteger calls = new AtomicInteger();
        List<Integer> consumed = new ArrayList<>();
        int[] events = {-1, 4, -1, 5};
        assertTrue(CodecOutputDrain.run(true, 1, 1000, () -> 0,
                timeout -> { assertEquals(10_000, timeout); return events[calls.getAndIncrement()]; },
                () -> {}, index -> { consumed.add(index); return index == 5; }));
        assertEquals(List.of(4, 5), consumed);
        assertEquals(4, calls.get());
    }

    @Test public void missingEndMarkerHasAFiniteDeadline() {
        AtomicLong clock = new AtomicLong();
        AtomicInteger calls = new AtomicInteger();
        assertFalse(CodecOutputDrain.run(true, 8, 30, clock::get,
                timeout -> { clock.addAndGet(timeout / 1000); calls.incrementAndGet(); return -1; },
                () -> {}, index -> false));
        assertEquals(3, calls.get());
        assertEquals(30, clock.get());
    }
}
