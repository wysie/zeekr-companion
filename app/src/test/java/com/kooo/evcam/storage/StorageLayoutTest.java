package com.kooo.evcam.storage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class StorageLayoutTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void newPublicFilesShareOneRoot() {
        File root = temporary.getRoot();
        assertEquals(new File(root, "ZeekrShortcutApp/Videos"), StorageLayout.directory(root, StorageLayout.Kind.VIDEO));
        assertEquals(new File(root, "ZeekrShortcutApp/Photos"), StorageLayout.directory(root, StorageLayout.Kind.PHOTO));
        assertEquals(new File(root, "ZeekrShortcutApp/Logs"), StorageLayout.directory(root, StorageLayout.Kind.LOG));
        assertEquals(new File(root, "ZeekrShortcutApp/Developer"), StorageLayout.developerDirectory(root));
        assertNull(StorageLayout.developerDirectory(null));
        assertFalse(new File(root, "ZeekrShortcutApp").exists());
        assertNull(StorageLayout.directory(null, StorageLayout.Kind.VIDEO));
        assertTrue(StorageLayout.readDirectories(null, StorageLayout.Kind.VIDEO).isEmpty());
    }

    @Test public void discoveryNeverCreatesOrMovesLegacyMediaOrLocks() throws Exception {
        File root = temporary.getRoot();
        File old = StorageLayout.legacyDirectory(root, StorageLayout.Kind.VIDEO);
        assertTrue(old.mkdirs());
        File clip = new File(old, "20261015_001000_surround.mp4");
        Files.write(clip.toPath(), new byte[]{1, 2, 3});
        File locks = new File(old, "locked.txt");
        Files.write(locks.toPath(), "20261015_001000_surround.mp4\n".getBytes(StandardCharsets.UTF_8));
        assertEquals(Collections.singletonList(old), StorageLayout.readDirectories(root, StorageLayout.Kind.VIDEO));
        assertFalse(StorageLayout.directory(root, StorageLayout.Kind.VIDEO).exists());
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(clip.toPath()));
        assertEquals("20261015_001000_surround.mp4\n", new String(Files.readAllBytes(locks.toPath()), StandardCharsets.UTF_8));
    }

    @Test public void cleanupFindsBothLayoutsOnlyOnTheActualRecordingVolume() throws Exception {
        File root = temporary.newFolder("selected");
        File current = StorageLayout.directory(root, StorageLayout.Kind.VIDEO);
        File old = StorageLayout.legacyDirectory(root, StorageLayout.Kind.VIDEO);
        assertTrue(current.mkdirs());
        assertTrue(old.mkdirs());
        File other = temporary.newFolder("other");
        assertTrue(StorageLayout.directory(other, StorageLayout.Kind.VIDEO).mkdirs());
        assertEquals(Arrays.asList(current, old), StorageLayout.videoDirectoriesFor(current));
        assertEquals(Arrays.asList(current, old), StorageLayout.videoDirectoriesFor(old));
        File relay = temporary.newFolder("temp_video");
        assertEquals(Collections.singletonList(relay), StorageLayout.videoDirectoriesFor(relay));
        assertTrue(StorageLayout.videoDirectoriesFor(null).isEmpty());
    }

    @Test public void photoAndLogDiscoveryIncludesOnlyTheAppsTwoLayouts() throws Exception {
        File root = temporary.getRoot();
        assertTrue(new File(root, "DCIM/Sentry").mkdirs());
        for (StorageLayout.Kind kind : Arrays.asList(StorageLayout.Kind.PHOTO, StorageLayout.Kind.LOG)) {
            File current = StorageLayout.directory(root, kind);
            File old = StorageLayout.legacyDirectory(root, kind);
            assertTrue(current.mkdirs());
            assertTrue(old.mkdirs());
            assertEquals(Arrays.asList(current, old), StorageLayout.readDirectories(root, kind));
        }
        assertTrue(StorageLayout.readDirectories(root, StorageLayout.Kind.VIDEO).isEmpty());
    }
}
