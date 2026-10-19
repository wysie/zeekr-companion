package com.kooo.evcam.zeekr;

import android.graphics.Rect;
import android.os.Build;
import android.util.Size;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

/** Overlay-local usable dimensions, independent of the app's potentially shorter resource metrics. */
final class OverlayAvailableSize {
    private OverlayAvailableSize() {}
    static Size read(View view, WindowManager windows) {
        if (Build.VERSION.SDK_INT >= 30) {
            android.view.WindowMetrics metrics = windows.getCurrentWindowMetrics();
            Rect bounds = metrics.getBounds();
            android.graphics.Insets bars = metrics.getWindowInsets().getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            return new Size(Math.max(1, bounds.width() - bars.left - bars.right),
                    Math.max(1, bounds.height() - bars.top - bars.bottom));
        }
        Rect frame = new Rect();
        if (view.isAttachedToWindow()) view.getWindowVisibleDisplayFrame(frame);
        return frame.width() > 0 && frame.height() > 0 ? new Size(frame.width(), frame.height())
                : new Size(Math.max(1, view.getResources().getDisplayMetrics().widthPixels),
                        Math.max(1, view.getResources().getDisplayMetrics().heightPixels));
    }
}
