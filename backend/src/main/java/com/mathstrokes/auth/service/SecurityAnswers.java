package com.mathstrokes.auth.service;

/**
 * Normalisation applied to a security answer before it is hashed, and again before it is checked.
 *
 * Applied on both sides so the comparison is case-insensitive and forgiving about spacing: a
 * student who typed "St Xavier's" at registration should not be locked out months later for
 * typing "st xaviers " with different capitalisation and a trailing space.
 */
public final class SecurityAnswers {

    private SecurityAnswers() {
    }

    public static String normalise(String answer) {
        return answer == null ? "" : answer.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
