package com.kooo.evcam.share;
import org.junit.Test;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class BoundedShareRunnerTest {
    @Test public void fifthSlowConnectionCannotCreateMoreThreadsOrQueueWork() throws Exception {
        ThreadPoolExecutor pool = BoundedShareRunner.newPool();
        CountDownLatch started = new CountDownLatch(4), release = new CountDownLatch(1);
        try {
            for (int i = 0; i < 4; i++) pool.execute(() -> {
                started.countDown();
                try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            assertTrue(started.await(3, TimeUnit.SECONDS));
            assertThrows(RejectedExecutionException.class, () -> pool.execute(() -> fail("Should reject excess client")));
            assertEquals(4, pool.getPoolSize()); assertTrue(pool.getQueue().isEmpty());
        } finally { release.countDown(); pool.shutdownNow(); }
        assertTrue(pool.awaitTermination(3, TimeUnit.SECONDS));
    }
    @Test public void filenameCannotInjectHeadersOrEscapeItsQuotes() {
        assertEquals("badInjected.mp4", FileShareServer.headerFilename("bad\r\n\"\\Injected.mp4"));
        assertEquals("download", FileShareServer.headerFilename("\n"));
    }
}
