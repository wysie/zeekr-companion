package com.kooo.evcam.zeekr;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import androidx.appcompat.app.AppCompatDelegate;

import com.kooo.evcam.R;
import com.kooo.evcam.camera.CameraNames;
import com.kooo.evcam.settings.Languages;

/** Camera direction badge, drawn after the camera's crop and mirror transforms. */
final class CameraLabel {
    private final Context context;
    private final Paint background = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final String[] names = new String[4];
    private String language;
    private final boolean followSystemTheme;
    private int themeMode = -1;

    CameraLabel(Context context) { this(context, true); }

    CameraLabel(Context context, boolean followSystemTheme) {
        this.context = context;
        this.followSystemTheme = followSystemTheme;
        background.setColor(0xB3000000);
        text.setColor(0xFFFFFFFF);
        text.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    void draw(Canvas canvas, int lane, int width, int height) {
        draw(canvas, lane, width, height, false, false);
    }

    void drawBottom(Canvas canvas, int lane, int width, int height) {
        draw(canvas, lane, width, height, true, false);
    }

    void drawBottom(Canvas canvas, int lane, int width, int height, boolean atRight) {
        draw(canvas, lane, width, height, true, atRight);
    }

    private void draw(Canvas canvas, int lane, int width, int height, boolean bottom, boolean atRight) {
        if (lane < 0 || lane >= names.length || width <= 0 || height <= 0) {
            return;
        }
        int mode = com.kooo.evcam.settings.AppTheme.nightMode(context);
        if (followSystemTheme && mode != themeMode) {
            themeMode = mode;
            Context palette = com.kooo.evcam.settings.AppTheme.palette(context);
            background.setColor(palette.getResources().getColor(R.color.camera_label_background, null));
            text.setColor(palette.getResources().getColor(R.color.camera_label_text, null));
        }
        String currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                + ":" + context.getResources().getConfiguration().getLocales().toLanguageTags();
        if (!currentLanguage.equals(language)) {
            Context localized = Languages.localized(context);
            for (int i = 0; i < names.length; i++) {
                names[i] = CameraNames.ofLane(localized, i);
            }
            language = currentLanguage;
        }
        float density = context.getResources().getDisplayMetrics().density;
        float size = Math.min(16f * density, Math.min(width / 6f, height / 5f));
        text.setTextSize(size);
        float margin = size / 2f;
        float paddingX = size / 2f;
        float paddingY = size / 4f;
        float top = text.ascent();
        float badgeWidth = text.measureText(names[lane]) + 2f * paddingX;
        float badgeHeight = text.descent() - top + 2f * paddingY;
        float y = bottom ? Math.max(margin, height - margin - badgeHeight) : margin;
        float x = atRight ? Math.max(margin, width - margin - badgeWidth) : margin;
        canvas.drawRoundRect(x, y, x + badgeWidth, y + badgeHeight,
                paddingY, paddingY, background);
        canvas.drawText(names[lane], x + paddingX, y + paddingY - top, text);
    }
}
