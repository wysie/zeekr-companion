package com.kooo.evcam.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Public app files on one selected volume. Resolving paths never moves files. */
public final class StorageLayout {
    public static final String ROOT = "ZeekrShortcutApp";
    public static final String VIDEOS = "Videos";
    public static final String PHOTOS = "Photos";
    public static final String LOGS = "Logs";
    public static final String DEVELOPER = "Developer";

    public static File developerDirectory(File volume) {
        return volume == null ? null : new File(new File(volume, ROOT), DEVELOPER);
    }

    public enum Kind { VIDEO, PHOTO, LOG }

    private StorageLayout() { }

    public static File directory(File volume, Kind kind) {
        return volume == null ? null : new File(new File(volume, ROOT), name(kind));
    }

    public static File legacyDirectory(File volume, Kind kind) {
        if (volume == null) return null;
        return new File(new File(volume, kind == Kind.LOG ? "Download" : "DCIM"),
                kind == Kind.VIDEO ? "EVCam_Video" : kind == Kind.PHOTO ? "EVCam_Photo" : "EVCam_Log");
    }

    /** Existing directories only; never creates a legacy folder during discovery. */
    public static List<File> readDirectories(File volume, Kind kind) {
        List<File> dirs = new ArrayList<>();
        if (volume != null) {
            File current = directory(volume, kind);
            File legacy = legacyDirectory(volume, kind);
            if (current.isDirectory()) dirs.add(current);
            if (legacy.isDirectory()) dirs.add(legacy);
        }
        return dirs;
    }

    /** For cleanup on an actual recording volume; relay/private directories stay isolated. */
    public static List<File> videoDirectoriesFor(File dir) {
        if (dir == null) return Collections.emptyList();
        File parent = dir.getParentFile();
        if (parent != null && ((VIDEOS.equals(dir.getName()) && ROOT.equals(parent.getName()))
                || ("EVCam_Video".equals(dir.getName()) && "DCIM".equals(parent.getName())))) {
            List<File> dirs = readDirectories(parent.getParentFile(), Kind.VIDEO);
            if (!dirs.contains(dir)) dirs.add(0, dir);
            return dirs;
        }
        return Collections.singletonList(dir);
    }

    /** Legacy counterpart only for a new-layout Videos directory. */
    public static File legacyVideoDirectoryFor(File current) {
        File parent = current == null ? null : current.getParentFile();
        return parent != null && VIDEOS.equals(current.getName()) && ROOT.equals(parent.getName())
                ? legacyDirectory(parent.getParentFile(), Kind.VIDEO) : null;
    }

    private static String name(Kind kind) {
        return kind == Kind.VIDEO ? VIDEOS : kind == Kind.PHOTO ? PHOTOS : LOGS;
    }
}
