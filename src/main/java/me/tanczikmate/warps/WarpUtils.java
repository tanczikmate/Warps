package me.tanczikmate.warps;

import java.text.Normalizer;
import java.util.Locale;

public final class WarpUtils {

    private WarpUtils() {}

    /**
     * Strips diacritical marks / accents from a string (e.g. "főtér" -> "foter").
     */
    public static String stripAccents(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    /**
     * Trims and converts a string to lowercase using Locale.ROOT.
     */
    public static String normalizeKey(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase(Locale.ROOT);
    }
}
