package com.mathstrokes.catalog.mapper;

import com.mathstrokes.catalog.dto.ChapterResponse;
import com.mathstrokes.catalog.dto.SubjectResponse;
import com.mathstrokes.catalog.entity.Chapter;
import com.mathstrokes.catalog.entity.Subject;
import org.springframework.stereotype.Component;

@Component
public class CatalogMapper {

    public SubjectResponse toResponse(Subject subject, long chapterCount) {
        return new SubjectResponse(subject.getId(), subject.getName(), subject.getCode(),
                subject.getDescription(), subject.isActive(), subject.getDisplayOrder(), chapterCount);
    }

    public ChapterResponse toResponse(Chapter chapter) {
        return new ChapterResponse(chapter.getId(), chapter.getSubject().getId(),
                chapter.getSubject().getName(), chapter.getName(), chapter.getDescription(),
                chapter.isActive(), chapter.getDisplayOrder());
    }
}
