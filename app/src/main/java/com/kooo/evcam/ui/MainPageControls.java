package com.kooo.evcam.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kooo.evcam.*;
import com.kooo.evcam.camera.CameraManagerHolder;
import com.kooo.evcam.overlay.OverlayCoordinator;
import com.kooo.evcam.playback.GalleryActivity;
import com.kooo.evcam.recording.RecordingCoordinator;
import com.kooo.evcam.settings.PreviewLayoutPicker;
import com.kooo.evcam.zeekr.*;

/** Main-page presentation only. Never initializes or reconfigures a camera pipeline. */
public final class MainPageControls implements DefaultLifecycleObserver {
    private final MainActivity activity;
    private final AppConfig config;
    private final View dock, header;
    private final MaterialButton expand, compactRecord, preview;
    private MaterialButton mirror, birdEyeWindow;
    private final TextView compactStatus;
    private final String layoutAtCreation;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Dialog panel;
    private boolean expanded;
    private final Runnable tick = new Runnable() {
        @Override public void run() { refresh(); handler.postDelayed(this, 500); }
    };

    public MainPageControls(MainActivity activity, Runnable toggleRecording, boolean expanded) {
        this.activity = activity;
        config = new AppConfig(activity);
        layoutAtCreation = config.getPreviewSourceMode();
        dock = activity.findViewById(R.id.action_rail);
        header = activity.findViewById(R.id.main_header);
        expand = activity.findViewById(R.id.btn_preview_expand);
        compactRecord = activity.findViewById(R.id.btn_preview_record);
        compactStatus = activity.findViewById(R.id.expanded_camera_status);
        preview = activity.findViewById(R.id.btn_preview_layout);
        expand.setOnClickListener(v -> setExpanded(!this.expanded));
        compactRecord.setOnClickListener(v -> toggleRecording.run());
        compactStatus.setOnClickListener(v -> activity.startActivity(new Intent(activity, CameraStatusActivity.class)));
        preview.setOnClickListener(v -> { dismissPanel(); panel = MainPreviewPanel.show(activity, this::refresh); });
        activity.findViewById(R.id.btn_main_gallery).setOnClickListener(v -> showGallery());
        activity.findViewById(R.id.btn_main_windows).setOnClickListener(v -> showWindows());
        setExpanded(expanded);
        activity.getLifecycle().addObserver(this);
    }

    public static void applyDriverSide(Activity activity, String side) {
        LinearLayout actions = activity.findViewById(R.id.main_dock_actions);
        if (actions == null) return;
        int[] order = {R.id.btn_start_record, R.id.btn_take_photo, R.id.btn_main_gallery,
                R.id.btn_main_windows, R.id.btn_fisheye};
        boolean left = "left".equals(side);
        for (int i = 0; i < order.length; i++) {
            View child = activity.findViewById(order[left ? i : order.length - 1 - i]);
            if (child == null) continue;
            // Only reorder controls; camera views are never detached.
            actions.removeView(child); actions.addView(child);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) child.getLayoutParams();
            params.setMarginStart(i == 0 ? 0 : Math.round(8 * activity.getResources().getDisplayMetrics().density));
            child.setLayoutParams(params);
        }
    }

    public boolean isExpanded() { return expanded; }

    /** Hide chrome, retaining camera views and an accessible stop/status/restore overlay. */
    public void setExpanded(boolean value) {
        expanded = value;
        dock.setVisibility(value ? View.GONE : View.VISIBLE);
        header.setVisibility(value ? View.GONE : View.VISIBLE);
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
        if (elapsed != null && elapsed.getVisibility() == View.VISIBLE && config.isRecordingStatsEnabled()) status += "\n" + elapsed.getText();
        text(compactStatus, status);
        String selected = config.getPreviewSourceMode();
        boolean pending = !layoutAtCreation.equals(selected) || CameraManagerHolder.getInstance().hasPendingChannelChange(activity);
        text(preview, activity.getString(!layoutAtCreation.equals(selected) ? R.string.home_pending_selection : R.string.main_preview_selection,
                PreviewLayoutPicker.label(activity, selected)));
        activity.findViewById(R.id.main_preview_pending).setVisibility(pending ? View.VISIBLE : View.GONE);
        View surround = activity.findViewById(R.id.composite_wrapper);
        FourLaneContainer composite = activity.findViewById(R.id.composite_container);
        boolean hasSurround = surround != null && surround.getVisibility() != View.GONE;
        View main360 = activity.findViewById(R.id.btn_bird_eye);
        main360.setEnabled(hasSurround);
        main360.setContentDescription(activity.getString(hasSurround ? R.string.bird_eye_button_description : R.string.home_surround_unavailable));
        boolean in360 = composite != null && composite.getDisplayMode() == FourLaneContainer.DisplayMode.BIRD_EYE;
        activity.findViewById(R.id.btn_fisheye).setVisibility(hasSurround && !in360 ? View.VISIBLE : View.GONE);
        if (mirror != null) {
            boolean enabled = config.isRearViewEnabled();
            window(mirror, R.string.main_mirror_window, RearViewMirrorService.isWindowVisible() ? R.string.main_window_open
                    : enabled ? R.string.main_window_paused : R.string.main_window_closed, enabled);
        }
        if (birdEyeWindow != null) window(birdEyeWindow, R.string.main_360_window,
                BirdEyePopupService.isWindowVisible() ? R.string.main_window_open
                : BirdEyePopupService.isRunning() ? R.string.main_window_paused : R.string.main_window_closed,
                BirdEyePopupService.isRunning());
    }

    private void showGallery() {
        dismissPanel();
        panel = CamDialogs.show(new MaterialAlertDialogBuilder(activity, R.style.Theme_Cam_MaterialAlertDialog)
                .setTitle(R.string.home_gallery)
                .setItems(new String[]{activity.getString(R.string.nav_video_playback), activity.getString(R.string.nav_photo_playback)},
                        (d, which) -> activity.startActivity(new Intent(activity, GalleryActivity.class)
                                .putExtra(GalleryActivity.EXTRA_TAB, which == 0 ? "videos" : "photos")))
                .setNegativeButton(R.string.action_cancel, null));
    }

    private void showWindows() {
        dismissPanel();
        LinearLayout content = new LinearLayout(activity); content.setOrientation(LinearLayout.VERTICAL);
        int gap = Math.round(12 * activity.getResources().getDisplayMetrics().density);
        content.setPadding(gap, gap, gap, gap);
        TextView help = new TextView(activity); help.setText(R.string.home_windows_help);
        help.setTextAppearance(R.style.TextAppearance_Cam_Body);
        help.setTextColor(ContextCompat.getColor(activity, R.color.text_primary));
        content.addView(help);
        mirror = windowButton(content, R.id.btn_main_mirror);
        birdEyeWindow = windowButton(content, R.id.btn_main_360_window);
        mirror.setOnClickListener(v -> {
            if (!OverlayCoordinator.setRearViewEnabled(activity, !config.isRearViewEnabled())) WakeUpHelper.requestOverlayPermission(activity);
            refresh();
        });
        birdEyeWindow.setOnClickListener(v -> {
            if (BirdEyePopupService.isRunning()) BirdEyePopupService.stop(activity); else activity.openBirdEyePopup();
            refresh();
        });
        panel = CamDialogs.show(new MaterialAlertDialogBuilder(activity, R.style.Theme_Cam_MaterialAlertDialog)
                .setTitle(R.string.main_windows).setView(content).setNegativeButton(R.string.action_close, null));
        panel.setOnDismissListener(d -> { mirror = null; birdEyeWindow = null; });
        refresh();
    }

    private MaterialButton windowButton(LinearLayout parent, int id) {
        MaterialButton button = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonStyle);
        button.setId(id); button.setAllCaps(false); button.setSingleLine(false); button.setMaxLines(4);
        button.setTextAppearance(R.style.TextAppearance_Cam_Body);
        button.setCornerRadius(Math.round(10 * activity.getResources().getDisplayMetrics().density));
        button.setMinHeight(Math.round(64 * activity.getResources().getDisplayMetrics().density));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = Math.round(12 * activity.getResources().getDisplayMetrics().density);
        parent.addView(button, params); return button;
    }

    private void dismissPanel() { if (panel != null) { panel.dismiss(); panel = null; } }
    private void window(MaterialButton button, int label, int state, boolean enabled) {
        String title = activity.getString(label), description = activity.getString(state);
        text(button, title + " · " + description);
        button.setContentDescription(title + ": " + description);
        androidx.core.view.ViewCompat.setStateDescription(button, description);
        tint(button, enabled);
    }
    private void tint(MaterialButton button, boolean selected) {
        button.setSelected(selected);
        button.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(activity, selected ? R.color.energy_quiet : R.color.card)));
        button.setTextColor(ContextCompat.getColor(activity, selected ? R.color.energy_text : R.color.text_primary));
    }
    private void text(TextView view, CharSequence value) { if (!value.toString().contentEquals(view.getText())) view.setText(value); }
    @Override public void onResume(LifecycleOwner owner) { handler.removeCallbacks(tick); handler.post(tick); }
    @Override public void onPause(LifecycleOwner owner) { handler.removeCallbacks(tick); }
    @Override public void onDestroy(LifecycleOwner owner) { close(); }
    public void close() { dismissPanel(); handler.removeCallbacks(tick); activity.getLifecycle().removeObserver(this); }
}
