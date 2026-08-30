package com.mathstrokes.marking.strategy;

import java.math.BigDecimal;
import java.util.Set;

import com.mathstrokes.common.enums.QuestionResultStatus;
import com.mathstrokes.marking.entity.MarkingConfig;
import com.mathstrokes.marking.entity.PartialCreditMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SingleCorrectEvaluationStrategyTest {

    private final SingleCorrectEvaluationStrategy strategy = new SingleCorrectEvaluationStrategy();

    /** The seeded JEE Main scheme: +4 correct, -1 wrong, 0 unattempted. */
    private static final MarkingConfig JEE_MAIN = new MarkingConfig(
            new BigDecimal("4.00"), new BigDecimal("-1.00"), BigDecimal.ZERO,
            PartialCreditMode.NONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    @Test
    @DisplayName("the correct option scores full marks")
    void correctAnswerScoresFullMarks() {
        AnswerEvaluation result = strategy.evaluate(Set.of(10L), Set.of(10L), JEE_MAIN);

        assertThat(result.status()).isEqualTo(QuestionResultStatus.CORRECT);
        assertThat(result.marksAwarded()).isEqualByComparingTo("4.00");
        assertThat(result.correctOptionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("a wrong option attracts the negative mark")
    void wrongAnswerScoresNegative() {
        AnswerEvaluation result = strategy.evaluate(Set.of(10L), Set.of(11L), JEE_MAIN);

        assertThat(result.status()).isEqualTo(QuestionResultStatus.INCORRECT);
        assertThat(result.marksAwarded()).isEqualByComparingTo("-1.00");
        assertThat(result.isNegative()).isTrue();
    }

    @Test
    @DisplayName("no selection scores zero and is not treated as an attempt")
    void unansweredScoresZero() {
        AnswerEvaluation result = strategy.evaluate(Set.of(10L), Set.of(), JEE_MAIN);

        assertThat(result.status()).isEqualTo(QuestionResultStatus.UNANSWERED);
        assertThat(result.marksAwarded()).isEqualByComparingTo("0.00");
        assertThat(result.isAttempted()).isFalse();
    }

    @Test
    @DisplayName("a null selection is treated the same as an empty one")
    void nullSelectionIsUnanswered() {
        AnswerEvaluation result = strategy.evaluate(Set.of(10L), null, JEE_MAIN);

        assertThat(result.status()).isEqualTo(QuestionResultStatus.UNANSWERED);
    }

    @Test
    @DisplayName("selecting several options on a single-correct question is wrong, not correct")
    void multipleSelectionsCannotScore() {
        // The API rejects this, but a stored answer that somehow held two selections must not be
        // scored as correct just because one of them happens to match the key.
        AnswerEvaluation result = strategy.evaluate(Set.of(10L), Set.of(10L, 11L), JEE_MAIN);

        assertThat(result.status()).isEqualTo(QuestionResultStatus.INCORRECT);
        assertThat(result.marksAwarded()).isEqualByComparingTo("-1.00");
    }

    @Test
    @DisplayName("marks come from the configuration, not from constants in the code")
    void marksFollowConfiguration() {
        MarkingConfig advanced = new MarkingConfig(
                new BigDecimal("3.00"), new BigDecimal("-1.00"), BigDecimal.ZERO,
                PartialCreditMode.NONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(strategy.evaluate(Set.of(1L), Set.of(1L), advanced).marksAwarded())
                .isEqualByComparingTo("3.00");
    }
}
