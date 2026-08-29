package com.mathstrokes.ranking.dto;

import java.math.BigDecimal;

/**
 * A leaderboard row. Carries the student's display name only - never a phone number, so the
 * board cannot be mined for contact details.
 */
public record LeaderboardEntryResponse(
        Integer rankPosition,
        String studentName,
        BigDecimal score,
        BigDecimal maxScore,
        Integer correctCount,
        Integer incorrectCount,
        Integer timeTakenSeconds,
        BigDecimal percentile,
        boolean isCurrentUser) {
}
