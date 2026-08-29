package com.mathstrokes.catalog.controller;

import java.util.List;

import com.mathstrokes.catalog.dto.ChapterRequest;
import com.mathstrokes.catalog.dto.ChapterResponse;
import com.mathstrokes.catalog.dto.SubjectRequest;
import com.mathstrokes.catalog.dto.SubjectResponse;
import com.mathstrokes.catalog.service.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Chapter and subject administration. Reachable only with ROLE_ADMIN (see SecurityConfig). */
@RestController
@RequestMapping("/admin/catalog")
@Tag(name = "Admin - Catalogue", description = "Subject and chapter management")
public class AdminCatalogController {

    private final CatalogService catalogService;

    public AdminCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/subjects")
    @Operation(summary = "List every subject, including inactive ones")
    public List<SubjectResponse> subjects() {
        return catalogService.listSubjects(true);
    }

    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a subject")
    public SubjectResponse createSubject(@Valid @RequestBody SubjectRequest request) {
        return catalogService.createSubject(request);
    }

    @PutMapping("/subjects/{id}")
    @Operation(summary = "Update a subject")
    public SubjectResponse updateSubject(@PathVariable Long id,
                                         @Valid @RequestBody SubjectRequest request) {
        return catalogService.updateSubject(id, request);
    }

    @GetMapping("/chapters")
    @Operation(summary = "List chapters, optionally filtered by subject")
    public List<ChapterResponse> chapters(@RequestParam(required = false) Long subjectId) {
        return catalogService.listAllChapters(subjectId);
    }

    @GetMapping("/chapters/{id}")
    @Operation(summary = "Fetch a single chapter")
    public ChapterResponse chapter(@PathVariable Long id) {
        return catalogService.getChapter(id);
    }

    @PostMapping("/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a chapter")
    public ChapterResponse createChapter(@Valid @RequestBody ChapterRequest request) {
        return catalogService.createChapter(request);
    }

    @PutMapping("/chapters/{id}")
    @Operation(summary = "Update a chapter")
    public ChapterResponse updateChapter(@PathVariable Long id,
                                         @Valid @RequestBody ChapterRequest request) {
        return catalogService.updateChapter(id, request);
    }

    @PatchMapping("/chapters/{id}/active")
    @Operation(summary = "Activate or deactivate a chapter")
    public ChapterResponse setChapterActive(@PathVariable Long id, @RequestParam boolean active) {
        return catalogService.setChapterActive(id, active);
    }
}
