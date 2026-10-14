package com.kooo.evcam.zeekr;

import java.util.List;
import java.util.Locale;

/** HUD detection adapted from Ferix PR14. Never infer the 009's display role from an ID alone. */
public final class HudDisplayLocator {
    public static final int NONE = -1;
    public static final int DEFAULT_DISPLAY_ID = 0;
    public static final int HUD_WIDTH = 1440;
    public static final int HUD_HEIGHT = 480;
    private static final int PRESENTATION = 0x8;
    private static final int PRIVATE = 0x4;

    private HudDisplayLocator() { }

    public static final class Candidate {
        public final int id, width, height, flags;
        public final String name;

        public Candidate(int id, int width, int height) {
            this(id, width, height, "", PRESENTATION);
        }

        public Candidate(int id, int width, int height, String name, int flags) {
            this.id = id;
            this.width = width;
            this.height = height;
            this.name = name == null ? "" : name;
            this.flags = flags;
        }
    }

    public static int find(List<Candidate> displays) {
        return find(displays, false);
    }

    /** Explicit owner-selected 009 mapping; do not infer a vehicle from an anonymous display. */
    public static int find(List<Candidate> displays, boolean zeekr009) {
        int chosen = NONE;
        for (Candidate c : displays) {
            if (!eligible(c) || !(zeekr009 ? matches009(c) : knownSize(c) || namedHud(c))) continue;
            if (chosen != NONE && chosen != c.id) return NONE; // Ambiguous: use the screen.
            chosen = c.id;
        }
        return chosen;
    }

    private static boolean matches009(Candidate c) {
        String name = c.name.toLowerCase(Locale.ROOT);
        return c.id == 3 && c.width == 1280 && c.height == 640
                && !name.contains("rear") && !name.contains("passenger")
                && !name.contains("后排") && !name.contains("乘客");
    }

    public static int findBySize(List<Candidate> displays) {
        int chosen = NONE;
        for (Candidate c : displays) {
            if (!eligible(c) || !knownSize(c)) continue;
            if (chosen != NONE && chosen != c.id) return NONE;
            chosen = c.id;
        }
        return chosen;
    }

    private static boolean eligible(Candidate c) {
        String name = c.name.toLowerCase(Locale.ROOT);
        return c.id != DEFAULT_DISPLAY_ID && c.width > 0 && c.height > 0
                && (c.flags & PRESENTATION) != 0 && (c.flags & PRIVATE) == 0
                && !(c.width == 2560 && c.height == 536)
                && !name.contains("cluster") && !name.contains("仪表");
    }

    private static boolean knownSize(Candidate c) { return c.width == HUD_WIDTH && c.height == HUD_HEIGHT; }

    private static boolean namedHud(Candidate c) {
        String name = c.name.toLowerCase(Locale.ROOT);
        return name.matches(".*\\bhud\\b.*") || name.contains("head-up") || name.contains("head up")
                || name.contains("抬头");
    }
}
