package com.kooo.evcam.ui;

import android.content.Context;
import android.util.Range;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.R;
import com.kooo.evcam.camera.ImageAdjustManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** In-app adaptation of FerixzTanz's merged CameraToolsPanel adjustment rows (285f2b79). */
public final class ImageAdjustPanel {
    private final FrameLayout parent;
    private final LinearLayout panel;
    private final LinearLayout adjustRows;
    private final LayoutInflater inflater;
    private final AppConfig config;
    private final ImageAdjustManager manager;
    private final Consumer<Boolean> onEnabled;
    private final List<Runnable> refreshers = new ArrayList<>();
    private final MaterialButton reset;
    private final SwitchCompat enabled;
    private SeekBar exposureBar;
    private TextView exposureValue;

    public ImageAdjustPanel(FrameLayout parent, ImageAdjustManager manager, Consumer<Boolean> onEnabled) {
        this.parent = parent;
        this.manager = manager;
        this.onEnabled = onEnabled;
        Context context = parent.getContext();
        config = new AppConfig(context);
        inflater = LayoutInflater.from(context);
        panel = new LinearLayout(context);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(context.getColor(R.color.bg));
        panel.setElevation(context.getResources().getDimension(R.dimen.gutter));
        int gap = context.getResources().getDimensionPixelSize(R.dimen.gutter);
        panel.setPadding(gap, gap, gap, gap);
        TextView title = new TextView(context);
        title.setTextAppearance(R.style.TextAppearance_Cam_Title);
        title.setTextColor(context.getColor(R.color.text_primary));
        title.setText(R.string.set_image_adjust_title);
        panel.addView(title);
        TextView note = new TextView(context);
        note.setTextAppearance(R.style.TextAppearance_Cam_Body);
        note.setTextColor(context.getColor(R.color.text_secondary));
        note.setText(R.string.set_image_adjust_summary);
        panel.addView(note);
        enabled = new SwitchCompat(context);
        enabled.setText(R.string.adjust_enabled);
        enabled.setTextAppearance(R.style.TextAppearance_Cam_Row);
        enabled.setTextColor(context.getColor(R.color.text_primary));
        enabled.setMinHeight(context.getResources().getDimensionPixelSize(R.dimen.icon_button_size));
        enabled.setChecked(config.isImageAdjustEnabled());
        enabled.setOnCheckedChangeListener((view, checked) -> {
            config.setImageAdjustEnabled(checked);
            onEnabled.accept(checked);
        });
        panel.addView(enabled);
        ScrollView scroll = new ScrollView(context);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        adjustRows = new LinearLayout(context);
        adjustRows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(adjustRows);
        WrappingActionRow actions = new WrappingActionRow(context, null);
        reset = new MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        reset.setText(R.string.set_image_adjust_reset_title);
        reset.setTextColor(context.getColor(R.color.text_primary));
        reset.setOnClickListener(v -> { manager.resetToDefault(); refreshers.forEach(Runnable::run); });
        actions.addView(reset);
        MaterialButton close = new MaterialButton(context);
        close.setText(R.string.action_close);
        close.setOnClickListener(v -> dismiss());
        actions.addView(close);
        panel.addView(actions);
        int width = Math.min(context.getResources().getDimensionPixelSize(R.dimen.rail_width),
                Math.max(1, parent.getWidth()));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, -1, Gravity.END);
        parent.addView(panel, params);
        refresh();
    }
    public void dismiss() {
        manager.saveParamsToConfig();
        parent.removeView(panel);
    }
    public boolean isShowing() { return panel.getParent() != null; }

    /** 按相机现在支持什么重新填调节那几行。 */
    public void refresh() {
        adjustRows.removeAllViews();
        refreshers.clear();
        ImageAdjustManager manager = this.manager;
        boolean any = false;
        if (manager != null) {
            if (manager.isExposureCompensationSupported() && manager.getExposureRange() != null) {
                addExposure(manager);
                any = true;
            }
            if (manager.isAwbModeSupported()) {
                addChoice(manager, R.string.adj_awb, manager.getSupportedAwbModes(),
                        AppConfig.AWB_MODE_DEFAULT, manager::getAwbMode, manager::setAwbMode,
                        AppConfig::getAwbModeDisplayName);
                any = true;
            }
            if (manager.isTonemapModeSupported()) {
                addChoice(manager, R.string.adj_tonemap, manager.getSupportedTonemapModes(),
                        AppConfig.TONEMAP_MODE_DEFAULT, manager::getTonemapMode, manager::setTonemapMode,
                        AppConfig::getTonemapModeDisplayName);
                any = true;
            }
            if (manager.isEdgeModeSupported()) {
                addChoice(manager, R.string.adj_edge, manager.getSupportedEdgeModes(),
                        AppConfig.EDGE_MODE_DEFAULT, manager::getEdgeMode, manager::setEdgeMode,
                        AppConfig::getEdgeModeDisplayName);
                any = true;
            }
            if (manager.isNoiseReductionModeSupported()) {
                addChoice(manager, R.string.adj_noise, manager.getSupportedNoiseReductionModes(),
                        AppConfig.NOISE_REDUCTION_DEFAULT, manager::getNoiseReductionMode,
                        manager::setNoiseReductionMode, AppConfig::getNoiseReductionModeDisplayName);
                any = true;
            }
            if (manager.isEffectModeSupported()) {
                addChoice(manager, R.string.adj_effect, manager.getSupportedEffectModes(),
                        AppConfig.EFFECT_MODE_DEFAULT, manager::getEffectMode, manager::setEffectMode,
                        AppConfig::getEffectModeDisplayName);
                any = true;
            }
        }
        if (!any) {
            View row = inflater.inflate(R.layout.pref_row_info, adjustRows, false);
            ((TextView) row.findViewById(android.R.id.title)).setText(R.string.adj_unsupported);
            row.findViewById(android.R.id.summary).setVisibility(View.GONE);
            adjustRows.addView(row);
        }
        reset.setVisibility(any ? View.VISIBLE : View.GONE);
    }

    private void addExposure(ImageAdjustManager manager) {
        Range<Integer> range = manager.getExposureRange();
        View row = inflater.inflate(R.layout.pref_row_seekbar, adjustRows, false);
        ((TextView) row.findViewById(android.R.id.title)).setText(R.string.adj_exposure);
        row.findViewById(android.R.id.summary).setVisibility(View.GONE);
        exposureBar = row.findViewById(R.id.seekbar);
        exposureValue = row.findViewById(R.id.seekbar_value);
        exposureBar.setMax(range.getUpper() - range.getLower());
        Runnable sync = () -> {
            exposureBar.setProgress(manager.getExposureCompensation() - range.getLower());
            int value = manager.getExposureCompensation();
            exposureValue.setText((value > 0 ? "+" : "") + value);
        };
        sync.run();
        refreshers.add(sync);
        exposureBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    return;
                }
                touched(manager);
                manager.setExposureCompensation(progress + range.getLower());
                manager.updateAllCameras();
                int value = manager.getExposureCompensation();
                exposureValue.setText((value > 0 ? "+" : "") + value);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                manager.saveParamsToConfig();
            }
        });
        adjustRows.addView(row);
    }

    private interface IntGetter { int get(); }
    private interface IntSetter { void set(int value); }
    private interface Namer { String name(Context context, int value); }

    /** 「名字 … 当前值 ›」一行，点开是单选对话框；第一项永远是「默认」。 */
    private void addChoice(ImageAdjustManager manager, int titleRes, int[] supported,
            int defaultValue, IntGetter getter, IntSetter setter, Namer namer) {
        Context context = panel.getContext();
        List<Integer> values = new ArrayList<>();
        List<String> names = new ArrayList<>();
        values.add(defaultValue);
        names.add(context.getString(R.string.adj_default));
        if (supported != null) {
            for (int mode : supported) {
                values.add(mode);
                names.add(namer.name(context, mode));
            }
        }
        View row = inflater.inflate(R.layout.pref_row_value, adjustRows, false);
        ((TextView) row.findViewById(android.R.id.title)).setText(titleRes);
        TextView summary = row.findViewById(android.R.id.summary);
        Runnable sync = () -> summary.setText(nameOf(names, values, getter.get()));
        sync.run();
        refreshers.add(sync);
        row.setOnClickListener(v -> {
            int checked = Math.max(0, values.indexOf(getter.get()));
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(
                    context, R.style.Theme_Cam_MaterialAlertDialog)
                    .setTitle(titleRes)
                    .setSingleChoiceItems(names.toArray(new String[0]), checked, (dialog, which) -> {
                        touched(manager);
                        setter.set(values.get(which));
                        manager.updateAllCameras();
                        manager.saveParamsToConfig();
                        sync.run();
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.action_cancel, null);
            CamDialogs.show(builder);
        });
        adjustRows.addView(row);
    }

    private static String nameOf(List<String> names, List<Integer> values, int value) {
        int index = values.indexOf(value);
        return index >= 0 ? names.get(index) : names.get(0);
    }

    /** 第一次动调节：启用画面调节（原来是开发者选项里的一行，现在动了就算开） */
    private void touched(ImageAdjustManager manager) {
        if (!config.isImageAdjustEnabled()) {
            config.setImageAdjustEnabled(true);
            onEnabled.accept(true);
            enabled.setChecked(true);
        }
    }

}
