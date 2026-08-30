package com.mathstrokes.marking.strategy;

import java.math.BigDecimal;
import java.util.Set;

import com.mathstrokes.common.enums.QuestionResultStatus;
import com.mathstrokes.marking.entity.MarkingConfig;
import com.mathstrokes.marking.entity.PartialCreditMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MultipleCorrectEvaluationStrategyTest {

    private final MultipleCorrectEvaluationStrategy strategy =
            new MultipleCorrectEvaluationStrategy();

    /** The seeded JEE Advanced scheme: +4 exact, +1 per correct option capped at +3, -2 wrong. */
    private static final MarkingConfig ADVANCED_PARTIAL = new MarkingConfig(
            new BigDecimal("4.00"), new BigDecimal("-2.00"), BigDecimal.ZERO,
            PartialCreditMode.PER_CORRECT_OPTION,
            new BigDecimal("1.00"), new BigDecimal("3.00"), BigDecimal.ZERO);

    private static final MarkingConfig EXACT_MATCH_ONLY = new MarkingConfig(
            new BigDecimal("4.00"), new BigDecimal("-1.00"), BigDecimal.ZERO,
            PartialCreditMode.NONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

    private static final Set<Long> KEY = Set.of(1L, 2L, 3L);

    @Test
    @DisplayName("the exact key scores full marks")
    void exactMatchScoresFull() {
        AnswerEvaluation result = strategy.evaluate(KEY, Set.of(1L, 2L, 3L), ADVANCED_PARTIAL);

        assertThat(result.status()).isEqualTo(QuestionResultStatus.CORRECT);
        assertThat(result.marksAwarded()).isEqualByComparingTo("4.00");
    }

    @Test
    @DisplayName("no selection scores zero")
    void noSelectionScoresZero() {
        AnswerEvaluation result = strategy.evaluate(KEY, Set.of(), ADVANCED_PARTIAL);

        assertThat(result.status()).isEqualTo(QuestionResultStatus.UNANSWERED);
        assertThat(result.marksAwarded()).isEqualByComparingTo("0.00");
    }

    @Nested
    @DisplayName("partial credit")
    class PartialCredit {

        @Test
        @DisplayName("one correct option out of three scores +1")
        void oneOfThree() {
            AnswerEvaluation result = strategy.evaluate(KEY, Set.of(1L), ADVANCED_PARTIAL);

            assertThat(result.status()).isEqualTo(QuestionResultStatus.PARTIALLY_CORRECT);
            assertThat(result.marksAwarded()).isEqualByComparingTo("1.00");
        }

        @Test
        @DisplayName("two correct options out of three score +2")
        void twoOfThree() {
            AnswerEvaluation result = strategy.evaluate(KEY, Set.of(1L, 2L), ADVANCED_PARTIAL);

            assertThat(result.status()).isEqualTo(QuestionResultStatus.PARTIALLY_CORRECT);
            assertThat(result.marksAwarded()).isEqualByComparingTo("2.00");
        }

        @Test
        @DisplayName("partial credit is capped, so a partial answer never matches a perfect one")
        void partialCreditIsCapped() {
            Set<Long> bigKey = Set.of(1L, 2L, 3L, 4L, 5L);
            AnswerEvaluation result =
                    strategy.evaluate(bigKey, Set.of(1L, 2L, 3L, 4L), ADVANCED_PARTIAL);

            // Four correct selections would earn +4 uncapped, equalling a perfect answer.
            assertThat(result.marksAwarded()).isEqualByComparingTo("3.00");
            assertThat(result.status()).isEqualTo(QuestionResultStatus.PARTIALLY_CORRECT);
        }

        @Test
        @DisplayName("with partial credit switched off a subset scores the no-credit value")
        void subsetWithoutPartialCredit() {
            AnswerEvaluation result = strategy.evaluate(KEY, Set.of(1L, 2L), EXACT_MATCH_ONLY);

            assertThat(result.status()).isEqualTo(QuestionResultStatus.INCORRECT);
            assertThat(result.marksAwarded()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("a wrong selection is penalised before partial credit is considered")
    class WrongSelection {

        @Test
        @DisplayName("one wrong option alongside two correct ones still scores -2")
        void wrongOptionOverridesPartialCredit() {
            AnswerEvaluation result =
                    strategy.evaluate(KEY, Set.of(1L, 2L, 99L), ADVANCED_PARTIAL);

            assertThat(result.status()).isEqualTo(QuestionResultStatus.INCORRECT);
            assertThat(result.marksAwarded()).isEqualByComparingTo("-2.00");
        }

        @Test
        @DisplayName("selecting the whole key plus one extra is wrong, not correct")
        void supersetOfKeyIsWrong() {
            AnswerEvaluation result =
                    strategy.evaluate(KEY, Set.of(1L, 2L, 3L, 99L), ADVANCED_PARTIAL);

            assertThat(result.status()).isEqualTo(QuestionResultStatus.INCORRECT);
            assertThat(result.marksAwarded()).isEqualByComparingTo("-2.00");
        }

        @Test
        @DisplayName("only wrong options selected scores the penalty")
        void allWrongScoresPenalty() {
            AnswerEvaluation result = strategy.evaluate(KEY, Set.of(98L, 99L), ADVANCED_PARTIAL);

            assertThat(result.status()).isEqualTo(QuestionResultStatus.INCORRECT);
            assertThat(result.marksAwarded()).isEqualByComparingTo("-2.00");
            assertThat(result.correctOptionCount()).isZero();
        }
    }

    @Test
    @DisplayName("a single-answer key is handled like any other")
    void singleOptionKey() {
        AnswerEvaluation result = strategy.evaluate(Set.of(7L), Set.of(7L), ADVANCED_PARTIAL);

        assertThat(result.status()).isEqualTo(QuestionResultStatus.CORRECT);
        assertThat(result.marksAwarded()).isEqualByComparingTo("4.00");
    }
}
