package com.mathstrokes.attempt.dto;

import java.util.List;

import com.mathstrokes.common.enums.AttemptStatus;
import com.mathstrokes.common.enums.ExamPattern;

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
        String chapterName,
        ExamPattern examPattern,
        AttemptStatus status,
        int totalQuestions,
        int durationMinutes,
        AttemptTimingResponse timing,
        long clientSequence,
        List<AttemptQuestionResponse> questions) {
}
