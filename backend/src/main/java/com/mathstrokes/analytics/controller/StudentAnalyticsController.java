package com.mathstrokes.analytics.controller;

import com.mathstrokes.analytics.dto.StudentPerformanceResponse;
import com.mathstrokes.analytics.service.StudentAnalyticsService;
import com.mathstrokes.security.service.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
@PreAuthorize("hasRole('STUDENT')")
@Tag(name = "Student", description = "The signed-in student's own dashboard")
public class StudentAnalyticsController {

    private final StudentAnalyticsService analyticsService;

    public StudentAnalyticsController(StudentAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/performance")
    @Operation(summary = "The signed-in student's performance summary",
            description = "Always scoped to the caller; there is no way to request another "
                    + "student's figures.")
    public StudentPerformanceResponse performance() {
        return analyticsService.summaryFor(SecurityUtils.requireUserId());
    }
}
