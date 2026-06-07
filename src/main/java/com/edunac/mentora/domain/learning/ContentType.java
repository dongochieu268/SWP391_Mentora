package com.edunac.mentora.domain.learning;

public enum ContentType {
    TEXT,
    VIDEO,
    FILE,
    LINK;

    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            ContentType.valueOf(value.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
