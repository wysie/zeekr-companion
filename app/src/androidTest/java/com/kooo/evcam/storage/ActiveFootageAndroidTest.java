package com.kooo.evcam.storage;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.camera.StorageGuard;
import com.kooo.evcam.ui.MainPageControlsAndroidTest;
import org.junit.Test;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class ActiveFootageAndroidTest extends MainPageControlsAndroidTest {
    @Test public void actualCleanupPreservesAnOpenOlderNamedSegmentEvenWithFootageLocksDisabled() throws Exception {
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        AppConfig config = new AppConfig(app); config.setVideoStorageLimitGb(2); config.setFootageLockEnabled(false);
        File dir = Files.createTempDirectory(app.getCacheDir().toPath(), "active-footage-").toFile();
        File open = new File(dir, "20261020_100000_surround.mp4"), future = new File(dir, "20261021_100000_surround.mp4");
        long gb = 1024L * 1024 * 1024;
        try {
            try (ActiveFootage.Lease lease = ActiveFootage.acquire(open);
                 RandomAccessFile writer = new RandomAccessFile(open, "rw");
                 RandomAccessFile old = new RandomAccessFile(future, "rw")) {
                writer.setLength(gb); old.setLength(gb);
                StorageGuard.enforce(app, dir);
                assertTrue("Current FD must remain linked despite its older wall-clock name", open.isFile());
                writer.seek(0); writer.writeInt(42);
            }
            StorageGuard.enforce(app, dir);
            assertFalse("Closed older segment becomes eligible normally", open.exists());
            assertTrue(future.isFile());
        } finally {
            Files.deleteIfExists(open.toPath()); Files.deleteIfExists(future.toPath()); Files.deleteIfExists(dir.toPath());
        }
    }
}
