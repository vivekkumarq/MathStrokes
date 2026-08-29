package com.mathstrokes.catalog.dto;

public record SubjectResponse(Long id, String name, String code, String description,
                              boolean active, int displayOrder, long chapterCount) {
}
