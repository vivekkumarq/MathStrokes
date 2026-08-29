package com.mathstrokes.exam.controller;

import com.mathstrokes.common.dto.PageResponse;
import com.mathstrokes.common.enums.ExamPattern;
import com.mathstrokes.common.enums.TestStatus;
import com.mathstrokes.exam.dto.TestRequest;
import com.mathstrokes.exam.dto.TestResponse;
import com.mathstrokes.exam.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/tests")
@Tag(name = "Admin - Tests", description = "Creating and publishing examinations")
public class AdminTestController {

    private final TestService testService;

    public AdminTestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping
    @Operation(summary = "List tests")
    public PageResponse<TestResponse> search(
            @RequestParam(required = false) TestStatus status,
            @RequestParam(required = false) Long chapterId,
            @RequestParam(required = false) ExamPattern examPattern,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return testService.search(status, chapterId, examPattern, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch one test")
    public TestResponse get(@PathVariable Long id) {
        return testService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a test as a draft")
    public TestResponse create(@Valid @RequestBody TestRequest request) {
        return testService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a draft test",
            description = "Only drafts are editable; a published test may already have been sat.")
    public TestResponse update(@PathVariable Long id, @Valid @RequestBody TestRequest request) {
        return testService.update(id, request);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a test",
            description = "For a fixed-set test this draws the paper once and pins it, so every "
                    + "student sits the identical examination and the ranking is comparable.")
    public TestResponse publish(@PathVariable Long id) {
        return testService.publish(id);
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a test",
            description = "Stops new attempts. Attempts already in progress finish normally.")
    public TestResponse close(@PathVariable Long id) {
        return testService.close(id);
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive a test")
    public TestResponse archive(@PathVariable Long id) {
        return testService.archive(id);
    }
}
