package com.kooo.evcam.camera;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.recording.RecordingCoordinator;

/** Process-owned Apply transaction. UI recreation must not duplicate it or lose its result. */
public final class CameraChanges {
    private static volatile boolean busy, failed, confirming;
    private static int transaction;
    private static final android.os.Handler MAIN = new android.os.Handler(android.os.Looper.getMainLooper());
    private CameraChanges() {}
    public static boolean busy() { return busy; }
    public static boolean failed() { return failed; }

    public static void request(MainActivity activity) {
        if (busy || confirming || activity.isFinishing() || activity.isDestroyed()) return;
        if (com.kooo.evcam.camera.CameraNeeds.current().developerProbe()) {
            android.widget.Toast.makeText(activity, R.string.camera_apply_probe, android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        if (!CameraManagerHolder.getInstance().hasPendingChannelChange(activity)) return;
        RecordingCoordinator recorder = RecordingCoordinator.get(activity);
        if (recorder.busyForDeveloperProbe()) {
            confirming = true;
            new MaterialAlertDialogBuilder(activity, R.style.Theme_Cam_MaterialAlertDialog).setTitle(R.string.camera_apply_title)
                    .setMessage(R.string.camera_apply_confirm)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.camera_apply_action, (dialog, which) -> apply(activity.getApplicationContext()))
                    .setOnDismissListener(dialog -> confirming = false).show();
        } else apply(activity.getApplicationContext());
    }

    /** Main thread. The application context outlives the Settings/Activity that started Apply. */
    private static void apply(android.content.Context context) {
        if (busy) return;
        busy = true;
        failed = false;
        final int token = ++transaction;
        RecordingCoordinator recorder = RecordingCoordinator.get(context);
        CameraManagerHolder holder = CameraManagerHolder.getInstance();
        Runnable replace = holder.prepareSavedChanges(context, success -> finish(recorder, success));
        recorder.pauseForCameraChanges(stopped -> {
            if (!stopped) { finish(recorder, false); return; }
            MultiCameraManager previous = holder.getCameraManager();
            if (previous == null) { replace.run(); return; }
            // Also cover cleanup remaining after an earlier stop deadline or a per-lane stop.
            Runnable timeout = () -> { if (busy && token == transaction) finish(recorder, false); };
            MAIN.postDelayed(timeout, 20_000);
            previous.afterStopsFinished(() -> MAIN.post(() -> {
                if (!busy || token != transaction) return;
                MAIN.removeCallbacks(timeout);
                replace.run();
            }));
        });
    }

    private static void finish(RecordingCoordinator recorder, boolean success) {
        failed = !success;
        busy = false;
        transaction++;
        recorder.finishCameraChanges(success);
    }
}
