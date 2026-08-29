package com.mathstrokes.marking.dto;

import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.marking.entity.MarkingConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MarkingSchemeRequest(
        @NotBlank(message = "Scheme name is required")
        @Size(max = 150, message = "Scheme name must be at most 150 characters")
        String name,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @NotNull(message = "Exam pattern is required")
        ExamPattern examPattern,

        @NotNull(message = "Question type is required")
        QuestionType questionType,

        @NotNull(message = "Marking configuration is required")
        @Valid
        MarkingConfig configuration,

        Boolean active) {
}
