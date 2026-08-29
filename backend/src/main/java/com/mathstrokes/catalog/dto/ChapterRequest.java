package com.mathstrokes.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ChapterRequest(
        @NotNull(message = "Subject is required")
        Long subjectId,

        @NotBlank(message = "Chapter name is required")
        @Size(max = 150, message = "Chapter name must be at most 150 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        Boolean active,

        @PositiveOrZero(message = "Display order cannot be negative")
        Integer displayOrder) {
}
