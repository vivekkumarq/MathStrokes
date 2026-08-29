package com.mathstrokes.attempt.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mathstrokes.common.enums.AttemptStatus;
import com.mathstrokes.common.enums.ExamPattern;

/**
 * The result dashboard payload.
 *
 * accuracy    = correct / attempted x 100   (0 when nothing was attempted)
 * attemptRate = attempted / total x 100
 *
 * A partially correct answer counts as attempted but not as correct, so accuracy reads as the
 * share of attempts that were fully right.
 */
public record AttemptResultResponse(
        Long attemptId,
        Long testId,
        String testTitle,
        String subjectName,
        String chapterName,
        ExamPattern examPattern,
        AttemptStatus status,
        Instant startedAt,
        Instant submittedAt,
        Integer timeTakenSeconds,
        int durationMinutes,
        int totalQuestions,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal negativeMarks,
        Integer correctCount,
        Integer partiallyCorrectCount,
        Integer incorrectCount,
        Integer unansweredCount,
        Integer attemptedCount,
        BigDecimal accuracy,
        BigDecimal attemptRate,
        boolean rankingEnabled,
        Integer rankPosition,
        Integer totalCandidates,
        BigDecimal percentile) {
}
