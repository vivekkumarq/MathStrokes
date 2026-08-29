package com.mathstrokes.auth.dto;

import java.time.Instant;
import java.util.List;

/** The authenticated user's own profile. Carries no hash of any kind. */
public record UserProfileResponse(
        Long id,
        String fullName,
        String phoneNumber,
        List<String> roles,
        boolean enabled,
        Instant createdAt) {
}
