package com.kooo.evcam.settings;

import android.content.Context;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.R;
import com.kooo.evcam.ui.CamDialogs;

/** Same entries/persistence as the Settings row; never edits or saves a recording profile. */
public final class PreviewLayoutPicker {
    private PreviewLayoutPicker() {}

    static String[] labels(Context context) {
        SettingSpec spec = SettingsRegistry.PREVIEW_LAYOUT;
        String[] labels = spec.displayNames(); int[] resources = spec.nameResIds();
        for (int i = 0; i < labels.length; i++) labels[i] = context.getString(resources[i]);
        return labels;
    }

    public static String label(Context context, String value) {
        String[] values = SettingsRegistry.PREVIEW_LAYOUT.values();
        String selected = SettingsRegistry.PREVIEW_LAYOUT.sanitize(value);
        String[] labels = labels(context);
        for (int i = 0; i < values.length; i++) if (values[i].equals(selected)) return labels[i];
        return labels[0];
    }

    public static void show(Context context, Runnable changed) { show(context, 0, changed); }

    public static void show(Context context, int help, Runnable changed) {
        AppConfig config = new AppConfig(context);
        String[] values = SettingsRegistry.PREVIEW_LAYOUT.values();
        int current = java.util.Arrays.asList(values).indexOf(config.getPreviewSourceMode());
        int[] picked = {Math.max(0, current)};
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.Theme_Cam_MaterialAlertDialog);
        if (help != 0) {
            android.view.View title = android.view.LayoutInflater.from(context).inflate(R.layout.dialog_preview_layout_title, null);
            ((android.widget.TextView) title.findViewById(R.id.preview_choice_help)).setText(help);
            builder.setCustomTitle(title);
        }
        CamDialogs.show(builder.setTitle(R.string.record_preview_layout)
                .setSingleChoiceItems(labels(context), picked[0], (dialog, which) -> picked[0] = which)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> {
                    config.setPreviewSourceMode(values[picked[0]]);
                    changed.run();
                })
                .setNegativeButton(R.string.action_cancel, null));
    }
}
