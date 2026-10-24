package com.kooo.evcam.update;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class UpdateDownloadsTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private static final byte[] ABC = {97, 98, 99};
    private static final String DIGEST = "sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

    @Test public void replacementCannotUnlinkOpenOldStreamAndOldCleanupCannotDeleteNewApk() throws Exception {
        UpdateDownloads downloads = new UpdateDownloads();
        File dir = temporary.newFolder();
        UpdateDownloads.Job first = downloads.begin(dir);
        try (FileOutputStream blockedWorker = new FileOutputStream(first.staging)) {
            blockedWorker.write('a');
            UpdateDownloads.Job second = downloads.begin(dir);
            assertTrue(first.staging.exists());
            assertNotEquals(first.staging, second.staging);
            Files.write(second.staging.toPath(), ABC);
            assertTrue(downloads.publish(second, DIGEST, 3));
            blockedWorker.write(new byte[]{98, 99});
            blockedWorker.flush();
            assertFalse(downloads.publish(first, DIGEST, 3));
            downloads.discard(first);
            downloads.discard(first); // Late or repeated failure cleanup stays local.
            assertTrue(downloads.isCurrent(second));
            assertArrayEquals(ABC, Files.readAllBytes(second.apk.toPath()));
            downloads.retainForInstaller(second);
            assertTrue(second.apk.exists());
        }
    }

    @Test public void supersededPublishedJobCannotInstallOrCancelReplacement() throws Exception {
        UpdateDownloads downloads = new UpdateDownloads();
        File dir = temporary.newFolder();
        UpdateDownloads.Job first = downloads.begin(dir);
        Files.write(first.staging.toPath(), ABC);
        assertTrue(downloads.publish(first, DIGEST, 3));
        UpdateDownloads.Job second = downloads.begin(dir);
        downloads.cancel(first);
        assertTrue(downloads.isCurrent(second));
        assertThrows(IllegalStateException.class, () -> downloads.retainForInstaller(first));
        downloads.discard(first);
        assertTrue(second.staging.exists());
        downloads.discard(second);
    }

    @Test public void publicationVerifiesActualFileAndCancelledJobNeverPublishes() throws Exception {
        UpdateDownloads downloads = new UpdateDownloads();
        UpdateDownloads.Job job = downloads.begin(temporary.newFolder());
        Files.write(job.staging.toPath(), new byte[]{97, 98, 100});
        assertThrows(IOException.class, () -> downloads.publish(job, DIGEST, 3));
        assertFalse(job.apk.exists());
        Files.write(job.staging.toPath(), ABC);
        downloads.cancel(job);
        assertFalse(downloads.publish(job, DIGEST, 3));
        downloads.discard(job);
        assertFalse(job.staging.exists());
    }

    @Test public void newRequestCleansFinishedApkButPreservesStillRunningJobs() throws Exception {
        UpdateDownloads downloads = new UpdateDownloads();
        File dir = temporary.newFolder();
        UpdateDownloads.Job first = downloads.begin(dir);
        Files.write(first.staging.toPath(), ABC);
        assertTrue(downloads.publish(first, DIGEST, 3));
        downloads.retainForInstaller(first);
        UpdateDownloads.Job second = downloads.begin(dir);
        assertFalse(first.apk.exists());
        assertTrue(second.staging.exists());
        downloads.discard(second);
    }
}
