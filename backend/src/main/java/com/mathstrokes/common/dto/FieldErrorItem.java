package com.mathstrokes.common.dto;

import java.util.Set;

/**
 * One field-level validation failure.
 *
 * {@code field} is the JSON property name of the request body, so a client form can bind the
 * message straight onto the offending input without any mapping.
 *
 * {@code rejectedValue} is echoed back to help a user see what went wrong - except for secret
 * fields, where echoing it would write the password into the response body, the browser network
 * log and very likely a log aggregator. Those come back with a null value instead.
 */
public record FieldErrorItem(String field, String message, Object rejectedValue) {

    private static final Set<String> SECRET_FIELDS = Set.of(
            "password", "confirmPassword", "newPassword", "currentPassword",
            "securityAnswer", "resetToken", "refreshToken", "accessToken");

    public FieldErrorItem {
        if (field != null && SECRET_FIELDS.contains(field)) {
            rejectedValue = null;
        }
    }

    public static FieldErrorItem of(String field, String message) {
        return new FieldErrorItem(field, message, null);
    }
}
