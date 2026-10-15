package com.kooo.evcam.developer;

import android.app.Activity;
import android.app.Presentation;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.kooo.evcam.R;
import com.kooo.evcam.ui.WrappingActionRow;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Manual observations and Android window results are deliberately separate evidence. */
final class DisplayIdentification {
    static final long CARD_MS = 30_000, IDLE_MS = 120_000;
    interface Listener {
        void card(String name, Bitmap bitmap);
        void changed();
        void finished(String reason);
    }
    private final Activity activity;
    private final FrameLayout host;
    private final JSONObject report;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Display> targets = new ArrayList<>();
    private final List<JSONObject> rows = new ArrayList<>();
    private final List<Bitmap> images = new ArrayList<>();
    private final LinearLayout panel;
    private final List<View> background = new ArrayList<>();
    private final List<Integer> backgroundVisibility = new ArrayList<>();
    private Presentation presentation;
    private JSONObject attempt;
    private TextView countdown;
    private boolean closed;
    private long deadline;
    private final Runnable timer = new Runnable() {
        @Override public void run() {
            if (closed) return;
            long remaining = deadline - SystemClock.elapsedRealtime();
            if (remaining <= 0) {
                if (attempt == null) { stop("Display selection timed out"); return; }
                boolean primary = attempt.optInt("display_id", -1) == Display.DEFAULT_DISPLAY;
                endAttempt("timed_out_unconfirmed");
                if (primary) stop("Main-screen check was not confirmed"); else choose();
                return;
            }
            if (countdown != null) countdown.setText(activity.getString(R.string.dev_display_countdown,
                    (remaining + 999) / 1000));
            main.postDelayed(this, Math.min(1000, remaining));
        }
    };

    DisplayIdentification(Activity activity, FrameLayout host, JSONObject report, Listener listener) {
        this.activity = activity; this.host = host; this.report = report; this.listener = listener;
        panel = new LinearLayout(activity); panel.setOrientation(LinearLayout.VERTICAL);
        int pad = activity.getResources().getDimensionPixelSize(R.dimen.gutter);
        panel.setPadding(pad, pad, pad, pad); panel.setBackgroundColor(activity.getColor(R.color.bg));
    }

    void start(Display[] displays) throws JSONException {
        report.put("test_version", 2).put("main_screen_confirmed", false).put("card_duration_ms", CARD_MS)
                .put("note", "Generated cards, NOT screenshots. Android window/layout success is not physical visibility. Main confirmation and secondary observations are entered by the user.");
        JSONArray results = new JSONArray(); report.put("displays", results);
        for (Display display : displays) {
            Display.Mode mode = display.getMode();
            JSONObject row = new JSONObject().put("id", display.getDisplayId()).put("name", display.getName())
                    .put("width", mode.getPhysicalWidth()).put("height", mode.getPhysicalHeight())
                    .put("flags", display.getFlags()).put("state", display.getState()).put("attempts", new JSONArray());
            results.put(row);
            if (!eligible(display)) { row.put("result", "Skipped: off, private or not presentation-capable"); continue; }
            Bitmap bitmap = card(display.getDisplayId(), mode.getPhysicalWidth(), mode.getPhysicalHeight());
            String name = "display_" + display.getDisplayId() + "_test_card.png";
            row.put("test_card", name).put("result", "Not tested");
            targets.add(display); rows.add(row); images.add(bitmap); listener.card(name, bitmap);
        }
        // Keep covered tools out of touch/accessibility traversal while the test owns this content.
        for (int i = 0; i < host.getChildCount(); i++) {
            View child = host.getChildAt(i); background.add(child); backgroundVisibility.add(child.getVisibility());
            child.setVisibility(View.INVISIBLE);
        }
        host.addView(panel, new FrameLayout.LayoutParams(-1, -1));
        int primary = -1;
        for (int i = 0; i < targets.size(); i++) if (targets.get(i).getDisplayId() == Display.DEFAULT_DISPLAY) primary = i;
        if (primary < 0) { stop("Main display unavailable; secondary tests skipped"); return; }
        showPrimary(primary);
    }

    private boolean eligible(Display display) {
        return display.isValid() && display.getState() == Display.STATE_ON
                && (display.getFlags() & Display.FLAG_PRIVATE) == 0
                && (display.getDisplayId() == Display.DEFAULT_DISPLAY || (display.getFlags() & Display.FLAG_PRESENTATION) != 0);
    }

    private void beginAttempt(int index) throws JSONException {
        attempt = new JSONObject().put("display_id", targets.get(index).getDisplayId())
                .put("started_at_ms", System.currentTimeMillis()).put("started_elapsed_ms", SystemClock.elapsedRealtime())
                .put("observation", "unconfirmed");
        rows.get(index).getJSONArray("attempts").put(attempt);
        rows.get(index).put("result", "Test started; physical visibility unconfirmed");
    }

    private void reset(int title, String detail) {
        main.removeCallbacks(timer); panel.removeAllViews(); countdown = null;
        text(panel, activity.getString(title), R.style.TextAppearance_Cam_Title);
        text(panel, detail, R.style.TextAppearance_Cam_Body);
    }

    private void showPrimary(int index) throws JSONException {
        beginAttempt(index);
        reset(R.string.dev_display_main_title, activity.getString(R.string.dev_display_main_help));
        ImageView image = new ImageView(activity); image.setImageBitmap(images.get(index));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER); image.setContentDescription("DISPLAY 0");
        panel.addView(image, new LinearLayout.LayoutParams(-1, 0, 1));
        countdown = text(panel, "", R.style.TextAppearance_Cam_Caption);
        WrappingActionRow actions = actions(panel);
        Button confirm = button(actions, R.string.dev_display_main_confirm, () -> {
            if (attempt == null || !attempt.optBoolean("android_layout_ready")) return;
            endAttempt("seen_on_main_screen");
            try { report.put("main_screen_confirmed", true); } catch (JSONException ignored) { }
            listener.changed(); choose();
        });
        confirm.setEnabled(false);
        button(actions, R.string.dev_display_main_not_seen, () -> {
            endAttempt("not_seen"); stop("Main-screen card not seen by user");
        });
        button(actions, R.string.dev_probe_stop, () -> stop("Stopped by user"));
        laidOut(image, attempt, confirm);
        arm(CARD_MS); listener.changed();
    }

    private void choose() {
        if (closed) return;
        dismissPresentation();
        reset(R.string.dev_display_choose_title, activity.getString(R.string.dev_display_choose_help));
        ScrollView scroll = new ScrollView(activity);
        LinearLayout list = new LinearLayout(activity); list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        int count = 0;
        for (int i = 0; i < targets.size(); i++) {
            if (targets.get(i).getDisplayId() == Display.DEFAULT_DISPLAY) continue;
            final int index = i; Display display = targets.get(i); JSONObject row = rows.get(i);
            Button test = new Button(activity);
            test.setText(activity.getString(R.string.dev_display_test_target, display.getDisplayId(),
                    row.optInt("width"), row.optInt("height")));
            test.setOnClickListener(v -> showSecondary(index)); list.addView(test); count++;
            JSONArray attempts = row.optJSONArray("attempts");
            if (attempts != null && attempts.length() > 0) {
                JSONObject last = attempts.optJSONObject(attempts.length() - 1);
                text(list, activity.getString(R.string.dev_display_last_result, observationLabel(last.optString("observation"))),
                        R.style.TextAppearance_Cam_Caption);
            }
        }
        if (count == 0) text(list, activity.getString(R.string.dev_display_no_secondary), R.style.TextAppearance_Cam_Body);
        countdown = text(panel, "", R.style.TextAppearance_Cam_Caption);
        button(actions(panel), R.string.dev_display_finish, () -> stop("Completed by user"));
        arm(IDLE_MS);
    }

    private void showSecondary(int index) {
        if (closed) return;
        Display display = targets.get(index);
        try {
            beginAttempt(index);
            if (!eligible(display)) throw new IllegalStateException("Display no longer available");
            reset(R.string.dev_display_secondary_title,
                    activity.getString(R.string.dev_display_secondary_help, display.getDisplayId()));
            presentation = new Presentation(activity, display);
            ImageView image = new ImageView(presentation.getContext()); image.setImageBitmap(images.get(index));
            image.setScaleType(ImageView.ScaleType.FIT_CENTER); presentation.setContentView(image);
            Window window = presentation.getWindow();
            if (window == null) throw new IllegalStateException("No Presentation window");
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            final Presentation current = presentation;
            presentation.setOnDismissListener(dialog -> {
                if (closed || presentation != current || attempt == null) return;
                try { attempt.put("error", "Presentation dismissed before observation"); } catch (JSONException ignored) { }
                endAttempt("cancelled_unconfirmed"); choose();
            });
            window.setGravity(Gravity.CENTER); presentation.show();
            int w = rows.get(index).optInt("width"), h = rows.get(index).optInt("height");
            window.setLayout(Math.max(1, w * 3 / 4), Math.max(1, h * 3 / 4));
            attempt.put("android_show_returned", true).put("marker_width", w * 3 / 4).put("marker_height", h * 3 / 4);
            laidOut(image, attempt, null);
            ScrollView scroll = new ScrollView(activity);
            LinearLayout choices = new LinearLayout(activity); choices.setOrientation(LinearLayout.VERTICAL); scroll.addView(choices);
            panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
            text(choices, activity.getString(R.string.dev_display_observation), R.style.TextAppearance_Cam_Row);
            Spinner outcome = new Spinner(activity);
            outcome.setContentDescription(activity.getString(R.string.dev_display_observation));
            String[] labels = activity.getResources().getStringArray(R.array.dev_display_observations);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, labels);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); outcome.setAdapter(adapter);
            choices.addView(outcome, new LinearLayout.LayoutParams(-1, -2));
            countdown = text(panel, "", R.style.TextAppearance_Cam_Caption);
            WrappingActionRow actions = actions(panel);
            Button save = button(actions, R.string.dev_display_save_observation, () -> {
                int selection = outcome.getSelectedItemPosition();
                if (selection <= 0 || attempt == null) return;
                endAttempt(OBSERVATIONS[selection]); choose();
            });
            save.setEnabled(false);
            outcome.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { save.setEnabled(position > 0); }
                @Override public void onNothingSelected(AdapterView<?> parent) { save.setEnabled(false); }
            });
            button(actions, R.string.dev_probe_stop, () -> stop("Stopped by user"));
            arm(CARD_MS); listener.changed();
        } catch (Exception e) {
            if (attempt != null) try { attempt.put("error", e.toString()); } catch (JSONException ignored) { }
            endAttempt("show_failed"); choose();
        }
    }

    private static final String[] OBSERVATIONS = { "unconfirmed", "hud", "instrument_cluster", "rear_passenger", "main_screen", "other", "not_seen" };
    private String observationLabel(String value) {
        String[] labels = activity.getResources().getStringArray(R.array.dev_display_observations);
        for (int i = 0; i < OBSERVATIONS.length; i++) if (OBSERVATIONS[i].equals(value)) return labels[i];
        if ("timed_out_unconfirmed".equals(value)) return activity.getString(R.string.dev_display_timed_out);
        if ("show_failed".equals(value)) return activity.getString(R.string.dev_display_show_failed);
        return activity.getString(R.string.dev_display_unconfirmed);
    }

    private void laidOut(ImageView image, JSONObject target, Button confirm) {
        image.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                if (closed || attempt != target) { image.getViewTreeObserver().removeOnPreDrawListener(this); return true; }
                if (image.isShown() && image.getWidth() > 0 && image.getHeight() > 0) {
                    image.getViewTreeObserver().removeOnPreDrawListener(this);
                    try { target.put("android_layout_ready", true).put("view_width", image.getWidth()).put("view_height", image.getHeight()); }
                    catch (JSONException ignored) { }
                    if (confirm != null) confirm.setEnabled(true);
                    listener.changed();
                }
                return true;
            }
        });
    }
    private void arm(long duration) { deadline = SystemClock.elapsedRealtime() + duration; main.removeCallbacks(timer); timer.run(); }
    private void endAttempt(String observation) {
        main.removeCallbacks(timer); dismissPresentation();
        if (attempt == null) return;
        try {
            attempt.put("observation", observation).put("ended_at_ms", System.currentTimeMillis())
                    .put("elapsed_ms", SystemClock.elapsedRealtime() - attempt.optLong("started_elapsed_ms"));
        } catch (JSONException ignored) { }
        attempt = null; listener.changed();
    }
    void stop(String reason) {
        if (closed) return;
        endAttempt("cancelled_unconfirmed"); closed = true; main.removeCallbacks(timer);
        host.removeView(panel);
        for (int i = 0; i < background.size(); i++) background.get(i).setVisibility(backgroundVisibility.get(i));
        background.clear(); backgroundVisibility.clear();
        try { report.put("end_reason", reason); } catch (JSONException ignored) { }
        listener.changed(); listener.finished(reason);
    }
    private void dismissPresentation() {
        if (presentation != null) {
            Presentation old = presentation; presentation = null;
            old.setOnDismissListener(null);
            try { old.dismiss(); } catch (Exception ignored) { }
        }
    }
    private TextView text(LinearLayout parent, String value, int style) {
        TextView text = new TextView(activity); text.setTextAppearance(style); text.setText(value); parent.addView(text); return text;
    }
    private WrappingActionRow actions(LinearLayout parent) {
        WrappingActionRow row = new WrappingActionRow(activity, null); parent.addView(row, new LinearLayout.LayoutParams(-1, -2)); return row;
    }
    private Button button(ViewGroup parent, int label, Runnable action) {
        Button button = new Button(activity); button.setText(label); button.setOnClickListener(v -> action.run()); parent.addView(button); return button;
    }
    private static Bitmap card(int id, int nativeW, int nativeH) {
        double scale = Math.min(1, Math.min(1280.0 / nativeW, 800.0 / nativeH));
        int w = Math.max(80, (int) (nativeW * scale)), h = Math.max(60, (int) (nativeH * scale));
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap); canvas.drawColor(Color.BLACK);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.CYAN); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(Math.max(4, h / 40f));
        canvas.drawRect(4, 4, w - 4, h - 4, paint); paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.CENTER);
        String label = "DISPLAY " + id;
        paint.setTextSize(Math.min(h * .32f, w / 5f));
        while (paint.measureText(label) > w * .9f) paint.setTextSize(paint.getTextSize() * .9f);
        canvas.drawText(label, w / 2f, h * .49f, paint);
        paint.setColor(Color.WHITE); paint.setTextSize(Math.min(h * .12f, w / 15f));
        canvas.drawText(nativeW + " × " + nativeH, w / 2f, h * .73f, paint);
        return bitmap;
    }
}
