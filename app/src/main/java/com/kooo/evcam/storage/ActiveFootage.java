package com.kooo.evcam.storage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** Process-local ownership of open recording files, independent of wall-clock filenames.
 * Acquire before opening/truncating; close only after the writer really releases the file.
 * A timed-out/abandoned writer keeps its lease until its own eventual close.
 */
public final class ActiveFootage {
    private static final Object GUARD = new Object();
    private static final Map<String, Integer> OPEN = new HashMap<>();
    private ActiveFootage() {}
    public static Object guard() { return GUARD; }
    private static String key(File file) {
        try { return file.getCanonicalPath(); }
        catch (java.io.IOException unavailable) { return file.getAbsoluteFile().toPath().normalize().toString(); }
    }
    public static Lease acquire(File file) {
        String path = key(file);
        synchronized (GUARD) { OPEN.put(path, OPEN.getOrDefault(path, 0) + 1); }
        return new Lease(path);
    }
    /** Call while holding guard through the decision and deletion. */
    public static boolean contains(File file) {
        synchronized (GUARD) { return OPEN.containsKey(key(file)); }
    }
    /** Recorder-owned invalid/empty-file cleanup; never unlink an uncertain or still-open writer. */
    public static boolean deleteIfClosed(File file) {
        synchronized (GUARD) { return !contains(file) && file.delete(); }
    }
    public static final class Lease implements AutoCloseable {
        private final String path;
        private boolean closed;
        private Lease(String path) { this.path = path; }
        @Override public void close() {
            synchronized (GUARD) {
                if (closed) return;
                closed = true;
                int count = OPEN.get(path);
                if (count == 1) OPEN.remove(path); else OPEN.put(path, count - 1);
            }
        }
    }
}
