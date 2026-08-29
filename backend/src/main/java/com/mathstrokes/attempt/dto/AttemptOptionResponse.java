package com.mathstrokes.attempt.dto;

/**
 * An option as shown to a student during a live attempt.
 *
 * There is deliberately no correctness field on this record. The answer key is not hidden by a
 * flag that could be forgotten - it is structurally absent, so it cannot be leaked by a mapper
 * change or read out of the network tab.
 */
public record AttemptOptionResponse(Long id, String optionKey, String content, int displayOrder) {
}
