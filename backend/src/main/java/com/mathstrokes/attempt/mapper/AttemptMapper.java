package com.mathstrokes.attempt.mapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mathstrokes.attempt.dto.ActiveAttemptResponse;
import com.mathstrokes.attempt.dto.AttemptOptionResponse;
import com.mathstrokes.attempt.dto.AttemptQuestionResponse;
import com.mathstrokes.attempt.dto.AttemptSummaryResponse;
import com.mathstrokes.attempt.dto.AttemptTimingResponse;
import com.mathstrokes.attempt.dto.PaletteEntryResponse;
import com.mathstrokes.attempt.entity.AttemptQuestion;
import com.mathstrokes.attempt.entity.StudentAnswer;
import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.common.enums.AnswerStatus;
import org.springframework.stereotype.Component;

@Component
public class AttemptMapper {

    public AttemptTimingResponse toTiming(TestAttempt attempt) {
        Instant now = Instant.now();
        return new AttemptTimingResponse(now, attempt.getStartedAt(), attempt.getExpiresAt(),
                attempt.remainingSeconds(now), attempt.hasExpired(now));
    }

    public ActiveAttemptResponse toActiveAttempt(TestAttempt attempt,
                                                 List<AttemptQuestion> questions,
                                                 Map<Long, StudentAnswer> answersByQuestionId,
                                                 long clientSequence) {
        List<AttemptQuestionResponse> questionResponses = questions.stream()
                .map(question -> toQuestion(question, answersByQuestionId.get(question.getId())))
                .toList();
        return new ActiveAttemptResponse(
                attempt.getId(),
                attempt.getTest().getId(),
                attempt.getTest().getTitle(),
                attempt.getTest().getSubject().getName(),
                attempt.getTest().getChapter().getName(),
                attempt.getTest().getExamPattern(),
                attempt.getStatus(),
                attempt.getTotalQuestions(),
                attempt.getDurationMinutes(),
                toTiming(attempt),
                clientSequence,
                questionResponses);
    }

    /** Builds the student view of one question. The answer key is not present in the output type. */
    public AttemptQuestionResponse toQuestion(AttemptQuestion question, StudentAnswer answer) {
        Set<Long> selected = answer == null ? Set.of() : answer.selectedOptionIds();
        List<AttemptOptionResponse> options = question.getOptions().stream()
                .sorted(java.util.Comparator.comparingInt(
                        com.mathstrokes.attempt.entity.AttemptQuestionOption::getDisplayOrder))
                .map(option -> new AttemptOptionResponse(option.getId(), option.getOptionKey(),
                        option.getContent(), option.getDisplayOrder()))
                .toList();
        return new AttemptQuestionResponse(
                question.getId(),
                question.getQuestionOrder(),
                question.getQuestionType(),
                question.getDifficulty(),
                question.getQuestionContent(),
                options,
                List.copyOf(selected),
                answer == null ? AnswerStatus.NOT_VISITED : answer.getAnswerStatus(),
                answer != null && answer.isMarkedForReview(),
                answer != null && answer.isVisited());
    }

    public List<PaletteEntryResponse> toPalette(List<AttemptQuestion> questions,
                                                Map<Long, StudentAnswer> answersByQuestionId) {
        return questions.stream()
                .map(question -> {
                    StudentAnswer answer = answersByQuestionId.get(question.getId());
                    return new PaletteEntryResponse(question.getId(), question.getQuestionOrder(),
                            answer == null ? AnswerStatus.NOT_VISITED : answer.getAnswerStatus());
                })
                .toList();
    }

    public AttemptSummaryResponse toSummary(TestAttempt attempt) {
        return new AttemptSummaryResponse(
                attempt.getId(),
                attempt.getTest().getId(),
                attempt.getTest().getTitle(),
                attempt.getTest().getChapter().getName(),
                attempt.getTest().getExamPattern(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getScore(),
                attempt.getMaxScore(),
                attempt.getRankPosition(),
                attempt.getTotalCandidates(),
                attempt.getPercentile(),
                attempt.getTotalQuestions());
    }
}
