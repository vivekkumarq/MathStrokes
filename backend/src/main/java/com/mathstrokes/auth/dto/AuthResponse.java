package com.mathstrokes.auth.dto;

/**
 * Issued on login and on refresh.
 *
 * The refresh token is rotated on every refresh, so the client must replace both values each
 * time rather than reusing the original refresh token.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserProfileResponse user) {

    public static AuthResponse of(String accessToken, String refreshToken, long expiresInSeconds,
                                  UserProfileResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresInSeconds, user);
    }
}
