package com.mathstrokes.user.controller;

import com.mathstrokes.analytics.dto.StudentPerformanceResponse;
import com.mathstrokes.analytics.service.StudentAnalyticsService;
import com.mathstrokes.attempt.dto.AttemptSummaryResponse;
import com.mathstrokes.attempt.service.AttemptService;
import com.mathstrokes.auth.dto.UserProfileResponse;
import com.mathstrokes.common.dto.PageResponse;
import com.mathstrokes.user.dto.StudentSummaryResponse;
import com.mathstrokes.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/students")
@Tag(name = "Admin - Students", description = "Student roster")
public class AdminStudentController {

    private final UserService userService;
    private final AttemptService attemptService;
    private final StudentAnalyticsService analyticsService;

    public AdminStudentController(UserService userService, AttemptService attemptService,
                                  StudentAnalyticsService analyticsService) {
        this.userService = userService;
        this.attemptService = attemptService;
        this.analyticsService = analyticsService;
    }

    @GetMapping
    @Operation(summary = "List students",
            description = "Searchable by name or phone number. Includes each student's attempt "
                    + "count; never any password or security-answer material.")
    public PageResponse<StudentSummaryResponse> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return userService.listStudents(search, pageable);
    }

    /**
     * A named student's attempts.
     *
     * The student-facing /attempts/history deliberately reads the caller's own identity and must
     * keep doing so. This is the deliberate admin counterpart: the id is explicit, it is checked
     * to belong to a student, and the whole route sits behind ROLE_ADMIN.
     */
    @GetMapping("/{id}/attempts")
    @Operation(summary = "A student's attempt history",
            description = "Scores and ranks, not answers. Paginated, most recent first.")
    public PageResponse<AttemptSummaryResponse> attempts(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        userService.requireStudent(id);
        return attemptService.history(id, pageable);
    }

    @GetMapping("/{id}/performance")
    @Operation(summary = "A student's performance summary",
            description = "The same figures the student sees on their own dashboard: tests taken, "
                    + "average and best score, accuracy, best rank, recent scores and a chapter "
                    + "breakdown.")
    public StudentPerformanceResponse performance(@PathVariable Long id) {
        userService.requireStudent(id);
        return analyticsService.summaryFor(id);
    }

    @PatchMapping("/{id}/enabled")
    @Operation(summary = "Enable or disable a student account",
            description = "Disabling blocks sign-in but keeps the account and its history. "
                    + "Accounts are never deleted, because results reference them.")
    public UserProfileResponse setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return userService.setEnabled(id, enabled);
    }
}
