package com.kooo.evcam.telemetry;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import static org.junit.Assert.*;

public class DriveLogFilesTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();
    private String read(File dir,String channel) throws Exception {
        File[] files=dir.listFiles((d,n)->n.startsWith(channel+"-")&&n.endsWith(".log"));
        Arrays.sort(files);StringBuilder out=new StringBuilder();
        for(File f:files)out.append(new String(Files.readAllBytes(f.toPath()),StandardCharsets.UTF_8));
        return out.toString();
    }
    @Test public void rotationsKeepEveryEventAndOldDriveSessions() throws Exception {
        File logs=temp.newFolder();
        DriveLogFiles first=new DriveLogFiles(logs,()->{},1000,8*1024*1024);
        StringBuilder expected=new StringBuilder();
        for(int i=0;i<65000;i++){String line="event "+i;expected.append(line).append('\n');first.line("signals",line);}
        first.line("app","camera state changed");first.line("lifecycle","Activity stopped");
        first.snapshot("start","{\"start\":1}");first.snapshot("stop","{\"stop\":1}");first.flush();first.close();
        assertEquals(expected.toString(),read(first.directory,"signals"));
        assertTrue(read(first.directory,"app").contains("camera state changed"));
        assertTrue(read(first.directory,"lifecycle").contains("Activity stopped"));
        assertTrue(new File(first.directory,"diagnostics-stop.json").isFile());
        try(DriveLogFiles second=new DriveLogFiles(logs,()->{})){
            assertNotEquals(first.directory,second.directory);second.line("signals","second drive");
        }
        assertEquals(expected.toString(),read(first.directory,"signals"));
    }
    @Test public void failureStopsWithoutDeletingEvidenceOrSwitchingDirectory() throws Exception {
        boolean[] connected={true};File logs=temp.newFolder();
        DriveLogFiles session=new DriveLogFiles(logs,()->{if(!connected[0])throw new IOException("disconnected");});
        session.line("signals","warning at t=1");session.flush();connected[0]=false;
        try{session.flush();fail();}catch(IOException expected){assertEquals("disconnected",expected.getMessage());}
        session.close();assertEquals("warning at t=1\n",read(session.directory,"signals"));
        assertEquals(1,logs.listFiles().length);
    }
    @Test public void sizeCapRejectsNewDataAndLeavesExistingPartsIntact() throws Exception {
        DriveLogFiles session=new DriveLogFiles(temp.newFolder(),()->{},20,30);
        session.line("signals","123456789");session.line("signals","abcdefghij");
        try{session.line("signals","exceeds remaining budget");fail();}catch(IOException expected){ }
        session.close();assertEquals("123456789\nabcdefghij\n",read(session.directory,"signals"));
    }
    @Test public void snapshotsNeverOverwriteAndClosedSessionsRejectWrites() throws Exception {
        DriveLogFiles session=new DriveLogFiles(temp.newFolder(),()->{});
        session.snapshot("start","{\"kept\":true}");
        try{session.snapshot("start","{}");fail();}catch(IOException expected){ }
        session.close();
        try{session.line("signals","after stop");fail();}catch(IOException expected){ }
        assertEquals("{\"kept\":true}",new String(Files.readAllBytes(new File(session.directory,"diagnostics-start.json").toPath()),StandardCharsets.UTF_8));
    }
}
