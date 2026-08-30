package com.mathstrokes.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * The student dashboard summary.
 *
 * {@code recentScores} is ordered oldest to newest so a chart can plot it directly without the
 * client having to reverse it.
 */
public record StudentPerformanceResponse(
        long testsTaken,
        long testsInProgress,
        BigDecimal averageScorePercentage,
        BigDecimal bestScorePercentage,
        BigDecimal averageAccuracy,
        Integer bestRank,
        List<ScorePoint> recentScores,
        List<ChapterPerformanceResponse> chapterBreakdown) {

    public record ScorePoint(Long attemptId, String testTitle, java.time.Instant takenAt,
                             BigDecimal score, BigDecimal maxScore,
                             BigDecimal scorePercentage) {
    }
}
