package com.mathstrokes.marking.strategy;

import java.math.BigDecimal;
import java.util.Set;

import com.mathstrokes.common.enums.QuestionResultStatus;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.marking.entity.MarkingConfig;
import com.mathstrokes.marking.entity.PartialCreditMode;
import org.springframework.stereotype.Component;

/**
 * One or more options may be correct.
 *
 * The rule ladder, in order:
 *   nothing selected                      -> unansweredMarks, UNANSWERED
 *   any option outside the key selected   -> wrongMarks, INCORRECT   (checked before partial credit)
 *   the key selected exactly              -> fullCorrectMarks, CORRECT
 *   a strict subset of the key, no errors -> partial credit, PARTIALLY_CORRECT
 *
 * With the seeded JEE Advanced configuration this reproduces the official scheme: +4 exact,
 * +1 per correct option capped at +3, -2 for any wrong selection, 0 for no attempt.
 */
@Component
public class MultipleCorrectEvaluationStrategy implements EvaluationStrategy {

    @Override
    public QuestionType supportedType() {
        return QuestionType.MULTIPLE_CORRECT;
    }

    @Override
    public AnswerEvaluation evaluate(Set<Long> correctOptionIds, Set<Long> selectedOptionIds,
                                     MarkingConfig config) {
        if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
            return new AnswerEvaluation(QuestionResultStatus.UNANSWERED, config.unansweredMarks(),
                    config.maxMarks(), 0, 0);
        }

        int selectedCount = selectedOptionIds.size();
        int matchedCount = (int) selectedOptionIds.stream().filter(correctOptionIds::contains).count();
        boolean selectedSomethingWrong = matchedCount < selectedCount;

        if (selectedSomethingWrong) {
            return new AnswerEvaluation(QuestionResultStatus.INCORRECT, config.wrongMarks(),
                    config.maxMarks(), selectedCount, matchedCount);
        }

        if (matchedCount == correctOptionIds.size()) {
            return new AnswerEvaluation(QuestionResultStatus.CORRECT, config.fullCorrectMarks(),
                    config.maxMarks(), selectedCount, matchedCount);
        }

        // Every selection is in the key, but the key is not fully covered.
        if (config.partialCreditMode() != PartialCreditMode.PER_CORRECT_OPTION) {
            return new AnswerEvaluation(QuestionResultStatus.INCORRECT, config.noPartialCreditMarks(),
                    config.maxMarks(), selectedCount, matchedCount);
        }

        BigDecimal earned = config.marksPerCorrectOption().multiply(BigDecimal.valueOf(matchedCount));
        BigDecimal awarded = earned.min(config.maxPartialMarks());
        return new AnswerEvaluation(QuestionResultStatus.PARTIALLY_CORRECT, awarded,
                config.maxMarks(), selectedCount, matchedCount);
    }
}
