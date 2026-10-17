package com.kooo.evcam.telemetry;

import android.content.Context;

import com.kooo.evcam.AppConfig;
import com.kooo.evcam.blackbox.BlackBox;

/**
 * 信号普查开不开，按开关（开发者选项）来：开着且没到点就记，否则停。
 * 应用启动时调一次（进程重启后接着记），开关一动调一次。到点自己停时把开关关掉。
 */
public final class SignalScanControl {

    private SignalScanControl() {
    }

    private static android.os.Handler deadlines;
    private static Runnable expiry;

    public static synchronized void apply(Context context) {
        final Context app = context.getApplicationContext();
        AppConfig config = new AppConfig(app);
        long scanLeft = config.signalScanRemainingMs(SignalScan.MAX_RUN_MS);
        if (config.isSignalScanEnabled() && scanLeft == 0) config.setSignalScanEnabled(false);
        long driveLeft = DriveLogService.remainingMs();
        long left = Math.max(scanLeft, driveLeft);
        // Each owner expires independently even when the other keeps subscriptions alive.
        if (deadlines == null) deadlines = new android.os.Handler(android.os.Looper.getMainLooper());
        if (expiry != null) deadlines.removeCallbacks(expiry);
        if (left > 0) {
            long next = scanLeft > 0 && driveLeft > 0 ? Math.min(scanLeft,driveLeft) : left;
            expiry = () -> apply(app);
            deadlines.postDelayed(expiry,Math.max(1L,next));
        }
        if (left > 0) {
            if (!SignalScan.isRunning()) BlackBox.noteImportant("signal scan on, " + (left / 60000) + " min left");
            long session = scanLeft > 0 ? config.signalScanSessionStartedAt() : DriveLogService.sessionStartedAt();
            SignalScan.start(app, left, session, () -> apply(app));
        } else {
            if (SignalScan.isRunning()) BlackBox.noteImportant("signal scan off");
            SignalScan.stop();
        }
    }
}
