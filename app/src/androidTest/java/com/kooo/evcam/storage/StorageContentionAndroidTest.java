package com.kooo.evcam.storage;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.camera.StorageGuard;
import com.kooo.evcam.camera.StoragePlan;
import com.kooo.evcam.playback.GalleryDelete;
import com.kooo.evcam.ui.MainPageControlsAndroidTest;
import org.junit.Test;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

/** Replay USB inventory/metadata stalls against real cleanup and late lock/open decisions. */
public class StorageContentionAndroidTest extends MainPageControlsAndroidTest {
    private Context app() { return InstrumentationRegistry.getInstrumentation().getTargetContext(); }
    private static void awaitRelease(CountDownLatch release) {
        try { assertTrue("Test I/O must be released", release.await(10, TimeUnit.SECONDS)); }
        catch (InterruptedException e) { throw new AssertionError(e); }
    }
    private static File sparse(File dir, String name) throws Exception {
        File file = new File(dir, name);
        try (RandomAccessFile out = new RandomAccessFile(file, "rw")) { out.setLength(1024L * 1024 * 1024); }
        return file;
    }
    private static void remove(File dir) throws Exception {
        File[] files = dir.listFiles();
        if (files != null) for (File file : files) Files.deleteIfExists(file.toPath());
        Files.deleteIfExists(dir.toPath());
    }

    @Test public void slowUsbInventoryDoesNotBlockWriterCloseOrNextFileAdmission() throws Exception {
        File root = Files.createTempDirectory(app().getCacheDir().toPath(), "slow-inventory-").toFile();
        CountDownLatch scanning = new CountDownLatch(1), release = new CountDownLatch(1);
        File dir = new File(root.getPath()) {
            @Override public File[] listFiles() { scanning.countDown(); awaitRelease(release); return super.listFiles(); }
        };
        ExecutorService tasks = Executors.newFixedThreadPool(2);
        File current = sparse(root, "20261021_100000_surround.mp4");
        ActiveFootage.Lease writer = ActiveFootage.acquire(current);
        try {
            Future<?> cleanup = tasks.submit(() -> StorageGuard.enforce(app(), dir));
            assertTrue(scanning.await(3, TimeUnit.SECONDS));
            tasks.submit(() -> {
                writer.close();
                try (ActiveFootage.Lease next = ActiveFootage.acquire(new File(root, "next.mp4"))) {
                    assertTrue(ActiveFootage.contains(new File(root, "next.mp4")));
                }
            }).get(1, TimeUnit.SECONDS);
            release.countDown(); cleanup.get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown(); tasks.shutdownNow(); tasks.awaitTermination(5, TimeUnit.SECONDS);
            writer.close(); remove(root);
        }
    }

    @Test public void slowPersistentLockIoDoesNotBlockRecorders() throws Exception {
        ExecutorService tasks = Executors.newSingleThreadExecutor();
        File file = new File(app().getCacheDir(), "independent-active.mp4");
        ActiveFootage.Lease current = ActiveFootage.acquire(file);
        try {
            synchronized (FootageLocks.guard()) {
                tasks.submit(() -> {
                    current.close();
                    try (ActiveFootage.Lease next = ActiveFootage.acquire(file)) {
                        assertTrue(ActiveFootage.contains(file));
                    }
                }).get(1, TimeUnit.SECONDS);
            }
        } finally { current.close(); tasks.shutdownNow(); tasks.awaitTermination(5, TimeUnit.SECONDS); }
    }

    @Test public void cleanupRechecksALockSavedAfterItsInventorySnapshot() throws Exception {
        AppConfig config = new AppConfig(app()); config.setVideoStorageLimitGb(2); config.setFootageLockEnabled(true);
        File root = Files.createTempDirectory(app().getCacheDir().toPath(), "late-lock-").toFile();
        File oldest = sparse(root, "20261020_100000_surround.mp4");
        File newest = sparse(root, "20261021_100000_surround.mp4");
        CountDownLatch scanning = new CountDownLatch(1), release = new CountDownLatch(1), saved = new CountDownLatch(1);
        AtomicBoolean locked = new AtomicBoolean();
        File dir = new File(root.getPath()) {
            @Override public File[] listFiles() { scanning.countDown(); awaitRelease(release); return new File[]{oldest, newest}; }
        };
        ExecutorService tasks = Executors.newSingleThreadExecutor();
        try {
            Future<StoragePlan.Decision> cleanup = tasks.submit(() -> StorageGuard.enforce(app(), dir));
            assertTrue(scanning.await(3, TimeUnit.SECONDS));
            FootageLocks.set(root, Collections.singleton(oldest.getName()), true, ok -> { locked.set(ok); saved.countDown(); });
            assertTrue("Inventory cannot delay saving a user lock", saved.await(3, TimeUnit.SECONDS));
            assertTrue(locked.get()); release.countDown();
            assertEquals("No pretend space after a late lock", StoragePlan.Verdict.FULL, cleanup.get(5, TimeUnit.SECONDS).verdict);
            assertTrue(oldest.isFile()); assertTrue(newest.isFile());
        } finally { release.countDown(); tasks.shutdownNow(); tasks.awaitTermination(5, TimeUnit.SECONDS); remove(root); }
    }

    @Test public void cleanupRechecksAWriterAdmittedAfterItScannedTheOlderFile() throws Exception {
        AppConfig config = new AppConfig(app()); config.setVideoStorageLimitGb(2); config.setFootageLockEnabled(false);
        File root = Files.createTempDirectory(app().getCacheDir().toPath(), "late-writer-").toFile();
        File oldest = sparse(root, "20261020_100000_surround.mp4");
        File newest = sparse(root, "20261021_100000_surround.mp4");
        CountDownLatch scanning = new CountDownLatch(1), release = new CountDownLatch(1);
        File slow = new File(newest.getPath()) {
            @Override public long length() { scanning.countDown(); awaitRelease(release); return super.length(); }
        };
        File dir = new File(root.getPath()) { @Override public File[] listFiles() { return new File[]{oldest, slow}; } };
        ExecutorService tasks = Executors.newSingleThreadExecutor();
        ActiveFootage.Lease writer = null;
        try {
            Future<StoragePlan.Decision> cleanup = tasks.submit(() -> StorageGuard.enforce(app(), dir));
            assertTrue(scanning.await(3, TimeUnit.SECONDS));
            writer = ActiveFootage.acquire(oldest); release.countDown();
            assertEquals(StoragePlan.Verdict.FULL, cleanup.get(5, TimeUnit.SECONDS).verdict);
            assertTrue(oldest.isFile());
            assertArrayEquals(new int[]{0, 1, 0}, GalleryDelete.files(app(), Collections.singleton(oldest)));
        } finally {
            release.countDown(); tasks.shutdownNow(); tasks.awaitTermination(5, TimeUnit.SECONDS);
            if (writer != null) writer.close(); remove(root);
        }
    }
    @Test public void failedUnlinkIsNotRetriedAndCleanupCanUseTheNextEligibleGroup() throws Exception {
        AppConfig config = new AppConfig(app()); config.setVideoStorageLimitGb(3); config.setFootageLockEnabled(false);
        File root = Files.createTempDirectory(app().getCacheDir().toPath(), "failed-unlink-").toFile();
        File oldest = sparse(root, "20261019_100000_surround.mp4");
        File middle = sparse(root, "20261020_100000_surround.mp4");
        File newest = sparse(root, "20261021_100000_surround.mp4");
        java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        File cannotDelete = new File(oldest.getPath()) {
            @Override public boolean delete() { attempts.incrementAndGet(); return false; }
        };
        File dir = new File(root.getPath()) {
            @Override public File[] listFiles() { return new File[]{cannotDelete, middle, newest}; }
        };
        try {
            assertEquals(StoragePlan.Verdict.DELETE, StorageGuard.enforce(app(), dir).verdict);
            assertEquals("Failed paths cannot spin in the replan loop", 1, attempts.get());
            assertTrue(oldest.isFile()); assertFalse(middle.exists()); assertTrue(newest.isFile());
        } finally { remove(root); }
    }
}
