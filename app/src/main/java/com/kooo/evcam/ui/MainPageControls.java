package com.kooo.evcam.ui;

import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.material.button.MaterialButton;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.WakeUpHelper;
import com.kooo.evcam.camera.CameraManagerHolder;
import com.kooo.evcam.overlay.OverlayCoordinator;
import com.kooo.evcam.recording.RecordingCoordinator;
import com.kooo.evcam.settings.PreviewLayoutPicker;
import com.kooo.evcam.zeekr.BirdEyePopupService;
import com.kooo.evcam.zeekr.RearViewMirrorService;

/** Main-page presentation. Uses existing selectors/coordinators; never initializes a pipeline. */
public final class MainPageControls implements DefaultLifecycleObserver {
    private final MainActivity activity;
    private final AppConfig config;
    private final View rail;
    private final MaterialButton expand, compactRecord, preview, mirror, birdEye;
    private final TextView compactStatus;
    private final String layoutAtCreation;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean expanded;
    private final Runnable tick = new Runnable() {
        @Override public void run() { refresh(); handler.postDelayed(this, 500); }
    };

    public MainPageControls(MainActivity activity, Runnable toggleRecording, boolean expanded) {
        this.activity = activity;
        config = new AppConfig(activity);
        layoutAtCreation = config.getPreviewSourceMode();
        rail = activity.findViewById(R.id.action_rail);
        expand = activity.findViewById(R.id.btn_preview_expand);
        compactRecord = activity.findViewById(R.id.btn_preview_record);
        compactStatus = activity.findViewById(R.id.expanded_camera_status);
        preview = activity.findViewById(R.id.btn_preview_layout);
        mirror = activity.findViewById(R.id.btn_main_mirror);
        birdEye = activity.findViewById(R.id.btn_main_360_window);
        expand.setOnClickListener(v -> setExpanded(!this.expanded));
        compactRecord.setOnClickListener(v -> toggleRecording.run());
        compactStatus.setOnClickListener(v -> activity.startActivity(
                new android.content.Intent(activity, CameraStatusActivity.class)));
        preview.setOnClickListener(v -> PreviewLayoutPicker.show(activity, R.string.main_preview_choice_help, this::refresh));
        mirror.setOnClickListener(v -> {
            boolean enabled = !config.isRearViewEnabled();
            if (!OverlayCoordinator.setRearViewEnabled(activity, enabled)) WakeUpHelper.requestOverlayPermission(activity);
            refresh();
        });
        birdEye.setOnClickListener(v -> {
            if (BirdEyePopupService.isRunning()) BirdEyePopupService.stop(activity);
            else activity.openBirdEyePopup();
            refresh();
        });
        setExpanded(expanded);
        activity.getLifecycle().addObserver(this);
    }

    public boolean isExpanded() { return expanded; }

    /** Only change bounds; keep camera TextureViews attached and all saved placements untouched. */
    public void setExpanded(boolean value) {
        expanded = value;
        rail.setVisibility(value ? View.GONE : View.VISIBLE);
        compactRecord.setVisibility(value ? View.VISIBLE : View.GONE);
        compactStatus.setVisibility(value ? View.VISIBLE : View.GONE);
        expand.setContentDescription(activity.getString(value ? R.string.main_restore_controls : R.string.main_expand_preview));
        expand.setSelected(value);
        expand.setIconResource(value ? R.drawable.ic_main_restore : R.drawable.ic_side_expand);
        refresh();
    }

    public void refresh() {
        RecordingCoordinator recorder = RecordingCoordinator.get(activity);
        boolean waitingUsb = recorder.isWaitingForStorage();
        boolean active = recorder.isRecordingOrResuming() || waitingUsb;
        TextView primaryTitle = activity.findViewById(R.id.record_title);
        if (waitingUsb) compactRecord.setText(R.string.record_cancel_usb_wait);
        else if (active) compactRecord.setText(R.string.record_stop);
        else if (primaryTitle != null) text(compactRecord, primaryTitle.getText());
        else compactRecord.setText(R.string.record_start);
        compactRecord.setIconResource(active ? R.drawable.ic_record_stop : R.drawable.ic_record);
        compactRecord.setContentDescription(compactRecord.getText());
        tint(compactRecord, active);
        String status = CameraStatusText.progress(activity, recorder.summary());
        TextView elapsed = activity.findViewById(R.id.tv_recording_stats);
        if (elapsed != null && elapsed.getVisibility() == View.VISIBLE && config.isRecordingStatsEnabled()) {
            status += "\n" + elapsed.getText();
        }
        text(compactStatus, status);
        String selected = config.getPreviewSourceMode();
        text(preview, activity.getString(R.string.main_preview_selection, PreviewLayoutPicker.label(activity, selected)));
        boolean pending = !layoutAtCreation.equals(selected) || CameraManagerHolder.getInstance().hasPendingChannelChange(activity);
        activity.findViewById(R.id.main_preview_pending).setVisibility(pending ? View.VISIBLE : View.GONE);
        boolean mirrorEnabled = config.isRearViewEnabled();
        int mirrorState = RearViewMirrorService.isWindowVisible() ? R.string.main_window_open
                : mirrorEnabled ? R.string.main_window_paused : R.string.main_window_closed;
        window(mirror, R.string.main_mirror_window, mirrorState, mirrorEnabled);
        int birdState = BirdEyePopupService.isWindowVisible() ? R.string.main_window_open
                : BirdEyePopupService.isRunning() ? R.string.main_window_paused : R.string.main_window_closed;
        window(birdEye, R.string.main_360_window, birdState, BirdEyePopupService.isRunning());
    }

    private void window(MaterialButton button, int label, int state, boolean enabled) {
        String title = activity.getString(label);
        String description = activity.getString(state);
        text(button, title + "\n" + description);
        button.setContentDescription(title + ": " + description);
        androidx.core.view.ViewCompat.setStateDescription(button, description);
        tint(button, enabled);
    }

    private void tint(MaterialButton button, boolean selected) {
        button.setSelected(selected);
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(activity,
                selected ? R.color.energy_quiet : R.color.card)));
        button.setTextColor(ContextCompat.getColor(activity, selected ? R.color.energy_text : R.color.text_primary));
    }

    private void text(TextView view, CharSequence value) {
        if (!value.toString().contentEquals(view.getText())) view.setText(value);
    }

    @Override public void onResume(LifecycleOwner owner) { handler.removeCallbacks(tick); handler.post(tick); }
    @Override public void onPause(LifecycleOwner owner) { handler.removeCallbacks(tick); }
    @Override public void onDestroy(LifecycleOwner owner) { close(); }
    public void close() { handler.removeCallbacks(tick); activity.getLifecycle().removeObserver(this); }
}
