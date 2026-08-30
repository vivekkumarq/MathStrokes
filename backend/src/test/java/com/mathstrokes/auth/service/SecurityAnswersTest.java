package com.mathstrokes.auth.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAnswersTest {

    @Test
    @DisplayName("casing and surrounding space do not change the answer")
    void caseAndSpaceInsensitive() {
        assertThat(SecurityAnswers.normalise("  Delhi  "))
                .isEqualTo(SecurityAnswers.normalise("delhi"));
        assertThat(SecurityAnswers.normalise("DELHI")).isEqualTo("delhi");
    }

    @Test
    @DisplayName("runs of internal whitespace collapse to a single space")
    void collapsesInternalWhitespace() {
        assertThat(SecurityAnswers.normalise("New    Delhi")).isEqualTo("new delhi");
        assertThat(SecurityAnswers.normalise("New\tDelhi")).isEqualTo("new delhi");
    }

    @Test
    @DisplayName("a null answer normalises to empty rather than throwing")
    void nullBecomesEmpty() {
        assertThat(SecurityAnswers.normalise(null)).isEmpty();
    }

    @Test
    @DisplayName("genuinely different answers stay different")
    void differentAnswersStayDifferent() {
        assertThat(SecurityAnswers.normalise("Delhi"))
                .isNotEqualTo(SecurityAnswers.normalise("Mumbai"));
    }
}
