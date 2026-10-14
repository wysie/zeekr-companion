package com.kooo.evcam.playback;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ClipExportFilesTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test public void exportsSkipSavedAndInProgressClips() throws Exception {
        File dir = temp.newFolder();
        Files.write(new File(dir, "clip.mp4").toPath(), new byte[]{1});
        Files.write(new File(dir, "clip_2.mp4.part").toPath(), new byte[]{2});
        ClipExportFiles.Reservation a = ClipExportFiles.reserve(dir, "clip");
        ClipExportFiles.Reservation b = ClipExportFiles.reserve(dir, "clip");
        assertEquals("clip_3.mp4", a.output.getName());
        assertEquals("clip_4.mp4", b.output.getName());
        assertArrayEquals(new byte[]{2}, Files.readAllBytes(new File(dir, "clip_2.mp4.part").toPath()));
    }

    @Test public void publishingCannotReplaceAFileCreatedAfterReservation() throws Exception {
        ClipExportFiles.Reservation r = ClipExportFiles.reserve(temp.newFolder(), "clip");
        Files.write(r.partial.toPath(), "new".getBytes(StandardCharsets.UTF_8));
        Files.write(r.output.toPath(), "saved".getBytes(StandardCharsets.UTF_8));
        assertNull(ClipExportFiles.publish(r));
        assertEquals("saved", new String(Files.readAllBytes(r.output.toPath()), StandardCharsets.UTF_8));
        assertFalse(r.partial.exists());
    }

    @Test public void exhaustedNamesReturnFailureWithoutOverwriting() throws Exception {
        File dir = temp.newFolder();
        for (int n = 1; n <= 100; n++) assertNotNull(ClipExportFiles.reserve(dir, "clip"));
        assertNull(ClipExportFiles.reserve(dir, "clip"));
    }
}
