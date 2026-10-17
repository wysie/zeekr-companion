package com.kooo.evcam.telemetry;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/** One USB session; called only on the logging worker. Never rotates away older evidence. */
final class DriveLogFiles implements Closeable {
    static final long SEGMENT_BYTES = 8L * 1024 * 1024;
    static final long SESSION_BYTES = 256L * 1024 * 1024;
    static final long FREE_RESERVE_BYTES = 64L * 1024 * 1024;
    interface Health { void check() throws IOException; }
    final File directory;
    private final Health health;
    private final long segmentLimit, sessionLimit;
    private final Map<String, Part> parts = new LinkedHashMap<>();
    private long bytes;
    private boolean closed;
    private static final class Part {
        int number; long bytes; FileOutputStream file; BufferedOutputStream output;
    }
    DriveLogFiles(File logs, Health health) throws IOException {
        this(logs, health, SEGMENT_BYTES, SESSION_BYTES);
    }
    DriveLogFiles(File logs, Health health, long segmentLimit, long sessionLimit) throws IOException {
        this.health=health;this.segmentLimit=segmentLimit;this.sessionLimit=sessionLimit;
        health.check();
        String stamp=new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.US).format(new Date());
        directory=new File(logs,"Drive_"+stamp+"_"+UUID.randomUUID().toString().substring(0,8));
        if(!directory.mkdirs()) throw new IOException("Cannot create USB session folder");
    }
    long bytes() { return bytes; }
    void line(String channel,String line) throws IOException {
        if(!Arrays.asList("signals","app","lifecycle","session").contains(channel))
            throw new IllegalArgumentException("Unknown log channel");
        byte[] data=(line+"\n").getBytes(StandardCharsets.UTF_8);
        check(data.length);
        Part p=parts.get(channel);
        if(p==null) {p=new Part();parts.put(channel,p);}
        if(p.output==null||p.bytes+data.length>segmentLimit) {
            health.check();
            closePart(p);
            p.number++;p.bytes=0;
            File next=new File(directory,String.format(Locale.US,"%s-%03d.log",channel,p.number));
            if(!next.createNewFile()) throw new IOException("Refusing to overwrite a log part");
            p.file=new FileOutputStream(next);p.output=new BufferedOutputStream(p.file,64*1024);
        }
        p.output.write(data);p.bytes+=data.length;bytes+=data.length;
    }
    void snapshot(String phase,String json) throws IOException {
        if(!"start".equals(phase)&&!"stop".equals(phase)) throw new IllegalArgumentException("Unknown snapshot");
        byte[] data=json.getBytes(StandardCharsets.UTF_8);
        if(data.length>4*1024*1024) {line("session","# "+phase+" Diagnostics snapshot exceeds 4 MiB; omitted");return;}
        check(data.length);health.check();
        File next=new File(directory,"diagnostics-"+phase+".json");
        if(!next.createNewFile()) throw new IOException("Refusing to overwrite Diagnostics");
        try(FileOutputStream out=new FileOutputStream(next)) {out.write(data);out.getFD().sync();}
        bytes+=data.length;
    }
    private void check(int incoming) throws IOException {
        if(closed) throw new IOException("Drive log session closed");
        if(bytes+incoming>sessionLimit) throw new IOException("256 MiB session limit reached");
    }
    void flush() throws IOException {
        check(0);health.check();
        for(Part p:parts.values()) if(p.output!=null) {p.output.flush();p.file.getFD().sync();}
    }
    private static void closePart(Part p) throws IOException {
        if(p.output==null)return;
        try {p.output.flush();p.file.getFD().sync();} finally {p.output.close();p.output=null;p.file=null;}
    }
    @Override public void close() throws IOException {
        closed=true;IOException failure=null;
        for(Part p:parts.values()) try {closePart(p);}catch(IOException e){failure=e;}
        if(failure!=null)throw failure;
    }
}
