package com.mathstrokes.attempt.dto;

import java.util.List;

import com.mathstrokes.common.enums.AttemptStatus;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.TestKind;

/**
 * Everything the exam screen needs in one call.
 *
 * Returned on start and on resume. Because the whole paper comes down at once, a refresh
 * repaints from this single response and the student never waits between questions.
 */
public record ActiveAttemptResponse(
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
        int totalQuestions,
        int durationMinutes,
        AttemptTimingResponse timing,
        long clientSequence,
        List<AttemptQuestionResponse> questions) {
}
