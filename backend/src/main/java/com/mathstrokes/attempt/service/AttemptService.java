package com.mathstrokes.attempt.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mathstrokes.attempt.dto.ActiveAttemptResponse;
import com.mathstrokes.attempt.dto.AttemptSummaryResponse;
import com.mathstrokes.attempt.entity.AttemptQuestion;
import com.mathstrokes.attempt.entity.StudentAnswer;
import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.attempt.mapper.AttemptMapper;
import com.mathstrokes.attempt.repository.AttemptQuestionRepository;
import com.mathstrokes.attempt.repository.StudentAnswerRepository;
import com.mathstrokes.attempt.repository.TestAttemptRepository;
import com.mathstrokes.common.dto.PageResponse;
import com.mathstrokes.common.enums.AttemptStatus;
import com.mathstrokes.common.enums.TestStatus;
import com.mathstrokes.common.exception.BusinessRuleException;
import com.mathstrokes.common.exception.ForbiddenOperationException;
import com.mathstrokes.common.exception.ResourceNotFoundException;
import com.mathstrokes.exam.entity.ExamTest;
import com.mathstrokes.exam.entity.TestQuestion;
import com.mathstrokes.exam.repository.TestQuestionRepository;
import com.mathstrokes.exam.service.QuestionSelectionService;
import com.mathstrokes.exam.service.TestService;
import com.mathstrokes.user.entity.User;
import com.mathstrokes.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AttemptService {

    private final TestAttemptRepository attemptRepository;
    private final AttemptQuestionRepository attemptQuestionRepository;
    private final StudentAnswerRepository answerRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final TestService testService;
    private final QuestionSelectionService selectionService;
    private final AttemptSnapshotService snapshotService;
    private final UserRepository userRepository;
    private final AttemptMapper mapper;

    public AttemptService(TestAttemptRepository attemptRepository,
                          AttemptQuestionRepository attemptQuestionRepository,
                          StudentAnswerRepository answerRepository,
                          TestQuestionRepository testQuestionRepository,
                          TestService testService,
                          QuestionSelectionService selectionService,
                          AttemptSnapshotService snapshotService,
                          UserRepository userRepository,
                          AttemptMapper mapper) {
        this.attemptRepository = attemptRepository;
        this.attemptQuestionRepository = attemptQuestionRepository;
        this.answerRepository = answerRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.testService = testService;
        this.selectionService = selectionService;
        this.snapshotService = snapshotService;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    /**
     * Loads an attempt and proves it belongs to the caller.
     *
     * Every route that touches an attempt goes through here. The student id comes from the
     * security context, never from the request, so one student cannot reach another's paper by
     * guessing an id.
     */
    public TestAttempt requireOwnedAttempt(Long attemptId, Long studentId) {
        TestAttempt attempt = attemptRepository.findByIdWithTest(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt", attemptId));
        if (!attempt.getStudent().getId().equals(studentId)) {
            // Deliberately phrased as not-found: confirming the attempt exists would leak that
            // somebody else holds that id.
            throw new ForbiddenOperationException("This attempt does not belong to you");
        }
        return attempt;
    }

    /**
     * Starts a test, or hands back the attempt already in progress.
     *
     * Resuming rather than erroring is the point: a student who refreshes, loses their network or
     * closes the tab comes back to the same paper with the same clock still running.
     */
    @Transactional
    public ActiveAttemptResponse startOrResume(Long studentId, Long testId) {
        Optional<TestAttempt> existing =
                attemptRepository.findActiveByStudentAndTest(studentId, testId);
        if (existing.isPresent()) {
            return loadActive(existing.get());
        }

        ExamTest test = testService.requireTest(testId);
        if (test.getStatus() != TestStatus.PUBLISHED) {
            throw new BusinessRuleException(test.getStatus() == TestStatus.CLOSED
                    ? "This test has been closed and is no longer accepting attempts."
                    : "This test is not open for attempts.");
        }

        long used = attemptRepository.countByStudentIdAndTestId(studentId, testId);
        if (used >= test.getMaxAttemptsPerStudent()) {
            throw new BusinessRuleException("You have already used all "
                    + test.getMaxAttemptsPerStudent() + " attempt(s) allowed on this test.");
        }

        List<Long> questionIds = resolvePaper(test);
        if (questionIds.size() != test.getQuestionCount()) {
            throw new BusinessRuleException("This test is configured for " + test.getQuestionCount()
                    + " questions but only " + questionIds.size()
                    + " could be assembled. Please tell your teacher.");
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        Instant now = Instant.now();
        TestAttempt attempt = new TestAttempt();
        attempt.setStudent(student);
        attempt.setTest(test);
        attempt.setAttemptNumber((int) used + 1);
        attempt.setStatus(AttemptStatus.ACTIVE);
        attempt.setStartedAt(now);
        // The deadline is fixed here and never moved again.
        attempt.setExpiresAt(now.plus(Duration.ofMinutes(test.getDurationMinutes())));
        attempt.setDurationMinutes(test.getDurationMinutes());
        attempt.setTotalQuestions(questionIds.size());
        snapshotService.snapshotOnto(attempt, questionIds);

        try {
            attemptRepository.saveAndFlush(attempt);
        } catch (DataIntegrityViolationException ex) {
            // Two "start" clicks landing together: the partial unique index on ACTIVE attempts
            // lets exactly one through. Return whichever won rather than showing an error.
            return attemptRepository.findActiveByStudentAndTest(studentId, testId)
                    .map(this::loadActive)
                    .orElseThrow(() -> ex);
        }
        return loadActive(attempt);
    }

    /**
     * A FIXED_SET test hands out the paper pinned at publish time; every student gets the same
     * questions in the same order, which is what makes the ranking cohort comparable. A
     * RANDOM_PER_ATTEMPT test draws a fresh paper here, once, and it is snapshotted immediately.
     */
    private List<Long> resolvePaper(ExamTest test) {
        if (test.hasFixedQuestionSet()) {
            List<TestQuestion> paper = testQuestionRepository.findByTestWithQuestions(test.getId());
            if (paper.isEmpty()) {
                throw new BusinessRuleException(
                        "This test has no questions attached. Please tell your teacher.");
            }
            return paper.stream().map(entry -> entry.getQuestion().getId()).toList();
        }
        return selectionService.selectQuestionIds(testService.blueprintOf(test));
    }

    /** The active attempt for the exam screen, or empty if the student has none in flight. */
    public Optional<ActiveAttemptResponse> findActiveAttempt(Long studentId) {
        return attemptRepository.findActiveByStudent(studentId).stream()
                .findFirst()
                .map(this::loadActive);
    }

    public ActiveAttemptResponse getActiveAttempt(Long attemptId, Long studentId) {
        TestAttempt attempt = requireOwnedAttempt(attemptId, studentId);
        if (attempt.getStatus() != AttemptStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "This attempt has already been submitted. Open the result instead.");
        }
        return loadActive(attempt);
    }

    private ActiveAttemptResponse loadActive(TestAttempt attempt) {
        List<AttemptQuestion> questions =
                attemptQuestionRepository.findByAttemptWithOptions(attempt.getId());
        Map<Long, StudentAnswer> answers = answersByQuestionId(attempt.getId());
        long highestSequence = answers.values().stream()
                .mapToLong(StudentAnswer::getClientSequence)
                .max()
                .orElse(0L);
        return mapper.toActiveAttempt(attempt, questions, answers, highestSequence);
    }

    Map<Long, StudentAnswer> answersByQuestionId(Long attemptId) {
        Map<Long, StudentAnswer> byQuestion = new HashMap<>();
        for (StudentAnswer answer : answerRepository.findByAttemptWithSelections(attemptId)) {
            byQuestion.put(answer.getAttemptQuestion().getId(), answer);
        }
        return byQuestion;
    }

    public PageResponse<AttemptSummaryResponse> history(Long studentId, Pageable pageable) {
        return PageResponse.from(attemptRepository.findByStudent(studentId, pageable),
                mapper::toSummary);
    }
}
