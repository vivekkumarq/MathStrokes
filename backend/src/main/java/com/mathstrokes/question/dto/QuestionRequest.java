package com.mathstrokes.question.dto;

import java.util.List;

import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Content fields carry LaTeX source. They are stored verbatim and rendered by KaTeX in the
 * browser; the server never interprets them as markup.
 */
public record QuestionRequest(
        @NotNull(message = "Chapter is required")
        Long chapterId,

        @NotNull(message = "Exam pattern is required")
        ExamPattern examPattern,

        @NotNull(message = "Difficulty is required")
        Difficulty difficulty,

        @NotNull(message = "Question type is required")
        QuestionType questionType,

        @NotBlank(message = "Question content is required")
        @Size(max = 20000, message = "Question content must be at most 20000 characters")
        String questionContent,

        @Size(max = 20000, message = "Solution must be at most 20000 characters")
        String solutionContent,

        /** Optional. When absent the active scheme for the pattern and type is used. */
        Long markingSchemeId,

        @NotEmpty(message = "A question needs at least two options")
        @Size(min = 2, max = 10, message = "A question must have between 2 and 10 options")
        @Valid
        List<QuestionOptionRequest> options) {
}
