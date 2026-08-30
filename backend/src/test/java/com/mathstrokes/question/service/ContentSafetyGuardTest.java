package com.mathstrokes.question.service;

import java.util.List;

import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.common.exception.ValidationException;
import com.mathstrokes.question.dto.QuestionOptionRequest;
import com.mathstrokes.question.dto.QuestionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The guard has to refuse markup without refusing mathematics. Both halves matter: a false
 * positive here would stop a teacher writing an inequality.
 */
class ContentSafetyGuardTest {

    private final QuestionValidator validator = new QuestionValidator();

    private static QuestionRequest request(String stem, String solution, String optionContent) {
        return new QuestionRequest(1L, ExamPattern.JEE_MAIN, Difficulty.EASY,
                QuestionType.SINGLE_CORRECT, stem, solution, null,
                List.of(new QuestionOptionRequest("A", optionContent, 0, true),
                        new QuestionOptionRequest("B", "$2$", 1, false)));
    }

    @Test
    @DisplayName("ordinary LaTeX passes untouched")
    void latexPasses() {
        assertThatCode(() -> validator.validateForSave(request(
                "Evaluate $$\\int_0^1 x^2 \\, dx$$", "The answer is $\\frac{1}{3}$",
                "$\\frac{1}{3}$"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("inequalities are mathematics, not markup, and must still be accepted")
    void inequalitiesPass() {
        assertThatCode(() -> validator.validateForSave(request(
                "Given $a < b$ and $b > c$, which holds?", "Since $a < b < c$ the claim follows.",
                "$a < c$"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a script tag in the stem is refused")
    void scriptTagInStemRejected() {
        assertThatThrownBy(() -> validator.validateForSave(request(
                "Solve <script>steal()</script>", "$1$", "$1$")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("could not be saved");
    }

    @Test
    @DisplayName("a script tag hidden in an option is refused, spacing and casing notwithstanding")
    void scriptTagInOptionRejected() {
        assertThatThrownBy(() -> validator.validateForSave(request(
                "$x = 1$", "$1$", "< SCRIPT >alert(1)</script>")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("an inline event handler is refused")
    void eventHandlerRejected() {
        assertThatThrownBy(() -> validator.validateForSave(request(
                "<img src=x onerror=alert(1)>", "$1$", "$1$")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("a javascript: URL is refused, wherever it appears")
    void javascriptUrlRejected() {
        assertThatThrownBy(() -> validator.validateForSave(request(
                "$x$", "See javascript:alert(1) for details", "$1$")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("the error names the field that carried the markup")
    void errorNamesTheField() {
        assertThatThrownBy(() -> validator.validateForSave(request(
                "$x$", "<iframe src=evil></iframe>", "$1$")))
                .isInstanceOfSatisfying(ValidationException.class, ex ->
                        assertThat(ex.getFieldErrors()).anySatisfy(error ->
                                assertThat(error.field()).isEqualTo("solutionContent")));
    }
}
