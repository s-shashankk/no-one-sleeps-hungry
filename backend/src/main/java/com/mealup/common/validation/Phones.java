package com.mealup.common.validation;

import java.util.regex.Pattern;

/** Phone numbers are stored without spaces, dashes or brackets, e.g. +919876543210. */
public final class Phones {

    /** Loose pattern for Bean Validation on request bodies; the stricter check runs after normalising. */
    public static final String INPUT_PATTERN = "^\\+?[0-9 ()\\-]{10,20}$";

    private static final Pattern NORMALISED = Pattern.compile("^\\+?[0-9]{10,15}$");

    private Phones() { }

    public static String normalise(String raw) {
        return raw == null ? null : raw.replaceAll("[ ()\\-]", "");
    }

    public static boolean isValid(String normalised) {
        return normalised != null && NORMALISED.matcher(normalised).matches();
    }

    /** For searching: keeps digits, plus and letters so "98765 43" still finds "+919876543210". */
    public static String normaliseForSearch(String raw) {
        return raw == null ? "" : raw.replaceAll("[ ()\\-]", "");
    }
}
