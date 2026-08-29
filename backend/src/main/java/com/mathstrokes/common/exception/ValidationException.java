package com.mathstrokes.common.exception;

import java.util.List;

import com.mathstrokes.common.dto.FieldErrorItem;

/** Raised by domain validators (e.g. question publish rules) that produce field-level detail. */
public class ValidationException extends ApiException {

    private final transient List<FieldErrorItem> fieldErrors;

    public ValidationException(String message, List<FieldErrorItem> fieldErrors) {
        super(ErrorCode.VALIDATION_ERROR, message);
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }

    public ValidationException(String message) {
        this(message, List.of());
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }
}
