package com.mathstrokes.catalog.dto;

public record ChapterResponse(Long id, Long subjectId, String subjectName, String name,
                              String description, boolean active, int displayOrder) {
}
