package com.mathstrokes.attempt.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.attempt.repository.TestAttemptRepository;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.exam.dto.AvailableTestResponse;
import com.mathstrokes.exam.entity.ExamTest;
import com.mathstrokes.exam.repository.ExamTestRepository;
import com.mathstrokes.exam.service.TestSchedule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What a student can sit right now.
 *
 * Lives in the attempt module rather than the exam module because the interesting part is not
 * the test but the student's relationship to it: how many attempts they have used, whether one
 * is already in flight, and therefore whether the Start button should be live.
 *
 * Nothing here exposes any question data. The paper is only revealed once an attempt exists.
 */
@Service
@Transactional(readOnly = true)
public class StudentTestCatalogService {

    private final ExamTestRepository testRepository;
    private final TestAttemptRepository attemptRepository;

    public StudentTestCatalogService(ExamTestRepository testRepository,
                                     TestAttemptRepository attemptRepository) {
        this.testRepository = testRepository;
        this.attemptRepository = attemptRepository;
    }

    public List<AvailableTestResponse> availableTests(Long studentId, Long subjectId,
                                                      Long chapterId, ExamPattern examPattern) {
        List<ExamTest> published = testRepository.findPublished(subjectId, chapterId, examPattern);

        // Both of these are asked once for the whole student rather than once per test. Asking
        // per test made this three queries plus two more for every row - 125 of them across the
        // 62 published tests - to build the first screen a student sees after signing in.
        Map<Long, Long> usedByTest = attemptRepository.attemptCountsByTest(studentId).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        Map<Long, TestAttempt> activeByTest = attemptRepository.findActiveByStudent(studentId)
                .stream()
                .collect(Collectors.toMap(a -> a.getTest().getId(), Function.identity(),
                        // A student should never hold two active attempts on one test - the
                        // start path enforces it. If the data ever disagrees, keep the first,
                        // which findActiveByStudent orders as the most recent, rather than
                        // letting toMap throw and take the whole catalogue down with it.
                        (first, second) -> first));

        // One clock for the entire list. Calling Instant.now() per row let a test open or close
        // partway down the page, so two rows could disagree about what time it was.
        Instant clock = Instant.now();

        return published.stream()
                .map(test -> toAvailable(test, usedByTest.getOrDefault(test.getId(), 0L),
                        activeByTest.get(test.getId()), clock))
                .toList();
    }

    private AvailableTestResponse toAvailable(ExamTest test, long used, TestAttempt activeAttempt,
                                              Instant clock) {
        boolean canStart;
        String reason;
        if (activeAttempt != null) {
            // An attempt in flight is always resumable, whatever the attempt allowance or the
            // scheduling window says. A student sitting a paper when the window closes finishes
            // it on their own clock, which is how closing a test has always behaved.
            canStart = true;
            reason = null;
        } else if (used >= test.getMaxAttemptsPerStudent()) {
            canStart = false;
            reason = test.getMaxAttemptsPerStudent() == 1
                    ? "You have already taken this test"
                    : "You have used all " + test.getMaxAttemptsPerStudent() + " attempts";
        } else if (!test.hasOpenedBy(clock)) {
            // Deliberately still listed rather than filtered out of the query: a class needs to
            // SEE tomorrow's paper in order to know it is coming.
            canStart = false;
            reason = "Opens on " + TestSchedule.humanise(test.getScheduledStartAt());
        } else if (test.hasWindowClosedBy(clock)) {
            canStart = false;
            reason = "This test has closed";
        } else {
            canStart = true;
            reason = null;
        }

        return new AvailableTestResponse(
                test.getId(),
                test.getTitle(),
                test.getDescription(),
                test.getSubject().getName(),
                test.chapterId(),
                test.chapterName(),
                test.getExamPattern(),
                test.getDurationMinutes(),
                test.getQuestionCount(),
                test.isRankingEnabled(),
                test.getMaxAttemptsPerStudent(),
                (int) used,
                canStart,
                activeAttempt == null ? null : activeAttempt.getId(),
                reason,
                test.getTestKind(),
                test.getScheduledStartAt(),
                test.getScheduledEndAt());
    }
}
