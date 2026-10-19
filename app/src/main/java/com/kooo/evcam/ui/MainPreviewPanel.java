package com.kooo.evcam.ui;

import android.app.Dialog;
import android.view.*;
import android.widget.*;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kooo.evcam.*;
import com.kooo.evcam.settings.PreviewLayoutPicker;
import java.util.LinkedHashMap;
import java.util.Map;

/** Staged preview choice. Cancel/Back/outside never writes; Save never edits a recording profile. */
public final class MainPreviewPanel {
    private MainPreviewPanel() {}

    public static Dialog show(MainActivity activity, Runnable changed) {
        AppConfig config = new AppConfig(activity);
        int hostWidth = activity.getWindow().getDecorView().getWidth();
        if (hostWidth <= 0) hostWidth = activity.getResources().getDisplayMetrics().widthPixels;
        int preferredWidth = dp(activity, Math.round(600 * Math.max(1f,
                activity.getResources().getConfiguration().fontScale / 1.3f)));
        final int panelWidth = Math.min(preferredWidth, hostWidth - dp(activity, 24));
        final boolean narrow = panelWidth < preferredWidth;
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(activity, 16);
        content.setPadding(pad, 0, pad, 0);
        section(content, R.string.home_presets);
        RadioGroup sources = new RadioGroup(activity);
        // Four complete labels remain readable at the panel's fixed tablet width.
        sources.setOrientation(LinearLayout.VERTICAL); sources.setBaselineAligned(false);
        Map<Integer, String> values = new LinkedHashMap<>();
        add(sources, values, "selected", PreviewLayoutPicker.label(activity, "selected"));
        add(sources, values, "surround", activity.getString(R.string.home_surround_four));
        add(sources, values, "roads", PreviewLayoutPicker.label(activity, "roads"));
        add(sources, values, "surround_roads", PreviewLayoutPicker.label(activity, "surround_roads"));
        final String[] selected = {config.getPreviewSourceMode()};
        final String[] arrangement = {config.getMainPreviewArrangement()};
        for (Map.Entry<Integer, String> e : values.entrySet())
            if (e.getValue().equals(selected[0])) sources.check(e.getKey());
        content.addView(sources);
        MaterialButton otherSources = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonStyle);
        otherSources.setAllCaps(false); otherSources.setSingleLine(false);
        otherSources.setTextAppearance(R.style.TextAppearance_Cam_Body);
        otherSources.setMinHeight(dp(activity, 56)); otherSources.setCornerRadius(dp(activity, 10));
        otherSources.setIconResource(R.drawable.ic_expand_more); otherSources.setIconGravity(MaterialButton.ICON_GRAVITY_END);
        content.addView(otherSources, new LinearLayout.LayoutParams(-1, -2));
        final Runnable[] reflectAvailability = {() -> {}};
        Runnable reflectOther = () -> {
            boolean other = !values.containsValue(selected[0]);
            otherSources.setText(other ? PreviewLayoutPicker.label(activity, selected[0]) : activity.getString(R.string.home_other_sources));
            otherSources.setSelected(other);
            otherSources.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(activity, other ? R.color.energy_quiet : R.color.sunken)));
            otherSources.setTextColor(ContextCompat.getColor(activity, other ? R.color.energy_text : R.color.text_primary));
            otherSources.setIconTint(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(activity, other ? R.color.energy_text : R.color.text_primary)));
            reflectAvailability[0].run();
        };
        sources.setOnCheckedChangeListener((group, checked) -> {
            if (values.containsKey(checked)) { selected[0] = values.get(checked); reflectOther.run(); }
        });
        reflectOther.run();
        otherSources.setOnClickListener(v -> {
            String[] extra = {"roadfront", "roadrear", "cabin"};
            String[] labels = new String[extra.length];
            for (int i = 0; i < extra.length; i++) labels[i] = PreviewLayoutPicker.label(activity, extra[i]);
            CamDialogs.show(new MaterialAlertDialogBuilder(activity, R.style.Theme_Cam_MaterialAlertDialog)
                    .setTitle(R.string.home_other_sources)
                    .setSingleChoiceItems(labels, java.util.Arrays.asList(extra).indexOf(selected[0]), (d, which) -> {
                        sources.clearCheck(); selected[0] = extra[which]; reflectOther.run(); d.dismiss();
                    }).setNegativeButton(R.string.action_cancel, null));
        });
        section(content, R.string.home_arrangement);
        TextView singleSourceHelp = paragraph(content, R.string.home_layout_single);
        LinearLayout layouts = new LinearLayout(activity); layouts.setOrientation(narrow ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        layouts.setBaselineAligned(false);
        String[] arrangements = {"auto", "equal", "stacked"};
        int[] names = {R.string.home_layout_auto, R.string.home_layout_equal, R.string.home_layout_stacked};
        int[] icons = {R.drawable.ic_home_layout_auto, R.drawable.ic_home_layout_equal, R.drawable.ic_home_layout_stacked};
        MaterialButton[] tiles = new MaterialButton[3];
        Runnable reflectLayout = () -> {
            for (int i = 0; i < tiles.length; i++) {
                if (tiles[i] == null) continue;
                boolean chosen = arrangements[i].equals(arrangement[0]);
                tiles[i].setSelected(chosen);
                androidx.core.view.ViewCompat.setStateDescription(tiles[i], chosen ? activity.getString(R.string.home_layout_selected) : null);
                tiles[i].setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(activity, chosen ? R.color.energy_quiet : R.color.sunken)));
                tiles[i].setTextColor(ContextCompat.getColor(activity, chosen ? R.color.energy_text : R.color.text_primary));
                tiles[i].setStrokeWidth(dp(activity, chosen ? 2 : 1));
                tiles[i].setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(activity, chosen ? R.color.energy : R.color.line)));
            }
        };
        for (int i = 0; i < 3; i++) {
            final int index = i;
            MaterialButton tile = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonStyle);
            tiles[i] = tile; tile.setText(names[i]); tile.setAllCaps(false); tile.setSingleLine(false);
            tile.setTextAppearance(R.style.TextAppearance_Cam_Caption); tile.setIconResource(icons[i]);
            tile.setIconGravity(narrow ? MaterialButton.ICON_GRAVITY_TEXT_START : MaterialButton.ICON_GRAVITY_TEXT_TOP); tile.setIconSize(dp(activity, 36));
            tile.setPaddingRelative(dp(activity, 8), tile.getPaddingTop(), dp(activity, 8), tile.getPaddingBottom());
            tile.setIconTint(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.text_primary)));
            tile.setMinHeight(dp(activity, narrow ? 56 : 100)); tile.setCornerRadius(dp(activity, 10));
            LinearLayout.LayoutParams params = narrow ? new LinearLayout.LayoutParams(-1, -2)
                    : new LinearLayout.LayoutParams(0, -1, 1);
            if (narrow) params.topMargin = i == 0 ? 0 : dp(activity, 8);
            else params.setMarginStart(i == 0 ? 0 : dp(activity, 8)); layouts.addView(tile, params);
            tile.setOnClickListener(v -> { arrangement[0] = arrangements[index]; reflectLayout.run(); });
        }
        reflectLayout.run(); content.addView(layouts, new LinearLayout.LayoutParams(-1, -2));
        reflectAvailability[0] = () -> {
            boolean multiple = com.kooo.evcam.profile.ProfileChannels.of(
                    new com.kooo.evcam.profile.ProfileStore(activity).current(), selected[0]).previewKeys.size() > 1;
            singleSourceHelp.setVisibility(multiple ? View.GONE : View.VISIBLE);
            layouts.setVisibility(multiple ? View.VISIBLE : View.GONE);
            for (MaterialButton tile : tiles) tile.setEnabled(multiple);
        };
        reflectAvailability[0].run();
        paragraph(content, R.string.home_preview_help);
        paragraph(content, R.string.home_layout_help);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(false);
        scroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        scroll.addView(content);
        LinearLayout title = new LinearLayout(activity); title.setOrientation(LinearLayout.VERTICAL);
        title.setPadding(pad, dp(activity, 4), pad, dp(activity, 4));
        section(title, R.string.home_preview);
        TextView restartHelp = paragraph(title, R.string.home_source_reopen);
        restartHelp.setPadding(0, 0, 0, dp(activity, 4));
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(activity, R.style.Theme_Cam_MaterialAlertDialog)
                .setCustomTitle(title).setView(scroll)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.home_preview_save, (d, which) -> {
                    config.setPreviewSourceMode(selected[0]);
                    config.setMainPreviewArrangement(arrangement[0]);
                    activity.applyMainPreviewArrangement();
                    changed.run();
                }).create();
        dialog.setOnShowListener(d -> {
            CamDialogs.paintNow(dialog);
            Window window = dialog.getWindow();
            if (window != null) {
                // Size from the host's usable bounds (including Android Auto / system insets).
                int height = activity.getWindow().getDecorView().getHeight();
                window.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
                window.setLayout(panelWidth, height - dp(activity, 24));
            }
        });
        dialog.show();
        return dialog;
    }

    private static void add(RadioGroup group, Map<Integer, String> values, String value, String label) {
        RadioButton button = new androidx.appcompat.widget.AppCompatRadioButton(group.getContext());
        button.setId(View.generateViewId()); button.setText(label);
        button.setTextAppearance(R.style.TextAppearance_Cam_Body);
        button.setTextColor(ContextCompat.getColor(group.getContext(), R.color.text_primary));
        boolean narrow = group.getOrientation() == LinearLayout.VERTICAL;
        button.setMinHeight(dp(group.getContext(), narrow ? 56 : 100));
        button.setPadding(dp(group.getContext(), 8), dp(group.getContext(), 8), dp(group.getContext(), 8), dp(group.getContext(), 8));
        RadioGroup.LayoutParams params = narrow ? new RadioGroup.LayoutParams(-1, -2)
                : new RadioGroup.LayoutParams(0, -2, 1);
        if (!narrow) params.setMarginStart(values.isEmpty() ? 0 : dp(group.getContext(), 8));
        group.addView(button, params);
        values.put(button.getId(), value);
    }
    private static TextView section(ViewGroup parent, int text) {
        TextView view = paragraph(parent, text);
        view.setTextAppearance(R.style.TextAppearance_Cam_Row);
        view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        view.setPadding(0, dp(parent.getContext(), 8), 0, dp(parent.getContext(), 4));
        return view;
    }
    private static TextView paragraph(ViewGroup parent, int text) {
        TextView view = new TextView(parent.getContext()); view.setText(text);
        view.setTextAppearance(R.style.TextAppearance_Cam_Caption);
        view.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.text_primary));
        view.setPadding(0, dp(parent.getContext(), 12), 0, dp(parent.getContext(), 8));
        parent.addView(view, new ViewGroup.LayoutParams(-1, -2));
        return view;
    }
    private static int dp(android.content.Context c, int value) { return Math.round(value * c.getResources().getDisplayMetrics().density); }
}
