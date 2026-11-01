package com.kooo.evcam.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.kooo.evcam.R;

/** Text-free gesture diagrams; the adjacent localized text carries the full instructions. */
public final class MirrorGuideDiagram extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int ink, accent, surface;
    private final int page;

    public MirrorGuideDiagram(Context context, int page) {
        super(context);
        this.page = page;
        ink = ContextCompat.getColor(context, R.color.dialog_guide_content);
        accent = ContextCompat.getColor(context, R.color.energy);
        surface = ContextCompat.getColor(context, R.color.settings_row);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int save = canvas.save();
        float scale = Math.min(getWidth() / 600f, getHeight() / 240f);
        canvas.translate((getWidth() - 600 * scale) / 2, (getHeight() - 240 * scale) / 2);
        canvas.scale(scale, scale);
        if (page == 2) {
            // Full source and its shallow retained strip, alongside the mirror viewport.
            frame(canvas, 65, 20, 245, 220, false);
            outline(canvas, new RectF(65, 65, 245, 130), accent);
            frame(canvas, 345, 65, 555, 130, false);
            line(canvas, 245, 65, 345, 65, ink);
            line(canvas, 245, 130, 345, 130, ink);
            arrow(canvas, 300, 180, 300, 70, true);
            finger(canvas, 450, 160);
            arrow(canvas, 450, 170, 450, 220, false);
        } else if (page == 3) {
            outline(canvas, new RectF(110, 15, 510, 225), ink);
            int clip = canvas.save(); canvas.clipRect(110, 15, 510, 225);
            frame(canvas, -115, 70, 155, 160, true);
            canvas.restoreToCount(clip);
            arrow(canvas, 370, 115, 205, 115, false);
            finger(canvas, 145, 115);
        } else {
            frame(canvas, 140, 65, 460, 175, page == 0);
            if (page == 0) {
                finger(canvas, 155, 120); arrow(canvas, 100, 120, 30, 120, false);
                // The same two corner controls as the live window: Expand and Close.
                line(canvas, 172, 84, 187, 84, ink); line(canvas, 172, 84, 172, 99, ink);
                line(canvas, 431, 84, 444, 97, ink); line(canvas, 444, 84, 431, 97, ink);
                arrow(canvas, 280, 120, 360, 120, true);
            } else if (page == 1) {
                finger(canvas, 300, 120);
                arrow(canvas, 270, 120, 180, 120, false);
                arrow(canvas, 330, 120, 420, 120, false);
            } else {
                finger(canvas, 245, 135); finger(canvas, 355, 105);
                arrow(canvas, 230, 150, 165, 210, false);
                arrow(canvas, 370, 90, 435, 30, false);
                outline(canvas, new RectF(90, 30, 510, 210), ink);
            }
        }
        canvas.restoreToCount(save);
    }

    private void frame(Canvas c, float l, float t, float r, float b, boolean handles) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(surface);
        c.drawRoundRect(l, t, r, b, 10, 10, paint);
        outline(c, new RectF(l, t, r, b), ink);
        // Neutral perspective lines identify the camera picture without invented vehicle imagery.
        float mid = (l + r) / 2, horizon = t + (b - t) * .4f;
        line(c, l + 8, horizon, r - 8, horizon, ink);
        line(c, mid - 12, horizon, mid - (r - l) * .2f, b - 8, ink);
        line(c, mid + 12, horizon, mid + (r - l) * .2f, b - 8, ink);
        if (handles) {
            line(c, l + 15, t + 30, l + 15, b - 20, accent);
            line(c, r - 15, t + 30, r - 15, b - 20, accent);
        }
    }

    private void outline(Canvas c, RectF rect, int color) {
        paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(2);
        c.drawRoundRect(rect, 10, 10, paint);
    }

    private void finger(Canvas c, float x, float y) {
        paint.setColor(accent); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(5);
        c.drawCircle(x, y, 12, paint);
    }

    private void line(Canvas c, float x, float y, float xx, float yy, int color) {
        paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3);
        paint.setStrokeCap(Paint.Cap.ROUND); c.drawLine(x, y, xx, yy, paint);
    }

    private void arrow(Canvas c, float x, float y, float xx, float yy, boolean both) {
        line(c, x, y, xx, yy, accent); head(c, x, y, xx, yy);
        if (both) head(c, xx, yy, x, y);
    }

    private void head(Canvas c, float x, float y, float xx, float yy) {
        double a = Math.atan2(yy - y, xx - x);
        Path p = new Path();
        p.moveTo(xx - (float)Math.cos(a - .55) * 12, yy - (float)Math.sin(a - .55) * 12);
        p.lineTo(xx, yy);
        p.lineTo(xx - (float)Math.cos(a + .55) * 12, yy - (float)Math.sin(a + .55) * 12);
        paint.setColor(accent); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(4);
        c.drawPath(p, paint);
    }
}
