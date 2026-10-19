package com.kooo.evcam.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

/**
 * 锁定清单（U 盘上的 locked.txt）的读写格式：一行一个文件名，只认本应用的文件名。
 */
public class FootageLocksTest {

    @Test
    public void readsOnlyTheAppsOwnFileNames() {
        Set<String> names = FootageLocks.parse("# header\n\n"
                + "20261003_101500_surround.mp4\n"
                + "  20261003_101500_cabinfront.mp4  \r\n"
                + "holiday.mp4\n"
                + "20261003_101501_surround.jpg\n");
        assertEquals(3, names.size());
        assertTrue(names.contains("20261003_101500_cabinfront.mp4"));
        assertTrue(names.contains("20261003_101501_surround.jpg"));
    }

    @Test
    public void writingAndReadingBackGivesTheSameNames() {
        Set<String> names = FootageLocks.parse(FootageLocks.format(Arrays.asList(
                "20261003_101600_surround.mp4", "20261003_101500_surround.mp4")));
        assertEquals(2, names.size());
        assertEquals("按时间排", "20261003_101500_surround.mp4", names.iterator().next());
    }

    @Test
    public void anEmptyOrMissingListLocksNothing() {
        assertTrue(FootageLocks.parse(null).isEmpty());
        assertTrue(FootageLocks.parse("").isEmpty());
    }
    @Test
    public void lockWritesGoBesideEachFileEvenWhenNamesMatch() {
        String name = "20261015_001000_surround.mp4";
        java.io.File current = new java.io.File("/storage/drive/ZeekrCompanion/Videos", name);
        java.io.File previous = new java.io.File("/storage/drive/ZeekrShortcutApp/Videos", name);
        java.io.File old = new java.io.File("/storage/drive/DCIM/EVCam_Video", name);
        java.util.Map<java.io.File, java.util.List<String>> groups = FootageLocks.groupFiles(
                Arrays.asList(current, previous, old, current));
        assertEquals(3, groups.size());
        assertEquals(Arrays.asList(name), groups.get(current.getParentFile()));
        assertEquals(Arrays.asList(name), groups.get(previous.getParentFile()));
        assertEquals(Arrays.asList(name), groups.get(old.getParentFile()));
    }

    @Test public void queuedRelayClipKeepsProtectionFromTheShortcutFolder() throws Exception {
        org.junit.rules.TemporaryFolder temp = new org.junit.rules.TemporaryFolder();
        temp.create();
        try {
            java.io.File current = StorageLayout.directory(temp.getRoot(), StorageLayout.Kind.VIDEO);
            java.io.File previous = StorageLayout.previousDirectory(temp.getRoot(), StorageLayout.Kind.VIDEO);
            assertTrue(current.mkdirs()); assertTrue(previous.mkdirs());
            String name = "20261019_001000_surround.mp4";
            byte[] list = (name + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            java.nio.file.Files.write(new java.io.File(previous, FootageLocks.FILE_NAME).toPath(), list);
            java.nio.file.Files.write(new java.io.File(previous, FootageLocks.AUTO_FILE_NAME).toPath(), list);
            assertTrue(FootageLocks.keepRelayProtection(new java.io.File(current, name)));
            assertTrue(FootageLocks.read(current).contains(name));
            org.junit.Assert.assertArrayEquals(list, java.nio.file.Files.readAllBytes(new java.io.File(previous, FootageLocks.FILE_NAME).toPath()));
            org.junit.Assert.assertArrayEquals(list, java.nio.file.Files.readAllBytes(new java.io.File(previous, FootageLocks.AUTO_FILE_NAME).toPath()));
            org.junit.Assert.assertFalse(new java.io.File(current, name).exists());
        } finally { temp.delete(); }
    }

    @Test
    public void queuedRelayClipKeepsPreUpgradeProtectionBeforePublication() throws Exception {
        java.nio.file.Path volume = java.nio.file.Files.createTempDirectory("relay-layout-test");
        try {
            java.io.File current = StorageLayout.directory(volume.toFile(), StorageLayout.Kind.VIDEO);
            java.io.File old = StorageLayout.legacyDirectory(volume.toFile(), StorageLayout.Kind.VIDEO);
            org.junit.Assert.assertTrue(current.mkdirs());
            org.junit.Assert.assertTrue(old.mkdirs());
            String name = "20261015_001000_surround.mp4";
            java.nio.file.Files.write(new java.io.File(old, FootageLocks.FILE_NAME).toPath(),
                    (name + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            org.junit.Assert.assertTrue(FootageLocks.keepRelayProtection(new java.io.File(current, name)));
            assertTrue(FootageLocks.read(current).contains(name));
            assertTrue(FootageLocks.read(old).contains(name));
            org.junit.Assert.assertFalse(new java.io.File(current, name).exists());
            org.junit.Assert.assertTrue(FootageLocks.keepRelayProtection(new java.io.File(current, name)));
        } finally {
            try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(volume)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }

}
