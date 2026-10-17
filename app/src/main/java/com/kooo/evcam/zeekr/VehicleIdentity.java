package com.kooo.evcam.zeekr;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative model-name parser; hardware configuration is handled by its callers. */
public final class VehicleIdentity {
    public enum Model { UNKNOWN, ZEEKR_009, ZEEKR_7X }
    private static final Pattern NAMED_MODEL = Pattern.compile(
            "\\bzeekr[ _-]*(009|7x)(?![a-z0-9])", Pattern.CASE_INSENSITIVE);
    private VehicleIdentity() {}

    public static Model resolve(String selection, String... reportedNames) {
        if ("009".equals(selection)) return Model.ZEEKR_009;
        if ("7x".equals(selection)) return Model.ZEEKR_7X;
        Model result = Model.UNKNOWN;
        if (reportedNames == null) return result;
        for (String name : reportedNames) {
            if (name == null) continue;
            String text = name.trim().toLowerCase(Locale.ROOT);
            if (text.startsWith("极氪")) text = "zeekr " + text.substring(2);
            Model found = model(text);
            if (found == Model.UNKNOWN) {
                Matcher match = NAMED_MODEL.matcher(text);
                while (match.find()) {
                    Model candidate = model(match.group(1));
                    if (found != Model.UNKNOWN && found != candidate) return Model.UNKNOWN;
                    found = candidate;
                }
            }
            if (found != Model.UNKNOWN) {
                if (result != Model.UNKNOWN && result != found) return Model.UNKNOWN;
                result = found;
            }
        }
        return result;
    }

    private static Model model(String value) {
        return "009".equals(value) ? Model.ZEEKR_009
                : "7x".equals(value) ? Model.ZEEKR_7X : Model.UNKNOWN;
    }
}
