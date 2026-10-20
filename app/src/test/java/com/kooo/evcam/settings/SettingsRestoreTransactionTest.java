package com.kooo.evcam.settings;

import java.io.IOException;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class SettingsRestoreTransactionTest {
    private static Map<String, Map<String,Object>> values(String value) {
        Map<String, Map<String,Object>> out = new LinkedHashMap<>();
        for (String name : new String[]{"app_config","profiles","calibration","window","appearance"}) {
            Map<String,Object> map = new HashMap<>(); map.put("choice",value); out.put(name,map);
        }
        return out;
    }
    private static class Stores implements SettingsRestoreTransaction.Stores {
        Map<String,Map<String,Object>> data = values("old");
        int calls, failAt = -1; boolean alwaysFail;
        public Map<String,Map<String,Object>> read() { return new LinkedHashMap<>(data); }
        public boolean replace(String name, Map<String,Object> value) {
            data.put(name,value); // SharedPreferences.commit can update memory even when disk write fails.
            return !alwaysFail && ++calls != failAt;
        }
    }
    private static class Journal implements SettingsRestoreTransaction.Journal {
        SettingsRestoreTransaction.Entry entry; boolean failPrepare, failCommit, retain;
        public SettingsRestoreTransaction.Entry read() { return entry; }
        public void write(SettingsRestoreTransaction.Entry next) throws IOException {
            if (next.committed ? failCommit : failPrepare) throw new IOException("injected disk failure");
            entry = next;
        }
        public void clear() { if (!retain) entry = null; }
    }
    @Test public void successfulRestoreCommitsAllStores() {
        Stores stores=new Stores(); Journal journal=new Journal();
        assertEquals(SettingsRestoreTransaction.Result.APPLIED,SettingsRestoreTransaction.apply(stores,journal,values("new")));
        assertEquals(values("new"),stores.data); assertNull(journal.entry);
    }
    @Test public void failureAtEveryStoreRollsBackAllIncludingTheFailedInMemoryWrite() {
        for(int fail=1;fail<=5;fail++) {
            Stores stores=new Stores();stores.failAt=fail;Journal journal=new Journal();
            assertEquals(SettingsRestoreTransaction.Result.FAILED,SettingsRestoreTransaction.apply(stores,journal,values("new")));
            assertEquals(values("old"),stores.data);assertNull(journal.entry);
        }
    }
    @Test public void interruptedPreparedTransactionRestoresBeforeReadingConfiguration() {
        Stores stores=new Stores();Journal journal=new Journal();
        journal.entry=new SettingsRestoreTransaction.Entry(false,values("old"));
        stores.data.put("profiles",values("new").get("profiles"));
        assertTrue(SettingsRestoreTransaction.recover(stores,journal));
        assertEquals(values("old"),stores.data);assertNull(journal.entry);
    }
    @Test public void committedMarkerKeepsNewValuesWhenJournalDeletionFails() {
        Stores stores=new Stores();Journal journal=new Journal();journal.retain=true;
        assertEquals(SettingsRestoreTransaction.Result.APPLIED,SettingsRestoreTransaction.apply(stores,journal,values("new")));
        assertTrue(journal.entry.committed);assertTrue(SettingsRestoreTransaction.recover(stores,journal));
        assertEquals(values("new"),stores.data);
    }
    @Test public void journalFailuresCannotPublishAHalfRestore() {
        Stores stores=new Stores();Journal journal=new Journal();journal.failPrepare=true;
        assertEquals(SettingsRestoreTransaction.Result.FAILED,SettingsRestoreTransaction.apply(stores,journal,values("new")));
        assertEquals(0,stores.calls);assertEquals(values("old"),stores.data);
        journal.failPrepare=false;journal.failCommit=true;
        assertEquals(SettingsRestoreTransaction.Result.FAILED,SettingsRestoreTransaction.apply(stores,journal,values("new")));
        assertEquals(values("old"),stores.data);assertNull(journal.entry);
    }
    @Test public void failedRollbackRetainsJournalAndBlocksUntilRecoverySucceeds() {
        Stores stores=new Stores();Journal journal=new Journal();stores.alwaysFail=true;
        assertEquals(SettingsRestoreTransaction.Result.BLOCKED,SettingsRestoreTransaction.apply(stores,journal,values("new")));
        assertNotNull(journal.entry);assertFalse(SettingsRestoreTransaction.recover(stores,journal));
        stores.alwaysFail=false;assertTrue(SettingsRestoreTransaction.recover(stores,journal));
        assertEquals(values("old"),stores.data);assertNull(journal.entry);
    }
}
