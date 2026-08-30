package com.mathstrokes.question.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mathstrokes.catalog.entity.Chapter;
import com.mathstrokes.catalog.service.CatalogService;
import com.mathstrokes.common.dto.PageResponse;
import com.mathstrokes.common.enums.Difficulty;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionStatus;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.common.exception.BusinessRuleException;
import com.mathstrokes.common.exception.ResourceNotFoundException;
import com.mathstrokes.marking.service.MarkingSchemeService;
import com.mathstrokes.question.dto.QuestionOptionRequest;
import com.mathstrokes.question.dto.QuestionRequest;
import com.mathstrokes.question.dto.QuestionResponse;
import com.mathstrokes.question.dto.QuestionSummaryResponse;
import com.mathstrokes.question.entity.Question;
import com.mathstrokes.question.entity.QuestionOption;
import com.mathstrokes.question.mapper.QuestionMapper;
import com.mathstrokes.question.repository.QuestionRepository;
import com.mathstrokes.question.repository.QuestionSpecifications;
import com.mathstrokes.security.service.SecurityUtils;
import com.mathstrokes.user.entity.User;
import com.mathstrokes.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final CatalogService catalogService;
    private final MarkingSchemeService markingSchemeService;
    private final QuestionValidator validator;
    private final QuestionMapper mapper;
    private final UserRepository userRepository;

    public QuestionService(QuestionRepository questionRepository,
                           CatalogService catalogService,
                           MarkingSchemeService markingSchemeService,
                           QuestionValidator validator,
                           QuestionMapper mapper,
                           UserRepository userRepository) {
        this.questionRepository = questionRepository;
        this.catalogService = catalogService;
        this.markingSchemeService = markingSchemeService;
        this.validator = validator;
        this.mapper = mapper;
        this.userRepository = userRepository;
    }

    public PageResponse<QuestionSummaryResponse> search(Long subjectId, Long chapterId,
                                                        ExamPattern examPattern,
                                                        Difficulty difficulty,
                                                        QuestionType questionType,
                                                        QuestionStatus status,
                                                        String search,
                                                        Pageable pageable) {
        Specification<Question> specification = QuestionSpecifications.allOf(
                QuestionSpecifications.subjectIs(subjectId),
                QuestionSpecifications.chapterIs(chapterId),
                QuestionSpecifications.examPatternIs(examPattern),
                QuestionSpecifications.difficultyIs(difficulty),
                QuestionSpecifications.questionTypeIs(questionType),
                QuestionSpecifications.statusIs(status),
                QuestionSpecifications.textContains(search));
        Page<Question> page = questionRepository.findAll(specification, pageable);
        return PageResponse.from(page, mapper::toSummary);
    }

    public QuestionResponse get(Long id) {
        return mapper.toResponse(requireQuestion(id));
    }

    public Question requireQuestion(Long id) {
        return questionRepository.findByIdWithOptions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question", id));
    }

    @Transactional
    public QuestionResponse create(QuestionRequest request) {
        validator.validateForSave(request);
        Question question = new Question();
        applyRequest(question, request);
        question.setStatus(QuestionStatus.DRAFT);
        currentUser().ifPresent(user -> {
            question.setCreatedBy(user);
            question.setUpdatedBy(user);
        });
        return mapper.toResponse(questionRepository.save(question));
    }

    /**
     * Editing a question that has already been used in an attempt is allowed: the attempt holds
     * its own snapshot of the content, options and answer key, so no historical result can move.
     * The version increments, which is what the snapshot recorded.
     */
    @Transactional
    public QuestionResponse update(Long id, QuestionRequest request) {
        validator.validateForSave(request);
        Question question = requireQuestion(id);
        if (question.getStatus() == QuestionStatus.ARCHIVED) {
            throw new BusinessRuleException(
                    "Archived questions are read-only. Restore it to a draft before editing.");
        }
        applyRequest(question, request);
        currentUser().ifPresent(question::setUpdatedBy);
        if (question.getStatus() == QuestionStatus.PUBLISHED) {
            // It stays published, so it must still satisfy the stricter publish rules.
            validator.validateForPublish(request.questionType(), request.options());
        }
        // Flush so the response carries the incremented version rather than the pre-write one.
        // The admin UI sends that value back on the next save, and a stale value would make an
        // ordinary second edit look like a concurrent-modification conflict.
        questionRepository.flush();
        return mapper.toResponse(question);
    }

    @Transactional
    public QuestionResponse publish(Long id) {
        Question question = requireQuestion(id);
        if (question.getStatus() == QuestionStatus.PUBLISHED) {
            return mapper.toResponse(question);
        }
        validator.validateForPublish(question.getQuestionType(), toOptionRequests(question));
        // Fail here rather than at test-generation time if nothing can score this question.
        markingSchemeService.resolveFor(question.getExamPattern(), question.getQuestionType());
        question.setStatus(QuestionStatus.PUBLISHED);
        question.setPublishedAt(Instant.now());
        currentUser().ifPresent(question::setUpdatedBy);
        return mapper.toResponse(question);
    }

    @Transactional
    public QuestionResponse revertToDraft(Long id) {
        Question question = requireQuestion(id);
        question.setStatus(QuestionStatus.DRAFT);
        currentUser().ifPresent(question::setUpdatedBy);
        return mapper.toResponse(question);
    }

    /**
     * Archiving withdraws a question from future test generation without deleting it, so the
     * attempts that already used it keep their foreign key and their analytics.
     */
    @Transactional
    public QuestionResponse archive(Long id) {
        Question question = requireQuestion(id);
        question.setStatus(QuestionStatus.ARCHIVED);
        currentUser().ifPresent(question::setUpdatedBy);
        return mapper.toResponse(question);
    }

    private void applyRequest(Question question, QuestionRequest request) {
        Chapter chapter = catalogService.requireChapter(request.chapterId());
        question.setChapter(chapter);
        question.setSubject(chapter.getSubject());
        question.setExamPattern(request.examPattern());
        question.setDifficulty(request.difficulty());
        question.setQuestionType(request.questionType());
        question.setQuestionContent(request.questionContent().trim());
        question.setSolutionContent(request.solutionContent());
        question.setMarkingScheme(request.markingSchemeId() == null
                ? null
                : markingSchemeService.requireScheme(request.markingSchemeId()));
        applyOptions(question, request.options());
    }

    /**
     * Merges the submitted options onto the question in place, matching on option label.
     *
     * Deliberately NOT a clear-and-recreate. Hibernate orders all inserts before all deletes
     * within a flush, so replacing the collection wholesale makes the new rows collide with the
     * old ones on the (question_id, option_key) unique index and the whole edit fails. Matching
     * by label means an ordinary edit issues UPDATEs and touches no constraint, and the explicit
     * flush after the removals covers the awkward case where an admin reshuffles labels.
     */
    private void applyOptions(Question question, List<QuestionOptionRequest> requests) {
        Map<String, QuestionOptionRequest> wanted = new LinkedHashMap<>();
        for (QuestionOptionRequest request : requests) {
            wanted.put(request.optionKey().toUpperCase(), request);
        }

        boolean removedAny = question.getOptions()
                .removeIf(option -> !wanted.containsKey(option.getOptionKey()));
        if (removedAny) {
            // Force the DELETEs out before any INSERT can claim a freed label.
            questionRepository.flush();
        }

        Map<String, QuestionOption> existing = question.getOptions().stream()
                .collect(Collectors.toMap(QuestionOption::getOptionKey, option -> option));

        int index = 0;
        for (Map.Entry<String, QuestionOptionRequest> entry : wanted.entrySet()) {
            QuestionOptionRequest source = entry.getValue();
            QuestionOption option = existing.get(entry.getKey());
            if (option == null) {
                option = new QuestionOption();
                option.setOptionKey(entry.getKey());
                question.addOption(option);
            }
            option.setContent(source.content().trim());
            option.setDisplayOrder(
                    source.displayOrder() == null ? index : source.displayOrder());
            option.setCorrect(source.isCorrect());
            index++;
        }
    }

    private List<QuestionOptionRequest> toOptionRequests(Question question) {
        return question.getOptions().stream()
                .map(o -> new QuestionOptionRequest(o.getOptionKey(), o.getContent(),
                        o.getDisplayOrder(), o.isCorrect()))
                .toList();
    }

    private Optional<User> currentUser() {
        return SecurityUtils.currentPrincipal()
                .flatMap(principal -> userRepository.findById(principal.id()));
    }
}
