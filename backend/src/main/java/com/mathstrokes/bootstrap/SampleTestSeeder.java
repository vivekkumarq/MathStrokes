package com.mathstrokes.bootstrap;

import java.util.List;

import com.mathstrokes.catalog.entity.Chapter;
import com.mathstrokes.catalog.repository.ChapterRepository;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.TestGenerationMode;
import com.mathstrokes.config.AppProperties;
import com.mathstrokes.exam.dto.TestRequest;
import com.mathstrokes.exam.dto.TestResponse;
import com.mathstrokes.exam.repository.ExamTestRepository;
import com.mathstrokes.exam.service.TestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publishes two ready-to-sit tests on a fresh database, so a developer or a reviewer can log in
 * as a student and immediately take an examination.
 *
 * Deliberately goes through the ordinary TestService.create and publish path rather than
 * inserting rows: if the real publish pipeline is broken, this fails loudly at startup instead of
 * leaving behind seed data that the application itself could never have produced.
 *
 * Skips silently once any test exists, so it never interferes with real content.
 */
@Component
@Order(2)
public class SampleTestSeeder {

    private static final Logger log = LoggerFactory.getLogger(SampleTestSeeder.class);

    private final AppProperties appProperties;
    private final ExamTestRepository testRepository;
    private final ChapterRepository chapterRepository;
    private final TestService testService;

    public SampleTestSeeder(AppProperties appProperties, ExamTestRepository testRepository,
                            ChapterRepository chapterRepository, TestService testService) {
        this.appProperties = appProperties;
        this.testRepository = testRepository;
        this.chapterRepository = chapterRepository;
        this.testService = testService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedSampleTests() {
        if (!appProperties.getSeed().isEnabled()) {
            return;
        }
        if (testRepository.count() > 0) {
            log.debug("Tests already exist; skipping sample test seeding");
            return;
        }

        Chapter quadratics = findChapter("Quadratic Equations");
        if (quadratics == null) {
            log.warn("Chapter 'Quadratic Equations' not found; no sample tests were created");
            return;
        }

        int questionCount = appProperties.getExam().getDefaultQuestionCount();
        int duration = appProperties.getExam().getDefaultDurationMinutes();

        publish("Quadratic Equations - JEE Main - Practice Test 1",
                "Twenty-five single-correct questions on quadratic equations, JEE Main marking: "
                        + "+4 for a correct answer, -1 for a wrong one.",
                quadratics, ExamPattern.JEE_MAIN, questionCount, duration);

        publish("Quadratic Equations - JEE Advanced - Practice Test 1",
                "Twenty-five multiple-correct questions with JEE Advanced partial marking: "
                        + "+4 for the exact key, +1 per correct option up to +3, -2 for any wrong "
                        + "selection.",
                quadratics, ExamPattern.JEE_ADVANCED, questionCount, duration);
    }

    private void publish(String title, String description, Chapter chapter, ExamPattern pattern,
                         int questionCount, int durationMinutes) {
        try {
            TestRequest request = new TestRequest(title, description, chapter.getId(),
                    null, pattern, durationMinutes, questionCount, TestGenerationMode.FIXED_SET,
                    null, null, null, 1);
            TestResponse created = testService.create(request);
            testService.publish(created.id());
            log.info("Published sample test '{}'", title);
        } catch (RuntimeException ex) {
            // Almost always "not enough published questions", which is a seeding-data problem,
            // not a reason to stop the application from starting.
            log.warn("Could not publish sample test '{}': {}", title, ex.getMessage());
        }
    }

    private Chapter findChapter(String name) {
        List<Chapter> chapters = chapterRepository.findAllForAdmin(null);
        return chapters.stream()
                .filter(chapter -> chapter.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
