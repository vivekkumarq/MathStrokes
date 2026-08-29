package com.mathstrokes.common.dto;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** The single error envelope returned by every failing endpoint. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorItem> fieldErrors) {

    public static ApiErrorResponse of(int status, String error, String message, String path,
                                      List<FieldErrorItem> fieldErrors) {
        return new ApiErrorResponse(Instant.now(), status, error, message, path,
                fieldErrors == null ? List.of() : fieldErrors);
    }
}
