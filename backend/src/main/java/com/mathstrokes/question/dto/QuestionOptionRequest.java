package com.mathstrokes.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record QuestionOptionRequest(
        @NotBlank(message = "Option label is required")
        @Pattern(regexp = "^[A-Z]$", message = "Option label must be a single uppercase letter")
        String optionKey,

        @NotBlank(message = "Option content is required")
        @Size(max = 5000, message = "Option content must be at most 5000 characters")
        String content,

        @PositiveOrZero(message = "Display order cannot be negative")
        Integer displayOrder,

        boolean isCorrect) {
}
