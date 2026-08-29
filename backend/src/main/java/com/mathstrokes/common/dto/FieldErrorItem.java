package com.mathstrokes.common.dto;

public record FieldErrorItem(String field, String message, Object rejectedValue) {

    public static FieldErrorItem of(String field, String message) {
        return new FieldErrorItem(field, message, null);
    }
}
