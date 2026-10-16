package com.kooo.evcam.zeekr;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kooo.evcam.R;

/** In-place manual calibration, saved independently from all other camera settings. */
public final class BirdEyeCalibrationDialog {
    private BirdEyeCalibrationDialog() {}
    public static void show(Context context, Runnable changed) {
        if (!BirdEyePreview.canConfigure()) {
            android.widget.Toast.makeText(context, R.string.bird_eye_vehicle_required, android.widget.Toast.LENGTH_LONG).show();
            return;
        }
        BirdEyeConfig config = new BirdEyeConfig(context);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        int inset = dp(context, 24);
        content.setPadding(inset, inset / 2, inset, inset);
        TextView note = new TextView(context);
        note.setText(R.string.bird_eye_calibration_note);
        content.addView(note);
        LinearLayout vehicle = new LinearLayout(context);
        vehicle.setOrientation(LinearLayout.VERTICAL); content.addView(vehicle);
        Spinner camera = new Spinner(context);
        String[] cameras = {context.getString(R.string.zeekr_lane_front), context.getString(R.string.zeekr_lane_back),
                context.getString(R.string.zeekr_lane_left), context.getString(R.string.zeekr_lane_right)};
        camera.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, cameras));
        camera.setContentDescription(context.getString(R.string.bird_eye_camera));
        content.addView(camera, new LinearLayout.LayoutParams(-1, dp(context, 56)));
        LinearLayout lens = new LinearLayout(context);
        lens.setOrientation(LinearLayout.VERTICAL); content.addView(lens);
        Runnable rebuild = () -> {
            vehicle.removeAllViews(); lens.removeAllViews();
            note.setText(config.imageFitted() ? R.string.bird_eye_profile_fitted_note : R.string.bird_eye_calibration_note);
            TextView qualityTitle = new TextView(context); qualityTitle.setText(R.string.bird_eye_quality);
            vehicle.addView(qualityTitle);
            Spinner quality = new Spinner(context);
            quality.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item,
                    new String[]{context.getString(R.string.bird_eye_quality_low),
                            context.getString(R.string.bird_eye_quality_balanced), context.getString(R.string.bird_eye_quality_full)}));
            quality.setContentDescription(context.getString(R.string.bird_eye_quality));
            vehicle.addView(quality, new LinearLayout.LayoutParams(-1, dp(context, 56)));
            quality.setSelection(config.quality());
            quality.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onNothingSelected(AdapterView<?> parent) {}
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position != config.quality() && BirdEyePreview.canConfigure()) {
                        config.set("quality", position); changed.run();
                    }
                }
            });
            TextView qualityNote = new TextView(context); qualityNote.setText(R.string.bird_eye_quality_note);
            vehicle.addView(qualityNote);
            if (!config.imageFitted()) {
                slider(context, vehicle, config, "length", R.string.bird_eye_length, 520, 300, 650, changed);
                slider(context, vehicle, config, "width", R.string.bird_eye_width, 200, 140, 260, changed);
            }
            slider(context, vehicle, config, config.imageFitted() ? BirdEyeConfig.FIT_MARGIN : "margin",
                    config.imageFitted() ? R.string.bird_eye_fit_margin : R.string.bird_eye_margin,
                    config.imageFitted() ? (int) (BirdEyeImageFit.DEFAULT_MARGIN * 100) : 200,
                    50, config.imageFitted() ? (int) (BirdEyeImageFit.MAX_MARGIN * 100) : 400, changed);
            int index = Math.max(0, camera.getSelectedItemPosition());
            Spinner lane = new Spinner(context);
            lane.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, cameras));
            TextView laneTitle = new TextView(context); laneTitle.setText(R.string.bird_eye_source_lane);
            lens.addView(laneTitle); lens.addView(lane, new LinearLayout.LayoutParams(-1, dp(context, 56)));
            lane.setSelection(config.get(BirdEyeConfig.key(index, "lane"), index));
            lane.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onNothingSelected(AdapterView<?> parent) {}
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position != config.get(BirdEyeConfig.key(index, "lane"), index)) {
                        if (!BirdEyePreview.canConfigure()) return;
                        config.assignLane(index, position); changed.run();
                    }
                }
            });
            if (config.imageFitted()) return; // Physical-lens sliders do not apply to an empirical fit.
            slider(context, lens, config, BirdEyeConfig.key(index, "height"), R.string.bird_eye_height, 100, 30, 200, changed);
            slider(context, lens, config, BirdEyeConfig.key(index, "pitch"), R.string.bird_eye_pitch, 35, 0, 90, changed);
            slider(context, lens, config, BirdEyeConfig.key(index, "yaw"), R.string.bird_eye_yaw,
                    new int[]{0, 180, 270, 90}[index], 0, 359, changed);
            slider(context, lens, config, BirdEyeConfig.key(index, "roll"), R.string.bird_eye_roll, 0, -180, 180, changed);
            slider(context, lens, config, BirdEyeConfig.key(index, "fov"), R.string.bird_eye_fov, 190, 120, 220, changed);
            slider(context, lens, config, BirdEyeConfig.key(index, "cx"), R.string.bird_eye_cx, 50, 25, 75, changed);
            slider(context, lens, config, BirdEyeConfig.key(index, "cy"), R.string.bird_eye_cy, 50, 25, 75, changed);
            slider(context, lens, config, BirdEyeConfig.key(index, "offset_x"), R.string.bird_eye_offset_x, 0, -200, 200, changed);
            slider(context, lens, config, BirdEyeConfig.key(index, "offset_y"), R.string.bird_eye_offset_y, 0, -250, 250, changed);
            SwitchCompat mirror = new SwitchCompat(context);
            mirror.setText(R.string.bird_eye_mirror);
            mirror.setChecked(config.preferences.getBoolean(BirdEyeConfig.key(index, "mirror"), false));
            mirror.setMinHeight(dp(context, 48)); lens.addView(mirror);
            mirror.setOnCheckedChangeListener((button, on) -> {
                if (!BirdEyePreview.canConfigure()) return;
                config.preferences.edit().putBoolean(BirdEyeConfig.key(index, "mirror"), on).apply(); changed.run();
            });
        };
        camera.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) {}
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { rebuild.run(); }
        });
        rebuild.run();
        ScrollView scroll = new ScrollView(context); scroll.addView(content);
        AlertDialog dialog = new MaterialAlertDialogBuilder(context, R.style.Theme_Cam_MaterialAlertDialog).setTitle(R.string.bird_eye_calibrate)
                .setView(scroll).setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.bird_eye_reset, null).create();
        dialog.setOnShowListener(ignored -> {
            com.kooo.evcam.ui.CamDialogs.paintNow(dialog);
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                if (!BirdEyePreview.canConfigure()) return;
                config.reset(); rebuild.run(); changed.run();
            });
        });
        com.kooo.evcam.telemetry.Telemetry.Listener guard = readings -> {
            if (!BirdEyePreview.canConfigure()) dialog.dismiss();
        };
        com.kooo.evcam.screen.ScreenState.Listener screenGuard = new com.kooo.evcam.screen.ScreenState.Listener() {
            public void onScreenOff() { dialog.dismiss(); }
            public void onScreenOn() {}
        };
        dialog.setOnDismissListener(ignored -> {
            com.kooo.evcam.telemetry.Telemetry.get().removeListener(guard);
            com.kooo.evcam.screen.ScreenState.removeListener(screenGuard);
        });
        com.kooo.evcam.telemetry.Telemetry.get().addListener(guard);
        com.kooo.evcam.screen.ScreenState.addListener(screenGuard);
        dialog.show();
    }
    private static void slider(Context context, LinearLayout parent, BirdEyeConfig config, String key,
                               int title, int fallback, int min, int max, Runnable changed) {
        TextView label = new TextView(context);
        label.setPadding(0, dp(context, 16), 0, 0); parent.addView(label);
        SeekBar bar = new SeekBar(context);
        bar.setMax(max - min);
        int value = Math.max(min, Math.min(max, config.get(key, fallback)));
        bar.setProgress(value - min);
        label.setText(context.getString(R.string.bird_eye_value, context.getString(title), value));
        bar.setContentDescription(label.getText());
        parent.addView(bar, new LinearLayout.LayoutParams(-1, dp(context, 48)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) { changed.run(); }
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                if (!BirdEyePreview.canConfigure()) return;
                int selected = min + progress;
                config.set(key, selected);
                label.setText(context.getString(R.string.bird_eye_value, context.getString(title), selected));
                bar.setContentDescription(label.getText());
                changed.run();
            }
        });
    }
    private static int dp(Context context, int value) { return (int) (value * context.getResources().getDisplayMetrics().density + .5f); }
}
