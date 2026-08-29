package com.mathstrokes.common.enums;

/**
 * Only types with a registered {@code EvaluationStrategy} are declared here.
 * To add NUMERICAL / INTEGER / MATCHING / ASSERTION_REASON / COMPREHENSION:
 * add the constant, implement an {@code EvaluationStrategy} for it, add a
 * {@code MarkingScheme} row, and extend {@code QuestionValidator}. Nothing else changes.
 */
public enum QuestionType {
    SINGLE_CORRECT,
    MULTIPLE_CORRECT
}
