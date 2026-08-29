package com.mathstrokes.marking.strategy;

import java.math.BigDecimal;

import com.mathstrokes.common.enums.QuestionResultStatus;

/**
 * Outcome of scoring one question.
 *
 * @param status              how the answer is classified for reporting
 * @param marksAwarded        signed marks, negative when a penalty applied
 * @param maxMarks            marks a perfect answer to this question was worth
 * @param selectedOptionCount how many options the student selected
 * @param correctOptionCount  how many of those selections were in the answer key
 */
public record AnswerEvaluation(
        QuestionResultStatus status,
        BigDecimal marksAwarded,
        BigDecimal maxMarks,
        int selectedOptionCount,
        int correctOptionCount) {

    public boolean isAttempted() {
        return status != QuestionResultStatus.UNANSWERED;
    }

    public boolean isNegative() {
        return marksAwarded.signum() < 0;
    }
}
