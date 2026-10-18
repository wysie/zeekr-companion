package com.kooo.evcam.settings;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Keeps short values at the trailing edge; stacks text when either label would be clipped. */
public final class ValuePreferenceRow extends LinearLayout {
    private TextView title, summary;
    private boolean stacked;
    private int titleTop, titleBottom;

    public ValuePreferenceRow(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override protected void onFinishInflate() {
        super.onFinishInflate();
        title = findViewById(android.R.id.title);
        summary = findViewById(android.R.id.summary);
        titleTop = title.getPaddingTop(); titleBottom = title.getPaddingBottom();
    }

    private boolean clipped(TextView text) {
        if (text.getVisibility() == GONE || text.length() == 0) return false;
        if (text.getMeasuredWidth() == 0 || text.getLayout() == null) return true;
        int last = text.getLayout().getLineCount() - 1;
        return last < 0 || text.getLayout().getEllipsisCount(last) > 0
                || text.getLayout().getLineEnd(last) < text.length();
    }

    private int trailingWidth() {
        int width = 0;
        for (int i = 2; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            LayoutParams p = (LayoutParams) child.getLayoutParams();
            width += child.getMeasuredWidth() + p.leftMargin + p.rightMargin;
        }
        return width;
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        title.setPadding(title.getPaddingLeft(), titleTop, title.getPaddingRight(), titleBottom);
        title.setMaxLines(2); summary.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END); summary.setEllipsize(TextUtils.TruncateAt.END);
        super.onMeasure(widthSpec, heightSpec);
        stacked = clipped(title) || clipped(summary);
        if (!stacked) return;
        int width = getMeasuredWidth();
        int available = Math.max(0, width - getPaddingLeft() - getPaddingRight() - trailingWidth());
        title.setPadding(title.getPaddingLeft(), titleTop, title.getPaddingRight(), dp(4));
        title.setMaxLines(Integer.MAX_VALUE); summary.setMaxLines(Integer.MAX_VALUE);
        title.setEllipsize(null); summary.setEllipsize(null);
        int textWidth = MeasureSpec.makeMeasureSpec(available, MeasureSpec.EXACTLY);
        int unconstrained = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        title.measure(textWidth, unconstrained);
        summary.measure(textWidth, unconstrained);
        int textHeight = title.getMeasuredHeight()
                + (summary.getVisibility() == GONE ? 0 : summary.getMeasuredHeight() + titleBottom);
        int accessoryHeight = 0;
        for (int i = 2; i < getChildCount(); i++) accessoryHeight = Math.max(accessoryHeight, getChildAt(i).getMeasuredHeight());
        int height = Math.max(getSuggestedMinimumHeight(), getPaddingTop() + getPaddingBottom()
                + Math.max(textHeight, accessoryHeight));
        setMeasuredDimension(width, resolveSize(height, heightSpec));
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!stacked) { super.onLayout(changed, l, t, r, b); return; }
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        int x = rtl ? getWidth() - getPaddingRight() - title.getMeasuredWidth() : getPaddingLeft();
        int y = getPaddingTop();
        title.layout(x, y, x + title.getMeasuredWidth(), y + title.getMeasuredHeight());
        y += title.getMeasuredHeight();
        if (summary.getVisibility() != GONE) {
            summary.layout(x, y, x + summary.getMeasuredWidth(), y + summary.getMeasuredHeight());
        }
        int edge = rtl ? getPaddingLeft() : getWidth() - getPaddingRight();
        for (int i = getChildCount() - 1; i >= 2; i--) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;
            LayoutParams p = (LayoutParams) child.getLayoutParams();
            int left = rtl ? edge + p.leftMargin : edge - p.rightMargin - child.getMeasuredWidth();
            int top = (getHeight() - child.getMeasuredHeight()) / 2;
            child.layout(left, top, left + child.getMeasuredWidth(), top + child.getMeasuredHeight());
            edge += (rtl ? 1 : -1) * (child.getMeasuredWidth() + p.leftMargin + p.rightMargin);
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
