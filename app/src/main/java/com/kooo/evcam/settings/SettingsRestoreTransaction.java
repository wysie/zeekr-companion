package com.kooo.evcam.settings;

import java.io.IOException;
import java.util.Map;

/** Durable preimage/commit-marker protocol for settings held in multiple preference files. */
final class SettingsRestoreTransaction {
    enum Result { APPLIED, FAILED, BLOCKED }
    interface Stores {
        Map<String, Map<String, Object>> read();
        boolean replace(String name, Map<String, Object> values);
    }
    interface Journal {
        Entry read() throws IOException;
        void write(Entry entry) throws IOException;
        void clear();
    }
    static final class Entry {
        final boolean committed;
        final Map<String, Map<String, Object>> before;
        Entry(boolean committed, Map<String, Map<String, Object>> before) {
            this.committed = committed; this.before = before;
        }
    }
    private SettingsRestoreTransaction() {}

    static boolean recover(Stores stores, Journal journal) {
        try {
            Entry entry = journal.read();
            if (entry == null) return true;
            if (!entry.committed && !rollback(stores, entry.before)) return false;
            journal.clear();
            return true;
        } catch (IOException | RuntimeException error) { return false; }
    }

    static Result apply(Stores stores, Journal journal, Map<String, Map<String, Object>> values) {
        if (!recover(stores, journal)) return Result.BLOCKED;
        Map<String, Map<String, Object>> before = stores.read();
        try { journal.write(new Entry(false, before)); }
        catch (IOException | RuntimeException error) { return Result.FAILED; }
        try {
            for (Map.Entry<String, Map<String, Object>> item : values.entrySet())
                if (!stores.replace(item.getKey(), item.getValue())) throw new IOException("Preference write failed");
            // A durable commit marker prevents later rollback if deleting the journal fails.
            journal.write(new Entry(true, before));
            journal.clear();
            return Result.APPLIED;
        } catch (IOException | RuntimeException error) {
            if (!rollback(stores, before)) return Result.BLOCKED;
            journal.clear();
            return Result.FAILED;
        }
    }
    private static boolean rollback(Stores stores, Map<String, Map<String, Object>> before) {
        boolean restored = true;
        for (Map.Entry<String, Map<String, Object>> item : before.entrySet()) {
            try { restored &= stores.replace(item.getKey(), item.getValue()); }
            catch (RuntimeException error) { restored = false; }
        }
        return restored;
    }
}
