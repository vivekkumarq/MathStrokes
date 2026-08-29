package com.mathstrokes.marking.service;

import java.util.List;

import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.QuestionType;
import com.mathstrokes.common.exception.BusinessRuleException;
import com.mathstrokes.common.exception.DuplicateResourceException;
import com.mathstrokes.common.exception.ResourceNotFoundException;
import com.mathstrokes.marking.dto.MarkingSchemeRequest;
import com.mathstrokes.marking.dto.MarkingSchemeResponse;
import com.mathstrokes.marking.entity.MarkingScheme;
import com.mathstrokes.marking.repository.MarkingSchemeRepository;
import com.mathstrokes.marking.strategy.EvaluationStrategyRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MarkingSchemeService {

    private final MarkingSchemeRepository repository;
    private final EvaluationStrategyRegistry strategyRegistry;

    public MarkingSchemeService(MarkingSchemeRepository repository,
                                EvaluationStrategyRegistry strategyRegistry) {
        this.repository = repository;
        this.strategyRegistry = strategyRegistry;
    }

    public List<MarkingSchemeResponse> listAll() {
        return repository.findAllByOrderByExamPatternAscQuestionTypeAscNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public MarkingScheme requireScheme(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marking scheme", id));
    }

    /**
     * The scheme a question is scored with, resolved at snapshot time. An explicit scheme on the
     * question wins; otherwise the active scheme for the (pattern, type) pair is used.
     */
    public MarkingScheme resolveFor(ExamPattern examPattern, QuestionType questionType) {
        return repository.findByExamPatternAndQuestionTypeAndActiveTrue(examPattern, questionType)
                .orElseThrow(() -> new BusinessRuleException(
                        "No active marking scheme is configured for " + examPattern + " / "
                                + questionType + ". Create one before publishing questions of this kind."));
    }

    @Transactional
    public MarkingSchemeResponse create(MarkingSchemeRequest request) {
        if (repository.findByName(request.name()).isPresent()) {
            throw new DuplicateResourceException(
                    "A marking scheme named '" + request.name() + "' already exists");
        }
        validate(request);
        MarkingScheme scheme = new MarkingScheme();
        applyRequest(scheme, request);
        boolean active = request.active() == null || request.active();
        if (active) {
            deactivateExistingActive(request.examPattern(), request.questionType(), null);
        }
        scheme.setActive(active);
        return toResponse(repository.save(scheme));
    }

    @Transactional
    public MarkingSchemeResponse update(Long id, MarkingSchemeRequest request) {
        MarkingScheme scheme = requireScheme(id);
        repository.findByName(request.name())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A marking scheme named '" + request.name() + "' already exists");
                });
        validate(request);
        applyRequest(scheme, request);
        boolean active = request.active() == null || request.active();
        if (active) {
            deactivateExistingActive(request.examPattern(), request.questionType(), id);
        }
        scheme.setActive(active);
        return toResponse(scheme);
    }

    @Transactional
    public MarkingSchemeResponse setActive(Long id, boolean active) {
        MarkingScheme scheme = requireScheme(id);
        if (active) {
            deactivateExistingActive(scheme.getExamPattern(), scheme.getQuestionType(), id);
        }
        scheme.setActive(active);
        return toResponse(scheme);
    }

    /**
     * Only one scheme per (pattern, type) may be active. Activating one stands the previous one
     * down instead of failing the unique index, which is what an admin expects.
     * Historical attempts are unaffected: they carry their own snapshot of the configuration.
     */
    private void deactivateExistingActive(ExamPattern pattern, QuestionType type, Long exceptId) {
        repository.findByExamPatternAndQuestionTypeAndActiveTrue(pattern, type)
                .filter(existing -> exceptId == null || !existing.getId().equals(exceptId))
                .ifPresent(existing -> existing.setActive(false));
        repository.flush();
    }

    private void validate(MarkingSchemeRequest request) {
        if (!strategyRegistry.supports(request.questionType())) {
            throw new BusinessRuleException("Question type " + request.questionType()
                    + " has no evaluation strategy, so it cannot be given a marking scheme yet");
        }
        request.configuration().validate();
    }

    private void applyRequest(MarkingScheme scheme, MarkingSchemeRequest request) {
        scheme.setName(request.name().trim());
        scheme.setDescription(request.description());
        scheme.setExamPattern(request.examPattern());
        scheme.setQuestionType(request.questionType());
        scheme.setConfiguration(request.configuration());
    }

    public MarkingSchemeResponse toResponse(MarkingScheme scheme) {
        return new MarkingSchemeResponse(scheme.getId(), scheme.getName(), scheme.getDescription(),
                scheme.getExamPattern(), scheme.getQuestionType(), scheme.getConfiguration(),
                scheme.isActive());
    }
}
