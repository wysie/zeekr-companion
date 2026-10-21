package com.kooo.evcam.storage;

import org.junit.Test;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class ActiveFootageTest {
    @Test public void alternateFilesystemAliasesAndRecorderCleanupKeepTheSameOpenFile() throws Exception {
        java.nio.file.Path root = java.nio.file.Files.createTempDirectory("active-footage-alias-");
        File real = root.resolve("real").toFile(); assertTrue(real.mkdir());
        java.nio.file.Path alias = root.resolve("alias");
        java.nio.file.Files.createSymbolicLink(alias, real.toPath());
        File file = new File(real,"active.mp4"), alternate = alias.resolve(file.getName()).toFile();
        try {
            try (ActiveFootage.Lease lease = ActiveFootage.acquire(alternate)) {
                assertTrue(file.createNewFile());
                assertTrue(ActiveFootage.contains(file)); assertTrue(ActiveFootage.contains(alternate));
                assertFalse("Even invalid-file cleanup retains an uncertain writer",ActiveFootage.deleteIfClosed(file));
                assertTrue(file.isFile());
            }
            assertTrue("Actual writer close makes its invalid file removable",ActiveFootage.deleteIfClosed(alternate));
        } finally {
            java.nio.file.Files.deleteIfExists(file.toPath()); java.nio.file.Files.deleteIfExists(alias);
            java.nio.file.Files.deleteIfExists(real.toPath()); java.nio.file.Files.deleteIfExists(root);
        }
    }
    @Test public void independentWritersAndLateCloseRetainProtection() {
        File file = new File("/tmp/active-footage-test/../late.mp4");
        ActiveFootage.Lease oldWriter = ActiveFootage.acquire(file);
        ActiveFootage.Lease rescue = ActiveFootage.acquire(new File("/tmp/late.mp4"));
        try {
            assertTrue(ActiveFootage.contains(file));
            rescue.close(); rescue.close();
            assertTrue("Caller/rescue finishing must not release the abandoned writer", ActiveFootage.contains(file));
            oldWriter.close(); assertFalse(ActiveFootage.contains(file));
        } finally { oldWriter.close(); rescue.close(); }
    }
    @Test public void openingCannotRaceTheDeletionDecision() throws Exception {
        File file = new File("/tmp/active-footage-guard.mp4");
        CountDownLatch attempted = new CountDownLatch(1), acquired = new CountDownLatch(1);
        Thread opener;
        synchronized (ActiveFootage.guard()) {
            assertFalse(ActiveFootage.contains(file));
            opener = new Thread(() -> {
                attempted.countDown();
                try (ActiveFootage.Lease lease = ActiveFootage.acquire(file)) { acquired.countDown(); }
            });
            opener.start(); assertTrue(attempted.await(2, TimeUnit.SECONDS));
            assertFalse("Open admission waits for the complete delete decision", acquired.await(100, TimeUnit.MILLISECONDS));
        }
        assertTrue(acquired.await(2, TimeUnit.SECONDS)); opener.join(2000);
        assertFalse(opener.isAlive()); assertFalse(ActiveFootage.contains(file));
    }
}
