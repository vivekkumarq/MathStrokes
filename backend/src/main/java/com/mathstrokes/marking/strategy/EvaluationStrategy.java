package com.mathstrokes.marking.strategy;

import java.util.Set;

import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.marking.entity.MarkingConfig;

/**
 * Scores a single question. One implementation per {@link QuestionType}.
 *
 * Implementations must be pure: given the same key, selection and configuration they must return
 * the same result forever. That is what lets an old attempt be re-evaluated identically years
 * later from its stored snapshot.
 *
 * To support a new question type, add an implementation and register it as a Spring bean;
 * {@code EvaluationStrategyRegistry} picks it up automatically.
 */
public interface EvaluationStrategy {

    QuestionType supportedType();

    /**
     * @param correctOptionIds  the answer key, taken from the attempt snapshot
     * @param selectedOptionIds what the student selected; empty means unattempted
     * @param config            the marking configuration snapshotted onto the attempt question
     */
    AnswerEvaluation evaluate(Set<Long> correctOptionIds, Set<Long> selectedOptionIds,
                              MarkingConfig config);
}
