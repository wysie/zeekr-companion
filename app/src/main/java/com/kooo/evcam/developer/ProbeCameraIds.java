package com.kooo.evcam.developer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/** Bounded discovery only. A candidate is not a claim that a camera exists or is accessible. */
public final class ProbeCameraIds {
    public static final int MAX_CANDIDATES = 48;
    public static final int NUMERIC_END = 15;
    private ProbeCameraIds() { }
    public static List<String> candidates(Collection<String> advertised, Collection<String> reported) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        add(ids, advertised); add(ids, reported);
        for (int i = 0; i <= NUMERIC_END && ids.size() < MAX_CANDIDATES; i++) ids.add(String.valueOf(i));
        return new ArrayList<>(ids);
    }
    private static void add(LinkedHashSet<String> out, Collection<String> values) {
        for (String value : values) {
            if (out.size() >= MAX_CANDIDATES) break;
            if (value != null && !value.trim().isEmpty() && value.length() <= 128) out.add(value);
        }
    }
}
