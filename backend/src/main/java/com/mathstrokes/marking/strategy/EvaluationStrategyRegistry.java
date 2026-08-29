package com.mathstrokes.marking.strategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.common.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

/**
 * Resolves the strategy for a question type. Built from whatever {@link EvaluationStrategy}
 * beans exist, so adding a question type is a matter of adding one class.
 *
 * A duplicate registration fails fast at startup rather than silently picking one at random.
 */
@Component
public class EvaluationStrategyRegistry {

    private final Map<QuestionType, EvaluationStrategy> strategies =
            new EnumMap<>(QuestionType.class);

    public EvaluationStrategyRegistry(List<EvaluationStrategy> availableStrategies) {
        for (EvaluationStrategy strategy : availableStrategies) {
            EvaluationStrategy existing = strategies.put(strategy.supportedType(), strategy);
            if (existing != null) {
                throw new IllegalStateException("Two evaluation strategies claim "
                        + strategy.supportedType() + ": " + existing.getClass().getName()
                        + " and " + strategy.getClass().getName());
            }
        }
    }

    public EvaluationStrategy forType(QuestionType questionType) {
        EvaluationStrategy strategy = strategies.get(questionType);
        if (strategy == null) {
            throw new BusinessRuleException(
                    "No evaluation strategy is registered for question type " + questionType);
        }
        return strategy;
    }

    public boolean supports(QuestionType questionType) {
        return strategies.containsKey(questionType);
    }
}
