package com.kooo.evcam.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.kooo.evcam.R;

/** Installer location diagram, not an interactive installer or a language-baked screenshot. */
public final class InstallGuideDiagram extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable logo;
    private final int ink, accent, warning, surface;
    private final String back, open, name;
    private final float fontScale;

    public InstallGuideDiagram(Context context) {
        super(context);
        ink = ContextCompat.getColor(context, R.color.dialog_guide_content);
        accent = ContextCompat.getColor(context, R.color.energy);
        warning = ContextCompat.getColor(context, R.color.recording);
        surface = ContextCompat.getColor(context, R.color.settings_row);
        logo = ContextCompat.getDrawable(context, R.drawable.ic_companion_mark);
        back = context.getString(R.string.action_back);
        open = context.getString(R.string.guide_install_open);
        name = context.getString(R.string.app_name);
        fontScale = getResources().getConfiguration().fontScale;
        // The adjacent localized body describes both locations and their consequences.
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int save = canvas.save();
        float scale = Math.min(getWidth() / 600f, getHeight() / 240f);
        canvas.translate((getWidth() - 600 * scale) / 2, (getHeight() - 240 * scale) / 2);
        canvas.scale(scale, scale);
        paint.setColor(surface); paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(65, 8, 535, 232, 12, 12, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setColor(ink); paint.setStrokeWidth(2);
        canvas.drawRoundRect(65, 8, 535, 232, 12, 12, paint);
        paint.setColor(accent); paint.setStrokeWidth(4);
        canvas.drawCircle(103, 43, 24, paint);
        Path arrow = new Path(); arrow.moveTo(108, 32); arrow.lineTo(97, 43); arrow.lineTo(108, 54);
        paint.setColor(ink); paint.setStrokeCap(Paint.Cap.ROUND); canvas.drawPath(arrow, paint);
        label(canvas, back, 140, 49, Paint.Align.LEFT, 18);
        if (logo != null) { logo.setBounds(271, 75, 329, 133); logo.draw(canvas); }
        label(canvas, name, 300, 162, Paint.Align.CENTER, 20);
        paint.setColor(surface); paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(225, 180, 375, 221, 6, 6, paint);
        paint.setColor(ink); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2);
        canvas.drawRoundRect(225, 180, 375, 221, 6, 6, paint);
        label(canvas, open, 300, 207, Paint.Align.CENTER, 18);
        paint.setColor(warning); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(5);
        canvas.drawLine(276, 183, 324, 218, paint);
        canvas.drawLine(324, 183, 276, 218, paint);
        canvas.restoreToCount(save);
    }

    private void label(Canvas canvas, String text, float x, float y, Paint.Align align, int size) {
        paint.setColor(ink); paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(align); paint.setTextSize(size * fontScale);
        canvas.drawText(text, x, y, paint);
    }
}
