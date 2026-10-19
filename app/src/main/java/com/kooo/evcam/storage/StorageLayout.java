package com.kooo.evcam.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Public app files on one selected volume. Resolving paths never moves files. */
public final class StorageLayout {
    public static final String ROOT = "ZeekrCompanion";
    public static final String PREVIOUS_ROOT = "ZeekrShortcutApp";
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

    public static File previousDirectory(File volume, Kind kind) {
        return volume == null ? null : new File(new File(volume, PREVIOUS_ROOT), name(kind));
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
            File previous = previousDirectory(volume, kind);
            File legacy = legacyDirectory(volume, kind);
            if (current.isDirectory()) dirs.add(current);
            if (previous.isDirectory()) dirs.add(previous);
            if (legacy.isDirectory()) dirs.add(legacy);
        }
        return dirs;
    }

    /** For cleanup on an actual recording volume; relay/private directories stay isolated. */
    public static List<File> videoDirectoriesFor(File dir) {
        if (dir == null) return Collections.emptyList();
        File parent = dir.getParentFile();
        if (parent != null && ((VIDEOS.equals(dir.getName()) && isAppRoot(parent))
                || ("EVCam_Video".equals(dir.getName()) && "DCIM".equals(parent.getName())))) {
            List<File> dirs = readDirectories(parent.getParentFile(), Kind.VIDEO);
            if (!dirs.contains(dir)) dirs.add(0, dir);
            return dirs;
        }
        return Collections.singletonList(dir);
    }

    /** Older metadata a queued relay clip may need before publication; same volume only. */
    public static List<File> priorVideoDirectoriesFor(File current) {
        File parent = current == null ? null : current.getParentFile();
        if (parent == null || !VIDEOS.equals(current.getName()) || !isAppRoot(parent))
            return Collections.emptyList();
        File volume = parent.getParentFile();
        List<File> dirs = new ArrayList<>();
        File previous = previousDirectory(volume, Kind.VIDEO);
        if (ROOT.equals(parent.getName()) && previous != null && previous.isDirectory()) dirs.add(previous);
        File legacy = legacyDirectory(volume, Kind.VIDEO);
        if (legacy != null && legacy.isDirectory()) dirs.add(legacy);
        return dirs;
    }

    public static List<File> readDeveloperDirectories(File volume) {
        List<File> dirs = new ArrayList<>();
        if (volume != null) {
            File current = developerDirectory(volume);
            File previous = new File(new File(volume, PREVIOUS_ROOT), DEVELOPER);
            if (current.isDirectory()) dirs.add(current);
            if (previous.isDirectory()) dirs.add(previous);
        }
        return dirs;
    }

    /** Preserve recognized relative paths when archiving; never combine equal-name layouts. */
    public static File archiveDirectoryOn(File source, File target, File dir) {
        if (source == null || target == null || dir == null) return null;
        for (Kind kind : Kind.values()) {
            if (dir.equals(directory(source, kind))) return directory(target, kind);
            if (dir.equals(previousDirectory(source, kind))) return previousDirectory(target, kind);
            if (dir.equals(legacyDirectory(source, kind))) return legacyDirectory(target, kind);
        }
        if (dir.equals(developerDirectory(source))) return developerDirectory(target);
        File previous = new File(new File(source, PREVIOUS_ROOT), DEVELOPER);
        return dir.equals(previous) ? new File(new File(target, PREVIOUS_ROOT), DEVELOPER) : null;
    }

    private static boolean isAppRoot(File dir) {
        return ROOT.equals(dir.getName()) || PREVIOUS_ROOT.equals(dir.getName());
    }

    private static String name(Kind kind) {
        return kind == Kind.VIDEO ? VIDEOS : kind == Kind.PHOTO ? PHOTOS : LOGS;
    }
}
