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
    @Test public void slowUnlinkReservesOnlyItsOwnPathAndNeverBlocksOtherWriters() throws Exception {
        File root = java.nio.file.Files.createTempDirectory("active-footage-delete-").toFile();
        File actual = new File(root, "old.mp4"); assertTrue(actual.createNewFile());
        CountDownLatch entered = new CountDownLatch(1), finish = new CountDownLatch(1);
        File slow = new File(actual.getPath()) {
            @Override public boolean delete() {
                entered.countDown();
                try { if (!finish.await(5, TimeUnit.SECONDS)) throw new AssertionError("Unlink not released"); }
                catch (InterruptedException e) { throw new AssertionError(e); }
                return super.delete();
            }
        };
        java.util.concurrent.ExecutorService tasks = java.util.concurrent.Executors.newFixedThreadPool(2);
        ActiveFootage.Lease oldWriter = ActiveFootage.acquire(new File(root, "previous.mp4"));
        try {
            java.util.concurrent.Future<ActiveFootage.DeleteResult> deletion = tasks.submit(() -> ActiveFootage.deleteClosed(slow));
            assertTrue(entered.await(2, TimeUnit.SECONDS));
            tasks.submit(() -> {
                oldWriter.close();
                try (ActiveFootage.Lease next = ActiveFootage.acquire(new File(root, "next.mp4"))) {
                    assertTrue(ActiveFootage.contains(new File(root, "next.mp4")));
                }
            }).get(1, TimeUnit.SECONDS);
            assertEquals(ActiveFootage.DeleteResult.BUSY, ActiveFootage.deleteClosed(actual));
            try {
                ActiveFootage.acquire(actual).close();
                fail("Opening a reserved unlink path must be refused");
            } catch (IllegalStateException expected) { }
            finish.countDown();
            assertEquals(ActiveFootage.DeleteResult.DELETED, deletion.get(2, TimeUnit.SECONDS));
            try (ActiveFootage.Lease next = ActiveFootage.acquire(actual)) {
                assertTrue("Reservation clears after unlink", ActiveFootage.contains(actual));
            }
        } finally {
            finish.countDown(); tasks.shutdownNow(); tasks.awaitTermination(5, TimeUnit.SECONDS);
            oldWriter.close(); actual.delete(); root.delete();
        }
    }

    @Test public void slowAliasResolutionDoesNotHoldTheRegistryMonitor() throws Exception {
        CountDownLatch resolving = new CountDownLatch(1), finish = new CountDownLatch(1);
        File slow = new File("/tmp/slow-alias-resolution.mp4") {
            @Override public String getCanonicalPath() throws java.io.IOException {
                resolving.countDown();
                try { if (!finish.await(5, TimeUnit.SECONDS)) throw new AssertionError("Resolution not released"); }
                catch (InterruptedException e) { throw new AssertionError(e); }
                return super.getCanonicalPath();
            }
        };
        java.util.concurrent.ExecutorService tasks = java.util.concurrent.Executors.newFixedThreadPool(2);
        ActiveFootage.Lease writer = ActiveFootage.acquire(new File("/tmp/independent-writer.mp4"));
        try {
            java.util.concurrent.Future<Boolean> lookup = tasks.submit(() -> ActiveFootage.contains(slow));
            assertTrue(resolving.await(2, TimeUnit.SECONDS));
            tasks.submit(writer::close).get(1, TimeUnit.SECONDS);
            finish.countDown(); assertFalse(lookup.get(2, TimeUnit.SECONDS));
        } finally {
            finish.countDown(); tasks.shutdownNow(); tasks.awaitTermination(5, TimeUnit.SECONDS); writer.close();
        }
    }

    @Test public void failedOrThrowingDeleteReleasesItsReservation() {
        for (boolean throwing : new boolean[]{false, true}) {
            File file = new File("/tmp/failed-unlink-" + throwing + ".mp4") {
                @Override public boolean delete() {
                    if (throwing) throw new SecurityException("Injected failure");
                    return false;
                }
            };
            try { assertEquals(ActiveFootage.DeleteResult.FAILED, ActiveFootage.deleteClosed(file)); }
            catch (SecurityException expected) { assertTrue(throwing); }
            try (ActiveFootage.Lease writer = ActiveFootage.acquire(file)) { assertTrue(ActiveFootage.contains(file)); }
        }
    }
}
