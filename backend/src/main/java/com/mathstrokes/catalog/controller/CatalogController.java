package com.mathstrokes.catalog.controller;

import java.util.List;

import com.mathstrokes.catalog.dto.ChapterResponse;
import com.mathstrokes.catalog.dto.SubjectResponse;
import com.mathstrokes.catalog.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only catalogue for authenticated users. Students see only what is active, which is why
 * the chapter list is never hardcoded in the Angular client.
 */
@RestController
@RequestMapping("/catalog")
@Tag(name = "Catalogue", description = "Subjects and chapters available to take tests in")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/subjects")
    @Operation(summary = "List active subjects")
    public List<SubjectResponse> subjects() {
        return catalogService.listSubjects(false);
    }

    @GetMapping("/subjects/{subjectId}/chapters")
    @Operation(summary = "List active chapters of a subject")
    public List<ChapterResponse> chapters(@PathVariable Long subjectId) {
        return catalogService.listActiveChapters(subjectId);
    }
}
