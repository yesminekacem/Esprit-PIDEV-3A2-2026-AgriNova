package tn.esprit.utils;

public class Validators {

    public static String normalizeSpaces(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ");
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static void requireLength(String value, int min, int max, String fieldName) {
        String v = value == null ? "" : value.trim();
        if (v.length() < min) throw new IllegalArgumentException(fieldName + " must be at least " + min + " characters.");
        if (v.length() > max) throw new IllegalArgumentException(fieldName + " must be at most " + max + " characters.");
    }

    public static void requireNotBlank(String value, String fieldName) {
        if (isBlank(value)) throw new IllegalArgumentException(fieldName + " is required.");
    }
}
