package com.mathstrokes.user.dto;

import java.time.Instant;

/**
 * A student as the teacher sees them. Carries no password or security-answer material of any
 * kind - those fields are not mapped anywhere outside the service layer.
 */
public record StudentSummaryResponse(
        Long id,
        String fullName,
        String phoneNumber,
        boolean enabled,
        Instant lastLoginAt,
        Instant registeredAt,
        long attemptCount) {
}
