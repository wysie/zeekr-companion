package com.kooo.evcam.playback;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import static org.junit.Assert.*;

public class PhotoStorageIdentityTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void identicalNamesInTwoFoldersRemainSeparateAndIndependentlyProtected() throws Exception {
        String name = "20261015_001000_surround.jpg";
        File current = new File(temporary.newFolder("Photos"), name);
        File old = new File(temporary.newFolder("EVCam_Photo"), name);
        Files.write(current.toPath(), new byte[]{1});
        Files.write(old.toPath(), new byte[]{2});
        PhotoGroup a = new PhotoGroup("20261015_001000");
        PhotoGroup b = new PhotoGroup("20261015_001000");
        a.addFile(current);
        b.addFile(old);
        assertNotEquals(a.storageKey(), b.storageKey());
        java.util.Set<String> protectedPaths = Collections.singleton(old.getAbsolutePath());
        assertFalse(a.isLockedPaths(protectedPaths));
        assertTrue(b.isLockedPaths(protectedPaths));
        assertEquals(1, a.deleteAllExceptPaths(protectedPaths));
        assertEquals(0, b.deleteAllExceptPaths(protectedPaths));
        assertFalse(current.exists());
        assertArrayEquals(new byte[]{2}, Files.readAllBytes(old.toPath()));
    }
}
