package com.mathstrokes.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LatexExcerptTest {

    @Test
    @DisplayName("short content is returned whole, whitespace collapsed")
    void shortContent() {
        assertThat(LatexExcerpt.of("Find  $x$\nif\n$x^2 = 4$", 160)).isEqualTo("Find $x$ if $x^2 = 4$");
        assertThat(LatexExcerpt.of(null, 160)).isEmpty();
    }

    @Test
    @DisplayName("a cut outside any formula stays where it is")
    void plainCut() {
        assertThat(LatexExcerpt.of("$a$ then some long plain words", 12)).isEqualTo("$a$ then som...");
    }

    @Test
    @DisplayName("a cut inside an inline formula moves to just before it")
    void inlineFormula() {
        assertThat(LatexExcerpt.of("Circle $O: x^2 + y^2 - 2x = 0$ is given", 12)).isEqualTo("Circle...");
    }

    @Test
    @DisplayName("a cut inside a display formula moves to just before it")
    void displayFormula() {
        assertThat(LatexExcerpt.of("Given below: $$x^2 + y^2 = 1 \\quad \\text{and} \\quad x = 1$$", 30))
                .isEqualTo("Given below:...");
    }

    @Test
    @DisplayName("a formula that opens the text is kept whole rather than leaving nothing")
    void leadingFormula() {
        assertThat(LatexExcerpt.of("$\\frac{a}{b} + \\frac{c}{d}$ is a sum of fractions", 8))
                .isEqualTo("$\\frac{a}{b} + \\frac{c}{d}$...");
    }

    @Test
    @DisplayName("an escaped dollar is text, not a delimiter")
    void escapedDollar() {
        assertThat(LatexExcerpt.of("It costs \\$5 and \\$7 for $x$ items in total", 14))
                .isEqualTo("It costs \\$5 a...");
    }

    @Test
    @DisplayName("the questions that showed raw LaTeX in the bank now end on a whole formula")
    void reportedQuestions() {
        String q1549 = "For the four circles M, N, O and P, the following four equations are given:\n"
                + "Circle $M: x^2 + y^2 = 1$\nCircle $N: x^2 + y^2 - 2x = 0$\n"
                + "Circle $O: x^2 + y^2 - 2x - 2y + 1 = 0$\nCircle $P: x^2 + y^2 - 2y = 0$";
        assertThat(LatexExcerpt.of(q1549, 160)).endsWith("2x = 0$ Circle...");

        String q1547 = "Choose the incorrect statement about the two circles whose equations are given below:\n"
                + "$$x^2 + y^2 - 10x - 10y + 41 = 0 \\quad \\text{and} \\quad x^2 + y^2 - 16x - 10y + 80 = 0$$";
        assertThat(LatexExcerpt.of(q1547, 160)).endsWith("given below:...");
    }
}
