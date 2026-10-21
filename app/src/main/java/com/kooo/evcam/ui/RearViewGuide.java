package com.kooo.evcam.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.kooo.evcam.AppConfig;
import com.kooo.evcam.R;

/** Five localized, scrollable gesture-help pages, also available from Settings. */
public final class RearViewGuide {

    /** Keep the existing bounded landscape guide footprint; text scrolls when needed. */
    private static final float ASPECT = 16f / 9f;

    private static final int[][] PAGES = {
            {R.string.guide_controls_title, R.string.guide_controls_body},
            {R.string.guide_cameras_title, R.string.guide_cameras_body},
            {R.string.guide_framing_title, R.string.guide_framing_body},
            {R.string.guide_docking_title, R.string.guide_docking_body},
            {R.string.guide_resize_title, R.string.guide_resize_body},
    };

    private RearViewGuide() {
    }

    /** 第一次打开超级后视镜时弹一次；弹过就不再弹。 */
    public static void showOnce(Activity activity) {
        if (activity == null) {
            return;
        }
        AppConfig config = new AppConfig(activity);
        if (config.isRearViewGuideSeen()) {
            return;
        }
        config.setRearViewGuideSeen();
        show(activity);
    }

    /** 什么时候调都打开。 */
    public static Dialog show(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_rearview_guide);
        dialog.setCancelable(true);
        Window window = dialog.getWindow();
        if (window != null) {
            // Full-screen transparent host: tapping outside the page dismisses the guide.
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.72f);
        }

        // Bound the page so the paging hint and final action remain visible.
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int pageWidth = Math.round(Math.min(metrics.widthPixels * 0.8f,
                metrics.heightPixels * 0.72f * ASPECT));
        int pageHeight = Math.round(pageWidth / ASPECT);

        RecyclerView pages = dialog.findViewById(R.id.guide_pages);
        LinearLayout dots = dialog.findViewById(R.id.guide_dots);
        TextView hint = dialog.findViewById(R.id.guide_hint);
        View done = dialog.findViewById(R.id.guide_done);

        LinearLayout.LayoutParams size = new LinearLayout.LayoutParams(pageWidth, pageHeight);
        pages.setLayoutParams(size);
        LinearLayoutManager manager =
                new LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false);
        pages.setLayoutManager(manager);
        pages.setAdapter(new PageAdapter(pageWidth, pageHeight));
        // 一次翻一整张，停在正中，不停在两张中间
        new PagerSnapHelper().attachToRecyclerView(pages);

        buildDots(dots);
        showPage(0, dots, hint, done);
        pages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView view, int state) {
                if (state != RecyclerView.SCROLL_STATE_IDLE) {
                    return;
                }
                int position = manager.findFirstCompletelyVisibleItemPosition();
                if (position != RecyclerView.NO_POSITION) {
                    showPage(position, dots, hint, done);
                }
            }
        });

        // Page scrolling consumes its own gestures; only outside taps reach this root.
        dialog.findViewById(R.id.guide_root).setOnClickListener(v -> dialog.dismiss());
        done.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        return dialog;
    }

    private static void buildDots(LinearLayout dots) {
        Context context = dots.getContext();
        int dot = dp(context, 10);
        int gap = dp(context, 10);
        for (int i = 0; i < PAGES.length; i++) {
            View view = new View(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dot, dot);
            if (i > 0) {
                params.setMarginStart(gap);
            }
            view.setLayoutParams(params);
            dots.addView(view);
        }
    }

    /** 翻到第几张：圆点跟着走，最后一张把提示换成「知道了」。 */
    private static void showPage(int position, LinearLayout dots, TextView hint, View done) {
        Context context = dots.getContext();
        int active = ContextCompat.getColor(context, R.color.energy);
        int idle = ContextCompat.getColor(context, R.color.status_overlay_text_dim);
        for (int i = 0; i < dots.getChildCount(); i++) {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(i == position ? active : idle);
            dots.getChildAt(i).setBackground(shape);
        }
        boolean last = position == PAGES.length - 1;
        hint.setVisibility(last ? View.INVISIBLE : View.VISIBLE);
        done.setVisibility(last ? View.VISIBLE : View.GONE);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {

        private final int width;
        private final int height;

        PageAdapter(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LocalizedGuidePage page = new LocalizedGuidePage(parent.getContext());
            page.setLayoutParams(new RecyclerView.LayoutParams(width, height));
            return new Holder(page);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            LocalizedGuidePage page = (LocalizedGuidePage) holder.itemView;
            page.bind(PAGES[position][0], PAGES[position][1]);
        }

        @Override
        public int getItemCount() {
            return PAGES.length;
        }

        static final class Holder extends RecyclerView.ViewHolder {
            Holder(View view) {
                super(view);
            }
        }
    }
}
