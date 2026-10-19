package com.kooo.evcam.zeekr;

/** Temporary expand/restore geometry; never writes the normal window preferences. */
public final class SideViewExpansion {
    public static final class Geometry {
        public final int x, y, width, height;

        public Geometry(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private Geometry original;
    private boolean halfScreen;

    public boolean expanded() {
        return original != null;
    }

    public Geometry toggle(Geometry current, int screenWidth, int screenHeight, int margin) {
        return toggle(current, screenWidth, screenHeight, margin, false);
    }

    /** Blind-side/360 expansion ignores the saved popup aspect; restore retains it exactly. */
    public Geometry toggleHalfScreen(Geometry current, int screenWidth, int screenHeight, int margin) {
        return toggle(current, screenWidth, screenHeight, margin, true);
    }

    private Geometry toggle(Geometry current, int screenWidth, int screenHeight, int margin, boolean halfScreen) {
        if (expanded()) {
            return restore();
        }
        original = current;
        this.halfScreen = halfScreen;
        return refit(screenWidth, screenHeight, margin);
    }

    /** Refit a temporary expansion after system bars/display bounds change; retain its restore point. */
    public Geometry refit(int screenWidth, int screenHeight, int margin) {
        if (original == null) return null;
        if (!halfScreen) return fit(original, screenWidth, screenHeight, margin);
        int height = Math.max(1, screenHeight - 2 * margin);
        int width = Math.min(height, Math.max(1, (screenWidth - 2 * margin) / 2));
        boolean right = original.x + original.width / 2d >= screenWidth / 2d;
        return new Geometry(right ? Math.max(margin, screenWidth - margin - width) : margin,
                margin, width, height);
    }

    private static Geometry fit(Geometry current, int screenWidth, int screenHeight, int margin) {
        int maxWidth = Math.max(1, screenWidth - 2 * margin);
        int maxHeight = Math.max(1, screenHeight - 2 * margin);
        double scale = Math.min((double) maxWidth / Math.max(1, current.width),
                (double) maxHeight / Math.max(1, current.height));
        int width = Math.min(maxWidth, Math.max(1, (int) Math.round(current.width * scale)));
        int height = Math.min(maxHeight, Math.max(1, (int) Math.round(current.height * scale)));
        int x = Math.max(margin, Math.min(screenWidth - margin - width,
                current.x + (current.width - width) / 2));
        int y = Math.max(margin, Math.min(screenHeight - margin - height,
                current.y + (current.height - height) / 2));
        return new Geometry(x, y, width, height);
    }

    public Geometry restore() {
        Geometry result = original;
        original = null;
        return result;
    }
}
