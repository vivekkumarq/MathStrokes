package com.mathstrokes.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SubjectRequest(
        @NotBlank(message = "Subject name is required")
        @Size(max = 100, message = "Subject name must be at most 100 characters")
        String name,

        @NotBlank(message = "Subject code is required")
        @Size(max = 20, message = "Subject code must be at most 20 characters")
        @Pattern(regexp = "^[A-Z0-9_]+$",
                message = "Subject code may contain only uppercase letters, digits and underscores")
        String code,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        Boolean active,

        @PositiveOrZero(message = "Display order cannot be negative")
        Integer displayOrder) {
}
