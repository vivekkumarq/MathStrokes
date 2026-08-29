package com.mathstrokes.marking.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mathstrokes.common.exception.ValidationException;
import jakarta.validation.constraints.NotNull;

/**
 * Typed view over the {@code marking_schemes.configuration} JSONB document.
 *
 * Stored as JSON so a new rule needs no migration, but read through this record so the rest of
 * the codebase never touches raw JSON. A snapshot of this document is copied onto every
 * attempt question, which is what makes historical results reproducible.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MarkingConfig(
        @NotNull BigDecimal fullCorrectMarks,
        @NotNull BigDecimal wrongMarks,
        @NotNull BigDecimal unansweredMarks,
        PartialCreditMode partialCreditMode,
        BigDecimal marksPerCorrectOption,
        BigDecimal maxPartialMarks,
        BigDecimal noPartialCreditMarks) {

    public MarkingConfig {
        partialCreditMode = partialCreditMode == null ? PartialCreditMode.NONE : partialCreditMode;
        marksPerCorrectOption = defaulted(marksPerCorrectOption);
        maxPartialMarks = defaulted(maxPartialMarks);
        noPartialCreditMarks = defaulted(noPartialCreditMarks);
    }

    private static BigDecimal defaulted(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** Marks a perfect answer is worth. Used to compute an attempt's maximum achievable score. */
    public BigDecimal maxMarks() {
        return fullCorrectMarks;
    }

    /**
     * Rejects configurations that would make scoring nonsensical. Called on every admin write,
     * so a broken scheme can never reach an attempt snapshot.
     */
    public void validate() {
        if (fullCorrectMarks == null || wrongMarks == null || unansweredMarks == null) {
            throw new ValidationException(
                    "fullCorrectMarks, wrongMarks and unansweredMarks are all required");
        }
        if (fullCorrectMarks.signum() <= 0) {
            throw new ValidationException("fullCorrectMarks must be greater than zero");
        }
        if (wrongMarks.signum() > 0) {
            throw new ValidationException("wrongMarks must be zero or negative");
        }
        if (partialCreditMode == PartialCreditMode.PER_CORRECT_OPTION) {
            if (marksPerCorrectOption.signum() <= 0) {
                throw new ValidationException(
                        "marksPerCorrectOption must be greater than zero when partial credit is enabled");
            }
            if (maxPartialMarks.signum() <= 0) {
                throw new ValidationException(
                        "maxPartialMarks must be greater than zero when partial credit is enabled");
            }
            if (maxPartialMarks.compareTo(fullCorrectMarks) >= 0) {
                throw new ValidationException(
                        "maxPartialMarks must be less than fullCorrectMarks, otherwise a partial "
                                + "answer would score at least as much as a perfect one");
            }
        }
    }
}
