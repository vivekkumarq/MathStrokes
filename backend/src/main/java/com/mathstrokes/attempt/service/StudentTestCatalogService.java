package com.mathstrokes.attempt.service;

import java.util.List;
import java.util.Optional;

import com.mathstrokes.attempt.entity.TestAttempt;
import com.mathstrokes.attempt.repository.TestAttemptRepository;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.exam.dto.AvailableTestResponse;
import com.mathstrokes.exam.entity.ExamTest;
import com.mathstrokes.exam.repository.ExamTestRepository;
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
        return testRepository.findPublished(subjectId, chapterId, examPattern).stream()
                .map(test -> toAvailable(test, studentId))
                .toList();
    }

    private AvailableTestResponse toAvailable(ExamTest test, Long studentId) {
        long used = attemptRepository.countByStudentIdAndTestId(studentId, test.getId());
        Optional<TestAttempt> active =
                attemptRepository.findActiveByStudentAndTest(studentId, test.getId());

        boolean canStart;
        String reason;
        if (active.isPresent()) {
            // An attempt in flight is always resumable, whatever the attempt allowance says.
            canStart = true;
            reason = null;
        } else if (used >= test.getMaxAttemptsPerStudent()) {
            canStart = false;
            reason = test.getMaxAttemptsPerStudent() == 1
                    ? "You have already taken this test"
                    : "You have used all " + test.getMaxAttemptsPerStudent() + " attempts";
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
                active.map(TestAttempt::getId).orElse(null),
                reason);
    }
}
