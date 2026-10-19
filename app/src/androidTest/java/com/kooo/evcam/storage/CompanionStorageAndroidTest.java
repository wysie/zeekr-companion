package com.kooo.evcam.storage;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import androidx.core.content.FileProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.ui.MainPageControlsAndroidTest;
import org.junit.Test;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

/** Emulator-only shared-file compatibility for both app-folder names and the oldest layout. */
public class CompanionStorageAndroidTest extends MainPageControlsAndroidTest {
    @Test public void oldAndNewShareUrisReadTheCorrectSameNamedFiles() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File volume = Environment.getExternalStorageDirectory();
        List<File> created = new ArrayList<>();
        String filename = "companion-storage-check-" + System.nanoTime() + ".txt";
        try {
            for (StorageLayout.Kind kind : StorageLayout.Kind.values()) {
                File[] dirs = {StorageLayout.directory(volume, kind),
                        StorageLayout.previousDirectory(volume, kind), StorageLayout.legacyDirectory(volume, kind)};
                int index = 0;
                for (File dir : dirs) {
                    assertTrue(dir.isDirectory() || dir.mkdirs());
                    File file = new File(dir, filename); created.add(file);
                    int expected = 10 * kind.ordinal() + ++index;
                    Files.write(file.toPath(), new byte[]{(byte) expected});
                    Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
                    if (index == 1) assertTrue(uri.getPath().startsWith("/companion_"));
                    if (index == 2) assertTrue("Old URI aliases remain valid", uri.getPath().startsWith("/app_"));
                    try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                        assertNotNull(in); assertEquals(expected, in.read()); assertEquals(-1, in.read());
                    }
                }
            }
        } finally {
            for (File file : created) assertTrue("Remove only this test's own shared file", file.delete());
        }
    }
}
