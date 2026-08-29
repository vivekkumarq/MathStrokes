package com.mathstrokes.marking.entity;

public enum PartialCreditMode {
    /** A strict subset of the correct options earns {@code noPartialCreditMarks} (normally zero). */
    NONE,
    /** Each selected correct option earns {@code marksPerCorrectOption}, capped at {@code maxPartialMarks}. */
    PER_CORRECT_OPTION
}
