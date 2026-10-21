package com.kooo.evcam.update;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kooo.evcam.R;
import com.kooo.evcam.ui.CamDialogs;

import java.util.Locale;

/** Presentation only: updater selection, integrity checks and install flow remain in UpdateFlow. */
final class UpdateOfferDialog {
    private UpdateOfferDialog() { }

    static AlertDialog show(Activity activity, GithubReleases.Release release,
            String current, Runnable install) {
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(activity, 24), dp(activity, 8), dp(activity, 24), dp(activity, 8));

        TextView version = text(activity, release.tagName, R.style.TextAppearance_Cam_SettingsTitle);
        content.addView(version);
        TextView installed = text(activity, activity.getString(R.string.upd_offer_current, current),
                R.style.TextAppearance_Cam_SettingsCaption);
        installed.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary));
        add(content, installed, 4);
        if (release.apkBytes > 0) {
            TextView size = text(activity, activity.getString(R.string.upd_offer_size,
                    String.format(Locale.US, "%.1f", release.apkBytes / 1024f / 1024f)),
                    R.style.TextAppearance_Cam_SettingsCaption);
            size.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary));
            add(content, size, 4);
        }

        View divider = new View(activity);
        divider.setBackgroundColor(ContextCompat.getColor(activity, R.color.line));
        LinearLayout.LayoutParams line = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 1));
        line.topMargin = dp(activity, 16);
        line.bottomMargin = dp(activity, 16);
        content.addView(divider, line);

        ScrollView scroll = new ScrollView(activity) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                Rect visible = visibleArea(activity);
                int cap = Math.min(dp(activity, 360), Math.round(visible.height() * .42f));
                if (MeasureSpec.getMode(heightSpec) != MeasureSpec.UNSPECIFIED)
                    cap = Math.min(cap, MeasureSpec.getSize(heightSpec));
                super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(cap, MeasureSpec.AT_MOST));
            }
        };
        scroll.setId(R.id.update_offer_notes);
        scroll.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.setScrollbarFadingEnabled(false);
        LinearLayout notes = new LinearLayout(activity);
        notes.setOrientation(LinearLayout.VERTICAL);
        notes.setPadding(0, 0, dp(activity, 8), dp(activity, 8));
        String body = release.notes.isEmpty()
                ? activity.getString(R.string.upd_offer_no_notes) : release.notes;
        for (String paragraph : body.split("\n")) {
            String value = paragraph.trim();
            if (value.isEmpty()) continue;
            boolean bullet = value.startsWith("• ");
            boolean heading = value.equals(activity.getString(R.string.upd_notes_heading))
                    || (value.startsWith("[") && value.endsWith("]"));
            TextView item = text(activity, bullet ? value.substring(2) : value,
                    heading ? R.style.TextAppearance_Cam_SettingsBody
                            : R.style.TextAppearance_Cam_SettingsCaption);
            if (heading) {
                item.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                androidx.core.view.ViewCompat.setAccessibilityHeading(item, true);
            }
            if (bullet) {
                SpannableString hanging = new SpannableString(value.substring(2));
                hanging.setSpan(new BulletSpan(dp(activity, 8)), 0, hanging.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                item.setText(hanging);
            }
            add(notes, item, notes.getChildCount() == 0 ? 0 : 12);
        }
        scroll.addView(notes, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        content.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = CamDialogs.show(new MaterialAlertDialogBuilder(activity,
                R.style.Theme_Cam_MaterialAlertDialog)
                .setTitle(R.string.upd_found_title)
                .setView(content)
                .setPositiveButton(R.string.upd_download, (d, which) -> install.run())
                .setNegativeButton(R.string.upd_later, null));
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(Math.min(dp(activity, 760),
                    Math.round(visibleArea(activity).width() * .94f)),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    private static TextView text(Activity activity, CharSequence value, int appearance) {
        TextView text = new TextView(activity);
        text.setTextAppearance(appearance);
        text.setText(value);
        text.setLineSpacing(dp(activity, 4), 1f);
        return text;
    }

    private static void add(LinearLayout parent, View child, int topDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp((Activity) parent.getContext(), topDp);
        parent.addView(child, params);
    }

    private static Rect visibleArea(Activity activity) {
        Rect area = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(area);
        if (area.isEmpty()) {
            area.set(0, 0, activity.getResources().getDisplayMetrics().widthPixels,
                    activity.getResources().getDisplayMetrics().heightPixels);
        }
        return area;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
