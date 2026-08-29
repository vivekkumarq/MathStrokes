package com.mathstrokes.exam.dto;

import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.TestGenerationMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record TestRequest(
        @NotBlank(message = "Test title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 1000, message = "Description must be at most 1000 characters")
        String description,

        @NotNull(message = "Chapter is required")
        Long chapterId,

        @NotNull(message = "Exam pattern is required")
        ExamPattern examPattern,

        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1 minute")
        @Max(value = 600, message = "Duration cannot exceed 600 minutes")
        Integer durationMinutes,

        @NotNull(message = "Question count is required")
        @Min(value = 1, message = "A test needs at least 1 question")
        @Max(value = 200, message = "A test cannot exceed 200 questions")
        Integer questionCount,

        @NotNull(message = "Generation mode is required")
        TestGenerationMode generationMode,

        @PositiveOrZero(message = "Easy count cannot be negative")
        Integer easyCount,

        @PositiveOrZero(message = "Medium count cannot be negative")
        Integer mediumCount,

        @PositiveOrZero(message = "Hard count cannot be negative")
        Integer hardCount,

        @Min(value = 1, message = "A student must be allowed at least one attempt")
        @Max(value = 100, message = "Attempt limit cannot exceed 100")
        Integer maxAttemptsPerStudent) {
}
