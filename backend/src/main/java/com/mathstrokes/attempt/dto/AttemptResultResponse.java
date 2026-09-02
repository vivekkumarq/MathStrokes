package com.mathstrokes.attempt.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mathstrokes.common.enums.AttemptStatus;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.TestKind;

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
        /** Absent for a full-syllabus paper and for a cross-chapter class test. */
        String chapterName,
        /**
         * Lets the client name the paper instead of falling back to its chapter. A hand-picked
         * class test spanning several chapters carries no chapterId, and "Full syllabus" - the
         * right label for a practice paper with no chapter - is a lie about a three-question
         * class test.
         */
        TestKind testKind,
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
