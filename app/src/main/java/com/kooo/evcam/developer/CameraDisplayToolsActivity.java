package com.kooo.evcam.developer;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.kooo.evcam.AppConfig;
import com.kooo.evcam.R;
import com.kooo.evcam.StorageHelper;
import com.kooo.evcam.camera.CameraManagerHolder;
import com.kooo.evcam.camera.CameraNeeds;
import com.kooo.evcam.camera.ChannelScheduler;
import com.kooo.evcam.camera.MultiCameraManager;
import com.kooo.evcam.recording.RecordingCoordinator;
import com.kooo.evcam.screen.ScreenState;
import com.kooo.evcam.settings.DeveloperMode;
import com.kooo.evcam.telemetry.Readings;
import com.kooo.evcam.telemetry.Signal;
import com.kooo.evcam.telemetry.Telemetry;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/** User-initiated identification. Never changes camera mappings or HUD settings. */
public final class CameraDisplayToolsActivity extends AppCompatActivity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private TextView status;
    private Button scanButton, displayButton, stopButton;
    private FrameLayout root;
    private boolean visible, identifying, leaseOwned, awaitingClose;
    private volatile boolean scanning, cancelScan;
    private long closeDeadline;
    private volatile CameraFeedExporter exporter;
    private DisplayRun displayRun;
    private static final class DisplayRun {
        final JSONObject report;
        final java.util.concurrent.ExecutorService files = java.util.concurrent.Executors.newSingleThreadExecutor();
        File folder;
        DisplayIdentification session;
        boolean completed;
        DisplayRun(JSONObject report) { this.report = report; }
    }
    private final Telemetry.Listener telemetryListener = r -> checkSafety();
    private final Runnable watch = new Runnable() {
        @Override public void run() {
            checkSafety();
            if (visible) main.postDelayed(this, 200);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        new AppConfig(this); // Initialize the developer preference gate before checking it.
        if (!DeveloperMode.isUnlocked()) { finish(); return; }
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL); content.setPadding(24, 16, 24, 16);
        ScrollView scroll = new ScrollView(this); scroll.addView(content);
        root = new FrameLayout(this); root.addView(scroll, new FrameLayout.LayoutParams(-1, -1)); setContentView(root);
        TextView title = new TextView(this); title.setText(R.string.dev_camera_display_title); title.setTextSize(24); content.addView(title);
        TextView help = new TextView(this);
        help.setText(R.string.dev_camera_display_help);
        help.setTextSize(17); content.addView(help);
        scanButton = button(content, getString(R.string.dev_camera_export), this::startScan);
        displayButton = button(content, getString(R.string.dev_display_identify), this::startDisplays);
        stopButton = button(content, getString(R.string.dev_probe_stop), () -> stop("Stopped by user"));
        status = new TextView(this); status.setTextSize(18); content.addView(status);
        button(content, getString(R.string.dev_probe_back), this::finish);
        Telemetry.get().addListener(telemetryListener);
        updateButtons();
    }
    private Button button(LinearLayout layout, String title, Runnable action) {
        Button button = new Button(this); button.setText(title); button.setOnClickListener(v -> action.run());
        layout.addView(button); return button;
    }
    @Override protected void onResume() {
        super.onResume();
        if (scanButton == null) return;
        visible = true;
        Telemetry.get().acquire(this, "developer-camera-display");
        Telemetry.get().reconcileNow(false, () -> { });
        main.post(watch);
    }
    @Override protected void onPause() {
        visible = false; main.removeCallbacks(watch);
        stop("Stopped: tools left the foreground");
        Telemetry.get().release("developer-camera-display");
        super.onPause();
    }
    @Override protected void onDestroy() {
        Telemetry.get().removeListener(telemetryListener);
        super.onDestroy();
    }
    private String block() {
        if (!visible || !DeveloperMode.isUnlocked() || ScreenState.dark() || ScreenState.androidOff()) return "Keep the tools open with the screen on.";
        Readings r = Telemetry.get().readings();
        if (!Telemetry.get().carLinkConnected()) return "A connected vehicle interface is required.";
        if (Boolean.TRUE.equals(r.bool(Signal.STOCK_360)) || Boolean.TRUE.equals(r.bool(Signal.STOCK_POPUP))
                || Boolean.TRUE.equals(r.bool(Signal.PARK_ASSIST))) return "Close the factory camera / parking-assist view first.";
        return null;
    }
    private boolean canStart() {
        if (scanning || identifying || CameraNeeds.current().developerProbe()) { show("A developer test is already running or still closing."); return false; }
        String reason = block();
        if (reason != null) { show(reason); return false; }
        if (RecordingCoordinator.get(this).busyForDeveloperProbe()) { show("Stop recording and wait for it to finish closing first."); return false; }
        MultiCameraManager manager = CameraManagerHolder.getInstance().getCameraManager();
        if (CameraNeeds.current().isHeld(CameraNeeds.Holder.PHOTO) || (manager != null && manager.developerPhotoBusy())) {
            show("Wait for the current photo to finish."); return false;
        }
        return true;
    }
    private void checkSafety() {
        String reason = block();
        if ((scanning || identifying) && reason != null) stop("Stopped: " + reason);
        else if (identifying && RecordingCoordinator.get(this).busyForDeveloperProbe()) stop("Stopped: recording started");
    }
    private void updateButtons() {
        boolean busy = scanning || identifying || CameraNeeds.current().developerProbe();
        scanButton.setEnabled(!busy); displayButton.setEnabled(!busy); stopButton.setEnabled(scanning || identifying);
    }
    private void show(String message) { if (!isDestroyed()) status.setText(message); }
    private File folder(String prefix) throws Exception {
        File root = StorageHelper.getDeveloperDir(this);
        if (root == null) throw new IllegalStateException("Selected storage is unavailable");
        File folder = new File(root, prefix + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(new Date())
                + "_" + UUID.randomUUID().toString().substring(0, 8));
        if (!folder.mkdirs()) throw new IllegalStateException("Cannot create " + folder);
        return folder;
    }
    private JSONObject report(String kind) throws Exception {
        AppConfig config = new AppConfig(this);
        return new JSONObject().put("kind", kind).put("app_version", getPackageManager().getPackageInfo(getPackageName(), 0).versionName)
                .put("created_at_ms", System.currentTimeMillis()).put("gear", Telemetry.get().readings().text(Signal.GEAR))
                .put("hud_009_enabled", config.isZeekr009HudEnabled()).put("side_output", config.getSideOutput());
    }
    private void startScan() {
        if (!canStart()) return;
        if (!CameraNeeds.current().tryBeginDeveloperProbe()) { show("Cameras are busy."); return; }
        leaseOwned = true; cancelScan = false; scanning = true; awaitingClose = true;
        closeDeadline = SystemClock.elapsedRealtime() + 30_000;
        ChannelScheduler.wakeAll(); updateButtons();
        show("Waiting for this app’s cameras to close…"); main.post(this::waitForClose);
    }
    private void waitForClose() {
        if (!awaitingClose) return;
        String reason = block();
        if (reason != null) { stop(reason); return; }
        if (ChannelScheduler.allCamerasClosedEverywhere() && !ChannelScheduler.closingAnywhere()) {
            awaitingClose = false;
            // File I/O and blocking waits stay off the UI thread.
            new Thread(() -> {
                File destination = null;
                try {
                    destination = folder("CameraScan");
                    JSONObject report = report("camera_ids");
                    report.put("note", "Exposed Camera2 IDs only; no profile/settings changes, no private-buffer access. Images retain native orientation. JPEG may differ from preview geometry.");
                    CameraFeedExporter task = new CameraFeedExporter(getApplicationContext(), destination,
                            text -> main.post(() -> show(text)), () -> main.post(this::releaseLease));
                    exporter = task;
                    if (cancelScan) task.cancel();
                    task.run(report);
                    writeReport(destination, report);
                    String result = "Camera export saved: " + destination + (task.waitingForNativeClosure()
                            ? "\nWaiting for native closure; restart the app if it does not recover." : "");
                    main.post(() -> show(result));
                } catch (Exception e) {
                    main.post(() -> { show("Camera export failed: " + e); if (exporter == null) releaseLease(); });
                } finally {
                    main.post(() -> { scanning = false; exporter = null; updateButtons(); });
                }
            }, "CameraExportFiles").start();
            return;
        }
        if (SystemClock.elapsedRealtime() >= closeDeadline) { stop("Regular camera closure was not confirmed. Export skipped; restart app before retrying."); return; }
        main.postDelayed(this::waitForClose, 100);
    }
    private void releaseLease() {
        if (leaseOwned) { leaseOwned = false; CameraNeeds.current().endDeveloperProbe(); ChannelScheduler.wakeAll(); }
        updateButtons();
    }
    private void stop(String reason) {
        cancelScan = true;
        if (awaitingClose) { awaitingClose = false; scanning = false; releaseLease(); }
        CameraFeedExporter task = exporter;
        if (task != null) task.cancel();
        if (identifying) finishDisplays(reason);
        if (status != null && (scanning || reason.startsWith("Regular"))) show(reason);
        if (scanButton != null) updateButtons();
    }

    private void startDisplays() {
        if (!canStart()) return;
        identifying = true; updateButtons();
        try {
            final DisplayRun run = new DisplayRun(report("display_test_cards"));
            displayRun = run;
            // All file operations are serialized, including observations, cancellation and final report.
            run.files.execute(() -> {
                try { run.folder = folder("DisplayTest"); }
                catch (Exception e) { displayExportFailed(run, e); }
            });
            run.session = new DisplayIdentification(this, root, run.report, new DisplayIdentification.Listener() {
                @Override public void card(String name, Bitmap bitmap) {
                    run.files.execute(() -> {
                        try {
                            if (run.folder == null) return;
                            try (FileOutputStream out = new FileOutputStream(new File(run.folder, name))) {
                                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) throw new IllegalStateException("PNG encoding failed");
                            }
                        } catch (Exception e) { displayExportFailed(run, e); }
                    });
                }
                @Override public void changed() { saveDisplayReport(run); }
                @Override public void finished(String reason) { completeDisplays(run, reason); }
            });
            DisplayManager dm = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
            run.session.start(dm.getDisplays());
        } catch (Exception e) { finishDisplays("Display test failed: " + e); }
    }
    private void displayExportFailed(DisplayRun run, Exception error) {
        main.post(() -> {
            if (displayRun != run || isDestroyed()) return;
            if (!run.completed) {
                try { run.report.put("export_error", error.toString()); } catch (Exception ignored) { }
                finishDisplays("Display export failed: " + error);
            } else show("Display export failed: " + error);
        });
    }
    private void saveDisplayReport(DisplayRun run) {
        if (run.completed) return;
        String json = run.report.toString();
        run.files.execute(() -> {
            try { if (run.folder != null) writeReport(run.folder, new JSONObject(json)); }
            catch (Exception e) { displayExportFailed(run, e); }
        });
    }
    private void finishDisplays(String reason) {
        DisplayRun run = displayRun;
        if (run == null || run.completed) { identifying = false; updateButtons(); return; }
        if (run.session != null) run.session.stop(reason);
        else {
            try { run.report.put("end_reason", reason); } catch (Exception ignored) { }
            saveDisplayReport(run); completeDisplays(run, reason);
        }
    }
    private void completeDisplays(DisplayRun run, String reason) {
        if (run.completed) return;
        run.completed = true; identifying = false; run.files.shutdown();
        show(reason + "\nExports are under ZeekrShortcutApp/Developer/DisplayTest_…"); updateButtons();
    }
    private static void writeReport(File folder, JSONObject report) throws Exception {
        File temp = new File(folder, "report.json.tmp");
        try (FileOutputStream out = new FileOutputStream(temp)) { out.write(report.toString(2).getBytes(StandardCharsets.UTF_8)); }
        if (!temp.renameTo(new File(folder, "report.json"))) throw new IllegalStateException("Cannot publish report.json");
    }
}
