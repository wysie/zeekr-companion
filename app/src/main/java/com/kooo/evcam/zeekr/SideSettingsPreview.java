package com.kooo.evcam.zeekr;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.camera.CameraManagerHolder;
import com.kooo.evcam.camera.CameraSlots;
import com.kooo.evcam.camera.MultiCameraManager;
import com.kooo.evcam.camera.SingleCamera;
import com.kooo.evcam.screen.ScreenState;
import com.kooo.evcam.telemetry.Readings;
import com.kooo.evcam.telemetry.Signal;
import com.kooo.evcam.telemetry.Telemetry;

/** Read-only, bounded settings sampler of the existing native preview. No camera declarations,
 * holders, sessions, popup services or saved values are changed by opening/switching this view.
 * Five samples/second is for tuning; the actual popup keeps its existing native frame rate. */
public final class SideSettingsPreview extends LinearLayout {
    private final MainActivity activity;
    private final AppConfig config;
    private final Picture picture;
    private final FrameLayout stage;
    private final TextView message;
    private final MaterialButton show, expand;
    private final java.util.function.Consumer<Boolean> onExpand;
    private final CameraFrameFreshness freshness = new CameraFrameFreshness();
    private Bitmap sample;
    private CompositeStreamGeometry.Plan plan;
    private boolean running, hidden, expanded;
    private int lane = LaneCycle.LEFT;
    private long copiedTimestamp;
    private SurfaceTexture copiedIdentity;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            sample();
            postDelayed(this, 200);
        }
    };

    public SideSettingsPreview(MainActivity activity, java.util.function.Consumer<Boolean> onExpand) {
        super(activity);
        this.activity = activity; this.onExpand = onExpand; config = new AppConfig(activity);
        setId(R.id.side_settings_preview); setOrientation(VERTICAL);
        setPadding(dp(16), dp(12), dp(20), dp(12));
        LinearLayout heading = new LinearLayout(activity); heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(R.string.side_settings_preview, R.style.TextAppearance_Cam_SettingsBody);
        heading.addView(title, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
        TabLayout sides = new TabLayout(new android.view.ContextThemeWrapper(activity, R.style.ThemeOverlay_Cam_SettingsTabs)); sides.setId(R.id.side_preview_left_right);
        sides.setTabMode(TabLayout.MODE_SCROLLABLE);
        sides.setTabTextColors(ContextCompat.getColor(activity, R.color.text_secondary), ContextCompat.getColor(activity, R.color.text_primary));
        sides.setSelectedTabIndicatorColor(ContextCompat.getColor(activity, R.color.energy));
        sides.addTab(sides.newTab().setText(R.string.zeekr_lane_left));
        sides.addTab(sides.newTab().setText(R.string.zeekr_lane_right));
        sides.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                lane = tab.getPosition() == 0 ? LaneCycle.LEFT : LaneCycle.RIGHT;
                picture.invalidate();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        heading.addView(sides, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addView(heading, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        stage = new PreviewStage(); stage.setBackgroundColor(Color.BLACK);
        picture = new Picture(); picture.setId(R.id.side_preview_picture);
        stage.addView(picture, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        message = text(R.string.side_view_waiting_live, R.style.TextAppearance_Cam_SettingsCaption);
        message.setId(R.id.side_preview_status);
        message.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary));
        message.setPadding(0, dp(8), 0, dp(8));
        message.setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE);
        // Status spans the pane, so a narrow/square preview cannot clip a long explanation.
        addView(message, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        expand = icon(R.drawable.ic_side_expand, R.string.side_settings_expand);
        expand.setId(R.id.side_preview_expand);
        FrameLayout.LayoutParams expandAt = new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.START);
        stage.addView(expand, expandAt);
        expand.setOnClickListener(v -> {
            expanded = !expanded;
            expand.setIconResource(expanded ? R.drawable.ic_main_restore : R.drawable.ic_side_expand);
            expand.setContentDescription(getContext().getString(expanded ? R.string.side_settings_restore : R.string.side_settings_expand));
            onExpand.accept(expanded);
        });
        MaterialButton close = icon(R.drawable.ic_side_close, R.string.side_settings_close);
        close.setId(R.id.side_preview_close);
        stage.addView(close, new FrameLayout.LayoutParams(dp(48), dp(48), Gravity.TOP | Gravity.END));
        close.setOnClickListener(v -> hidePreview());
        LayoutParams stageAt = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1);
        stageAt.gravity = Gravity.CENTER_HORIZONTAL;
        addView(stage, stageAt);
        show = new MaterialButton(activity);
        show.setText(R.string.side_settings_show); show.setVisibility(GONE);
        addView(show, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        show.setOnClickListener(v -> { hidden = false; stage.setVisibility(VISIBLE); show.setVisibility(GONE); sample(); });
        TextView hint = text(R.string.side_settings_unchanged, R.style.TextAppearance_Cam_SettingsMicro);
        hint.setPadding(0, dp(10), 0, 0); hint.setTextColor(ContextCompat.getColor(activity, R.color.text_secondary));
        addView(hint, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    }

    private void hidePreview() {
        hidden = true; stage.setVisibility(GONE); show.setVisibility(VISIBLE);
        if (expanded) { expanded = false; expand.setIconResource(R.drawable.ic_side_expand);
            expand.setContentDescription(getContext().getString(R.string.side_settings_expand)); onExpand.accept(false); }
        clear(); message.setVisibility(GONE);
    }

    public void start() { running = true; clear(); removeCallbacks(tick); post(tick); }
    public void stop() { running = false; removeCallbacks(tick); clear(); sample = null; plan = null; }
    @Override protected void onDetachedFromWindow() { stop(); super.onDetachedFromWindow(); }

    private void clear() {
        freshness.reset(); copiedIdentity = null; copiedTimestamp = 0;
        picture.invalidate(); message.setVisibility(VISIBLE);
    }

    private void unavailable(int text) {
        clear();
        if (!getContext().getString(text).contentEquals(message.getText())) message.setText(text);
    }

    private void sample() {
        if (!running || hidden || !isShown()) return;
        Readings readings = Telemetry.get().readings();
        boolean factory = "R".equals(readings.text(Signal.GEAR)) || Boolean.TRUE.equals(readings.bool(Signal.STOCK_360))
                || Boolean.TRUE.equals(readings.bool(Signal.STOCK_POPUP)) || Boolean.TRUE.equals(readings.bool(Signal.PARK_ASSIST));
        if (ScreenState.dark() || !Telemetry.get().carLinkConnected() || factory) {
            unavailable(R.string.side_settings_paused); return;
        }
        TextureView source = activity.settingsPreviewTexture();
        if (source == null) { unavailable(R.string.side_settings_unavailable); return; }
        if (PreviewDewarp.isActive(source)) { unavailable(R.string.side_settings_gpu); return; }
        MultiCameraManager manager = CameraManagerHolder.getInstance().getCameraManager();
        SingleCamera camera = manager != null ? manager.getCamera(CameraSlots.KEY_SURROUND) : null;
        android.util.Size size = camera != null ? camera.getPreviewSize() : null;
        if (!source.isAvailable() || size == null || !CompositeStreamGeometry.looksLikeComposite(
                camera.getCameraId(), size.getWidth(), size.getHeight())) {
            unavailable(R.string.side_view_waiting_live); return;
        }
        try {
            SurfaceTexture identity = source.getSurfaceTexture();
            long timestamp = identity == null ? 0 : identity.getTimestamp();
            long now = SystemClock.elapsedRealtime();
            freshness.observe(identity, timestamp, now);
            if (!freshness.fresh(now)) { message.setText(R.string.side_view_waiting_live); message.setVisibility(VISIBLE); picture.invalidate(); return; }
            if (identity != copiedIdentity || timestamp != copiedTimestamp) {
                // At most 512 px per lane and 2048 px along the strip; one reusable allocation.
                float scale = Math.min(1f, 2048f / Math.max(size.getWidth(), size.getHeight()));
                int width = Math.max(1, Math.round(size.getWidth() * scale));
                int height = Math.max(1, Math.round(size.getHeight() * scale));
                if (sample == null || sample.getWidth() != width || sample.getHeight() != height)
                    sample = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                sample.eraseColor(Color.TRANSPARENT);
                if (source.getBitmap(sample) == null || source.getSurfaceTexture() != identity
                        || sample.getPixel(width / 2, height / 2) >>> 24 != 255) {
                    unavailable(R.string.side_view_waiting_live); return;
                }
                plan = CompositeStreamGeometry.analyse(camera.getCameraId(), size.getWidth(), size.getHeight());
                copiedIdentity = identity; copiedTimestamp = timestamp;
            }
            message.setVisibility(GONE); picture.invalidate();
        } catch (RuntimeException | OutOfMemoryError failure) {
            unavailable(R.string.side_view_waiting_live);
        }
    }

    /** Fit the whole window shape, including its controls, instead of adding black side bars. */
    private final class PreviewStage extends FrameLayout {
        PreviewStage() { super(activity); }
        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int availableWidth = MeasureSpec.getSize(widthSpec), availableHeight = MeasureSpec.getSize(heightSpec);
            float aspect = (float) SidePopupSizing.aspect(config.getSidePopupAspect());
            int width = Math.min(availableWidth, Math.round(availableHeight * aspect));
            int height = Math.min(availableHeight, Math.round(width / aspect));
            super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        }
    }

    private final class Picture extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final FisheyeMesh mesh = new FisheyeMesh();
        private final SideViewFraming framing = new SideViewFraming();
        private final Matrix transform = new Matrix();
        private final RectF source = new RectF(), target = new RectF();
        private final CameraLabel label = new CameraLabel(getContext(), true);
        private final SideTurnArrow arrow = new SideTurnArrow(getContext(), true);
        Picture() { super(activity); setBackgroundColor(Color.BLACK); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (sample == null || plan == null || !freshness.fresh(SystemClock.elapsedRealtime())) return;
            float aspect = (float) SidePopupSizing.aspect(config.getSidePopupAspect());
            int width = Math.min(getWidth(), Math.round(getHeight() * aspect));
            int height = Math.min(getHeight(), Math.round(width / aspect));
            if (width <= 0 || height <= 0) return;
            int save = canvas.save(); canvas.translate((getWidth() - width) / 2f, (getHeight() - height) / 2f);
            boolean straighten = config.isSidePopupStraighten();
            int rotation = config.getSidePopupRotation(lane);
            int inner = SideViewRotation.begin(canvas, straighten ? 0 : rotation, width, height, config.isSidePopupMirrored());
            if (straighten) {
                framing.configure(config.getSidePopupFov(), config.getSidePopupYaw(), config.getSidePopupPitch(), lane,
                        (float) width / height, rotation, config.isSidePopupAutoFit());
                RearViewGeometry.ShaderRects rect = RearViewGeometry.toShaderRects(plan, lane, RearViewGeometry.Viewport.full());
                mesh.prepare(SideViewFraming.meshDivisions(width, height), rect.laneOffsetX * width,
                        rect.laneOffsetY * height, rect.laneScaleX * width, rect.laneScaleY * height, framing::sourcePoint);
                target.set(0, 0, width, height);
                FisheyeMesh.Painter draw = c -> c.drawBitmap(sample, null, target, paint);
                if (config.isSidePopupAutoFit()) mesh.draw(canvas, 0, 0, width, height, draw);
                else mesh.drawClippedToLane(canvas, 0, 0, width, height, draw);
            } else {
                RearViewGeometry.Viewport aim = SideViewAim.viewport(lane, Math.round(180f / config.getSidePopupFov() * 100),
                        Math.round(config.getSidePopupYaw() / SideViewProjection.MAX_YAW_DEGREES * 100),
                        Math.round(config.getSidePopupPitch() / SideViewProjection.MAX_PITCH_DEGREES * 100));
                RearViewGeometry.Viewport crop = RearViewGeometry.Viewport.forWindow(width, height, .5f);
                float[] rect = RearViewGeometry.combinedSourceRect(plan, lane, new RearViewGeometry.Viewport(
                        aim.x + crop.x * aim.width, aim.y + crop.y * aim.height, crop.width * aim.width, crop.height * aim.height));
                source.set(rect[0] * sample.getWidth(), rect[1] * sample.getHeight(),
                        (rect[0] + rect[2]) * sample.getWidth(), (rect[1] + rect[3]) * sample.getHeight());
                target.set(0, 0, width, height); transform.setRectToRect(source, target, Matrix.ScaleToFit.FILL);
                canvas.concat(transform); canvas.drawBitmap(sample, 0, 0, paint);
            }
            canvas.restoreToCount(inner);
            if (config.isRearViewCameraLabelEnabled()) label.drawBottom(canvas, lane, width, height);
            arrow.draw(canvas, lane, null, width, height);
            canvas.restoreToCount(save);
        }
    }
    private MaterialButton icon(int icon, int description) {
        MaterialButton button = new MaterialButton(activity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setIconResource(icon); button.setIconSize(dp(24)); button.setIconPadding(0);
        button.setContentDescription(getContext().getString(description)); button.setMinWidth(0); button.setMinimumWidth(0);
        button.setPadding(dp(10), 0, dp(10), 0); button.setInsetTop(0); button.setInsetBottom(0); button.setCornerRadius(dp(4));
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.side_control_background)));
        button.setIconTint(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.side_control_icon)));
        return button;
    }
    private TextView text(int value, int appearance) {
        TextView text = new TextView(activity); text.setTextAppearance(appearance); text.setText(value); return text;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
