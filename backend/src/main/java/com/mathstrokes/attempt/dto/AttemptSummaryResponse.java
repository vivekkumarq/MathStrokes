package com.mathstrokes.attempt.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.mathstrokes.common.enums.AttemptStatus;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.TestKind;

/** A row in the student's attempt history. */
public record AttemptSummaryResponse(
        Long attemptId,
        Long testId,
        String testTitle,
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
        BigDecimal score,
        BigDecimal maxScore,
        Integer rankPosition,
        Integer totalCandidates,
        BigDecimal percentile,
        int totalQuestions) {
}
