package com.mathstrokes.attempt.dto;

import java.math.BigDecimal;
import java.util.List;

import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.QuestionResultStatus;
import com.mathstrokes.common.enums.QuestionType;

/**
 * Post-submission review of one question. This is the only place the answer key and the worked
 * solution ever reach a student, and only for their own evaluated attempt.
 */
public record QuestionReviewResponse(
        Long attemptQuestionId,
        int questionOrder,
        QuestionType questionType,
        Difficulty difficulty,
        String questionContent,
        String solutionContent,
        List<ReviewOptionResponse> options,
        List<Long> selectedOptionIds,
        List<Long> correctOptionIds,
        QuestionResultStatus resultStatus,
        BigDecimal marksAwarded,
        BigDecimal maxMarks) {

    /** Carries the answer key, unlike the live-attempt option record. */
    public record ReviewOptionResponse(Long id, String optionKey, String content,
                                       int displayOrder, boolean isCorrect, boolean selected) {
    }
}
