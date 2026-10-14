package com.kooo.evcam.playback;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** Reserve an export without overwriting saved clips or another export's partial file. */
final class ClipExportFiles {
    static final class Reservation {
        final File partial;
        final File output;
        Reservation(File partial, File output) { this.partial = partial; this.output = output; }
    }

    static Reservation reserve(File dir, String base) throws IOException {
        for (int n = 1; n <= 100; n++) {
            File out = new File(dir, base + (n == 1 ? "" : "_" + n) + ".mp4");
            File partial = new File(dir, out.getName() + ".part");
            if (!out.exists() && partial.createNewFile()) return new Reservation(partial, out);
        }
        return null;
    }

    static File publish(Reservation reservation) {
        try {
            // No REPLACE_EXISTING: a destination created after reservation remains untouched.
            Files.move(reservation.partial.toPath(), reservation.output.toPath());
            return reservation.output;
        } catch (IOException e) {
            reservation.partial.delete();
            return null;
        }
    }
}
