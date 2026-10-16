package com.kooo.evcam.telemetry;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.Files;
import static org.junit.Assert.*;
public class SignalScanFileTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    @Test public void immediateRestartKeepsChangesAndTheExistingBudget() throws Exception {
        File f = temp.newFile();
        SignalScanFile first = SignalScanFile.open(f, 123, 2);
        first.writer.write("event1\nevent2\n"); first.writer.close();
        SignalScanFile resumed = SignalScanFile.open(f, 123, 2);
        assertTrue(resumed.resumed); assertEquals(2,resumed.changes);
        resumed.writer.write("# resumed\n"); resumed.writer.close();
        assertTrue(new String(Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8).contains("event1\nevent2"));
    }
    @Test public void newSessionDoesNotAppendThePreviousScan() throws Exception {
        File f = temp.newFile(); SignalScanFile old=SignalScanFile.open(f,123,10);
        old.writer.write("old event\n"); old.writer.close();
        SignalScanFile fresh=SignalScanFile.open(f,124,10);
        assertFalse(fresh.resumed); assertEquals(0,fresh.changes);fresh.writer.close();
        assertFalse(new String(Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8).contains("old event"));
    }
    @Test public void legacyActiveFileIsKeptDuringUpgrade() throws Exception {
        File f=temp.newFile(); Files.write(f.toPath(),"# signal scan started legacy\nold event\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long started=f.lastModified()-1000;
        SignalScanFile resumed=SignalScanFile.open(f,started,10);
        assertTrue(resumed.resumed);assertEquals(1,resumed.changes);resumed.writer.close();
    }
}
