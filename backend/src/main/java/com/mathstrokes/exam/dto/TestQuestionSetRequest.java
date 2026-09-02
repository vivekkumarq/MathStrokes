package com.mathstrokes.exam.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * The exact paper a teacher has hand-picked, in the order it will be sat.
 *
 * The list order IS the order on the paper - there is no separate position field that could
 * disagree with it. Sending the list again replaces the paper wholesale, which is also how a
 * reorder is expressed: there is no partial update, so the request always describes the whole
 * paper and can never leave it half-changed.
 */
public record TestQuestionSetRequest(

        @NotEmpty(message = "Pick at least one question")
        @Size(max = 200, message = "A test cannot exceed 200 questions")
        List<Long> questionIds) {
}
