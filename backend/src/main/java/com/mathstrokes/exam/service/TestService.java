package com.mathstrokes.exam.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.mathstrokes.catalog.entity.Chapter;
import com.mathstrokes.catalog.service.CatalogService;
import com.mathstrokes.common.dto.PageResponse;
import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.TestGenerationMode;
import com.mathstrokes.common.enums.TestStatus;
import com.mathstrokes.common.exception.BusinessRuleException;
import com.mathstrokes.common.exception.ResourceNotFoundException;
import com.mathstrokes.exam.dto.TestRequest;
import com.mathstrokes.exam.dto.TestResponse;
import com.mathstrokes.exam.entity.ExamTest;
import com.mathstrokes.exam.entity.TestQuestion;
import com.mathstrokes.exam.mapper.TestMapper;
import com.mathstrokes.exam.repository.ExamTestRepository;
import com.mathstrokes.exam.repository.TestQuestionRepository;
import com.mathstrokes.question.entity.Question;
import com.mathstrokes.question.repository.QuestionRepository;
import com.mathstrokes.security.service.SecurityUtils;
import com.mathstrokes.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TestService {

    private final ExamTestRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionSelectionService selectionService;
    private final CatalogService catalogService;
    private final UserRepository userRepository;
    private final TestMapper mapper;

    public TestService(ExamTestRepository testRepository,
                       TestQuestionRepository testQuestionRepository,
                       QuestionRepository questionRepository,
                       QuestionSelectionService selectionService,
                       CatalogService catalogService,
                       UserRepository userRepository,
                       TestMapper mapper) {
        this.testRepository = testRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.questionRepository = questionRepository;
        this.selectionService = selectionService;
        this.catalogService = catalogService;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    public PageResponse<TestResponse> search(TestStatus status, Long chapterId,
                                             ExamPattern examPattern, Pageable pageable) {
        Page<ExamTest> page = testRepository.findForAdmin(status, chapterId, examPattern, pageable);
        return PageResponse.from(page,
                test -> mapper.toResponse(test, testQuestionRepository.countByTestId(test.getId())));
    }

    public ExamTest requireTest(Long id) {
        return testRepository.findByIdWithCatalog(id)
                .orElseThrow(() -> new ResourceNotFoundException("Test", id));
    }

    public TestResponse get(Long id) {
        ExamTest test = requireTest(id);
        return mapper.toResponse(test, testQuestionRepository.countByTestId(id));
    }

    @Transactional
    public TestResponse create(TestRequest request) {
        Chapter chapter = catalogService.requireChapter(request.chapterId());
        ExamTest test = new ExamTest();
        test.setChapter(chapter);
        test.setSubject(chapter.getSubject());
        applyRequest(test, request);
        test.setStatus(TestStatus.DRAFT);
        SecurityUtils.currentPrincipal()
                .flatMap(principal -> userRepository.findById(principal.id()))
                .ifPresent(test::setCreatedBy);
        return mapper.toResponse(testRepository.save(test), 0);
    }

    @Transactional
    public TestResponse update(Long id, TestRequest request) {
        ExamTest test = requireTest(id);
        if (test.getStatus() != TestStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only a draft test can be edited. This test is " + test.getStatus()
                            + " and students may already have sat it.");
        }
        Chapter chapter = catalogService.requireChapter(request.chapterId());
        test.setChapter(chapter);
        test.setSubject(chapter.getSubject());
        applyRequest(test, request);
        return mapper.toResponse(test, testQuestionRepository.countByTestId(id));
    }

    /**
     * Publishing a FIXED_SET test draws its paper once and writes it to test_questions. From this
     * moment every student receives the identical 25 questions, which is what makes the ranking
     * cohort fair. A RANDOM_PER_ATTEMPT test draws per attempt instead and is not ranked.
     */
    @Transactional
    public TestResponse publish(Long id) {
        ExamTest test = requireTest(id);
        if (test.getStatus() == TestStatus.PUBLISHED) {
            return mapper.toResponse(test, testQuestionRepository.countByTestId(id));
        }
        if (test.getStatus() == TestStatus.ARCHIVED) {
            throw new BusinessRuleException("An archived test cannot be published again.");
        }

        if (test.hasFixedQuestionSet() && testQuestionRepository.countByTestId(id) == 0) {
            materialiseQuestionSet(test);
        } else if (!test.hasFixedQuestionSet()) {
            // Draw nothing yet, but fail now rather than at a student's first click if the bank
            // cannot support the blueprint.
            selectionService.selectQuestionIds(blueprintOf(test));
        }

        test.setStatus(TestStatus.PUBLISHED);
        test.setPublishedAt(Instant.now());
        return mapper.toResponse(test, testQuestionRepository.countByTestId(id));
    }

    /**
     * Closing stops new attempts while leaving those in flight to finish and remain rankable.
     */
    @Transactional
    public TestResponse close(Long id) {
        ExamTest test = requireTest(id);
        if (test.getStatus() != TestStatus.PUBLISHED) {
            throw new BusinessRuleException("Only a published test can be closed.");
        }
        test.setStatus(TestStatus.CLOSED);
        test.setClosedAt(Instant.now());
        return mapper.toResponse(test, testQuestionRepository.countByTestId(id));
    }

    @Transactional
    public TestResponse archive(Long id) {
        ExamTest test = requireTest(id);
        test.setStatus(TestStatus.ARCHIVED);
        return mapper.toResponse(test, testQuestionRepository.countByTestId(id));
    }

    /** Draws the paper and pins it to the test in a stable order. */
    private void materialiseQuestionSet(ExamTest test) {
        List<Long> questionIds = selectionService.selectQuestionIds(blueprintOf(test));
        List<Question> questions = questionRepository.findAllByIdWithOptions(questionIds);
        Map<Long, Question> byId = new java.util.HashMap<>();
        questions.forEach(question -> byId.put(question.getId(), question));

        List<TestQuestion> paper = new ArrayList<>();
        int order = 1;
        for (Long questionId : questionIds) {
            Question question = byId.get(questionId);
            if (question == null) {
                continue;
            }
            TestQuestion entry = new TestQuestion();
            entry.setQuestion(question);
            entry.setQuestionOrder(order++);
            paper.add(entry);
        }
        test.replaceQuestions(paper);
    }

    private void applyRequest(ExamTest test, TestRequest request) {
        test.setTitle(request.title().trim());
        test.setDescription(request.description());
        test.setExamPattern(request.examPattern());
        test.setDurationMinutes(request.durationMinutes());
        test.setQuestionCount(request.questionCount());
        test.setGenerationMode(request.generationMode());
        test.setEasyCount(request.easyCount());
        test.setMediumCount(request.mediumCount());
        test.setHardCount(request.hardCount());
        test.setMaxAttemptsPerStudent(
                request.maxAttemptsPerStudent() == null ? 1 : request.maxAttemptsPerStudent());
        // Only an identical paper for every candidate yields a comparable cohort.
        test.setRankingEnabled(request.generationMode() == TestGenerationMode.FIXED_SET);
        validateBlueprint(test);
    }

    private void validateBlueprint(ExamTest test) {
        int banded = zeroIfNull(test.getEasyCount()) + zeroIfNull(test.getMediumCount())
                + zeroIfNull(test.getHardCount());
        if (banded > test.getQuestionCount()) {
            throw new BusinessRuleException("The difficulty split adds up to " + banded
                    + " questions, which is more than the " + test.getQuestionCount()
                    + " this test is meant to have.");
        }
    }

    private int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    public QuestionSelectionService.Blueprint blueprintOf(ExamTest test) {
        Map<Difficulty, Integer> bands = new EnumMap<>(Difficulty.class);
        putIfPositive(bands, Difficulty.EASY, test.getEasyCount());
        putIfPositive(bands, Difficulty.MEDIUM, test.getMediumCount());
        putIfPositive(bands, Difficulty.HARD, test.getHardCount());
        return new QuestionSelectionService.Blueprint(test.getChapter().getId(),
                test.getExamPattern(), test.getQuestionCount(), bands);
    }

    private void putIfPositive(Map<Difficulty, Integer> bands, Difficulty difficulty, Integer count) {
        if (count != null && count > 0) {
            bands.put(difficulty, count);
        }
    }
}
