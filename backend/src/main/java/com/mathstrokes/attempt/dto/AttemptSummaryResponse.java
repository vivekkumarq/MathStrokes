package com.mathstrokes.attempt.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mathstrokes.common.enums.AttemptStatus;
import com.mathstrokes.common.enums.ExamPattern;

/** A row in the student's attempt history. */
public record AttemptSummaryResponse(
        Long attemptId,
        Long testId,
        String testTitle,
        String chapterName,
        ExamPattern examPattern,
        AttemptStatus status,
        Instant startedAt,
        Instant submittedAt,
        BigDecimal score,
        BigDecimal maxScore,
        Integer rankPosition,
        Integer totalCandidates,
        BigDecimal percentile,
        int totalQuestions) {
}
