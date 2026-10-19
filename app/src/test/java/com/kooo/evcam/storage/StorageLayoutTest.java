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
        assertEquals(new File(root, "ZeekrCompanion/Videos"), StorageLayout.directory(root, StorageLayout.Kind.VIDEO));
        assertEquals(new File(root, "ZeekrCompanion/Photos"), StorageLayout.directory(root, StorageLayout.Kind.PHOTO));
        assertEquals(new File(root, "ZeekrCompanion/Logs"), StorageLayout.directory(root, StorageLayout.Kind.LOG));
        assertEquals(new File(root, "ZeekrCompanion/Developer"), StorageLayout.developerDirectory(root));
        assertNull(StorageLayout.developerDirectory(null));
        assertFalse(new File(root, "ZeekrCompanion").exists());
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

    @Test public void cleanupFindsAllThreeLayoutsOnlyOnTheActualRecordingVolume() throws Exception {
        File root = temporary.newFolder("selected");
        File current = StorageLayout.directory(root, StorageLayout.Kind.VIDEO);
        File old = StorageLayout.legacyDirectory(root, StorageLayout.Kind.VIDEO);
        File previous = StorageLayout.previousDirectory(root, StorageLayout.Kind.VIDEO);
        assertTrue(current.mkdirs());
        assertTrue(previous.mkdirs());
        assertTrue(old.mkdirs());
        File other = temporary.newFolder("other");
        assertTrue(StorageLayout.directory(other, StorageLayout.Kind.VIDEO).mkdirs());
        assertEquals(Arrays.asList(current, previous, old), StorageLayout.videoDirectoriesFor(current));
        assertEquals(Arrays.asList(current, previous, old), StorageLayout.videoDirectoriesFor(previous));
        assertEquals(Arrays.asList(current, previous, old), StorageLayout.videoDirectoriesFor(old));
        File relay = temporary.newFolder("temp_video");
        assertEquals(Collections.singletonList(relay), StorageLayout.videoDirectoriesFor(relay));
        assertTrue(StorageLayout.videoDirectoriesFor(null).isEmpty());
    }

    @Test public void allThreeLayoutsKeepEqualNamesAndProtectionMetadataInPlace() throws Exception {
        File root = temporary.getRoot();
        for (StorageLayout.Kind kind : StorageLayout.Kind.values()) {
            File current = StorageLayout.directory(root, kind);
            File previous = StorageLayout.previousDirectory(root, kind);
            File legacy = StorageLayout.legacyDirectory(root, kind);
            assertEquals(new File(root, "ZeekrShortcutApp/" + current.getName()), previous);
            int value = 0;
            for (File dir : Arrays.asList(current, previous, legacy)) {
                assertTrue(dir.mkdirs());
                Files.write(new File(dir, "same-name").toPath(), new byte[]{(byte) ++value});
                Files.write(new File(dir, "locked.txt").toPath(), new byte[]{(byte) value});
                Files.write(new File(dir, "locked-auto.txt").toPath(), new byte[]{(byte) value});
            }
            assertEquals(Arrays.asList(current, previous, legacy), StorageLayout.readDirectories(root, kind));
            value = 0;
            for (File dir : StorageLayout.readDirectories(root, kind)) {
                byte[] bytes = new byte[]{(byte) ++value};
                assertArrayEquals(bytes, Files.readAllBytes(new File(dir, "same-name").toPath()));
                assertArrayEquals(bytes, Files.readAllBytes(new File(dir, "locked.txt").toPath()));
                assertArrayEquals(bytes, Files.readAllBytes(new File(dir, "locked-auto.txt").toPath()));
            }
        }
    }

    @Test public void archiveKeepsEachOldAndNewDirectorySeparateOnTheTarget() throws Exception {
        File source = temporary.newFolder("source");
        File target = temporary.newFolder("target");
        for (StorageLayout.Kind kind : StorageLayout.Kind.values()) {
            assertEquals(StorageLayout.directory(target, kind),
                    StorageLayout.archiveDirectoryOn(source, target, StorageLayout.directory(source, kind)));
            assertEquals(StorageLayout.previousDirectory(target, kind),
                    StorageLayout.archiveDirectoryOn(source, target, StorageLayout.previousDirectory(source, kind)));
            assertEquals(StorageLayout.legacyDirectory(target, kind),
                    StorageLayout.archiveDirectoryOn(source, target, StorageLayout.legacyDirectory(source, kind)));
        }
        assertEquals(StorageLayout.developerDirectory(target), StorageLayout.archiveDirectoryOn(source, target, StorageLayout.developerDirectory(source)));
        File previousDeveloper = new File(source, "ZeekrShortcutApp/Developer");
        assertEquals(new File(target, "ZeekrShortcutApp/Developer"), StorageLayout.archiveDirectoryOn(source, target, previousDeveloper));
        assertTrue(previousDeveloper.mkdirs());
        assertEquals(Collections.singletonList(previousDeveloper), StorageLayout.readDeveloperDirectories(source));
        assertFalse(StorageLayout.developerDirectory(source).exists());
        assertNull(StorageLayout.archiveDirectoryOn(source, target, new File(source, "DCIM/Sentry")));
        assertNull(StorageLayout.archiveDirectoryOn(source, target, new File(temporary.getRoot(), "Videos")));
        assertTrue(StorageLayout.readDeveloperDirectories(null).isEmpty());
    }

    @Test public void photoAndLogDiscoveryDoesNotScanFactoryFolders() throws Exception {
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
