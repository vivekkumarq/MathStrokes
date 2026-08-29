package com.mathstrokes.common.enums;

/**
 * FIXED_SET      - the question set is materialised once, at publish time, and every student
 *                  receives exactly the same 25 questions. This is the only mode that yields a
 *                  fair ranking cohort.
 * RANDOM_PER_ATTEMPT - each attempt draws its own set from the blueprint. Useful for practice;
 *                  ranking is disabled for these tests (see {@code ExamTest#rankingEnabled}).
 */
public enum TestGenerationMode {
    FIXED_SET,
    RANDOM_PER_ATTEMPT
}
