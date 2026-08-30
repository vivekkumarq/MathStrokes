package com.mathstrokes.question.service;

import java.util.List;

import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.common.exception.ValidationException;
import com.mathstrokes.question.dto.QuestionOptionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionValidatorTest {

    private final QuestionValidator validator = new QuestionValidator();

    private static QuestionOptionRequest option(String key, boolean correct) {
        return new QuestionOptionRequest(key, "$x = 1$", 0, correct);
    }

    @Test
    @DisplayName("a single-correct question with exactly one key publishes")
    void validSingleCorrect() {
        assertThatCode(() -> validator.validateForPublish(QuestionType.SINGLE_CORRECT,
                List.of(option("A", true), option("B", false), option("C", false),
                        option("D", false)))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a single-correct question with two keys cannot be published")
    void singleCorrectRejectsTwoKeys() {
        assertThatThrownBy(() -> validator.validateForPublish(QuestionType.SINGLE_CORRECT,
                List.of(option("A", true), option("B", true), option("C", false))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not ready to be published");
    }

    @Test
    @DisplayName("a single-correct question with no key cannot be published")
    void singleCorrectRejectsNoKey() {
        assertThatThrownBy(() -> validator.validateForPublish(QuestionType.SINGLE_CORRECT,
                List.of(option("A", false), option("B", false))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("a multiple-correct question with several keys publishes")
    void validMultipleCorrect() {
        assertThatCode(() -> validator.validateForPublish(QuestionType.MULTIPLE_CORRECT,
                List.of(option("A", true), option("B", true), option("C", false),
                        option("D", false)))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a multiple-correct question where every option is correct is unanswerable")
    void multipleCorrectRejectsAllCorrect() {
        assertThatThrownBy(() -> validator.validateForPublish(QuestionType.MULTIPLE_CORRECT,
                List.of(option("A", true), option("B", true))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("a question with fewer than two options cannot be published")
    void rejectsTooFewOptions() {
        assertThatThrownBy(() -> validator.validateForPublish(QuestionType.SINGLE_CORRECT,
                List.of(option("A", true))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("duplicate option labels are rejected")
    void rejectsDuplicateLabels() {
        assertThatThrownBy(() -> validator.validateForPublish(QuestionType.SINGLE_CORRECT,
                List.of(option("A", true), option("A", false), option("B", false))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("validation errors name the request field, so a form can bind them")
    void errorsNameTheRequestField() {
        assertThatThrownBy(() -> validator.validateForPublish(QuestionType.SINGLE_CORRECT,
                List.of(option("A", true), option("B", true))))
                .isInstanceOfSatisfying(ValidationException.class, ex ->
                        org.assertj.core.api.Assertions.assertThat(ex.getFieldErrors())
                                .isNotEmpty()
                                .allSatisfy(error -> org.assertj.core.api.Assertions
                                        .assertThat(error.field()).isEqualTo("options")));
    }
}
