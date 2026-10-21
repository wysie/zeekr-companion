package com.kooo.evcam.update;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.kooo.evcam.R;

/** Localized installer reminder; only explicit confirmation starts the download. */
final class UpdateInstallGuide {

    /** Match the existing bounded guide footprint, with scrollable text. */
    private static final float ASPECT = 16f / 9f;

    private UpdateInstallGuide() {
    }

    /** 弹出提醒；点「知道了，开始下载」才执行 {@code onProceed}，按返回键就什么都不做。 */
    static Dialog show(Activity activity, Runnable onProceed) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_update_guide);
        dialog.setCancelable(true);
        // Outside taps do not dismiss; Back cancels without starting a download.
        dialog.setCanceledOnTouchOutside(false);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.72f);
        }

        // Keep the confirmation action outside the scrollable content.
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int width = Math.round(Math.min(metrics.widthPixels * 0.8f,
                metrics.heightPixels * 0.72f * ASPECT));
        int height = Math.round(width / ASPECT);

        com.kooo.evcam.ui.LocalizedGuidePage page = dialog.findViewById(R.id.guide_image);
        page.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        page.bind(R.string.guide_install_title, R.string.guide_install_body);

        dialog.findViewById(R.id.guide_done).setOnClickListener(v -> {
            dialog.dismiss();
            onProceed.run();
        });
        dialog.show();
        return dialog;
    }
}
