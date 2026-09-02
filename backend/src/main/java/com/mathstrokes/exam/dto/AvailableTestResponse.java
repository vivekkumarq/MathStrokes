package com.mathstrokes.exam.dto;

import java.time.Instant;

import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.TestKind;

/**
 * What a student sees when browsing tests. Carries no question data at all - the paper is only
 * revealed once an attempt has been created.
 */
public record AvailableTestResponse(
        Long id,
        String title,
        String description,
        String subjectName,
        /** Absent for a full-syllabus test. Absence is the signal, not a sentinel value. */
        Long chapterId,
        String chapterName,
        ExamPattern examPattern,
        int durationMinutes,
        int questionCount,
        boolean rankingEnabled,
        int maxAttemptsPerStudent,
        int attemptsUsed,
        boolean canStart,
        Long activeAttemptId,
        String unavailableReason,
        /** Lets the client group class tests separately without inferring it from the schedule. */
        TestKind testKind,
        /**
         * The window in which this paper may be STARTED, either bound absent for unbounded.
         * Present so the client can show a countdown; it is not the gate. canStart is computed
         * on the server and the server refuses a start outside the window regardless.
         */
        Instant scheduledStartAt,
        Instant scheduledEndAt) {
}
