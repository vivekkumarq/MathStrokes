package com.mathstrokes.question.mapper;

import java.util.List;

import com.mathstrokes.question.dto.QuestionOptionResponse;
import com.mathstrokes.question.dto.QuestionResponse;
import com.mathstrokes.question.dto.QuestionSummaryResponse;
import com.mathstrokes.question.entity.Question;
import com.mathstrokes.question.entity.QuestionOption;
import org.springframework.stereotype.Component;

@Component
public class QuestionMapper {

    private static final int PREVIEW_LENGTH = 160;

    public QuestionResponse toResponse(Question question) {
        return new QuestionResponse(
                question.getId(),
                question.getSubject().getId(),
                question.getSubject().getName(),
                question.getChapter().getId(),
                question.getChapter().getName(),
                question.getExamPattern(),
                question.getDifficulty(),
                question.getQuestionType(),
                question.getQuestionContent(),
                question.getSolutionContent(),
                question.getStatus(),
                question.getMarkingScheme() == null ? null : question.getMarkingScheme().getId(),
                question.getMarkingScheme() == null ? null : question.getMarkingScheme().getName(),
                question.getCreatedBy() == null ? null : question.getCreatedBy().getFullName(),
                question.getPublishedAt(),
                question.getCreatedAt(),
                question.getUpdatedAt(),
                question.getVersion(),
                toOptionResponses(question.getOptions()));
    }

    public QuestionSummaryResponse toSummary(Question question) {
        return new QuestionSummaryResponse(
                question.getId(),
                question.getChapter().getName(),
                question.getExamPattern(),
                question.getDifficulty(),
                question.getQuestionType(),
                preview(question.getQuestionContent()),
                question.getStatus(),
                question.getOptions().size(),
                question.getUpdatedAt(),
                question.getVersion());
    }

    public List<QuestionOptionResponse> toOptionResponses(List<QuestionOption> options) {
        return options.stream()
                .sorted(java.util.Comparator.comparingInt(QuestionOption::getDisplayOrder)
                        .thenComparing(QuestionOption::getOptionKey))
                .map(o -> new QuestionOptionResponse(o.getId(), o.getOptionKey(), o.getContent(),
                        o.getDisplayOrder(), o.isCorrect()))
                .toList();
    }

    /**
     * Short excerpt for the grid. LaTeX is left as-is rather than stripped: a truncated formula
     * still tells the teacher which question this is, and the grid does not render maths.
     */
    private String preview(String content) {
        if (content == null) {
            return "";
        }
        String collapsed = content.replaceAll("\\s+", " ").trim();
        return collapsed.length() <= PREVIEW_LENGTH
                ? collapsed
                : collapsed.substring(0, PREVIEW_LENGTH) + "...";
    }
}
