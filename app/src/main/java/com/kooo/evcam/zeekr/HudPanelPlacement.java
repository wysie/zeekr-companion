package com.kooo.evcam.zeekr;

/**
 * HUD 上侧视画面的位置和大小。
 *
 * <p>高度是 HUD 高度的百分比，宽度 = 高度 × 形状（宽 ÷ 高 ×100），最多一整个 HUD 宽；
 * 左右位置是「离打灯那一边的距离」：左灯的画面离左边这么远，右灯的画面离右边这么远，
 * 都是剩余宽度里的百分比，0 贴边、50 居中、100 贴对面（用户 2026-10-14：「左灯左边、右灯右边」）；
 * 上下位置同样是剩余高度里的百分比，0 贴顶、100 贴底。
 * 固定位置默认保持本分支原有设置；启用跟随转向灯时才从对应边量。</p>
 *
 * <p>纯 Java，不碰 Android，见 {@code HudPanelPlacementTest}。</p>
 */
public final class HudPanelPlacement {

    /** 再小就看不清了（像素）。 */
    public static final int MIN_SIZE_PX = 48;
    public static final int MAX_INNER_CROP_PERCENT = 60;

    private HudPanelPlacement() {
    }

    /** 窗口在 HUD 里的位置（像素）。 */
    public static final class Rect {
        public final int left;
        public final int top;
        public final int width;
        public final int height;

        Rect(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }
    }

    /** Fixed-position compatibility path, measured from the left. */
    public static Rect compute(int width, int height, int size, int shape, int left, int top) {
        return compute(width, height, size, shape, left, top, false);
    }

    /**
     * @param edgePercent 离打灯那一边多远（剩余宽度的百分比）
     * @param rightSide   右灯：从右边量；否则从左边量
     */
    public static Rect compute(int hudWidth, int hudHeight, int sizePercent, int shapePercent,
                               int edgePercent, int verticalPercent, boolean rightSide) {
        if (hudWidth <= 0 || hudHeight <= 0) throw new IllegalArgumentException("Missing HUD viewport");
        int height = Math.round(hudHeight * sizePercent / 100f);
        height = Math.max(Math.min(MIN_SIZE_PX, hudHeight), Math.min(height, hudHeight));
        int width = Math.round(height * shapePercent / 100f);
        if (width > hudWidth) {
            height = Math.max(1, Math.round(height * (hudWidth / (float) width)));
            width = hudWidth; // Fit, preserving shape instead of squeezing the picture.
        }
        width = Math.max(Math.min(MIN_SIZE_PX, hudWidth), Math.min(width, hudWidth));
        int fromEdge = Math.round(Math.max(0, hudWidth - width) * clampPercent(edgePercent) / 100f);
        int left = rightSide ? hudWidth - width - fromEdge : fromEdge;
        int top = Math.round(Math.max(0, hudHeight - height) * clampPercent(verticalPercent) / 100f);
        return new Rect(left, top, width, height);
    }

    private static int clampPercent(int percent) {
        return Math.max(0, Math.min(100, percent));
    }

    /** Visible bounds and the retained part of the original, unscaled camera picture. */
    public static final class Crop {
        public final Rect bounds;
        public final int pictureWidth;
        public final int pictureOffsetX;

        private Crop(Rect bounds, int pictureWidth, int pictureOffsetX) {
            this.bounds = bounds;
            this.pictureWidth = pictureWidth;
            this.pictureOffsetX = pictureOffsetX;
        }
    }

    /** Car-body edge in the final mirrored picture; independent of where the HUD panel sits. */
    public static boolean keepsRightEdge(int lane, boolean mirrored) {
        if (lane != LaneCycle.LEFT && lane != LaneCycle.RIGHT) throw new IllegalArgumentException("Not a side camera");
        return (lane == LaneCycle.LEFT) == mirrored;
    }

    /**
     * Retain the car-body edge of the displayed picture, trimming the road-side edge.
     * Panel bounds still shrink away from the HUD centre to retain the saved outer
     * placement, but source offset follows camera side/mirroring, not panel position.
     */
    public static Crop cropRoadEdge(Rect panel, int hudWidth, int percent, int lane, boolean mirrored) {
        int removed = Math.min(Math.max(0, panel.width - MIN_SIZE_PX),
                Math.round(panel.width * Math.max(0, Math.min(MAX_INNER_CROP_PERCENT, percent)) / 100f));
        boolean onRight = panel.left * 2 + panel.width >= hudWidth;
        int boundsOffset = onRight ? removed : 0;
        int pictureOffset = keepsRightEdge(lane, mirrored) ? removed : 0;
        return new Crop(new Rect(panel.left + boundsOffset, panel.top, panel.width - removed, panel.height),
                panel.width, pictureOffset);
    }
}
