package com.kooo.evcam.ui;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import com.kooo.evcam.R;

/** Resource-backed help: selectable, scalable text in the current app language and theme. */
public final class LocalizedGuidePage extends ScrollView {
    private final TextView title;
    private final TextView body;

    public LocalizedGuidePage(Context context) {
        this(context, null);
    }

    public LocalizedGuidePage(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        setFillViewport(true);
        setClickable(true);
        GradientDrawable background = new GradientDrawable();
        background.setColor(ContextCompat.getColor(context, R.color.dialog_guide_background));
        background.setCornerRadius(dp(16));
        setBackground(background);
        setClipToOutline(true);
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(28), dp(24), dp(28), dp(24));
        title = new TextView(context);
        title.setTextAppearance(R.style.TextAppearance_Cam_SettingsTitle);
        title.setTextColor(ContextCompat.getColor(context, R.color.dialog_guide_title));
        ViewCompat.setAccessibilityHeading(title, true);
        content.addView(title, new LinearLayout.LayoutParams(-1, -2));
        body = new TextView(context);
        body.setTextAppearance(R.style.TextAppearance_Cam_SettingsBody);
        body.setTextColor(ContextCompat.getColor(context, R.color.dialog_guide_content));
        body.setLineSpacing(dp(4), 1f);
        body.setTextIsSelectable(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(20);
        content.addView(body, params);
        addView(content, new ScrollView.LayoutParams(-1, -2));
    }

    public void bind(int titleRes, int bodyRes) {
        title.setText(titleRes);
        body.setText(bodyRes);
        scrollTo(0, 0);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
