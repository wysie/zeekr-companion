package com.kooo.evcam.share;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD;

/** At most four serving threads; no waiting queue of sockets or file streams. */
final class BoundedShareRunner implements NanoHTTPD.AsyncRunner {
    static final int MAX_CLIENTS = 4;
    private final Set<NanoHTTPD.ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor pool = newPool();

    static ThreadPoolExecutor newPool() {
        return new ThreadPoolExecutor(0, MAX_CLIENTS, 30, TimeUnit.SECONDS,
                new SynchronousQueue<>(), task -> {
                    Thread thread = new Thread(task, "PhoneShare");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    @Override public void exec(NanoHTTPD.ClientHandler client) {
        clients.add(client);
        try {
            pool.execute(() -> {
                try {
                    try { android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND); }
                    catch (SecurityException unavailable) { /* Thread bound still applies. */ }
                    client.run();
                } finally { client.close(); closed(client); }
            });
        } catch (RejectedExecutionException full) {
            clients.remove(client);
            client.close();
        }
    }

    @Override public void closed(NanoHTTPD.ClientHandler client) { clients.remove(client); }

    @Override public void closeAll() {
        pool.shutdownNow();
        for (NanoHTTPD.ClientHandler client : clients) client.close();
        clients.clear();
    }
}
