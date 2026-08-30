package com.mathstrokes.marking.entity;

import java.math.BigDecimal;

import com.mathstrokes.common.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkingConfigTest {

    private static MarkingConfig config(String full, String wrong, PartialCreditMode mode,
                                        String perOption, String maxPartial) {
        return new MarkingConfig(new BigDecimal(full), new BigDecimal(wrong), BigDecimal.ZERO,
                mode, new BigDecimal(perOption), new BigDecimal(maxPartial), BigDecimal.ZERO);
    }

    @Test
    @DisplayName("omitted optional fields default to zero rather than blowing up later")
    void optionalFieldsDefault() {
        MarkingConfig parsed = new MarkingConfig(new BigDecimal("4"), new BigDecimal("-1"),
                BigDecimal.ZERO, null, null, null, null);

        assertThat(parsed.partialCreditMode()).isEqualTo(PartialCreditMode.NONE);
        assertThat(parsed.marksPerCorrectOption()).isEqualByComparingTo("0");
        assertThat(parsed.maxPartialMarks()).isEqualByComparingTo("0");
        assertThat(parsed.noPartialCreditMarks()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("the seeded JEE Advanced configuration is valid")
    void advancedConfigurationIsValid() {
        assertThatCode(() -> config("4", "-2", PartialCreditMode.PER_CORRECT_OPTION, "1", "3")
                .validate()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a non-positive full mark is rejected")
    void fullMarksMustBePositive() {
        assertThatThrownBy(() -> config("0", "-1", PartialCreditMode.NONE, "0", "0").validate())
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("fullCorrectMarks");
    }

    @Test
    @DisplayName("a positive penalty is rejected, since it would reward a wrong answer")
    void wrongMarksCannotBePositive() {
        assertThatThrownBy(() -> config("4", "1", PartialCreditMode.NONE, "0", "0").validate())
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("wrongMarks");
    }

    @Test
    @DisplayName("partial credit that could match or beat a perfect answer is rejected")
    void partialCreditMustStayBelowFullMarks() {
        assertThatThrownBy(() -> config("4", "-2", PartialCreditMode.PER_CORRECT_OPTION, "1", "4")
                .validate())
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("less than fullCorrectMarks");
    }

    @Test
    @DisplayName("enabling partial credit without a per-option value is rejected")
    void partialCreditNeedsAPerOptionValue() {
        assertThatThrownBy(() -> config("4", "-2", PartialCreditMode.PER_CORRECT_OPTION, "0", "3")
                .validate())
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("marksPerCorrectOption");
    }
}
