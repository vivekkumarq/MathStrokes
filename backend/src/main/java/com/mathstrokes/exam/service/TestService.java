package com.mathstrokes.exam.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mathstrokes.catalog.dto.SubjectResponse;
import com.mathstrokes.catalog.entity.Chapter;
import com.mathstrokes.catalog.entity.Subject;
import com.mathstrokes.catalog.service.CatalogService;
import com.mathstrokes.common.dto.PageResponse;
import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionStatus;
import com.mathstrokes.common.enums.TestGenerationMode;
import com.mathstrokes.common.enums.TestKind;
import com.mathstrokes.common.enums.TestStatus;
import com.mathstrokes.common.exception.BusinessRuleException;
import com.mathstrokes.common.exception.ResourceNotFoundException;
import com.mathstrokes.exam.dto.TestQuestionSetRequest;
import com.mathstrokes.exam.dto.TestRequest;
import com.mathstrokes.exam.dto.TestResponse;
import com.mathstrokes.exam.entity.ExamTest;
import com.mathstrokes.exam.entity.TestQuestion;
import com.mathstrokes.exam.mapper.TestMapper;
import com.mathstrokes.exam.repository.ExamTestRepository;
import com.mathstrokes.exam.repository.TestQuestionRepository;
import com.mathstrokes.question.dto.QuestionSummaryResponse;
import com.mathstrokes.question.entity.Question;
import com.mathstrokes.question.mapper.QuestionMapper;
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
    private final QuestionMapper questionMapper;

    public TestService(ExamTestRepository testRepository,
                       TestQuestionRepository testQuestionRepository,
                       QuestionRepository questionRepository,
                       QuestionSelectionService selectionService,
                       CatalogService catalogService,
                       UserRepository userRepository,
                       TestMapper mapper,
                       QuestionMapper questionMapper) {
        this.testRepository = testRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.questionRepository = questionRepository;
        this.selectionService = selectionService;
        this.catalogService = catalogService;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.questionMapper = questionMapper;
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
        ExamTest test = new ExamTest();
        assignScope(test, request);
        applyRequest(test, request, 0);
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
        assignScope(test, request);
        applyRequest(test, request, testQuestionRepository.countByTestId(id));
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

        // The same zero-guard that lets a hand-picked paper survive publishing is a trap at the
        // other end of it. For a PRACTICE test, "no questions attached yet" correctly means "draw
        // one now". For a class test it means the teacher has not chosen the paper, and drawing
        // one anyway would hand them a random examination while they believed they had picked it.
        // A silent wrong answer is worse than a refusal: nobody finds out until the class is
        // sitting the paper.
        if (test.isClassTest() && testQuestionRepository.countByTestId(id) == 0) {
            throw new BusinessRuleException("This class test has no questions yet. Choose its "
                    + "paper before making it live.");
        }

        if (test.hasFixedQuestionSet() && testQuestionRepository.countByTestId(id) == 0) {
            materialiseQuestionSet(test);
        } else if (!test.hasFixedQuestionSet()) {
            // Draw nothing yet, but fail now rather than at a student's first click if the bank
            // cannot support the blueprint.
            selectionService.selectQuestionIds(blueprintOf(test));
        }

        // A hand-picked paper is the authority on its own length. AttemptService refuses to
        // start an attempt when the stored count and the attached questions disagree, and the
        // error it raises reaches the student rather than the teacher - so reconcile it here,
        // where a teacher is present, rather than leaving it for a student's first click.
        long attached = testQuestionRepository.countByTestId(id);
        if (attached > 0) {
            test.setQuestionCount((int) attached);
        }

        test.setStatus(TestStatus.PUBLISHED);
        test.setPublishedAt(Instant.now());
        // Publishing a CLOSED test reopens it. Leaving closedAt behind would have the paper
        // reporting that it closed at a moment before it was open - which mattered little when
        // publishing happened once, and matters now that taking a class test on and off air is a
        // routine act.
        test.setClosedAt(null);
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

    /**
     * Pins an exact, hand-picked paper to a draft test.
     *
     * The list replaces whatever was attached, so a reorder and an edit are the same operation and
     * a half-changed paper is not representable. The position on the paper is the position in the
     * list; there is no separate order field that could drift out of step with it.
     *
     * Drafts only, for the same reason update() is drafts only: a published test may already have
     * been sat, and changing the questions underneath a submitted attempt would rewrite what the
     * student was actually asked.
     */
    @Transactional
    public TestResponse attachQuestions(Long id, TestQuestionSetRequest request) {
        ExamTest test = requireTest(id);
        if (test.getStatus() != TestStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only a draft test can have its questions changed. This test is "
                            + test.getStatus() + " and students may already have sat it.");
        }
        if (!test.hasFixedQuestionSet()) {
            throw new BusinessRuleException(
                    "This test draws a fresh set of questions for every attempt, so a fixed paper "
                            + "cannot be pinned to it. Change it to a fixed set first.");
        }

        List<Long> requested = request.questionIds();
        Set<Long> distinct = new LinkedHashSet<>(requested);
        if (distinct.size() != requested.size()) {
            throw new BusinessRuleException(
                    "The same question has been picked more than once. A question can appear on a "
                            + "paper only once.");
        }

        Map<Long, Question> byId = new java.util.HashMap<>();
        questionRepository.findAllByIdWithOptions(requested)
                .forEach(question -> byId.put(question.getId(), question));

        List<Long> missing = requested.stream().filter(qid -> !byId.containsKey(qid)).toList();
        if (!missing.isEmpty()) {
            throw new BusinessRuleException(
                    "These questions no longer exist: " + missing + ". Refresh and pick again.");
        }

        List<TestQuestion> paper = new ArrayList<>();
        int order = 1;
        for (Long questionId : requested) {
            Question question = byId.get(questionId);
            validateFitsPaper(test, question);
            TestQuestion entry = new TestQuestion();
            entry.setQuestion(question);
            entry.setQuestionOrder(order++);
            paper.add(entry);
        }

        // Emptied and flushed before the new rows go in. test_questions is uniquely indexed on
        // (test_id, question_order), and without the flush Hibernate is free to issue the inserts
        // before the deletes and collide with the paper being replaced.
        test.getQuestions().clear();
        testRepository.flush();
        paper.forEach(test::addQuestion);

        // The picked list defines the length of the paper. Nothing else is allowed to hold an
        // opinion about it - see the note in publish().
        test.setQuestionCount(paper.size());
        return mapper.toResponse(test, paper.size());
    }

    /** The attached paper, in the order it will be sat. Empty until questions are picked. */
    public List<QuestionSummaryResponse> listQuestions(Long id) {
        // Resolve the test first, so an unknown id is a 404 rather than a convincing empty paper.
        requireTest(id);
        return testQuestionRepository.findByTestWithQuestions(id).stream()
                .map(entry -> questionMapper.toSummary(entry.getQuestion()))
                .toList();
    }

    /**
     * Whether one hand-picked question belongs on this paper.
     *
     * A chapter test may hold only questions from its chapter; a full-syllabus paper may mix
     * chapters freely, which is how a teacher builds a paper spanning the term. That asymmetry is
     * not a special case - it is V6's rule that the ABSENCE of a chapter is the full-syllabus
     * signal, read from the other direction.
     */
    private void validateFitsPaper(ExamTest test, Question question) {
        if (question.getStatus() != QuestionStatus.PUBLISHED) {
            throw new BusinessRuleException(quoted(question) + " is still a "
                    + question.getStatus().name().toLowerCase()
                    + " question. Publish it before putting it on a paper.");
        }
        if (question.getExamPattern() != test.getExamPattern()) {
            throw new BusinessRuleException(quoted(question) + " is a "
                    + label(question.getExamPattern()) + " question and this is a "
                    + label(test.getExamPattern()) + " paper. The two are marked differently, so "
                    + "they cannot be mixed on one test.");
        }
        if (!question.getSubject().getId().equals(test.getSubject().getId())) {
            throw new BusinessRuleException(quoted(question) + " belongs to "
                    + question.getSubject().getName() + ", not " + test.getSubject().getName()
                    + ".");
        }
        if (test.chapterId() != null && !question.getChapter().getId().equals(test.chapterId())) {
            throw new BusinessRuleException(quoted(question) + " is from "
                    + question.getChapter().getName() + ", but this test is set to "
                    + test.chapterName() + ". Clear the chapter to build a paper that spans "
                    + "several of them.");
        }
    }

    private String label(ExamPattern pattern) {
        return pattern == ExamPattern.JEE_ADVANCED ? "JEE Advanced" : "JEE Main";
    }

    /**
     * Enough of a question to identify it in an error a teacher has to act on. An id alone would
     * send them back to the picker to work out which row it meant.
     */
    private String quoted(Question question) {
        String content = question.getQuestionContent() == null
                ? ""
                : question.getQuestionContent().replaceAll("\\s+", " ").trim();
        return "\"" + (content.length() <= 60 ? content : content.substring(0, 60) + "...") + "\"";
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

    /**
     * Decides what the test draws from.
     *
     * A chapter id means one chapter, and the subject comes from it. No chapter id means the full
     * syllabus, and the subject has to be supplied - or inferred while the platform has exactly
     * one, which keeps the admin form simple today without stopping a second subject later.
     */
    private void assignScope(ExamTest test, TestRequest request) {
        if (request.chapterId() != null) {
            Chapter chapter = catalogService.requireChapter(request.chapterId());
            test.setChapter(chapter);
            test.setSubject(chapter.getSubject());
            return;
        }
        test.setChapter(null);
        test.setSubject(resolveSubjectForFullSyllabus(request.subjectId()));
    }

    private Subject resolveSubjectForFullSyllabus(Long subjectId) {
        if (subjectId != null) {
            return catalogService.requireSubject(subjectId);
        }
        List<SubjectResponse> active = catalogService.listSubjects(false);
        if (active.size() == 1) {
            return catalogService.requireSubject(active.get(0).id());
        }
        throw new BusinessRuleException(
                "A full-syllabus test needs a subject. Send subjectId, or choose a chapter.");
    }

    private void applyRequest(ExamTest test, TestRequest request, long attachedQuestionCount) {
        test.setTitle(request.title().trim());
        test.setDescription(request.description());
        test.setExamPattern(request.examPattern());
        test.setDurationMinutes(request.durationMinutes());
        // Once a paper has been hand-picked its length is a fact about the questions attached,
        // not a number the form is allowed to disagree with. Accepting a different value here
        // would make AttemptService refuse the start and blame the teacher in a message the
        // student is the one who reads.
        test.setQuestionCount(attachedQuestionCount > 0
                ? (int) attachedQuestionCount
                : request.questionCount());
        test.setGenerationMode(request.generationMode());
        test.setEasyCount(request.easyCount());
        test.setMediumCount(request.mediumCount());
        test.setHardCount(request.hardCount());
        test.setMaxAttemptsPerStudent(
                request.maxAttemptsPerStudent() == null ? 1 : request.maxAttemptsPerStudent());
        // Only an identical paper for every candidate yields a comparable cohort.
        test.setRankingEnabled(request.generationMode() == TestGenerationMode.FIXED_SET);
        test.setTestKind(request.testKind() == null ? TestKind.PRACTICE : request.testKind());
        test.setScheduledStartAt(request.scheduledStartAt());
        test.setScheduledEndAt(request.scheduledEndAt());
        validateBlueprint(test);
        validateSchedule(test);
    }

    /**
     * A window shorter than the test's own duration is allowed on purpose: "begin any time in this
     * ten-minute slot, then you get your full hour" is a real way to run a class test. Only an end
     * that does not follow its start is meaningless, so only that is refused.
     */
    private void validateSchedule(ExamTest test) {
        if (test.getScheduledStartAt() != null && test.getScheduledEndAt() != null
                && !test.getScheduledEndAt().isAfter(test.getScheduledStartAt())) {
            throw new BusinessRuleException(
                    "This test would stop accepting students at or before it opens. Check the "
                            + "start and end times.");
        }
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
        // A null chapter id widens the draw to every chapter of the subject.
        return new QuestionSelectionService.Blueprint(test.chapterId(),
                test.getExamPattern(), test.getQuestionCount(), bands);
    }

    private void putIfPositive(Map<Difficulty, Integer> bands, Difficulty difficulty, Integer count) {
        if (count != null && count > 0) {
            bands.put(difficulty, count);
        }
    }
}
