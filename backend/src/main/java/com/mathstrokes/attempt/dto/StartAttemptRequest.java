package com.mathstrokes.attempt.dto;

import jakarta.validation.constraints.NotNull;

public record StartAttemptRequest(
        @NotNull(message = "Please choose a test")
        Long testId) {
}
