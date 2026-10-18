package com.kooo.evcam.zeekr;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.kooo.evcam.R;

/** Ferix's indicator badge, placed between our two corner controls. */
final class SideTurnArrow {
    private final float density;
    private final Context context;
    private final boolean followSystemTheme;
    private int themeMode = -1;
    private int litColor = 0xFF2BD15B, unlitColor = 0xFF92959B;
    private final Paint arrow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backdrop = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    SideTurnArrow(Context context) { this(context, true); }

    SideTurnArrow(Context context, boolean followSystemTheme) {
        this.context = context;
        this.followSystemTheme = followSystemTheme;
        density = context.getResources().getDisplayMetrics().density;
        backdrop.setColor(0xCC000000);
    }

    void draw(Canvas canvas, int lane, Integer display, int width, int height) {
        if (lane != LaneCycle.LEFT && lane != LaneCycle.RIGHT) return;
        int mode = com.kooo.evcam.settings.AppTheme.nightMode(context);
        if (followSystemTheme && mode != themeMode) {
            themeMode = mode;
            Context palette = com.kooo.evcam.settings.AppTheme.palette(context);
            backdrop.setColor(palette.getResources().getColor(R.color.side_arrow_background, null));
            litColor = palette.getResources().getColor(R.color.side_arrow_lit, null);
            unlitColor = palette.getResources().getColor(R.color.side_arrow_unlit, null);
        }
        float gap = Math.max(0f, width - 112f * density);
        float size = Math.min(28f * density, gap * 0.8f);
        float padding = Math.min(3f * density, (gap - size) / 2f);
        if (size <= 0f) return;
        float cx = width / 2f;
        float cy = Math.min(height / 2f, 32f * density);
        float half = size / 2f;
        canvas.drawRoundRect(cx - half - padding, cy - half - padding,
                cx + half + padding, cy + half + padding, 4f * density, 4f * density, backdrop);
        path.reset();
        path.moveTo(-half, -size * 0.16f);
        path.lineTo(size * 0.02f, -size * 0.16f);
        path.lineTo(size * 0.02f, -half * 0.8f);
        path.lineTo(half, 0f);
        path.lineTo(size * 0.02f, half * 0.8f);
        path.lineTo(size * 0.02f, size * 0.16f);
        path.lineTo(-half, size * 0.16f);
        path.close();
        Boolean lit = SideViewDecision.indicatorLit(display, lane);
        arrow.setColor(lit == null || lit ? litColor : unlitColor);
        int save = canvas.save();
        canvas.translate(cx, cy);
        if (lane == LaneCycle.LEFT) canvas.scale(-1f, 1f);
        canvas.drawPath(path, arrow);
        canvas.restoreToCount(save);
    }
}
