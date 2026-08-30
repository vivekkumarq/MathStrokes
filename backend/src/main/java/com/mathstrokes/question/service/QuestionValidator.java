package com.mathstrokes.question.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mathstrokes.common.dto.FieldErrorItem;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.common.exception.ValidationException;
import com.mathstrokes.question.dto.QuestionOptionRequest;
import com.mathstrokes.question.dto.QuestionRequest;
import org.springframework.stereotype.Component;

/**
 * Structural rules a question must satisfy.
 *
 * Draft rules are permissive so a teacher can save half-finished work; publish rules are strict,
 * because a published question can be drawn into a live examination. Field names in the reported
 * errors match the request body so the Angular form can bind them directly.
 */
@Component
public class QuestionValidator {

    /** Applied on every save, including drafts. */
    public void validateForSave(QuestionRequest request) {
        List<FieldErrorItem> errors = new ArrayList<>();
        validateOptionKeysUnique(request.options(), errors);
        validateContentNotBlank(request, errors);
        validateNoMarkup(request, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException("The question could not be saved", errors);
        }
    }

    /**
     * Applied before a question may be published. An invalid question can never enter the bank
     * that test generation draws from.
     */
    public void validateForPublish(QuestionType questionType, List<QuestionOptionRequest> options) {
        List<FieldErrorItem> errors = new ArrayList<>();
        long correctCount = options.stream().filter(QuestionOptionRequest::isCorrect).count();

        if (options.size() < 2) {
            errors.add(FieldErrorItem.of("options", "A published question needs at least two options"));
        }
        switch (questionType) {
            case SINGLE_CORRECT -> {
                if (correctCount != 1) {
                    errors.add(FieldErrorItem.of("options",
                            "A single-correct question must have exactly one correct option, found "
                                    + correctCount));
                }
            }
            case MULTIPLE_CORRECT -> {
                if (correctCount < 1) {
                    errors.add(FieldErrorItem.of("options",
                            "A multiple-correct question must have at least one correct option"));
                }
                if (correctCount == options.size()) {
                    errors.add(FieldErrorItem.of("options",
                            "Every option is marked correct, which makes the question unanswerable"));
                }
            }
        }
        validateOptionKeysUnique(options, errors);
        if (!errors.isEmpty()) {
            throw new ValidationException("The question is not ready to be published", errors);
        }
    }

    private void validateOptionKeysUnique(List<QuestionOptionRequest> options,
                                          List<FieldErrorItem> errors) {
        if (options == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (QuestionOptionRequest option : options) {
            if (option.optionKey() != null && !seen.add(option.optionKey().toUpperCase())) {
                errors.add(FieldErrorItem.of("options",
                        "Option label '" + option.optionKey() + "' is used more than once"));
            }
        }
    }

    private void validateContentNotBlank(QuestionRequest request, List<FieldErrorItem> errors) {
        if (request.questionContent() != null && request.questionContent().isBlank()) {
            errors.add(FieldErrorItem.of("questionContent", "Question content cannot be blank"));
        }
        if (request.options() != null) {
            for (int i = 0; i < request.options().size(); i++) {
                QuestionOptionRequest option = request.options().get(i);
                if (option.content() != null && option.content().isBlank()) {
                    errors.add(FieldErrorItem.of("options[" + i + "].content",
                            "Option content cannot be blank"));
                }
            }
        }
    }

    /**
     * Content is LaTeX and plain text, never markup. The browser renders it with HTML trust
     * disabled and escapes everything outside the maths delimiters, so this is defence in depth
     * rather than the primary boundary - but it means a compromised admin account cannot plant a
     * stored payload for every student who later sits the paper.
     */
    private void validateNoMarkup(QuestionRequest request, List<FieldErrorItem> errors) {
        String message = "Questions are written in LaTeX, not HTML. Remove the markup and use "
                + "$...$ for inline mathematics or $$...$$ for a displayed equation.";
        if (ContentSafetyGuard.containsMarkup(request.questionContent())) {
            errors.add(FieldErrorItem.of("questionContent", message));
        }
        if (ContentSafetyGuard.containsMarkup(request.solutionContent())) {
            errors.add(FieldErrorItem.of("solutionContent", message));
        }
        if (request.options() != null) {
            for (int i = 0; i < request.options().size(); i++) {
                if (ContentSafetyGuard.containsMarkup(request.options().get(i).content())) {
                    errors.add(FieldErrorItem.of("options[" + i + "].content", message));
                }
            }
        }
    }
}
