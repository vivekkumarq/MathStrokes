package com.mathstrokes.auth.dto;

/**
 * Step 2 of recovery. The token is single-use and short-lived, and authorises exactly one
 * password change for the account that answered the challenge.
 */
public record PasswordResetTokenResponse(String resetToken, long expiresInSeconds) {
}
