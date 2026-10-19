package com.kooo.evcam.ui;

import android.app.Activity;
import android.view.View;
import android.widget.LinearLayout;
import com.kooo.evcam.R;

/** Only changes existing view bounds. TextureViews keep their parents and SurfaceTextures. */
public final class MainPreviewArrangement {
    private MainPreviewArrangement() {}

    public static void apply(Activity activity, String arrangement) {
        LinearLayout sources = activity.findViewById(R.id.main_source_panes);
        LinearLayout others = activity.findViewById(R.id.cabin_column);
        View surround = activity.findViewById(R.id.composite_wrapper);
        if (sources == null || others == null) return;
        boolean stacked = "stacked".equals(arrangement);
        boolean hasSurround = surround.getVisibility() != View.GONE;
        boolean horizontalOthers = hasSurround ? stacked : "equal".equals(arrangement);
        sources.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        others.setOrientation(horizontalOthers ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        int gap = Math.round(8 * activity.getResources().getDisplayMetrics().density);
        boolean both = surround.getVisibility() != View.GONE && others.getVisibility() != View.GONE;
        LinearLayout.LayoutParams primary = new LinearLayout.LayoutParams(stacked ? -1 : 0,
                stacked ? 0 : -1, "auto".equals(arrangement) ? 22 : 1);
        surround.setLayoutParams(primary);
        LinearLayout.LayoutParams secondary = new LinearLayout.LayoutParams(stacked ? -1 : 0,
                stacked ? 0 : -1, "auto".equals(arrangement) ? 14 : 1);
        secondary.setMarginStart(!stacked && both ? gap : 0);
        secondary.topMargin = stacked && both ? gap : 0;
        others.setLayoutParams(secondary);
        boolean first = true;
        for (int i = 0; i < others.getChildCount(); i++) {
            View pane = others.getChildAt(i);
            if (pane.getVisibility() == View.GONE) continue;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(horizontalOthers ? 0 : -1,
                    horizontalOthers ? -1 : 0, 1);
            params.setMarginStart(horizontalOthers && !first ? gap : 0);
            params.topMargin = !horizontalOthers && !first ? gap : 0;
            pane.setLayoutParams(params);
            first = false;
        }
    }
}
