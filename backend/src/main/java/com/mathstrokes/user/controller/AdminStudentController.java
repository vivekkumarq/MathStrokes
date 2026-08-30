package com.mathstrokes.user.controller;

import com.mathstrokes.auth.dto.UserProfileResponse;
import com.mathstrokes.common.dto.PageResponse;
import com.mathstrokes.user.dto.StudentSummaryResponse;
import com.mathstrokes.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
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

    public AdminStudentController(UserService userService) {
        this.userService = userService;
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

    @PatchMapping("/{id}/enabled")
    @Operation(summary = "Enable or disable a student account",
            description = "Disabling blocks sign-in but keeps the account and its history. "
                    + "Accounts are never deleted, because results reference them.")
    public UserProfileResponse setEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        return userService.setEnabled(id, enabled);
    }
}
