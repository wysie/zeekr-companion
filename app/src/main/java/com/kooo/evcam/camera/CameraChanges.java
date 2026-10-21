package com.kooo.evcam.camera;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.recording.RecordingCoordinator;

/** Process-owned Apply transaction. UI recreation must not duplicate it or lose its result. */
public final class CameraChanges {
    private static volatile boolean busy, failed, confirming;
    private static int transaction;
    private static RecordingCoordinator activeRecorder;
    private static java.lang.ref.WeakReference<MainActivity> confirmingOwner = new java.lang.ref.WeakReference<>(null);
    private static java.lang.ref.WeakReference<android.app.Dialog> confirmation = new java.lang.ref.WeakReference<>(null);
    private static final android.os.Handler MAIN = new android.os.Handler(android.os.Looper.getMainLooper());
    private CameraChanges() {}
    public static boolean busy() { return busy; }
    public static boolean failed() { return failed; }

    public static void request(MainActivity activity) {
        MainActivity previousOwner = confirmingOwner.get();
        android.app.Dialog currentConfirmation = confirmation.get();
        if (confirming && (currentConfirmation == null || !currentConfirmation.isShowing()
                || previousOwner == null || previousOwner.isFinishing() || previousOwner.isDestroyed()))
            cancelConfirmation();
        com.kooo.evcam.AppLog.i("CameraChanges", "Apply request: busy=" + busy + ", confirming=" + confirming
                + ", finishing=" + activity.isFinishing() + ", destroyed=" + activity.isDestroyed()
                + ", " + CameraManagerHolder.getInstance().configurationSummary(activity));
        if (busy || confirming || activity.isFinishing() || activity.isDestroyed()) return;
        if (com.kooo.evcam.camera.CameraNeeds.current().developerProbe()) {
            android.widget.Toast.makeText(activity, R.string.camera_apply_probe, android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        if (!CameraManagerHolder.getInstance().hasPendingChannelChange(activity)) return;
        RecordingCoordinator recorder = RecordingCoordinator.get(activity);
        if (recorder.busyForDeveloperProbe()) {
            confirming = true;
            confirmingOwner = new java.lang.ref.WeakReference<>(activity);
            android.app.Dialog dialog = new MaterialAlertDialogBuilder(activity, R.style.Theme_Cam_MaterialAlertDialog).setTitle(R.string.camera_apply_title)
                    .setMessage(R.string.camera_apply_confirm)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.camera_apply_action, (clicked, which) -> apply(activity.getApplicationContext()))
                    .setOnDismissListener(dismissed -> {
                        if (confirmation.get() == dismissed) {
                            confirming = false;
                            confirmingOwner.clear();
                            confirmation.clear();
                        }
                    }).show();
            confirmation = new java.lang.ref.WeakReference<>(dialog);
        } else apply(activity.getApplicationContext());
    }

    /** Main thread. The application context outlives the Settings/Activity that started Apply. */
    private static void apply(android.content.Context context) {
        if (busy) return;
        busy = true;
        failed = false;
        final int token = ++transaction;
        RecordingCoordinator recorder = RecordingCoordinator.get(context);
        activeRecorder = recorder;
        CameraManagerHolder holder = CameraManagerHolder.getInstance();
        Runnable replace = holder.prepareSavedChanges(context, success -> finish(recorder, token, success));
        recorder.pauseForCameraChanges(stopped -> {
            if (!busy || token != transaction) return;
            if (!stopped) { finish(recorder, token, false); return; }
            MultiCameraManager previous = holder.getCameraManager();
            if (previous == null) { replace.run(); return; }
            // Also cover cleanup remaining after an earlier stop deadline or a per-lane stop.
            Runnable timeout = () -> finish(recorder, token, false);
            MAIN.postDelayed(timeout, 20_000);
            previous.afterStopsFinished(() -> MAIN.post(() -> {
                if (!busy || token != transaction) return;
                MAIN.removeCallbacks(timeout);
                replace.run();
            }));
        });
    }

    /** Pipeline release also cancels an Apply still draining its recorder, before replacement began. */
    static void cancel() {
        cancelConfirmation();
        if (busy && activeRecorder != null) finish(activeRecorder, transaction, false);
    }

    private static void cancelConfirmation() {
        confirming = false;
        confirmingOwner.clear();
        android.app.Dialog dialog = confirmation.get();
        confirmation.clear();
        if (dialog != null) {
            try { dialog.dismiss(); }
            catch (IllegalArgumentException detached) {
                // Activity teardown may already have removed this dialog's window.
                com.kooo.evcam.AppLog.d("CameraChanges", "Confirmation window already removed with its Activity");
            }
        }
    }

    private static void finish(RecordingCoordinator recorder, int token, boolean success) {
        if (!busy || token != transaction) return;
        failed = !success;
        busy = false;
        activeRecorder = null;
        transaction++;
        recorder.finishCameraChanges(success);
    }
}
