package com.kooo.evcam.storage;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Process-local ownership of open recording files, independent of wall-clock filenames.
 * Acquire before opening/truncating; close only after the writer really releases the file.
 * A timed-out/abandoned writer keeps its lease until its own eventual close.
 * The registry monitor never covers filesystem work or persistent lock-list edits.
 */
public final class ActiveFootage {
    private static final Object GUARD = new Object();
    private static final Map<String, Integer> OPEN = new HashMap<>();
    private static final Set<String> DELETING = new HashSet<>();
    private ActiveFootage() {}

    private static String key(File file) {
        try { return file.getCanonicalPath(); }
        catch (java.io.IOException unavailable) { return file.getAbsoluteFile().toPath().normalize().toString(); }
    }

    public static Lease acquire(File file) {
        String path = key(file);
        synchronized (GUARD) {
            // Never wait for an unbounded USB unlink, or open a file whose unlink was admitted.
            // Normal recorder/rescue names are new; a colliding caller must select another name.
            if (DELETING.contains(path)) throw new IllegalStateException("File deletion in progress: " + path);
            OPEN.put(path, OPEN.getOrDefault(path, 0) + 1);
        }
        return new Lease(path);
    }

    public static boolean contains(File file) {
        String path = key(file); // Resolving a filesystem alias can itself block: outside the monitor.
        synchronized (GUARD) { return OPEN.containsKey(path); }
    }

    public enum DeleteResult { DELETED, BUSY, FAILED }

    /** Reserve a closed path before unlinking it. Other paths' writers remain independent of USB I/O.
     * Callers must separately serialize persistent lock-list checks/edits with FootageLocks.guard().
     */
    public static DeleteResult deleteClosed(File file) {
        String path = key(file);
        synchronized (GUARD) {
            if (OPEN.containsKey(path) || !DELETING.add(path)) return DeleteResult.BUSY;
        }
        try {
            return file.delete() ? DeleteResult.DELETED : DeleteResult.FAILED;
        } finally {
            synchronized (GUARD) { DELETING.remove(path); }
        }
    }

    /** Recorder-owned invalid/empty-file cleanup; never unlink an uncertain or still-open writer. */
    public static boolean deleteIfClosed(File file) {
        return deleteClosed(file) == DeleteResult.DELETED;
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
