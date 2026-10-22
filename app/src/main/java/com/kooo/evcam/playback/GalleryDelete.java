package com.kooo.evcam.playback;

import android.content.Context;
import com.kooo.evcam.storage.FootageLocks;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Recheck each directory's protection under the same guard as lock edits. */
public final class GalleryDelete {
    private GalleryDelete() { }
    public static int[] files(Context context, Collection<File> files) {
        int deleted = 0, kept = 0, failed = 0;
        Set<String> visited = new HashSet<>();
        for (File file : files) {
            if (!visited.add(file.getAbsolutePath())) continue;
            synchronized (FootageLocks.guard()) {
                Set<String> protectedNames = FootageLocks.protectedNames(context, file.getParentFile());
                if (com.kooo.evcam.storage.ActiveFootage.contains(file)
                        || protectedNames == null || protectedNames.contains(file.getName())) {
                    kept++;
                } else if (file.isFile()) {
                    com.kooo.evcam.storage.ActiveFootage.DeleteResult result =
                            com.kooo.evcam.storage.ActiveFootage.deleteClosed(file);
                    if (result == com.kooo.evcam.storage.ActiveFootage.DeleteResult.DELETED) deleted++;
                    else if (result == com.kooo.evcam.storage.ActiveFootage.DeleteResult.BUSY) kept++;
                    else if (file.exists()) failed++;
                } else if (file.exists()) failed++;
            }
        }
        return new int[]{deleted, kept, failed};
    }
}
