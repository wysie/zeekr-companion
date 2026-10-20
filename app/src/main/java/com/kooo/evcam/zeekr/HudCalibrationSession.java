package com.kooo.evcam.zeekr;

import android.app.Activity;
import android.app.Presentation;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import com.kooo.evcam.AppConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.UUID;
import java.lang.ref.WeakReference;

/** Temporary public-HUD marker. No camera, crop, saved bounds or recording changes. */
final class HudCalibrationSession {
    private static WeakReference<HudCalibrationSession> owner = new WeakReference<>(null);
    static boolean active() { return owner.get() != null; }
    final String id = UUID.randomUUID().toString();
    final Display display;
    final Marker marker;
    private final Activity activity;
    private final Presentation presentation;
    private final Runnable ended;
    private boolean closed, positioned;
    int x, y, size = 200, color = Color.RED;
    private int drawnX, drawnY, drawnSize, drawnColor;
    private boolean drawn;
    HudCalibrationSession(Activity activity, Display display, Runnable layoutChanged, Runnable ended) {
        if (active()) throw new IllegalStateException("HUD test already running");
        this.activity = activity; this.display = display; this.ended = ended;
        presentation = new Presentation(activity, display);
        marker = new Marker(presentation.getContext());
        marker.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            if (r-l != or-ol || b-t != ob-ot) {
                size = HudCalibrationGeometry.size(size, r-l, b-t);
                if (!positioned && r > l && b > t) {
                    positioned = true; x = (r-l-size)/2; y = (b-t-size)/2;
                }
                x = HudCalibrationGeometry.position(x, r-l, size);
                y = HudCalibrationGeometry.position(y, b-t, size);
                layoutChanged.run(); marker.invalidate();
            }
        });
        presentation.setContentView(marker);
        Window window = presentation.getWindow();
        if (window == null) throw new IllegalStateException("HUD window unavailable");
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        presentation.setOnDismissListener(d -> close());
    }
    void start() {
        owner = new WeakReference<>(this);
        try {
            SideViewPopupService.hudCalibrationChanged();
            presentation.show();
            presentation.getWindow().setLayout(-1, -1);
        } catch (RuntimeException e) { close(); throw e; }
    }
    void move(int x, int y, int size, int color) {
        this.size = HudCalibrationGeometry.size(size, marker.getWidth(), marker.getHeight());
        this.x = HudCalibrationGeometry.position(x, marker.getWidth(), this.size);
        this.y = HudCalibrationGeometry.position(y, marker.getHeight(), this.size);
        this.color = color; marker.invalidate();
    }
    private String version() {
        try { return activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName; }
        catch (android.content.pm.PackageManager.NameNotFoundException e) { return "unknown"; }
    }
    JSONObject observation(String outcome) throws JSONException {
        if (!drawn || drawnX != x || drawnY != y || drawnSize != size || drawnColor != color
                || closed || marker.getWidth() <= 0 || marker.getHeight() <= 0 || !presentation.isShowing())
            throw new IllegalStateException("HUD marker not ready");
        Point real = new Point(); display.getRealSize(real);
        int[] origin = new int[2]; marker.getLocationOnScreen(origin);
        AppConfig config = new AppConfig(activity);
        return new JSONObject().put("session", id).put("time_ms", System.currentTimeMillis())
                .put("version", version()).put("observation", outcome)
                .put("evidence", "User observation of physical HUD; Android cannot detect optical/factory masking")
                .put("display_id", display.getDisplayId()).put("display_name", display.getName())
                .put("display_flags", display.getFlags()).put("display_width", real.x).put("display_height", real.y)
                .put("viewport_width", marker.getWidth()).put("viewport_height", marker.getHeight())
                .put("viewport_origin_x", origin[0]).put("viewport_origin_y", origin[1])
                .put("x", x).put("y", y).put("size", size).put("color_argb", color)
                .put("logical_visible_rect", new JSONArray(HudCalibrationGeometry.visible(x, y, size, marker.getWidth(), marker.getHeight())))
                .put("hud_height_percent", config.getHudSideSizePercent())
                .put("hud_shape_percent", config.getHudSideShapePercent()).put("hud_left_percent", config.getHudSideLeftPercent())
                .put("hud_vertical_percent", config.getHudSideVerticalPercent()).put("hud_trim_percent", config.getHudSideInnerCropPercent())
                .put("hud_mirrored", config.isHudSideMirrored());
    }
    void close() {
        if (closed) return;
        closed = true; presentation.setOnDismissListener(null);
        try { presentation.dismiss(); } finally {
            if (owner.get() == this) owner.clear();
            SideViewPopupService.hudCalibrationChanged(); ended.run();
        }
    }
    final class Marker extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Marker(android.content.Context context) { super(context); setBackgroundColor(Color.TRANSPARENT); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setStyle(Paint.Style.FILL); paint.setColor(color);
            canvas.drawRect(x, y, x + size, y + size, paint);
            int ink = color == Color.RED ? Color.WHITE : Color.BLACK;
            paint.setColor(ink); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(4);
            canvas.drawRect(x+2, y+2, x+size-2, y+size-2, paint);
            paint.setColor(Color.BLACK); paint.setStrokeWidth(2);
            canvas.drawLine(x, y+size/2f, x+size, y+size/2f, paint);
            canvas.drawLine(x+size/2f, y, x+size/2f, y+size, paint);
            paint.setStyle(Paint.Style.FILL); paint.setColor(ink);
            paint.setTextAlign(Paint.Align.CENTER); paint.setTextSize(Math.max(12, size/7f));
            canvas.drawText("L", x+size*.12f, y+size*.45f, paint);
            canvas.drawText("R", x+size*.88f, y+size*.45f, paint);
            canvas.drawText("T", x+size*.5f, y+size*.18f, paint);
            canvas.drawText("B", x+size*.5f, y+size*.9f, paint);
            drawn = true; drawnX = x; drawnY = y; drawnSize = size; drawnColor = color;
        }
    }
}
