package com.mealup.common.validation;

/** Tiny helpers for text typed into forms. */
public final class Text {

    private Text() { }

    /** Trims; an empty or missing value becomes null so optional fields stay clean in the database. */
    public static String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
