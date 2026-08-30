package com.mathstrokes.attempt.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mathstrokes.attempt.dto.PaletteEntryResponse;
import com.mathstrokes.attempt.dto.SaveAnswerRequest;
import com.mathstrokes.attempt.dto.SaveAnswerResponse;
import com.mathstrokes.attempt.entity.AttemptQuestion;
import com.mathstrokes.attempt.entity.AttemptQuestionOption;
import com.mathstrokes.attempt.entity.StudentAnswer;
import com.mathstrokes.attempt.entity.StudentAnswerOption;
import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.attempt.mapper.AttemptMapper;
import com.mathstrokes.attempt.repository.AttemptQuestionRepository;
import com.mathstrokes.attempt.repository.StudentAnswerRepository;
import com.mathstrokes.common.enums.AttemptStatus;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.common.exception.BusinessRuleException;
import com.mathstrokes.common.exception.ErrorCode;
import com.mathstrokes.common.exception.ForbiddenOperationException;
import com.mathstrokes.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autosave.
 *
 * Every write is checked four ways before anything is stored: the attempt belongs to the caller,
 * it is still ACTIVE, the server clock says it has not expired, and the question is part of THIS
 * attempt. Together these make it impossible to answer somebody else's paper, to answer a
 * question that was never assigned, or to keep writing after time is up.
 */
@Service
public class AnswerService {

    private final AttemptService attemptService;
    private final AttemptQuestionRepository attemptQuestionRepository;
    private final StudentAnswerRepository answerRepository;
    private final AttemptMapper mapper;

    public AnswerService(AttemptService attemptService,
                         AttemptQuestionRepository attemptQuestionRepository,
                         StudentAnswerRepository answerRepository,
                         AttemptMapper mapper) {
        this.attemptService = attemptService;
        this.attemptQuestionRepository = attemptQuestionRepository;
        this.answerRepository = answerRepository;
        this.mapper = mapper;
    }

    @Transactional
    public SaveAnswerResponse save(Long attemptId, Long studentId, SaveAnswerRequest request) {
        TestAttempt attempt = attemptService.requireOwnedAttempt(attemptId, studentId);
        assertWritable(attempt);

        AttemptQuestion question = attemptQuestionRepository
                .findByIdWithOptions(request.attemptQuestionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question",
                        request.attemptQuestionId()));
        if (!question.getAttempt().getId().equals(attemptId)) {
            throw new ForbiddenOperationException("That question is not part of this attempt");
        }

        StudentAnswer answer = answerRepository.findByAttemptQuestion(question.getId())
                .orElseGet(() -> newAnswer(attempt, question));

        long incomingSequence = request.clientSequence() == null ? 0L : request.clientSequence();
        if (incomingSequence > 0 && incomingSequence < answer.getClientSequence()) {
            // A late packet from a flaky connection. Keep what we have and tell the client what
            // that is, so it can reconcile rather than assume its write landed.
            return respond(attempt, answer, false);
        }

        applySelection(question, answer, request.selectedOptionIds());
        if (request.markedForReview() != null) {
            answer.setMarkedForReview(request.markedForReview());
        }
        if (request.visited() == null || request.visited()) {
            answer.setVisited(true);
        }
        answer.setClientSequence(Math.max(incomingSequence, answer.getClientSequence()));
        answer.refreshStatus();
        answerRepository.save(answer);

        return respond(attempt, answer, true);
    }

    /** Marks a question as seen without touching the selection. */
    @Transactional
    public SaveAnswerResponse markVisited(Long attemptId, Long studentId, Long attemptQuestionId) {
        TestAttempt attempt = attemptService.requireOwnedAttempt(attemptId, studentId);
        assertWritable(attempt);

        AttemptQuestion question = attemptQuestionRepository.findByIdWithOptions(attemptQuestionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", attemptQuestionId));
        if (!question.getAttempt().getId().equals(attemptId)) {
            throw new ForbiddenOperationException("That question is not part of this attempt");
        }

        StudentAnswer answer = answerRepository.findByAttemptQuestion(question.getId())
                .orElseGet(() -> newAnswer(attempt, question));
        answer.setVisited(true);
        answer.refreshStatus();
        answerRepository.save(answer);
        return respond(attempt, answer, true);
    }

    /**
     * The four guards. Expiry is judged against the server clock only; a client that keeps
     * showing a running timer after the deadline still cannot write.
     */
    private void assertWritable(TestAttempt attempt) {
        if (attempt.getStatus().isFinalised()) {
            throw new BusinessRuleException(ErrorCode.ATTEMPT_ALREADY_FINALISED,
                    "This attempt has already been submitted and can no longer be changed.");
        }
        if (attempt.getStatus() != AttemptStatus.ACTIVE) {
            throw new BusinessRuleException("This attempt has not been started.");
        }
        if (attempt.hasExpired(Instant.now())) {
            throw new BusinessRuleException(ErrorCode.ATTEMPT_EXPIRED,
                    "Time is up for this test. Your saved answers are being submitted.");
        }
    }

    /**
     * Replaces the selection wholesale. Sending an empty list is how the client clears an answer,
     * which keeps the operation idempotent under retry.
     */
    private void applySelection(AttemptQuestion question, StudentAnswer answer,
                                List<Long> requestedOptionIds) {
        Set<Long> requested = requestedOptionIds == null
                ? Set.of()
                : new LinkedHashSet<>(requestedOptionIds);

        if (question.getQuestionType() == QuestionType.SINGLE_CORRECT && requested.size() > 1) {
            throw new BusinessRuleException(
                    "This question accepts only one answer, but " + requested.size()
                            + " options were selected.");
        }

        Map<Long, AttemptQuestionOption> allowed = question.getOptions().stream()
                .collect(java.util.stream.Collectors.toMap(AttemptQuestionOption::getId,
                        option -> option));
        for (Long optionId : requested) {
            if (!allowed.containsKey(optionId)) {
                throw new BusinessRuleException(
                        "Option " + optionId + " does not belong to this question.");
            }
        }

        Set<Long> current = answer.selectedOptionIds();
        if (current.equals(requested)) {
            return;
        }

        answer.getSelectedOptions().clear();
        for (Long optionId : requested) {
            answer.getSelectedOptions()
                    .add(new StudentAnswerOption(answer, allowed.get(optionId)));
        }
        answer.setAnsweredAt(requested.isEmpty() ? null : Instant.now());
    }

    private StudentAnswer newAnswer(TestAttempt attempt, AttemptQuestion question) {
        StudentAnswer answer = new StudentAnswer();
        answer.setAttempt(attempt);
        answer.setAttemptQuestion(question);
        answer.setSelectedOptions(new HashSet<>());
        return answer;
    }

    /**
     * The acknowledgement carries the palette and the server clock so the exam screen can update
     * its navigator and correct its countdown without extra round trips.
     */
    private SaveAnswerResponse respond(TestAttempt attempt, StudentAnswer answer, boolean accepted) {
        List<AttemptQuestion> questions =
                attemptQuestionRepository.findByAttemptWithOptions(attempt.getId());
        Map<Long, StudentAnswer> answers = attemptService.answersByQuestionId(attempt.getId());
        List<PaletteEntryResponse> palette = mapper.toPalette(questions, answers);
        return new SaveAnswerResponse(
                accepted,
                answer.getAttemptQuestion().getId(),
                List.copyOf(answer.selectedOptionIds()),
                answer.getAnswerStatus(),
                answer.isMarkedForReview(),
                answer.getClientSequence(),
                mapper.toTiming(attempt),
                palette);
    }
}
