package com.kooo.evcam.update;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

/** Process-owned download generations. A superseded worker can only remove its own files. */
final class UpdateDownloads {
    static final class Job {
        final File staging;
        final File apk;
        private Job(File staging) {
            this.staging = staging;
            apk = new File(staging.getParentFile(), staging.getName() + ".apk");
        }
    }

    private final Set<Job> pending = new HashSet<>();
    private Job current;

    synchronized Job begin(File directory) throws IOException {
        if (!directory.isDirectory() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Cannot create update directory");
        }
        // Retain every live worker's files, including superseded workers still unwinding.
        File[] old = directory.listFiles();
        if (old != null) for (File file : old) {
            boolean owned = false;
            for (Job job : pending) owned |= file.equals(job.staging) || file.equals(job.apk);
            if (!owned && file.isFile()) file.delete();
        }
        Job job = new Job(File.createTempFile("download-", ".part", directory));
        pending.add(job);
        current = job;
        return job;
    }

    synchronized boolean isCurrent(Job job) { return current == job; }

    synchronized void cancel(Job job) {
        if (current == job) current = null;
        // The worker still owns open handles. It discards its files after closing them.
    }

    boolean publish(Job job, String digest, long bytes) throws IOException {
        // Verify the closed file that the installer will receive, not only network buffers.
        DownloadIntegrity.verifyFile(digest, job.staging, bytes);
        synchronized (this) {
            if (!isCurrent(job)) return false;
            Files.move(job.staging.toPath(), job.apk.toPath());
            return true;
        }
    }

    synchronized void discard(Job job) {
        cancel(job);
        pending.remove(job);
        job.staging.delete();
        job.apk.delete();
    }

    synchronized void retainForInstaller(Job job) {
        if (!isCurrent(job)) throw new IllegalStateException("Superseded update download");
        pending.remove(job);
        current = null;
        // Installation reads asynchronously; preserve this APK until the next requested download.
    }
}
