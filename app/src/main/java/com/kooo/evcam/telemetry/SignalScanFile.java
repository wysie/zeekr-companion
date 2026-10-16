package com.kooo.evcam.telemetry;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Scan identity, not elapsed-time heuristics, determines whether a restarted process appends. */
final class SignalScanFile {
    private static final String PREFIX = "# signal scan session ";
    private static final long MAX_BYTES = 24L * 1024 * 1024;
    final Writer writer;
    final boolean resumed;
    final int changes;
    private SignalScanFile(Writer writer, boolean resumed, int changes) {
        this.writer = writer; this.resumed = resumed; this.changes = changes;
    }
    static SignalScanFile open(File file, long session, int limit) throws IOException {
        if (session <= 0) throw new IllegalArgumentException("Missing scan session identity");
        boolean append = false; int count = 0;
        if (file.isFile()) {
            if (file.length() > MAX_BYTES) throw new IOException("Existing signal scan exceeds the bounded size; export it before starting another scan");
            try (BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
                String first = input.readLine();
                append = (PREFIX + session).equals(first)
                        || (first != null && !first.startsWith(PREFIX) && file.lastModified() >= session);
                if (append) {
                    String line = first;
                    while (line != null) {
                        if (!line.isEmpty() && !line.startsWith("#")) count = Math.min(limit, count + 1);
                        line = input.readLine();
                    }
                }
            }
        }
        Writer writer = new OutputStreamWriter(new FileOutputStream(file, append), StandardCharsets.UTF_8);
        if (!append) { writer.write(PREFIX + session + "\n"); writer.flush(); }
        return new SignalScanFile(writer, append, count);
    }
}
