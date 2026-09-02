package com.mathstrokes.exam.mapper;

import com.mathstrokes.exam.dto.TestResponse;
import com.mathstrokes.exam.entity.ExamTest;
import org.springframework.stereotype.Component;

@Component
public class TestMapper {

    public TestResponse toResponse(ExamTest test, long attachedQuestionCount) {
        return new TestResponse(
                test.getId(),
                test.getTitle(),
                test.getDescription(),
                test.getSubject().getId(),
                test.getSubject().getName(),
                test.chapterId(),
                test.chapterName(),
                test.getExamPattern(),
                test.getDurationMinutes(),
                test.getQuestionCount(),
                test.getGenerationMode(),
                test.getEasyCount(),
                test.getMediumCount(),
                test.getHardCount(),
                test.getStatus(),
                test.isRankingEnabled(),
                test.getMaxAttemptsPerStudent(),
                attachedQuestionCount,
                test.getPublishedAt(),
                test.getClosedAt(),
                test.getCreatedAt(),
                test.getVersion(),
                test.getTestKind(),
                test.getScheduledStartAt(),
                test.getScheduledEndAt());
    }
}
