package com.mathstrokes.marking.strategy;

import java.util.Set;

import com.mathstrokes.common.enums.QuestionResultStatus;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.marking.entity.MarkingConfig;
import org.springframework.stereotype.Component;

/**
 * Exactly one option is correct.
 *
 * A selection of more than one option cannot arise through the API (the answer service rejects
 * it), but if a stored answer ever contained several it is scored as incorrect rather than
 * silently treated as a match, which is the conservative reading.
 */
@Component
public class SingleCorrectEvaluationStrategy implements EvaluationStrategy {

    @Override
    public QuestionType supportedType() {
        return QuestionType.SINGLE_CORRECT;
    }

    @Override
    public AnswerEvaluation evaluate(Set<Long> correctOptionIds, Set<Long> selectedOptionIds,
                                     MarkingConfig config) {
        if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
            return new AnswerEvaluation(QuestionResultStatus.UNANSWERED, config.unansweredMarks(),
                    config.maxMarks(), 0, 0);
        }
        long matched = selectedOptionIds.stream().filter(correctOptionIds::contains).count();
        boolean correct = selectedOptionIds.size() == 1 && matched == 1;
        return new AnswerEvaluation(
                correct ? QuestionResultStatus.CORRECT : QuestionResultStatus.INCORRECT,
                correct ? config.fullCorrectMarks() : config.wrongMarks(),
                config.maxMarks(),
                selectedOptionIds.size(),
                (int) matched);
    }
}
