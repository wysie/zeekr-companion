package com.kooo.evcam.zeekr;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.screen.ScreenState;
import com.kooo.evcam.ui.WrappingActionRow;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;

/** Main-display square controls and explicit, viewport-bound horizontal HUD calibration. */
public final class HudCalibrationActivity extends AppCompatActivity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private HudCalibrationSession session;
    private TextView position, result;
    private Button start, stop;
    private LinearLayout controls;
    private SeekBar horizontal, vertical, size;
    private SeekBar margin;
    private TextView boundaries, marginLabel;
    private Button setLeft, setRight, saveBounds, resetBounds;
    private JSONObject draftFrame;
    private Integer draftLeft, draftRight;
    private int draftMargin = HudSafeArea.DEFAULT_MARGIN;
    private boolean syncing;
    private int step = 1;
    private final Runnable timeout = () -> stopTest(R.string.hud_test_timeout);
    private final ScreenState.Listener screen = new ScreenState.Listener() {
        @Override public void onScreenOff() { stopTest(R.string.hud_test_stopped); }
        @Override public void onScreenOn() { }
    };
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        int gutter = getResources().getDimensionPixelSize(R.dimen.gutter);
        page.setPadding(gutter, gutter, gutter, gutter); page.setBackgroundColor(getColor(R.color.bg));
        setContentView(page);
        text(page, R.string.hud_test_title, R.style.TextAppearance_Cam_SettingsTitle);
        text(page, R.string.hud_test_help, R.style.TextAppearance_Cam_SettingsBody);
        WrappingActionRow actions = row(page);
        start = button(actions, R.string.hud_test_start, this::startTest);
        stop = button(actions, R.string.hud_test_stop, () -> stopTest(R.string.hud_test_stopped));
        button(actions, R.string.hud_test_back, this::finish);
        position = text(page, R.string.hud_test_idle, R.style.TextAppearance_Cam_SettingsBody);
        result = text(page, R.string.hud_test_idle, R.style.TextAppearance_Cam_SettingsBody);
        result.setText(""); result.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        ScrollView scroll = new ScrollView(this); page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); scroll.addView(body);
        text(body, R.string.hud_safe_title, R.style.TextAppearance_Cam_SettingsTitle);
        text(body, R.string.hud_safe_help, R.style.TextAppearance_Cam_SettingsBody);
        boundaries = text(body, R.string.hud_safe_none, R.style.TextAppearance_Cam_SettingsBody);
        boundaries.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        horizontal = slider(body, R.string.hud_test_x);
        WrappingActionRow edges = row(body);
        setLeft = button(edges, R.string.hud_safe_set_left, () -> setBoundary(false));
        setRight = button(edges, R.string.hud_safe_set_right, () -> setBoundary(true));
        marginLabel = text(body, R.string.hud_safe_margin, R.style.TextAppearance_Cam_SettingsBody);
        margin = new SeekBar(this); margin.setMax(5);
        margin.setContentDescription(getString(R.string.hud_safe_margin_label));
        margin.setMinimumHeight((int)(48 * getResources().getDisplayMetrics().density));
        body.addView(margin, new LinearLayout.LayoutParams(-1, -2));
        margin.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int value, boolean user) {
                if (!user) return;
                draftMargin = value; touch(); updateCalibration();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { touch(); }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        WrappingActionRow calibrationActions = row(body);
        saveBounds = button(calibrationActions, R.string.hud_safe_save, this::saveCalibration);
        resetBounds = button(calibrationActions, R.string.hud_safe_reset, this::resetCalibration);
        text(body, R.string.hud_safe_adjust, R.style.TextAppearance_Cam_SettingsTitle);
        controls = new LinearLayout(this); controls.setOrientation(LinearLayout.VERTICAL); body.addView(controls);
        vertical = slider(controls, R.string.hud_test_y);
        size = slider(controls, R.string.hud_test_size);
        WrappingActionRow moves = row(controls);
        button(moves, R.string.hud_test_left, () -> nudge(-step, 0));
        button(moves, R.string.hud_test_right, () -> nudge(step, 0));
        button(moves, R.string.hud_test_up, () -> nudge(0, -step));
        button(moves, R.string.hud_test_down, () -> nudge(0, step));
        Button coarse = button(moves, R.string.hud_test_fine, () -> { });
        coarse.setOnClickListener(v -> { step = step == 1 ? 10 : 1;
            coarse.setText(step == 1 ? R.string.hud_test_fine : R.string.hud_test_coarse); touch(); });
        WrappingActionRow colors = row(controls);
        button(colors, R.string.hud_test_red, () -> color(Color.RED));
        button(colors, R.string.hud_test_cyan, () -> color(Color.CYAN));
        button(colors, R.string.hud_test_white, () -> color(Color.WHITE));
        text(controls, R.string.hud_test_observe, R.style.TextAppearance_Cam_SettingsBody);
        WrappingActionRow observations = row(controls);
        observe(observations, R.string.hud_test_visible, "fully_visible");
        observe(observations, R.string.hud_test_clip_left, "left_cut_off");
        observe(observations, R.string.hud_test_clip_right, "right_cut_off");
        observe(observations, R.string.hud_test_clip_top, "top_cut_off");
        observe(observations, R.string.hud_test_clip_bottom, "bottom_cut_off");
        observe(observations, R.string.hud_test_not_seen, "not_seen");
        text(body, R.string.hud_test_log_help, R.style.TextAppearance_Cam_SettingsBody);
        button(row(body), R.string.hud_test_diagnostics,
                () -> startActivity(new Intent(this, DiagnosticsActivity.class)));
        ScreenState.addListener(screen); update();
    }
    private void startTest() {
        if (session != null || ScreenState.dark()) return;
        Display display = SideViewHudPanel.find(this);
        if (display == null) { result.setText(R.string.hud_test_no_display); return; }
        try {
            draftFrame = null; draftLeft = null; draftRight = null; draftMargin = HudSafeArea.DEFAULT_MARGIN;
            session = new HudCalibrationSession(this, display, this::update, () -> {
                session = null; main.removeCallbacks(timeout); update();
            });
            session.start(); touch(); update();
        } catch (RuntimeException e) {
            session = null; update(); result.setText(getString(R.string.hud_test_failed, e.getClass().getSimpleName()));
        }
    }
    private void setBoundary(boolean right) {
        if (session == null) return;
        try {
            session.observation("boundary_selection"); // Refuse a position not yet drawn on the actual HUD.
            if (session.x < 0 || session.y < 0 || session.x + session.size > session.marker.getWidth()
                    || session.y + session.size > session.marker.getHeight()) {
                result.setText(R.string.hud_safe_inside); return;
            }
            JSONObject frame = HudSafeArea.frame(session.display, session.marker);
            if (!HudSafeArea.matches(draftFrame, frame)) { update(); return; }
            if (right) draftRight = session.x + session.size; else draftLeft = session.x;
            touch(); updateCalibration(); result.setText(right ? R.string.hud_safe_right_set : R.string.hud_safe_left_set);
        } catch (Exception notReady) { result.setText(R.string.hud_test_not_ready); }
    }
    private void saveCalibration() {
        if (session == null || draftLeft == null || draftRight == null) return;
        try {
            JSONObject observation = session.observation("safe_area_saved");
            JSONObject frame = HudSafeArea.frame(session.display, session.marker);
            if (!HudSafeArea.matches(draftFrame, frame)) throw new IllegalStateException("Viewport changed");
            JSONObject record = HudSafeArea.record(frame, draftLeft, draftRight, draftMargin);
            if (!HudSafeArea.save(this, record)) { result.setText(R.string.hud_safe_save_failed); return; }
            HudCalibrationLog.save(this, observation.put("safe_area", record));
            stopTest(R.string.hud_safe_saved);
            SideViewPopupService.hudCalibrationChanged();
        } catch (Exception invalid) { result.setText(R.string.hud_safe_invalid); }
    }
    private void resetCalibration() {
        if (!HudSafeArea.reset(this)) { result.setText(R.string.hud_safe_save_failed); return; }
        draftFrame = null; draftLeft = null; draftRight = null; draftMargin = HudSafeArea.DEFAULT_MARGIN;
        SideViewPopupService.hudCalibrationChanged(); updateCalibration();
        result.setText(R.string.hud_safe_reset_done); touch();
    }
    private void updateCalibration() {
        if (boundaries == null) return;
        JSONObject saved = HudSafeArea.read(this), frame = null;
        try { if (session != null) frame = HudSafeArea.frame(session.display, session.marker); }
        catch (Exception unmeasured) { /* Controls remain unavailable until the Presentation is measured. */ }
        boolean ready = frame != null;
        if (ready && !HudSafeArea.matches(draftFrame, frame)) {
            draftFrame = frame; draftLeft = null; draftRight = null; draftMargin = HudSafeArea.DEFAULT_MARGIN;
            if (HudSafeArea.matches(saved, frame)) {
                draftLeft = saved.optInt("left"); draftRight = saved.optInt("right");
                draftMargin = saved.optInt("margin_percent", HudSafeArea.DEFAULT_MARGIN);
            }
        }
        boolean valid = false;
        String summary;
        if (ready) {
            summary = getString(R.string.hud_safe_draft,
                    draftLeft == null ? getString(R.string.hud_safe_not_set) : draftLeft.toString(),
                    draftRight == null ? getString(R.string.hud_safe_not_set) : draftRight.toString());
            if (draftLeft != null && draftRight != null) {
                try {
                    HudPanelPlacement.safeInterval(frame.optInt("viewport_width"), draftLeft, draftRight, draftMargin);
                    valid = true;
                } catch (IllegalArgumentException invalid) { summary += "\n" + getString(R.string.hud_safe_invalid); }
            }
            if (saved != null && !HudSafeArea.matches(saved, frame)) summary += "\n" + getString(R.string.hud_safe_mismatch);
        } else if (saved != null) {
            summary = getString(R.string.hud_safe_stored, saved.optInt("left"), saved.optInt("right"),
                    saved.optInt("margin_percent"), saved.optInt("viewport_width"), saved.optInt("viewport_height"));
        } else summary = getString(R.string.hud_safe_none);
        boundaries.setText(summary);
        setLeft.setEnabled(ready); setRight.setEnabled(ready); margin.setEnabled(ready); horizontal.setEnabled(ready);
        saveBounds.setEnabled(ready && valid); resetBounds.setEnabled(HudSafeArea.hasSaved(this));
        margin.setProgress(draftMargin); marginLabel.setText(getString(R.string.hud_safe_margin, draftMargin));
    }
    private void stopTest(int reason) {
        main.removeCallbacks(timeout);
        if (session != null) session.close();
        update(); if (result != null) result.setText(reason);
    }
    private void touch() { main.removeCallbacks(timeout); if (session != null) main.postDelayed(timeout, 180_000); }
    private void nudge(int dx, int dy) {
        if (session == null) return;
        session.move(session.x + dx, session.y + dy, session.size, session.color); touch(); update();
    }
    private void color(int color) {
        if (session == null) return;
        session.move(session.x, session.y, session.size, color); touch(); update();
    }
    private void observe(WrappingActionRow row, int label, String outcome) {
        button(row, label, () -> {
            if (session == null) return;
            try {
                JSONObject record = session.observation(outcome);
                if (!HudCalibrationLog.save(this, record)) { result.setText(R.string.hud_test_save_failed); return; }
                result.setText(getString(R.string.hud_test_saved, getString(label), session.x, session.y, session.size,
                        HudCalibrationLog.read(this).length()));
                touch();
            } catch (Exception e) { result.setText(R.string.hud_test_not_ready); }
        });
    }
    private SeekBar slider(LinearLayout parent, int label) {
        text(parent, label, R.style.TextAppearance_Cam_SettingsBody);
        SeekBar bar = new SeekBar(this); bar.setContentDescription(getString(label));
        bar.setMinimumHeight((int)(48 * getResources().getDisplayMetrics().density));
        parent.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b, int progress, boolean user) {
                if (syncing || !user || session == null) return;
                int nextSize = b == size ? progress + 32 : session.size;
                session.move(b == horizontal ? progress-session.size : session.x,
                        b == vertical ? progress-session.size : session.y, nextSize, session.color);
                touch(); update();
            }
            @Override public void onStartTrackingTouch(SeekBar b) { touch(); }
            @Override public void onStopTrackingTouch(SeekBar b) { }
        }); return bar;
    }
    private void update() {
        if (start == null) return;
        boolean active = session != null;
        start.setEnabled(!active); stop.setEnabled(active);
        enable(controls, active && session.marker.getWidth() > 0 && session.marker.getHeight() > 0);
        updateCalibration();
        if (!active) { position.setText(R.string.hud_test_idle); return; }
        int w = session.marker.getWidth(), h = session.marker.getHeight();
        position.setText(getString(R.string.hud_test_position, session.display.getDisplayId(), w, h,
                session.x, session.y, session.size));
        syncing = true;
        horizontal.setMax(w + session.size); horizontal.setProgress(session.x + session.size);
        vertical.setMax(h + session.size); vertical.setProgress(session.y + session.size);
        size.setMax(Math.max(0, Math.min(w, h)-32)); size.setProgress(Math.max(0, session.size-32));
        syncing = false;
    }
    private static void enable(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) enable(group.getChildAt(i), enabled);
        }
    }
    private TextView text(LinearLayout parent, int label, int style) {
        TextView text = new TextView(this); text.setTextAppearance(style); text.setText(label);
        int gap = getResources().getDimensionPixelSize(R.dimen.playback_action_gap);
        text.setPadding(0, gap, 0, gap); parent.addView(text); return text;
    }
    private WrappingActionRow row(LinearLayout parent) {
        WrappingActionRow row = new WrappingActionRow(this, null);
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2)); return row;
    }
    private Button button(WrappingActionRow parent, int label, Runnable action) {
        MaterialButton button = new MaterialButton(this, null,
                label == R.string.hud_test_start ? com.google.android.material.R.attr.materialButtonStyle
                        : com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setAllCaps(false); button.setSingleLine(false);
        button.setTextAppearance(R.style.TextAppearance_Cam_SettingsBody);
        if (label != R.string.hud_test_start) button.setTextColor(getColor(R.color.text_primary));
        button.setCornerRadius((int)(8 * getResources().getDisplayMetrics().density));
        button.setMinHeight((int)(52 * getResources().getDisplayMetrics().density));
        button.setText(label); button.setOnClickListener(v -> action.run());
        parent.addView(button); return button;
    }
    @Override protected void onPause() { stopTest(R.string.hud_test_stopped); super.onPause(); }
    @Override protected void onDestroy() {
        ScreenState.removeListener(screen); main.removeCallbacks(timeout);
        if (session != null) session.close(); super.onDestroy();
    }
}
