package com.mathstrokes.common.enums;

/**
 * Examination pattern. Deliberately separate from {@link Difficulty}: a JEE Advanced
 * question can be EASY and a JEE Main question can be HARD.
 * New patterns can be added here without touching the evaluation engine, provided a
 * matching {@code MarkingScheme} row exists.
 */
public enum ExamPattern {
    JEE_MAIN,
    JEE_ADVANCED
}
