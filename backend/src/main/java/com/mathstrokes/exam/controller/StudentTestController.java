package com.mathstrokes.exam.controller;

import java.util.List;

import com.mathstrokes.attempt.service.StudentTestCatalogService;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.exam.dto.AvailableTestResponse;
import com.mathstrokes.security.service.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tests")
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Tests", description = "Tests a student can sit")
public class StudentTestController {

    private final StudentTestCatalogService catalogService;

    public StudentTestController(StudentTestCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @Operation(summary = "Published tests, filtered by chapter and exam pattern",
            description = "Carries no question data. Each entry says whether the student can "
                    + "start it and whether an attempt is already in progress.")
    public List<AvailableTestResponse> available(@RequestParam(required = false) Long subjectId,
                                                 @RequestParam(required = false) Long chapterId,
                                                 @RequestParam(required = false) ExamPattern examPattern) {
        return catalogService.availableTests(SecurityUtils.requireUserId(), subjectId, chapterId,
                examPattern);
    }
}
