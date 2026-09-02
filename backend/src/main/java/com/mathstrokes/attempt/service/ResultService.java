package com.mathstrokes.attempt.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mathstrokes.attempt.dto.AttemptResultResponse;
import com.mathstrokes.attempt.dto.QuestionReviewResponse;
import com.mathstrokes.attempt.entity.AttemptQuestion;
import com.mathstrokes.attempt.entity.QuestionAttemptResult;
import com.mathstrokes.attempt.entity.StudentAnswer;
import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.attempt.repository.AttemptQuestionRepository;
import com.mathstrokes.attempt.repository.QuestionAttemptResultRepository;
import com.mathstrokes.common.enums.AttemptStatus;
import com.mathstrokes.common.exception.BusinessRuleException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the post-submission views.
 *
 * This is the only place where the answer key and the worked solution leave the server, and only
 * for an attempt that is finished and belongs to the caller.
 */
@Service
@Transactional(readOnly = true)
public class ResultService {

    private final AttemptService attemptService;
    private final AttemptQuestionRepository attemptQuestionRepository;
    private final QuestionAttemptResultRepository resultRepository;

    public ResultService(AttemptService attemptService,
                         AttemptQuestionRepository attemptQuestionRepository,
                         QuestionAttemptResultRepository resultRepository) {
        this.attemptService = attemptService;
        this.attemptQuestionRepository = attemptQuestionRepository;
        this.resultRepository = resultRepository;
    }

    public AttemptResultResponse getResult(Long attemptId, Long studentId) {
        TestAttempt attempt = attemptService.requireOwnedAttempt(attemptId, studentId);
        requireFinished(attempt);
        return buildResult(attempt);
    }

    public AttemptResultResponse buildResult(TestAttempt attempt) {
        return new AttemptResultResponse(
                attempt.getId(),
                attempt.getTest().getId(),
                attempt.getTest().getTitle(),
                attempt.getTest().getSubject().getName(),
                attempt.getTest().chapterName(),
                attempt.getTest().getTestKind(),
                attempt.getTest().getExamPattern(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                attempt.getTimeTakenSeconds(),
                attempt.getDurationMinutes(),
                attempt.getTotalQuestions(),
                attempt.getScore(),
                attempt.getMaxScore(),
                attempt.getNegativeMarks(),
                attempt.getCorrectCount(),
                attempt.getPartiallyCorrectCount(),
                attempt.getIncorrectCount(),
                attempt.getUnansweredCount(),
                attempt.getAttemptedCount(),
                attempt.getAccuracy(),
                attempt.getAttemptRate(),
                attempt.getTest().isRankingEnabled(),
                attempt.getRankPosition(),
                attempt.getTotalCandidates(),
                attempt.getPercentile());
    }

    /** Question-by-question review, with the answer key and the solution. */
    public List<QuestionReviewResponse> getReview(Long attemptId, Long studentId) {
        TestAttempt attempt = attemptService.requireOwnedAttempt(attemptId, studentId);
        requireFinished(attempt);

        List<AttemptQuestion> questions =
                attemptQuestionRepository.findByAttemptWithOptions(attemptId);
        Map<Long, StudentAnswer> answers = attemptService.answersByQuestionId(attemptId);
        Map<Long, QuestionAttemptResult> results = resultRepository.findByAttempt(attemptId).stream()
                .collect(Collectors.toMap(result -> result.getAttemptQuestion().getId(),
                        Function.identity()));

        return questions.stream().map(question -> toReview(question,
                answers.get(question.getId()), results.get(question.getId()))).toList();
    }

    private QuestionReviewResponse toReview(AttemptQuestion question, StudentAnswer answer,
                                            QuestionAttemptResult result) {
        Set<Long> selected = answer == null ? Set.of() : answer.selectedOptionIds();
        List<QuestionReviewResponse.ReviewOptionResponse> options = question.getOptions().stream()
                .sorted(java.util.Comparator.comparingInt(
                        com.mathstrokes.attempt.entity.AttemptQuestionOption::getDisplayOrder))
                .map(option -> new QuestionReviewResponse.ReviewOptionResponse(
                        option.getId(), option.getOptionKey(), option.getContent(),
                        option.getDisplayOrder(), option.isCorrect(),
                        selected.contains(option.getId())))
                .toList();

        return new QuestionReviewResponse(
                question.getId(),
                question.getQuestionOrder(),
                question.getQuestionType(),
                question.getDifficulty(),
                question.getQuestionContent(),
                question.getSolutionContent(),
                options,
                List.copyOf(selected),
                List.copyOf(question.correctOptionIds()),
                result == null ? null : result.getResultStatus(),
                result == null ? null : result.getMarksAwarded(),
                result == null ? question.getMaxMarks() : result.getMaxMarks());
    }

    /** Refuses to reveal anything about a paper that is still in progress. */
    private void requireFinished(TestAttempt attempt) {
        if (attempt.getStatus() == AttemptStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "This test is still in progress. Submit it to see your result.");
        }
    }
}
