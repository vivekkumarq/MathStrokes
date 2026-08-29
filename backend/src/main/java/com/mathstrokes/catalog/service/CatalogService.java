package com.mathstrokes.catalog.service;

import java.util.List;

import com.mathstrokes.catalog.dto.ChapterRequest;
import com.mathstrokes.catalog.dto.ChapterResponse;
import com.mathstrokes.catalog.dto.SubjectRequest;
import com.mathstrokes.catalog.dto.SubjectResponse;
import com.mathstrokes.catalog.entity.Chapter;
import com.mathstrokes.catalog.entity.Subject;
import com.mathstrokes.catalog.mapper.CatalogMapper;
import com.mathstrokes.catalog.repository.ChapterRepository;
import com.mathstrokes.catalog.repository.SubjectRepository;
import com.mathstrokes.common.exception.DuplicateResourceException;
import com.mathstrokes.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final SubjectRepository subjectRepository;
    private final ChapterRepository chapterRepository;
    private final CatalogMapper mapper;

    public CatalogService(SubjectRepository subjectRepository,
                          ChapterRepository chapterRepository,
                          CatalogMapper mapper) {
        this.subjectRepository = subjectRepository;
        this.chapterRepository = chapterRepository;
        this.mapper = mapper;
    }

    // ------------------------------------------------------------------ subjects

    public List<SubjectResponse> listSubjects(boolean includeInactive) {
        List<Subject> subjects = includeInactive
                ? subjectRepository.findAllByOrderByDisplayOrderAscNameAsc()
                : subjectRepository.findAllByActiveTrueOrderByDisplayOrderAscNameAsc();
        return subjects.stream()
                .map(s -> mapper.toResponse(s, chapterRepository.countBySubjectId(s.getId())))
                .toList();
    }

    public Subject requireSubject(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", id));
    }

    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        if (subjectRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("A subject named '" + request.name() + "' already exists");
        }
        if (subjectRepository.existsByCodeIgnoreCase(request.code())) {
            throw new DuplicateResourceException("Subject code '" + request.code() + "' is already in use");
        }
        Subject subject = new Subject();
        subject.setName(request.name().trim());
        subject.setCode(request.code().trim().toUpperCase());
        subject.setDescription(request.description());
        subject.setActive(request.active() == null || request.active());
        subject.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        Subject saved = subjectRepository.save(subject);
        return mapper.toResponse(saved, 0);
    }

    @Transactional
    public SubjectResponse updateSubject(Long id, SubjectRequest request) {
        Subject subject = requireSubject(id);
        if (!subject.getName().equalsIgnoreCase(request.name())
                && subjectRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("A subject named '" + request.name() + "' already exists");
        }
        if (!subject.getCode().equalsIgnoreCase(request.code())
                && subjectRepository.existsByCodeIgnoreCase(request.code())) {
            throw new DuplicateResourceException("Subject code '" + request.code() + "' is already in use");
        }
        subject.setName(request.name().trim());
        subject.setCode(request.code().trim().toUpperCase());
        subject.setDescription(request.description());
        if (request.active() != null) {
            subject.setActive(request.active());
        }
        if (request.displayOrder() != null) {
            subject.setDisplayOrder(request.displayOrder());
        }
        return mapper.toResponse(subject, chapterRepository.countBySubjectId(subject.getId()));
    }

    // ------------------------------------------------------------------ chapters

    /** Student-facing: only active chapters of an active subject. */
    public List<ChapterResponse> listActiveChapters(Long subjectId) {
        return chapterRepository.findActiveBySubject(subjectId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /** Admin-facing: everything, including deactivated chapters. */
    public List<ChapterResponse> listAllChapters(Long subjectId) {
        return chapterRepository.findAllForAdmin(subjectId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public Chapter requireChapter(Long id) {
        return chapterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Chapter", id));
    }

    public ChapterResponse getChapter(Long id) {
        return mapper.toResponse(requireChapter(id));
    }

    @Transactional
    public ChapterResponse createChapter(ChapterRequest request) {
        Subject subject = requireSubject(request.subjectId());
        if (chapterRepository.existsBySubjectIdAndNameIgnoreCase(subject.getId(), request.name())) {
            throw new DuplicateResourceException(
                    "Chapter '" + request.name() + "' already exists in " + subject.getName());
        }
        Chapter chapter = new Chapter();
        chapter.setSubject(subject);
        chapter.setName(request.name().trim());
        chapter.setDescription(request.description());
        chapter.setActive(request.active() == null || request.active());
        chapter.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
        return mapper.toResponse(chapterRepository.save(chapter));
    }

    @Transactional
    public ChapterResponse updateChapter(Long id, ChapterRequest request) {
        Chapter chapter = requireChapter(id);
        Subject subject = requireSubject(request.subjectId());
        boolean movingOrRenaming = !chapter.getSubject().getId().equals(subject.getId())
                || !chapter.getName().equalsIgnoreCase(request.name());
        if (movingOrRenaming
                && chapterRepository.existsBySubjectIdAndNameIgnoreCase(subject.getId(), request.name())) {
            throw new DuplicateResourceException(
                    "Chapter '" + request.name() + "' already exists in " + subject.getName());
        }
        chapter.setSubject(subject);
        chapter.setName(request.name().trim());
        chapter.setDescription(request.description());
        if (request.active() != null) {
            chapter.setActive(request.active());
        }
        if (request.displayOrder() != null) {
            chapter.setDisplayOrder(request.displayOrder());
        }
        return mapper.toResponse(chapter);
    }

    /**
     * Chapters are deactivated rather than deleted: questions and historical attempts reference
     * them, so a hard delete would either fail on the foreign key or orphan reporting data.
     */
    @Transactional
    public ChapterResponse setChapterActive(Long id, boolean active) {
        Chapter chapter = requireChapter(id);
        chapter.setActive(active);
        return mapper.toResponse(chapter);
    }
}
